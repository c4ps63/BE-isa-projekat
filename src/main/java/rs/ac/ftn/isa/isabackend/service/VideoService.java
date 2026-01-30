package rs.ac.ftn.isa.isabackend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import rs.ac.ftn.isa.isabackend.dto.VideoDTO;
import rs.ac.ftn.isa.isabackend.model.User;
import rs.ac.ftn.isa.isabackend.model.Video;
import rs.ac.ftn.isa.isabackend.repository.UserRepository;
import rs.ac.ftn.isa.isabackend.repository.VideoRepository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;
import rs.ac.ftn.isa.isabackend.dto.TileClusterDTO;

@Service
public class VideoService {

    private final VideoRepository videoRepository;
    private final UserRepository userRepository;
    private final TileService tileService;
    private final Path rootLocation = Paths.get("uploads");

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    public VideoService(VideoRepository videoRepository, UserRepository userRepository, TileService tileService, CacheManager cacheManager) {
        this.videoRepository = videoRepository;
        this.userRepository = userRepository;
        this.tileService = tileService;
        this.cacheManager = cacheManager;
    }

    public Page<Video> findAll(int page, int size, String filter) {
        Pageable pageable = PageRequest.of(page, size);
        LocalDateTime cutoffDate;

        if ("LAST_30_DAYS".equalsIgnoreCase(filter)) {
            cutoffDate = LocalDateTime.now().minusDays(30);
            return videoRepository.findByUploadedAtAfterOrderByUploadedAtDesc(cutoffDate, pageable);
        } else if ("THIS_YEAR".equalsIgnoreCase(filter)) {
            cutoffDate = LocalDateTime.now().withDayOfYear(1).toLocalDate().atStartOfDay();
            return videoRepository.findByUploadedAtAfterOrderByUploadedAtDesc(cutoffDate, pageable);
        } else {
            return videoRepository.findAllByOrderByUploadedAtDesc(pageable);
        }
    }

    public Optional<Video> findById(Long id) {
        return videoRepository.findById(id);
    }

    public Page<Video> findByOwnerId(Long ownerId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return videoRepository.findByOwnerIdOrderByUploadedAtDesc(ownerId, pageable);
    }

    @Transactional
    public void incrementViewCount(Long videoId) {
        videoRepository.incrementViewCount(videoId);
    }

    @Transactional
    public Video save(Video video) {
        return videoRepository.save(video);
    }

    @Transactional
    public Video update(Video video) {
        return videoRepository.save(video);
    }

    @Transactional
    public void deleteById(Long id) {
        videoRepository.deleteById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public VideoDTO uploadVideoWithUser(String title, String description, MultipartFile videoFile, MultipartFile thumbnailFile, String username, Integer duration, String street, String number, String city) throws IOException {

        User owner = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Korisnik nije pronađen! (Tražen username: " + username + ")"));

        if (!Files.exists(rootLocation)) {
            Files.createDirectories(rootLocation);
        }

        String videoFileName = "vid_" + UUID.randomUUID() + "_" + videoFile.getOriginalFilename();
        String thumbFileName = "img_" + UUID.randomUUID() + "_" + thumbnailFile.getOriginalFilename();

        Files.copy(videoFile.getInputStream(), this.rootLocation.resolve(videoFileName));
        Files.copy(thumbnailFile.getInputStream(), this.rootLocation.resolve(thumbFileName));

        Double finalLat = 0.0;
        Double finalLon = 0.0;

        Double[] coords = getCoordinatesFromAddress(street, number, city);

        if (coords != null) {
            finalLat = coords[0];
            finalLon = coords[1];
        } else {
            System.out.println("Upozorenje: Nije moguće pronaći koordinate za datu adresu.");
        }

        Video video = new Video();
        video.setTitle(title);
        video.setDescription(description);
        video.setVideoUrl(videoFileName);
        video.setThumbnailUrl(thumbFileName);
        video.setOwner(owner);
        video.setUploadedAt(LocalDateTime.now());
        video.setViewCount(0L);
        video.setDuration(duration);
        video.setLatitude(finalLat);
        video.setLongitude(finalLon);
        video.setLocation(street + " " + number + ", " + city);

        Video savedVideo = videoRepository.save(video);

        if (finalLat != 0.0 && finalLon != 0.0) {
            updateMapCache(finalLat, finalLon);
        }

        return new VideoDTO(savedVideo);
    }

    private void updateMapCache(Double lat, Double lon) {
        try {
            for (int z = 1; z <= 18; z++) {
                int x = tileService.getTileX(lon, z);
                int y = tileService.getTileY(lat, z);

                String cacheKey = z + "-" + x + "-" + y;

                // Invalidacija standardnog tile cache-a
                if (cacheManager.getCache("mapTiles") != null) {
                    cacheManager.getCache("mapTiles").evict(cacheKey);
                }

                // Invalidacija klasteriranog tile cache-a
                if (cacheManager.getCache("mapTilesClustered") != null) {
                    cacheManager.getCache("mapTilesClustered").evict(cacheKey);
                }
            }
            System.out.println("CACHE: Obrisani tile-ovi za novu lokaciju videa.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Cacheable("thumbnails")
    public byte[] getThumbnail(String filename) throws IOException {
        Path destination = this.rootLocation.resolve(filename);
        return Files.readAllBytes(destination);
    }

    public List<VideoDTO> getVideosInView(Double minLat, Double maxLat, Double minLng, Double maxLng) {
        if (minLat == null || maxLat == null || minLng == null || maxLng == null) {
            return new ArrayList<>();
        }

        List<Video> videos = videoRepository.findByLatitudeBetweenAndLongitudeBetween(minLat, maxLat, minLng, maxLng);

        return videos.stream()
                .map(VideoDTO::new)
                .collect(Collectors.toList());
    }

    @Cacheable(value = "mapTiles", key = "#z + '-' + #x + '-' + #y")
    public List<VideoDTO> getVideosByTile(int z, int x, int y) {
        System.out.println("Podaci iz baze za tile " + z + "/" + x + "/" + y);
        TileService.BoundingBox box = tileService.getBoundingBox(x, y, z);

        List<Video> videos = videoRepository.findByLatitudeBetweenAndLongitudeBetween(
                box.minLat, box.maxLat, box.minLng, box.maxLng
        );

        return videos.stream()
                .map(VideoDTO::new)
                .collect(Collectors.toList());
    }

    /**
     * Vraca klasterizirane video snimke za dati tile.
     * Frontend salje efektivni zoom nivo, pa backend samo treba da vrati:
     * - HIGH zoom (>=12): Svi pojedinacni video snimci
     * - MEDIUM/LOW zoom (<12): Jedan klaster po tile-u sa reprezentativnim videom
     */
    @Cacheable(value = "mapTilesClustered", key = "#z + '-' + #x + '-' + #y")
    public List<TileClusterDTO> getClusteredVideosByTile(int z, int x, int y) {
        String zoomLevel = tileService.getZoomLevel(z);
        System.out.println("Clustered tile " + z + "/" + x + "/" + y + " - Zoom level: " + zoomLevel);

        TileService.BoundingBox box = tileService.getBoundingBox(x, y, z);

        List<Video> videos = videoRepository.findByLatitudeBetweenAndLongitudeBetween(
                box.minLat, box.maxLat, box.minLng, box.maxLng
        );

        if (videos.isEmpty()) {
            return new ArrayList<>();
        }

        if ("HIGH".equals(zoomLevel)) {
            // Visoki zoom - vrati sve video snimke kao pojedinacne "klastere"
            return videos.stream()
                    .map(video -> new TileClusterDTO(
                            video.getLatitude(),
                            video.getLongitude(),
                            1,
                            new VideoDTO(video),
                            x, y, z
                    ))
                    .collect(Collectors.toList());
        } else {
            // Srednji i niski zoom - vrati jedan klaster za ceo tile
            // Pronadji reprezentativni video (najvise pregleda)
            Video representative = videos.stream()
                    .max((v1, v2) -> Long.compare(
                            v1.getViewCount() != null ? v1.getViewCount() : 0L,
                            v2.getViewCount() != null ? v2.getViewCount() : 0L
                    ))
                    .orElse(videos.get(0));

            // Koristi koordinate reprezentativnog videa, NE centar tile-a
            // Tako klaster ostaje na vidljivoj lokaciji videa
            TileClusterDTO cluster = new TileClusterDTO(
                    representative.getLatitude(),
                    representative.getLongitude(),
                    videos.size(),
                    new VideoDTO(representative),
                    x, y, z
            );

            return List.of(cluster);
        }
    }

    /**
     * Vraca klasterizirane video snimke za viewport.
     * Svi videi u viewport-u se uvijek prikazu - pojedinacno ili kao klasteri.
     */
    public List<TileClusterDTO> getClusteredVideosByViewport(
            Double minLat, Double maxLat, Double minLng, Double maxLng, int zoom, String filter) {

        System.out.println("Viewport clustered: zoom=" + zoom + ", filter=" + filter + ", bounds=[" +
                minLat + "," + maxLat + "," + minLng + "," + maxLng + "]");

        // Ucitaj videe u viewport-u sa primijenjenim filterom
        List<Video> allVideos;

        if ("LAST_30_DAYS".equalsIgnoreCase(filter)) {
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);
            allVideos = videoRepository.findByLatitudeBetweenAndLongitudeBetweenAndUploadedAtAfter(
                    minLat, maxLat, minLng, maxLng, cutoffDate);
        } else if ("THIS_YEAR".equalsIgnoreCase(filter)) {
            LocalDateTime cutoffDate = LocalDateTime.now().withDayOfYear(1).toLocalDate().atStartOfDay();
            allVideos = videoRepository.findByLatitudeBetweenAndLongitudeBetweenAndUploadedAtAfter(
                    minLat, maxLat, minLng, maxLng, cutoffDate);
        } else {
            allVideos = videoRepository.findByLatitudeBetweenAndLongitudeBetween(
                    minLat, maxLat, minLng, maxLng);
        }

        if (allVideos.isEmpty()) {
            return new ArrayList<>();
        }

        String zoomLevel = tileService.getZoomLevel(zoom);

        if ("HIGH".equals(zoomLevel)) {
            // Visoki zoom - svaki video je svoj klaster
            return allVideos.stream()
                    .map(video -> new TileClusterDTO(
                            video.getLatitude(),
                            video.getLongitude(),
                            1,
                            new VideoDTO(video),
                            0, 0, zoom
                    ))
                    .collect(Collectors.toList());
        } else {
            // Srednji/niski zoom - grupisanje po tile-ovima
            int effectiveZoom = tileService.getEffectiveZoom(zoom);

            // Grupisanje videa po tile koordinatama na efektivnom zoom-u
            Map<String, List<Video>> groupedByTile = new HashMap<>();

            for (Video video : allVideos) {
                int tileX = tileService.getTileX(video.getLongitude(), effectiveZoom);
                int tileY = tileService.getTileY(video.getLatitude(), effectiveZoom);
                String key = tileX + "-" + tileY;

                groupedByTile.computeIfAbsent(key, k -> new ArrayList<>()).add(video);
            }

            // Kreiraj klaster za svaku grupu
            List<TileClusterDTO> clusters = new ArrayList<>();

            for (Map.Entry<String, List<Video>> entry : groupedByTile.entrySet()) {
                List<Video> videosInTile = entry.getValue();
                String[] coords = entry.getKey().split("-");
                int tileX = Integer.parseInt(coords[0]);
                int tileY = Integer.parseInt(coords[1]);

                // Reprezentativni video - najvise pregleda
                Video representative = videosInTile.stream()
                        .max((v1, v2) -> Long.compare(
                                v1.getViewCount() != null ? v1.getViewCount() : 0L,
                                v2.getViewCount() != null ? v2.getViewCount() : 0L
                        ))
                        .orElse(videosInTile.get(0));

                // Klaster na lokaciji reprezentativnog videa
                TileClusterDTO cluster = new TileClusterDTO(
                        representative.getLatitude(),
                        representative.getLongitude(),
                        videosInTile.size(),
                        new VideoDTO(representative),
                        tileX, tileY, effectiveZoom
                );

                clusters.add(cluster);
            }

            return clusters;
        }
    }

    private Double[] getCoordinatesFromAddress(String street, String number, String city) {
        try {
            String addressQuery = street + " " + number + ", " + city;
            String encodedAddress = URLEncoder.encode(addressQuery, StandardCharsets.UTF_8);
            String url = "https://nominatim.openstreetmap.org/search?q=" + encodedAddress + "&format=json&limit=1";

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "ISABackendProjekat/1.0")
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootArray = mapper.readTree(response.body());

            if (rootArray.isArray() && !rootArray.isEmpty()) {
                JsonNode firstResult = rootArray.get(0);
                Double lat = firstResult.get("lat").asDouble();
                Double lon = firstResult.get("lon").asDouble();
                return new Double[]{lat, lon};
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
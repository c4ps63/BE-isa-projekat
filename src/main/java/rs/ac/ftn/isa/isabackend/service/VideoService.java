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
import rs.ac.ftn.isa.isabackend.model.TranscodingStatus;
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

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;
import rs.ac.ftn.isa.isabackend.dto.TileClusterDTO;

import rs.ac.ftn.isa.isabackend.repository.VideoViewRepository;
import rs.ac.ftn.isa.isabackend.model.VideoView;
import rs.ac.ftn.isa.isabackend.dto.UploadEvent;

@Service
public class VideoService {

    private final VideoRepository videoRepository;
    private final UserRepository userRepository;
    private final TileService tileService;
    private final TranscodingProducer transcodingProducer;
    private final UploadEventProducer uploadEventProducer;
    private final VideoViewRepository videoViewRepository;
    private final Path rootLocation = Paths.get("uploads");


    @Autowired
    private CacheManager cacheManager;

    @Autowired
    public VideoService(VideoRepository videoRepository,
                      UserRepository userRepository,
                      TileService tileService,
                      CacheManager cacheManager,
                      TranscodingProducer transcodingProducer,
                      UploadEventProducer uploadEventProducer,
                      VideoViewRepository videoViewRepository) {
        this.videoRepository = videoRepository;
        this.userRepository = userRepository;
        this.tileService = tileService;
        this.cacheManager = cacheManager;
        this.transcodingProducer = transcodingProducer;
        this.uploadEventProducer = uploadEventProducer;
        this.videoViewRepository = videoViewRepository;
    }

    public Page<Video> findAll(int page, int size, String filter) {
        Pageable pageable = PageRequest.of(page, size);
        LocalDateTime now = LocalDateTime.now();

        if ("LAST_30_DAYS".equalsIgnoreCase(filter)) {
            LocalDateTime cutoffDate = now.minusDays(30);
            return videoRepository.findAvailableVideosAfterDate(now, cutoffDate, pageable);
        } else if ("THIS_YEAR".equalsIgnoreCase(filter)) {
            LocalDateTime cutoffDate = now.withDayOfYear(1).toLocalDate().atStartOfDay();
            return videoRepository.findAvailableVideosAfterDate(now, cutoffDate, pageable);
        } else {
            return videoRepository.findAvailableVideos(now, pageable);
        }
    }

    public Optional<Video> findById(Long id) {
        return videoRepository.findById(id);
    }

    public Long slowQueryForLoadTest() {
        return videoRepository.slowCountForLoadTest();
    }

    public VideoDTO getVideoForPlayback(Long id) {
        Video video = videoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Video not found"));

        VideoDTO dto = new VideoDTO(video);

        if (Boolean.FALSE.equals(video.getIsScheduled()) || video.getScheduledDateTime() == null) {
            dto.setStreamingStatus("VOD");
            dto.setCurrentOffset(0L);
            return dto;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = video.getScheduledDateTime();

        if (now.isBefore(start)) {
            dto.setStreamingStatus("WAITING");
            dto.setCurrentOffset(0L);
            return dto;
        }

        long secondsSinceStart = ChronoUnit.SECONDS.between(start, now);

        if (video.getDuration() != null && video.getDuration() > 0 && secondsSinceStart > video.getDuration()) {
            dto.setStreamingStatus("VOD");
            dto.setCurrentOffset(0L);
        } else {
            // 4. Video je LIVE
            dto.setStreamingStatus("LIVE");
            dto.setCurrentOffset(secondsSinceStart);
        }

        return dto;
    }

    public Page<Video> findByOwnerId(Long ownerId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return videoRepository.findByOwnerIdOrderByUploadedAtDesc(ownerId, pageable);
    }

        @Transactional
        public void incrementViewCount(Long videoId) {
            videoRepository.incrementViewCount(videoId);

            Video video = videoRepository.findById(videoId)
                    .orElseThrow(() -> new RuntimeException("Video not found for view count increment"));

            VideoView view = new VideoView(video);
            videoViewRepository.save(view);
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
    public VideoDTO uploadVideoWithUser(String title, String description, MultipartFile videoFile,
                                        MultipartFile thumbnailFile, String username, Integer duration,
                                        String street, String number, String city,
                                        Boolean isScheduled, LocalDateTime scheduledTime) throws IOException {

        User owner = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Korisnik nije pronađen! (Tražen username: " + username + ")"));

        if (!Files.exists(rootLocation)) {
            Files.createDirectories(rootLocation);
        }

        String videoExt = getFileExtension(videoFile.getOriginalFilename());
        String thumbExt = getFileExtension(thumbnailFile.getOriginalFilename());
        String videoFileName = "vid_" + UUID.randomUUID() + videoExt;
        String thumbFileName = "img_" + UUID.randomUUID() + thumbExt;

        Files.copy(videoFile.getInputStream(), this.rootLocation.resolve(videoFileName));
        Files.copy(thumbnailFile.getInputStream(), this.rootLocation.resolve(thumbFileName));

        Double finalLat = 0.0;
        Double finalLon = 0.0;

        Double[] coords = getCoordinatesFromAddress(street, number, city);

        if (coords != null) {
            finalLat = coords[0];
            finalLon = coords[1];
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
        video.setTranscodingStatus(TranscodingStatus.PENDING);

        video.setIsScheduled(isScheduled != null ? isScheduled : false);
        video.setScheduledDateTime(scheduledTime);

        Video savedVideo = videoRepository.save(video);

        if (finalLat != 0.0 && finalLon != 0.0) {
            updateMapCache(finalLat, finalLon);
        }

        // Slanje u red poruka za transcoding NAKON sto se transakcija commituje
        // (inace consumer ne moze naci video u bazi jer transakcija jos traje)
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    transcodingProducer.sendForTranscoding(savedVideo);
                } catch (Exception e) {
                    System.err.println("TRANSCODING: Greska pri slanju u queue: " + e.getMessage());
                }

                try {
                    UploadEvent uploadEvent = new UploadEvent(
                            savedVideo.getId(),
                            savedVideo.getTitle(),
                            savedVideo.getVideoUrl(),
                            videoFile.getSize(),
                            username,
                            savedVideo.getUploadedAt().toString(),
                            getFileExtension(savedVideo.getVideoUrl()),
                            savedVideo.getLocation(),
                            savedVideo.getLatitude(),
                            savedVideo.getLongitude(),
                            savedVideo.getDuration() != null ? savedVideo.getDuration() : 0,
                            savedVideo.getDescription()
                    );
                    uploadEventProducer.sendUploadEvent(uploadEvent);
                } catch (Exception e) {
                    System.err.println("UPLOAD-EVENT: Greska pri slanju u queue: " + e.getMessage());
                }
            }
        });

        return new VideoDTO(savedVideo);
    }

    private void updateMapCache(Double lat, Double lon) {
        try {
            for (int z = 1; z <= 18; z++) {
                int x = tileService.getTileX(lon, z);
                int y = tileService.getTileY(lat, z);

                String cacheKey = z + "-" + x + "-" + y;

                if (cacheManager.getCache("mapTiles") != null) {
                    cacheManager.getCache("mapTiles").evict(cacheKey);
                }
                if (cacheManager.getCache("mapTilesClustered") != null) {
                    cacheManager.getCache("mapTilesClustered").evict(cacheKey);
                }
            }
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
        return videos.stream().map(VideoDTO::new).collect(Collectors.toList());
    }

    @Cacheable(value = "mapTiles", key = "#z + '-' + #x + '-' + #y")
    public List<VideoDTO> getVideosByTile(int z, int x, int y) {
        TileService.BoundingBox box = tileService.getBoundingBox(x, y, z);
        List<Video> videos = videoRepository.findByLatitudeBetweenAndLongitudeBetween(
                box.minLat, box.maxLat, box.minLng, box.maxLng
        );
        return videos.stream().map(VideoDTO::new).collect(Collectors.toList());
    }

    @Cacheable(value = "mapTilesClustered", key = "#z + '-' + #x + '-' + #y")
    public List<TileClusterDTO> getClusteredVideosByTile(int z, int x, int y) {
        String zoomLevel = tileService.getZoomLevel(z);
        TileService.BoundingBox box = tileService.getBoundingBox(x, y, z);
        List<Video> videos = videoRepository.findByLatitudeBetweenAndLongitudeBetween(
                box.minLat, box.maxLat, box.minLng, box.maxLng
        );

        if (videos.isEmpty()) return new ArrayList<>();

        if ("HIGH".equals(zoomLevel)) {
            return videos.stream()
                    .map(video -> new TileClusterDTO(
                            video.getLatitude(), video.getLongitude(), 1, new VideoDTO(video), x, y, z
                    )).collect(Collectors.toList());
        } else {
            Video representative = videos.stream()
                    .max((v1, v2) -> Long.compare(
                            v1.getViewCount() != null ? v1.getViewCount() : 0L,
                            v2.getViewCount() != null ? v2.getViewCount() : 0L
                    )).orElse(videos.get(0));

            return List.of(new TileClusterDTO(
                    representative.getLatitude(), representative.getLongitude(), videos.size(),
                    new VideoDTO(representative), x, y, z
            ));
        }
    }

    public List<TileClusterDTO> getClusteredVideosByViewport(
            Double minLat, Double maxLat, Double minLng, Double maxLng, int zoom, String filter) {
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
            allVideos = videoRepository.findByLatitudeBetweenAndLongitudeBetween(minLat, maxLat, minLng, maxLng);
        }

        if (allVideos.isEmpty()) return new ArrayList<>();

        String zoomLevel = tileService.getZoomLevel(zoom);

        if ("HIGH".equals(zoomLevel)) {
            return allVideos.stream()
                    .map(video -> new TileClusterDTO(
                            video.getLatitude(), video.getLongitude(), 1, new VideoDTO(video), 0, 0, zoom
                    )).collect(Collectors.toList());
        } else {
            int effectiveZoom = tileService.getEffectiveZoom(zoom);
            Map<String, List<Video>> groupedByTile = new HashMap<>();

            for (Video video : allVideos) {
                int tileX = tileService.getTileX(video.getLongitude(), effectiveZoom);
                int tileY = tileService.getTileY(video.getLatitude(), effectiveZoom);
                String key = tileX + "-" + tileY;
                groupedByTile.computeIfAbsent(key, k -> new ArrayList<>()).add(video);
            }

            List<TileClusterDTO> clusters = new ArrayList<>();
            for (Map.Entry<String, List<Video>> entry : groupedByTile.entrySet()) {
                List<Video> videosInTile = entry.getValue();
                String[] coords = entry.getKey().split("-");
                int tileX = Integer.parseInt(coords[0]);
                int tileY = Integer.parseInt(coords[1]);

                Video representative = videosInTile.stream()
                        .max((v1, v2) -> Long.compare(
                                v1.getViewCount() != null ? v1.getViewCount() : 0L,
                                v2.getViewCount() != null ? v2.getViewCount() : 0L
                        )).orElse(videosInTile.get(0));

                clusters.add(new TileClusterDTO(
                        representative.getLatitude(), representative.getLongitude(), videosInTile.size(),
                        new VideoDTO(representative), tileX, tileY, effectiveZoom
                ));
            }
            return clusters;
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null) return "";
        int dotIndex = filename.lastIndexOf('.');
        return (dotIndex >= 0) ? filename.substring(dotIndex) : "";
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
                    .GET().build();

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
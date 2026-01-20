package rs.ac.ftn.isa.isabackend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

@Service
public class VideoService {

    private final VideoRepository videoRepository;
    private final UserRepository userRepository;
    private final TileService tileService;
    private final Path rootLocation = Paths.get("uploads");

    @Autowired
    public VideoService(VideoRepository videoRepository, UserRepository userRepository, TileService tileService) {
        this.videoRepository = videoRepository;
        this.userRepository = userRepository;
        this.tileService = tileService;
    }

    public Page<Video> findAll(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return videoRepository.findAllByOrderByUploadedAtDesc(pageable);
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
    public VideoDTO uploadVideoWithUser(String title, String description, MultipartFile videoFile, MultipartFile thumbnailFile, String username, Integer duration, Double latitude, Double longitude) throws IOException {

        User owner = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Korisnik nije pronađen! (Tražen username: " + username + ")"));

        if (!Files.exists(rootLocation)) {
            Files.createDirectories(rootLocation);
        }

        String videoFileName = "vid_" + UUID.randomUUID() + "_" + videoFile.getOriginalFilename();
        String thumbFileName = "img_" + UUID.randomUUID() + "_" + thumbnailFile.getOriginalFilename();

        Files.copy(videoFile.getInputStream(), this.rootLocation.resolve(videoFileName));
        Files.copy(thumbnailFile.getInputStream(), this.rootLocation.resolve(thumbFileName));

        Video video = new Video();
        video.setTitle(title);
        video.setDescription(description);
        video.setVideoUrl(videoFileName);
        video.setThumbnailUrl(thumbFileName);
        video.setOwner(owner);
        video.setUploadedAt(LocalDateTime.now());
        video.setViewCount(0L);
        video.setDuration(duration);
        video.setLatitude(latitude);
        video.setLongitude(longitude);

        Video savedVideo = videoRepository.save(video);

        return new VideoDTO(savedVideo);
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

    public List<VideoDTO> getVideosByTile(int z, int x, int y) {
        TileService.BoundingBox box = tileService.getBoundingBox(x, y, z);

        List<Video> videos = videoRepository.findByLatitudeBetweenAndLongitudeBetween(
                box.minLat, box.maxLat, box.minLng, box.maxLng
        );

        return videos.stream()
                .map(VideoDTO::new)
                .collect(Collectors.toList());
    }
}
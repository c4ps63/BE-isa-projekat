package rs.ac.ftn.isa.isabackend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import rs.ac.ftn.isa.isabackend.model.User;
import rs.ac.ftn.isa.isabackend.model.Video;
import rs.ac.ftn.isa.isabackend.repository.UserRepository;
import rs.ac.ftn.isa.isabackend.repository.VideoRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class VideoService {

    private final VideoRepository videoRepository;
    private final UserRepository userRepository;
    private final Path fileStorageLocation;

    @Autowired
    public VideoService(VideoRepository videoRepository, UserRepository userRepository, @Value("${file.upload-dir}") String uploadDir) {
        this.videoRepository = videoRepository;
        this.userRepository = userRepository;
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("Ne mogu da kreiram folder za upload fajlova.", ex);
        }
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

    @Transactional
    public void incrementViewCount(Long videoId) {
        videoRepository.incrementViewCount(videoId);
    }

    @Transactional(rollbackFor = Exception.class)
    public Video uploadVideo(String title, String description, Set<String> tags,String location, MultipartFile videoFile, MultipartFile thumbnailFile, Long userId) throws IOException {
        User owner = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("Korisnik nije pronađen"));

        String videoFileName = "vid_" + UUID.randomUUID() + "_" + videoFile.getOriginalFilename();
        String thumbFileName = "img_" + UUID.randomUUID() + "_" + thumbnailFile.getOriginalFilename();

        Path videoTargetLocation = this.fileStorageLocation.resolve(videoFileName);
        Path thumbTargetLocation = this.fileStorageLocation.resolve(thumbFileName);

        try {
            Files.copy(videoFile.getInputStream(), videoTargetLocation, StandardCopyOption.REPLACE_EXISTING);
            Files.copy(thumbnailFile.getInputStream(), thumbTargetLocation, StandardCopyOption.REPLACE_EXISTING);

            Video video = new Video();
            video.setTitle(title);
            video.setDescription(description);
            video.setTags(tags);
            video.setLocation(location);
            video.setVideoUrl(videoFileName);
            video.setThumbnailUrl(thumbFileName);
            video.setUploadedAt(LocalDateTime.now());
            video.setOwner(owner);
            video.setDuration(0);

            return videoRepository.save(video);

        } catch (Exception e) {
            Files.deleteIfExists(videoTargetLocation);
            Files.deleteIfExists(thumbTargetLocation);
            throw new RuntimeException("Neuspešan upload, izvršen rollback.", e);
        }
    }
    @Cacheable(value = "thumbnails", key = "#fileName")
    public byte[] getThumbnailBytes(String fileName) throws IOException {
        Path filePath = this.fileStorageLocation.resolve(fileName).normalize();
        return Files.readAllBytes(filePath);
    }
}
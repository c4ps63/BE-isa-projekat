package rs.ac.ftn.isa.isabackend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import rs.ac.ftn.isa.isabackend.dto.VideoDTO;
import rs.ac.ftn.isa.isabackend.model.Video;
import rs.ac.ftn.isa.isabackend.service.VideoService;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;

@RestController
@RequestMapping("/api/videos")
@CrossOrigin(origins = "http://localhost:4200")
public class VideoController {

    private final VideoService videoService;

    @Autowired
    public VideoController(VideoService videoService) {
        this.videoService = videoService;
    }

    @GetMapping
    public ResponseEntity<Page<VideoDTO>> getAllVideos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<Video> videos = videoService.findAll(page, size);
        Page<VideoDTO> videoDTOs = videos.map(VideoDTO::new);
        return ResponseEntity.ok(videoDTOs);
    }

    @GetMapping("/{id}")
    public ResponseEntity<VideoDTO> getVideoById(@PathVariable Long id) {
        Optional<Video> video = videoService.findById(id);

        if (video.isPresent()) {
            // Uvećaj broj pregleda
            videoService.incrementViewCount(id);
            return ResponseEntity.ok(new VideoDTO(video.get()));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<VideoDTO>> getVideosByUserId(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<Video> videos = videoService.findByOwnerId(userId, page, size);
        Page<VideoDTO> videoDTOs = videos.map(VideoDTO::new);
        return ResponseEntity.ok(videoDTOs);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadVideo(
            @RequestParam("title") String title,
            @RequestParam("description") String description,
            @RequestParam("tags") Set<String> tags,
            @RequestParam(value = "location", required = false) String location,
            @RequestParam("videoFile") MultipartFile videoFile,
            @RequestParam("thumbnailFile") MultipartFile thumbnailFile,
            @RequestParam("userId") Long userId
    ) {
        try {
            if (videoFile.isEmpty() || thumbnailFile.isEmpty()) {
                return ResponseEntity.badRequest().body("Video i thumbnail su obavezni.");
            }

            Video savedVideo = videoService.uploadVideo(title, description, tags, location, videoFile, thumbnailFile, userId);
            return ResponseEntity.ok(new VideoDTO(savedVideo));

        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Greška pri čitanju fajla: " + e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.internalServerError().body("Greška: " + e.getMessage());
        }
    }

    @GetMapping(value = "/thumbnail/{fileName}", produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<byte[]> getThumbnail(@PathVariable String fileName) {
        try {
            byte[] imageBytes = videoService.getThumbnailBytes(fileName);
            return ResponseEntity.ok(imageBytes);
        } catch (IOException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping(value = "/stream/{fileName}", produces = "video/mp4")
    public ResponseEntity<byte[]> streamVideo(@PathVariable String fileName) {
        try {
            byte[] videoBytes = videoService.getThumbnailBytes(fileName);
            return ResponseEntity.ok(videoBytes);
        } catch (IOException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // TODO: POST, PUT, DELETE - za autentifikovane korisnike (dodajemo kasnije sa Spring Security)
}
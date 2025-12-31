package rs.ac.ftn.isa.isabackend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rs.ac.ftn.isa.isabackend.dto.VideoDTO;
import rs.ac.ftn.isa.isabackend.model.Video;
import rs.ac.ftn.isa.isabackend.service.VideoService;

import java.util.Optional;

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

    // TODO: POST, PUT, DELETE - za autentifikovane korisnike (dodajemo kasnije sa Spring Security)
}
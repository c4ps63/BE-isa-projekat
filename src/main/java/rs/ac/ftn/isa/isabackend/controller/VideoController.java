package rs.ac.ftn.isa.isabackend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import rs.ac.ftn.isa.isabackend.dto.VideoDTO;
import rs.ac.ftn.isa.isabackend.model.Video;
import rs.ac.ftn.isa.isabackend.service.VideoService;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;
import java.security.Principal;

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

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<?> uploadVideo(
            @RequestParam("title") String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "location", required = false) String location,
            @RequestParam(value = "tags", required = false) String tags,
            @RequestParam("videoFile") MultipartFile videoFile,
            @RequestParam("thumbnailFile") MultipartFile thumbnailFile,
            Principal principal
    ) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Korisnik nije ulogovan.");
        }
        try {
            if (videoFile.getSize() > 200 * 1024 * 1024) {
                return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body("Video je prevelik (max 200MB)");
            }

            String username = principal.getName();
            VideoDTO savedVideo = videoService.uploadVideoWithUser(title, description, videoFile, thumbnailFile, username);

            return ResponseEntity.status(HttpStatus.CREATED).body(savedVideo);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Greška pri čuvanju fajla: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
    // TODO: POST, PUT, DELETE - za autentifikovane korisnike (dodajemo kasnije sa Spring Security)
}
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
import rs.ac.ftn.isa.isabackend.dto.TileClusterDTO;
import rs.ac.ftn.isa.isabackend.model.Video;
import rs.ac.ftn.isa.isabackend.service.VideoService;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.Set;
import java.security.Principal;
import java.util.List;

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
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "ALL") String filter) {

        Page<Video> videos = videoService.findAll(page, size, filter);
        Page<VideoDTO> videoDTOs = videos.map(VideoDTO::new);
        return ResponseEntity.ok(videoDTOs);
    }

    @GetMapping("/{id}")
    public ResponseEntity<VideoDTO> getVideoById(@PathVariable Long id) {
        try {
            VideoDTO videoDTO = videoService.getVideoForPlayback(id);
            return ResponseEntity.ok(videoDTO);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/view")
    public ResponseEntity<Void> registerView(@PathVariable Long id) {
        try {
            videoService.incrementViewCount(id);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<VideoDTO>> getVideosByUser(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {

        Page<Video> videos = videoService.findByOwnerId(userId, page, size);
        Page<VideoDTO> videoDTOs = videos.map(VideoDTO::new);
        return ResponseEntity.ok(videoDTOs);
    }

    @GetMapping("/load-test")
    public ResponseEntity<Long> loadTest() {
        Long count = videoService.slowQueryForLoadTest();
        return ResponseEntity.ok(count);
    }

    @GetMapping("/viewport")
    public ResponseEntity<List<VideoDTO>> getVideosByViewport(
            @RequestParam Double minLat,
            @RequestParam Double maxLat,
            @RequestParam Double minLng,
            @RequestParam Double maxLng) {

        return ResponseEntity.ok(videoService.getVideosInView(minLat, maxLat, minLng, maxLng));
    }

    @GetMapping("/tile/{z}/{x}/{y}")
    public ResponseEntity<List<VideoDTO>> getVideosByTile(
            @PathVariable int z,
            @PathVariable int x,
            @PathVariable int y) {

        return ResponseEntity.ok(videoService.getVideosByTile(z, x, y));
    }

    /**
     * Vraca klasterizirane video snimke za prikaz na mapi.
     * Na visokom zoom-u vraca pojedinacne video snimke,
     * na nizem zoom-u vraca grupisane klastere sa reprezentativnim videom.
     */
    @GetMapping("/tile-clustered/{z}/{x}/{y}")
    public ResponseEntity<List<TileClusterDTO>> getClusteredVideosByTile(
            @PathVariable int z,
            @PathVariable int x,
            @PathVariable int y) {

        return ResponseEntity.ok(videoService.getClusteredVideosByTile(z, x, y));
    }

    /**
     * Vraca klasterizirane video snimke za dati viewport.
     * Garantuje da se svi videi u viewport-u prikazu - pojedinacno ili kao klasteri.
     */
    @GetMapping("/viewport-clustered")
    public ResponseEntity<List<TileClusterDTO>> getClusteredVideosByViewport(
            @RequestParam Double minLat,
            @RequestParam Double maxLat,
            @RequestParam Double minLng,
            @RequestParam Double maxLng,
            @RequestParam int zoom,
            @RequestParam(defaultValue = "ALL") String filter) {

        return ResponseEntity.ok(videoService.getClusteredVideosByViewport(minLat, maxLat, minLng, maxLng, zoom, filter));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<?> uploadVideo(
            @RequestParam("title") String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam("street") String street,
            @RequestParam("number") String number,
            @RequestParam("city") String city,
            @RequestParam(value = "tags", required = false) String tags,
            @RequestParam("duration") Integer duration,

            // NOVI PARAMETRI
            @RequestParam(value = "isScheduled", required = false, defaultValue = "false") Boolean isScheduled,
            @RequestParam(value = "scheduledTime", required = false) String scheduledTimeStr,

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

            // Parsiranje datuma
            LocalDateTime scheduledDateTime = null;
            if (Boolean.TRUE.equals(isScheduled) && scheduledTimeStr != null && !scheduledTimeStr.isEmpty()) {
                // Front salje ISO string (2026-02-15T20:00:00)
                scheduledDateTime = LocalDateTime.parse(scheduledTimeStr, DateTimeFormatter.ISO_DATE_TIME);
            }

            VideoDTO savedVideo = videoService.uploadVideoWithUser(
                    title, description, videoFile, thumbnailFile, username, duration,
                    street, number, city, isScheduled, scheduledDateTime
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(savedVideo);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Greška pri čuvanju fajla: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
}
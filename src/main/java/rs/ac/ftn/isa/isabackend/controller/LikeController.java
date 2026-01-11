package rs.ac.ftn.isa.isabackend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import rs.ac.ftn.isa.isabackend.model.Like;
import rs.ac.ftn.isa.isabackend.model.User;
import rs.ac.ftn.isa.isabackend.model.Video;
import rs.ac.ftn.isa.isabackend.repository.VideoRepository;
import rs.ac.ftn.isa.isabackend.security.auth.TokenBasedAuthentication;
import rs.ac.ftn.isa.isabackend.service.LikeService;

@RestController
@RequestMapping("/api/likes")
@CrossOrigin(origins = "http://localhost:4200")
public class LikeController {

    @Autowired
    private LikeService likeService;

    @Autowired
    private VideoRepository videoRepository;

    @PostMapping("/toggle/{videoId}")
    public ResponseEntity<?> toggleLike(@PathVariable Long videoId) {
        try {
            TokenBasedAuthentication auth = (TokenBasedAuthentication) SecurityContextHolder.getContext().getAuthentication();
            User user = (User) auth.getPrincipal();

            Video video = videoRepository.findById(videoId)
                    .orElseThrow(() -> new RuntimeException("Video nije pronađen"));

            Like result = likeService.toggleLike(user, video);

            if (result == null) {
                return ResponseEntity.ok().body(new LikeResponseDto(false, "Like je uklonjen"));
            } else {
                return ResponseEntity.ok().body(new LikeResponseDto(true, "Video je lajkovan"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorDto(e.getMessage()));
        }
    }

    @GetMapping("/is-liked/{videoId}")
    public ResponseEntity<?> isVideoLikedByUser(@PathVariable Long videoId) {
        try {
            TokenBasedAuthentication auth = (TokenBasedAuthentication) SecurityContextHolder.getContext().getAuthentication();
            User user = (User) auth.getPrincipal();

            boolean isLiked = likeService.isLikedByUser(user.getId(), videoId);
            return ResponseEntity.ok(isLiked);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorDto(e.getMessage()));
        }
    }

    @GetMapping("/count/{videoId}")
    public ResponseEntity<?> getLikeCount(@PathVariable Long videoId) {
        try {
            Video video = videoRepository.findById(videoId)
                    .orElseThrow(() -> new RuntimeException("Video nije pronađen"));

            long likeCount = video.getLikeCount();
            return ResponseEntity.ok(likeCount);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorDto(e.getMessage()));
        }
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteLike(@PathVariable Long id) {
        try {
            likeService.deleteById(id);
            return ResponseEntity.ok().body(new MessageDto("Like je obrisan"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorDto(e.getMessage()));
        }
    }

    static class LikeResponseDto {
        public boolean liked;
        public String message;

        public LikeResponseDto(boolean liked, String message) {
            this.liked = liked;
            this.message = message;
        }
    }

    static class ErrorDto {
        public String error;

        public ErrorDto(String error) {
            this.error = error;
        }
    }

    static class MessageDto {
        public String message;

        public MessageDto(String message) {
            this.message = message;
        }
    }
}

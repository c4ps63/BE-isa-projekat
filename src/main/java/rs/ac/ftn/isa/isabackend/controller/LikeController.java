package rs.ac.ftn.isa.isabackend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import rs.ac.ftn.isa.isabackend.model.Like;
import rs.ac.ftn.isa.isabackend.service.LikeService;
import rs.ac.ftn.isa.isabackend.model.Video;
import rs.ac.ftn.isa.isabackend.repository.VideoRepository;
import rs.ac.ftn.isa.isabackend.repository.UserRepository;

import org.springframework.security.access.prepost.PreAuthorize;

import java.util.Map;

@RestController
@RequestMapping("/api/likes")
public class LikeController {

    private final LikeService likeService;
    private final UserRepository userRepository;
    private final VideoRepository videoRepository;

    @Autowired
    public LikeController(LikeService likeService,
                          UserRepository userRepository,
                          VideoRepository videoRepository) {
        this.likeService = likeService;
        this.userRepository = userRepository;
        this.videoRepository = videoRepository;
    }

    @PostMapping("/video/{videoId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Boolean>> toggleLike(@PathVariable Long videoId) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();

        var user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        var video = videoRepository.findById(videoId)
                .orElseThrow(() -> new RuntimeException("Video not found"));

        Like like = likeService.toggleLike(user, video);

        // vraća true ako je sada lajkovano, false ako je unlike
        return ResponseEntity.ok(Map.of("liked", like != null));
    }

    @GetMapping("/video/{videoId}/is-liked")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Boolean>> isLiked(@PathVariable Long videoId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();

        var user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        boolean liked = likeService.isLikedByUser(user.getId(), videoId);

        return ResponseEntity.ok(Map.of("liked", liked));
    }

}

package rs.ac.ftn.isa.isabackend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rs.ac.ftn.isa.isabackend.dto.CommentDTO;
import rs.ac.ftn.isa.isabackend.model.Comment;
import rs.ac.ftn.isa.isabackend.service.CommentService;
import rs.ac.ftn.isa.isabackend.service.CommentRateLimiterService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Map;


@RestController
@RequestMapping("/api/comments")
public class CommentController {

    private final CommentService commentService;
    private final CommentRateLimiterService rateLimiterService;

    @Autowired
    public CommentController(CommentService commentService, CommentRateLimiterService rateLimiterService) {
        this.commentService = commentService;
        this.rateLimiterService = rateLimiterService;
    }

    @GetMapping("/video/{videoId}")
    public ResponseEntity<Page<CommentDTO>> getCommentsByVideoId(
            @PathVariable Long videoId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<Comment> comments = commentService.findByVideoId(videoId, page, size);
        Page<CommentDTO> commentDTOs = comments.map(CommentDTO::new);
        return ResponseEntity.ok(commentDTOs);
    }

    @PostMapping
    public ResponseEntity<?> createComment(@RequestBody CommentDTO commentDTO) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        if (!rateLimiterService.isAllowed(username)) {
            int remaining = rateLimiterService.getRemainingComments(username);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of(
                            "error", "Prekoračili ste limit od 60 komentara po satu.",
                            "remainingComments", remaining
                    ));
        }

        Comment comment = commentService.createComment(commentDTO.getVideoId(), commentDTO.getText(), username);
        return ResponseEntity.ok(new CommentDTO(comment));
    }
}
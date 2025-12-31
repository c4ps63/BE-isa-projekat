package rs.ac.ftn.isa.isabackend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rs.ac.ftn.isa.isabackend.dto.CommentDTO;
import rs.ac.ftn.isa.isabackend.model.Comment;
import rs.ac.ftn.isa.isabackend.service.CommentService;

@RestController
@RequestMapping("/api/comments")
@CrossOrigin(origins = "http://localhost:4200")
public class CommentController {

    private final CommentService commentService;

    @Autowired
    public CommentController(CommentService commentService) {
        this.commentService = commentService;
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

    // TODO: POST, DELETE - za autentifikovane korisnike
}
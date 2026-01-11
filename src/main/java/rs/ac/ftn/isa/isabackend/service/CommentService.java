package rs.ac.ftn.isa.isabackend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.ac.ftn.isa.isabackend.model.Comment;
import rs.ac.ftn.isa.isabackend.repository.CommentRepository;
import rs.ac.ftn.isa.isabackend.model.User;
import rs.ac.ftn.isa.isabackend.model.Video;
import rs.ac.ftn.isa.isabackend.repository.UserRepository;
import rs.ac.ftn.isa.isabackend.repository.VideoRepository;
import java.time.LocalDateTime;


import java.util.Optional;

@Service
public class CommentService {

    private final CommentRepository commentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VideoRepository videoRepository;


    @Autowired
    public CommentService(CommentRepository commentRepository) {
        this.commentRepository = commentRepository;
    }

    @Cacheable(
            value = "commentsByVideo",
            key = "#videoId + '-' + #page + '-' + #size"
    )

    public Page<Comment> findByVideoId(Long videoId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return commentRepository.findByVideoIdOrderByCreatedAtDesc(videoId, pageable);
    }

    public Optional<Comment> findById(Long id) {
        return commentRepository.findById(id);
    }

    @Transactional
    public Comment save(Comment comment) {
        return commentRepository.save(comment);
    }

    @Transactional
    @CacheEvict(
            value = "commentsByVideo",
            allEntries = true
    )
    public Comment createComment(Long videoId, String text, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new RuntimeException("Video not found"));

        Comment comment = new Comment();
        comment.setVideo(video);
        comment.setAuthor(user);
        comment.setText(text);
        comment.setCreatedAt(LocalDateTime.now());

        return commentRepository.save(comment);
    }

    @Transactional
    @CacheEvict(value = "commentsByVideo", allEntries = true)
    public void deleteById(Long id) {
        commentRepository.deleteById(id);
    }

}
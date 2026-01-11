package rs.ac.ftn.isa.isabackend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.ac.ftn.isa.isabackend.model.Like;
import rs.ac.ftn.isa.isabackend.model.User;
import rs.ac.ftn.isa.isabackend.model.Video;
import rs.ac.ftn.isa.isabackend.repository.LikeRepository;

import java.util.Optional;

@Service
public class LikeService {

    private final LikeRepository likeRepository;

    @Autowired
    public LikeService(LikeRepository likeRepository) {
        this.likeRepository = likeRepository;
    }

    public boolean isLikedByUser(Long userId, Long videoId) {
        return likeRepository.existsByUserIdAndVideoId(userId, videoId);
    }

    public Optional<Like> findByUserIdAndVideoId(Long userId, Long videoId) {
        return likeRepository.findByUserIdAndVideoId(userId, videoId);
    }

    @Transactional
    public Like toggleLike(User user, Video video) {
        Optional<Like> existingLike = likeRepository.findByUserIdAndVideoId(user.getId(), video.getId());

        if (existingLike.isPresent()) {
            // Unlike - ukloni like
            likeRepository.delete(existingLike.get());
            return null;
        } else {
            // Like - dodaj novi like
            Like newLike = new Like();
            newLike.setUser(user);
            newLike.setVideo(video);
            return likeRepository.save(newLike);
        }
    }

    @Transactional
    public void deleteById(Long id) {
        likeRepository.deleteById(id);
    }
}
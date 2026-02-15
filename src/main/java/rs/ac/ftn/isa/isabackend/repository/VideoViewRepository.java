package rs.ac.ftn.isa.isabackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import rs.ac.ftn.isa.isabackend.model.VideoView;

import java.time.LocalDateTime;
import java.util.List;

public interface VideoViewRepository extends JpaRepository<VideoView, Long> {
    List<VideoView> findAllByViewedAtAfter(LocalDateTime date);
}
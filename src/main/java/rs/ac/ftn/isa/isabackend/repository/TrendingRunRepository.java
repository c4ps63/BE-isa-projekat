package rs.ac.ftn.isa.isabackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import rs.ac.ftn.isa.isabackend.model.TrendingRun;

import java.util.Optional;

public interface TrendingRunRepository extends JpaRepository<TrendingRun, Long> {
    Optional<TrendingRun> findTopByOrderByRanAtDesc();
}
package rs.ac.ftn.isa.isabackend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import rs.ac.ftn.isa.isabackend.model.Video;

@Repository
public interface VideoRepository extends JpaRepository<Video, Long> {
    Page<Video> findAllByOrderByUploadedAtDesc(Pageable pageable);

    Page<Video> findByOwnerIdOrderByUploadedAtDesc(Long ownerId, Pageable pageable);

    @Modifying
    @Query("UPDATE Video v SET v.viewCount = v.viewCount + 1 WHERE v.id = :videoId")
    void incrementViewCount(@Param("videoId") Long videoId);
}
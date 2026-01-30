package rs.ac.ftn.isa.isabackend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import rs.ac.ftn.isa.isabackend.model.Video;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface VideoRepository extends JpaRepository<Video, Long> {
    Page<Video> findAllByOrderByUploadedAtDesc(Pageable pageable);
    Page<Video> findByOwnerIdOrderByUploadedAtDesc(Long ownerId, Pageable pageable);
    Page<Video> findByUploadedAtAfterOrderByUploadedAtDesc(LocalDateTime date, Pageable pageable);

    @Modifying
    @Query("UPDATE Video v SET v.viewCount = v.viewCount + 1 WHERE v.id = :videoId")
    void incrementViewCount(@Param("videoId") Long videoId);

    List<Video> findByLatitudeBetweenAndLongitudeBetween(Double minLat, Double maxLat, Double minLng, Double maxLng);

    // Pronalazi reprezentativni video (sa najvise pregleda) u datom bounding box-u
    @Query("SELECT v FROM Video v WHERE v.latitude BETWEEN :minLat AND :maxLat " +
           "AND v.longitude BETWEEN :minLng AND :maxLng " +
           "ORDER BY v.viewCount DESC LIMIT 1")
    Optional<Video> findTopByBoundingBoxOrderByViewCountDesc(
            @Param("minLat") Double minLat,
            @Param("maxLat") Double maxLat,
            @Param("minLng") Double minLng,
            @Param("maxLng") Double maxLng);

    // Broji video snimke u datom bounding box-u
    @Query("SELECT COUNT(v) FROM Video v WHERE v.latitude BETWEEN :minLat AND :maxLat " +
           "AND v.longitude BETWEEN :minLng AND :maxLng")
    Long countByBoundingBox(
            @Param("minLat") Double minLat,
            @Param("maxLat") Double maxLat,
            @Param("minLng") Double minLng,
            @Param("maxLng") Double maxLng);
}
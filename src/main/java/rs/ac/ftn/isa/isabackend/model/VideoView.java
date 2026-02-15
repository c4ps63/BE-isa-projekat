package rs.ac.ftn.isa.isabackend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "video_views")
@Getter
@Setter
@NoArgsConstructor
public class VideoView {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id", nullable = false)
    private Video video;



    @Column(nullable = false)
    private LocalDateTime viewedAt;

    public VideoView(Video video) {
        this.video = video;
        this.viewedAt = LocalDateTime.now();
    }
}
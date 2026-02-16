package rs.ac.ftn.isa.isabackend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "trending_items")
@Getter
@Setter
@NoArgsConstructor
public class TrendingItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "run_id", nullable = false)
    private TrendingRun run;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "video_id", nullable = false)
    private Video video;

    private Double popularityScore;

    private Integer rank;

    public TrendingItem(TrendingRun run, Video video, Double popularityScore, Integer rank) {
        this.run = run;
        this.video = video;
        this.popularityScore = popularityScore;
        this.rank = rank;
    }
}
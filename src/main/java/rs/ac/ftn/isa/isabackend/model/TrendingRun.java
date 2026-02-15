package rs.ac.ftn.isa.isabackend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "trending_runs")
@Getter
@Setter
@NoArgsConstructor
public class TrendingRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime ranAt;

    @OneToMany(mappedBy = "run", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<TrendingItem> items = new ArrayList<>();

    public TrendingRun(LocalDateTime ranAt) {
        this.ranAt = ranAt;
    }
}
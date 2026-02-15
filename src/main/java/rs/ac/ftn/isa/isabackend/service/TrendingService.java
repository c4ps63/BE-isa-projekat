package rs.ac.ftn.isa.isabackend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.ac.ftn.isa.isabackend.model.TrendingItem;
import rs.ac.ftn.isa.isabackend.model.TrendingRun;
import rs.ac.ftn.isa.isabackend.model.Video;
import rs.ac.ftn.isa.isabackend.model.VideoView;
import rs.ac.ftn.isa.isabackend.repository.TrendingRunRepository;
import rs.ac.ftn.isa.isabackend.repository.VideoViewRepository;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TrendingService {

    @Autowired
    private VideoViewRepository videoViewRepository;

    @Autowired
    private TrendingRunRepository trendingRunRepository;

    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void runEtlPipeline() {
        System.out.println(">>> ETL PIPELINE STARTED: Calculating popular videos...");
        LocalDateTime now = LocalDateTime.now();

        // 1. EXTRACT: Citati informacije o pregledima iz poslednjih 7 dana
        LocalDateTime sevenDaysAgo = now.minusDays(7);
        List<VideoView> recentViews = videoViewRepository.findAllByViewedAtAfter(sevenDaysAgo);

        // 2. TRANSFORM: Izračunati popularity score
        Map<Video, Double> videoScores = new HashMap<>();

        for (VideoView view : recentViews) {
            long daysAgo = ChronoUnit.DAYS.between(view.getViewedAt(), now);

            double weight = 7 - daysAgo + 1;

            if (weight < 0) weight = 0;

            videoScores.put(view.getVideo(), videoScores.getOrDefault(view.getVideo(), 0.0) + weight);
        }

        List<Map.Entry<Video, Double>> sortedVideos = videoScores.entrySet().stream()
                .sorted(Map.Entry.<Video, Double>comparingByValue().reversed())
                .limit(3) // Uzimamo top 3
                .collect(Collectors.toList());

        // 3. LOAD: Upisati rezultate u novu tabelu
        TrendingRun run = new TrendingRun(now);

        List<TrendingItem> items = new ArrayList<>();
        int rank = 1;

        for (Map.Entry<Video, Double> entry : sortedVideos) {
            TrendingItem item = new TrendingItem(run, entry.getKey(), entry.getValue(), rank++);
            items.add(item);
        }

        run.setItems(items);
        trendingRunRepository.save(run);

        System.out.println(">>> ETL PIPELINE FINISHED. Saved top " + items.size() + " videos.");
    }

    public List<TrendingItem> getLatestTrending() {
        return trendingRunRepository.findTopByOrderByRanAtDesc()
                .map(TrendingRun::getItems)
                .orElse(Collections.emptyList());
    }
}
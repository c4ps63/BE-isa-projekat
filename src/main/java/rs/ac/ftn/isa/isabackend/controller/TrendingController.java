package rs.ac.ftn.isa.isabackend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rs.ac.ftn.isa.isabackend.dto.VideoDTO;
import rs.ac.ftn.isa.isabackend.model.TrendingItem;
import rs.ac.ftn.isa.isabackend.service.TrendingService;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/trending")
public class TrendingController {

    @Autowired
    private TrendingService trendingService;

    @GetMapping
    public ResponseEntity<List<VideoDTO>> getTrendingVideos() {
        List<TrendingItem> trendingItems = trendingService.getLatestTrending();

        List<VideoDTO> dtos = trendingItems.stream()
                .map(item -> new VideoDTO(item.getVideo()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    // Pomoćni endpoint za testiranje
    // http://localhost:8080/api/trending/run-etl
    //@PostMapping("/run-etl")
    @GetMapping("/run-etl") // stoji get da bih mogao odmah da testiram
    public ResponseEntity<String> forceRunEtl() {
        trendingService.runEtlPipeline();
        return ResponseEntity.ok("ETL pipeline successfully executed manually.");
    }
}
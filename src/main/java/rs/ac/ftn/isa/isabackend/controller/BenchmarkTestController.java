package rs.ac.ftn.isa.isabackend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rs.ac.ftn.isa.isabackend.dto.UploadEvent;
import rs.ac.ftn.isa.isabackend.service.UploadEventProducer;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;

@RestController
@RequestMapping("/api/benchmark")
public class BenchmarkTestController {

    @Autowired
    private UploadEventProducer uploadEventProducer;

    @PostMapping("/send/{count}")
    public ResponseEntity<Map<String, Object>> sendTestMessages(@PathVariable int count) {
        if (count < 1 || count > 1000) {
            return ResponseEntity.badRequest().body(Map.of("error", "Count must be between 1 and 1000"));
        }

        Random random = new Random();
        long startTime = System.currentTimeMillis();

        for (int i = 1; i <= count; i++) {
            UploadEvent event = new UploadEvent(
                    (long) i,
                    "Benchmark Video #" + i,
                    "benchmark_video_" + i + ".mp4",
                    random.nextInt(100_000_000) + 1_000_000L,
                    "benchmark_user",
                    LocalDateTime.now().toString(),
                    ".mp4",
                    "Beograd, Srbija",
                    44.7866 + random.nextDouble() * 0.1,
                    20.4489 + random.nextDouble() * 0.1,
                    random.nextInt(3600) + 30,
                    "Benchmark test video description for message #" + i
            );
            uploadEventProducer.sendUploadEvent(event);
        }

        long elapsed = System.currentTimeMillis() - startTime;

        return ResponseEntity.ok(Map.of(
                "sent", count,
                "totalTimeMs", elapsed,
                "message", "Sent " + count + " upload events to both JSON and Protobuf queues"
        ));
    }
}

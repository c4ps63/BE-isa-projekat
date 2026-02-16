package rs.ac.ftn.isa.uploadeventconsumer.benchmark;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/benchmark")
public class BenchmarkController {

    @Autowired
    private BenchmarkService benchmarkService;

    @GetMapping("/report")
    public ResponseEntity<Map<String, Object>> getReport() {
        return ResponseEntity.ok(benchmarkService.getReport());
    }

    @PostMapping("/reset")
    public ResponseEntity<Map<String, String>> resetMetrics() {
        benchmarkService.resetMetrics();
        return ResponseEntity.ok(Map.of("message", "Metrics reset successfully"));
    }
}

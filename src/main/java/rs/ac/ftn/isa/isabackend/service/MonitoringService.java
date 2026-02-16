package rs.ac.ftn.isa.isabackend.service;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;
import rs.ac.ftn.isa.isabackend.repository.UserRepository;
import java.time.LocalDateTime;

@Service
public class MonitoringService {

    private final UserRepository userRepository;

    public MonitoringService(MeterRegistry meterRegistry, UserRepository userRepository) {
        this.userRepository = userRepository;

        Gauge.builder("active_users_24h", this, MonitoringService::countActiveUsers)
                .description("Broj korisnika ulogovanih u poslednjih 24h")
                .register(meterRegistry);
    }

    public double countActiveUsers() {
        return userRepository.countByLastLoginAfter(LocalDateTime.now().minusHours(24));
    }
}
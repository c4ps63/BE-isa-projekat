package rs.ac.ftn.isa.isabackend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class SchedulerService {

    @Autowired
    private CacheManager cacheManager;

    //format: sekund minut sat dan mesec i dan u nedelji
    @Scheduled(cron = "0 0 3 * * ?")
    public void clearTileCache() {
        System.out.println("SCHEDULER: Pokrećem noćno čišćenje keša mape... " + LocalDateTime.now());

        if (cacheManager.getCache("mapTiles") != null) {
            cacheManager.getCache("mapTiles").clear();
            System.out.println("SCHEDULER: Keš 'mapTiles' je uspešno obrisan.");
        }
    }
}
package rs.ac.ftn.isa.isabackend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import rs.ac.ftn.isa.isabackend.model.User;
import rs.ac.ftn.isa.isabackend.model.Video;
import rs.ac.ftn.isa.isabackend.repository.UserRepository;
import rs.ac.ftn.isa.isabackend.repository.VideoRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Component
@Order(2)
public class BulkDataInitializer implements CommandLineRunner {

    private final VideoRepository videoRepository;
    private final UserRepository userRepository;

    @Value("${app.db.init-bulk-data:false}")
    private boolean shouldInitBulkData;

    public BulkDataInitializer(VideoRepository videoRepository, UserRepository userRepository) {
        this.videoRepository = videoRepository;
        this.userRepository = userRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        if (videoRepository.count() < 50) {
            System.out.println("=== POČINJE GENERISANJE 5000 VIDEA (BULK) ===");

            List<User> users = userRepository.findAll();
            if (users.isEmpty()) {
                System.out.println("Nema korisnika u bazi! Preskačem generisanje videa.");
                return;
            }

            List<Video> bulkVideos = new ArrayList<>();
            Random rand = new Random();


            String existingVideoFile = "video1.mp4";
            String existingImageFile = "image_4.jpg";

            for (int i = 0; i < 1; i++) {
                Video v = new Video();
                v.setTitle("Bulk Video #" + (i + 1));
                v.setDescription("Automatski generisan video za testiranje performansi mape.");

                v.setVideoUrl(existingVideoFile);
                v.setThumbnailUrl(existingImageFile);

                v.setDuration(rand.nextInt(600) + 15);
                v.setViewCount((long) rand.nextInt(50000));
                v.setUploadedAt(LocalDateTime.now().minusDays(rand.nextInt(365)));

                v.setOwner(users.get(rand.nextInt(users.size())));

                double lat = 35.0 + (70.0 - 35.0) * rand.nextDouble();

                double lon = -10.0 + (40.0 - (-10.0)) * rand.nextDouble();

                v.setLatitude(lat);
                v.setLongitude(lon);
                v.setLocation("Generisana Lokacija " + i);

                bulkVideos.add(v);

                if ((i + 1) % 1000 == 0) {
                    System.out.println("-> Pripremljeno " + (i + 1) + " od 5000 videa...");
                }
            }

            videoRepository.saveAll(bulkVideos);

            System.out.println("=== USPEŠNO SAČUVANO 5000 VIDEA U BAZU! ===");
        }
    }
}
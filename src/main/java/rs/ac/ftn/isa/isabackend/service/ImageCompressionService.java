package rs.ac.ftn.isa.isabackend.service;

import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.ac.ftn.isa.isabackend.model.Video;
import rs.ac.ftn.isa.isabackend.repository.VideoRepository;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ImageCompressionService {

    private static final double COMPRESSION_QUALITY = 0.5;
    private static final String COMPRESSED_PREFIX = "compressed_";
    private final Path uploadDir = Paths.get("uploads");

    @Autowired
    private VideoRepository videoRepository;

    @Scheduled(cron = "*/30 * * * * ?")
    //@Scheduled(cron = "0 0 4 * * ?")
    public void compressOldThumbnails() {
        System.out.println("KOMPRESIJA: Pokretanje dnevne kompresije slika... " + LocalDateTime.now());
        int result = compressImages();
        System.out.println("KOMPRESIJA: Zavrseno. Kompresovano " + result + " slika.");
    }

    @Transactional
    public int compressImages() {
        LocalDateTime oneMonthAgo = LocalDateTime.now().minusMonths(1);
        List<Video> videos = videoRepository.findByThumbnailCompressedFalseAndUploadedAtBefore(oneMonthAgo);

        if (videos.isEmpty()) {
            System.out.println("KOMPRESIJA: Nema nekompresovanih slika starijih od mjesec dana.");
            return 0;
        }

        System.out.println("KOMPRESIJA: Pronadjeno " + videos.size() + " slika za kompresiju.");
        int compressed = 0;

        for (Video video : videos) {
            try {
                if (compressThumbnail(video)) {
                    compressed++;
                }
            } catch (Exception e) {
                System.err.println("KOMPRESIJA: Greska pri kompresiji thumbnails za video ID " + video.getId() + ": " + e.getMessage());
            }
        }

        return compressed;
    }

    private boolean compressThumbnail(Video video) throws IOException {
        String originalFilename = video.getThumbnailUrl();
        if (originalFilename == null || originalFilename.isEmpty()) {
            return false;
        }

        Path originalPath = uploadDir.resolve(originalFilename);
        if (!Files.exists(originalPath)) {
            System.err.println("KOMPRESIJA: Fajl ne postoji: " + originalPath);
            return false;
        }

        // Uvijek cuva kao JPEG - PNG outputQuality nema efekta jer je lossless
        String nameWithoutExt = originalFilename.substring(0, originalFilename.lastIndexOf('.'));
        String compressedFilename = COMPRESSED_PREFIX + nameWithoutExt + ".jpg";
        Path compressedPath = uploadDir.resolve(compressedFilename);

        long originalSize = Files.size(originalPath);

        Thumbnails.of(originalPath.toFile())
                .scale(1.0)
                .outputFormat("jpg")
                .outputQuality(COMPRESSION_QUALITY)
                .toFile(compressedPath.toFile());

        long compressedSize = Files.size(compressedPath);

        if (compressedSize >= originalSize) {
            Files.delete(compressedPath);
            video.setThumbnailCompressed(true);
            videoRepository.save(video);
            System.out.println("KOMPRESIJA: " + originalFilename +
                    " - preskoceno (kompresovana verzija " + compressedSize / 1024 +
                    " KB >= original " + originalSize / 1024 + " KB)");
            return true;
        }

        video.setThumbnailUrl(compressedFilename);
        video.setThumbnailCompressed(true);
        videoRepository.save(video);

        System.out.println("KOMPRESIJA: " + originalFilename +
                " (" + originalSize / 1024 + " KB -> " + compressedSize / 1024 + " KB, " +
                "ušteda: " + (100 - (compressedSize * 100 / originalSize)) + "%)");

        return true;
    }
}

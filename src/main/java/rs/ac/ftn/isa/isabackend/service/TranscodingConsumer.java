package rs.ac.ftn.isa.isabackend.service;

import com.rabbitmq.client.Channel;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;
import rs.ac.ftn.isa.isabackend.config.RabbitMQConfig;
import rs.ac.ftn.isa.isabackend.dto.TranscodingMessage;
import rs.ac.ftn.isa.isabackend.model.TranscodingStatus;
import rs.ac.ftn.isa.isabackend.model.Video;
import rs.ac.ftn.isa.isabackend.repository.VideoRepository;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
public class TranscodingConsumer {

    private static final Path UPLOAD_DIR = Paths.get("uploads");

    @Autowired
    private VideoRepository videoRepository;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_NAME, concurrency = "2")
    public void processTranscoding(TranscodingMessage message, Channel channel,
                                   @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {

        String consumer = Thread.currentThread().getName();
        System.out.println("TRANSCODING [" + consumer + "]: Primljena poruka za video ID " + message.getVideoId());

        try {
            Optional<Video> optVideo = videoRepository.findById(message.getVideoId());

            if (optVideo.isEmpty()) {
                System.err.println("TRANSCODING [" + consumer + "]: Video ID " + message.getVideoId() + " ne postoji u bazi. Preskacemo.");
                channel.basicAck(tag, false);
                return;
            }

            Video video = optVideo.get();

            // Idempotentnost: ako je video vec transkodiran, preskoci (zastita od duple obrade)
            if (video.getTranscodingStatus() == TranscodingStatus.COMPLETED) {
                System.out.println("TRANSCODING [" + consumer + "]: Video ID " + message.getVideoId() +
                        " vec transkodiran (COMPLETED). Preskacemo - idempotentnost.");
                channel.basicAck(tag, false);
                return;
            }

            // Postavi status na PROCESSING
            video.setTranscodingStatus(TranscodingStatus.PROCESSING);
            videoRepository.save(video);
            System.out.println("TRANSCODING [" + consumer + "]: Status -> PROCESSING za video ID " + message.getVideoId());

            // Pripremi putanje
            String inputFileName = message.getInputPath();
            Path inputPath = UPLOAD_DIR.resolve(inputFileName);

            if (!Files.exists(inputPath)) {
                System.err.println("TRANSCODING [" + consumer + "]: Fajl ne postoji: " + inputPath);
                video.setTranscodingStatus(TranscodingStatus.FAILED);
                videoRepository.save(video);
                channel.basicAck(tag, false);
                return;
            }

            String nameWithoutExt = inputFileName.contains(".")
                    ? inputFileName.substring(0, inputFileName.lastIndexOf('.'))
                    : inputFileName;
            String outputFileName = "transcoded_" + nameWithoutExt + "." + message.getOutputFormat();
            Path outputPath = UPLOAD_DIR.resolve(outputFileName);

            // Pokreni FFmpeg
            List<String> command = Arrays.asList(
                    "ffmpeg",
                    "-i", inputPath.toString(),
                    "-vf", "scale=-2:" + message.getResolution(),
                    "-c:v", "libx264",
                    "-preset", "fast",
                    "-crf", "23",
                    "-c:a", "aac",
                    "-b:a", "128k",
                    "-y",
                    outputPath.toString()
            );

            System.out.println("TRANSCODING [" + consumer + "]: FFmpeg komanda: " + String.join(" ", command));

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            // Citaj FFmpeg output (inace se proces moze blokirati ako se buffer napuni)
            StringBuilder ffmpegOutput = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    ffmpegOutput.append(line).append("\n");
                }
            }

            boolean finished = process.waitFor(30, TimeUnit.MINUTES);

            if (!finished) {
                process.destroyForcibly();
                System.err.println("TRANSCODING [" + consumer + "]: FFmpeg timeout za video ID " + message.getVideoId());
                video.setTranscodingStatus(TranscodingStatus.FAILED);
                videoRepository.save(video);
                channel.basicAck(tag, false);
                return;
            }

            int exitCode = process.exitValue();

            if (exitCode == 0 && Files.exists(outputPath)) {
                long originalSize = Files.size(inputPath);
                long transcodedSize = Files.size(outputPath);

                video.setVideoUrl(outputFileName);
                video.setTranscodingStatus(TranscodingStatus.COMPLETED);
                videoRepository.save(video);

                System.out.println("TRANSCODING [" + consumer + "]: Uspjesno za video ID " + message.getVideoId() +
                        " (" + originalSize / 1024 + " KB -> " + transcodedSize / 1024 + " KB)");
            } else {
                video.setTranscodingStatus(TranscodingStatus.FAILED);
                videoRepository.save(video);
                System.err.println("TRANSCODING [" + consumer + "]: FFmpeg greska (exit code: " + exitCode +
                        ") za video ID " + message.getVideoId());
                System.err.println("TRANSCODING [" + consumer + "]: FFmpeg output:\n" + ffmpegOutput);
            }

        } catch (Exception e) {
            System.err.println("TRANSCODING [" + consumer + "]: Greska za video ID " + message.getVideoId() + ": " + e.getMessage());

            try {
                Optional<Video> optVideo = videoRepository.findById(message.getVideoId());
                if (optVideo.isPresent()) {
                    Video video = optVideo.get();
                    video.setTranscodingStatus(TranscodingStatus.FAILED);
                    videoRepository.save(video);
                }
            } catch (Exception dbError) {
                System.err.println("TRANSCODING [" + consumer + "]: Greska pri azuriranju statusa: " + dbError.getMessage());
            }
        }

        channel.basicAck(tag, false);
    }
}

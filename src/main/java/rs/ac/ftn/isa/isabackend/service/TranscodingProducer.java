package rs.ac.ftn.isa.isabackend.service;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import rs.ac.ftn.isa.isabackend.config.RabbitMQConfig;
import rs.ac.ftn.isa.isabackend.dto.TranscodingMessage;
import rs.ac.ftn.isa.isabackend.model.Video;

@Service
public class TranscodingProducer {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Value("${transcoding.output-format:mp4}")
    private String outputFormat;

    @Value("${transcoding.resolution:720}")
    private String resolution;

    public void sendForTranscoding(Video video) {
        TranscodingMessage message = new TranscodingMessage(
                video.getId(),
                video.getVideoUrl(),
                outputFormat,
                resolution
        );

        rabbitTemplate.convertAndSend(RabbitMQConfig.QUEUE_NAME, message);

        System.out.println("TRANSCODING: Poruka poslana u queue za video ID " + video.getId() +
                " (" + video.getVideoUrl() + ")");
    }
}

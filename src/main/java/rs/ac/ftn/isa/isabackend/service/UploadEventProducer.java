package rs.ac.ftn.isa.isabackend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import rs.ac.ftn.isa.isabackend.config.RabbitMQConfig;
import rs.ac.ftn.isa.isabackend.dto.UploadEvent;
import rs.ac.ftn.isa.isabackend.proto.UploadEventProto;

@Service
public class UploadEventProducer {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public void sendUploadEvent(UploadEvent event) {
        // --- JSON serialization with timing ---
        long jsonStart = System.nanoTime();
        byte[] jsonBytes;
        try {
            jsonBytes = objectMapper.writeValueAsBytes(event);
        } catch (Exception e) {
            System.err.println("UPLOAD-EVENT: JSON serialization error: " + e.getMessage());
            return;
        }
        long jsonSerTime = System.nanoTime() - jsonStart;

        MessageProperties jsonProps = new MessageProperties();
        jsonProps.setContentType("application/json");
        jsonProps.setHeader("serializationTimeNanos", jsonSerTime);
        jsonProps.setHeader("messageSizeBytes", jsonBytes.length);
        rabbitTemplate.send(RabbitMQConfig.UPLOAD_EVENT_JSON_QUEUE, new Message(jsonBytes, jsonProps));

        System.out.println("UPLOAD-EVENT [JSON]: video ID " + event.getVideoId()
                + " | size=" + jsonBytes.length + "B | serTime=" + (jsonSerTime / 1000) + "us");

        // --- Protobuf serialization with timing ---
        long protoStart = System.nanoTime();
        UploadEventProto.UploadEvent protoEvent = UploadEventProto.UploadEvent.newBuilder()
                .setVideoId(event.getVideoId() != null ? event.getVideoId() : 0)
                .setTitle(event.getTitle() != null ? event.getTitle() : "")
                .setFileName(event.getFileName() != null ? event.getFileName() : "")
                .setFileSizeBytes(event.getFileSizeBytes())
                .setAuthorUsername(event.getAuthorUsername() != null ? event.getAuthorUsername() : "")
                .setUploadTimestamp(event.getUploadTimestamp() != null ? event.getUploadTimestamp() : "")
                .setVideoFormat(event.getVideoFormat() != null ? event.getVideoFormat() : "")
                .setLocation(event.getLocation() != null ? event.getLocation() : "")
                .setLatitude(event.getLatitude())
                .setLongitude(event.getLongitude())
                .setDurationSeconds(event.getDurationSeconds())
                .setDescription(event.getDescription() != null ? event.getDescription() : "")
                .build();
        byte[] protoBytes = protoEvent.toByteArray();
        long protoSerTime = System.nanoTime() - protoStart;

        MessageProperties protoProps = new MessageProperties();
        protoProps.setContentType("application/x-protobuf");
        protoProps.setHeader("serializationTimeNanos", protoSerTime);
        protoProps.setHeader("messageSizeBytes", protoBytes.length);
        rabbitTemplate.send(RabbitMQConfig.UPLOAD_EVENT_PROTOBUF_QUEUE, new Message(protoBytes, protoProps));

        System.out.println("UPLOAD-EVENT [PROTO]: video ID " + event.getVideoId()
                + " | size=" + protoBytes.length + "B | serTime=" + (protoSerTime / 1000) + "us");
    }
}

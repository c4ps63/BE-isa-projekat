package rs.ac.ftn.isa.uploadeventconsumer.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rs.ac.ftn.isa.uploadeventconsumer.benchmark.BenchmarkService;
import rs.ac.ftn.isa.uploadeventconsumer.config.RabbitMQConfig;
import rs.ac.ftn.isa.uploadeventconsumer.dto.UploadEvent;

@Component
public class JsonUploadEventConsumer {

    @Autowired
    private BenchmarkService benchmarkService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @RabbitListener(queues = RabbitMQConfig.UPLOAD_EVENT_JSON_QUEUE)
    public void receiveJsonMessage(Message message) {
        byte[] body = message.getBody();
        int messageSize = body.length;

        Long serializationTimeNanos = message.getMessageProperties().getHeader("serializationTimeNanos");
        if (serializationTimeNanos == null) serializationTimeNanos = 0L;

        long deserStart = System.nanoTime();
        try {
            UploadEvent event = objectMapper.readValue(body, UploadEvent.class);
            long deserTime = System.nanoTime() - deserStart;

            System.out.println("JSON RECEIVED: videoId=" + event.getVideoId()
                    + " | size=" + messageSize + "B"
                    + " | serTime=" + (serializationTimeNanos / 1000) + "us"
                    + " | deserTime=" + (deserTime / 1000) + "us");

            benchmarkService.recordJson(serializationTimeNanos, deserTime, messageSize);

        } catch (Exception e) {
            System.err.println("JSON deserialization error: " + e.getMessage());
        }
    }
}

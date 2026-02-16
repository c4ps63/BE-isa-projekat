package rs.ac.ftn.isa.uploadeventconsumer.config;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String UPLOAD_EVENT_JSON_QUEUE = "upload-event-json";
    public static final String UPLOAD_EVENT_PROTOBUF_QUEUE = "upload-event-protobuf";

    @Bean
    public Queue uploadEventJsonQueue() {
        return new Queue(UPLOAD_EVENT_JSON_QUEUE, true);
    }

    @Bean
    public Queue uploadEventProtobufQueue() {
        return new Queue(UPLOAD_EVENT_PROTOBUF_QUEUE, true);
    }
}

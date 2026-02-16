package rs.ac.ftn.isa.isabackend.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String QUEUE_NAME = "transcoding-queue";
    public static final String UPLOAD_EVENT_JSON_QUEUE = "upload-event-json";
    public static final String UPLOAD_EVENT_PROTOBUF_QUEUE = "upload-event-protobuf";

    @Bean
    public Queue transcodingQueue() {
        return new Queue(QUEUE_NAME, true);
    }

    @Bean
    public Queue uploadEventJsonQueue() {
        return new Queue(UPLOAD_EVENT_JSON_QUEUE, true);
    }

    @Bean
    public Queue uploadEventProtobufQueue() {
        return new Queue(UPLOAD_EVENT_PROTOBUF_QUEUE, true);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}

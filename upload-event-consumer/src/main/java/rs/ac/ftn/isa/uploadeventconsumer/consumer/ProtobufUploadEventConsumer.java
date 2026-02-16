package rs.ac.ftn.isa.uploadeventconsumer.consumer;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rs.ac.ftn.isa.isabackend.proto.UploadEventProto;
import rs.ac.ftn.isa.uploadeventconsumer.benchmark.BenchmarkService;
import rs.ac.ftn.isa.uploadeventconsumer.config.RabbitMQConfig;

@Component
public class ProtobufUploadEventConsumer {

    @Autowired
    private BenchmarkService benchmarkService;

    @RabbitListener(queues = RabbitMQConfig.UPLOAD_EVENT_PROTOBUF_QUEUE)
    public void receiveProtobufMessage(Message message) {
        byte[] body = message.getBody();
        int messageSize = body.length;

        Long serializationTimeNanos = message.getMessageProperties().getHeader("serializationTimeNanos");
        if (serializationTimeNanos == null) serializationTimeNanos = 0L;

        long deserStart = System.nanoTime();
        try {
            UploadEventProto.UploadEvent event = UploadEventProto.UploadEvent.parseFrom(body);
            long deserTime = System.nanoTime() - deserStart;

            System.out.println("PROTOBUF RECEIVED: videoId=" + event.getVideoId()
                    + " | size=" + messageSize + "B"
                    + " | serTime=" + (serializationTimeNanos / 1000) + "us"
                    + " | deserTime=" + (deserTime / 1000) + "us");

            benchmarkService.recordProtobuf(serializationTimeNanos, deserTime, messageSize);

        } catch (Exception e) {
            System.err.println("Protobuf deserialization error: " + e.getMessage());
        }
    }
}

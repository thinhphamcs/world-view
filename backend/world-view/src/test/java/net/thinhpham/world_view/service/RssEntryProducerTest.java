package net.thinhpham.world_view.service;

import static org.mockito.Mockito.verify;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import net.thinhpham.world_view.constant.KafkaTopics;
import net.thinhpham.world_view.dto.RssEntryDto;

@ExtendWith (MockitoExtension.class)
public class RssEntryProducerTest {
    @Mock 
    KafkaTemplate<String, RssEntryDto> kafkaTemplate;
    @InjectMocks 
    RssEntryProducer rssEntryProducer;
    @Test 
    public void send_validDto_sendsToTopicWithLinkAsKey() {
        // Arrange
        RssEntryDto dto = new RssEntryDto("http://www.test.com", Instant.now(), "NPR");
        // Act
        rssEntryProducer.send(dto);
        // Assert
        verify(kafkaTemplate).send(KafkaTopics.RSS_ENTRIES, dto.link(), dto);
    }
}

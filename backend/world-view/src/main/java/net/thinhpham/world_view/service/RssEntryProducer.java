package net.thinhpham.world_view.service;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import net.thinhpham.world_view.constant.KafkaTopics;
import net.thinhpham.world_view.dto.RssEntryDto;

@Service
public class RssEntryProducer {
    private final KafkaTemplate<String, RssEntryDto> kafkaTemplate;
    
    public RssEntryProducer(KafkaTemplate<String, RssEntryDto> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void send(RssEntryDto rssEntryDto) {
        kafkaTemplate.send(KafkaTopics.RSS_ENTRIES, rssEntryDto.link(), rssEntryDto);
    }
}
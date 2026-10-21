package net.thinhpham.world_view.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import net.thinhpham.world_view.dto.RssEntryDto;
import net.thinhpham.world_view.repository.RssEntryRepository;
import net.thinhpham.world_view.entity.RssEntry;

@Service
public class RssEntryConsumer {
    private final RssEntryRepository rssEntryRepository;
    private static final Logger logger = LoggerFactory.getLogger(RssEntryConsumer.class);

    public RssEntryConsumer(RssEntryRepository rssEntryRepository) {
        this.rssEntryRepository = rssEntryRepository;
    }

    @KafkaListener(topics = "rss-entries")
    public void consume(RssEntryDto rssEntryDto) {
        if (rssEntryRepository.existsByLink(rssEntryDto.link())) {
            logger.info("Entry with link {} already exists.", rssEntryDto.link());
        } else {
            // Process the new RSS entry
            RssEntry rssEntry = new RssEntry(
                rssEntryDto.link(),
                rssEntryDto.publishedAt(),
                rssEntryDto.sourceName()
            );
            logger.info("Processing new entry with link {}.", rssEntryDto.link());
            rssEntryRepository.save(rssEntry);
        }
    }
}
    
package net.thinhpham.world_view.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.rometools.rome.feed.synd.SyndEntry;

import net.thinhpham.world_view.mapper.RssEntryMapper;

@Service
public class RssPollingScheduler {
    private static final Logger logger = LoggerFactory.getLogger(RssPollingScheduler.class);
    private final RssFeedService rssFeedService;
    private final RssEntryMapper rssEntryMapper;
    private final RssEntryProducer rssEntryProducer;

    public RssPollingScheduler(RssFeedService rssFeedService, RssEntryMapper rssEntryMapper, RssEntryProducer rssEntryProducer) {
        this.rssFeedService = rssFeedService;
        this.rssEntryMapper = rssEntryMapper;
        this.rssEntryProducer = rssEntryProducer;
    }   

    @Scheduled(fixedDelay = 900000)
    public void checkForNewEntries() {
        try {
            List<SyndEntry> newEntries = rssFeedService.fetchLatestEntries();
            logger.info(newEntries.size() + " news entries fetched from the RSS feed.");
            int counter = 0;
            for(SyndEntry entry : newEntries) {
                try {
                    rssEntryProducer.send(rssEntryMapper.toDto(entry));
                    counter++;
                } catch(Exception e) {
                    logger.error("Failed to send entry {}", entry.getLink(), e);
                }
            }
            logger.info("Sent {} entries to Kafka", counter);
        } catch (Exception e) {
            logger.error("Error occurred while fetching latest RSS entries.", e);
        }
    }
}

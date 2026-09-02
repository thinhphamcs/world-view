package net.thinhpham.world_view.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.rometools.rome.feed.synd.SyndEntry;

@Service
public class RssPollingScheduler {
    private static final Logger logger = LoggerFactory.getLogger(RssPollingScheduler.class);
    private final RssFeedService rssFeedService;

    public RssPollingScheduler(RssFeedService rssFeedService) {
        this.rssFeedService = rssFeedService;
    }   

    @Scheduled(fixedDelay = 900000)
    public void checkForNewEntries() {
        try {
            List<SyndEntry> newEntries = rssFeedService.fetchLatestEntries();
            logger.info(newEntries.size() + " news entries fetched from the RSS feed.");
        } catch (Exception e) {
            logger.error("Error occurred while fetching latest RSS entries.", e);
        }
    }
}

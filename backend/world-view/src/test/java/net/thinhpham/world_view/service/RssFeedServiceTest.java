package net.thinhpham.world_view.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.io.InputStream;

import org.junit.jupiter.api.Test;

import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;

import net.thinhpham.world_view.config.NprFeedProperties;

public class RssFeedServiceTest {
    @Test
    public void parseFeed_validXml_returnAllEntries() throws IOException, FeedException {
        InputStream inputStream = getClass().getResourceAsStream("/sample-feed-only-2.xml");
        RssFeedService rssFeedService = new RssFeedService(new NprFeedProperties());
        SyndFeed feed = rssFeedService.parseFeed(inputStream);
        assertNotNull(feed);
        assertEquals(2, feed.getEntries().size(), "Expected 2 entries in the feed");
        assertEquals("Senate Democrats say banks turned blind eye to suspicious moves by Jeffrey Epstein", feed.getEntries().get(0).getTitle(), "First entry title does match");
    }
}

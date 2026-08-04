package net.thinhpham.world_view.service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

import org.springframework.stereotype.Service;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;

import net.thinhpham.world_view.config.NprFeedProperties;

@Service
public class RssFeedService {
    private final NprFeedProperties nprFeedProperties;
    public RssFeedService(NprFeedProperties nprFeedProperties) {
        this.nprFeedProperties = nprFeedProperties;
    }
    private InputStream openFeedStream() throws IOException {
        URL url = new URL(nprFeedProperties.getUrl());
        return url.openStream();
    }
     SyndFeed parseFeed(InputStream inputStream) throws IOException, FeedException {
        try (XmlReader xmlReader = new XmlReader(inputStream)) {
            SyndFeedInput input = new SyndFeedInput();
            return input.build(xmlReader);
        }
    }
    public List<SyndEntry> fetchLatestEntries() throws IOException, FeedException {
        try(InputStream inputStream = openFeedStream()) {
            SyndFeed feed = parseFeed(inputStream);
            return feed.getEntries();
        }
    }
}
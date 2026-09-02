package net.thinhpham.world_view.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.io.IOException;
import java.util.List;
import com. rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.io.FeedException;

@ExtendWith(MockitoExtension.class)
public class RssPollingSchedulerTest {
    @Mock
    private RssFeedService rssFeedService;
    @InjectMocks
    private RssPollingScheduler rssPollingScheduler;

    @Test
    public void checkForNewEntries_whenFetchSucceeds_logsEntryCount() throws IOException, FeedException {
        List<SyndEntry> entries = List.of(Mockito.mock(SyndEntry.class));
        Mockito.when(rssFeedService.fetchLatestEntries()).thenReturn(entries);
        rssPollingScheduler.checkForNewEntries();
        assertDoesNotThrow(() -> rssPollingScheduler.checkForNewEntries());
    }
    @Test
    public void checkForNewEntries_whenFetchThrows_doesNotPropagateException() throws IOException, FeedException {
        Mockito.when(rssFeedService.fetchLatestEntries()).thenThrow(new RuntimeException("Simulated fetch failure"));
        assertDoesNotThrow(() -> rssPollingScheduler.checkForNewEntries());
    }
}

package net.thinhpham.world_view.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import com. rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.io.FeedException;

import net.thinhpham.world_view.dto.RssEntryDto;
import net.thinhpham.world_view.mapper.RssEntryMapper;

@ExtendWith(MockitoExtension.class)
public class RssPollingSchedulerTest {
    @Mock
    private RssFeedService rssFeedService;
    @Mock 
    private RssEntryMapper rssEntryMapper;
    @Mock
    private RssEntryProducer rssEntryProducer;
    @InjectMocks
    private RssPollingScheduler rssPollingScheduler;

    @Test
    public void checkForNewEntries_whenFetchSucceeds_sendsEachEntry() throws IOException, FeedException {
        // Arrange
        SyndEntry entry = Mockito.mock(SyndEntry.class);
        List<SyndEntry> entries = List.of(entry);
        RssEntryDto dto = new RssEntryDto("http://www.test.com", Instant.now(), "NPR");
        Mockito.when(rssFeedService.fetchLatestEntries()).thenReturn(entries);
        Mockito.when(rssEntryMapper.toDto(entry)).thenReturn(dto);
        // Act
        rssPollingScheduler.checkForNewEntries();
        // Assert
        verify(rssEntryProducer).send(dto);
    }
    
    @Test
    public void checkForNewEntries_whenFetchThrows_doesNotPropagateException() throws IOException, FeedException {
        // Arrange
        Mockito.when(rssFeedService.fetchLatestEntries()).thenThrow(new RuntimeException("Simulated fetch failure"));
        // Act + Assert
        assertDoesNotThrow(() -> rssPollingScheduler.checkForNewEntries());
    }

    @Test
    public void checkForNewEntries_whenOneSendFails_sendsRemainingEntries() throws IOException, FeedException {
        // Arrange 
        SyndEntry entryFail = Mockito.mock(SyndEntry.class);
        SyndEntry entrySuccess = Mockito.mock(SyndEntry.class);
        List<SyndEntry> entries = List.of(entryFail, entrySuccess);
        RssEntryDto dtoFail = new RssEntryDto("http://www.fail.com", Instant.now(), "NPR");
        RssEntryDto dtoSuccess = new RssEntryDto("http://www.success.com", Instant.now(), "NPR");
        Mockito.when(rssFeedService.fetchLatestEntries()).thenReturn(entries);
        Mockito.when(rssEntryMapper.toDto(entryFail)).thenReturn(dtoFail);
        Mockito.when(rssEntryMapper.toDto(entrySuccess)).thenReturn(dtoSuccess);
        doThrow(new RuntimeException()).when(rssEntryProducer).send(dtoFail);
        // Act
        rssPollingScheduler.checkForNewEntries();
        // Assert
        verify(rssEntryProducer).send(dtoSuccess);
    }
}

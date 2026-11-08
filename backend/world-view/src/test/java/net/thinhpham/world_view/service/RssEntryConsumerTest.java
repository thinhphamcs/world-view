package net.thinhpham.world_view.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import net.thinhpham.world_view.dto.RssEntryDto;
import net.thinhpham.world_view.entity.RssEntry;
import net.thinhpham.world_view.repository.RssEntryRepository;

@ExtendWith(MockitoExtension.class)
public class RssEntryConsumerTest {
    @Mock
    private RssEntryRepository rssEntryRepository;
    @InjectMocks 
    private RssEntryConsumer rssEntryConsumer;

    @Test 
    public void consume_newLink_savesEntry() {
        // Arrange
        RssEntryDto rssEntryDto = new RssEntryDto("http://test.com", "TEST_1", "This is first test", Instant.now(), "Mock up source");
        // Simulate that the link does not exist in the repository
        when(rssEntryRepository.existsByLink(rssEntryDto.link())).thenReturn(false);
        // Act
        rssEntryConsumer.consume(rssEntryDto);
        // Assert
        verify(rssEntryRepository).save(any(RssEntry.class));
    }
    
    @Test
    public void consume_existingLink_skipsSave() {
        // Arrange
        RssEntryDto rssEntryDto = new RssEntryDto("http://test.com", "TEST_2", "This is second test", Instant.now(), "Mock up source");
        // Simulate that the link already exists in the repository
        when(rssEntryRepository.existsByLink(rssEntryDto.link())).thenReturn(true);
        // Act
        rssEntryConsumer.consume(rssEntryDto);
        // Assert
        verify(rssEntryRepository, never()).save(any(RssEntry.class));
    }

    @Test 
    public void consume_newLink_savesAllFields() {
        // Arrange
        Instant nowInstant = Instant.now();
        RssEntryDto rssEntryDto = new RssEntryDto("http://test.com", "TEST_3", "This is third test", nowInstant, "Mock up source");
        // Simulate that the link does not exist in the repository
        when(rssEntryRepository.existsByLink(rssEntryDto.link())).thenReturn(false);
        ArgumentCaptor<RssEntry> captor = ArgumentCaptor.forClass(RssEntry.class);
        // Act
        rssEntryConsumer.consume(rssEntryDto);
        // Assert
        verify(rssEntryRepository).save(captor.capture());
        RssEntry savedEntry = captor.getValue();
        assertEquals("http://test.com", savedEntry.getLink());
        assertEquals("TEST_3", savedEntry.getTitle());
        assertEquals("This is third test", savedEntry.getSummary());
        assertEquals(nowInstant, savedEntry.getPublishedAt());
        assertEquals("Mock up source", savedEntry.getSourceName());
    }

    @Test
    public void consume_incompleteEntry_skipsSave() {
        // Arrange
        RssEntryDto rssEntryDto = new RssEntryDto("http://test.com", null, "This is fourth test", Instant.now(), "Mock up source");
        // Act
        rssEntryConsumer.consume(rssEntryDto);
        // Assert
        verify(rssEntryRepository, never()).save(any(RssEntry.class));
        verify(rssEntryRepository, never()).existsByLink(any());
    }
}

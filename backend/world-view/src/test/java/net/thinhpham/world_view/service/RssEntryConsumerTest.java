package net.thinhpham.world_view.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
        RssEntryDto rssEntryDto = new RssEntryDto("http://test.com", Instant.now(), "Mock up source");
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
        RssEntryDto rssEntryDto = new RssEntryDto("http://test.com", Instant.now(), "Mock up source");
        // Simulate that the link already exists in the repository
        when(rssEntryRepository.existsByLink(rssEntryDto.link())).thenReturn(true);
        // Act
        rssEntryConsumer.consume(rssEntryDto);
        // Assert
        verify(rssEntryRepository, never()).save(any(RssEntry.class));
    }
}

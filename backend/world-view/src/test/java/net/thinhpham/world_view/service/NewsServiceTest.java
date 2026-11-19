package net.thinhpham.world_view.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import net.thinhpham.world_view.dto.PageResponse;
import net.thinhpham.world_view.dto.Story;
import net.thinhpham.world_view.entity.RssEntry;
import net.thinhpham.world_view.repository.RssEntryRepository;

@ExtendWith (MockitoExtension.class)
public class NewsServiceTest {
    @Mock 
    private RssEntryRepository rssEntryRepository;
    @InjectMocks 
    private NewsService newsService;
    
    @Test
    public void getNews_oneEntry_mapsAllFieldsToStory() {
        // Arrange
        RssEntry rssEntry = new RssEntry("https://www.test.com/story", "TEST_1", "This is first test", Instant.parse("2100-01-01T12:00:00Z"), "NPR");
        ReflectionTestUtils.setField(rssEntry, "id", 42L);
        Page<RssEntry> fakePage = new PageImpl<>(List.of(rssEntry), PageRequest.of(0, 20), 1);
        when(rssEntryRepository.findAllByOrderByPublishedAtDescIdDesc(any(Pageable.class))).thenReturn(fakePage);
        // Act
        PageResponse<Story> result = newsService.getNews(0, 20);
        // Assert
        assertEquals(42L, result.items().get(0).id());
        assertEquals("TEST_1", result.items().get(0).title());
        assertEquals("This is first test", result.items().get(0).summary());
        assertEquals("https://www.test.com/story", result.items().get(0).link());
        assertEquals(Instant.parse("2100-01-01T12:00:00Z"), result.items().get(0).publishedAt());
        assertEquals("NPR", result.items().get(0).sourceName());
    }

    @Test
    public void getNews_pageAndLimit_passesThemToRepository() {
        // Arrange
        when(rssEntryRepository.findAllByOrderByPublishedAtDescIdDesc(any(Pageable.class))).thenReturn(Page.empty());
        // Act
        newsService.getNews(2, 5);
        // Assert
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(rssEntryRepository).findAllByOrderByPublishedAtDescIdDesc(captor.capture());
        assertEquals(2, captor.getValue().getPageNumber());
        assertEquals(5, captor.getValue().getPageSize());
    }

    @Test
    public void getNews_middlePage_copiesPagingInfo() {
        // Arrange
        RssEntry rssEntry = new RssEntry("https://www.test.com/story", "TEST_3", "This is third test", Instant.parse("2100-01-01T12:00:00Z"), "NPR");
        ReflectionTestUtils.setField(rssEntry, "id", 43L);
        Page<RssEntry> fakePage = new PageImpl<>(List.of(rssEntry), PageRequest.of(1, 2), 5);
        when(rssEntryRepository.findAllByOrderByPublishedAtDescIdDesc(any(Pageable.class))).thenReturn(fakePage);
        // Act
        PageResponse<Story> result = newsService.getNews(1, 2);
        // Assert
        assertEquals(1, result.page());
        assertEquals(2, result.size());
        assertEquals(3, result.totalPages());
        assertEquals(5L, result.totalElements());
        assertTrue(result.hasNext());
    }
}

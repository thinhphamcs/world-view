package net.thinhpham.world_view.controller;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import net.thinhpham.world_view.dto.PageResponse;
import net.thinhpham.world_view.dto.Story;
import net.thinhpham.world_view.service.NewsService;


@WebMvcTest(NewsController.class)
public class NewsControllerTest {
    @Autowired 
    private MockMvc mockMvc;
    @MockitoBean 
    private NewsService newsService;

    @Test 
    public void getNews_noParams_usesDefaults() throws Exception {
        // Arrange
        // Act
        mockMvc.perform(get("/api/news")).andExpect(status().isOk());
        // Assert
        verify(newsService).getNews(0, 20);
    }

    @Test 
    public void getNews_validRequest_returnsStoriesAsJson() throws Exception {
        // Arrange
        Story story = new Story(42L, "TEST_2", "This is second test", "https://www.test.com/news", "NPR", Instant.parse("2100-01-01T12:00:00Z"));
        PageResponse<Story> response = new PageResponse<>(List.of(story), 0, 20, 1L, 1, false);
        when(newsService.getNews(0, 20)).thenReturn(response);
        // Act + Assert
        mockMvc.perform(get("/api/news")).andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].id").value(42))
            .andExpect(jsonPath("$.items[0].title").value("TEST_2"))
            .andExpect(jsonPath("$.items[0].summary").value("This is second test"))
            .andExpect(jsonPath("$.items[0].link").value("https://www.test.com/news"))
            .andExpect(jsonPath("$.items[0].sourceName").value("NPR"))
            .andExpect(jsonPath("$.items[0].publishedAt").value("2100-01-01T12:00:00Z"))
            .andExpect(jsonPath("$.hasNext").value(false))
            .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test 
    public void getNews_limitAboveMax_returnsBadRequest() throws Exception {
        // Arrange
        // Act + Assert
        mockMvc.perform(get("/api/news?limit=26")).andExpect(status().isBadRequest());
        verify(newsService, never()).getNews(anyInt(), anyInt());
    }

    @Test 
    public void getNews_limitBelowOne_returnsBadRequest() throws Exception {
        // Arrange
        // Act + Assert
        mockMvc.perform(get("/api/news?limit=0")).andExpect(status().isBadRequest());
        verify(newsService, never()).getNews(anyInt(), anyInt());
    }

    @Test 
    public void getNews_negativePage_returnsBadRequest() throws Exception {
        // Arrange
        // Act + Assert
        mockMvc.perform(get("/api/news?page=-1")).andExpect(status().isBadRequest());
        verify(newsService, never()).getNews(anyInt(), anyInt());
    }

    @Test 
    public void getNews_nonNumericPage_returnsBadRequest() throws Exception {
        // Arrange
        // Act + Assert
        mockMvc.perform(get("/api/news?page=abc")).andExpect(status().isBadRequest());
        verify(newsService, never()).getNews(anyInt(), anyInt());
    }
    
}

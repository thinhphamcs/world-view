package net.thinhpham.world_view.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import net.thinhpham.world_view.dto.PageResponse;
import net.thinhpham.world_view.dto.Story;
import net.thinhpham.world_view.service.NewsService;

@RestController 
public class NewsController {

    private final NewsService newsService;
    private static final int MAX_LIMIT = 25;

    public NewsController(NewsService newsService) {
        this.newsService = newsService;
    }

    @GetMapping("/api/news")
    public PageResponse<Story> getNews(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int limit) {
        if(limit < 1 || limit > MAX_LIMIT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Limit must be between 1 and " + MAX_LIMIT);
        }
        else if(page < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Page must be 0 or more");
        }
        return newsService.getNews(page, limit);
    }
}

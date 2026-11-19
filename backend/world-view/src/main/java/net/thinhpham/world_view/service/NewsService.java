package net.thinhpham.world_view.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import net.thinhpham.world_view.dto.PageResponse;
import net.thinhpham.world_view.dto.Story;
import net.thinhpham.world_view.entity.RssEntry;
import net.thinhpham.world_view.repository.RssEntryRepository;

@Service 
public class NewsService {
    private final RssEntryRepository rssEntryRepository;
    public NewsService(RssEntryRepository rssEntryRepository) {
        this.rssEntryRepository = rssEntryRepository;
    }

    public PageResponse<Story> getNews(int page, int limit) {
        Page<RssEntry> result = rssEntryRepository.findAllByOrderByPublishedAtDescIdDesc(PageRequest.of(page, limit));
        Page<Story> stories = result.map(Story::from);
        return PageResponse.from(stories);
    }
}

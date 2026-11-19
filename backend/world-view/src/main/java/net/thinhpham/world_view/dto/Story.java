package net.thinhpham.world_view.dto;

import java.time.Instant;

import net.thinhpham.world_view.entity.RssEntry;

public record Story (
    Long id,
    String title,
    String summary,
    String link,
    String sourceName,
    Instant publishedAt
) {
    public static Story from(RssEntry entry) {
        return new Story(entry.getId(), entry.getTitle(), entry.getSummary(), entry.getLink(), entry.getSourceName(), entry.getPublishedAt());
    }
}

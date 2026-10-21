package net.thinhpham.world_view.dto;

import java.time.Instant;

public record RssEntryDto(
    String link,
    Instant publishedAt,
    String sourceName
) {

}

package net.thinhpham.world_view.dto;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonIgnore;

public record RssEntryDto(
    String link,
    String title,
    String summary,
    Instant publishedAt,
    String sourceName
) {
    @JsonIgnore 
    public boolean isComplete() {
        if((title() != null && summary() != null) && (!title().isBlank() && !summary().isBlank())) {
            return true;
        }
        return false;
    }
}

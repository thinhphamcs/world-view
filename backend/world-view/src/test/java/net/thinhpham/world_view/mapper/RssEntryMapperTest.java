package net.thinhpham.world_view.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.time.Instant;
import java.util.Date;

import org.junit.jupiter.api.Test;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndEntryImpl;

import net.thinhpham.world_view.dto.RssEntryDto;

public class RssEntryMapperTest {
    @Test 
    public void toDto_publishedDatePresent_usesPublishedDate () {
        // Arrange
        Date date = new Date();
        SyndEntry entry = new SyndEntryImpl();
        entry.setLink("http://www.test.com");
        entry.setPublishedDate(date);
        RssEntryMapper mapper = new RssEntryMapper();
        // Act
        RssEntryDto dto = mapper.toDto(entry);
        // Assert
        assertEquals(new RssEntryDto("http://www.test.com", date.toInstant(), "NPR"), dto);
    }

    @Test 
    public void toDto_onlyUpdatedDate_usesUpdatedDate () {
        // Arrange
        Date date = new Date();
        SyndEntry entry = new SyndEntryImpl();
        entry.setLink("http://www.test.com");
        entry.setUpdatedDate(date);
        entry.setPublishedDate(null);
        RssEntryMapper mapper = new RssEntryMapper();
        // Act
        RssEntryDto dto = mapper.toDto(entry);
        // Assert
        assertEquals(new RssEntryDto("http://www.test.com", date.toInstant(), "NPR"), dto);
    }

    @Test 
    public void toDto_noDates_usesCurrentTime () {
        // Arrange
        Instant instantBefore = Instant.now();
        SyndEntry entry = new SyndEntryImpl();
        entry.setLink("http://www.test.com");
        RssEntryMapper mapper = new RssEntryMapper();
        // Act
        RssEntryDto dto = mapper.toDto(entry);
        Instant instantAfter = Instant.now();
        // Assert
        assertEquals("http://www.test.com", dto.link());
        assertEquals("NPR", dto.sourceName());
        assertFalse(dto.publishedAt().isBefore(instantBefore));
        assertFalse(dto.publishedAt().isAfter(instantAfter));
    }
}

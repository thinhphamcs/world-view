package net.thinhpham.world_view.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Instant;
import java.util.Date;

import org.junit.jupiter.api.Test;

import com.rometools.rome.feed.synd.SyndContent;
import com.rometools.rome.feed.synd.SyndContentImpl;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndEntryImpl;

import net.thinhpham.world_view.dto.RssEntryDto;

public class RssEntryMapperTest {
    @Test 
    public void toDto_publishedDatePresent_usesPublishedDate() {
        // Arrange
        Date date = new Date();
        SyndEntry entry = new SyndEntryImpl();
        SyndContent description = new SyndContentImpl();
        description.setValue("This is first test");
        entry.setLink("http://www.test.com");
        entry.setTitle("TEST_1");
        entry.setDescription(description);
        entry.setPublishedDate(date);
        RssEntryMapper mapper = new RssEntryMapper();
        // Act
        RssEntryDto dto = mapper.toDto(entry);
        // Assert
        assertEquals(new RssEntryDto("http://www.test.com", "TEST_1", "This is first test", date.toInstant(), "NPR"), dto);
    }

    @Test 
    public void toDto_onlyUpdatedDate_usesUpdatedDate() {
        // Arrange
        Date date = new Date();
        SyndEntry entry = new SyndEntryImpl();
        SyndContent description = new SyndContentImpl();
        description.setValue("This is second test");
        entry.setLink("http://www.test.com");
        entry.setTitle("TEST_2");
        entry.setDescription(description);
        entry.setUpdatedDate(date);
        entry.setPublishedDate(null);
        RssEntryMapper mapper = new RssEntryMapper();
        // Act
        RssEntryDto dto = mapper.toDto(entry);
        // Assert
        assertEquals(new RssEntryDto("http://www.test.com", "TEST_2", "This is second test", date.toInstant(), "NPR"), dto);
    }

    @Test 
    public void toDto_noDates_usesCurrentTime() {
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

    @Test 
    public void toDto_titlePresent_mapsTitle() {
        // Arrange
        SyndEntry entry = new SyndEntryImpl();
        entry.setTitle("TEST_3");
        RssEntryMapper mapper = new RssEntryMapper();
        // Act
        RssEntryDto dto = mapper.toDto(entry);
        // Assert
        assertEquals("TEST_3", dto.title());
    }

    @Test 
    public void toDto_descriptionPresent_mapsSummary() {
        // Arrange
        SyndEntry entry = new SyndEntryImpl();
        SyndContent description = new SyndContentImpl();
        description.setValue("This is third test");
        entry.setDescription(description);
        RssEntryMapper mapper = new RssEntryMapper();
        // Act
        RssEntryDto dto = mapper.toDto(entry);
        // Assert
        assertEquals("This is third test", dto.summary());
    }

    @Test 
    public void toDto_noDescription_returnsNullSummary() {
        // Arrange
        SyndEntry entry = new SyndEntryImpl();
        entry.setDescription(null);
        RssEntryMapper mapper = new RssEntryMapper();
        // Act
        RssEntryDto dto = mapper.toDto(entry);
        // Assert
        assertNull(dto.summary());
    }

    @Test 
    public void toDto_htmlDescription_stripsTags() {
        // Arrange
        SyndEntry entry = new SyndEntryImpl();
        SyndContent description = new SyndContentImpl();
        description.setValue("<p>Hello <b>World</b></p>");
        entry.setDescription(description);
        RssEntryMapper mapper = new RssEntryMapper();
        // Act
        RssEntryDto dto = mapper.toDto(entry);
        // Assert
        assertEquals("Hello World", dto.summary());
    }

    @Test 
    public void toDto_htmlEntities_decodesEntities() {
        // Arrange
        SyndEntry entry = new SyndEntryImpl();
        SyndContent description = new SyndContentImpl();
        description.setValue("Hello &amp; World");
        entry.setDescription(description);
        RssEntryMapper mapper = new RssEntryMapper();
        // Act
        RssEntryDto dto = mapper.toDto(entry);
        // Assert
        assertEquals("Hello & World", dto.summary());
    }

    @Test 
    public void toDto_descriptionWithNullValue_returnsNullSummary() {
        // Arrange
        SyndEntry entry = new SyndEntryImpl();
        SyndContent description = new SyndContentImpl();
        entry.setDescription(description);
        RssEntryMapper mapper = new RssEntryMapper();
        // Act
        RssEntryDto dto = mapper.toDto(entry);
        // Assert
        assertNull(dto.summary());
    }
}

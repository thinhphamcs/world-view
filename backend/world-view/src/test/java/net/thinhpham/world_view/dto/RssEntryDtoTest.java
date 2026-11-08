package net.thinhpham.world_view.dto;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;

import org.junit.jupiter.api.Test;

public class RssEntryDtoTest {
    @Test 
    public void isComplete_titleAndSummaryPresent_returnsTrue() {
        // Arrange
        RssEntryDto dto = new RssEntryDto("http://www.test.com", "TEST_1", "This is the first test", Instant.now(), "NPR");
        // Act + Assert
        assertTrue(dto.isComplete());
    }

    @Test 
    public void isComplete_nullTitle_returnsFalse() {
        // Arrange
        RssEntryDto dto = new RssEntryDto("http://www.test.com", null, "This is the second test", Instant.now(), "NPR");
        // Act + Assert
        assertFalse(dto.isComplete());
    }

    @Test 
    public void isComplete_blankTitle_returnsFalse() {
        // Arrange
        RssEntryDto dto = new RssEntryDto("http://www.test.com", " ", "This is the third test", Instant.now(), "NPR");
        // Act + Assert
        assertFalse(dto.isComplete());
    }

    @Test 
    public void isComplete_nullSummary_returnsFalse() {
        // Arrange
        RssEntryDto dto = new RssEntryDto("http://www.test.com", "TEST_4", null, Instant.now(), "NPR");
        // Act + Assert
        assertFalse(dto.isComplete());
    }

    @Test 
    public void isComplete_blankSummary_returnsFalse() {
        // Arrange
        RssEntryDto dto = new RssEntryDto("http://www.test.com", "TEST_5", " ", Instant.now(), "NPR");
        // Act + Assert
        assertFalse(dto.isComplete());
    }
}

package net.thinhpham.world_view.repository;


import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import net.thinhpham.world_view.entity.RssEntry;

@DataJpaTest 
@AutoConfigureTestDatabase(
    replace = AutoConfigureTestDatabase.Replace.NONE
)
@ActiveProfiles(
    "test"
)
public class RssEntryRepositoryTest {

    @Autowired 
    private RssEntryRepository rssEntryRepository;

    @Test 
    public void findAllByOrderByPublishedAtDescIdDesc_differentDates_returnsNewestFirst() {
        // Arrange
        RssEntry older = new RssEntry("https://test.example/older", "TEST_1", "This is first test", Instant.parse("2100-01-01T10:00:00Z"), "NPR");
        rssEntryRepository.save(older);
        RssEntry newer = new RssEntry("https://test.example/newer", "TEST_2", "This is second test", Instant.parse("2100-01-01T12:00:00Z"), "NPR");
        rssEntryRepository.save(newer);
        // Act
        Page<RssEntry> result = rssEntryRepository.findAllByOrderByPublishedAtDescIdDesc(PageRequest.of(0, 2));
        // Assert
        assertEquals("https://test.example/newer", result.getContent().get(0).getLink());
        assertEquals("https://test.example/older", result.getContent().get(1).getLink());
    }

    @Test 
    public void findAllByOrderByPublishedAtDescIdDesc_sameDate_returnsHigherIdFirst() {
        // Arrange
        RssEntry older = new RssEntry("https://test.example/older", "TEST_1", "This is first test", Instant.parse("2100-01-02T10:00:00Z"), "NPR");
        rssEntryRepository.save(older);
        RssEntry newer = new RssEntry("https://test.example/newer", "TEST_2", "This is second test", Instant.parse("2100-01-02T10:00:00Z"), "NPR");
        rssEntryRepository.save(newer);
        // Act
        Page<RssEntry> result = rssEntryRepository.findAllByOrderByPublishedAtDescIdDesc(PageRequest.of(0, 2));
        // Assert
        assertEquals("https://test.example/newer", result.getContent().get(0).getLink());
        assertEquals("https://test.example/older", result.getContent().get(1).getLink());
    }
}

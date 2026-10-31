package net.thinhpham.world_view.mapper;

import java.time.Instant;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.rometools.rome.feed.synd.SyndEntry;

import net.thinhpham.world_view.dto.RssEntryDto;

@Component 
public class RssEntryMapper {
    private static final Logger logger = LoggerFactory.getLogger(RssEntryMapper.class);
    public RssEntryDto toDto(SyndEntry entry) {
        Date checkPublishedDate = entry.getPublishedDate();
        if(checkPublishedDate == null) {
            Date checkUpdatedDate = entry.getUpdatedDate();
            if (checkUpdatedDate == null) {
                logger.warn("No date for {}, using fetch time", entry.getLink());
                return new RssEntryDto(entry.getLink(), Instant.now(), "NPR");
            }
            else {
                return new RssEntryDto(entry.getLink(), entry.getUpdatedDate().toInstant(), "NPR");
            }
        }
        else {
            return new RssEntryDto(entry.getLink(), entry.getPublishedDate().toInstant(), "NPR");
        }        
    }
}

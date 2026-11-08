package net.thinhpham.world_view.mapper;

import java.time.Instant;
import java.util.Date;

import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.rometools.rome.feed.synd.SyndContent;
import com.rometools.rome.feed.synd.SyndEntry;

import net.thinhpham.world_view.dto.RssEntryDto;

@Component 
public class RssEntryMapper {
    
    private static final Logger logger = LoggerFactory.getLogger(RssEntryMapper.class);
    
    public RssEntryDto toDto(SyndEntry entry) {
        Date checkPublishedDate = entry.getPublishedDate();
        SyndContent description = entry.getDescription();
        String summary;
        if(description == null) {
            summary = null;
        }
        else {
            summary = toPlainText(description.getValue());
        }
        if(checkPublishedDate == null) {
            Date checkUpdatedDate = entry.getUpdatedDate();
            if (checkUpdatedDate == null) {
                logger.warn("No date for {}, using fetch time", entry.getLink());
                return new RssEntryDto(entry.getLink(), entry.getTitle(), summary, Instant.now(), "NPR");
            }
            else {
                return new RssEntryDto(entry.getLink(), entry.getTitle(), summary, entry.getUpdatedDate().toInstant(), "NPR");
            }
        }
        else {
            return new RssEntryDto(entry.getLink(), entry.getTitle(), summary, entry.getPublishedDate().toInstant(), "NPR");
        }        
    }

    private String toPlainText(String html) {
        if(html == null) {
            return null;
        }
        else {
            return Jsoup.parse(html).text();
        }
    }
}

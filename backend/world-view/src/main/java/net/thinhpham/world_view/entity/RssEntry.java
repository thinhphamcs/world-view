package net.thinhpham.world_view.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

@Entity
@Table(name = "rss_entry")
public class RssEntry {

    @Id
    @Column(name = "id")
    @GeneratedValue(
        strategy = GenerationType.SEQUENCE, generator = "rss_entry_seq"
    )
    @SequenceGenerator(
        name = "rss_entry_seq",
        sequenceName = "rss_entry_seq",
        allocationSize = 50
    )
    private Long id;

    @Column(
        name = "link", 
        length = 1024, 
        nullable = false, 
        unique = true
    )
    private String link;
    
    @Column(
        name = "published_at", 
        nullable = false
    )
    private Instant publishedAt;
    
    @Column(
        name = "source_name", 
        length = 255, 
        nullable = false
    )
    private String sourceName;

    public RssEntry(String link, Instant publishedAt, String sourceName) {
        this.link = link;
        this.publishedAt = publishedAt;
        this.sourceName = sourceName;
    }

    public RssEntry() {
    }
}

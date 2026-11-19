package net.thinhpham.world_view.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import net.thinhpham.world_view.entity.RssEntry;

public interface RssEntryRepository extends JpaRepository<RssEntry, Long> {
    boolean existsByLink(String link);
    Page<RssEntry> findAllByOrderByPublishedAtDescIdDesc(Pageable pageable);
}

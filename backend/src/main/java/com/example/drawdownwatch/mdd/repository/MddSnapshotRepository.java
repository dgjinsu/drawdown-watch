package com.example.drawdownwatch.mdd.repository;

import com.example.drawdownwatch.mdd.entity.MddSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface MddSnapshotRepository extends JpaRepository<MddSnapshot, Long> {

    Optional<MddSnapshot> findTopByWatchlistItemIdOrderByCalcDateDesc(Long watchlistItemId);

    Optional<MddSnapshot> findByWatchlistItemIdAndCalcDate(Long watchlistItemId, LocalDate calcDate);
}

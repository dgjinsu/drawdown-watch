package com.example.drawdownwatch.mdd.entity;

import com.example.drawdownwatch.watchlist.entity.WatchlistItem;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "mdd_snapshots",
        uniqueConstraints = @UniqueConstraint(columnNames = {"watchlist_item_id", "calc_date"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class MddSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "watchlist_item_id", nullable = false)
    private WatchlistItem watchlistItem;

    @Column(nullable = false)
    private LocalDate calcDate;

    @Column(nullable = false, precision = 20, scale = 4)
    private BigDecimal peakPrice;

    @Column(nullable = false, precision = 20, scale = 4)
    private BigDecimal currentPrice;

    @Column(nullable = false, precision = 7, scale = 4)
    private BigDecimal mddValue;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}

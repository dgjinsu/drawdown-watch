package com.example.drawdownwatch.watchlist.domain;

import com.example.drawdownwatch.global.entity.BaseEntity;
import com.example.drawdownwatch.stock.domain.Stock;
import com.example.drawdownwatch.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "watchlist_items",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "stock_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class WatchlistItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_id", nullable = false)
    private Stock stock;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal threshold;

    @Column(nullable = false, length = 10)
    private String mddPeriod;

    public void updateSettings(BigDecimal threshold, String mddPeriod) {
        if (threshold != null) this.threshold = threshold;
        if (mddPeriod != null) this.mddPeriod = mddPeriod;
    }
}

package com.example.drawdownwatch.stock.domain;

import com.example.drawdownwatch.global.entity.BaseEntity;
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
import java.time.LocalDate;

@Entity
@Table(name = "stock_price_stats",
        uniqueConstraints = @UniqueConstraint(columnNames = {"stock_id", "calc_date"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class StockPriceStat extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_id", nullable = false)
    private Stock stock;

    @Column(nullable = false)
    private LocalDate calcDate;

    @Column(nullable = false, precision = 20, scale = 4)
    private BigDecimal currentPrice;

    @Column(name = "change_1d", precision = 10, scale = 4)
    private BigDecimal change1d;

    @Column(name = "change_1w", precision = 10, scale = 4)
    private BigDecimal change1w;

    @Column(name = "change_1m", precision = 10, scale = 4)
    private BigDecimal change1m;

    @Column(name = "change_ytd", precision = 10, scale = 4)
    private BigDecimal changeYtd;

    public void updateRates(BigDecimal currentPrice, BigDecimal change1d,
                            BigDecimal change1w, BigDecimal change1m, BigDecimal changeYtd) {
        this.currentPrice = currentPrice;
        this.change1d = change1d;
        this.change1w = change1w;
        this.change1m = change1m;
        this.changeYtd = changeYtd;
    }
}

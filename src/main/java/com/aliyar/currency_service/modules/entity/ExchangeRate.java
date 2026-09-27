package com.aliyar.currency_service.modules.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "exchange_rates",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uc_base_quote_date",
                        columnNames = {"base_currency", "quote_currency", "effective_at"}
                )
        }, indexes = {
        @Index(name = "idx_base_date", columnList = "base_currency, effective_at DESC"),
        @Index(name = "idx_quote_date", columnList = "quote_currency, effective_at DESC"),
        @Index(name = "idx_date_only", columnList = "effective_at DESC")
}
)
@Getter
@Setter
@NoArgsConstructor
@ToString
public class ExchangeRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "base_currency", nullable = false, length = 3)
    private String base;

    @Column(name = "quote_currency", nullable = false, length = 3)
    private String quote;

    @Column(nullable = false, precision = 19, scale = 6)
    private BigDecimal rate;

    @Column(name = "effective_at", nullable = false)
    private Instant effectiveAt;

    @Column(nullable = false, length = 50)
    private String providerName;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ExchangeRate that)) return false;

        return Objects.equals(base, that.getBase()) &&
                Objects.equals(quote, that.getQuote()) &&
                Objects.equals(effectiveAt, that.getEffectiveAt());
    }

    @Override
    public int hashCode() {
        return Objects.hash(base, quote, effectiveAt);
    }
}
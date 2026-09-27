package com.aliyar.currency_service.modules.repository;

import com.aliyar.currency_service.modules.entity.ExchangeRate;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Long> {

    @Query("""
            SELECT e
            FROM ExchangeRate e
            WHERE (e.base = :currency OR e.quote = :currency)
              AND e.effectiveAt BETWEEN :start AND :end
            ORDER BY e.effectiveAt DESC
            """)
    List<ExchangeRate> findExchangeRatesByCurrencyAndDateRange(
            @Param("currency") String currency,
            @Param("start") Instant start,
            @Param("end") Instant end,
            Pageable pageable
    );

    @Query(
            value = """
                    SELECT
                        a.effective_at AS effectiveAt,
                         ROUND(b.rate / a.rate, 6) AS rate
                    FROM exchange_rates a
                    JOIN exchange_rates b
                        ON date_trunc('day', a.effective_at) = date_trunc('day', b.effective_at)
                    WHERE a.base_currency = :bridge
                      AND a.quote_currency = :base
                      AND b.base_currency = :bridge
                      AND b.quote_currency = :quote
                      AND a.effective_at BETWEEN :start AND :end
                    ORDER BY a.effective_at DESC
                    """,
            countQuery = """
                    SELECT count(*)
                    FROM exchange_rates a
                    JOIN exchange_rates b
                        ON date_trunc('day', a.effective_at) = date_trunc('day', b.effective_at)
                    WHERE a.base_currency = :bridge
                      AND a.quote_currency = :base
                      AND b.base_currency = :bridge
                      AND b.quote_currency = :quote
                      AND a.effective_at BETWEEN :start AND :end
                    """,
            nativeQuery = true
    )
    List<CrossRatePoint> findCrossRateHistory(
            @Param("base") String base,
            @Param("quote") String quote,
            @Param("bridge") String bridge,
            @Param("start") Instant start,
            @Param("end") Instant end,
            Pageable pageable
    );


    @Modifying
    @Query(value = """
            INSERT INTO exchange_rates (base_currency, quote_currency, rate, effective_at, provider_name)
            VALUES (:base, :quote, :rate, :effectiveAt, :providerName)
            ON CONFLICT (base_currency, quote_currency, effective_at) DO NOTHING
            """, nativeQuery = true)
    void insertIfAbsent(
            @Param("base") String base,
            @Param("quote") String quote,
            @Param("rate") BigDecimal rate,
            @Param("effectiveAt") Instant effectiveAt,
            @Param("providerName") String providerName
    );

    List<ExchangeRate> findByBaseOrderByEffectiveAtDesc(String base, Pageable pageable);

    Optional<ExchangeRate> findFirstByBaseAndQuoteOrderByEffectiveAtDesc(String base, String quote);

    Optional<ExchangeRate> findFirstByOrderByEffectiveAtDesc();
}
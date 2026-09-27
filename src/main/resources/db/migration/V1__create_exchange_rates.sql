CREATE TABLE exchange_rates
(
    id BIGSERIAL PRIMARY KEY,

    base_currency VARCHAR(3) NOT NULL,
    quote_currency VARCHAR(3) NOT NULL,

    rate NUMERIC(19,6) NOT NULL,

    effective_at TIMESTAMP WITH TIME ZONE NOT NULL,

    provider_name VARCHAR(50) NOT NULL,

    CONSTRAINT uc_base_quote_date
        UNIQUE (base_currency, quote_currency, effective_at)
);

CREATE INDEX idx_base_date
    ON exchange_rates (base_currency, effective_at DESC);

CREATE INDEX idx_quote_date
    ON exchange_rates (quote_currency, effective_at DESC);

CREATE INDEX idx_date_only
    ON exchange_rates (effective_at DESC);
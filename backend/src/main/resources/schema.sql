CREATE TABLE IF NOT EXISTS news (
    id           BIGSERIAL PRIMARY KEY,
    url          VARCHAR(2048) NOT NULL UNIQUE,
    title        VARCHAR(500)  NOT NULL,
    description  TEXT,
    source       VARCHAR(100)  NOT NULL,
    published_at TIMESTAMP,
    collected_at TIMESTAMP     NOT NULL DEFAULT now()
);

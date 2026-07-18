CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS dish_embeddings (
    id BIGSERIAL PRIMARY KEY,
    dish_id BIGINT NOT NULL,
    dish_name VARCHAR(100) NOT NULL,
    category VARCHAR(50),
    taste VARCHAR(50),
    spiciness VARCHAR(20),
    price DECIMAL(10,2),
    description TEXT,
    embedding VECTOR(1024),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_dish_embeddings_dish_id ON dish_embeddings (dish_id);
CREATE INDEX IF NOT EXISTS idx_dish_embeddings_vector ON dish_embeddings USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);

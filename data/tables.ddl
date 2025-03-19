-- Drop existing objects if they exist
DROP INDEX IF EXISTS idx_capitals_id;
DROP INDEX IF EXISTS idx_capital_chunks_id;
DROP TABLE IF EXISTS capital_continents;
DROP TABLE IF EXISTS capital_chunks;
DROP TABLE IF EXISTS continents;
DROP TABLE IF EXISTS capitals;
DROP TYPE IF EXISTS embed_type;

-- Create enum type for embedding types
CREATE TYPE embed_type AS ENUM ('hierarchical', 'hypothetical', 'contextual', 'late');

-- Modified capitals table (without content, chunk, and embedding columns)
CREATE TABLE capitals (
                          capital_id SERIAL PRIMARY KEY,
                          capital VARCHAR(255) NOT NULL,
                          country VARCHAR(255) NOT NULL
);

-- Table for Continents
CREATE TABLE continents (
                            continent_id SERIAL PRIMARY KEY,
                            continent_name VARCHAR(255) UNIQUE NOT NULL
);

-- Linking table for Capitals and Continents (Many-to-Many)
CREATE TABLE capital_continents (
                                    capital_id INT NOT NULL,
                                    continent_id INT NOT NULL,
                                    PRIMARY KEY (capital_id, continent_id),
                                    FOREIGN KEY (capital_id) REFERENCES capitals(capital_id),
                                    FOREIGN KEY (continent_id) REFERENCES continents(continent_id)
);

-- New table for capital chunks with content, chunk, and embedding
CREATE TABLE capital_chunks (
                                chunk_id SERIAL PRIMARY KEY,
                                capital_id INT NOT NULL,
                                embed embed_type NOT NULL DEFAULT 'hypothetical',
                                content TEXT NOT NULL,
                                chunk TEXT NOT NULL,
                                embedding public.vector GENERATED ALWAYS AS (public.embedding('text-embedding-005'::text, content)) STORED,
                                FOREIGN KEY (capital_id) REFERENCES capitals(capital_id)
);

-- Create indexes
CREATE INDEX idx_capitals_id ON capitals (capital_id);
CREATE INDEX idx_capital_chunks_id ON capital_chunks (capital_id);


-- Insert a capital
INSERT INTO capitals (capital, country)
VALUES ('Ankara', 'Turkey');

-- Insert continents
INSERT INTO continents (continent_name)
VALUES ('Europe'), ('Asia'), ('Africa'), ('North America'), ('South America'), ('Oceania'), ('Antarctica')
    ON CONFLICT (continent_name) DO NOTHING;

-- Link capital to continents (for Ankara: Europe and Asia)
INSERT INTO capital_continents (capital_id, continent_id)
VALUES
    ((SELECT capital_id FROM capitals WHERE capital = 'Ankara'),
     (SELECT continent_id FROM continents WHERE continent_name = 'Europe')),
    ((SELECT capital_id FROM capitals WHERE capital = 'Ankara'),
     (SELECT continent_id FROM continents WHERE continent_name = 'Asia'));

-- Insert multiple chunks for the same capital
INSERT INTO capital_chunks (capital_id, embed, content, chunk)
VALUES
    ((SELECT capital_id FROM capitals WHERE capital = 'Ankara'),
     'contextual', 'Historical content about Ankara...', 'Ankara is the capital of Turkey and was made the capital in 1923...'),
    ((SELECT capital_id FROM capitals WHERE capital = 'Ankara'),
     'hypothetical', 'Geographical content about Ankara...', 'Ankara is located in central Anatolia and has a continental climate...'),
    ((SELECT capital_id FROM capitals WHERE capital = 'Ankara'),
     'contextual', 'Cultural content about Ankara...', 'The culture of Ankara reflects its position as Turkey's capital...');

-- Example SELECT to retrieve a capital with all its chunks and continents
SELECT
    c.capital_id,
    c.capital,
    c.country,
    cc.chunk_id,
    cc.embed,
    cc.content,
    cc.chunk,
    cont.continent_name
FROM
    capitals AS c
JOIN
    capital_chunks AS cc ON c.capital_id = cc.capital_id
JOIN
    capital_continents AS ccont ON c.capital_id = ccont.capital_id
JOIN
    continents AS cont ON ccont.continent_id = cont.continent_id
WHERE
    c.capital = 'Ankara';
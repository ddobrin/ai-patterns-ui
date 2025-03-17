DROP INDEX IF EXISTS idx_capitals_id;
DROP TABLE IF EXISTS capital_continents;
DROP TABLE IF EXISTS continents;
DROP TABLE IF EXISTS capitals;
DROP TYPE IF EXISTS embed_type;

CREATE TYPE embed_type AS ENUM ('hierarchical', 'hypothetical', 'contextual', 'late');

-- Modified capitals table (continent column removed)
create TABLE capitals (
                          capital_id SERIAL PRIMARY KEY,
                          capital VARCHAR(255) NOT NULL,
                          country VARCHAR(255) NOT NULL,
                          embed embed_type NOT NULL DEFAULT 'hypothetical',
                          content VARCHAR(255) NOT NULL,
                          chunk VARCHAR(255) NOT NULL,
                          embedding public.vector GENERATED ALWAYS AS (public.embedding('text-embedding-005'::text, chunk)) STORED
);

-- Table for Continents
CREATE TABLE continents (
                            continent_id SERIAL PRIMARY KEY,
                            continent_name VARCHAR(255) UNIQUE NOT NULL -- Unique continent names
);

-- Linking table for Capitals and Continents (Many-to-Many)
CREATE TABLE capital_continents (
                                    capital_id INT NOT NULL,
                                    continent_id INT NOT NULL,
                                    PRIMARY KEY (capital_id, continent_id), -- Composite primary key
                                    FOREIGN KEY (capital_id) REFERENCES capitals(capital_id),
                                    FOREIGN KEY (continent_id) REFERENCES continents(continent_id)
);

CREATE INDEX idx_capitals_id ON capitals (capital_id);

--
-- Insert into the continents table (if it doesn't already exist)
INSERT INTO continents (continent_name)
SELECT 'Europe'
    WHERE NOT EXISTS (SELECT 1 FROM continents WHERE continent_name = 'Europe');

INSERT INTO continents (continent_name)
SELECT 'Asia'
    WHERE NOT EXISTS (SELECT 1 FROM continents WHERE continent_name = 'Asia');

INSERT INTO continents (continent_name)
SELECT 'Africa'
    WHERE NOT EXISTS (SELECT 1 FROM continents WHERE continent_name = 'Africa');

INSERT INTO continents (continent_name)
SELECT 'Antarctica'
    WHERE NOT EXISTS (SELECT 1 FROM continents WHERE continent_name = 'Antarctica');

INSERT INTO continents (continent_name)
SELECT 'Oceania'
    WHERE NOT EXISTS (SELECT 1 FROM continents WHERE continent_name = 'Oceania');

INSERT INTO continents (continent_name)
SELECT 'North America'
    WHERE NOT EXISTS (SELECT 1 FROM continents WHERE continent_name = 'North America');

INSERT INTO continents (continent_name)
SELECT 'South America'
    WHERE NOT EXISTS (SELECT 1 FROM continents WHERE continent_name = 'South America');

-- ...

INSERT INTO capitals (capital, country, content, chunk)
VALUES ('Ankara', 'Turkey', 'Content about Ankara...', 'Chunk of text...');

INSERT INTO capitals (capital, country, content, chunk)
VALUES ('Paris', 'France', 'Content about Paris...', 'Chunk about Paris...');

-- Ankara (Turkey) in Europe and Asia
INSERT INTO capital_continents (capital_id, continent_id)
VALUES ((SELECT capital_id FROM capitals WHERE capital = 'Ankara'), (SELECT continent_id FROM continents WHERE continent_name = 'Europe'));
INSERT INTO capital_continents (capital_id, continent_id)
VALUES ((SELECT capital_id FROM capitals WHERE capital = 'Ankara'), (SELECT continent_id FROM continents WHERE continent_name = 'Asia'));

-- Paris (France) in Europe
INSERT INTO capital_continents (capital_id, continent_id)
VALUES ((SELECT capital_id FROM capitals WHERE capital = 'Paris'), (SELECT continent_id FROM continents WHERE continent_name = 'Europe'));


-- SELECT data from the DB
SELECT
    cc.chunk_id,
    c.capital,
    c.country,
    cc.embed::text,
        cc.content,
    cc.chunk,
    string_agg(cont.continent_name, ', ') AS continents,
    cc.embedding <-> public.embedding('text-embedding-005', 'population of Berlin')::vector AS distance
FROM
    capital_chunks cc
        JOIN
    capitals c ON cc.capital_id = c.capital_id
        LEFT JOIN
    capital_continents cc_rel ON c.capital_id = cc_rel.capital_id
        LEFT JOIN
    continents cont ON cc_rel.continent_id = cont.continent_id
WHERE
    cc.embed = 'hypothetical'
GROUP BY
    cc.chunk_id,
    c.capital,
    c.country,
    cc.embed,
    cc.content,
    cc.chunk,
    cc.embedding
ORDER BY
    distance ASC
    LIMIT 10;

--- parameterized SQL query
PREPARE embedding_search(text, embed_type) AS
SELECT
    cc.chunk_id,
    c.capital,
    c.country,
    cc.embed::text,
        cc.content,
    cc.chunk,
    string_agg(cont.continent_name, ', ') AS continents,
    cc.embedding <-> public.embedding('text-embedding-005', $1)::vector AS distance
FROM
    capital_chunks cc
        JOIN
    capitals c ON cc.capital_id = c.capital_id
        LEFT JOIN
    capital_continents cc_rel ON c.capital_id = cc_rel.capital_id
        LEFT JOIN
    continents cont ON cc_rel.continent_id = cont.continent_id
WHERE
    cc.embed = $2
GROUP BY
    cc.chunk_id,
    c.capital,
    c.country,
    cc.embed,
    cc.content,
    cc.chunk,
    cc.embedding
ORDER BY
    distance ASC
    LIMIT 10;

-- Execute with specific parameters:
EXECUTE embedding_search('population of Berlin', 'hypothetical');
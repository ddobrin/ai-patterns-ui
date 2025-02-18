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
INSERT INTO continents (continent_name) VALUES ('Europe');
INSERT INTO continents (continent_name) VALUES ('Asia');
INSERT INTO continents (continent_name) VALUES ('North America');
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




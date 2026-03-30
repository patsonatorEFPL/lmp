-- Coordonnées postales éditables depuis l'admin (factures, etc.)
CREATE TABLE company_profile (
    id           SMALLINT PRIMARY KEY CHECK (id = 1),
    address_line VARCHAR(500) NOT NULL,
    city_region  VARCHAR(200) NOT NULL,
    updated_at   TIMESTAMP    NOT NULL DEFAULT NOW()
);

INSERT INTO company_profile (id, address_line, city_region, updated_at)
VALUES (
    1,
    '123 Rue Principale, Ville, Province, Code Postal',
    'Ville, Province',
    NOW()
)
ON CONFLICT (id) DO NOTHING;

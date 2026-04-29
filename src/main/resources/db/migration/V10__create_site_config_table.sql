-- Table de configuration du site (modifiable à chaud sans redémarrage)
CREATE TABLE site_config (
    config_key VARCHAR(128) PRIMARY KEY,
    config_value VARCHAR(4000),
    config_description VARCHAR(500),
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Index pour les recherches rapides
CREATE INDEX idx_site_config_key ON site_config(config_key);

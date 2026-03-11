-- Migration pour ajouter le support des rendez-vous anonymes
-- Ajout des champs client pour les rendez-vous sans utilisateur connecté

-- Modifier la contrainte foreign key pour permettre user_id NULL
ALTER TABLE appointments MODIFY COLUMN user_id BIGINT NULL;

-- Ajouter les colonnes pour les informations client anonyme
ALTER TABLE appointments 
ADD COLUMN client_name VARCHAR(100) NULL COMMENT 'Nom du client pour les rendez-vous anonymes',
ADD COLUMN client_email VARCHAR(100) NULL COMMENT 'Email du client pour les rendez-vous anonymes',
ADD COLUMN client_phone VARCHAR(20) NULL COMMENT 'Téléphone du client pour les rendez-vous anonymes';

-- Ajouter un index sur client_email pour les recherches
CREATE INDEX idx_appointments_client_email ON appointments(client_email);

-- Ajouter une contrainte pour s'assurer qu'au moins un utilisateur ou des infos client sont présents
ALTER TABLE appointments 
ADD CONSTRAINT chk_appointment_has_client 
CHECK (
    user_id IS NOT NULL OR 
    (client_name IS NOT NULL AND client_email IS NOT NULL AND client_phone IS NOT NULL)
);

-- =============================================
-- V23 : Support paiement en plusieurs fois (installments)
-- =============================================
-- Ajoute le support des échéances de paiement sur les commandes.
-- Modèle aligné sur external ERP Payment Terms / Payment Schedule.

-- Nombre d'échéances choisi par le client (2, 3, 4) — null = paiement unique
ALTER TABLE orders ADD COLUMN IF NOT EXISTS installment_count INTEGER;

-- Nom du Payment Terms Template external ERP associé (ex: "Paiement en 3x")
ALTER TABLE orders ADD COLUMN IF NOT EXISTS payment_terms_template VARCHAR(140);

-- Table enfant : une ligne par échéance
CREATE TABLE IF NOT EXISTS order_installments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,

    -- Numéro d'échéance (1, 2, 3…)
    installment_number INTEGER NOT NULL,

    -- Nom du Payment Term external ERP (ex: "1ère échéance")
    payment_term VARCHAR(140) NOT NULL,

    -- Pourcentage de la facture (ex: 33.33)
    invoice_portion NUMERIC(6, 2) NOT NULL,

    -- Montant calculé pour cette échéance
    amount NUMERIC(10, 2) NOT NULL,

    -- Date d'échéance prévue
    due_date DATE NOT NULL,

    -- Statut du paiement de cette échéance
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',

    -- Stripe PaymentIntent ID pour cette échéance (chaque échéance a le sien)
    stripe_payment_intent_id VARCHAR(255),

    -- ID du Payment Entry créé dans le système externe
    external_payment_id VARCHAR(140),

    -- Date effective du paiement
    paid_at TIMESTAMP,

    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),

    CONSTRAINT uq_order_installment UNIQUE (order_id, installment_number)
);

CREATE INDEX IF NOT EXISTS idx_order_installments_order_id ON order_installments(order_id);
CREATE INDEX IF NOT EXISTS idx_order_installments_status ON order_installments(status);
CREATE INDEX IF NOT EXISTS idx_order_installments_due_date ON order_installments(due_date);

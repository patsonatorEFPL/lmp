-- Snapshot FX stocké sur chaque commande pour traçabilité complète.
ALTER TABLE orders ADD COLUMN IF NOT EXISTS amount_base_eur  NUMERIC(10,2)  NULL;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS fx_rate          NUMERIC(18,6)  NULL;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS fx_source        VARCHAR(32)    NULL;

COMMENT ON COLUMN orders.amount_base_eur IS 'Montant en EUR au moment de la création (devise de base du catalogue)';
COMMENT ON COLUMN orders.fx_rate         IS 'Taux de change EUR→currency appliqué (effectif, avec marge FX)';
COMMENT ON COLUMN orders.fx_source       IS 'Origine du taux : frankfurter | static';

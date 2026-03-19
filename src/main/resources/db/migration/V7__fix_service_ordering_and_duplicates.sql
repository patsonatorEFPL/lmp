-- =============================================
-- V7: Fix duplicate displayOrder values and remove duplicate "Création Site Web"
--
-- Issues identified in audit (18 mars 2026):
-- 1. Multiple services share the same displayOrder (orders 1-6 all duplicated)
-- 2. Two services named "Création Site Web" (slugs: creation-site-web-v5 and creation-site-web)
--
-- Resolution:
-- 1. Deactivate + unfeature the older "Création Site Web" (slug: creation-site-web)
--    We keep it in DB to preserve FK integrity with any existing order_items.
-- 2. Reassign unique sequential displayOrder values to all remaining active services
-- =============================================

-- Step 1: Deactivate the duplicate "Création Site Web" (slug = 'creation-site-web')
-- Keep the V5 version (creation-site-web-v5) which has better description and benefits
UPDATE services
SET active = false, featured = false, display_order = 99, updated_at = NOW()
WHERE slug = 'creation-site-web';

-- Step 2: Assign unique displayOrder values (1-14) to active services
-- Ordered by strategic business importance:
-- Security/Access → Core SEO → Premium SEO → Reputation → Social → Local →
-- Advanced SEO → Web Dev → E-commerce → Design → Ads → Premium Bundle → Support → Promo

UPDATE services SET display_order = 1,  updated_at = NOW() WHERE slug = 'securisations-acces-gmb';
UPDATE services SET display_order = 2,  updated_at = NOW() WHERE slug = 'referencement-seo';
UPDATE services SET display_order = 3,  updated_at = NOW() WHERE slug = 'ref-optimale-vip';
UPDATE services SET display_order = 4,  updated_at = NOW() WHERE slug = 'gestion-avis';
UPDATE services SET display_order = 5,  updated_at = NOW() WHERE slug = 'gestion-reseaux-sociaux';
UPDATE services SET display_order = 6,  updated_at = NOW() WHERE slug = 'presence-locales';
UPDATE services SET display_order = 7,  updated_at = NOW() WHERE slug = 'seo-naturel';
UPDATE services SET display_order = 8,  updated_at = NOW() WHERE slug = 'creation-site-web-v5';
UPDATE services SET display_order = 9,  updated_at = NOW() WHERE slug = 'boutique-e-commerce';
UPDATE services SET display_order = 10, updated_at = NOW() WHERE slug = 'logo-identite-visuelle';
UPDATE services SET display_order = 11, updated_at = NOW() WHERE slug = 'publicite-en-ligne';
UPDATE services SET display_order = 12, updated_at = NOW() WHERE slug = 'mise-jour-2026';
UPDATE services SET display_order = 13, updated_at = NOW() WHERE slug = 'assistance-technique';
UPDATE services SET display_order = 14, updated_at = NOW() WHERE slug = 'black-friday';

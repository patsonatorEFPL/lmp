-- =============================================
-- V5: Seed default service categories, services, offers and benefits
-- Idempotent: uses ON CONFLICT DO NOTHING and conditional inserts
-- Uses subqueries for category_id to avoid FK constraint issues
-- when categories already exist from DataInitializer with different UUIDs
-- =============================================

-- ===== CATEGORIES (unique on slug) =====

INSERT INTO service_categories (id, name, slug, description, icon, display_order) VALUES
    (gen_random_uuid(), 'Marketing Digital', 'marketing-digital', 'Services de marketing digital et publicité en ligne', '📈', 1),
    (gen_random_uuid(), 'Développement Web', 'developpement-web', 'Création et développement de sites web et applications', '💻', 2),
    (gen_random_uuid(), 'Design & Branding', 'design-branding', 'Identité visuelle, logos et design graphique', '🎨', 3)
ON CONFLICT (slug) DO NOTHING;

-- ===== SERVICES (unique on slug, using subqueries for category_id) =====

INSERT INTO services (id, category_id, title, slug, description, icon, display_order, featured, active, created_at, updated_at)
SELECT gen_random_uuid(), sc.id, 'Référencement SEO', 'referencement-seo',
       'Optimisez votre visibilité sur Google et les moteurs de recherche. Audit complet, stratégie de mots-clés, optimisation on-page et off-page pour dominer les résultats de recherche.',
       '🔍', 1, true, true, NOW(), NOW()
FROM service_categories sc WHERE sc.slug = 'marketing-digital'
AND NOT EXISTS (SELECT 1 FROM services WHERE slug = 'referencement-seo');

INSERT INTO services (id, category_id, title, slug, description, icon, display_order, featured, active, created_at, updated_at)
SELECT gen_random_uuid(), sc.id, 'Gestion Réseaux Sociaux', 'gestion-reseaux-sociaux',
       'Développez votre présence sur les réseaux sociaux avec une stratégie sur mesure. Création de contenu, planification, engagement communautaire et analyse des performances.',
       '📱', 2, true, true, NOW(), NOW()
FROM service_categories sc WHERE sc.slug = 'marketing-digital'
AND NOT EXISTS (SELECT 1 FROM services WHERE slug = 'gestion-reseaux-sociaux');

INSERT INTO services (id, category_id, title, slug, description, icon, display_order, featured, active, created_at, updated_at)
SELECT gen_random_uuid(), sc.id, 'Création Site Web', 'creation-site-web-v5',
       'Site web professionnel, responsive et optimisé. Design moderne, expérience utilisateur soignée et performances maximales pour convertir vos visiteurs en clients.',
       '🌐', 3, true, true, NOW(), NOW()
FROM service_categories sc WHERE sc.slug = 'developpement-web'
AND NOT EXISTS (SELECT 1 FROM services WHERE slug = 'creation-site-web-v5')
AND NOT EXISTS (SELECT 1 FROM services WHERE slug = 'creation-site-web');

INSERT INTO services (id, category_id, title, slug, description, icon, display_order, featured, active, created_at, updated_at)
SELECT gen_random_uuid(), sc.id, 'Boutique E-commerce', 'boutique-e-commerce',
       'Lancez votre boutique en ligne avec une plateforme e-commerce complète. Paiement sécurisé, gestion des stocks, et interface d''achat optimisée pour maximiser vos ventes.',
       '🛒', 4, false, true, NOW(), NOW()
FROM service_categories sc WHERE sc.slug = 'developpement-web'
AND NOT EXISTS (SELECT 1 FROM services WHERE slug = 'boutique-e-commerce');

INSERT INTO services (id, category_id, title, slug, description, icon, display_order, featured, active, created_at, updated_at)
SELECT gen_random_uuid(), sc.id, 'Logo & Identité Visuelle', 'logo-identite-visuelle',
       'Créez une identité de marque forte et mémorable. Logo professionnel, charte graphique complète et supports de communication cohérents.',
       '✨', 5, true, true, NOW(), NOW()
FROM service_categories sc WHERE sc.slug = 'design-branding'
AND NOT EXISTS (SELECT 1 FROM services WHERE slug = 'logo-identite-visuelle');

INSERT INTO services (id, category_id, title, slug, description, icon, display_order, featured, active, created_at, updated_at)
SELECT gen_random_uuid(), sc.id, 'Publicité en Ligne', 'publicite-en-ligne',
       'Campagnes publicitaires ciblées sur Google Ads, Facebook Ads et Instagram. Maximisez votre ROI avec des stratégies data-driven et un suivi des conversions précis.',
       '📣', 6, false, true, NOW(), NOW()
FROM service_categories sc WHERE sc.slug = 'marketing-digital'
AND NOT EXISTS (SELECT 1 FROM services WHERE slug = 'publicite-en-ligne');

-- ===== SERVICE BENEFITS (only for newly inserted V5 services) =====

INSERT INTO service_benefits (id, service_id, benefit)
SELECT gen_random_uuid(), s.id, b.benefit
FROM services s
CROSS JOIN (VALUES
    ('Audit SEO complet de votre site'),
    ('Recherche et stratégie de mots-clés'),
    ('Optimisation on-page et technique'),
    ('Rapport de positionnement mensuel')
) AS b(benefit)
WHERE s.slug = 'referencement-seo'
AND NOT EXISTS (SELECT 1 FROM service_benefits sb WHERE sb.service_id = s.id AND sb.benefit = b.benefit);

INSERT INTO service_benefits (id, service_id, benefit)
SELECT gen_random_uuid(), s.id, b.benefit
FROM services s
CROSS JOIN (VALUES
    ('Stratégie de contenu personnalisée'),
    ('Création de 20 publications/mois'),
    ('Gestion de la communauté'),
    ('Analyse de performance mensuelle')
) AS b(benefit)
WHERE s.slug = 'gestion-reseaux-sociaux'
AND NOT EXISTS (SELECT 1 FROM service_benefits sb WHERE sb.service_id = s.id AND sb.benefit = b.benefit);

INSERT INTO service_benefits (id, service_id, benefit)
SELECT gen_random_uuid(), s.id, b.benefit
FROM services s
CROSS JOIN (VALUES
    ('Design responsive et moderne'),
    ('Optimisé pour le référencement'),
    ('Hébergement inclus 1 an'),
    ('Formation à la gestion du contenu')
) AS b(benefit)
WHERE s.slug IN ('creation-site-web-v5', 'creation-site-web')
AND NOT EXISTS (SELECT 1 FROM service_benefits sb WHERE sb.service_id = s.id AND sb.benefit = b.benefit);

INSERT INTO service_benefits (id, service_id, benefit)
SELECT gen_random_uuid(), s.id, b.benefit
FROM services s
CROSS JOIN (VALUES
    ('Paiement sécurisé (Stripe)'),
    ('Gestion des produits et stocks'),
    ('Tableau de bord analytique'),
    ('Intégration livraison')
) AS b(benefit)
WHERE s.slug = 'boutique-e-commerce'
AND NOT EXISTS (SELECT 1 FROM service_benefits sb WHERE sb.service_id = s.id AND sb.benefit = b.benefit);

INSERT INTO service_benefits (id, service_id, benefit)
SELECT gen_random_uuid(), s.id, b.benefit
FROM services s
CROSS JOIN (VALUES
    ('3 propositions de logo'),
    ('Charte graphique complète'),
    ('Fichiers haute résolution (tous formats)'),
    ('Guide d''utilisation de la marque')
) AS b(benefit)
WHERE s.slug = 'logo-identite-visuelle'
AND NOT EXISTS (SELECT 1 FROM service_benefits sb WHERE sb.service_id = s.id AND sb.benefit = b.benefit);

INSERT INTO service_benefits (id, service_id, benefit)
SELECT gen_random_uuid(), s.id, b.benefit
FROM services s
CROSS JOIN (VALUES
    ('Création et gestion de campagnes'),
    ('Ciblage avancé par audience'),
    ('Optimisation du budget publicitaire'),
    ('Rapports de performance détaillés')
) AS b(benefit)
WHERE s.slug = 'publicite-en-ligne'
AND NOT EXISTS (SELECT 1 FROM service_benefits sb WHERE sb.service_id = s.id AND sb.benefit = b.benefit);

-- ===== SERVICE OFFERS (default pricing, only for V5 services that exist) =====

INSERT INTO service_offers (id, service_id, name, price, original_price, duration_type, duration, is_default, active)
SELECT gen_random_uuid(), s.id, 'SEO Mensuel', 499.00, 699.00, 'MONTHLY', 'MONTHLY', true, true
FROM services s WHERE s.slug = 'referencement-seo'
AND NOT EXISTS (SELECT 1 FROM service_offers so WHERE so.service_id = s.id AND so.is_default = true);

INSERT INTO service_offers (id, service_id, name, price, original_price, duration_type, duration, is_default, active)
SELECT gen_random_uuid(), s.id, 'Social Media Mensuel', 399.00, 549.00, 'MONTHLY', 'MONTHLY', true, true
FROM services s WHERE s.slug = 'gestion-reseaux-sociaux'
AND NOT EXISTS (SELECT 1 FROM service_offers so WHERE so.service_id = s.id AND so.is_default = true);

INSERT INTO service_offers (id, service_id, name, price, original_price, duration_type, duration, is_default, active)
SELECT gen_random_uuid(), s.id, 'Site Web Vitrine', 1499.00, 1999.00, 'ONE_TIME', 'ONE_TIME', true, true
FROM services s WHERE s.slug IN ('creation-site-web-v5', 'creation-site-web')
AND NOT EXISTS (SELECT 1 FROM service_offers so WHERE so.service_id = s.id AND so.is_default = true);

INSERT INTO service_offers (id, service_id, name, price, original_price, duration_type, duration, is_default, active)
SELECT gen_random_uuid(), s.id, 'Boutique E-commerce', 2999.00, 3999.00, 'ONE_TIME', 'ONE_TIME', true, true
FROM services s WHERE s.slug = 'boutique-e-commerce'
AND NOT EXISTS (SELECT 1 FROM service_offers so WHERE so.service_id = s.id AND so.is_default = true);

INSERT INTO service_offers (id, service_id, name, price, original_price, duration_type, duration, is_default, active)
SELECT gen_random_uuid(), s.id, 'Pack Identité Visuelle', 799.00, 999.00, 'ONE_TIME', 'ONE_TIME', true, true
FROM services s WHERE s.slug = 'logo-identite-visuelle'
AND NOT EXISTS (SELECT 1 FROM service_offers so WHERE so.service_id = s.id AND so.is_default = true);

INSERT INTO service_offers (id, service_id, name, price, original_price, duration_type, duration, is_default, active)
SELECT gen_random_uuid(), s.id, 'Publicité Mensuelle', 599.00, NULL, 'MONTHLY', 'MONTHLY', true, true
FROM services s WHERE s.slug = 'publicite-en-ligne'
AND NOT EXISTS (SELECT 1 FROM service_offers so WHERE so.service_id = s.id AND so.is_default = true);

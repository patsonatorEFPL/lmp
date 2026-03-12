-- =============================================
-- V5: Seed default service categories, services, offers and benefits
-- =============================================

-- ===== CATEGORIES =====

INSERT INTO service_categories (id, name, slug, description, icon, display_order) VALUES
    ('a1000000-0000-0000-0000-000000000001', 'Marketing Digital', 'marketing-digital', 'Services de marketing digital et publicité en ligne', '📈', 1),
    ('a1000000-0000-0000-0000-000000000002', 'Développement Web', 'developpement-web', 'Création et développement de sites web et applications', '💻', 2),
    ('a1000000-0000-0000-0000-000000000003', 'Design & Branding', 'design-branding', 'Identité visuelle, logos et design graphique', '🎨', 3);

-- ===== SERVICES =====

-- Service 1: SEO
INSERT INTO services (id, category_id, title, slug, description, icon, display_order, featured, active, created_at, updated_at) VALUES
    ('b1000000-0000-0000-0000-000000000001',
     'a1000000-0000-0000-0000-000000000001',
     'Référencement SEO',
     'referencement-seo',
     'Optimisez votre visibilité sur Google et les moteurs de recherche. Audit complet, stratégie de mots-clés, optimisation on-page et off-page pour dominer les résultats de recherche.',
     '🔍', 1, true, true, NOW(), NOW());

-- Service 2: Social Media
INSERT INTO services (id, category_id, title, slug, description, icon, display_order, featured, active, created_at, updated_at) VALUES
    ('b1000000-0000-0000-0000-000000000002',
     'a1000000-0000-0000-0000-000000000001',
     'Gestion Réseaux Sociaux',
     'gestion-reseaux-sociaux',
     'Développez votre présence sur les réseaux sociaux avec une stratégie sur mesure. Création de contenu, planification, engagement communautaire et analyse des performances.',
     '📱', 2, true, true, NOW(), NOW());

-- Service 3: Site Web
INSERT INTO services (id, category_id, title, slug, description, icon, display_order, featured, active, created_at, updated_at) VALUES
    ('b1000000-0000-0000-0000-000000000003',
     'a1000000-0000-0000-0000-000000000002',
     'Création Site Web',
     'creation-site-web',
     'Site web professionnel, responsive et optimisé. Design moderne, expérience utilisateur soignée et performances maximales pour convertir vos visiteurs en clients.',
     '🌐', 3, true, true, NOW(), NOW());

-- Service 4: E-commerce
INSERT INTO services (id, category_id, title, slug, description, icon, display_order, featured, active, created_at, updated_at) VALUES
    ('b1000000-0000-0000-0000-000000000004',
     'a1000000-0000-0000-0000-000000000002',
     'Boutique E-commerce',
     'boutique-e-commerce',
     'Lancez votre boutique en ligne avec une plateforme e-commerce complète. Paiement sécurisé, gestion des stocks, et interface d''achat optimisée pour maximiser vos ventes.',
     '🛒', 4, false, true, NOW(), NOW());

-- Service 5: Logo & Branding
INSERT INTO services (id, category_id, title, slug, description, icon, display_order, featured, active, created_at, updated_at) VALUES
    ('b1000000-0000-0000-0000-000000000005',
     'a1000000-0000-0000-0000-000000000003',
     'Logo & Identité Visuelle',
     'logo-identite-visuelle',
     'Créez une identité de marque forte et mémorable. Logo professionnel, charte graphique complète et supports de communication cohérents.',
     '✨', 5, true, true, NOW(), NOW());

-- Service 6: Publicité en ligne
INSERT INTO services (id, category_id, title, slug, description, icon, display_order, featured, active, created_at, updated_at) VALUES
    ('b1000000-0000-0000-0000-000000000006',
     'a1000000-0000-0000-0000-000000000001',
     'Publicité en Ligne',
     'publicite-en-ligne',
     'Campagnes publicitaires ciblées sur Google Ads, Facebook Ads et Instagram. Maximisez votre ROI avec des stratégies data-driven et un suivi des conversions précis.',
     '📣', 6, false, true, NOW(), NOW());

-- ===== SERVICE BENEFITS =====

-- SEO Benefits
INSERT INTO service_benefits (id, service_id, benefit) VALUES
    (gen_random_uuid(), 'b1000000-0000-0000-0000-000000000001', 'Audit SEO complet de votre site'),
    (gen_random_uuid(), 'b1000000-0000-0000-0000-000000000001', 'Recherche et stratégie de mots-clés'),
    (gen_random_uuid(), 'b1000000-0000-0000-0000-000000000001', 'Optimisation on-page et technique'),
    (gen_random_uuid(), 'b1000000-0000-0000-0000-000000000001', 'Rapport de positionnement mensuel');

-- Social Media Benefits
INSERT INTO service_benefits (id, service_id, benefit) VALUES
    (gen_random_uuid(), 'b1000000-0000-0000-0000-000000000002', 'Stratégie de contenu personnalisée'),
    (gen_random_uuid(), 'b1000000-0000-0000-0000-000000000002', 'Création de 20 publications/mois'),
    (gen_random_uuid(), 'b1000000-0000-0000-0000-000000000002', 'Gestion de la communauté'),
    (gen_random_uuid(), 'b1000000-0000-0000-0000-000000000002', 'Analyse de performance mensuelle');

-- Site Web Benefits
INSERT INTO service_benefits (id, service_id, benefit) VALUES
    (gen_random_uuid(), 'b1000000-0000-0000-0000-000000000003', 'Design responsive et moderne'),
    (gen_random_uuid(), 'b1000000-0000-0000-0000-000000000003', 'Optimisé pour le référencement'),
    (gen_random_uuid(), 'b1000000-0000-0000-0000-000000000003', 'Hébergement inclus 1 an'),
    (gen_random_uuid(), 'b1000000-0000-0000-0000-000000000003', 'Formation à la gestion du contenu');

-- E-commerce Benefits
INSERT INTO service_benefits (id, service_id, benefit) VALUES
    (gen_random_uuid(), 'b1000000-0000-0000-0000-000000000004', 'Paiement sécurisé (Stripe)'),
    (gen_random_uuid(), 'b1000000-0000-0000-0000-000000000004', 'Gestion des produits et stocks'),
    (gen_random_uuid(), 'b1000000-0000-0000-0000-000000000004', 'Tableau de bord analytique'),
    (gen_random_uuid(), 'b1000000-0000-0000-0000-000000000004', 'Intégration livraison');

-- Logo & Branding Benefits
INSERT INTO service_benefits (id, service_id, benefit) VALUES
    (gen_random_uuid(), 'b1000000-0000-0000-0000-000000000005', '3 propositions de logo'),
    (gen_random_uuid(), 'b1000000-0000-0000-0000-000000000005', 'Charte graphique complète'),
    (gen_random_uuid(), 'b1000000-0000-0000-0000-000000000005', 'Fichiers haute résolution (tous formats)'),
    (gen_random_uuid(), 'b1000000-0000-0000-0000-000000000005', 'Guide d''utilisation de la marque');

-- Publicité en ligne Benefits
INSERT INTO service_benefits (id, service_id, benefit) VALUES
    (gen_random_uuid(), 'b1000000-0000-0000-0000-000000000006', 'Création et gestion de campagnes'),
    (gen_random_uuid(), 'b1000000-0000-0000-0000-000000000006', 'Ciblage avancé par audience'),
    (gen_random_uuid(), 'b1000000-0000-0000-0000-000000000006', 'Optimisation du budget publicitaire'),
    (gen_random_uuid(), 'b1000000-0000-0000-0000-000000000006', 'Rapports de performance détaillés');

-- ===== SERVICE OFFERS (default pricing) =====

INSERT INTO service_offers (id, service_id, name, price, original_price, duration_type, duration, is_default, active) VALUES
    (gen_random_uuid(), 'b1000000-0000-0000-0000-000000000001', 'SEO Mensuel', 499.00, 699.00, 'MONTHLY', 'MONTHLY', true, true),
    (gen_random_uuid(), 'b1000000-0000-0000-0000-000000000002', 'Social Media Mensuel', 399.00, 549.00, 'MONTHLY', 'MONTHLY', true, true),
    (gen_random_uuid(), 'b1000000-0000-0000-0000-000000000003', 'Site Web Vitrine', 1499.00, 1999.00, 'ONE_TIME', 'ONE_TIME', true, true),
    (gen_random_uuid(), 'b1000000-0000-0000-0000-000000000004', 'Boutique E-commerce', 2999.00, 3999.00, 'ONE_TIME', 'ONE_TIME', true, true),
    (gen_random_uuid(), 'b1000000-0000-0000-0000-000000000005', 'Pack Identité Visuelle', 799.00, 999.00, 'ONE_TIME', 'ONE_TIME', true, true),
    (gen_random_uuid(), 'b1000000-0000-0000-0000-000000000006', 'Publicité Mensuelle', 599.00, NULL, 'MONTHLY', 'MONTHLY', true, true);

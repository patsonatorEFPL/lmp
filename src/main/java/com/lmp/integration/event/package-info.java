/**
 * Event bus pour la communication inter-modules et l'intégration ERP.
 *
 * Pattern : Module → ApplicationEventPublisher → LmpBusinessEvent → ErpEventListener
 * Couplage faible : les modules métier ne nomment pas de fournisseur ERP précis.
 */
package com.lmp.integration.event;

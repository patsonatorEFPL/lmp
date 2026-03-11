/**
 * Event bus pour la communication inter-modules et l'intégration external ERP.
 *
 * Pattern : Module → ApplicationEventPublisher → LmpBusinessEvent → external ERPEventListener
 * Couplage faible garanti : aucun module ne connaît external ERP directement.
 */
package com.lmp.integration.event;

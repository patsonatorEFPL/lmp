/**
 * Event bus pour la communication inter-modules et l'intégration ERPNext.
 *
 * Pattern : Module → ApplicationEventPublisher → LmpBusinessEvent → ErpNextEventListener
 * Couplage faible garanti : aucun module ne connaît ERPNext directement.
 */
package com.lmp.integration.event;

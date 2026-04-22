package com.lmp.integration.sync;

/**
 * Direction d'un événement de synchronisation.
 */
public enum SyncDirection {
    /** LMP → système externe */
    OUTBOUND,
    /** Système externe → LMP */
    INBOUND
}

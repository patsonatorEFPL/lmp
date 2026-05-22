package com.lmp.shared.config.site;

import org.springframework.context.ApplicationEvent;

/**
 * Événement émis quand une valeur de configuration du site change.
 * Les listeners peuvent se mettre à jour sans redémarrage.
 */
public class SiteConfigChangedEvent extends ApplicationEvent {

    private final String key;
    private final String newValue;

    public SiteConfigChangedEvent(Object source, String key, String newValue) {
        super(source);
        this.key = key;
        this.newValue = newValue;
    }

    public String getKey() { return key; }
    public String getNewValue() { return newValue; }
}

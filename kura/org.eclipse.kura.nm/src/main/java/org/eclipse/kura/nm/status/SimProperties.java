package org.eclipse.kura.nm.status;

import org.freedesktop.dbus.interfaces.Properties;

public class SimProperties {

    private final Properties properties;
    private final boolean isActive;
    private final boolean isPrimary;

    public SimProperties(Properties properties, boolean isActive, boolean isPrimary) {
        this.properties = properties;
        this.isActive = isActive;
        this.isPrimary = isPrimary;
    }

    public Properties getProperties() {
        return this.properties;
    }

    public boolean isActive() {
        return this.isActive;
    }

    public boolean isPrimary() {
        return this.isPrimary;
    }

}

package org.eclipse.kura.nm.status;

import org.freedesktop.dbus.interfaces.Properties;

public class WirelessProperties {

    private final Properties deviceProperties;
    private final Properties wirelessDeviceProperties;

    public WirelessProperties(Properties deviceProps, Properties wirelessProps) {
        this.deviceProperties = deviceProps;
        this.wirelessDeviceProperties = wirelessProps;
    }

    public Properties getDeviceProperties() {
        return this.deviceProperties;
    }

    public Properties getWirelessDeviceProperties() {
        return this.wirelessDeviceProperties;
    }

}

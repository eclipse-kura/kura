package org.eclipse.kura.nm.status;

import java.util.Optional;

import org.eclipse.kura.nm.NMDeviceType;
import org.freedesktop.dbus.interfaces.Properties;

public class DevicePropertiesWrapper {

    private final Properties deviceProperties;
    private final Optional<Properties> deviceSpecificProperties;
    private final NMDeviceType deviceType;

    public DevicePropertiesWrapper(Properties deviceProps, Optional<Properties> wirelessProps, NMDeviceType deviceType) {
        this.deviceProperties = deviceProps;
        this.deviceSpecificProperties = wirelessProps;
        this.deviceType = deviceType;
    }

    public Properties getDeviceProperties() {
        return this.deviceProperties;
    }

    public Optional<Properties> getDeviceSpecificProperties() {
        return this.deviceSpecificProperties;
    }
    
    public NMDeviceType getDeviceType() { 
        return this.deviceType;
    }

}

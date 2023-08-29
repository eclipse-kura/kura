package org.eclipse.kura.nm.configuration.enterprise;

import java.util.Map;

import org.eclipse.kura.nm.NetworkProperties;
import org.freedesktop.dbus.types.Variant;

public abstract class NM8021xEapAndPhase2Configurator {

    protected NetworkProperties props;
    protected String deviceId;

    public NM8021xEapAndPhase2Configurator(NetworkProperties props, String deviceId) {
        this.props = props;
        this.deviceId = deviceId;
    }

    public abstract void writeConfigurationsToMap(Map<String, Variant<?>> settings);

    protected NetworkProperties getProps() {
        return props;
    }

    protected String getDeviceId() {
        return deviceId;
    }

}

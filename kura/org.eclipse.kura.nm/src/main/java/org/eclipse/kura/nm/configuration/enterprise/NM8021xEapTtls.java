package org.eclipse.kura.nm.configuration.enterprise;

import java.util.Map;

import org.eclipse.kura.nm.NetworkProperties;
import org.freedesktop.dbus.types.Variant;

public class NM8021xEapTtls extends NM8021xEapAndPhase2Configurator {

    public NM8021xEapTtls(NetworkProperties props, String deviceId) {
        super(props, deviceId);
    }

    @Override
    public void writeConfigurationsToMap(Map<String, Variant<?>> settings) {

    }
}

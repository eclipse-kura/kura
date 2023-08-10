package org.eclipse.kura.nm.configuration.enterprise;

import java.util.Map;

import org.eclipse.kura.nm.NetworkProperties;
import org.freedesktop.dbus.types.Variant;

public class NM8021xEapTtls extends NM8021xEapAndPhase2Configurator {

    public NM8021xEapTtls(NetworkProperties props, String deviceId, String propMode) {
        super(props, deviceId, propMode);
    }

    @Override
    public void writeConfigurationsToMap(Map<String, Variant<?>> settings) {

        String eap = props.get(String.class, "net.interface.%s.config.802-1x.eap", getDeviceId(), getPropMode().toLowerCase());
        settings.put("eap", new Variant<>(new String[] { eap }));

    }
}

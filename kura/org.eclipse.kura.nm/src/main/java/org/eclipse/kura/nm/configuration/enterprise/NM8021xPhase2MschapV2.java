package org.eclipse.kura.nm.configuration.enterprise;

import java.util.Map;

import org.eclipse.kura.configuration.Password;
import org.eclipse.kura.nm.NetworkProperties;
import org.freedesktop.dbus.types.Variant;

public class NM8021xPhase2MschapV2 extends NM8021xEapAndPhase2Configurator {

    public NM8021xPhase2MschapV2(NetworkProperties props, String deviceId, String propMode) {
        super(props, deviceId, propMode);
    }

    @Override
    public void writeConfigurationsToMap(Map<String, Variant<?>> settings) {

        String innerAuth = props.get(String.class, "net.interface.%s.config.802-1x.innerAuth", getDeviceId(),
                propMode.toLowerCase());
        settings.put("phase2-auth", new Variant<>(innerAuth));

        String identity = props.get(String.class, "net.interface.%s.config.802-1x.identity", getDeviceId(),
                propMode.toLowerCase());
        settings.put("identity", new Variant<>(identity));

        String password = props
                .get(Password.class, "net.interface.%s.config.802-1x.password", getDeviceId(),
                        getPropMode().toLowerCase())
                .toString();
        settings.put("password", new Variant<>(password));

    }

}

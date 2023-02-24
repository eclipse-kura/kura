package org.eclipse.kura.nm.status;

import java.util.List;
import java.util.Optional;

import org.freedesktop.dbus.interfaces.Properties;

public class AccessPointsProperties {

    private final Optional<Properties> activeAccessPoint;
    private final List<Properties> availableAccessPoints;

    public AccessPointsProperties(Optional<Properties> activeAP, List<Properties> availableAPs) {
        this.activeAccessPoint = activeAP;
        this.availableAccessPoints = availableAPs;
    }

    public Optional<Properties> getActiveAccessPoint() {
        return activeAccessPoint;
    }

    public List<Properties> getAvailableAccessPoints() {
        return availableAccessPoints;
    }

}

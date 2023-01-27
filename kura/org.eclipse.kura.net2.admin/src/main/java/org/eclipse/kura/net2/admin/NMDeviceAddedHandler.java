package org.eclipse.kura.net2.admin;

import org.freedesktop.NetworkManager;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.DBusSigHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NMDeviceAddedHandler implements DBusSigHandler<NetworkManager.DeviceAdded> {

    private static final Logger logger = LoggerFactory.getLogger(NMDeviceAddedHandler.class);

    public void handle(NetworkManager.DeviceAdded s) {
        try {
            NMDbusConnector nm = NMDbusConnector.getInstance();
            logger.info("New network device connected at {}", s.getDevicePath());
            nm.apply();
        } catch (DBusException e) {
            logger.error("Failed to handle DeviceAdded event for device: {}. Caused by:", s.getDevicePath(), e);
        }
    }
}

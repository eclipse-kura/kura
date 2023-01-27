package org.eclipse.kura.net2.admin;

import org.freedesktop.NetworkManager;
import org.freedesktop.dbus.interfaces.DBusSigHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NMDeviceAddedHandler implements DBusSigHandler<NetworkManager.DeviceAdded> {

    private static final Logger logger = LoggerFactory.getLogger(NMDeviceAddedHandler.class);

    public void handle(NetworkManager.DeviceAdded s) {
        logger.info("Device added: {}", s.getName());
        logger.info("On interface: {}", s.getInterface());
        logger.info("On path: {}", s.getDevicePath());
    }

}

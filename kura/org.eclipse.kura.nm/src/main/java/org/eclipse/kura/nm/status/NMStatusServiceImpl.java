package org.eclipse.kura.nm.status;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.eclipse.kura.net.NetInterface;
import org.eclipse.kura.net.NetInterfaceAddress;
import org.eclipse.kura.net.status.NetworkStatusService;
import org.eclipse.kura.nm.configuration.NMDbusConnector;
import org.freedesktop.dbus.exceptions.DBusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NMStatusServiceImpl implements NetworkStatusService {

    private static final Logger logger = LoggerFactory.getLogger(NMStatusServiceImpl.class);

    private NMDbusConnector nmDbusConnector;

    public void activate() throws DBusException {
        logger.debug("Activate NMStatusService...");
        this.nmDbusConnector = NMDbusConnector.getInstance();
    }

    public void update() {
        logger.info("Update TritonServerService...");
    }

    public void deactivate() {
        logger.debug("Deactivate NMStatusService...");
        this.nmDbusConnector.closeConnection();
    }

    @Override
    public List<NetInterface<NetInterfaceAddress>> getNetworkStatus() {
        List<String> availableInterfaces = getInterfaceNames();

        List<NetInterface<NetInterfaceAddress>> interfaceStatuses = new ArrayList<>();
        for (String iface : availableInterfaces) {
            NetInterface<NetInterfaceAddress> status = getNetworkStatus(iface);
            if (Objects.nonNull(status)) {
                interfaceStatuses.add(status);
            }
        }

        return interfaceStatuses;
    }

    @Override
    public NetInterface<NetInterfaceAddress> getNetworkStatus(String interfaceName) {
        try {
            return this.nmDbusConnector.getInterfaceStatus(interfaceName);
        } catch (DBusException e) {
            logger.warn("Could not retrieve status for \"{}\" interface from NM because: ", interfaceName, e);
        }

        return null;
    }

    @Override
    public List<String> getInterfaceNames() {
        List<String> interfaces = new ArrayList<>();
        try {
            interfaces = this.nmDbusConnector.getInterfaces();
        } catch (DBusException e) {
            logger.warn("Could not retrieve interfaces from NM because: ", e);
        }

        return interfaces;
    }

}

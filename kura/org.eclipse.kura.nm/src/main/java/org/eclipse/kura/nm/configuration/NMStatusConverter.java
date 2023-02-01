package org.eclipse.kura.nm.configuration;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

import org.eclipse.kura.core.net.EthernetInterfaceImpl;
import org.eclipse.kura.core.net.NetInterfaceAddressImpl;
import org.eclipse.kura.net.NetInterface;
import org.eclipse.kura.net.NetInterfaceAddress;
import org.eclipse.kura.net.NetInterfaceState;
import org.freedesktop.dbus.interfaces.Properties;
import org.freedesktop.dbus.types.UInt32;
import org.freedesktop.dbus.types.Variant;
import org.freedesktop.networkmanager.Device;
import org.freedesktop.networkmanager.settings.Connection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NMStatusConverter {

    private static final Logger logger = LoggerFactory.getLogger(NMStatusConverter.class);

    private static final String NM_DEVICE_BUS_NAME = "org.freedesktop.NetworkManager.Device";

    private static final EnumMap<NMDeviceState, NetInterfaceState> DEVICE_STATE_CONVERTER = initDeviceStateConverter();

    private NMStatusConverter() {
        throw new IllegalStateException("Utility class");
    }

    public static NetInterface<NetInterfaceAddress> buildEthernetStatus(String interfaceName, Device device,
            Properties properties, Optional<Connection> connection) {
        EthernetInterfaceImpl<NetInterfaceAddress> ethInterface = new EthernetInterfaceImpl<>(interfaceName);

        ethInterface.setVirtual(false);
        ethInterface.setLoopback(false);
        ethInterface.setAutoConnect(properties.Get(NM_DEVICE_BUS_NAME, "Autoconnect"));
        ethInterface.setFirmwareVersion(properties.Get(NM_DEVICE_BUS_NAME, "FirmwareVersion"));
        ethInterface.setDriver(properties.Get(NM_DEVICE_BUS_NAME, "Driver"));
        ethInterface.setDriverVersion(properties.Get(NM_DEVICE_BUS_NAME, "DriverVersion"));

        NMDeviceState deviceState = NMDeviceState.fromUInt32(properties.Get(NM_DEVICE_BUS_NAME, "State"));
        ethInterface.setState(DEVICE_STATE_CONVERTER.get(deviceState));
        ethInterface.setUp(NMDeviceState.isConnected(deviceState));
        ethInterface.setLinkUp(NMDeviceState.isConnected(deviceState));

        ethInterface.setPointToPoint(false); // TBD

        UInt32 mtu = properties.Get(NM_DEVICE_BUS_NAME, "Mtu");
        ethInterface.setMTU(mtu.intValue());

        String hwAddress = properties.Get(NM_DEVICE_BUS_NAME, "HwAddress");
        ethInterface.setHardwareAddress(hwAddress.getBytes());

        if (connection.isPresent()) {
            Map<String, Map<String, Variant<?>>> connectionSettings = connection.get().GetSettings();

            NetInterfaceAddressImpl address = new NetInterfaceAddressImpl();
            // WIP
            address.setAddress(null);
            address.setBroadcast(null);
            address.setGateway(null);
            address.setNetworkPrefixLength((short) 0);
            address.setNetmask(null);
            address.setDnsServers(null);
        }

        return ethInterface;

    }

    private static EnumMap<NMDeviceState, NetInterfaceState> initDeviceStateConverter() {
        EnumMap<NMDeviceState, NetInterfaceState> map = new EnumMap<>(NMDeviceState.class);

        map.put(NMDeviceState.NM_DEVICE_STATE_UNKNOWN, NetInterfaceState.UNKNOWN);
        map.put(NMDeviceState.NM_DEVICE_STATE_UNMANAGED, NetInterfaceState.UNMANAGED);
        map.put(NMDeviceState.NM_DEVICE_STATE_UNAVAILABLE, NetInterfaceState.UNAVAILABLE);
        map.put(NMDeviceState.NM_DEVICE_STATE_DISCONNECTED, NetInterfaceState.DISCONNECTED);
        map.put(NMDeviceState.NM_DEVICE_STATE_PREPARE, NetInterfaceState.PREPARE);
        map.put(NMDeviceState.NM_DEVICE_STATE_CONFIG, NetInterfaceState.CONFIG);
        map.put(NMDeviceState.NM_DEVICE_STATE_NEED_AUTH, NetInterfaceState.NEED_AUTH);
        map.put(NMDeviceState.NM_DEVICE_STATE_IP_CONFIG, NetInterfaceState.IP_CONFIG);
        map.put(NMDeviceState.NM_DEVICE_STATE_IP_CHECK, NetInterfaceState.IP_CHECK);
        map.put(NMDeviceState.NM_DEVICE_STATE_SECONDARIES, NetInterfaceState.SECONDARIES);
        map.put(NMDeviceState.NM_DEVICE_STATE_ACTIVATED, NetInterfaceState.ACTIVATED);
        map.put(NMDeviceState.NM_DEVICE_STATE_DEACTIVATING, NetInterfaceState.DEACTIVATING);
        map.put(NMDeviceState.NM_DEVICE_STATE_FAILED, NetInterfaceState.FAILED);

        return map;
    }

}

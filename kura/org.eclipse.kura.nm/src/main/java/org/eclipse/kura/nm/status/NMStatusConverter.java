package org.eclipse.kura.nm.status;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.eclipse.kura.core.net.EthernetInterfaceImpl;
import org.eclipse.kura.core.net.NetInterfaceAddressImpl;
import org.eclipse.kura.net.IPAddress;
import org.eclipse.kura.net.NetInterface;
import org.eclipse.kura.net.NetInterfaceAddress;
import org.eclipse.kura.net.NetInterfaceState;
import org.eclipse.kura.nm.configuration.NMDeviceState;
import org.freedesktop.dbus.interfaces.Properties;
import org.freedesktop.dbus.types.UInt32;
import org.freedesktop.dbus.types.Variant;
import org.freedesktop.networkmanager.Device;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NMStatusConverter {

    private static final Logger logger = LoggerFactory.getLogger(NMStatusConverter.class);

    private static final String NM_DEVICE_BUS_NAME = "org.freedesktop.NetworkManager.Device";
    private static final String NM_IP4CONFIG_BUS_NAME = "org.freedesktop.NetworkManager.IP4Config";

    private static final EnumMap<NMDeviceState, NetInterfaceState> DEVICE_STATE_CONVERTER = initDeviceStateConverter();

    private NMStatusConverter() {
        throw new IllegalStateException("Utility class");
    }

    public static NetInterface<NetInterfaceAddress> buildEthernetStatus(String interfaceName, Device device,
            Properties deviceProperties, Properties ip4ConfigProperties, Properties dhcp4ConfigProperties) {
        EthernetInterfaceImpl<NetInterfaceAddress> ethInterface = new EthernetInterfaceImpl<>(interfaceName);

        ethInterface.setVirtual(false);
        ethInterface.setLoopback(false);
        ethInterface.setAutoConnect(deviceProperties.Get(NM_DEVICE_BUS_NAME, "Autoconnect"));
        ethInterface.setFirmwareVersion(deviceProperties.Get(NM_DEVICE_BUS_NAME, "FirmwareVersion"));
        ethInterface.setDriver(deviceProperties.Get(NM_DEVICE_BUS_NAME, "Driver"));
        ethInterface.setDriverVersion(deviceProperties.Get(NM_DEVICE_BUS_NAME, "DriverVersion"));

        NMDeviceState deviceState = NMDeviceState.fromUInt32(deviceProperties.Get(NM_DEVICE_BUS_NAME, "State"));
        ethInterface.setState(DEVICE_STATE_CONVERTER.get(deviceState));
        ethInterface.setUp(NMDeviceState.isConnected(deviceState));
        ethInterface.setLinkUp(NMDeviceState.isConnected(deviceState));

        ethInterface.setPointToPoint(false); // TBD

        UInt32 mtu = deviceProperties.Get(NM_DEVICE_BUS_NAME, "Mtu");
        ethInterface.setMTU(mtu.intValue());

        String hwAddress = deviceProperties.Get(NM_DEVICE_BUS_NAME, "HwAddress");
        ethInterface.setHardwareAddress(getMacAddressBytes(hwAddress));

        // Address informations
        NetInterfaceAddressImpl address = new NetInterfaceAddressImpl();

        String gateway = ip4ConfigProperties.Get(NM_IP4CONFIG_BUS_NAME, "Gateway");
        try {
            IPAddress ipGateway = IPAddress.parseHostAddress(gateway);
            address.setGateway(ipGateway);
        } catch (UnknownHostException e) {
            logger.debug("Could not retrieve gateway address {} due to:", gateway, e);
        }

        List<Map<String, Variant<?>>> addressData = ip4ConfigProperties.Get(NM_IP4CONFIG_BUS_NAME, "AddressData");
        for (Map<String, Variant<?>> data : addressData) {
            String addressStr = String.class.cast(data.get("address").getValue());
            UInt32 prefix = UInt32.class.cast(data.get("prefix").getValue());
            try {
                address.setAddress(IPAddress.parseHostAddress(addressStr));
            } catch (UnknownHostException e) {
                logger.debug("Could not retrieve ip address {} due to:", addressStr, e);
            }
            address.setNetworkPrefixLength(prefix.shortValue());
        }

        List<IPAddress> dnsServers = new ArrayList<>();
        List<Map<String, Variant<?>>> nameserverData = ip4ConfigProperties.Get(NM_IP4CONFIG_BUS_NAME, "NameserverData");
        for (Map<String, Variant<?>> data : nameserverData) {
            String addressStr = String.class.cast(data.get("address").getValue());
            try {
                dnsServers.add(IPAddress.parseHostAddress(addressStr));
            } catch (UnknownHostException e) {
                logger.debug("Could not retrieve ip address {} due to:", addressStr, e);
            }
        }
        address.setDnsServers(dnsServers);

        // Hardcode netmask for testing WIP
        try {
            address.setNetmask(IPAddress.parseHostAddress(new String("255.255.255.0")));
        } catch (UnknownHostException e) {
            logger.warn("Could not retrieve ip address {} due to:", "255.255.255.0", e);
        }

        // WIP: Grab all addresses
        ethInterface.setNetInterfaceAddresses(Arrays.asList(address));

        return ethInterface;

    }

    private static byte[] getMacAddressBytes(String macAddress) {
        String[] macAddressParts = macAddress.split(":");

        byte[] macAddressBytes = new byte[6];
        for (int i = 0; i < 6; i++) {
            Integer hex = Integer.parseInt(macAddressParts[i], 16);
            macAddressBytes[i] = hex.byteValue();
        }

        return macAddressBytes;
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

package org.eclipse.kura.nm.status;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.kura.core.net.AbstractNetInterface;
import org.eclipse.kura.core.net.EthernetInterfaceImpl;
import org.eclipse.kura.core.net.LoopbackInterfaceImpl;
import org.eclipse.kura.core.net.NetInterfaceAddressImpl;
import org.eclipse.kura.net.IPAddress;
import org.eclipse.kura.net.NetInterface;
import org.eclipse.kura.net.NetInterfaceAddress;
import org.eclipse.kura.net.NetInterfaceState;
import org.eclipse.kura.nm.configuration.NMDeviceState;
import org.freedesktop.dbus.interfaces.Properties;
import org.freedesktop.dbus.types.UInt32;
import org.freedesktop.dbus.types.Variant;
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

    public static NetInterface<NetInterfaceAddress> buildEthernetStatus(String interfaceName,
            Properties deviceProperties, Optional<Properties> ip4configProperties) {
        EthernetInterfaceImpl<NetInterfaceAddress> ethInterface = new EthernetInterfaceImpl<>(interfaceName);

        ethInterface.setVirtual(false);
        ethInterface.setLoopback(false);
        ethInterface.setPointToPoint(false); // TBD

        setDeviceStatus(ethInterface, deviceProperties);
        setIP4Status(ethInterface, ip4configProperties);

        return ethInterface;

    }

    public static NetInterface<NetInterfaceAddress> buildLoopbackStatus(String interfaceName,
            Properties deviceProperties, Optional<Properties> ip4configProperties) {
        LoopbackInterfaceImpl<NetInterfaceAddress> loInterface = new LoopbackInterfaceImpl<>(interfaceName);

        loInterface.setVirtual(true);
        loInterface.setLoopback(true);
        loInterface.setPointToPoint(false);

        setDeviceStatus(loInterface, deviceProperties);
        setIP4Status(loInterface, ip4configProperties);

        return loInterface;
    }

    private static void setDeviceStatus(AbstractNetInterface<NetInterfaceAddress> iface, Properties deviceProperties) {
        iface.setAutoConnect(deviceProperties.Get(NM_DEVICE_BUS_NAME, "Autoconnect"));
        iface.setFirmwareVersion(deviceProperties.Get(NM_DEVICE_BUS_NAME, "FirmwareVersion"));
        iface.setDriver(deviceProperties.Get(NM_DEVICE_BUS_NAME, "Driver"));
        iface.setDriverVersion(deviceProperties.Get(NM_DEVICE_BUS_NAME, "DriverVersion"));

        NMDeviceState deviceState = NMDeviceState.fromUInt32(deviceProperties.Get(NM_DEVICE_BUS_NAME, "State"));
        iface.setState(DEVICE_STATE_CONVERTER.get(deviceState));
        iface.setUp(NMDeviceState.isConnected(deviceState));

        UInt32 mtu = deviceProperties.Get(NM_DEVICE_BUS_NAME, "Mtu");
        iface.setMTU(mtu.intValue());

        String hwAddress = deviceProperties.Get(NM_DEVICE_BUS_NAME, "HwAddress");
        iface.setHardwareAddress(getMacAddressBytes(hwAddress));
    }

    private static void setIP4Status(AbstractNetInterface<NetInterfaceAddress> iface,
            Optional<Properties> ip4configProperties) {
        if (!ip4configProperties.isPresent()) {
            return;
        }

        List<NetInterfaceAddress> addressList = new ArrayList<>();

        String gateway = ip4configProperties.get().Get(NM_IP4CONFIG_BUS_NAME, "Gateway");
        List<Map<String, Variant<?>>> addressData = ip4configProperties.get().Get(NM_IP4CONFIG_BUS_NAME, "AddressData");
        List<Map<String, Variant<?>>> nameserverData = ip4configProperties.get().Get(NM_IP4CONFIG_BUS_NAME,
                "NameserverData");
        for (Map<String, Variant<?>> data : addressData) {
            NetInterfaceAddressImpl address = new NetInterfaceAddressImpl();

            try {
                IPAddress ipGateway = IPAddress.parseHostAddress(gateway);
                address.setGateway(ipGateway);
            } catch (UnknownHostException e) {
                logger.debug("Could not retrieve gateway address \"{}\" due to:", gateway, e);
            }

            String addressStr = String.class.cast(data.get("address").getValue());
            UInt32 prefix = UInt32.class.cast(data.get("prefix").getValue());
            try {
                address.setAddress(IPAddress.parseHostAddress(addressStr));
                address.setNetworkPrefixLength(prefix.shortValue());
                address.setNetmask(IPAddress.parseHostAddress(getNetmaskStringFrom(prefix.intValue())));
            } catch (UnknownHostException e) {
                logger.debug("Could not retrieve ip address due to:", e);
            }

            List<IPAddress> dnsServers = new ArrayList<>();
            for (Map<String, Variant<?>> dns : nameserverData) {
                String dnsAddressStr = String.class.cast(dns.get("address").getValue());
                try {
                    dnsServers.add(IPAddress.parseHostAddress(dnsAddressStr));
                } catch (UnknownHostException e) {
                    logger.debug("Could not retrieve ip address \"{}\" due to:", dnsAddressStr, e);
                }
            }
            address.setDnsServers(dnsServers);

            addressList.add(address);
        }

        iface.setNetInterfaceAddresses(addressList);
    }

    private static String getNetmaskStringFrom(int prefix) {
        if (prefix >= 1 && prefix <= 32) {
            int mask = ~((1 << 32 - prefix) - 1);
            return dottedQuad(mask);
        } else {
            throw new IllegalArgumentException("prefix is invalid: " + Integer.toString(prefix));
        }
    }

    private static String dottedQuad(int ip) {
        String[] items = new String[4];
        for (int i = 3; i >= 0; i--) {
            int value = ip & 0xFF;
            items[i] = Integer.toString(value);
            ip = ip >>> 8;
        }

        return String.join(".", items);
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

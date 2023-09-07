/*******************************************************************************
 * Copyright (c) 2023 Eurotech and/or its affiliates and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Eurotech
 *******************************************************************************/
package org.eclipse.kura.nm.enums;

import static org.junit.Assert.assertEquals;

import org.freedesktop.dbus.types.UInt32;
import org.junit.Test;

public class NMDeviceTypeTest {

    NMDeviceType type;
    UInt32 typeInt;

    @Test
    public void conversionWorksForTypeUnknown() {
        whenInt32StateIsPassed(new UInt32(0));
        thenTypeShouldBeEqualTo(NMDeviceType.NM_DEVICE_TYPE_UNKNOWN);
    }

    @Test
    public void conversionWorksForTypeEthernet() {
        whenInt32StateIsPassed(new UInt32(1));
        thenTypeShouldBeEqualTo(NMDeviceType.NM_DEVICE_TYPE_ETHERNET);
    }

    @Test
    public void conversionWorksForTypeWiFi() {
        whenInt32StateIsPassed(new UInt32(2));
        thenTypeShouldBeEqualTo(NMDeviceType.NM_DEVICE_TYPE_WIFI);
    }

    @Test
    public void conversionWorksForTypeUnused() {
        whenInt32StateIsPassed(new UInt32(3));
        thenTypeShouldBeEqualTo(NMDeviceType.NM_DEVICE_TYPE_UNUSED1);
    }

    @Test
    public void conversionWorksForTypeUnused2() {
        whenInt32StateIsPassed(new UInt32(4));
        thenTypeShouldBeEqualTo(NMDeviceType.NM_DEVICE_TYPE_UNUSED2);
    }

    @Test
    public void conversionWorksForTypeBt() {
        whenInt32StateIsPassed(new UInt32(5));
        thenTypeShouldBeEqualTo(NMDeviceType.NM_DEVICE_TYPE_BT);
    }

    @Test
    public void conversionWorksForTypeOlpcMesh() {
        whenInt32StateIsPassed(new UInt32(6));
        thenTypeShouldBeEqualTo(NMDeviceType.NM_DEVICE_TYPE_OLPC_MESH);
    }

    @Test
    public void conversionWorksForTypeWiMax() {
        whenInt32StateIsPassed(new UInt32(7));
        thenTypeShouldBeEqualTo(NMDeviceType.NM_DEVICE_TYPE_WIMAX);
    }

    @Test
    public void conversionWorksForTypeModem() {
        whenInt32StateIsPassed(new UInt32(8));
        thenTypeShouldBeEqualTo(NMDeviceType.NM_DEVICE_TYPE_MODEM);
    }

    @Test
    public void conversionWorksForTypeInfiniband() {
        whenInt32StateIsPassed(new UInt32(9));
        thenTypeShouldBeEqualTo(NMDeviceType.NM_DEVICE_TYPE_INFINIBAND);
    }

    @Test
    public void conversionWorksForTypeBond() {
        whenInt32StateIsPassed(new UInt32(10));
        thenTypeShouldBeEqualTo(NMDeviceType.NM_DEVICE_TYPE_BOND);
    }

    @Test
    public void conversionWorksForTypeVlan() {
        whenInt32StateIsPassed(new UInt32(11));
        thenTypeShouldBeEqualTo(NMDeviceType.NM_DEVICE_TYPE_VLAN);
    }

    @Test
    public void conversionWorksForTypeAdsl() {
        whenInt32StateIsPassed(new UInt32(12));
        thenTypeShouldBeEqualTo(NMDeviceType.NM_DEVICE_TYPE_ADSL);
    }

    @Test
    public void conversionWorksForTypeBridge() {
        whenInt32StateIsPassed(new UInt32(13));
        thenTypeShouldBeEqualTo(NMDeviceType.NM_DEVICE_TYPE_BRIDGE);
    }

    @Test
    public void conversionWorksForTypeGeneric() {
        whenInt32StateIsPassed(new UInt32(14));
        thenTypeShouldBeEqualTo(NMDeviceType.NM_DEVICE_TYPE_GENERIC);
    }

    @Test
    public void conversionWorksForTypeTeam() {
        whenInt32StateIsPassed(new UInt32(15));
        thenTypeShouldBeEqualTo(NMDeviceType.NM_DEVICE_TYPE_TEAM);
    }

    @Test
    public void conversionWorksForTypeTun() {
        whenInt32StateIsPassed(new UInt32(16));
        thenTypeShouldBeEqualTo(NMDeviceType.NM_DEVICE_TYPE_TUN);
    }

    @Test
    public void conversionWorksForTypeIpTunnel() {
        whenInt32StateIsPassed(new UInt32(17));
        thenTypeShouldBeEqualTo(NMDeviceType.NM_DEVICE_TYPE_IP_TUNNEL);
    }

    @Test
    public void conversionWorksForTypeMacVlan() {
        whenInt32StateIsPassed(new UInt32(18));
        thenTypeShouldBeEqualTo(NMDeviceType.NM_DEVICE_TYPE_MACVLAN);
    }

    @Test
    public void conversionWorksForTypeVxlan() {
        whenInt32StateIsPassed(new UInt32(19));
        thenTypeShouldBeEqualTo(NMDeviceType.NM_DEVICE_TYPE_VXLAN);
    }

    @Test
    public void conversionWorksForTypeVeth() {
        whenInt32StateIsPassed(new UInt32(20));
        thenTypeShouldBeEqualTo(NMDeviceType.NM_DEVICE_TYPE_VETH);
    }

    @Test
    public void conversionWorksForTypeMacsec() {
        whenInt32StateIsPassed(new UInt32(21));
        thenTypeShouldBeEqualTo(NMDeviceType.NM_DEVICE_TYPE_MACSEC);
    }

    @Test
    public void conversionWorksForTypeDummy() {
        whenInt32StateIsPassed(new UInt32(22));
        thenTypeShouldBeEqualTo(NMDeviceType.NM_DEVICE_TYPE_DUMMY);
    }

    @Test
    public void conversionWorksForTypePpp() {
        whenInt32StateIsPassed(new UInt32(23));
        thenTypeShouldBeEqualTo(NMDeviceType.NM_DEVICE_TYPE_PPP);
    }

    @Test
    public void conversionWorksForTypeOvsInterface() {
        whenInt32StateIsPassed(new UInt32(24));
        thenTypeShouldBeEqualTo(NMDeviceType.NM_DEVICE_TYPE_OVS_INTERFACE);
    }

    @Test
    public void conversionWorksForTypeOvsPort() {
        whenInt32StateIsPassed(new UInt32(25));
        thenTypeShouldBeEqualTo(NMDeviceType.NM_DEVICE_TYPE_OVS_PORT);
    }

    @Test
    public void conversionWorksForTypeOvsBridge() {
        whenInt32StateIsPassed(new UInt32(26));
        thenTypeShouldBeEqualTo(NMDeviceType.NM_DEVICE_TYPE_OVS_BRIDGE);
    }

    @Test
    public void conversionWorksForTypeWpan() {
        whenInt32StateIsPassed(new UInt32(27));
        thenTypeShouldBeEqualTo(NMDeviceType.NM_DEVICE_TYPE_WPAN);
    }

    @Test
    public void conversionWorksForType6LowPan() {
        whenInt32StateIsPassed(new UInt32(28));
        thenTypeShouldBeEqualTo(NMDeviceType.NM_DEVICE_TYPE_6LOWPAN);
    }

    @Test
    public void conversionWorksForTypeWireguard() {
        whenInt32StateIsPassed(new UInt32(29));
        thenTypeShouldBeEqualTo(NMDeviceType.NM_DEVICE_TYPE_WIREGUARD);
    }

    @Test
    public void conversionWorksForTypeWiFiP2p() {
        whenInt32StateIsPassed(new UInt32(30));
        thenTypeShouldBeEqualTo(NMDeviceType.NM_DEVICE_TYPE_WIFI_P2P);
    }

    @Test
    public void conversionWorksForTypeVrf() {
        whenInt32StateIsPassed(new UInt32(31));
        thenTypeShouldBeEqualTo(NMDeviceType.NM_DEVICE_TYPE_VRF);
    }

    @Test
    public void conversionWorksForTypeNull() {
        whenInt32StateIsPassed(new UInt32(32));
        thenTypeShouldBeEqualTo(NMDeviceType.NM_DEVICE_TYPE_LOOPBACK);
    }

    @Test
    public void conversionWorksForOutOfBounds() {
        whenInt32StateIsPassed(new UInt32(6536));
        thenTypeShouldBeEqualTo(NMDeviceType.NM_DEVICE_TYPE_UNKNOWN);
    }

    @Test
    public void conversionWorksForTypeUnknownUInt() {
        whenNMDeviceTypeStateIsPassed(NMDeviceType.NM_DEVICE_TYPE_UNKNOWN);
        thenTypeUInt32ShouldBeEqualTo(new UInt32(0));
    }

    @Test
    public void conversionWorksForTypeEthernetUInt() {
        whenNMDeviceTypeStateIsPassed(NMDeviceType.NM_DEVICE_TYPE_ETHERNET);
        thenTypeUInt32ShouldBeEqualTo(new UInt32(1));
    }

    @Test
    public void conversionWorksForTypeWiFiUInt() {
        whenNMDeviceTypeStateIsPassed(NMDeviceType.NM_DEVICE_TYPE_WIFI);
        thenTypeUInt32ShouldBeEqualTo(new UInt32(2));
    }

    @Test
    public void conversionWorksForTypeUnusedUInt() {
        whenNMDeviceTypeStateIsPassed(NMDeviceType.NM_DEVICE_TYPE_UNUSED1);
        thenTypeUInt32ShouldBeEqualTo(new UInt32(3));
    }

    @Test
    public void conversionWorksForTypeUnused2UInt() {
        whenNMDeviceTypeStateIsPassed(NMDeviceType.NM_DEVICE_TYPE_UNUSED2);
        thenTypeUInt32ShouldBeEqualTo(new UInt32(4));
    }

    @Test
    public void conversionWorksForTypeBtUInt() {
        whenNMDeviceTypeStateIsPassed(NMDeviceType.NM_DEVICE_TYPE_BT);
        thenTypeUInt32ShouldBeEqualTo(new UInt32(5));
    }

    @Test
    public void conversionWorksForTypeOlpcMeshUInt() {
        whenNMDeviceTypeStateIsPassed(NMDeviceType.NM_DEVICE_TYPE_OLPC_MESH);
        thenTypeUInt32ShouldBeEqualTo(new UInt32(6));
    }

    @Test
    public void conversionWorksForTypeWiMaxUInt() {
        whenNMDeviceTypeStateIsPassed(NMDeviceType.NM_DEVICE_TYPE_WIMAX);
        thenTypeUInt32ShouldBeEqualTo(new UInt32(7));
    }

    @Test
    public void conversionWorksForTypeModemUInt() {
        whenNMDeviceTypeStateIsPassed(NMDeviceType.NM_DEVICE_TYPE_MODEM);
        thenTypeUInt32ShouldBeEqualTo(new UInt32(8));
    }

    @Test
    public void conversionWorksForTypeInfinibandUInt() {
        whenNMDeviceTypeStateIsPassed(NMDeviceType.NM_DEVICE_TYPE_INFINIBAND);
        thenTypeUInt32ShouldBeEqualTo(new UInt32(9));
    }

    @Test
    public void conversionWorksForTypeBondUInt() {
        whenNMDeviceTypeStateIsPassed(NMDeviceType.NM_DEVICE_TYPE_BOND);
        thenTypeUInt32ShouldBeEqualTo(new UInt32(10));
    }

    @Test
    public void conversionWorksForTypeVlanUInt() {
        whenNMDeviceTypeStateIsPassed(NMDeviceType.NM_DEVICE_TYPE_VLAN);
        thenTypeUInt32ShouldBeEqualTo(new UInt32(11));
    }

    @Test
    public void conversionWorksForTypeAdslUInt() {
        whenNMDeviceTypeStateIsPassed(NMDeviceType.NM_DEVICE_TYPE_ADSL);
        thenTypeUInt32ShouldBeEqualTo(new UInt32(12));
    }

    @Test
    public void conversionWorksForTypeBridgeUInt() {
        whenNMDeviceTypeStateIsPassed(NMDeviceType.NM_DEVICE_TYPE_BRIDGE);
        thenTypeUInt32ShouldBeEqualTo(new UInt32(13));
    }

    @Test
    public void conversionWorksForTypeGenericUInt() {
        whenNMDeviceTypeStateIsPassed(NMDeviceType.NM_DEVICE_TYPE_GENERIC);
        thenTypeUInt32ShouldBeEqualTo(new UInt32(14));
    }

    @Test
    public void conversionWorksForTypeTeamUInt() {
        whenNMDeviceTypeStateIsPassed(NMDeviceType.NM_DEVICE_TYPE_TEAM);
        thenTypeUInt32ShouldBeEqualTo(new UInt32(15));
    }

    @Test
    public void conversionWorksForTypeTunUInt() {
        whenNMDeviceTypeStateIsPassed(NMDeviceType.NM_DEVICE_TYPE_TUN);
        thenTypeUInt32ShouldBeEqualTo(new UInt32(16));
    }

    @Test
    public void conversionWorksForTypeIpTunnelUInt() {
        whenNMDeviceTypeStateIsPassed(NMDeviceType.NM_DEVICE_TYPE_IP_TUNNEL);
        thenTypeUInt32ShouldBeEqualTo(new UInt32(17));
    }

    @Test
    public void conversionWorksForTypeMacVlanUInt() {
        whenNMDeviceTypeStateIsPassed(NMDeviceType.NM_DEVICE_TYPE_MACVLAN);
        thenTypeUInt32ShouldBeEqualTo(new UInt32(18));
    }

    @Test
    public void conversionWorksForTypeVxlanUInt() {
        whenNMDeviceTypeStateIsPassed(NMDeviceType.NM_DEVICE_TYPE_VXLAN);
        thenTypeUInt32ShouldBeEqualTo(new UInt32(19));
    }

    @Test
    public void conversionWorksForTypeVethUInt() {
        whenNMDeviceTypeStateIsPassed(NMDeviceType.NM_DEVICE_TYPE_VETH);
        thenTypeUInt32ShouldBeEqualTo(new UInt32(20));
    }

    @Test
    public void conversionWorksForTypeMacsecUInt() {
        whenNMDeviceTypeStateIsPassed(NMDeviceType.NM_DEVICE_TYPE_MACSEC);
        thenTypeUInt32ShouldBeEqualTo(new UInt32(21));
    }

    @Test
    public void conversionWorksForTypeDummyUInt() {
        whenNMDeviceTypeStateIsPassed(NMDeviceType.NM_DEVICE_TYPE_DUMMY);
        thenTypeUInt32ShouldBeEqualTo(new UInt32(22));
    }

    @Test
    public void conversionWorksForTypePppUInt() {
        whenNMDeviceTypeStateIsPassed(NMDeviceType.NM_DEVICE_TYPE_PPP);
        thenTypeUInt32ShouldBeEqualTo(new UInt32(23));
    }

    @Test
    public void conversionWorksForTypeOvsInterfaceUInt() {
        whenNMDeviceTypeStateIsPassed(NMDeviceType.NM_DEVICE_TYPE_OVS_INTERFACE);
        thenTypeUInt32ShouldBeEqualTo(new UInt32(24));
    }

    @Test
    public void conversionWorksForTypeOvsPortUInt() {
        whenNMDeviceTypeStateIsPassed(NMDeviceType.NM_DEVICE_TYPE_OVS_PORT);
        thenTypeUInt32ShouldBeEqualTo(new UInt32(25));
    }

    @Test
    public void conversionWorksForTypeOvsBridgeUInt() {
        whenNMDeviceTypeStateIsPassed(NMDeviceType.NM_DEVICE_TYPE_OVS_BRIDGE);
        thenTypeUInt32ShouldBeEqualTo(new UInt32(26));
    }

    @Test
    public void conversionWorksForTypeWpanUInt() {
        whenNMDeviceTypeStateIsPassed(NMDeviceType.NM_DEVICE_TYPE_WPAN);
        thenTypeUInt32ShouldBeEqualTo(new UInt32(27));
    }

    @Test
    public void conversionWorksForType6LowPanUInt() {
        whenNMDeviceTypeStateIsPassed(NMDeviceType.NM_DEVICE_TYPE_6LOWPAN);
        thenTypeUInt32ShouldBeEqualTo(new UInt32(28));
    }

    @Test
    public void conversionWorksForTypeWireguardUInt() {
        whenNMDeviceTypeStateIsPassed(NMDeviceType.NM_DEVICE_TYPE_WIREGUARD);
        thenTypeUInt32ShouldBeEqualTo(new UInt32(29));
    }

    @Test
    public void conversionWorksForTypeWiFiP2pUInt() {
        whenNMDeviceTypeStateIsPassed(NMDeviceType.NM_DEVICE_TYPE_WIFI_P2P);
        thenTypeUInt32ShouldBeEqualTo(new UInt32(30));
    }

    @Test
    public void conversionWorksForTypeVrfUInt() {
        whenNMDeviceTypeStateIsPassed(NMDeviceType.NM_DEVICE_TYPE_VRF);
        thenTypeUInt32ShouldBeEqualTo(new UInt32(31));
    }

    @Test
    public void conversionWorksForTypeNullUInt() {
        whenNMDeviceTypeStateIsPassed(NMDeviceType.NM_DEVICE_TYPE_LOOPBACK);
        thenTypeUInt32ShouldBeEqualTo(new UInt32(32));
    }

    @Test
    public void conversionWorksForTypeUnknownProperty() {
        whenPropertyStringDeviceTypeIsPassed("UNKNOWN");
        thenTypeShouldBeEqualTo(NMDeviceType.NM_DEVICE_TYPE_UNKNOWN);
    }
    
    @Test
    public void conversionWorksForTypeEthernetProperty() {
        whenPropertyStringDeviceTypeIsPassed("ETHERNET");
        thenTypeShouldBeEqualTo(NMDeviceType.NM_DEVICE_TYPE_ETHERNET);
    }
    
    @Test
    public void conversionWorksForTypeWifiProperty() {
        whenPropertyStringDeviceTypeIsPassed("WIFI");
        thenTypeShouldBeEqualTo(NMDeviceType.NM_DEVICE_TYPE_WIFI);
    }
    
    @Test
    public void conversionWorksForTypeUnused1Property() {
        whenPropertyStringDeviceTypeIsPassed("UNUSED1");
        thenTypeShouldBeEqualTo(NMDeviceType.NM_DEVICE_TYPE_UNUSED1);
    }
    
    @Test
    public void conversionWorksForTypeUnused2Property() {
        whenPropertyStringDeviceTypeIsPassed("UNUSED2");
        thenTypeShouldBeEqualTo(NMDeviceType.NM_DEVICE_TYPE_UNUSED2);
    }
    
    @Test
    public void conversionWorksForTypeBtProperty() {
        whenPropertyStringDeviceTypeIsPassed("BT");
        thenTypeShouldBeEqualTo(NMDeviceType.NM_DEVICE_TYPE_BT);
    }
    
    @Test
    public void conversionWorksForTypeOlpcMeshProperty() {
        whenPropertyStringDeviceTypeIsPassed("OLPC_MESH");
        thenTypeShouldBeEqualTo(NMDeviceType.NM_DEVICE_TYPE_OLPC_MESH);
    }
    
    @Test
    public void conversionWorksForTypeWimaxProperty() {
        whenPropertyStringDeviceTypeIsPassed("WIMAX");
        thenTypeShouldBeEqualTo(NMDeviceType.NM_DEVICE_TYPE_WIMAX);
    }
    
    @Test
    public void conversionWorksForTypeModemProperty() {
        whenPropertyStringDeviceTypeIsPassed("MODEM");
        thenTypeShouldBeEqualTo(NMDeviceType.NM_DEVICE_TYPE_MODEM);
    }
    
    @Test
    public void conversionWorksForTypeInfinibandProperty() {
        whenPropertyStringDeviceTypeIsPassed("INFINIBAND");
        thenTypeShouldBeEqualTo(NMDeviceType.NM_DEVICE_TYPE_INFINIBAND);
    }
    
    @Test
    public void conversionWorksForTypeBondProperty() {
        whenPropertyStringDeviceTypeIsPassed("BOND");
        thenTypeShouldBeEqualTo(NMDeviceType.NM_DEVICE_TYPE_BOND);
    }
    
    @Test
    public void conversionWorksForTypeVLANProperty() {
        whenPropertyStringDeviceTypeIsPassed("VLAN");
        thenTypeShouldBeEqualTo(NMDeviceType.NM_DEVICE_TYPE_VLAN);
    }
    
    @Test
    public void conversionWorksForTypeAdslProperty() {
        whenPropertyStringDeviceTypeIsPassed("ADSL");
        thenTypeShouldBeEqualTo(NMDeviceType.NM_DEVICE_TYPE_ADSL);
    }
    
    @Test
    public void conversionWorksForTypeLoopbackProperty() {
        whenPropertyStringDeviceTypeIsPassed("LOOPBACK");
        thenTypeShouldBeEqualTo(NMDeviceType.NM_DEVICE_TYPE_LOOPBACK);
    }
        
    @Test
    public void conversionWorksForTypeUnexpectedProperty() {
        whenPropertyStringDeviceTypeIsPassed("unexpectedValue");
        thenTypeShouldBeEqualTo(NMDeviceType.NM_DEVICE_TYPE_UNKNOWN);
    }

    private void whenInt32StateIsPassed(UInt32 type) {
        this.type = NMDeviceType.fromUInt32(type);
    }

    private void whenNMDeviceTypeStateIsPassed(NMDeviceType type) {
        this.typeInt = NMDeviceType.toUInt32(type);
    }
    
    private void whenPropertyStringDeviceTypeIsPassed(String propertyType) {
        this.type = NMDeviceType.fromPropertiesString(propertyType);
    }

    private void thenTypeShouldBeEqualTo(NMDeviceType type) {
        assertEquals(type, this.type);
    }

    private void thenTypeUInt32ShouldBeEqualTo(UInt32 type) {
        assertEquals(type, this.typeInt);
    }

}

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
package org.eclipse.kura.nm;

import static org.junit.Assert.assertEquals;

import org.freedesktop.dbus.types.UInt32;
import org.junit.Test;

public class NMDeviceTypeTest {

    NMDeviceType type;

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

    private void whenInt32StateIsPassed(UInt32 type) {
        this.type = NMDeviceType.fromUInt32(type);
    }

    private void thenTypeShouldBeEqualTo(NMDeviceType type) {
        assertEquals(this.type, type);

    }

}

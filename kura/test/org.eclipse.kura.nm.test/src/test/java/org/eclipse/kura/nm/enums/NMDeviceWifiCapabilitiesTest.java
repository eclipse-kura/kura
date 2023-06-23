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
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.freedesktop.dbus.types.UInt32;
import org.junit.Test;

public class NMDeviceWifiCapabilitiesTest {

    UInt32 value;
    List<NMDeviceWifiCapabilities> capabilities;

    @Test
    public void fromUInt32WorksWithNone() {
        givenValue(0x00000000);

        whenFromUInt32IsCalledWith(this.value);

        thenCapabilitiesSizeIs(1);
        thenCapabilitiesContains(NMDeviceWifiCapabilities.NM_WIFI_DEVICE_CAP_NONE);
    }

    @Test
    public void fromUInt32WorksWithCipherWEP40() {
        givenValue(0x00000001);

        whenFromUInt32IsCalledWith(this.value);

        thenCapabilitiesSizeIs(1);
        thenCapabilitiesContains(NMDeviceWifiCapabilities.NM_WIFI_DEVICE_CAP_CIPHER_WEP40);
    }

    @Test
    public void fromUInt32WorksWithCipherWEP104() {
        givenValue(0x00000002);

        whenFromUInt32IsCalledWith(this.value);

        thenCapabilitiesSizeIs(1);
        thenCapabilitiesContains(NMDeviceWifiCapabilities.NM_WIFI_DEVICE_CAP_CIPHER_WEP104);
    }

    @Test
    public void fromUInt32WorksWithCipherTKIP() {
        givenValue(0x00000004);

        whenFromUInt32IsCalledWith(this.value);

        thenCapabilitiesSizeIs(1);
        thenCapabilitiesContains(NMDeviceWifiCapabilities.NM_WIFI_DEVICE_CAP_CIPHER_TKIP);
    }

    @Test
    public void fromUInt32WorksWithCipherCCMP() {
        givenValue(0x00000008);

        whenFromUInt32IsCalledWith(this.value);

        thenCapabilitiesSizeIs(1);
        thenCapabilitiesContains(NMDeviceWifiCapabilities.NM_WIFI_DEVICE_CAP_CIPHER_CCMP);
    }

    @Test
    public void fromUInt32WorksWithWPA() {
        givenValue(0x00000010);

        whenFromUInt32IsCalledWith(this.value);

        thenCapabilitiesSizeIs(1);
        thenCapabilitiesContains(NMDeviceWifiCapabilities.NM_WIFI_DEVICE_CAP_WPA);
    }

    @Test
    public void fromUInt32WorksWithRSN() {
        givenValue(0x00000020);

        whenFromUInt32IsCalledWith(this.value);

        thenCapabilitiesSizeIs(1);
        thenCapabilitiesContains(NMDeviceWifiCapabilities.NM_WIFI_DEVICE_CAP_RSN);
    }

    @Test
    public void fromUInt32WorksWithAP() {
        givenValue(0x00000040);

        whenFromUInt32IsCalledWith(this.value);

        thenCapabilitiesSizeIs(1);
        thenCapabilitiesContains(NMDeviceWifiCapabilities.NM_WIFI_DEVICE_CAP_AP);
    }

    @Test
    public void fromUInt32WorksWithADHOC() {
        givenValue(0x00000080);

        whenFromUInt32IsCalledWith(this.value);

        thenCapabilitiesSizeIs(1);
        thenCapabilitiesContains(NMDeviceWifiCapabilities.NM_WIFI_DEVICE_CAP_ADHOC);
    }

    @Test
    public void fromUInt32WorksWithFreqValid() {
        givenValue(0x00000100);

        whenFromUInt32IsCalledWith(this.value);

        thenCapabilitiesSizeIs(1);
        thenCapabilitiesContains(NMDeviceWifiCapabilities.NM_WIFI_DEVICE_CAP_FREQ_VALID);
    }

    @Test
    public void fromUInt32WorksWithFreq2GHZ() {
        givenValue(0x00000200);

        whenFromUInt32IsCalledWith(this.value);

        thenCapabilitiesSizeIs(1);
        thenCapabilitiesContains(NMDeviceWifiCapabilities.NM_WIFI_DEVICE_CAP_FREQ_2GHZ);
    }

    @Test
    public void fromUInt32WorksWithFreq5GHZ() {
        givenValue(0x00000400);

        whenFromUInt32IsCalledWith(this.value);

        thenCapabilitiesSizeIs(1);
        thenCapabilitiesContains(NMDeviceWifiCapabilities.NM_WIFI_DEVICE_CAP_FREQ_5GHZ);
    }

    @Test
    public void fromUInt32WorksWithMesh() {
        givenValue(0x00001000);

        whenFromUInt32IsCalledWith(this.value);

        thenCapabilitiesSizeIs(1);
        thenCapabilitiesContains(NMDeviceWifiCapabilities.NM_WIFI_DEVICE_CAP_MESH);
    }

    @Test
    public void fromUInt32WorksWithIBSSRSN() {
        givenValue(0x00002000);

        whenFromUInt32IsCalledWith(this.value);

        thenCapabilitiesSizeIs(1);
        thenCapabilitiesContains(NMDeviceWifiCapabilities.NM_WIFI_DEVICE_CAP_IBSS_RSN);
    }

    @Test
    public void fromUInt32WorksWithMultipleCapabilities1() {
        givenValue(0x00000003);

        whenFromUInt32IsCalledWith(this.value);

        thenCapabilitiesSizeIs(2);
        thenCapabilitiesContains(NMDeviceWifiCapabilities.NM_WIFI_DEVICE_CAP_CIPHER_WEP40);
        thenCapabilitiesContains(NMDeviceWifiCapabilities.NM_WIFI_DEVICE_CAP_CIPHER_WEP104);
    }

    @Test
    public void fromUInt32WorksWithMultipleCapabilities2() {
        givenValue(0x000007ff);

        whenFromUInt32IsCalledWith(this.value);

        thenCapabilitiesSizeIs(11);
        thenCapabilitiesContains(NMDeviceWifiCapabilities.NM_WIFI_DEVICE_CAP_CIPHER_WEP40);
        thenCapabilitiesContains(NMDeviceWifiCapabilities.NM_WIFI_DEVICE_CAP_CIPHER_WEP104);
        thenCapabilitiesContains(NMDeviceWifiCapabilities.NM_WIFI_DEVICE_CAP_CIPHER_TKIP);
        thenCapabilitiesContains(NMDeviceWifiCapabilities.NM_WIFI_DEVICE_CAP_CIPHER_CCMP);
        thenCapabilitiesContains(NMDeviceWifiCapabilities.NM_WIFI_DEVICE_CAP_WPA);
        thenCapabilitiesContains(NMDeviceWifiCapabilities.NM_WIFI_DEVICE_CAP_RSN);
        thenCapabilitiesContains(NMDeviceWifiCapabilities.NM_WIFI_DEVICE_CAP_AP);
        thenCapabilitiesContains(NMDeviceWifiCapabilities.NM_WIFI_DEVICE_CAP_ADHOC);
        thenCapabilitiesContains(NMDeviceWifiCapabilities.NM_WIFI_DEVICE_CAP_FREQ_VALID);
        thenCapabilitiesContains(NMDeviceWifiCapabilities.NM_WIFI_DEVICE_CAP_FREQ_2GHZ);
        thenCapabilitiesContains(NMDeviceWifiCapabilities.NM_WIFI_DEVICE_CAP_FREQ_5GHZ);
    }

    @Test
    public void fromUInt32WorksWithMultipleCapabilities3() {
        givenValue(0x000033ff);

        whenFromUInt32IsCalledWith(this.value);

        thenCapabilitiesSizeIs(12);
        thenCapabilitiesContains(NMDeviceWifiCapabilities.NM_WIFI_DEVICE_CAP_CIPHER_WEP40);
        thenCapabilitiesContains(NMDeviceWifiCapabilities.NM_WIFI_DEVICE_CAP_CIPHER_WEP104);
        thenCapabilitiesContains(NMDeviceWifiCapabilities.NM_WIFI_DEVICE_CAP_CIPHER_TKIP);
        thenCapabilitiesContains(NMDeviceWifiCapabilities.NM_WIFI_DEVICE_CAP_CIPHER_CCMP);
        thenCapabilitiesContains(NMDeviceWifiCapabilities.NM_WIFI_DEVICE_CAP_WPA);
        thenCapabilitiesContains(NMDeviceWifiCapabilities.NM_WIFI_DEVICE_CAP_RSN);
        thenCapabilitiesContains(NMDeviceWifiCapabilities.NM_WIFI_DEVICE_CAP_AP);
        thenCapabilitiesContains(NMDeviceWifiCapabilities.NM_WIFI_DEVICE_CAP_ADHOC);
        thenCapabilitiesContains(NMDeviceWifiCapabilities.NM_WIFI_DEVICE_CAP_FREQ_VALID);
        thenCapabilitiesContains(NMDeviceWifiCapabilities.NM_WIFI_DEVICE_CAP_FREQ_2GHZ);
        thenCapabilitiesContains(NMDeviceWifiCapabilities.NM_WIFI_DEVICE_CAP_MESH);
        thenCapabilitiesContains(NMDeviceWifiCapabilities.NM_WIFI_DEVICE_CAP_IBSS_RSN);
    }

    private void givenValue(int intValue) {
        this.value = new UInt32(intValue);
    }

    private void whenFromUInt32IsCalledWith(UInt32 intValue) {
        this.capabilities = NMDeviceWifiCapabilities.fromUInt32(intValue);
    }

    private void thenCapabilitiesSizeIs(int size) {
        assertEquals(size, this.capabilities.size());
    }

    private void thenCapabilitiesContains(NMDeviceWifiCapabilities nmWifiDeviceCap) {
        assertTrue(this.capabilities.contains(nmWifiDeviceCap));
    }
}
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

public class NM80211ApSecurityFlagsTest {

    UInt32 value;
    List<NM80211ApSecurityFlags> capabilities;

    @Test
    public void fromUInt32WorksWithNone() {
        givenValue(0x00000000);

        whenFromUInt32IsCalledWith(this.value);

        thenFlagsSizeIs(1);
        thenFlagsContains(NM80211ApSecurityFlags.NM_802_11_AP_SEC_NONE);
    }

    @Test
    public void fromUInt32WorksWithPairWep40() {
        givenValue(0x00000001);

        whenFromUInt32IsCalledWith(this.value);

        thenFlagsSizeIs(1);
        thenFlagsContains(NM80211ApSecurityFlags.NM_802_11_AP_SEC_PAIR_WEP40);
    }

    @Test
    public void fromUInt32WorksWithPairWep104() {
        givenValue(0x00000002);

        whenFromUInt32IsCalledWith(this.value);

        thenFlagsSizeIs(1);
        thenFlagsContains(NM80211ApSecurityFlags.NM_802_11_AP_SEC_PAIR_WEP104);
    }

    @Test
    public void fromUInt32WorksWithPairTKIP() {
        givenValue(0x00000004);

        whenFromUInt32IsCalledWith(this.value);

        thenFlagsSizeIs(1);
        thenFlagsContains(NM80211ApSecurityFlags.NM_802_11_AP_SEC_PAIR_TKIP);
    }

    @Test
    public void fromUInt32WorksWithPairCCMP() {
        givenValue(0x00000008);

        whenFromUInt32IsCalledWith(this.value);

        thenFlagsSizeIs(1);
        thenFlagsContains(NM80211ApSecurityFlags.NM_802_11_AP_SEC_PAIR_CCMP);
    }

    @Test
    public void fromUInt32WorksWithGroupWep40() {
        givenValue(0x00000010);

        whenFromUInt32IsCalledWith(this.value);

        thenFlagsSizeIs(1);
        thenFlagsContains(NM80211ApSecurityFlags.NM_802_11_AP_SEC_GROUP_WEP40);
    }

    @Test
    public void fromUInt32WorksWithGroupWep104() {
        givenValue(0x00000020);

        whenFromUInt32IsCalledWith(this.value);

        thenFlagsSizeIs(1);
        thenFlagsContains(NM80211ApSecurityFlags.NM_802_11_AP_SEC_GROUP_WEP104);
    }

    @Test
    public void fromUInt32WorksWithGroupTKIP() {
        givenValue(0x00000040);

        whenFromUInt32IsCalledWith(this.value);

        thenFlagsSizeIs(1);
        thenFlagsContains(NM80211ApSecurityFlags.NM_802_11_AP_SEC_GROUP_TKIP);
    }

    @Test
    public void fromUInt32WorksWithGroupCCMP() {
        givenValue(0x00000080);

        whenFromUInt32IsCalledWith(this.value);

        thenFlagsSizeIs(1);
        thenFlagsContains(NM80211ApSecurityFlags.NM_802_11_AP_SEC_GROUP_CCMP);
    }

    @Test
    public void fromUInt32WorksWithMgmtPSK() {
        givenValue(0x00000100);

        whenFromUInt32IsCalledWith(this.value);

        thenFlagsSizeIs(1);
        thenFlagsContains(NM80211ApSecurityFlags.NM_802_11_AP_SEC_KEY_MGMT_PSK);
    }

    @Test
    public void fromUInt32WorksWithMgmt8021X() {
        givenValue(0x00000200);

        whenFromUInt32IsCalledWith(this.value);

        thenFlagsSizeIs(1);
        thenFlagsContains(NM80211ApSecurityFlags.NM_802_11_AP_SEC_KEY_MGMT_802_1X);
    }

    @Test
    public void fromUInt32WorksWithMgmtSAE() {
        givenValue(0x00000400);

        whenFromUInt32IsCalledWith(this.value);

        thenFlagsSizeIs(1);
        thenFlagsContains(NM80211ApSecurityFlags.NM_802_11_AP_SEC_KEY_MGMT_SAE);
    }

    @Test
    public void fromUInt32WorksWithMgmtOWE() {
        givenValue(0x00000800);

        whenFromUInt32IsCalledWith(this.value);

        thenFlagsSizeIs(1);
        thenFlagsContains(NM80211ApSecurityFlags.NM_802_11_AP_SEC_KEY_MGMT_OWE);
    }

    @Test
    public void fromUInt32WorksWithMgmtOWETM() {
        givenValue(0x00001000);

        whenFromUInt32IsCalledWith(this.value);

        thenFlagsSizeIs(1);
        thenFlagsContains(NM80211ApSecurityFlags.NM_802_11_AP_SEC_KEY_MGMT_OWE_TM);
    }

    @Test
    public void fromUInt32WorksWithMgmtEAPSUITEB192() {
        givenValue(0x00002000);

        whenFromUInt32IsCalledWith(this.value);

        thenFlagsSizeIs(1);
        thenFlagsContains(NM80211ApSecurityFlags.NM_802_11_AP_SEC_KEY_MGMT_EAP_SUITE_B_192);
    }

    @Test
    public void fromUInt32WorksWithMultipleFlags1() {
        givenValue(0x00000003);

        whenFromUInt32IsCalledWith(this.value);

        thenFlagsSizeIs(2);
        thenFlagsContains(NM80211ApSecurityFlags.NM_802_11_AP_SEC_PAIR_WEP40);
        thenFlagsContains(NM80211ApSecurityFlags.NM_802_11_AP_SEC_PAIR_WEP104);
    }

    private void givenValue(int intValue) {
        this.value = new UInt32(intValue);
    }

    private void whenFromUInt32IsCalledWith(UInt32 intValue) {
        this.capabilities = NM80211ApSecurityFlags.fromUInt32(intValue);
    }

    private void thenFlagsSizeIs(int size) {
        assertEquals(size, this.capabilities.size());
    }

    private void thenFlagsContains(NM80211ApSecurityFlags nmWifiDeviceCap) {
        assertTrue(this.capabilities.contains(nmWifiDeviceCap));
    }
}
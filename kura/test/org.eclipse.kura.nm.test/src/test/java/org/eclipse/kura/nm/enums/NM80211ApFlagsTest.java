/*******************************************************************************
 * Copyright (c) 2024 Eurotech and/or its affiliates and others
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

public class NM80211ApFlagsTest {

    UInt32 value;
    List<NM80211ApFlags> flags;

    @Test
    public void fromUInt32WorksWithNone() {
        givenValue(0x00000000);

        whenFromUInt32IsCalledWith(this.value);

        thenFlagsSizeIs(1);
        thenFlagsContains(NM80211ApFlags.NM_802_11_AP_FLAGS_NONE);
    }

    @Test
    public void fromUInt32WorksWithPrivacy() {
        givenValue(0x00000001);

        whenFromUInt32IsCalledWith(this.value);

        thenFlagsSizeIs(1);
        thenFlagsContains(NM80211ApFlags.NM_802_11_AP_FLAGS_PRIVACY);
    }

    @Test
    public void fromUInt32WorksWithWPS() {
        givenValue(0x00000002);

        whenFromUInt32IsCalledWith(this.value);

        thenFlagsSizeIs(1);
        thenFlagsContains(NM80211ApFlags.NM_802_11_AP_FLAGS_WPS);
    }

    @Test
    public void fromUInt32WorksWithWPSPBC() {
        givenValue(0x00000004);

        whenFromUInt32IsCalledWith(this.value);

        thenFlagsSizeIs(1);
        thenFlagsContains(NM80211ApFlags.NM_802_11_AP_FLAGS_WPS_PBC);
    }

    @Test
    public void fromUInt32WorksWithWPSPin() {
        givenValue(0x00000008);

        whenFromUInt32IsCalledWith(this.value);

        thenFlagsSizeIs(1);
        thenFlagsContains(NM80211ApFlags.NM_802_11_AP_FLAGS_WPS_PIN);
    }

    @Test
    public void fromUInt32WorksWithMultipleFlags() {
        givenValue(0x00000003);

        whenFromUInt32IsCalledWith(this.value);

        thenFlagsSizeIs(2);
        thenFlagsContains(NM80211ApFlags.NM_802_11_AP_FLAGS_PRIVACY);
        thenFlagsContains(NM80211ApFlags.NM_802_11_AP_FLAGS_WPS);
    }

    private void givenValue(int intValue) {
        this.value = new UInt32(intValue);
    }

    private void whenFromUInt32IsCalledWith(UInt32 intValue) {
        this.flags = NM80211ApFlags.fromUInt32(intValue);
    }

    private void thenFlagsSizeIs(int size) {
        assertEquals(size, this.flags.size());
    }

    private void thenFlagsContains(NM80211ApFlags nmWifiFlag) {
        assertTrue(this.flags.contains(nmWifiFlag));
    }
}
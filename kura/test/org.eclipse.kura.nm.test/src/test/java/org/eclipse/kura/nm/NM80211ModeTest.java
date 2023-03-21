package org.eclipse.kura.nm;

import static org.junit.Assert.assertEquals;

import org.freedesktop.dbus.types.UInt32;
import org.junit.Test;

public class NM80211ModeTest {

    NM80211Mode mode;

    @Test
    public void conversionWorksForModeUnknown() {
        whenFromUInt32IsCalledWith(new UInt32(0));
        thenModeShouldBeEqualTo(NM80211Mode.NM_802_11_MODE_UNKNOWN);
    }

    @Test
    public void conversionWorksForModeADHOC() {
        whenFromUInt32IsCalledWith(new UInt32(1));
        thenModeShouldBeEqualTo(NM80211Mode.NM_802_11_MODE_ADHOC);
    }

    @Test
    public void conversionWorksForModeINFRA() {
        whenFromUInt32IsCalledWith(new UInt32(2));
        thenModeShouldBeEqualTo(NM80211Mode.NM_802_11_MODE_INFRA);
    }

    @Test
    public void conversionWorksForModeAP() {
        whenFromUInt32IsCalledWith(new UInt32(3));
        thenModeShouldBeEqualTo(NM80211Mode.NM_802_11_MODE_AP);
    }

    @Test
    public void conversionWorksForModeMESH() {
        whenFromUInt32IsCalledWith(new UInt32(4));
        thenModeShouldBeEqualTo(NM80211Mode.NM_802_11_MODE_MESH);
    }

    @Test
    public void conversionWorksForUnsupported1() {
        whenFromUInt32IsCalledWith(new UInt32(5));
        thenModeShouldBeEqualTo(NM80211Mode.NM_802_11_MODE_UNKNOWN);
    }

    @Test
    public void conversionWorksForUnsupported2() {
        whenFromUInt32IsCalledWith(new UInt32(6535));
        thenModeShouldBeEqualTo(NM80211Mode.NM_802_11_MODE_UNKNOWN);
    }

    private void whenFromUInt32IsCalledWith(UInt32 uInt32) {
        this.mode = NM80211Mode.fromUInt32(uInt32);
    }

    private void thenModeShouldBeEqualTo(NM80211Mode result) {
        assertEquals(this.mode, result);
    }

}
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
 ******************************************************************************/
package org.eclipse.kura.nm.enums;

import static org.junit.Assert.assertEquals;

import org.freedesktop.dbus.types.UInt32;
import org.junit.Test;

public class NMDeviceStateTest {

    NMDeviceState state;
    UInt32 stateInt;

    @Test
    public void conversionWorksForStateUnknown() {
        whenInt32StateIsPassed(new UInt32(0));
        thenStateShouldBeEqualTo(NMDeviceState.NM_DEVICE_STATE_UNKNOWN);
    }

    @Test
    public void conversionWorksForStateUnmanaged() {
        whenInt32StateIsPassed(new UInt32(10));
        thenStateShouldBeEqualTo(NMDeviceState.NM_DEVICE_STATE_UNMANAGED);
    }

    @Test
    public void conversionWorksForStateUnavailable() {
        whenInt32StateIsPassed(new UInt32(20));
        thenStateShouldBeEqualTo(NMDeviceState.NM_DEVICE_STATE_UNAVAILABLE);
    }

    @Test
    public void conversionWorksForStateDisconnected() {
        whenInt32StateIsPassed(new UInt32(30));
        thenStateShouldBeEqualTo(NMDeviceState.NM_DEVICE_STATE_DISCONNECTED);
    }

    @Test
    public void conversionWorksForStatePrepare() {
        whenInt32StateIsPassed(new UInt32(40));
        thenStateShouldBeEqualTo(NMDeviceState.NM_DEVICE_STATE_PREPARE);
    }

    @Test
    public void conversionWorksForStateConfig() {
        whenInt32StateIsPassed(new UInt32(50));
        thenStateShouldBeEqualTo(NMDeviceState.NM_DEVICE_STATE_CONFIG);
    }

    @Test
    public void conversionWorksForStateNeedAuth() {
        whenInt32StateIsPassed(new UInt32(60));
        thenStateShouldBeEqualTo(NMDeviceState.NM_DEVICE_STATE_NEED_AUTH);
    }

    @Test
    public void conversionWorksForStateIpConfig() {
        whenInt32StateIsPassed(new UInt32(70));
        thenStateShouldBeEqualTo(NMDeviceState.NM_DEVICE_STATE_IP_CONFIG);
    }

    @Test
    public void conversionWorksForStateIpCheck() {
        whenInt32StateIsPassed(new UInt32(80));
        thenStateShouldBeEqualTo(NMDeviceState.NM_DEVICE_STATE_IP_CHECK);
    }

    @Test
    public void conversionWorksForStateSecondaries() {
        whenInt32StateIsPassed(new UInt32(90));
        thenStateShouldBeEqualTo(NMDeviceState.NM_DEVICE_STATE_SECONDARIES);
    }

    @Test
    public void conversionWorksForStateActivated() {
        whenInt32StateIsPassed(new UInt32(100));
        thenStateShouldBeEqualTo(NMDeviceState.NM_DEVICE_STATE_ACTIVATED);
    }

    @Test
    public void conversionWorksForStateDeactivating() {
        whenInt32StateIsPassed(new UInt32(110));
        thenStateShouldBeEqualTo(NMDeviceState.NM_DEVICE_STATE_DEACTIVATING);
    }

    @Test
    public void conversionWorksForStateFailed() {
        whenInt32StateIsPassed(new UInt32(120));
        thenStateShouldBeEqualTo(NMDeviceState.NM_DEVICE_STATE_FAILED);
    }

    @Test
    public void conversionWorksForStateUnknownDefault() {
        whenInt32StateIsPassed(new UInt32(121));
        thenStateShouldBeEqualTo(NMDeviceState.NM_DEVICE_STATE_UNKNOWN);
    }

    @Test
    public void shouldReturnConnectionFalseUnknown() {
        whenStateIsSetTo(NMDeviceState.NM_DEVICE_STATE_UNKNOWN);
        thenIsConnectShouldReturn(false);
    }

    @Test
    public void shouldReturnConnectionFalseUnmanaged() {
        whenStateIsSetTo(NMDeviceState.NM_DEVICE_STATE_UNMANAGED);
        thenIsConnectShouldReturn(false);
    }

    @Test
    public void shouldReturnConnectionFalseUnavailable() {
        whenStateIsSetTo(NMDeviceState.NM_DEVICE_STATE_UNAVAILABLE);
        thenIsConnectShouldReturn(false);
    }

    @Test
    public void shouldReturnConnectionFalseDisconnected() {
        whenStateIsSetTo(NMDeviceState.NM_DEVICE_STATE_DISCONNECTED);
        thenIsConnectShouldReturn(false);
    }

    @Test
    public void shouldReturnConnectionTruePrepare() {
        whenStateIsSetTo(NMDeviceState.NM_DEVICE_STATE_PREPARE);
        thenIsConnectShouldReturn(true);
    }

    @Test
    public void shouldReturnConnectionTrueConfig() {
        whenStateIsSetTo(NMDeviceState.NM_DEVICE_STATE_CONFIG);
        thenIsConnectShouldReturn(true);
    }

    @Test
    public void shouldReturnConnectionTrueNeedAuth() {
        whenStateIsSetTo(NMDeviceState.NM_DEVICE_STATE_NEED_AUTH);
        thenIsConnectShouldReturn(true);
    }

    @Test
    public void shouldReturnConnectionTrueIpConfig() {
        whenStateIsSetTo(NMDeviceState.NM_DEVICE_STATE_IP_CONFIG);
        thenIsConnectShouldReturn(true);
    }

    @Test
    public void shouldReturnConnectionTrueIpCheck() {
        whenStateIsSetTo(NMDeviceState.NM_DEVICE_STATE_IP_CHECK);
        thenIsConnectShouldReturn(true);
    }

    @Test
    public void shouldReturnConnectionTrueSecondaries() {
        whenStateIsSetTo(NMDeviceState.NM_DEVICE_STATE_SECONDARIES);
        thenIsConnectShouldReturn(true);
    }

    @Test
    public void shouldReturnConnectionTrueActivated() {
        whenStateIsSetTo(NMDeviceState.NM_DEVICE_STATE_ACTIVATED);
        thenIsConnectShouldReturn(true);
    }

    @Test
    public void shouldReturnConnectionTrueDeactivating() {
        whenStateIsSetTo(NMDeviceState.NM_DEVICE_STATE_DEACTIVATING);
        thenIsConnectShouldReturn(true);
    }

    @Test
    public void shouldReturnConnectionTrueFailed() {
        whenStateIsSetTo(NMDeviceState.NM_DEVICE_STATE_FAILED);
        thenIsConnectShouldReturn(true);
    }

    @Test
    public void conversionWorksForStateUnknownToUINT() {
        whenNMDeviceStateIsPassed(NMDeviceState.NM_DEVICE_STATE_UNKNOWN);
        thenStateUIntShouldBeEqualTo(new UInt32(0));
    }

    @Test
    public void conversionWorksForStateUnmanagedUINT() {
        whenNMDeviceStateIsPassed(NMDeviceState.NM_DEVICE_STATE_UNMANAGED);
        thenStateUIntShouldBeEqualTo(new UInt32(10));
    }

    @Test
    public void conversionWorksForStateUnavailableUINT() {
        whenNMDeviceStateIsPassed(NMDeviceState.NM_DEVICE_STATE_UNAVAILABLE);
        thenStateUIntShouldBeEqualTo(new UInt32(20));
    }

    @Test
    public void conversionWorksForStateDisconnectedUINT() {
        whenNMDeviceStateIsPassed(NMDeviceState.NM_DEVICE_STATE_DISCONNECTED);
        thenStateUIntShouldBeEqualTo(new UInt32(30));
    }

    @Test
    public void conversionWorksForStatePrepareUINT() {
        whenNMDeviceStateIsPassed(NMDeviceState.NM_DEVICE_STATE_PREPARE);
        thenStateUIntShouldBeEqualTo(new UInt32(40));
    }

    @Test
    public void conversionWorksForStateConfigUINT() {
        whenNMDeviceStateIsPassed(NMDeviceState.NM_DEVICE_STATE_CONFIG);
        thenStateUIntShouldBeEqualTo(new UInt32(50));
    }

    @Test
    public void conversionWorksForStateNeedAuthUINT() {
        whenNMDeviceStateIsPassed(NMDeviceState.NM_DEVICE_STATE_NEED_AUTH);
        thenStateUIntShouldBeEqualTo(new UInt32(60));
    }

    @Test
    public void conversionWorksForStateIpConfigUINT() {
        whenNMDeviceStateIsPassed(NMDeviceState.NM_DEVICE_STATE_IP_CONFIG);
        thenStateUIntShouldBeEqualTo(new UInt32(70));
    }

    @Test
    public void conversionWorksForStateIpCheckUINT() {
        whenNMDeviceStateIsPassed(NMDeviceState.NM_DEVICE_STATE_IP_CHECK);
        thenStateUIntShouldBeEqualTo(new UInt32(80));
    }

    @Test
    public void conversionWorksForStateSecondariesUINT() {
        whenNMDeviceStateIsPassed(NMDeviceState.NM_DEVICE_STATE_SECONDARIES);
        thenStateUIntShouldBeEqualTo(new UInt32(90));
    }

    @Test
    public void conversionWorksForStateActivatedUINT() {
        whenNMDeviceStateIsPassed(NMDeviceState.NM_DEVICE_STATE_ACTIVATED);
        thenStateUIntShouldBeEqualTo(new UInt32(100));
    }

    @Test
    public void conversionWorksForStateDeactivatingUINT() {
        whenNMDeviceStateIsPassed(NMDeviceState.NM_DEVICE_STATE_DEACTIVATING);
        thenStateUIntShouldBeEqualTo(new UInt32(110));
    }

    @Test
    public void conversionWorksForStateFailedUINT() {
        whenNMDeviceStateIsPassed(NMDeviceState.NM_DEVICE_STATE_FAILED);
        thenStateUIntShouldBeEqualTo(new UInt32(120));
    }

    @Test
    public void conversionWorksForStateUnknownDefaultUINT() {
        whenNMDeviceStateIsPassed(NMDeviceState.NM_DEVICE_STATE_UNKNOWN);
        thenStateUIntShouldBeEqualTo(new UInt32(0));
    }

    public void whenInt32StateIsPassed(UInt32 state) {
        this.state = NMDeviceState.fromUInt32(state);
    }

    public void whenNMDeviceStateIsPassed(NMDeviceState state) {
        this.stateInt = NMDeviceState.toUInt32(state);
    }

    public void whenStateIsSetTo(NMDeviceState type) {
        this.state = type;
    }

    public void thenStateShouldBeEqualTo(NMDeviceState type) {
        assertEquals(this.state, type);
    }

    public void thenStateUIntShouldBeEqualTo(UInt32 state) {
        assertEquals(this.stateInt, state);
    }

    public void thenIsConnectShouldReturn(Boolean bool) {
        assertEquals(NMDeviceState.isConnected(this.state), bool);
    }

}

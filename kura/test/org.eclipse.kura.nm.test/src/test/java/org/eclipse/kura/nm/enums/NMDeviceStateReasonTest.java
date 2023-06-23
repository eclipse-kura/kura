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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.freedesktop.dbus.types.UInt32;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Enclosed.class)
public class NMDeviceStateReasonTest {

    @RunWith(Parameterized.class)
    public static class fromUInt32Test {

        @Parameters
        public static Collection<Object[]> NMDeviceStateReasonParams() {
            List<Object[]> params = new ArrayList<>();
            params.add(new Object[] { new UInt32(0), NMDeviceStateReason.NM_DEVICE_STATE_REASON_NONE });
            params.add(new Object[] { new UInt32(1), NMDeviceStateReason.NM_DEVICE_STATE_REASON_UNKNOWN });
            params.add(new Object[] { new UInt32(2), NMDeviceStateReason.NM_DEVICE_STATE_REASON_NOW_MANAGED });
            params.add(new Object[] { new UInt32(3), NMDeviceStateReason.NM_DEVICE_STATE_REASON_NOW_UNMANAGED });
            params.add(new Object[] { new UInt32(4), NMDeviceStateReason.NM_DEVICE_STATE_REASON_CONFIG_FAILED });
            params.add(
                    new Object[] { new UInt32(5), NMDeviceStateReason.NM_DEVICE_STATE_REASON_IP_CONFIG_UNAVAILABLE });
            params.add(new Object[] { new UInt32(6), NMDeviceStateReason.NM_DEVICE_STATE_REASON_IP_CONFIG_EXPIRED });
            params.add(new Object[] { new UInt32(7), NMDeviceStateReason.NM_DEVICE_STATE_REASON_NO_SECRETS });
            params.add(
                    new Object[] { new UInt32(8), NMDeviceStateReason.NM_DEVICE_STATE_REASON_SUPPLICANT_DISCONNECT });
            params.add(new Object[] { new UInt32(9),
                    NMDeviceStateReason.NM_DEVICE_STATE_REASON_SUPPLICANT_CONFIG_FAILED });
            params.add(new Object[] { new UInt32(10), NMDeviceStateReason.NM_DEVICE_STATE_REASON_SUPPLICANT_FAILED });
            params.add(new Object[] { new UInt32(11), NMDeviceStateReason.NM_DEVICE_STATE_REASON_SUPPLICANT_TIMEOUT });
            params.add(new Object[] { new UInt32(12), NMDeviceStateReason.NM_DEVICE_STATE_REASON_PPP_START_FAILED });
            params.add(new Object[] { new UInt32(13), NMDeviceStateReason.NM_DEVICE_STATE_REASON_PPP_DISCONNECT });
            params.add(new Object[] { new UInt32(14), NMDeviceStateReason.NM_DEVICE_STATE_REASON_PPP_FAILED });
            params.add(new Object[] { new UInt32(15), NMDeviceStateReason.NM_DEVICE_STATE_REASON_DHCP_START_FAILED });
            params.add(new Object[] { new UInt32(16), NMDeviceStateReason.NM_DEVICE_STATE_REASON_DHCP_ERROR });
            params.add(new Object[] { new UInt32(17), NMDeviceStateReason.NM_DEVICE_STATE_REASON_DHCP_FAILED });
            params.add(new Object[] { new UInt32(18), NMDeviceStateReason.NM_DEVICE_STATE_REASON_SHARED_START_FAILED });
            params.add(new Object[] { new UInt32(19), NMDeviceStateReason.NM_DEVICE_STATE_REASON_SHARED_FAILED });
            params.add(new Object[] { new UInt32(20), NMDeviceStateReason.NM_DEVICE_STATE_REASON_AUTOIP_START_FAILED });
            params.add(new Object[] { new UInt32(21), NMDeviceStateReason.NM_DEVICE_STATE_REASON_AUTOIP_ERROR });
            params.add(new Object[] { new UInt32(22), NMDeviceStateReason.NM_DEVICE_STATE_REASON_AUTOIP_FAILED });
            params.add(new Object[] { new UInt32(23), NMDeviceStateReason.NM_DEVICE_STATE_REASON_MODEM_BUSY });
            params.add(new Object[] { new UInt32(24), NMDeviceStateReason.NM_DEVICE_STATE_REASON_MODEM_NO_DIAL_TONE });
            params.add(new Object[] { new UInt32(25), NMDeviceStateReason.NM_DEVICE_STATE_REASON_MODEM_NO_CARRIER });
            params.add(new Object[] { new UInt32(26), NMDeviceStateReason.NM_DEVICE_STATE_REASON_MODEM_DIAL_TIMEOUT });
            params.add(new Object[] { new UInt32(27), NMDeviceStateReason.NM_DEVICE_STATE_REASON_MODEM_DIAL_FAILED });
            params.add(new Object[] { new UInt32(28), NMDeviceStateReason.NM_DEVICE_STATE_REASON_MODEM_INIT_FAILED });
            params.add(new Object[] { new UInt32(29), NMDeviceStateReason.NM_DEVICE_STATE_REASON_GSM_APN_FAILED });
            params.add(new Object[] { new UInt32(30),
                    NMDeviceStateReason.NM_DEVICE_STATE_REASON_GSM_REGISTRATION_NOT_SEARCHING });
            params.add(new Object[] { new UInt32(31),
                    NMDeviceStateReason.NM_DEVICE_STATE_REASON_GSM_REGISTRATION_DENIED });
            params.add(new Object[] { new UInt32(32),
                    NMDeviceStateReason.NM_DEVICE_STATE_REASON_GSM_REGISTRATION_TIMEOUT });
            params.add(new Object[] { new UInt32(33),
                    NMDeviceStateReason.NM_DEVICE_STATE_REASON_GSM_REGISTRATION_FAILED });
            params.add(
                    new Object[] { new UInt32(34), NMDeviceStateReason.NM_DEVICE_STATE_REASON_GSM_PIN_CHECK_FAILED });
            params.add(new Object[] { new UInt32(35), NMDeviceStateReason.NM_DEVICE_STATE_REASON_FIRMWARE_MISSING });
            params.add(new Object[] { new UInt32(36), NMDeviceStateReason.NM_DEVICE_STATE_REASON_REMOVED });
            params.add(new Object[] { new UInt32(37), NMDeviceStateReason.NM_DEVICE_STATE_REASON_SLEEPING });
            params.add(new Object[] { new UInt32(38), NMDeviceStateReason.NM_DEVICE_STATE_REASON_CONNECTION_REMOVED });
            params.add(new Object[] { new UInt32(39), NMDeviceStateReason.NM_DEVICE_STATE_REASON_USER_REQUESTED });
            params.add(new Object[] { new UInt32(40), NMDeviceStateReason.NM_DEVICE_STATE_REASON_CARRIER });
            params.add(new Object[] { new UInt32(41), NMDeviceStateReason.NM_DEVICE_STATE_REASON_CONNECTION_ASSUMED });
            params.add(
                    new Object[] { new UInt32(42), NMDeviceStateReason.NM_DEVICE_STATE_REASON_SUPPLICANT_AVAILABLE });
            params.add(new Object[] { new UInt32(43), NMDeviceStateReason.NM_DEVICE_STATE_REASON_MODEM_NOT_FOUND });
            params.add(new Object[] { new UInt32(44), NMDeviceStateReason.NM_DEVICE_STATE_REASON_BT_FAILED });
            params.add(
                    new Object[] { new UInt32(45), NMDeviceStateReason.NM_DEVICE_STATE_REASON_GSM_SIM_NOT_INSERTED });
            params.add(
                    new Object[] { new UInt32(46), NMDeviceStateReason.NM_DEVICE_STATE_REASON_GSM_SIM_PIN_REQUIRED });
            params.add(
                    new Object[] { new UInt32(47), NMDeviceStateReason.NM_DEVICE_STATE_REASON_GSM_SIM_PUK_REQUIRED });
            params.add(new Object[] { new UInt32(48), NMDeviceStateReason.NM_DEVICE_STATE_REASON_GSM_SIM_WRONG });
            params.add(new Object[] { new UInt32(49), NMDeviceStateReason.NM_DEVICE_STATE_REASON_INFINIBAND_MODE });
            params.add(new Object[] { new UInt32(50), NMDeviceStateReason.NM_DEVICE_STATE_REASON_DEPENDENCY_FAILED });
            params.add(new Object[] { new UInt32(51), NMDeviceStateReason.NM_DEVICE_STATE_REASON_BR2684_FAILED });
            params.add(new Object[] { new UInt32(52),
                    NMDeviceStateReason.NM_DEVICE_STATE_REASON_MODEM_MANAGER_UNAVAILABLE });
            params.add(new Object[] { new UInt32(53), NMDeviceStateReason.NM_DEVICE_STATE_REASON_SSID_NOT_FOUND });
            params.add(new Object[] { new UInt32(54),
                    NMDeviceStateReason.NM_DEVICE_STATE_REASON_SECONDARY_CONNECTION_FAILED });
            params.add(new Object[] { new UInt32(55), NMDeviceStateReason.NM_DEVICE_STATE_REASON_DCB_FCOE_FAILED });
            params.add(
                    new Object[] { new UInt32(56), NMDeviceStateReason.NM_DEVICE_STATE_REASON_TEAMD_CONTROL_FAILED });
            params.add(new Object[] { new UInt32(57), NMDeviceStateReason.NM_DEVICE_STATE_REASON_MODEM_FAILED });
            params.add(new Object[] { new UInt32(58), NMDeviceStateReason.NM_DEVICE_STATE_REASON_MODEM_AVAILABLE });
            params.add(new Object[] { new UInt32(59), NMDeviceStateReason.NM_DEVICE_STATE_REASON_SIM_PIN_INCORRECT });
            params.add(new Object[] { new UInt32(60), NMDeviceStateReason.NM_DEVICE_STATE_REASON_NEW_ACTIVATION });
            params.add(new Object[] { new UInt32(61), NMDeviceStateReason.NM_DEVICE_STATE_REASON_PARENT_CHANGED });
            params.add(
                    new Object[] { new UInt32(62), NMDeviceStateReason.NM_DEVICE_STATE_REASON_PARENT_MANAGED_CHANGED });
            params.add(new Object[] { new UInt32(63), NMDeviceStateReason.NM_DEVICE_STATE_REASON_OVSDB_FAILED });
            params.add(
                    new Object[] { new UInt32(64), NMDeviceStateReason.NM_DEVICE_STATE_REASON_IP_ADDRESS_DUPLICATE });
            params.add(
                    new Object[] { new UInt32(65), NMDeviceStateReason.NM_DEVICE_STATE_REASON_IP_METHOD_UNSUPPORTED });
            params.add(new Object[] { new UInt32(66),
                    NMDeviceStateReason.NM_DEVICE_STATE_REASON_SRIOV_CONFIGURATION_FAILED });
            params.add(new Object[] { new UInt32(67), NMDeviceStateReason.NM_DEVICE_STATE_REASON_PEER_NOT_FOUND });
            params.add(new Object[] { new UInt32(69), NMDeviceStateReason.NM_DEVICE_STATE_REASON_UNKNOWN });
            return params;
        }

        private final UInt32 inputValue;
        private final NMDeviceStateReason expectedValue;
        private NMDeviceStateReason computedValue;

        public fromUInt32Test(UInt32 intValue, NMDeviceStateReason expectedReason) {
            this.inputValue = intValue;
            this.expectedValue = expectedReason;
        }

        @Test
        public void shouldReturnExpectedDeviceStateReason() {
            whenConversionMethodIsCalledWith(this.inputValue);
            thenCalculatedValueMatches(this.expectedValue);
        }

        private void whenConversionMethodIsCalledWith(UInt32 input) {
            this.computedValue = NMDeviceStateReason.fromUInt32(input);
        }

        private void thenCalculatedValueMatches(NMDeviceStateReason expected) {
            assertEquals(expected, this.computedValue);
        }

    }

}

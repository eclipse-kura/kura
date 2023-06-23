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

import org.eclipse.kura.net.status.modem.ModemPortType;
import org.freedesktop.dbus.types.UInt32;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Enclosed.class)
public class MMModemPortTest {

    @RunWith(Parameterized.class)
    public static class MMModemPortTypeToMMModemPortTypeTest {

        @Parameters
        public static Collection<Object[]> ModemPortTypeParams() {
            List<Object[]> params = new ArrayList<>();
            params.add(new Object[] { new UInt32(0x01), MMModemPortType.MM_MODEM_PORT_TYPE_UNKNOWN });
            params.add(new Object[] { new UInt32(0x02), MMModemPortType.MM_MODEM_PORT_TYPE_NET });
            params.add(new Object[] { new UInt32(0x03), MMModemPortType.MM_MODEM_PORT_TYPE_AT });
            params.add(new Object[] { new UInt32(0x04), MMModemPortType.MM_MODEM_PORT_TYPE_QCDM });
            params.add(new Object[] { new UInt32(0x05), MMModemPortType.MM_MODEM_PORT_TYPE_GPS });
            params.add(new Object[] { new UInt32(0x06), MMModemPortType.MM_MODEM_PORT_TYPE_QMI });
            params.add(new Object[] { new UInt32(0x07), MMModemPortType.MM_MODEM_PORT_TYPE_MBIM });
            params.add(new Object[] { new UInt32(0x08), MMModemPortType.MM_MODEM_PORT_TYPE_AUDIO });
            params.add(new Object[] { new UInt32(0x09), MMModemPortType.MM_MODEM_PORT_TYPE_IGNORED });
            return params;
        }

        private final UInt32 inputIntValue;
        private final MMModemPortType expectedModemPortType;
        private MMModemPortType calculatedModemPortType;

        public MMModemPortTypeToMMModemPortTypeTest(UInt32 intValue, MMModemPortType modemPortType) {
            this.inputIntValue = intValue;
            this.expectedModemPortType = modemPortType;
        }

        @Test
        public void shouldReturnCorrectMMModemPortType() {
            whenCalculateMMModemPortType();
            thenCalculatedMMModemPortTypeIsCorrect();
        }

        private void whenCalculateMMModemPortType() {
            this.calculatedModemPortType = MMModemPortType.toMMModemPortType(this.inputIntValue);
        }

        private void thenCalculatedMMModemPortTypeIsCorrect() {
            assertEquals(this.expectedModemPortType, this.calculatedModemPortType);
        }
    }

    @RunWith(Parameterized.class)
    public static class MMBearerIpFamilyToModemPortTypeTest {

        @Parameters
        public static Collection<Object[]> ModemPortTypeParams() {
            List<Object[]> params = new ArrayList<>();
            params.add(new Object[] { new UInt32(0x01), ModemPortType.UNKNOWN });
            params.add(new Object[] { new UInt32(0x02), ModemPortType.NET });
            params.add(new Object[] { new UInt32(0x03), ModemPortType.AT });
            params.add(new Object[] { new UInt32(0x04), ModemPortType.QCDM });
            params.add(new Object[] { new UInt32(0x05), ModemPortType.GPS });
            params.add(new Object[] { new UInt32(0x06), ModemPortType.QMI });
            params.add(new Object[] { new UInt32(0x07), ModemPortType.MBIM });
            params.add(new Object[] { new UInt32(0x08), ModemPortType.AUDIO });
            params.add(new Object[] { new UInt32(0x09), ModemPortType.IGNORED });
            return params;
        }

        private final UInt32 inputIntValue;
        private final ModemPortType expectedModemPortType;
        private ModemPortType calculatedModemPortType;

        public MMBearerIpFamilyToModemPortTypeTest(UInt32 intValue, ModemPortType modemPortType) {
            this.inputIntValue = intValue;
            this.expectedModemPortType = modemPortType;
        }

        @Test
        public void shouldReturnCorrectModemPortType() {
            whenCalculatedModemPortType();
            thenCalculatedModemPortTypeIsCorrect();
        }

        private void whenCalculatedModemPortType() {
            this.calculatedModemPortType = MMModemPortType.toModemPortType(this.inputIntValue);
        }

        private void thenCalculatedModemPortTypeIsCorrect() {
            assertEquals(this.expectedModemPortType, this.calculatedModemPortType);
        }
    }

    @RunWith(Parameterized.class)
    public static class MMModemPortTypeToUInt32Test {

        @Parameters
        public static Collection<Object[]> ModemPortTypeParams() {
            List<Object[]> params = new ArrayList<>();
            params.add(new Object[] { MMModemPortType.MM_MODEM_PORT_TYPE_UNKNOWN, new UInt32(0x01), });
            params.add(new Object[] { MMModemPortType.MM_MODEM_PORT_TYPE_NET, new UInt32(0x02) });
            params.add(new Object[] { MMModemPortType.MM_MODEM_PORT_TYPE_AT, new UInt32(0x03) });
            params.add(new Object[] { MMModemPortType.MM_MODEM_PORT_TYPE_QCDM, new UInt32(0x04) });
            params.add(new Object[] { MMModemPortType.MM_MODEM_PORT_TYPE_GPS, new UInt32(0x05) });
            params.add(new Object[] { MMModemPortType.MM_MODEM_PORT_TYPE_QMI, new UInt32(0x06) });
            params.add(new Object[] { MMModemPortType.MM_MODEM_PORT_TYPE_MBIM, new UInt32(0x07) });
            params.add(new Object[] { MMModemPortType.MM_MODEM_PORT_TYPE_AUDIO, new UInt32(0x08) });
            params.add(new Object[] { MMModemPortType.MM_MODEM_PORT_TYPE_IGNORED, new UInt32(0x09) });
            return params;
        }

        private final MMModemPortType inputModemPortType;
        private final UInt32 expectedIntValue;
        private UInt32 calculatedUInt32;

        public MMModemPortTypeToUInt32Test(MMModemPortType modemPortType, UInt32 intValue) {
            this.expectedIntValue = intValue;
            this.inputModemPortType = modemPortType;
        }

        @Test
        public void shouldReturnCorrectUInt32() {
            whenCalculatedUInt32();
            thenCalculatedUInt32IsCorrect();
        }

        private void whenCalculatedUInt32() {
            this.calculatedUInt32 = this.inputModemPortType.toUInt32();
        }

        private void thenCalculatedUInt32IsCorrect() {
            assertEquals(this.expectedIntValue, this.calculatedUInt32);
        }
    }
}

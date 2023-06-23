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
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.eclipse.kura.net.status.modem.AccessTechnology;
import org.freedesktop.dbus.types.UInt32;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Enclosed.class)
public class MMModemAccessTechnologyTest {

    @RunWith(Parameterized.class)
    public static class MMModemAccessTechnologyToMMModemAccessTechnologyTest {

        @Parameters
        public static Collection<Object[]> AccessTechnologyParams() {
            List<Object[]> params = new ArrayList<>();
            params.add(new Object[] { new UInt32(0x00000000),
                    MMModemAccessTechnology.MM_MODEM_ACCESS_TECHNOLOGY_UNKNOWN });
            params.add(
                    new Object[] { new UInt32(0x00000001), MMModemAccessTechnology.MM_MODEM_ACCESS_TECHNOLOGY_POTS });
            params.add(new Object[] { new UInt32(0x00000002), MMModemAccessTechnology.MM_MODEM_ACCESS_TECHNOLOGY_GSM });
            params.add(new Object[] { new UInt32(0x00000004),
                    MMModemAccessTechnology.MM_MODEM_ACCESS_TECHNOLOGY_GSM_COMPACT });
            params.add(
                    new Object[] { new UInt32(0x00000008), MMModemAccessTechnology.MM_MODEM_ACCESS_TECHNOLOGY_GPRS });
            params.add(
                    new Object[] { new UInt32(0x00000010), MMModemAccessTechnology.MM_MODEM_ACCESS_TECHNOLOGY_EDGE });
            params.add(
                    new Object[] { new UInt32(0x00000020), MMModemAccessTechnology.MM_MODEM_ACCESS_TECHNOLOGY_UMTS });
            params.add(
                    new Object[] { new UInt32(0x00000040), MMModemAccessTechnology.MM_MODEM_ACCESS_TECHNOLOGY_HSDPA });
            params.add(
                    new Object[] { new UInt32(0x00000080), MMModemAccessTechnology.MM_MODEM_ACCESS_TECHNOLOGY_HSUPA });
            params.add(
                    new Object[] { new UInt32(0x00000100), MMModemAccessTechnology.MM_MODEM_ACCESS_TECHNOLOGY_HSPA });
            params.add(new Object[] { new UInt32(0x00000200),
                    MMModemAccessTechnology.MM_MODEM_ACCESS_TECHNOLOGY_HSPA_PLUS });
            params.add(
                    new Object[] { new UInt32(0x00000400), MMModemAccessTechnology.MM_MODEM_ACCESS_TECHNOLOGY_1XRTT });
            params.add(
                    new Object[] { new UInt32(0x00000800), MMModemAccessTechnology.MM_MODEM_ACCESS_TECHNOLOGY_EVDO0 });
            params.add(
                    new Object[] { new UInt32(0x00001000), MMModemAccessTechnology.MM_MODEM_ACCESS_TECHNOLOGY_EVDOA });
            params.add(
                    new Object[] { new UInt32(0x00002000), MMModemAccessTechnology.MM_MODEM_ACCESS_TECHNOLOGY_EVDOB });
            params.add(new Object[] { new UInt32(0x00004000), MMModemAccessTechnology.MM_MODEM_ACCESS_TECHNOLOGY_LTE });
            params.add(
                    new Object[] { new UInt32(0x00008000), MMModemAccessTechnology.MM_MODEM_ACCESS_TECHNOLOGY_5GNR });
            params.add(new Object[] { new UInt32(0x00010000),
                    MMModemAccessTechnology.MM_MODEM_ACCESS_TECHNOLOGY_LTE_CAT_M });
            params.add(new Object[] { new UInt32(0x00020000),
                    MMModemAccessTechnology.MM_MODEM_ACCESS_TECHNOLOGY_LTE_NB_IOT });
            params.add(new Object[] { new UInt32(Integer.toUnsignedString(0xFFFFFFFF)),
                    MMModemAccessTechnology.MM_MODEM_ACCESS_TECHNOLOGY_ANY });
            params.add(new Object[] { new UInt32(0x12345678),
                    MMModemAccessTechnology.MM_MODEM_ACCESS_TECHNOLOGY_UNKNOWN });
            return params;
        }

        private final UInt32 inputIntValue;
        private final MMModemAccessTechnology expectedAccessTechnology;
        private MMModemAccessTechnology calculatedAccessTechnology;

        public MMModemAccessTechnologyToMMModemAccessTechnologyTest(UInt32 intValue,
                MMModemAccessTechnology accessTechnology) {
            this.inputIntValue = intValue;
            this.expectedAccessTechnology = accessTechnology;
        }

        @Test
        public void shouldReturnCorrectIpFamily() {
            whenCalculateMMModemAccessTechnology();
            thenCalculatedMMModemAccessTechnologyIsCorrect();
        }

        private void whenCalculateMMModemAccessTechnology() {
            this.calculatedAccessTechnology = MMModemAccessTechnology.toMMModemAccessTechnology(this.inputIntValue);
        }

        private void thenCalculatedMMModemAccessTechnologyIsCorrect() {
            assertEquals(this.expectedAccessTechnology, this.calculatedAccessTechnology);
        }
    }

    @RunWith(Parameterized.class)
    public static class MMModemAccessTechnologyToAccessTechnologyTest {

        @Parameters
        public static Collection<Object[]> AccessTechnologyParams() {
            List<Object[]> params = new ArrayList<>();
            params.add(new Object[] { new UInt32(0x00000000), AccessTechnology.UNKNOWN });
            params.add(new Object[] { new UInt32(0x00000001), AccessTechnology.POTS });
            params.add(new Object[] { new UInt32(0x00000002), AccessTechnology.GSM });
            params.add(new Object[] { new UInt32(0x00000004), AccessTechnology.GSM_COMPACT });
            params.add(new Object[] { new UInt32(0x00000008), AccessTechnology.GPRS });
            params.add(new Object[] { new UInt32(0x00000010), AccessTechnology.EDGE });
            params.add(new Object[] { new UInt32(0x00000020), AccessTechnology.UMTS });
            params.add(new Object[] { new UInt32(0x00000040), AccessTechnology.HSDPA });
            params.add(new Object[] { new UInt32(0x00000080), AccessTechnology.HSUPA });
            params.add(new Object[] { new UInt32(0x00000100), AccessTechnology.HSPA });
            params.add(new Object[] { new UInt32(0x00000200), AccessTechnology.HSPA_PLUS });
            params.add(new Object[] { new UInt32(0x00000400), AccessTechnology.ONEXRTT });
            params.add(new Object[] { new UInt32(0x00000800), AccessTechnology.EVDO0 });
            params.add(new Object[] { new UInt32(0x00001000), AccessTechnology.EVDOA });
            params.add(new Object[] { new UInt32(0x00002000), AccessTechnology.EVDOB });
            params.add(new Object[] { new UInt32(0x00004000), AccessTechnology.LTE });
            params.add(new Object[] { new UInt32(0x00008000), AccessTechnology.FIVEGNR });
            params.add(new Object[] { new UInt32(0x00010000), AccessTechnology.LTE_CAT_M });
            params.add(new Object[] { new UInt32(0x00020000), AccessTechnology.LTE_NB_IOT });
            params.add(new Object[] { new UInt32(Integer.toUnsignedString(0xFFFFFFFF)), AccessTechnology.ANY });
            params.add(new Object[] { new UInt32(0x12345678), AccessTechnology.UNKNOWN });
            return params;
        }

        private final UInt32 inputIntValue;
        private final AccessTechnology expectedAccessTechnology;
        private AccessTechnology calculatedAccessTechnology;

        public MMModemAccessTechnologyToAccessTechnologyTest(UInt32 intValue, AccessTechnology accessTechnology) {
            this.inputIntValue = intValue;
            this.expectedAccessTechnology = accessTechnology;
        }

        @Test
        public void shouldReturnCorrectAccessTechnology() {
            whenCalculatedAccessTechnology();
            thenCalculatedAccessTechnologyIsCorrect();
        }

        private void whenCalculatedAccessTechnology() {
            this.calculatedAccessTechnology = MMModemAccessTechnology.toAccessTechnology(this.inputIntValue);
        }

        private void thenCalculatedAccessTechnologyIsCorrect() {
            assertEquals(this.expectedAccessTechnology, this.calculatedAccessTechnology);
        }
    }

    @RunWith(Parameterized.class)
    public static class MMModemAccessTechnologyToAccessTechnologyFromBitMaskTest {

        @Parameters
        public static Collection<Object[]> AccessTechnologyParams() {
            List<Object[]> params = new ArrayList<>();
            params.add(new Object[] { new UInt32(0x00000000), EnumSet.of(AccessTechnology.UNKNOWN) });
            params.add(
                    new Object[] { new UInt32(0x00000003), EnumSet.of(AccessTechnology.POTS, AccessTechnology.GSM) });
            params.add(new Object[] { new UInt32(0x00000006),
                    EnumSet.of(AccessTechnology.GSM, AccessTechnology.GSM_COMPACT) });
            params.add(new Object[] { new UInt32(0x00000004), EnumSet.of(AccessTechnology.GSM_COMPACT) });
            params.add(new Object[] { new UInt32(0x0000000F), EnumSet.of(AccessTechnology.POTS, AccessTechnology.GSM,
                    AccessTechnology.GSM_COMPACT, AccessTechnology.GPRS) });
            params.add(new Object[] { new UInt32(0x00030F00),
                    EnumSet.of(AccessTechnology.HSPA, AccessTechnology.HSPA_PLUS, AccessTechnology.ONEXRTT,
                            AccessTechnology.EVDO0, AccessTechnology.LTE_CAT_M, AccessTechnology.LTE_NB_IOT) });
            params.add(new Object[] { new UInt32(Integer.toUnsignedString(0xFFFFFFFF)),
                    EnumSet.of(AccessTechnology.ANY) });
            params.add(new Object[] { new UInt32(0x12300000), EnumSet.noneOf(AccessTechnology.class) });
            return params;
        }

        private final UInt32 inputIntValue;
        private final Set<AccessTechnology> expectedAccessTechnology;
        private Set<AccessTechnology> calculatedAccessTechnology;

        public MMModemAccessTechnologyToAccessTechnologyFromBitMaskTest(UInt32 intValue,
                Set<AccessTechnology> accessTechnology) {
            this.inputIntValue = intValue;
            this.expectedAccessTechnology = accessTechnology;
        }

        @Test
        public void shouldReturnCorrectAccessTechnology() {
            whenCalculatedAccessTechnologyType();
            thenCalculatedAccessTechnologyIsCorrect();
        }

        private void whenCalculatedAccessTechnologyType() {
            this.calculatedAccessTechnology = MMModemAccessTechnology.toAccessTechnologyFromBitMask(this.inputIntValue);
        }

        private void thenCalculatedAccessTechnologyIsCorrect() {
            assertEquals(this.expectedAccessTechnology, this.calculatedAccessTechnology);
        }

    }

    @RunWith(Parameterized.class)
    public static class MMModemAccessTechnologyToUInt32Test {

        @Parameters
        public static Collection<Object[]> AccessTechnologyParams() {
            List<Object[]> params = new ArrayList<>();
            params.add(new Object[] { MMModemAccessTechnology.MM_MODEM_ACCESS_TECHNOLOGY_UNKNOWN,
                    new UInt32(0x00000000) });
            params.add(
                    new Object[] { MMModemAccessTechnology.MM_MODEM_ACCESS_TECHNOLOGY_POTS, new UInt32(0x00000001) });
            params.add(new Object[] { MMModemAccessTechnology.MM_MODEM_ACCESS_TECHNOLOGY_GSM, new UInt32(0x00000002) });
            params.add(new Object[] { MMModemAccessTechnology.MM_MODEM_ACCESS_TECHNOLOGY_GSM_COMPACT,
                    new UInt32(0x00000004) });
            params.add(
                    new Object[] { MMModemAccessTechnology.MM_MODEM_ACCESS_TECHNOLOGY_GPRS, new UInt32(0x00000008) });
            params.add(
                    new Object[] { MMModemAccessTechnology.MM_MODEM_ACCESS_TECHNOLOGY_EDGE, new UInt32(0x00000010) });
            params.add(
                    new Object[] { MMModemAccessTechnology.MM_MODEM_ACCESS_TECHNOLOGY_UMTS, new UInt32(0x00000020) });
            params.add(
                    new Object[] { MMModemAccessTechnology.MM_MODEM_ACCESS_TECHNOLOGY_HSDPA, new UInt32(0x00000040) });
            params.add(
                    new Object[] { MMModemAccessTechnology.MM_MODEM_ACCESS_TECHNOLOGY_HSUPA, new UInt32(0x00000080) });
            params.add(
                    new Object[] { MMModemAccessTechnology.MM_MODEM_ACCESS_TECHNOLOGY_HSPA, new UInt32(0x00000100) });
            params.add(new Object[] { MMModemAccessTechnology.MM_MODEM_ACCESS_TECHNOLOGY_HSPA_PLUS,
                    new UInt32(0x00000200) });
            params.add(
                    new Object[] { MMModemAccessTechnology.MM_MODEM_ACCESS_TECHNOLOGY_1XRTT, new UInt32(0x00000400) });
            params.add(
                    new Object[] { MMModemAccessTechnology.MM_MODEM_ACCESS_TECHNOLOGY_EVDO0, new UInt32(0x00000800) });
            params.add(
                    new Object[] { MMModemAccessTechnology.MM_MODEM_ACCESS_TECHNOLOGY_EVDOA, new UInt32(0x00001000) });
            params.add(
                    new Object[] { MMModemAccessTechnology.MM_MODEM_ACCESS_TECHNOLOGY_EVDOB, new UInt32(0x00002000) });
            params.add(new Object[] { MMModemAccessTechnology.MM_MODEM_ACCESS_TECHNOLOGY_LTE, new UInt32(0x00004000) });
            params.add(
                    new Object[] { MMModemAccessTechnology.MM_MODEM_ACCESS_TECHNOLOGY_5GNR, new UInt32(0x00008000) });
            params.add(new Object[] { MMModemAccessTechnology.MM_MODEM_ACCESS_TECHNOLOGY_LTE_CAT_M,
                    new UInt32(0x00010000) });
            params.add(new Object[] { MMModemAccessTechnology.MM_MODEM_ACCESS_TECHNOLOGY_LTE_NB_IOT,
                    new UInt32(0x00020000) });
            params.add(new Object[] { MMModemAccessTechnology.MM_MODEM_ACCESS_TECHNOLOGY_ANY,
                    new UInt32(Integer.toUnsignedString(0xFFFFFFFF)) });
            params.add(new Object[] { MMModemAccessTechnology.MM_MODEM_ACCESS_TECHNOLOGY_UNKNOWN,
                    new UInt32(0x00000000) });
            return params;
        }

        private final MMModemAccessTechnology inputAccessTechnology;
        private final UInt32 expectedIntValue;
        private UInt32 calculatedUInt32;

        public MMModemAccessTechnologyToUInt32Test(MMModemAccessTechnology accessTechnology, UInt32 intValue) {
            this.expectedIntValue = intValue;
            this.inputAccessTechnology = accessTechnology;
        }

        @Test
        public void shouldReturnCorrectUInt32() {
            whenCalculatedUInt32();
            thenCalculatedUInt32IsCorrect();
        }

        private void whenCalculatedUInt32() {
            this.calculatedUInt32 = this.inputAccessTechnology.toUInt32();
        }

        private void thenCalculatedUInt32IsCorrect() {
            assertEquals(this.expectedIntValue, this.calculatedUInt32);
        }
    }
}

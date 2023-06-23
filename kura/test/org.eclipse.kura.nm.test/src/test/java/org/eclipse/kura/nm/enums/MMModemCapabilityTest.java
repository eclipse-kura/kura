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

import org.eclipse.kura.net.status.modem.ModemCapability;
import org.freedesktop.dbus.types.UInt32;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Enclosed.class)
public class MMModemCapabilityTest {

    @RunWith(Parameterized.class)
    public static class MMModemCapabilityToMMModemCapabilityTest {

        @Parameters
        public static Collection<Object[]> ModemCapabilityParams() {
            List<Object[]> params = new ArrayList<>();
            params.add(new Object[] { new UInt32(0x00000000L), MMModemCapability.MM_MODEM_CAPABILITY_NONE });
            params.add(new Object[] { new UInt32(0x00000001L), MMModemCapability.MM_MODEM_CAPABILITY_POTS });
            params.add(new Object[] { new UInt32(0x00000002L), MMModemCapability.MM_MODEM_CAPABILITY_CDMA_EVDO });
            params.add(new Object[] { new UInt32(0x00000004L), MMModemCapability.MM_MODEM_CAPABILITY_GSM_UMTS });
            params.add(new Object[] { new UInt32(0x00000008L), MMModemCapability.MM_MODEM_CAPABILITY_LTE });
            params.add(new Object[] { new UInt32(0x00000010L), MMModemCapability.MM_MODEM_CAPABILITY_NONE });
            params.add(new Object[] { new UInt32(0x00000020L), MMModemCapability.MM_MODEM_CAPABILITY_IRIDIUM });
            params.add(new Object[] { new UInt32(0x00000040L), MMModemCapability.MM_MODEM_CAPABILITY_5GNR });
            params.add(new Object[] { new UInt32(0x00000080L), MMModemCapability.MM_MODEM_CAPABILITY_TDS });
            params.add(new Object[] { new UInt32(0xFFFFFFFFL), MMModemCapability.MM_MODEM_CAPABILITY_ANY });
            return params;
        }

        private final UInt32 inputIntValue;
        private final MMModemCapability expectedModemCapability;
        private MMModemCapability calculatedModemCapability;

        public MMModemCapabilityToMMModemCapabilityTest(UInt32 intValue, MMModemCapability modemCapability) {
            this.inputIntValue = intValue;
            this.expectedModemCapability = modemCapability;
        }

        @Test
        public void shouldReturnCorrectModemCapability() {
            whenCalculateMMModemCapability();
            thenCalculatedMMModemCapabilityIsCorrect();
        }

        private void whenCalculateMMModemCapability() {
            this.calculatedModemCapability = MMModemCapability.toMMModemCapability(this.inputIntValue);
        }

        private void thenCalculatedMMModemCapabilityIsCorrect() {
            assertEquals(this.expectedModemCapability, this.calculatedModemCapability);
        }
    }

    @RunWith(Parameterized.class)
    public static class MMModemCapabilityToModemCapabilityTest {

        @Parameters
        public static Collection<Object[]> ModemCapabilityParams() {
            List<Object[]> params = new ArrayList<>();
            params.add(new Object[] { new UInt32(0x00000000L), ModemCapability.NONE });
            params.add(new Object[] { new UInt32(0x00000001L), ModemCapability.POTS });
            params.add(new Object[] { new UInt32(0x00000002L), ModemCapability.EVDO });
            params.add(new Object[] { new UInt32(0x00000004L), ModemCapability.GSM_UMTS });
            params.add(new Object[] { new UInt32(0x00000008L), ModemCapability.LTE });
            params.add(new Object[] { new UInt32(0x00000010L), ModemCapability.NONE });
            params.add(new Object[] { new UInt32(0x00000020L), ModemCapability.IRIDIUM });
            params.add(new Object[] { new UInt32(0x00000040L), ModemCapability.FIVE_GNR });
            params.add(new Object[] { new UInt32(0x00000080L), ModemCapability.TDS });
            params.add(new Object[] { new UInt32(0xFFFFFFFFL), ModemCapability.ANY });
            return params;
        }

        private final UInt32 inputIntValue;
        private final ModemCapability expectedModemCapability;
        private ModemCapability calculatedModemCapability;

        public MMModemCapabilityToModemCapabilityTest(UInt32 intValue, ModemCapability modemCapability) {
            this.inputIntValue = intValue;
            this.expectedModemCapability = modemCapability;
        }

        @Test
        public void shouldReturnCorrectModemCapability() {
            whenCalculatedModemCapability();
            thenCalculatedModemCapabilityIsCorrect();
        }

        private void whenCalculatedModemCapability() {
            this.calculatedModemCapability = MMModemCapability.toModemCapability(this.inputIntValue);
        }

        private void thenCalculatedModemCapabilityIsCorrect() {
            assertEquals(this.expectedModemCapability, this.calculatedModemCapability);
        }
    }

    @RunWith(Parameterized.class)
    public static class MMModemCapabilityToModemCapabilitiesFromBitMaskTest {

        @Parameters
        public static Collection<Object[]> ModemCapabilityParams() {
            List<Object[]> params = new ArrayList<>();
            params.add(new Object[] { new UInt32(0x00000000L), EnumSet.of(ModemCapability.NONE) });
            params.add(
                    new Object[] { new UInt32(0x00000003L), EnumSet.of(ModemCapability.POTS, ModemCapability.EVDO) });
            params.add(new Object[] { new UInt32(0x00000006L),
                    EnumSet.of(ModemCapability.EVDO, ModemCapability.GSM_UMTS) });
            params.add(new Object[] { new UInt32(0x00000004L), EnumSet.of(ModemCapability.GSM_UMTS) });
            params.add(new Object[] { new UInt32(0x0000000FL), EnumSet.of(ModemCapability.POTS, ModemCapability.EVDO,
                    ModemCapability.GSM_UMTS, ModemCapability.LTE) });
            params.add(new Object[] { new UInt32(0xFFFFFFFFL), EnumSet.of(ModemCapability.ANY) });
            params.add(new Object[] { new UInt32(0x12345600L), EnumSet.noneOf(ModemCapability.class) });
            return params;
        }

        private final UInt32 inputIntValue;
        private final Set<ModemCapability> expectedModemCapability;
        private Set<ModemCapability> calculatedModemCapability;

        public MMModemCapabilityToModemCapabilitiesFromBitMaskTest(UInt32 intValue,
                Set<ModemCapability> modemCapability) {
            this.inputIntValue = intValue;
            this.expectedModemCapability = modemCapability;
        }

        @Test
        public void shouldReturnCorrectModemCapabilities() {
            whenCalculatedModemCapability();
            thenCalculatedModemCapabilityIsCorrect();
        }

        private void whenCalculatedModemCapability() {
            this.calculatedModemCapability = MMModemCapability.toModemCapabilitiesFromBitMask(this.inputIntValue);
        }

        private void thenCalculatedModemCapabilityIsCorrect() {
            assertEquals(this.expectedModemCapability, this.calculatedModemCapability);
        }

    }

    @RunWith(Parameterized.class)
    public static class MMModemCapabilityToUInt32Test {

        @Parameters
        public static Collection<Object[]> ModemCapabilityParams() {
            List<Object[]> params = new ArrayList<>();
            params.add(new Object[] { MMModemCapability.MM_MODEM_CAPABILITY_NONE, new UInt32(0x00000000L) });
            params.add(new Object[] { MMModemCapability.MM_MODEM_CAPABILITY_POTS, new UInt32(0x00000001L) });
            params.add(new Object[] { MMModemCapability.MM_MODEM_CAPABILITY_CDMA_EVDO, new UInt32(0x00000002L) });
            params.add(new Object[] { MMModemCapability.MM_MODEM_CAPABILITY_GSM_UMTS, new UInt32(0x00000004L) });
            params.add(new Object[] { MMModemCapability.MM_MODEM_CAPABILITY_LTE, new UInt32(0x00000008L) });
            params.add(new Object[] { MMModemCapability.MM_MODEM_CAPABILITY_IRIDIUM, new UInt32(0x00000020L) });
            params.add(new Object[] { MMModemCapability.MM_MODEM_CAPABILITY_5GNR, new UInt32(0x00000040L) });
            params.add(new Object[] { MMModemCapability.MM_MODEM_CAPABILITY_TDS, new UInt32(0x00000080L) });
            params.add(new Object[] { MMModemCapability.MM_MODEM_CAPABILITY_ANY, new UInt32(0xFFFFFFFFL) });
            return params;
        }

        private final MMModemCapability inputModemCapability;
        private final UInt32 expectedIntValue;
        private UInt32 calculatedUInt32;

        public MMModemCapabilityToUInt32Test(MMModemCapability ipFamily, UInt32 intValue) {
            this.expectedIntValue = intValue;
            this.inputModemCapability = ipFamily;
        }

        @Test
        public void shouldReturnCorrectUInt32() {
            whenCalculatedUInt32();
            thenCalculatedUInt32IsCorrect();
        }

        private void whenCalculatedUInt32() {
            this.calculatedUInt32 = this.inputModemCapability.toUInt32();
        }

        private void thenCalculatedUInt32IsCorrect() {
            assertEquals(this.expectedIntValue, this.calculatedUInt32);
        }
    }
}

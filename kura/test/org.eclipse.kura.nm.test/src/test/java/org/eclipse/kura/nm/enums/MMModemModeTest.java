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

import org.eclipse.kura.net.status.modem.ModemMode;
import org.freedesktop.dbus.types.UInt32;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Enclosed.class)
public class MMModemModeTest {

    @RunWith(Parameterized.class)
    public static class MMModemModeToMMModemModeTest {

        @Parameters
        public static Collection<Object[]> ModemModeParams() {
            List<Object[]> params = new ArrayList<>();
            params.add(new Object[] { new UInt32(0x00000000L), MMModemMode.MM_MODEM_MODE_NONE });
            params.add(new Object[] { new UInt32(0x00000001L), MMModemMode.MM_MODEM_MODE_CS });
            params.add(new Object[] { new UInt32(0x00000002L), MMModemMode.MM_MODEM_MODE_2G });
            params.add(new Object[] { new UInt32(0x00000004L), MMModemMode.MM_MODEM_MODE_3G });
            params.add(new Object[] { new UInt32(0x00000008L), MMModemMode.MM_MODEM_MODE_4G });
            params.add(new Object[] { new UInt32(0x00000010L), MMModemMode.MM_MODEM_MODE_5G });
            params.add(new Object[] { new UInt32(0xFFFFFFFFL), MMModemMode.MM_MODEM_MODE_ANY });
            params.add(new Object[] { new UInt32(0x12345678L), MMModemMode.MM_MODEM_MODE_NONE });
            return params;
        }

        private final UInt32 inputIntValue;
        private final MMModemMode expectedModemMode;
        private MMModemMode calculatedModemMode;

        public MMModemModeToMMModemModeTest(UInt32 intValue, MMModemMode ipFamily) {
            this.inputIntValue = intValue;
            this.expectedModemMode = ipFamily;
        }

        @Test
        public void shouldReturnCorrectModemMode() {
            whenCalculateMMModemMode();
            thenCalculatedMMModemModeIsCorrect();
        }

        private void whenCalculateMMModemMode() {
            this.calculatedModemMode = MMModemMode.toMMModemMode(this.inputIntValue);
        }

        private void thenCalculatedMMModemModeIsCorrect() {
            assertEquals(this.expectedModemMode, this.calculatedModemMode);
        }
    }

    @RunWith(Parameterized.class)
    public static class MMModemModeToModemModeTest {

        @Parameters
        public static Collection<Object[]> ModemModeParams() {
            List<Object[]> params = new ArrayList<>();
            params.add(new Object[] { new UInt32(0x00000000L), ModemMode.NONE });
            params.add(new Object[] { new UInt32(0x00000001L), ModemMode.CS });
            params.add(new Object[] { new UInt32(0x00000002L), ModemMode.MODE_2G });
            params.add(new Object[] { new UInt32(0x00000004L), ModemMode.MODE_3G });
            params.add(new Object[] { new UInt32(0x00000008L), ModemMode.MODE_4G });
            params.add(new Object[] { new UInt32(0x00000010L), ModemMode.MODE_5G });
            params.add(new Object[] { new UInt32(0xFFFFFFFFL), ModemMode.ANY });
            params.add(new Object[] { new UInt32(0x12345678L), ModemMode.NONE });
            return params;
        }

        private final UInt32 inputIntValue;
        private final ModemMode expectedModemMode;
        private ModemMode calculatedModemMode;

        public MMModemModeToModemModeTest(UInt32 intValue, ModemMode modemMode) {
            this.inputIntValue = intValue;
            this.expectedModemMode = modemMode;
        }

        @Test
        public void shouldReturnCorrectModemMode() {
            whenCalculatedModemMode();
            thenCalculatedModemModeIsCorrect();
        }

        private void whenCalculatedModemMode() {
            this.calculatedModemMode = MMModemMode.toModemMode(this.inputIntValue);
        }

        private void thenCalculatedModemModeIsCorrect() {
            assertEquals(this.expectedModemMode, this.calculatedModemMode);
        }
    }

    @RunWith(Parameterized.class)
    public static class MMModemModeToModemModeFromBitMaskTest {

        @Parameters
        public static Collection<Object[]> ModemModeParams() {
            List<Object[]> params = new ArrayList<>();
            params.add(new Object[] { new UInt32(0x00000000L), EnumSet.of(ModemMode.NONE) });
            params.add(new Object[] { new UInt32(0x00000003L), EnumSet.of(ModemMode.CS, ModemMode.MODE_2G) });
            params.add(new Object[] { new UInt32(0x00000006L), EnumSet.of(ModemMode.MODE_2G, ModemMode.MODE_3G) });
            params.add(new Object[] { new UInt32(0x00000004L), EnumSet.of(ModemMode.MODE_3G) });
            params.add(new Object[] { new UInt32(0x0000000FL),
                    EnumSet.of(ModemMode.CS, ModemMode.MODE_2G, ModemMode.MODE_3G, ModemMode.MODE_4G) });
            params.add(new Object[] { new UInt32(0xFFFFFFFFL), EnumSet.of(ModemMode.ANY) });
            params.add(new Object[] { new UInt32(0x12345600L), EnumSet.noneOf(ModemMode.class) });
            return params;
        }

        private final UInt32 inputIntValue;
        private final Set<ModemMode> expectedModemModes;
        private Set<ModemMode> calculatedModemModes;

        public MMModemModeToModemModeFromBitMaskTest(UInt32 intValue, Set<ModemMode> modemModes) {
            this.inputIntValue = intValue;
            this.expectedModemModes = modemModes;
        }

        @Test
        public void shouldReturnCorrectModemModes() {
            whenCalculatedModemModes();
            thenCalculatedModemModesIsCorrect();
        }

        private void whenCalculatedModemModes() {
            this.calculatedModemModes = MMModemMode.toModemModeFromBitMask(this.inputIntValue);
        }

        private void thenCalculatedModemModesIsCorrect() {
            assertEquals(this.expectedModemModes, this.calculatedModemModes);
        }

    }

    @RunWith(Parameterized.class)
    public static class MMModemModeToUInt32Test {

        @Parameters
        public static Collection<Object[]> ModemModeParams() {
            List<Object[]> params = new ArrayList<>();
            params.add(new Object[] { MMModemMode.MM_MODEM_MODE_NONE, new UInt32(0x00000000L), });
            params.add(new Object[] { MMModemMode.MM_MODEM_MODE_CS, new UInt32(0x00000001L) });
            params.add(new Object[] { MMModemMode.MM_MODEM_MODE_2G, new UInt32(0x00000002L) });
            params.add(new Object[] { MMModemMode.MM_MODEM_MODE_3G, new UInt32(0x00000004L) });
            params.add(new Object[] { MMModemMode.MM_MODEM_MODE_4G, new UInt32(0x00000008L) });
            params.add(new Object[] { MMModemMode.MM_MODEM_MODE_5G, new UInt32(0x00000010L) });
            params.add(new Object[] { MMModemMode.MM_MODEM_MODE_ANY, new UInt32(0xFFFFFFFFL) });
            return params;
        }

        private final MMModemMode inputModemMode;
        private final UInt32 expectedIntValue;
        private UInt32 calculatedUInt32;

        public MMModemModeToUInt32Test(MMModemMode modemMode, UInt32 intValue) {
            this.expectedIntValue = intValue;
            this.inputModemMode = modemMode;
        }

        @Test
        public void shouldReturnCorrectUInt32() {
            whenCalculatedUInt32();
            thenCalculatedUInt32IsCorrect();
        }

        private void whenCalculatedUInt32() {
            this.calculatedUInt32 = this.inputModemMode.toUInt32();
        }

        private void thenCalculatedUInt32IsCorrect() {
            assertEquals(this.expectedIntValue, this.calculatedUInt32);
        }
    }
}

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

import org.eclipse.kura.net.status.modem.ESimStatus;
import org.freedesktop.dbus.types.UInt32;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Enclosed.class)
public class MMSimESimStatusTest {

    @RunWith(Parameterized.class)
    public static class MMSimEsimStatusToMMSimEsimStatusTest {

        @Parameters
        public static Collection<Object[]> SimEsimStatusParams() {
            List<Object[]> params = new ArrayList<>();
            params.add(new Object[] { new UInt32(0x00), MMSimEsimStatus.MM_SIM_ESIM_STATUS_UNKNOWN });
            params.add(new Object[] { new UInt32(0x01), MMSimEsimStatus.MM_SIM_ESIM_STATUS_NO_PROFILES });
            params.add(new Object[] { new UInt32(0x02), MMSimEsimStatus.MM_SIM_ESIM_STATUS_WITH_PROFILES });
            params.add(new Object[] { new UInt32(0x13), MMSimEsimStatus.MM_SIM_ESIM_STATUS_UNKNOWN });
            return params;
        }

        private final UInt32 inputIntValue;
        private final MMSimEsimStatus expectedEsimStatus;
        private MMSimEsimStatus calculatedEsimStatus;

        public MMSimEsimStatusToMMSimEsimStatusTest(UInt32 intValue, MMSimEsimStatus eSimStatus) {
            this.inputIntValue = intValue;
            this.expectedEsimStatus = eSimStatus;
        }

        @Test
        public void shouldReturnCorrectMMSimESimStatus() {
            whenCalculateMMSimESimStatus();
            thenCalculatedMMSimESimStatusIsCorrect();
        }

        private void whenCalculateMMSimESimStatus() {
            this.calculatedEsimStatus = MMSimEsimStatus.toMMSimEsimStatus(this.inputIntValue);
        }

        private void thenCalculatedMMSimESimStatusIsCorrect() {
            assertEquals(this.expectedEsimStatus, this.calculatedEsimStatus);
        }
    }

    @RunWith(Parameterized.class)
    public static class MMSimEsimStatusToESimStatusTest {

        @Parameters
        public static Collection<Object[]> SimEsimStatusParams() {
            List<Object[]> params = new ArrayList<>();
            params.add(new Object[] { new UInt32(0x00), ESimStatus.UNKNOWN });
            params.add(new Object[] { new UInt32(0x01), ESimStatus.NO_PROFILES });
            params.add(new Object[] { new UInt32(0x02), ESimStatus.WITH_PROFILES });
            params.add(new Object[] { new UInt32(0x13), ESimStatus.UNKNOWN });
            return params;
        }

        private final UInt32 inputIntValue;
        private final ESimStatus expectedEsimStatus;
        private ESimStatus calculatedEsimStatus;

        public MMSimEsimStatusToESimStatusTest(UInt32 intValue, ESimStatus eSimStatus) {
            this.inputIntValue = intValue;
            this.expectedEsimStatus = eSimStatus;
        }

        @Test
        public void shouldReturnCorrectESimStatus() {
            whenCalculatedESimStatus();
            thenCalculatedESimStatusIsCorrect();
        }

        private void whenCalculatedESimStatus() {
            this.calculatedEsimStatus = MMSimEsimStatus.toESimStatus(this.inputIntValue);
        }

        private void thenCalculatedESimStatusIsCorrect() {
            assertEquals(this.expectedEsimStatus, this.calculatedEsimStatus);
        }
    }

    @RunWith(Parameterized.class)
    public static class MMSimEsimStatusToUInt32Test {

        @Parameters
        public static Collection<Object[]> SimEsimStatusParams() {
            List<Object[]> params = new ArrayList<>();
            params.add(new Object[] { MMSimEsimStatus.MM_SIM_ESIM_STATUS_UNKNOWN, new UInt32(0x00) });
            params.add(new Object[] { MMSimEsimStatus.MM_SIM_ESIM_STATUS_NO_PROFILES, new UInt32(0x01) });
            params.add(new Object[] { MMSimEsimStatus.MM_SIM_ESIM_STATUS_WITH_PROFILES, new UInt32(0x02) });
            return params;
        }

        private final MMSimEsimStatus inputEsimStatus;
        private final UInt32 expectedIntValue;
        private UInt32 calculatedUInt32;

        public MMSimEsimStatusToUInt32Test(MMSimEsimStatus eSimStatus, UInt32 intValue) {
            this.expectedIntValue = intValue;
            this.inputEsimStatus = eSimStatus;
        }

        @Test
        public void shouldReturnCorrectUInt32() {
            whenCalculatedUInt32();
            thenCalculatedUInt32IsCorrect();
        }

        private void whenCalculatedUInt32() {
            this.calculatedUInt32 = this.inputEsimStatus.toUInt32();
        }

        private void thenCalculatedUInt32IsCorrect() {
            assertEquals(this.expectedIntValue, this.calculatedUInt32);
        }
    }
}

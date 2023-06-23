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

import org.eclipse.kura.net.status.modem.SimType;
import org.freedesktop.dbus.types.UInt32;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Enclosed.class)
public class MMSimTypeTest {

    @RunWith(Parameterized.class)
    public static class MMSimTypeToMMSimTypeTest {

        @Parameters
        public static Collection<Object[]> SimTypeParams() {
            List<Object[]> params = new ArrayList<>();
            params.add(new Object[] { new UInt32(0x00), MMSimType.MM_SIM_TYPE_UNKNOWN });
            params.add(new Object[] { new UInt32(0x01), MMSimType.MM_SIM_TYPE_PHYSICAL });
            params.add(new Object[] { new UInt32(0x02), MMSimType.MM_SIM_TYPE_ESIM });
            params.add(new Object[] { new UInt32(0x14), MMSimType.MM_SIM_TYPE_UNKNOWN });
            return params;
        }

        private final UInt32 inputIntValue;
        private final MMSimType expectedSimType;
        private MMSimType calculatedSimType;

        public MMSimTypeToMMSimTypeTest(UInt32 intValue, MMSimType simType) {
            this.inputIntValue = intValue;
            this.expectedSimType = simType;
        }

        @Test
        public void shouldReturnCorrectMMSImType() {
            whenCalculateMMSimType();
            thenCalculatedMMSimTypeIsCorrect();
        }

        private void whenCalculateMMSimType() {
            this.calculatedSimType = MMSimType.toMMSimType(this.inputIntValue);
        }

        private void thenCalculatedMMSimTypeIsCorrect() {
            assertEquals(this.expectedSimType, this.calculatedSimType);
        }
    }

    @RunWith(Parameterized.class)
    public static class MMSimTypeToSimTypeTest {

        @Parameters
        public static Collection<Object[]> SimTypeParams() {
            List<Object[]> params = new ArrayList<>();
            params.add(new Object[] { new UInt32(0x00), SimType.UNKNOWN });
            params.add(new Object[] { new UInt32(0x01), SimType.PHYSICAL });
            params.add(new Object[] { new UInt32(0x02), SimType.ESIM });
            params.add(new Object[] { new UInt32(0x14), SimType.UNKNOWN });
            return params;
        }

        private final UInt32 inputIntValue;
        private final SimType expectedSimType;
        private SimType calculatedSimType;

        public MMSimTypeToSimTypeTest(UInt32 intValue, SimType simType) {
            this.inputIntValue = intValue;
            this.expectedSimType = simType;
        }

        @Test
        public void shouldReturnCorrectSimType() {
            whenCalculatedSimType();
            thenCalculatedSimTypeIsCorrect();
        }

        private void whenCalculatedSimType() {
            this.calculatedSimType = MMSimType.toSimType(this.inputIntValue);
        }

        private void thenCalculatedSimTypeIsCorrect() {
            assertEquals(this.expectedSimType, this.calculatedSimType);
        }
    }

    @RunWith(Parameterized.class)
    public static class MMSimTypeToUInt32Test {

        @Parameters
        public static Collection<Object[]> SimTypeParams() {
            List<Object[]> params = new ArrayList<>();
            params.add(new Object[] { MMSimType.MM_SIM_TYPE_UNKNOWN, new UInt32(0x00), });
            params.add(new Object[] { MMSimType.MM_SIM_TYPE_PHYSICAL, new UInt32(0x01) });
            params.add(new Object[] { MMSimType.MM_SIM_TYPE_ESIM, new UInt32(0x02) });
            return params;
        }

        private final MMSimType inputSimType;
        private final UInt32 expectedIntValue;
        private UInt32 calculatedUInt32;

        public MMSimTypeToUInt32Test(MMSimType simType, UInt32 intValue) {
            this.expectedIntValue = intValue;
            this.inputSimType = simType;
        }

        @Test
        public void shouldReturnCorrectUInt32() {
            whenCalculatedUInt32();
            thenCalculatedUInt32IsCorrect();
        }

        private void whenCalculatedUInt32() {
            this.calculatedUInt32 = this.inputSimType.toUInt32();
        }

        private void thenCalculatedUInt32IsCorrect() {
            assertEquals(this.expectedIntValue, this.calculatedUInt32);
        }
    }
}

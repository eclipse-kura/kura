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

import org.eclipse.kura.net.status.modem.BearerIpType;
import org.freedesktop.dbus.types.UInt32;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Enclosed.class)
public class MMBearerIpFamilyTest {

    @RunWith(Parameterized.class)
    public static class MMBearerIpFamilyToMMBearerIpFamilyTest {

        @Parameters
        public static Collection<Object[]> BearerIpFamilyParams() {
            List<Object[]> params = new ArrayList<>();
            params.add(new Object[] { new UInt32(0x00000000L), MMBearerIpFamily.MM_BEARER_IP_FAMILY_NONE });
            params.add(new Object[] { new UInt32(0x00000001L), MMBearerIpFamily.MM_BEARER_IP_FAMILY_IPV4 });
            params.add(new Object[] { new UInt32(0x00000002L), MMBearerIpFamily.MM_BEARER_IP_FAMILY_IPV6 });
            params.add(new Object[] { new UInt32(0x00000004L), MMBearerIpFamily.MM_BEARER_IP_FAMILY_IPV4V6 });
            params.add(new Object[] { new UInt32(0x00000008L), MMBearerIpFamily.MM_BEARER_IP_FAMILY_NON_IP });
            params.add(new Object[] { new UInt32(0xFFFFFFF7L), MMBearerIpFamily.MM_BEARER_IP_FAMILY_ANY });
            params.add(new Object[] { new UInt32(0x12345678L), MMBearerIpFamily.MM_BEARER_IP_FAMILY_NONE });
            return params;
        }

        private final UInt32 inputIntValue;
        private final MMBearerIpFamily expectedIpFamily;
        private MMBearerIpFamily calculatedIpFamily;

        public MMBearerIpFamilyToMMBearerIpFamilyTest(UInt32 intValue, MMBearerIpFamily ipFamily) {
            this.inputIntValue = intValue;
            this.expectedIpFamily = ipFamily;
        }

        @Test
        public void shouldReturnCorrectIpFamily() {
            whenCalculateMMBearerIpFamily();
            thenCalculatedMMBearerIpFamilyIsCorrect();
        }

        private void whenCalculateMMBearerIpFamily() {
            this.calculatedIpFamily = MMBearerIpFamily.toMMBearerIpFamily(this.inputIntValue);
        }

        private void thenCalculatedMMBearerIpFamilyIsCorrect() {
            assertEquals(this.expectedIpFamily, this.calculatedIpFamily);
        }
    }

    @RunWith(Parameterized.class)
    public static class MMBearerIpFamilyToBearerIpTypeTest {

        @Parameters
        public static Collection<Object[]> BearerIpFamilyParams() {
            List<Object[]> params = new ArrayList<>();
            params.add(new Object[] { new UInt32(0x00000000L), BearerIpType.NONE });
            params.add(new Object[] { new UInt32(0x00000001L), BearerIpType.IPV4 });
            params.add(new Object[] { new UInt32(0x00000002L), BearerIpType.IPV6 });
            params.add(new Object[] { new UInt32(0x00000004L), BearerIpType.IPV4V6 });
            params.add(new Object[] { new UInt32(0x00000008L), BearerIpType.NON_IP });
            params.add(new Object[] { new UInt32(0xFFFFFFF7L), BearerIpType.ANY });
            params.add(new Object[] { new UInt32(0x12345678L), BearerIpType.NONE });
            return params;
        }

        private final UInt32 inputIntValue;
        private final BearerIpType expectedIpFamily;
        private BearerIpType calculatedIpFamily;

        public MMBearerIpFamilyToBearerIpTypeTest(UInt32 intValue, BearerIpType ipFamily) {
            this.inputIntValue = intValue;
            this.expectedIpFamily = ipFamily;
        }

        @Test
        public void shouldReturnCorrectIpFamily() {
            whenCalculatedBearerIpType();
            thenCalculatedBearerIpTypeIsCorrect();
        }

        private void whenCalculatedBearerIpType() {
            this.calculatedIpFamily = MMBearerIpFamily.toBearerIpType(this.inputIntValue);
        }

        private void thenCalculatedBearerIpTypeIsCorrect() {
            assertEquals(this.expectedIpFamily, this.calculatedIpFamily);
        }
    }

    @RunWith(Parameterized.class)
    public static class MMBearerIpFamilyToBearerIpTypeFromBitMaskTest {

        @Parameters
        public static Collection<Object[]> BearerIpFamilyParams() {
            List<Object[]> params = new ArrayList<>();
            params.add(new Object[] { new UInt32(0x00000000L), EnumSet.of(BearerIpType.NONE) });
            params.add(new Object[] { new UInt32(0x00000003L), EnumSet.of(BearerIpType.IPV4, BearerIpType.IPV6) });
            params.add(new Object[] { new UInt32(0x00000006L), EnumSet.of(BearerIpType.IPV6, BearerIpType.IPV4V6) });
            params.add(new Object[] { new UInt32(0x00000004L), EnumSet.of(BearerIpType.IPV4V6) });
            params.add(new Object[] { new UInt32(0x0000000FL),
                    EnumSet.of(BearerIpType.IPV4, BearerIpType.IPV6, BearerIpType.IPV4V6, BearerIpType.NON_IP) });
            params.add(new Object[] { new UInt32(0xFFFFFFF7L), EnumSet.of(BearerIpType.ANY) });
            params.add(new Object[] { new UInt32(0x12345670L), EnumSet.noneOf(BearerIpType.class) });
            return params;
        }

        private final UInt32 inputIntValue;
        private final Set<BearerIpType> expectedIpFamilies;
        private Set<BearerIpType> calculatedIpFamily;

        public MMBearerIpFamilyToBearerIpTypeFromBitMaskTest(UInt32 intValue, Set<BearerIpType> ipFamilies) {
            this.inputIntValue = intValue;
            this.expectedIpFamilies = ipFamilies;
        }

        @Test
        public void shouldReturnCorrectIpFamily() {
            whenCalculatedBearerIpType();
            thenCalculatedBearerIpTypeIsCorrect();
        }

        private void whenCalculatedBearerIpType() {
            this.calculatedIpFamily = MMBearerIpFamily.toBearerIpTypeFromBitMask(this.inputIntValue);
        }

        private void thenCalculatedBearerIpTypeIsCorrect() {
            assertEquals(this.expectedIpFamilies, this.calculatedIpFamily);
        }

    }

    @RunWith(Parameterized.class)
    public static class MMBearerIpFamilyToUInt32Test {

        @Parameters
        public static Collection<Object[]> BearerIpFamilyParams() {
            List<Object[]> params = new ArrayList<>();
            params.add(new Object[] { MMBearerIpFamily.MM_BEARER_IP_FAMILY_NONE, new UInt32(0x00000000L) });
            params.add(new Object[] { MMBearerIpFamily.MM_BEARER_IP_FAMILY_IPV4, new UInt32(0x00000001L) });
            params.add(new Object[] { MMBearerIpFamily.MM_BEARER_IP_FAMILY_IPV6, new UInt32(0x00000002L) });
            params.add(new Object[] { MMBearerIpFamily.MM_BEARER_IP_FAMILY_IPV4V6, new UInt32(0x00000004L) });
            params.add(new Object[] { MMBearerIpFamily.MM_BEARER_IP_FAMILY_NON_IP, new UInt32(0x00000008L) });
            params.add(new Object[] { MMBearerIpFamily.MM_BEARER_IP_FAMILY_ANY, new UInt32(0xFFFFFFF7L) });
            return params;
        }

        private final MMBearerIpFamily inputIpFamily;
        private final UInt32 expectedIntValue;
        private UInt32 calculatedUInt32;

        public MMBearerIpFamilyToUInt32Test(MMBearerIpFamily ipFamily, UInt32 intValue) {
            this.expectedIntValue = intValue;
            this.inputIpFamily = ipFamily;
        }

        @Test
        public void shouldReturnCorrectUInt32() {
            whenCalculatedUInt32();
            thenCalculatedUInt32IsCorrect();
        }

        private void whenCalculatedUInt32() {
            this.calculatedUInt32 = this.inputIpFamily.toUInt32();
        }

        private void thenCalculatedUInt32IsCorrect() {
            assertEquals(this.expectedIntValue, this.calculatedUInt32);
        }
    }
}

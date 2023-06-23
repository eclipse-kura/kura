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

import org.freedesktop.dbus.types.UInt32;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Enclosed.class)
public class MMModemLocationSourceTest {

    @RunWith(Parameterized.class)
    public static class MMModemLocationSourceToMMModemLocationSourceTest {

        @Parameters
        public static Collection<Object[]> locationSourceParams() {
            List<Object[]> params = new ArrayList<>();
            params.add(new Object[] { new UInt32(0x00000000L), MMModemLocationSource.MM_MODEM_LOCATION_SOURCE_NONE });
            params.add(new Object[] { new UInt32(0x00000001L),
                    MMModemLocationSource.MM_MODEM_LOCATION_SOURCE_3GPP_LAC_CI });
            params.add(
                    new Object[] { new UInt32(0x00000002L), MMModemLocationSource.MM_MODEM_LOCATION_SOURCE_GPS_RAW });
            params.add(
                    new Object[] { new UInt32(0x00000004L), MMModemLocationSource.MM_MODEM_LOCATION_SOURCE_GPS_NMEA });
            params.add(
                    new Object[] { new UInt32(0x00000008L), MMModemLocationSource.MM_MODEM_LOCATION_SOURCE_CDMA_BS });
            params.add(new Object[] { new UInt32(0x00000010L),
                    MMModemLocationSource.MM_MODEM_LOCATION_SOURCE_GPS_UNMANAGED });
            params.add(
                    new Object[] { new UInt32(0x00000020L), MMModemLocationSource.MM_MODEM_LOCATION_SOURCE_AGPS_MSA });
            params.add(
                    new Object[] { new UInt32(0x00000040L), MMModemLocationSource.MM_MODEM_LOCATION_SOURCE_AGPS_MSB });
            params.add(new Object[] { new UInt32(0x12345600L), MMModemLocationSource.MM_MODEM_LOCATION_SOURCE_NONE });
            return params;
        }

        private final UInt32 inputIntValue;
        private final MMModemLocationSource expectedLocationSource;
        private MMModemLocationSource calculatedLocationSource;

        public MMModemLocationSourceToMMModemLocationSourceTest(UInt32 intValue, MMModemLocationSource locationSource) {
            this.inputIntValue = intValue;
            this.expectedLocationSource = locationSource;
        }

        @Test
        public void shouldReturnCorrectMMLocationSource() {
            whenCalculateMMModemLocationSource();
            thenCalculatedMMModemLocationSourceIsCorrect();
        }

        private void whenCalculateMMModemLocationSource() {
            this.calculatedLocationSource = MMModemLocationSource.toMMModemLocationSource(this.inputIntValue);
        }

        private void thenCalculatedMMModemLocationSourceIsCorrect() {
            assertEquals(this.expectedLocationSource, this.calculatedLocationSource);
        }
    }

    @RunWith(Parameterized.class)
    public static class MMModemLocationSourceToMMModemLocationSourceFromBitMaskTest {

        @Parameters
        public static Collection<Object[]> locationSourceParams() {
            List<Object[]> params = new ArrayList<>();
            params.add(new Object[] { new UInt32(0x00000000L),
                    EnumSet.of(MMModemLocationSource.MM_MODEM_LOCATION_SOURCE_NONE) });
            params.add(new Object[] { new UInt32(0x00000003L),
                    EnumSet.of(MMModemLocationSource.MM_MODEM_LOCATION_SOURCE_3GPP_LAC_CI,
                            MMModemLocationSource.MM_MODEM_LOCATION_SOURCE_GPS_RAW) });
            params.add(new Object[] { new UInt32(0x00000006L),
                    EnumSet.of(MMModemLocationSource.MM_MODEM_LOCATION_SOURCE_GPS_RAW,
                            MMModemLocationSource.MM_MODEM_LOCATION_SOURCE_GPS_NMEA) });
            params.add(new Object[] { new UInt32(0x00000004L),
                    EnumSet.of(MMModemLocationSource.MM_MODEM_LOCATION_SOURCE_GPS_NMEA) });
            params.add(new Object[] { new UInt32(0x0000000FL),
                    EnumSet.of(MMModemLocationSource.MM_MODEM_LOCATION_SOURCE_3GPP_LAC_CI,
                            MMModemLocationSource.MM_MODEM_LOCATION_SOURCE_GPS_RAW,
                            MMModemLocationSource.MM_MODEM_LOCATION_SOURCE_GPS_NMEA,
                            MMModemLocationSource.MM_MODEM_LOCATION_SOURCE_CDMA_BS) });
            params.add(new Object[] { new UInt32(0x12345600L), EnumSet.noneOf(MMModemLocationSource.class) });
            return params;
        }

        private final UInt32 inputIntValue;
        private final Set<MMModemLocationSource> expectedLocationSource;
        private Set<MMModemLocationSource> calculatedLocationSource;

        public MMModemLocationSourceToMMModemLocationSourceFromBitMaskTest(UInt32 intValue,
                Set<MMModemLocationSource> locationSource) {
            this.inputIntValue = intValue;
            this.expectedLocationSource = locationSource;
        }

        @Test
        public void shouldReturnCorrectLocationSourceSet() {
            whenConversionMethodIsCalled();
            thenCalculatedLocationSourceSetMatches();
        }

        private void whenConversionMethodIsCalled() {
            this.calculatedLocationSource = MMModemLocationSource
                    .toMMModemLocationSourceFromBitMask(this.inputIntValue);
        }

        private void thenCalculatedLocationSourceSetMatches() {
            assertEquals(this.expectedLocationSource, this.calculatedLocationSource);
        }

    }

    @RunWith(Parameterized.class)
    public static class MMModemLocationSourceToBitMaskFromMMModemLocationSourceTest {

        @Parameters
        public static Collection<Object[]> locationSourceParams() {
            List<Object[]> params = new ArrayList<>();
            params.add(new Object[] { EnumSet.of(MMModemLocationSource.MM_MODEM_LOCATION_SOURCE_NONE),
                    new UInt32(0x00000000L) });
            params.add(
                    new Object[] {
                            EnumSet.of(MMModemLocationSource.MM_MODEM_LOCATION_SOURCE_3GPP_LAC_CI,
                                    MMModemLocationSource.MM_MODEM_LOCATION_SOURCE_GPS_RAW),
                            new UInt32(0x00000003L), });
            params.add(
                    new Object[] {
                            EnumSet.of(MMModemLocationSource.MM_MODEM_LOCATION_SOURCE_3GPP_LAC_CI,
                                    MMModemLocationSource.MM_MODEM_LOCATION_SOURCE_GPS_RAW,
                                    MMModemLocationSource.MM_MODEM_LOCATION_SOURCE_GPS_NMEA),
                            new UInt32(0x00000007L) });
            params.add(
                    new Object[] {
                            EnumSet.of(MMModemLocationSource.MM_MODEM_LOCATION_SOURCE_GPS_RAW,
                                    MMModemLocationSource.MM_MODEM_LOCATION_SOURCE_GPS_NMEA),
                            new UInt32(0x00000006L) });
            params.add(new Object[] { EnumSet.of(MMModemLocationSource.MM_MODEM_LOCATION_SOURCE_GPS_NMEA),
                    new UInt32(0x00000004L) });
            params.add(new Object[] { EnumSet.of(MMModemLocationSource.MM_MODEM_LOCATION_SOURCE_3GPP_LAC_CI,
                    MMModemLocationSource.MM_MODEM_LOCATION_SOURCE_GPS_RAW,
                    MMModemLocationSource.MM_MODEM_LOCATION_SOURCE_GPS_NMEA,
                    MMModemLocationSource.MM_MODEM_LOCATION_SOURCE_CDMA_BS), new UInt32(0x0000000FL) });
            return params;
        }

        private final Set<MMModemLocationSource> inputSet;
        private final UInt32 expectedBitmask;
        private UInt32 calculatedBitmask;

        public MMModemLocationSourceToBitMaskFromMMModemLocationSourceTest(Set<MMModemLocationSource> locationSources,
                UInt32 bitmask) {
            this.inputSet = locationSources;
            this.expectedBitmask = bitmask;
        }

        @Test
        public void shouldReturnCorrectBitmask() {
            whenMethodIsCalled();
            thenCalculatedAndExpectedBitmaskMatch();
        }

        private void whenMethodIsCalled() {
            this.calculatedBitmask = MMModemLocationSource.toBitMaskFromMMModemLocationSource(this.inputSet);
        }

        private void thenCalculatedAndExpectedBitmaskMatch() {
            assertEquals(this.expectedBitmask, this.calculatedBitmask);
        }

    }

    @RunWith(Parameterized.class)
    public static class MMModemLocationSourceToUInt32Test {

        @Parameters
        public static Collection<Object[]> LocationSourceParams() {
            List<Object[]> params = new ArrayList<>();
            params.add(new Object[] { MMModemLocationSource.MM_MODEM_LOCATION_SOURCE_NONE, new UInt32(0x00000000L) });
            params.add(new Object[] { MMModemLocationSource.MM_MODEM_LOCATION_SOURCE_3GPP_LAC_CI,
                    new UInt32(0x00000001L) });
            params.add(
                    new Object[] { MMModemLocationSource.MM_MODEM_LOCATION_SOURCE_GPS_RAW, new UInt32(0x00000002L) });
            params.add(
                    new Object[] { MMModemLocationSource.MM_MODEM_LOCATION_SOURCE_GPS_NMEA, new UInt32(0x00000004L) });
            params.add(
                    new Object[] { MMModemLocationSource.MM_MODEM_LOCATION_SOURCE_CDMA_BS, new UInt32(0x00000008L) });
            params.add(new Object[] { MMModemLocationSource.MM_MODEM_LOCATION_SOURCE_GPS_UNMANAGED,
                    new UInt32(0x00000010L) });
            params.add(
                    new Object[] { MMModemLocationSource.MM_MODEM_LOCATION_SOURCE_AGPS_MSA, new UInt32(0x00000020L) });
            params.add(
                    new Object[] { MMModemLocationSource.MM_MODEM_LOCATION_SOURCE_AGPS_MSB, new UInt32(0x00000040L) });
            return params;
        }

        private final MMModemLocationSource inputLocationSource;
        private final UInt32 expectedIntValue;
        private UInt32 calculatedUInt32;

        public MMModemLocationSourceToUInt32Test(MMModemLocationSource locationSource, UInt32 intValue) {
            this.expectedIntValue = intValue;
            this.inputLocationSource = locationSource;
        }

        @Test
        public void shouldReturnCorrectUInt32() {
            whenCalculatedUInt32();
            thenCalculatedUInt32IsCorrect();
        }

        private void whenCalculatedUInt32() {
            this.calculatedUInt32 = this.inputLocationSource.toUInt32();
        }

        private void thenCalculatedUInt32IsCorrect() {
            assertEquals(this.expectedIntValue, this.calculatedUInt32);
        }
    }
}

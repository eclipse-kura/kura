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
package org.eclipse.kura.nm.status;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Enclosed.class)
public class NMStatusConverterSignalConverterTest {

    @RunWith(Parameterized.class)
    public static class ModemSignalStrengthConverterTest {

        @Parameters
        public static Collection<Object[]> ModemSignalQualityParams() {
            List<Object[]> params = new ArrayList<>();
            params.add(new Object[] { 0, -113 });
            params.add(new Object[] { 1, -112 });
            params.add(new Object[] { 2, -112 });
            params.add(new Object[] { 3, -111 });
            params.add(new Object[] { 4, -111 });
            params.add(new Object[] { 5, -110 });
            params.add(new Object[] { 6, -109 });
            params.add(new Object[] { 7, -109 });
            params.add(new Object[] { 8, -108 });
            params.add(new Object[] { 9, -108 });
            params.add(new Object[] { 10, -107 });
            params.add(new Object[] { 11, -106 });
            params.add(new Object[] { 12, -106 });
            params.add(new Object[] { 13, -105 });
            params.add(new Object[] { 14, -105 });
            params.add(new Object[] { 15, -104 });
            params.add(new Object[] { 16, -103 });
            params.add(new Object[] { 17, -103 });
            params.add(new Object[] { 18, -102 });
            params.add(new Object[] { 19, -102 });
            params.add(new Object[] { 20, -101 });
            params.add(new Object[] { 21, -100 });
            params.add(new Object[] { 22, -100 });
            params.add(new Object[] { 23, -99 });
            params.add(new Object[] { 24, -99 });
            params.add(new Object[] { 25, -98 });
            params.add(new Object[] { 26, -97 });
            params.add(new Object[] { 27, -97 });
            params.add(new Object[] { 28, -96 });
            params.add(new Object[] { 29, -96 });
            params.add(new Object[] { 30, -95 });
            params.add(new Object[] { 31, -94 });
            params.add(new Object[] { 32, -94 });
            params.add(new Object[] { 33, -93 });
            params.add(new Object[] { 34, -93 });
            params.add(new Object[] { 35, -92 });
            params.add(new Object[] { 36, -91 });
            params.add(new Object[] { 37, -91 });
            params.add(new Object[] { 38, -90 });
            params.add(new Object[] { 39, -90 });
            params.add(new Object[] { 40, -89 });
            params.add(new Object[] { 41, -88 });
            params.add(new Object[] { 42, -88 });
            params.add(new Object[] { 43, -87 });
            params.add(new Object[] { 44, -87 });
            params.add(new Object[] { 45, -86 });
            params.add(new Object[] { 46, -85 });
            params.add(new Object[] { 47, -85 });
            params.add(new Object[] { 48, -84 });
            params.add(new Object[] { 49, -84 });
            params.add(new Object[] { 50, -83 });
            params.add(new Object[] { 51, -82 });
            params.add(new Object[] { 52, -82 });
            params.add(new Object[] { 53, -81 });
            params.add(new Object[] { 54, -81 });
            params.add(new Object[] { 55, -80 });
            params.add(new Object[] { 56, -79 });
            params.add(new Object[] { 57, -79 });
            params.add(new Object[] { 58, -78 });
            params.add(new Object[] { 59, -78 });
            params.add(new Object[] { 60, -77 });
            params.add(new Object[] { 61, -76 });
            params.add(new Object[] { 62, -76 });
            params.add(new Object[] { 63, -75 });
            params.add(new Object[] { 64, -75 });
            params.add(new Object[] { 65, -74 });
            params.add(new Object[] { 66, -73 });
            params.add(new Object[] { 67, -73 });
            params.add(new Object[] { 68, -72 });
            params.add(new Object[] { 69, -72 });
            params.add(new Object[] { 70, -71 });
            params.add(new Object[] { 71, -70 });
            params.add(new Object[] { 72, -70 });
            params.add(new Object[] { 73, -69 });
            params.add(new Object[] { 74, -69 });
            params.add(new Object[] { 75, -68 });
            params.add(new Object[] { 76, -67 });
            params.add(new Object[] { 77, -67 });
            params.add(new Object[] { 78, -66 });
            params.add(new Object[] { 79, -66 });
            params.add(new Object[] { 80, -65 });
            params.add(new Object[] { 81, -64 });
            params.add(new Object[] { 82, -64 });
            params.add(new Object[] { 83, -63 });
            params.add(new Object[] { 84, -63 });
            params.add(new Object[] { 85, -62 });
            params.add(new Object[] { 86, -61 });
            params.add(new Object[] { 87, -61 });
            params.add(new Object[] { 88, -60 });
            params.add(new Object[] { 89, -60 });
            params.add(new Object[] { 90, -59 });
            params.add(new Object[] { 91, -58 });
            params.add(new Object[] { 92, -58 });
            params.add(new Object[] { 93, -57 });
            params.add(new Object[] { 94, -57 });
            params.add(new Object[] { 95, -56 });
            params.add(new Object[] { 96, -55 });
            params.add(new Object[] { 97, -55 });
            params.add(new Object[] { 98, -54 });
            params.add(new Object[] { 99, -54 });
            params.add(new Object[] { 100, -53 });
            return params;
        }

        private final int inputSignalQuality;
        private final int expectedSignalStrength;
        private int calculatedSignalStrength;

        public ModemSignalStrengthConverterTest(int inputSignalQuality, int expectedSignalStrength) {
            this.inputSignalQuality = inputSignalQuality;
            this.expectedSignalStrength = expectedSignalStrength;
        }

        @Test
        public void shouldReturnCorrectModemSignalStrength() {
            whenConvertModemSignalQuality(this.inputSignalQuality);
            thenCalculatedModemSignalStrengthIs(this.expectedSignalStrength);
        }

        private void whenConvertModemSignalQuality(int inputSignalQuality) {
            this.calculatedSignalStrength = NMStatusConverter.convertToModemSignalStrength(inputSignalQuality);
        }

        private void thenCalculatedModemSignalStrengthIs(int expectedSignalStrength) {
            assertEquals(expectedSignalStrength, this.calculatedSignalStrength);
        }
    }

    @RunWith(Parameterized.class)
    public static class WifiSignalStrengthConverterTest {

        @Parameters
        public static Collection<Object[]> WifiSignalQualityParams() {
            List<Object[]> params = new ArrayList<>();
            params.add(new Object[] { 0, -104 });
            params.add(new Object[] { 1, -104 });
            params.add(new Object[] { 2, -103 });
            params.add(new Object[] { 3, -102 });
            params.add(new Object[] { 4, -101 });
            params.add(new Object[] { 5, -100 });
            params.add(new Object[] { 6, -99 });
            params.add(new Object[] { 7, -99 });
            params.add(new Object[] { 8, -98 });
            params.add(new Object[] { 9, -97 });
            params.add(new Object[] { 10, -96 });
            params.add(new Object[] { 11, -95 });
            params.add(new Object[] { 12, -94 });
            params.add(new Object[] { 13, -93 });
            params.add(new Object[] { 14, -93 });
            params.add(new Object[] { 15, -92 });
            params.add(new Object[] { 16, -91 });
            params.add(new Object[] { 17, -90 });
            params.add(new Object[] { 18, -89 });
            params.add(new Object[] { 19, -88 });
            params.add(new Object[] { 20, -88 });
            params.add(new Object[] { 21, -87 });
            params.add(new Object[] { 22, -86 });
            params.add(new Object[] { 23, -85 });
            params.add(new Object[] { 24, -84 });
            params.add(new Object[] { 25, -83 });
            params.add(new Object[] { 26, -82 });
            params.add(new Object[] { 27, -82 });
            params.add(new Object[] { 28, -81 });
            params.add(new Object[] { 29, -80 });
            params.add(new Object[] { 30, -79 });
            params.add(new Object[] { 31, -78 });
            params.add(new Object[] { 32, -77 });
            params.add(new Object[] { 33, -77 });
            params.add(new Object[] { 34, -76 });
            params.add(new Object[] { 35, -75 });
            params.add(new Object[] { 36, -74 });
            params.add(new Object[] { 37, -73 });
            params.add(new Object[] { 38, -72 });
            params.add(new Object[] { 39, -71 });
            params.add(new Object[] { 40, -71 });
            params.add(new Object[] { 41, -70 });
            params.add(new Object[] { 42, -69 });
            params.add(new Object[] { 43, -68 });
            params.add(new Object[] { 44, -67 });
            params.add(new Object[] { 45, -66 });
            params.add(new Object[] { 46, -66 });
            params.add(new Object[] { 47, -65 });
            params.add(new Object[] { 48, -64 });
            params.add(new Object[] { 49, -63 });
            params.add(new Object[] { 50, -62 });
            params.add(new Object[] { 51, -61 });
            params.add(new Object[] { 52, -61 });
            params.add(new Object[] { 53, -60 });
            params.add(new Object[] { 54, -59 });
            params.add(new Object[] { 55, -58 });
            params.add(new Object[] { 56, -57 });
            params.add(new Object[] { 57, -56 });
            params.add(new Object[] { 58, -55 });
            params.add(new Object[] { 59, -55 });
            params.add(new Object[] { 60, -54 });
            params.add(new Object[] { 61, -53 });
            params.add(new Object[] { 62, -52 });
            params.add(new Object[] { 63, -51 });
            params.add(new Object[] { 64, -50 });
            params.add(new Object[] { 65, -50 });
            params.add(new Object[] { 66, -49 });
            params.add(new Object[] { 67, -48 });
            params.add(new Object[] { 68, -47 });
            params.add(new Object[] { 69, -46 });
            params.add(new Object[] { 70, -45 });
            params.add(new Object[] { 71, -44 });
            params.add(new Object[] { 72, -44 });
            params.add(new Object[] { 73, -43 });
            params.add(new Object[] { 74, -42 });
            params.add(new Object[] { 75, -41 });
            params.add(new Object[] { 76, -40 });
            params.add(new Object[] { 77, -39 });
            params.add(new Object[] { 78, -39 });
            params.add(new Object[] { 79, -38 });
            params.add(new Object[] { 80, -37 });
            params.add(new Object[] { 81, -36 });
            params.add(new Object[] { 82, -35 });
            params.add(new Object[] { 83, -34 });
            params.add(new Object[] { 84, -34 });
            params.add(new Object[] { 85, -33 });
            params.add(new Object[] { 86, -32 });
            params.add(new Object[] { 87, -31 });
            params.add(new Object[] { 88, -30 });
            params.add(new Object[] { 89, -29 });
            params.add(new Object[] { 90, -28 });
            params.add(new Object[] { 91, -28 });
            params.add(new Object[] { 92, -27 });
            params.add(new Object[] { 93, -26 });
            params.add(new Object[] { 94, -25 });
            params.add(new Object[] { 95, -24 });
            params.add(new Object[] { 96, -23 });
            params.add(new Object[] { 97, -23 });
            params.add(new Object[] { 98, -22 });
            params.add(new Object[] { 99, -21 });
            params.add(new Object[] { 100, -20 });
            return params;
        }

        private final int inputSignalQuality;
        private final int expectedSignalStrength;
        private int calculatedSignalStrength;

        public WifiSignalStrengthConverterTest(int inputSignalQuality, int expectedSignalStrength) {
            this.inputSignalQuality = inputSignalQuality;
            this.expectedSignalStrength = expectedSignalStrength;
        }

        @Test
        public void shouldReturnCorrectWifiSignalStrength() {
            whenConvertWifiSignalQuality(this.inputSignalQuality);
            thenCalculatedWifiSignalStrengthIs(this.expectedSignalStrength);
        }

        private void whenConvertWifiSignalQuality(int inputSignalQuality) {
            this.calculatedSignalStrength = NMStatusConverter.convertToWifiSignalStrength(inputSignalQuality);
        }

        private void thenCalculatedWifiSignalStrengthIs(int expectedSignalStrength) {
            assertEquals(expectedSignalStrength, this.calculatedSignalStrength);
        }
    }
}

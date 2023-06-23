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

import org.eclipse.kura.net.status.modem.ModemBand;
import org.freedesktop.dbus.types.UInt32;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Enclosed.class)
public class MMModemBandTest {

    @RunWith(Parameterized.class)
    public static class MMModemBandTestToMMModemBandTestTest {

        @Parameters
        public static Collection<Object[]> ModemBandParams() {
            List<Object[]> params = new ArrayList<>();
            params.add(new Object[] { new UInt32(0), MMModemBand.MM_MODEM_BAND_UNKNOWN });
            params.add(new Object[] { new UInt32(1), MMModemBand.MM_MODEM_BAND_EGSM });
            params.add(new Object[] { new UInt32(2), MMModemBand.MM_MODEM_BAND_DCS });
            params.add(new Object[] { new UInt32(3), MMModemBand.MM_MODEM_BAND_PCS });
            params.add(new Object[] { new UInt32(4), MMModemBand.MM_MODEM_BAND_G850 });
            params.add(new Object[] { new UInt32(5), MMModemBand.MM_MODEM_BAND_UTRAN_1 });
            params.add(new Object[] { new UInt32(6), MMModemBand.MM_MODEM_BAND_UTRAN_3 });
            params.add(new Object[] { new UInt32(7), MMModemBand.MM_MODEM_BAND_UTRAN_4 });
            params.add(new Object[] { new UInt32(8), MMModemBand.MM_MODEM_BAND_UTRAN_6 });
            params.add(new Object[] { new UInt32(9), MMModemBand.MM_MODEM_BAND_UTRAN_5 });
            params.add(new Object[] { new UInt32(10), MMModemBand.MM_MODEM_BAND_UTRAN_8 });
            params.add(new Object[] { new UInt32(11), MMModemBand.MM_MODEM_BAND_UTRAN_9 });
            params.add(new Object[] { new UInt32(12), MMModemBand.MM_MODEM_BAND_UTRAN_2 });
            params.add(new Object[] { new UInt32(13), MMModemBand.MM_MODEM_BAND_UTRAN_7 });
            params.add(new Object[] { new UInt32(14), MMModemBand.MM_MODEM_BAND_G450 });
            params.add(new Object[] { new UInt32(15), MMModemBand.MM_MODEM_BAND_G480 });
            params.add(new Object[] { new UInt32(16), MMModemBand.MM_MODEM_BAND_G750 });
            params.add(new Object[] { new UInt32(17), MMModemBand.MM_MODEM_BAND_G380 });
            params.add(new Object[] { new UInt32(18), MMModemBand.MM_MODEM_BAND_G410 });
            params.add(new Object[] { new UInt32(19), MMModemBand.MM_MODEM_BAND_G710 });
            params.add(new Object[] { new UInt32(20), MMModemBand.MM_MODEM_BAND_G810 });
            params.add(new Object[] { new UInt32(31), MMModemBand.MM_MODEM_BAND_EUTRAN_1 });
            params.add(new Object[] { new UInt32(32), MMModemBand.MM_MODEM_BAND_EUTRAN_2 });
            params.add(new Object[] { new UInt32(33), MMModemBand.MM_MODEM_BAND_EUTRAN_3 });
            params.add(new Object[] { new UInt32(34), MMModemBand.MM_MODEM_BAND_EUTRAN_4 });
            params.add(new Object[] { new UInt32(35), MMModemBand.MM_MODEM_BAND_EUTRAN_5 });
            params.add(new Object[] { new UInt32(36), MMModemBand.MM_MODEM_BAND_EUTRAN_6 });
            params.add(new Object[] { new UInt32(37), MMModemBand.MM_MODEM_BAND_EUTRAN_7 });
            params.add(new Object[] { new UInt32(38), MMModemBand.MM_MODEM_BAND_EUTRAN_8 });
            params.add(new Object[] { new UInt32(39), MMModemBand.MM_MODEM_BAND_EUTRAN_9 });
            params.add(new Object[] { new UInt32(40), MMModemBand.MM_MODEM_BAND_EUTRAN_10 });
            params.add(new Object[] { new UInt32(41), MMModemBand.MM_MODEM_BAND_EUTRAN_11 });
            params.add(new Object[] { new UInt32(42), MMModemBand.MM_MODEM_BAND_EUTRAN_12 });
            params.add(new Object[] { new UInt32(43), MMModemBand.MM_MODEM_BAND_EUTRAN_13 });
            params.add(new Object[] { new UInt32(44), MMModemBand.MM_MODEM_BAND_EUTRAN_14 });
            params.add(new Object[] { new UInt32(47), MMModemBand.MM_MODEM_BAND_EUTRAN_17 });
            params.add(new Object[] { new UInt32(48), MMModemBand.MM_MODEM_BAND_EUTRAN_18 });
            params.add(new Object[] { new UInt32(49), MMModemBand.MM_MODEM_BAND_EUTRAN_19 });
            params.add(new Object[] { new UInt32(50), MMModemBand.MM_MODEM_BAND_EUTRAN_20 });
            params.add(new Object[] { new UInt32(51), MMModemBand.MM_MODEM_BAND_EUTRAN_21 });
            params.add(new Object[] { new UInt32(52), MMModemBand.MM_MODEM_BAND_EUTRAN_22 });
            params.add(new Object[] { new UInt32(53), MMModemBand.MM_MODEM_BAND_EUTRAN_23 });
            params.add(new Object[] { new UInt32(54), MMModemBand.MM_MODEM_BAND_EUTRAN_24 });
            params.add(new Object[] { new UInt32(55), MMModemBand.MM_MODEM_BAND_EUTRAN_25 });
            params.add(new Object[] { new UInt32(56), MMModemBand.MM_MODEM_BAND_EUTRAN_26 });
            params.add(new Object[] { new UInt32(57), MMModemBand.MM_MODEM_BAND_EUTRAN_27 });
            params.add(new Object[] { new UInt32(58), MMModemBand.MM_MODEM_BAND_EUTRAN_28 });
            params.add(new Object[] { new UInt32(59), MMModemBand.MM_MODEM_BAND_EUTRAN_29 });
            params.add(new Object[] { new UInt32(60), MMModemBand.MM_MODEM_BAND_EUTRAN_30 });
            params.add(new Object[] { new UInt32(61), MMModemBand.MM_MODEM_BAND_EUTRAN_31 });
            params.add(new Object[] { new UInt32(62), MMModemBand.MM_MODEM_BAND_EUTRAN_32 });
            params.add(new Object[] { new UInt32(63), MMModemBand.MM_MODEM_BAND_EUTRAN_33 });
            params.add(new Object[] { new UInt32(64), MMModemBand.MM_MODEM_BAND_EUTRAN_34 });
            params.add(new Object[] { new UInt32(65), MMModemBand.MM_MODEM_BAND_EUTRAN_35 });
            params.add(new Object[] { new UInt32(66), MMModemBand.MM_MODEM_BAND_EUTRAN_36 });
            params.add(new Object[] { new UInt32(67), MMModemBand.MM_MODEM_BAND_EUTRAN_37 });
            params.add(new Object[] { new UInt32(68), MMModemBand.MM_MODEM_BAND_EUTRAN_38 });
            params.add(new Object[] { new UInt32(69), MMModemBand.MM_MODEM_BAND_EUTRAN_39 });
            params.add(new Object[] { new UInt32(70), MMModemBand.MM_MODEM_BAND_EUTRAN_40 });
            params.add(new Object[] { new UInt32(71), MMModemBand.MM_MODEM_BAND_EUTRAN_41 });
            params.add(new Object[] { new UInt32(72), MMModemBand.MM_MODEM_BAND_EUTRAN_42 });
            params.add(new Object[] { new UInt32(73), MMModemBand.MM_MODEM_BAND_EUTRAN_43 });
            params.add(new Object[] { new UInt32(74), MMModemBand.MM_MODEM_BAND_EUTRAN_44 });
            params.add(new Object[] { new UInt32(75), MMModemBand.MM_MODEM_BAND_EUTRAN_45 });
            params.add(new Object[] { new UInt32(76), MMModemBand.MM_MODEM_BAND_EUTRAN_46 });
            params.add(new Object[] { new UInt32(77), MMModemBand.MM_MODEM_BAND_EUTRAN_47 });
            params.add(new Object[] { new UInt32(78), MMModemBand.MM_MODEM_BAND_EUTRAN_48 });
            params.add(new Object[] { new UInt32(79), MMModemBand.MM_MODEM_BAND_EUTRAN_49 });
            params.add(new Object[] { new UInt32(80), MMModemBand.MM_MODEM_BAND_EUTRAN_50 });
            params.add(new Object[] { new UInt32(81), MMModemBand.MM_MODEM_BAND_EUTRAN_51 });
            params.add(new Object[] { new UInt32(82), MMModemBand.MM_MODEM_BAND_EUTRAN_52 });
            params.add(new Object[] { new UInt32(83), MMModemBand.MM_MODEM_BAND_EUTRAN_53 });
            params.add(new Object[] { new UInt32(84), MMModemBand.MM_MODEM_BAND_EUTRAN_54 });
            params.add(new Object[] { new UInt32(85), MMModemBand.MM_MODEM_BAND_EUTRAN_55 });
            params.add(new Object[] { new UInt32(86), MMModemBand.MM_MODEM_BAND_EUTRAN_56 });
            params.add(new Object[] { new UInt32(87), MMModemBand.MM_MODEM_BAND_EUTRAN_57 });
            params.add(new Object[] { new UInt32(88), MMModemBand.MM_MODEM_BAND_EUTRAN_58 });
            params.add(new Object[] { new UInt32(89), MMModemBand.MM_MODEM_BAND_EUTRAN_59 });
            params.add(new Object[] { new UInt32(90), MMModemBand.MM_MODEM_BAND_EUTRAN_60 });
            params.add(new Object[] { new UInt32(91), MMModemBand.MM_MODEM_BAND_EUTRAN_61 });
            params.add(new Object[] { new UInt32(92), MMModemBand.MM_MODEM_BAND_EUTRAN_62 });
            params.add(new Object[] { new UInt32(93), MMModemBand.MM_MODEM_BAND_EUTRAN_63 });
            params.add(new Object[] { new UInt32(94), MMModemBand.MM_MODEM_BAND_EUTRAN_64 });
            params.add(new Object[] { new UInt32(95), MMModemBand.MM_MODEM_BAND_EUTRAN_65 });
            params.add(new Object[] { new UInt32(96), MMModemBand.MM_MODEM_BAND_EUTRAN_66 });
            params.add(new Object[] { new UInt32(97), MMModemBand.MM_MODEM_BAND_EUTRAN_67 });
            params.add(new Object[] { new UInt32(98), MMModemBand.MM_MODEM_BAND_EUTRAN_68 });
            params.add(new Object[] { new UInt32(99), MMModemBand.MM_MODEM_BAND_EUTRAN_69 });
            params.add(new Object[] { new UInt32(100), MMModemBand.MM_MODEM_BAND_EUTRAN_70 });
            params.add(new Object[] { new UInt32(101), MMModemBand.MM_MODEM_BAND_EUTRAN_71 });
            params.add(new Object[] { new UInt32(128), MMModemBand.MM_MODEM_BAND_CDMA_BC0 });
            params.add(new Object[] { new UInt32(129), MMModemBand.MM_MODEM_BAND_CDMA_BC1 });
            params.add(new Object[] { new UInt32(130), MMModemBand.MM_MODEM_BAND_CDMA_BC2 });
            params.add(new Object[] { new UInt32(131), MMModemBand.MM_MODEM_BAND_CDMA_BC3 });
            params.add(new Object[] { new UInt32(132), MMModemBand.MM_MODEM_BAND_CDMA_BC4 });
            params.add(new Object[] { new UInt32(134), MMModemBand.MM_MODEM_BAND_CDMA_BC5 });
            params.add(new Object[] { new UInt32(135), MMModemBand.MM_MODEM_BAND_CDMA_BC6 });
            params.add(new Object[] { new UInt32(136), MMModemBand.MM_MODEM_BAND_CDMA_BC7 });
            params.add(new Object[] { new UInt32(137), MMModemBand.MM_MODEM_BAND_CDMA_BC8 });
            params.add(new Object[] { new UInt32(138), MMModemBand.MM_MODEM_BAND_CDMA_BC9 });
            params.add(new Object[] { new UInt32(139), MMModemBand.MM_MODEM_BAND_CDMA_BC10 });
            params.add(new Object[] { new UInt32(140), MMModemBand.MM_MODEM_BAND_CDMA_BC11 });
            params.add(new Object[] { new UInt32(141), MMModemBand.MM_MODEM_BAND_CDMA_BC12 });
            params.add(new Object[] { new UInt32(142), MMModemBand.MM_MODEM_BAND_CDMA_BC13 });
            params.add(new Object[] { new UInt32(143), MMModemBand.MM_MODEM_BAND_CDMA_BC14 });
            params.add(new Object[] { new UInt32(144), MMModemBand.MM_MODEM_BAND_CDMA_BC15 });
            params.add(new Object[] { new UInt32(145), MMModemBand.MM_MODEM_BAND_CDMA_BC16 });
            params.add(new Object[] { new UInt32(146), MMModemBand.MM_MODEM_BAND_CDMA_BC17 });
            params.add(new Object[] { new UInt32(147), MMModemBand.MM_MODEM_BAND_CDMA_BC18 });
            params.add(new Object[] { new UInt32(148), MMModemBand.MM_MODEM_BAND_CDMA_BC19 });
            params.add(new Object[] { new UInt32(210), MMModemBand.MM_MODEM_BAND_UTRAN_10 });
            params.add(new Object[] { new UInt32(211), MMModemBand.MM_MODEM_BAND_UTRAN_11 });
            params.add(new Object[] { new UInt32(212), MMModemBand.MM_MODEM_BAND_UTRAN_12 });
            params.add(new Object[] { new UInt32(213), MMModemBand.MM_MODEM_BAND_UTRAN_13 });
            params.add(new Object[] { new UInt32(214), MMModemBand.MM_MODEM_BAND_UTRAN_14 });
            params.add(new Object[] { new UInt32(219), MMModemBand.MM_MODEM_BAND_UTRAN_19 });
            params.add(new Object[] { new UInt32(220), MMModemBand.MM_MODEM_BAND_UTRAN_20 });
            params.add(new Object[] { new UInt32(221), MMModemBand.MM_MODEM_BAND_UTRAN_21 });
            params.add(new Object[] { new UInt32(222), MMModemBand.MM_MODEM_BAND_UTRAN_22 });
            params.add(new Object[] { new UInt32(225), MMModemBand.MM_MODEM_BAND_UTRAN_25 });
            params.add(new Object[] { new UInt32(226), MMModemBand.MM_MODEM_BAND_UTRAN_26 });
            params.add(new Object[] { new UInt32(232), MMModemBand.MM_MODEM_BAND_UTRAN_32 });
            params.add(new Object[] { new UInt32(256), MMModemBand.MM_MODEM_BAND_ANY });
            return params;
        }

        private final UInt32 inputIntValue;
        private final MMModemBand expectedModemBand;
        private MMModemBand calculatedModemBand;

        public MMModemBandTestToMMModemBandTestTest(UInt32 intValue, MMModemBand modemBand) {
            this.inputIntValue = intValue;
            this.expectedModemBand = modemBand;
        }

        @Test
        public void shouldReturnCorrectModemBand() {
            whenCalculateMMModemBand();
            thenCalculatedMMModemBandIsCorrect();
        }

        private void whenCalculateMMModemBand() {
            this.calculatedModemBand = MMModemBand.toMMModemBand(this.inputIntValue);
        }

        private void thenCalculatedMMModemBandIsCorrect() {
            assertEquals(this.expectedModemBand, this.calculatedModemBand);
        }
    }

    @RunWith(Parameterized.class)
    public static class MMModemBandTestToModemBandTestTest {

        @Parameters
        public static Collection<Object[]> ModemBandParams() {
            List<Object[]> params = new ArrayList<>();
            params.add(new Object[] { new UInt32(0), ModemBand.UNKNOWN });
            params.add(new Object[] { new UInt32(1), ModemBand.EGSM });
            params.add(new Object[] { new UInt32(2), ModemBand.DCS });
            params.add(new Object[] { new UInt32(3), ModemBand.PCS });
            params.add(new Object[] { new UInt32(4), ModemBand.G850 });
            params.add(new Object[] { new UInt32(5), ModemBand.UTRAN_1 });
            params.add(new Object[] { new UInt32(6), ModemBand.UTRAN_3 });
            params.add(new Object[] { new UInt32(7), ModemBand.UTRAN_4 });
            params.add(new Object[] { new UInt32(8), ModemBand.UTRAN_6 });
            params.add(new Object[] { new UInt32(9), ModemBand.UTRAN_5 });
            params.add(new Object[] { new UInt32(10), ModemBand.UTRAN_8 });
            params.add(new Object[] { new UInt32(11), ModemBand.UTRAN_9 });
            params.add(new Object[] { new UInt32(12), ModemBand.UTRAN_2 });
            params.add(new Object[] { new UInt32(13), ModemBand.UTRAN_7 });
            params.add(new Object[] { new UInt32(14), ModemBand.G450 });
            params.add(new Object[] { new UInt32(15), ModemBand.G480 });
            params.add(new Object[] { new UInt32(16), ModemBand.G750 });
            params.add(new Object[] { new UInt32(17), ModemBand.G380 });
            params.add(new Object[] { new UInt32(18), ModemBand.G410 });
            params.add(new Object[] { new UInt32(19), ModemBand.G710 });
            params.add(new Object[] { new UInt32(20), ModemBand.G810 });
            params.add(new Object[] { new UInt32(31), ModemBand.EUTRAN_1 });
            params.add(new Object[] { new UInt32(32), ModemBand.EUTRAN_2 });
            params.add(new Object[] { new UInt32(33), ModemBand.EUTRAN_3 });
            params.add(new Object[] { new UInt32(34), ModemBand.EUTRAN_4 });
            params.add(new Object[] { new UInt32(35), ModemBand.EUTRAN_5 });
            params.add(new Object[] { new UInt32(36), ModemBand.EUTRAN_6 });
            params.add(new Object[] { new UInt32(37), ModemBand.EUTRAN_7 });
            params.add(new Object[] { new UInt32(38), ModemBand.EUTRAN_8 });
            params.add(new Object[] { new UInt32(39), ModemBand.EUTRAN_9 });
            params.add(new Object[] { new UInt32(40), ModemBand.EUTRAN_10 });
            params.add(new Object[] { new UInt32(41), ModemBand.EUTRAN_11 });
            params.add(new Object[] { new UInt32(42), ModemBand.EUTRAN_12 });
            params.add(new Object[] { new UInt32(43), ModemBand.EUTRAN_13 });
            params.add(new Object[] { new UInt32(44), ModemBand.EUTRAN_14 });
            params.add(new Object[] { new UInt32(47), ModemBand.EUTRAN_17 });
            params.add(new Object[] { new UInt32(48), ModemBand.EUTRAN_18 });
            params.add(new Object[] { new UInt32(49), ModemBand.EUTRAN_19 });
            params.add(new Object[] { new UInt32(50), ModemBand.EUTRAN_20 });
            params.add(new Object[] { new UInt32(51), ModemBand.EUTRAN_21 });
            params.add(new Object[] { new UInt32(52), ModemBand.EUTRAN_22 });
            params.add(new Object[] { new UInt32(53), ModemBand.EUTRAN_23 });
            params.add(new Object[] { new UInt32(54), ModemBand.EUTRAN_24 });
            params.add(new Object[] { new UInt32(55), ModemBand.EUTRAN_25 });
            params.add(new Object[] { new UInt32(56), ModemBand.EUTRAN_26 });
            params.add(new Object[] { new UInt32(57), ModemBand.EUTRAN_27 });
            params.add(new Object[] { new UInt32(58), ModemBand.EUTRAN_28 });
            params.add(new Object[] { new UInt32(59), ModemBand.EUTRAN_29 });
            params.add(new Object[] { new UInt32(60), ModemBand.EUTRAN_30 });
            params.add(new Object[] { new UInt32(61), ModemBand.EUTRAN_31 });
            params.add(new Object[] { new UInt32(62), ModemBand.EUTRAN_32 });
            params.add(new Object[] { new UInt32(63), ModemBand.EUTRAN_33 });
            params.add(new Object[] { new UInt32(64), ModemBand.EUTRAN_34 });
            params.add(new Object[] { new UInt32(65), ModemBand.EUTRAN_35 });
            params.add(new Object[] { new UInt32(66), ModemBand.EUTRAN_36 });
            params.add(new Object[] { new UInt32(67), ModemBand.EUTRAN_37 });
            params.add(new Object[] { new UInt32(68), ModemBand.EUTRAN_38 });
            params.add(new Object[] { new UInt32(69), ModemBand.EUTRAN_39 });
            params.add(new Object[] { new UInt32(70), ModemBand.EUTRAN_40 });
            params.add(new Object[] { new UInt32(71), ModemBand.EUTRAN_41 });
            params.add(new Object[] { new UInt32(72), ModemBand.EUTRAN_42 });
            params.add(new Object[] { new UInt32(73), ModemBand.EUTRAN_43 });
            params.add(new Object[] { new UInt32(74), ModemBand.EUTRAN_44 });
            params.add(new Object[] { new UInt32(75), ModemBand.EUTRAN_45 });
            params.add(new Object[] { new UInt32(76), ModemBand.EUTRAN_46 });
            params.add(new Object[] { new UInt32(77), ModemBand.EUTRAN_47 });
            params.add(new Object[] { new UInt32(78), ModemBand.EUTRAN_48 });
            params.add(new Object[] { new UInt32(79), ModemBand.EUTRAN_49 });
            params.add(new Object[] { new UInt32(80), ModemBand.EUTRAN_50 });
            params.add(new Object[] { new UInt32(81), ModemBand.EUTRAN_51 });
            params.add(new Object[] { new UInt32(82), ModemBand.EUTRAN_52 });
            params.add(new Object[] { new UInt32(83), ModemBand.EUTRAN_53 });
            params.add(new Object[] { new UInt32(84), ModemBand.EUTRAN_54 });
            params.add(new Object[] { new UInt32(85), ModemBand.EUTRAN_55 });
            params.add(new Object[] { new UInt32(86), ModemBand.EUTRAN_56 });
            params.add(new Object[] { new UInt32(87), ModemBand.EUTRAN_57 });
            params.add(new Object[] { new UInt32(88), ModemBand.EUTRAN_58 });
            params.add(new Object[] { new UInt32(89), ModemBand.EUTRAN_59 });
            params.add(new Object[] { new UInt32(90), ModemBand.EUTRAN_60 });
            params.add(new Object[] { new UInt32(91), ModemBand.EUTRAN_61 });
            params.add(new Object[] { new UInt32(92), ModemBand.EUTRAN_62 });
            params.add(new Object[] { new UInt32(93), ModemBand.EUTRAN_63 });
            params.add(new Object[] { new UInt32(94), ModemBand.EUTRAN_64 });
            params.add(new Object[] { new UInt32(95), ModemBand.EUTRAN_65 });
            params.add(new Object[] { new UInt32(96), ModemBand.EUTRAN_66 });
            params.add(new Object[] { new UInt32(97), ModemBand.EUTRAN_67 });
            params.add(new Object[] { new UInt32(98), ModemBand.EUTRAN_68 });
            params.add(new Object[] { new UInt32(99), ModemBand.EUTRAN_69 });
            params.add(new Object[] { new UInt32(100), ModemBand.EUTRAN_70 });
            params.add(new Object[] { new UInt32(101), ModemBand.EUTRAN_71 });
            params.add(new Object[] { new UInt32(128), ModemBand.CDMA_BC0 });
            params.add(new Object[] { new UInt32(129), ModemBand.CDMA_BC1 });
            params.add(new Object[] { new UInt32(130), ModemBand.CDMA_BC2 });
            params.add(new Object[] { new UInt32(131), ModemBand.CDMA_BC3 });
            params.add(new Object[] { new UInt32(132), ModemBand.CDMA_BC4 });
            params.add(new Object[] { new UInt32(134), ModemBand.CDMA_BC5 });
            params.add(new Object[] { new UInt32(135), ModemBand.CDMA_BC6 });
            params.add(new Object[] { new UInt32(136), ModemBand.CDMA_BC7 });
            params.add(new Object[] { new UInt32(137), ModemBand.CDMA_BC8 });
            params.add(new Object[] { new UInt32(138), ModemBand.CDMA_BC9 });
            params.add(new Object[] { new UInt32(139), ModemBand.CDMA_BC10 });
            params.add(new Object[] { new UInt32(140), ModemBand.CDMA_BC11 });
            params.add(new Object[] { new UInt32(141), ModemBand.CDMA_BC12 });
            params.add(new Object[] { new UInt32(142), ModemBand.CDMA_BC13 });
            params.add(new Object[] { new UInt32(143), ModemBand.CDMA_BC14 });
            params.add(new Object[] { new UInt32(144), ModemBand.CDMA_BC15 });
            params.add(new Object[] { new UInt32(145), ModemBand.CDMA_BC16 });
            params.add(new Object[] { new UInt32(146), ModemBand.CDMA_BC17 });
            params.add(new Object[] { new UInt32(147), ModemBand.CDMA_BC18 });
            params.add(new Object[] { new UInt32(148), ModemBand.CDMA_BC19 });
            params.add(new Object[] { new UInt32(210), ModemBand.UTRAN_10 });
            params.add(new Object[] { new UInt32(211), ModemBand.UTRAN_11 });
            params.add(new Object[] { new UInt32(212), ModemBand.UTRAN_12 });
            params.add(new Object[] { new UInt32(213), ModemBand.UTRAN_13 });
            params.add(new Object[] { new UInt32(214), ModemBand.UTRAN_14 });
            params.add(new Object[] { new UInt32(219), ModemBand.UTRAN_19 });
            params.add(new Object[] { new UInt32(220), ModemBand.UTRAN_20 });
            params.add(new Object[] { new UInt32(221), ModemBand.UTRAN_21 });
            params.add(new Object[] { new UInt32(222), ModemBand.UTRAN_22 });
            params.add(new Object[] { new UInt32(225), ModemBand.UTRAN_25 });
            params.add(new Object[] { new UInt32(226), ModemBand.UTRAN_26 });
            params.add(new Object[] { new UInt32(232), ModemBand.UTRAN_32 });
            params.add(new Object[] { new UInt32(256), ModemBand.ANY });
            return params;
        }

        private final UInt32 inputIntValue;
        private final ModemBand expectedModemBand;
        private ModemBand calculatedModemBand;

        public MMModemBandTestToModemBandTestTest(UInt32 intValue, ModemBand modemBand) {
            this.inputIntValue = intValue;
            this.expectedModemBand = modemBand;
        }

        @Test
        public void shouldReturnCorrect() {
            whenCalculatedModemBand();
            thenCalculatedModemBandIsCorrect();
        }

        private void whenCalculatedModemBand() {
            this.calculatedModemBand = MMModemBand.toModemBands(this.inputIntValue);
        }

        private void thenCalculatedModemBandIsCorrect() {
            assertEquals(this.expectedModemBand, this.calculatedModemBand);
        }
    }

    @RunWith(Parameterized.class)
    public static class MMModemBandToUInt32Test {

        @Parameters
        public static Collection<Object[]> BearerIpFamilyParams() {
            List<Object[]> params = new ArrayList<>();
            params.add(new Object[] { new UInt32(0), MMModemBand.MM_MODEM_BAND_UNKNOWN });
            params.add(new Object[] { new UInt32(1), MMModemBand.MM_MODEM_BAND_EGSM });
            params.add(new Object[] { new UInt32(2), MMModemBand.MM_MODEM_BAND_DCS });
            params.add(new Object[] { new UInt32(3), MMModemBand.MM_MODEM_BAND_PCS });
            params.add(new Object[] { new UInt32(4), MMModemBand.MM_MODEM_BAND_G850 });
            params.add(new Object[] { new UInt32(5), MMModemBand.MM_MODEM_BAND_UTRAN_1 });
            params.add(new Object[] { new UInt32(6), MMModemBand.MM_MODEM_BAND_UTRAN_3 });
            params.add(new Object[] { new UInt32(7), MMModemBand.MM_MODEM_BAND_UTRAN_4 });
            params.add(new Object[] { new UInt32(8), MMModemBand.MM_MODEM_BAND_UTRAN_6 });
            params.add(new Object[] { new UInt32(9), MMModemBand.MM_MODEM_BAND_UTRAN_5 });
            params.add(new Object[] { new UInt32(10), MMModemBand.MM_MODEM_BAND_UTRAN_8 });
            params.add(new Object[] { new UInt32(11), MMModemBand.MM_MODEM_BAND_UTRAN_9 });
            params.add(new Object[] { new UInt32(12), MMModemBand.MM_MODEM_BAND_UTRAN_2 });
            params.add(new Object[] { new UInt32(13), MMModemBand.MM_MODEM_BAND_UTRAN_7 });
            params.add(new Object[] { new UInt32(14), MMModemBand.MM_MODEM_BAND_G450 });
            params.add(new Object[] { new UInt32(15), MMModemBand.MM_MODEM_BAND_G480 });
            params.add(new Object[] { new UInt32(16), MMModemBand.MM_MODEM_BAND_G750 });
            params.add(new Object[] { new UInt32(17), MMModemBand.MM_MODEM_BAND_G380 });
            params.add(new Object[] { new UInt32(18), MMModemBand.MM_MODEM_BAND_G410 });
            params.add(new Object[] { new UInt32(19), MMModemBand.MM_MODEM_BAND_G710 });
            params.add(new Object[] { new UInt32(20), MMModemBand.MM_MODEM_BAND_G810 });
            params.add(new Object[] { new UInt32(31), MMModemBand.MM_MODEM_BAND_EUTRAN_1 });
            params.add(new Object[] { new UInt32(32), MMModemBand.MM_MODEM_BAND_EUTRAN_2 });
            params.add(new Object[] { new UInt32(33), MMModemBand.MM_MODEM_BAND_EUTRAN_3 });
            params.add(new Object[] { new UInt32(34), MMModemBand.MM_MODEM_BAND_EUTRAN_4 });
            params.add(new Object[] { new UInt32(35), MMModemBand.MM_MODEM_BAND_EUTRAN_5 });
            params.add(new Object[] { new UInt32(36), MMModemBand.MM_MODEM_BAND_EUTRAN_6 });
            params.add(new Object[] { new UInt32(37), MMModemBand.MM_MODEM_BAND_EUTRAN_7 });
            params.add(new Object[] { new UInt32(38), MMModemBand.MM_MODEM_BAND_EUTRAN_8 });
            params.add(new Object[] { new UInt32(39), MMModemBand.MM_MODEM_BAND_EUTRAN_9 });
            params.add(new Object[] { new UInt32(40), MMModemBand.MM_MODEM_BAND_EUTRAN_10 });
            params.add(new Object[] { new UInt32(41), MMModemBand.MM_MODEM_BAND_EUTRAN_11 });
            params.add(new Object[] { new UInt32(42), MMModemBand.MM_MODEM_BAND_EUTRAN_12 });
            params.add(new Object[] { new UInt32(43), MMModemBand.MM_MODEM_BAND_EUTRAN_13 });
            params.add(new Object[] { new UInt32(44), MMModemBand.MM_MODEM_BAND_EUTRAN_14 });
            params.add(new Object[] { new UInt32(47), MMModemBand.MM_MODEM_BAND_EUTRAN_17 });
            params.add(new Object[] { new UInt32(48), MMModemBand.MM_MODEM_BAND_EUTRAN_18 });
            params.add(new Object[] { new UInt32(49), MMModemBand.MM_MODEM_BAND_EUTRAN_19 });
            params.add(new Object[] { new UInt32(50), MMModemBand.MM_MODEM_BAND_EUTRAN_20 });
            params.add(new Object[] { new UInt32(51), MMModemBand.MM_MODEM_BAND_EUTRAN_21 });
            params.add(new Object[] { new UInt32(52), MMModemBand.MM_MODEM_BAND_EUTRAN_22 });
            params.add(new Object[] { new UInt32(53), MMModemBand.MM_MODEM_BAND_EUTRAN_23 });
            params.add(new Object[] { new UInt32(54), MMModemBand.MM_MODEM_BAND_EUTRAN_24 });
            params.add(new Object[] { new UInt32(55), MMModemBand.MM_MODEM_BAND_EUTRAN_25 });
            params.add(new Object[] { new UInt32(56), MMModemBand.MM_MODEM_BAND_EUTRAN_26 });
            params.add(new Object[] { new UInt32(57), MMModemBand.MM_MODEM_BAND_EUTRAN_27 });
            params.add(new Object[] { new UInt32(58), MMModemBand.MM_MODEM_BAND_EUTRAN_28 });
            params.add(new Object[] { new UInt32(59), MMModemBand.MM_MODEM_BAND_EUTRAN_29 });
            params.add(new Object[] { new UInt32(60), MMModemBand.MM_MODEM_BAND_EUTRAN_30 });
            params.add(new Object[] { new UInt32(61), MMModemBand.MM_MODEM_BAND_EUTRAN_31 });
            params.add(new Object[] { new UInt32(62), MMModemBand.MM_MODEM_BAND_EUTRAN_32 });
            params.add(new Object[] { new UInt32(63), MMModemBand.MM_MODEM_BAND_EUTRAN_33 });
            params.add(new Object[] { new UInt32(64), MMModemBand.MM_MODEM_BAND_EUTRAN_34 });
            params.add(new Object[] { new UInt32(65), MMModemBand.MM_MODEM_BAND_EUTRAN_35 });
            params.add(new Object[] { new UInt32(66), MMModemBand.MM_MODEM_BAND_EUTRAN_36 });
            params.add(new Object[] { new UInt32(67), MMModemBand.MM_MODEM_BAND_EUTRAN_37 });
            params.add(new Object[] { new UInt32(68), MMModemBand.MM_MODEM_BAND_EUTRAN_38 });
            params.add(new Object[] { new UInt32(69), MMModemBand.MM_MODEM_BAND_EUTRAN_39 });
            params.add(new Object[] { new UInt32(70), MMModemBand.MM_MODEM_BAND_EUTRAN_40 });
            params.add(new Object[] { new UInt32(71), MMModemBand.MM_MODEM_BAND_EUTRAN_41 });
            params.add(new Object[] { new UInt32(72), MMModemBand.MM_MODEM_BAND_EUTRAN_42 });
            params.add(new Object[] { new UInt32(73), MMModemBand.MM_MODEM_BAND_EUTRAN_43 });
            params.add(new Object[] { new UInt32(74), MMModemBand.MM_MODEM_BAND_EUTRAN_44 });
            params.add(new Object[] { new UInt32(75), MMModemBand.MM_MODEM_BAND_EUTRAN_45 });
            params.add(new Object[] { new UInt32(76), MMModemBand.MM_MODEM_BAND_EUTRAN_46 });
            params.add(new Object[] { new UInt32(77), MMModemBand.MM_MODEM_BAND_EUTRAN_47 });
            params.add(new Object[] { new UInt32(78), MMModemBand.MM_MODEM_BAND_EUTRAN_48 });
            params.add(new Object[] { new UInt32(79), MMModemBand.MM_MODEM_BAND_EUTRAN_49 });
            params.add(new Object[] { new UInt32(80), MMModemBand.MM_MODEM_BAND_EUTRAN_50 });
            params.add(new Object[] { new UInt32(81), MMModemBand.MM_MODEM_BAND_EUTRAN_51 });
            params.add(new Object[] { new UInt32(82), MMModemBand.MM_MODEM_BAND_EUTRAN_52 });
            params.add(new Object[] { new UInt32(83), MMModemBand.MM_MODEM_BAND_EUTRAN_53 });
            params.add(new Object[] { new UInt32(84), MMModemBand.MM_MODEM_BAND_EUTRAN_54 });
            params.add(new Object[] { new UInt32(85), MMModemBand.MM_MODEM_BAND_EUTRAN_55 });
            params.add(new Object[] { new UInt32(86), MMModemBand.MM_MODEM_BAND_EUTRAN_56 });
            params.add(new Object[] { new UInt32(87), MMModemBand.MM_MODEM_BAND_EUTRAN_57 });
            params.add(new Object[] { new UInt32(88), MMModemBand.MM_MODEM_BAND_EUTRAN_58 });
            params.add(new Object[] { new UInt32(89), MMModemBand.MM_MODEM_BAND_EUTRAN_59 });
            params.add(new Object[] { new UInt32(90), MMModemBand.MM_MODEM_BAND_EUTRAN_60 });
            params.add(new Object[] { new UInt32(91), MMModemBand.MM_MODEM_BAND_EUTRAN_61 });
            params.add(new Object[] { new UInt32(92), MMModemBand.MM_MODEM_BAND_EUTRAN_62 });
            params.add(new Object[] { new UInt32(93), MMModemBand.MM_MODEM_BAND_EUTRAN_63 });
            params.add(new Object[] { new UInt32(94), MMModemBand.MM_MODEM_BAND_EUTRAN_64 });
            params.add(new Object[] { new UInt32(95), MMModemBand.MM_MODEM_BAND_EUTRAN_65 });
            params.add(new Object[] { new UInt32(96), MMModemBand.MM_MODEM_BAND_EUTRAN_66 });
            params.add(new Object[] { new UInt32(97), MMModemBand.MM_MODEM_BAND_EUTRAN_67 });
            params.add(new Object[] { new UInt32(98), MMModemBand.MM_MODEM_BAND_EUTRAN_68 });
            params.add(new Object[] { new UInt32(99), MMModemBand.MM_MODEM_BAND_EUTRAN_69 });
            params.add(new Object[] { new UInt32(100), MMModemBand.MM_MODEM_BAND_EUTRAN_70 });
            params.add(new Object[] { new UInt32(101), MMModemBand.MM_MODEM_BAND_EUTRAN_71 });
            params.add(new Object[] { new UInt32(128), MMModemBand.MM_MODEM_BAND_CDMA_BC0 });
            params.add(new Object[] { new UInt32(129), MMModemBand.MM_MODEM_BAND_CDMA_BC1 });
            params.add(new Object[] { new UInt32(130), MMModemBand.MM_MODEM_BAND_CDMA_BC2 });
            params.add(new Object[] { new UInt32(131), MMModemBand.MM_MODEM_BAND_CDMA_BC3 });
            params.add(new Object[] { new UInt32(132), MMModemBand.MM_MODEM_BAND_CDMA_BC4 });
            params.add(new Object[] { new UInt32(134), MMModemBand.MM_MODEM_BAND_CDMA_BC5 });
            params.add(new Object[] { new UInt32(135), MMModemBand.MM_MODEM_BAND_CDMA_BC6 });
            params.add(new Object[] { new UInt32(136), MMModemBand.MM_MODEM_BAND_CDMA_BC7 });
            params.add(new Object[] { new UInt32(137), MMModemBand.MM_MODEM_BAND_CDMA_BC8 });
            params.add(new Object[] { new UInt32(138), MMModemBand.MM_MODEM_BAND_CDMA_BC9 });
            params.add(new Object[] { new UInt32(139), MMModemBand.MM_MODEM_BAND_CDMA_BC10 });
            params.add(new Object[] { new UInt32(140), MMModemBand.MM_MODEM_BAND_CDMA_BC11 });
            params.add(new Object[] { new UInt32(141), MMModemBand.MM_MODEM_BAND_CDMA_BC12 });
            params.add(new Object[] { new UInt32(142), MMModemBand.MM_MODEM_BAND_CDMA_BC13 });
            params.add(new Object[] { new UInt32(143), MMModemBand.MM_MODEM_BAND_CDMA_BC14 });
            params.add(new Object[] { new UInt32(144), MMModemBand.MM_MODEM_BAND_CDMA_BC15 });
            params.add(new Object[] { new UInt32(145), MMModemBand.MM_MODEM_BAND_CDMA_BC16 });
            params.add(new Object[] { new UInt32(146), MMModemBand.MM_MODEM_BAND_CDMA_BC17 });
            params.add(new Object[] { new UInt32(147), MMModemBand.MM_MODEM_BAND_CDMA_BC18 });
            params.add(new Object[] { new UInt32(148), MMModemBand.MM_MODEM_BAND_CDMA_BC19 });
            params.add(new Object[] { new UInt32(210), MMModemBand.MM_MODEM_BAND_UTRAN_10 });
            params.add(new Object[] { new UInt32(211), MMModemBand.MM_MODEM_BAND_UTRAN_11 });
            params.add(new Object[] { new UInt32(212), MMModemBand.MM_MODEM_BAND_UTRAN_12 });
            params.add(new Object[] { new UInt32(213), MMModemBand.MM_MODEM_BAND_UTRAN_13 });
            params.add(new Object[] { new UInt32(214), MMModemBand.MM_MODEM_BAND_UTRAN_14 });
            params.add(new Object[] { new UInt32(219), MMModemBand.MM_MODEM_BAND_UTRAN_19 });
            params.add(new Object[] { new UInt32(220), MMModemBand.MM_MODEM_BAND_UTRAN_20 });
            params.add(new Object[] { new UInt32(221), MMModemBand.MM_MODEM_BAND_UTRAN_21 });
            params.add(new Object[] { new UInt32(222), MMModemBand.MM_MODEM_BAND_UTRAN_22 });
            params.add(new Object[] { new UInt32(225), MMModemBand.MM_MODEM_BAND_UTRAN_25 });
            params.add(new Object[] { new UInt32(226), MMModemBand.MM_MODEM_BAND_UTRAN_26 });
            params.add(new Object[] { new UInt32(232), MMModemBand.MM_MODEM_BAND_UTRAN_32 });
            params.add(new Object[] { new UInt32(256), MMModemBand.MM_MODEM_BAND_ANY });
            return params;
        }

        private final MMModemBand inputModemBand;
        private final UInt32 expectedIntValue;
        private UInt32 calculatedUInt32;

        public MMModemBandToUInt32Test(UInt32 intValue, MMModemBand ipFamily) {
            this.expectedIntValue = intValue;
            this.inputModemBand = ipFamily;
        }

        @Test
        public void shouldReturnCorrectUInt32() {
            whenCalculatedUInt32();
            thenCalculatedUInt32IsCorrect();
        }

        private void whenCalculatedUInt32() {
            this.calculatedUInt32 = this.inputModemBand.toUInt32();
        }

        private void thenCalculatedUInt32IsCorrect() {
            assertEquals(this.expectedIntValue, this.calculatedUInt32);
        }
    }
}

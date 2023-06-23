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

import org.eclipse.kura.net.status.modem.ModemBand;
import org.freedesktop.dbus.types.UInt32;

public enum MMModemBand {

    MM_MODEM_BAND_UNKNOWN(0),
    MM_MODEM_BAND_EGSM(1),
    MM_MODEM_BAND_DCS(2),
    MM_MODEM_BAND_PCS(3),
    MM_MODEM_BAND_G850(4),
    MM_MODEM_BAND_UTRAN_1(5),
    MM_MODEM_BAND_UTRAN_3(6),
    MM_MODEM_BAND_UTRAN_4(7),
    MM_MODEM_BAND_UTRAN_6(8),
    MM_MODEM_BAND_UTRAN_5(9),
    MM_MODEM_BAND_UTRAN_8(10),
    MM_MODEM_BAND_UTRAN_9(11),
    MM_MODEM_BAND_UTRAN_2(12),
    MM_MODEM_BAND_UTRAN_7(13),
    MM_MODEM_BAND_G450(14),
    MM_MODEM_BAND_G480(15),
    MM_MODEM_BAND_G750(16),
    MM_MODEM_BAND_G380(17),
    MM_MODEM_BAND_G410(18),
    MM_MODEM_BAND_G710(19),
    MM_MODEM_BAND_G810(20),
    MM_MODEM_BAND_EUTRAN_1(31),
    MM_MODEM_BAND_EUTRAN_2(32),
    MM_MODEM_BAND_EUTRAN_3(33),
    MM_MODEM_BAND_EUTRAN_4(34),
    MM_MODEM_BAND_EUTRAN_5(35),
    MM_MODEM_BAND_EUTRAN_6(36),
    MM_MODEM_BAND_EUTRAN_7(37),
    MM_MODEM_BAND_EUTRAN_8(38),
    MM_MODEM_BAND_EUTRAN_9(39),
    MM_MODEM_BAND_EUTRAN_10(40),
    MM_MODEM_BAND_EUTRAN_11(41),
    MM_MODEM_BAND_EUTRAN_12(42),
    MM_MODEM_BAND_EUTRAN_13(43),
    MM_MODEM_BAND_EUTRAN_14(44),
    MM_MODEM_BAND_EUTRAN_17(47),
    MM_MODEM_BAND_EUTRAN_18(48),
    MM_MODEM_BAND_EUTRAN_19(49),
    MM_MODEM_BAND_EUTRAN_20(50),
    MM_MODEM_BAND_EUTRAN_21(51),
    MM_MODEM_BAND_EUTRAN_22(52),
    MM_MODEM_BAND_EUTRAN_23(53),
    MM_MODEM_BAND_EUTRAN_24(54),
    MM_MODEM_BAND_EUTRAN_25(55),
    MM_MODEM_BAND_EUTRAN_26(56),
    MM_MODEM_BAND_EUTRAN_27(57),
    MM_MODEM_BAND_EUTRAN_28(58),
    MM_MODEM_BAND_EUTRAN_29(59),
    MM_MODEM_BAND_EUTRAN_30(60),
    MM_MODEM_BAND_EUTRAN_31(61),
    MM_MODEM_BAND_EUTRAN_32(62),
    MM_MODEM_BAND_EUTRAN_33(63),
    MM_MODEM_BAND_EUTRAN_34(64),
    MM_MODEM_BAND_EUTRAN_35(65),
    MM_MODEM_BAND_EUTRAN_36(66),
    MM_MODEM_BAND_EUTRAN_37(67),
    MM_MODEM_BAND_EUTRAN_38(68),
    MM_MODEM_BAND_EUTRAN_39(69),
    MM_MODEM_BAND_EUTRAN_40(70),
    MM_MODEM_BAND_EUTRAN_41(71),
    MM_MODEM_BAND_EUTRAN_42(72),
    MM_MODEM_BAND_EUTRAN_43(73),
    MM_MODEM_BAND_EUTRAN_44(74),
    MM_MODEM_BAND_EUTRAN_45(75),
    MM_MODEM_BAND_EUTRAN_46(76),
    MM_MODEM_BAND_EUTRAN_47(77),
    MM_MODEM_BAND_EUTRAN_48(78),
    MM_MODEM_BAND_EUTRAN_49(79),
    MM_MODEM_BAND_EUTRAN_50(80),
    MM_MODEM_BAND_EUTRAN_51(81),
    MM_MODEM_BAND_EUTRAN_52(82),
    MM_MODEM_BAND_EUTRAN_53(83),
    MM_MODEM_BAND_EUTRAN_54(84),
    MM_MODEM_BAND_EUTRAN_55(85),
    MM_MODEM_BAND_EUTRAN_56(86),
    MM_MODEM_BAND_EUTRAN_57(87),
    MM_MODEM_BAND_EUTRAN_58(88),
    MM_MODEM_BAND_EUTRAN_59(89),
    MM_MODEM_BAND_EUTRAN_60(90),
    MM_MODEM_BAND_EUTRAN_61(91),
    MM_MODEM_BAND_EUTRAN_62(92),
    MM_MODEM_BAND_EUTRAN_63(93),
    MM_MODEM_BAND_EUTRAN_64(94),
    MM_MODEM_BAND_EUTRAN_65(95),
    MM_MODEM_BAND_EUTRAN_66(96),
    MM_MODEM_BAND_EUTRAN_67(97),
    MM_MODEM_BAND_EUTRAN_68(98),
    MM_MODEM_BAND_EUTRAN_69(99),
    MM_MODEM_BAND_EUTRAN_70(100),
    MM_MODEM_BAND_EUTRAN_71(101),
    MM_MODEM_BAND_CDMA_BC0(128),
    MM_MODEM_BAND_CDMA_BC1(129),
    MM_MODEM_BAND_CDMA_BC2(130),
    MM_MODEM_BAND_CDMA_BC3(131),
    MM_MODEM_BAND_CDMA_BC4(132),
    MM_MODEM_BAND_CDMA_BC5(134),
    MM_MODEM_BAND_CDMA_BC6(135),
    MM_MODEM_BAND_CDMA_BC7(136),
    MM_MODEM_BAND_CDMA_BC8(137),
    MM_MODEM_BAND_CDMA_BC9(138),
    MM_MODEM_BAND_CDMA_BC10(139),
    MM_MODEM_BAND_CDMA_BC11(140),
    MM_MODEM_BAND_CDMA_BC12(141),
    MM_MODEM_BAND_CDMA_BC13(142),
    MM_MODEM_BAND_CDMA_BC14(143),
    MM_MODEM_BAND_CDMA_BC15(144),
    MM_MODEM_BAND_CDMA_BC16(145),
    MM_MODEM_BAND_CDMA_BC17(146),
    MM_MODEM_BAND_CDMA_BC18(147),
    MM_MODEM_BAND_CDMA_BC19(148),
    MM_MODEM_BAND_UTRAN_10(210),
    MM_MODEM_BAND_UTRAN_11(211),
    MM_MODEM_BAND_UTRAN_12(212),
    MM_MODEM_BAND_UTRAN_13(213),
    MM_MODEM_BAND_UTRAN_14(214),
    MM_MODEM_BAND_UTRAN_19(219),
    MM_MODEM_BAND_UTRAN_20(220),
    MM_MODEM_BAND_UTRAN_21(221),
    MM_MODEM_BAND_UTRAN_22(222),
    MM_MODEM_BAND_UTRAN_25(225),
    MM_MODEM_BAND_UTRAN_26(226),
    MM_MODEM_BAND_UTRAN_32(232),
    MM_MODEM_BAND_ANY(256);

    private int value;

    private MMModemBand(int value) {
        this.value = value;
    }

    public int getValue() {
        return this.value;
    }

    public UInt32 toUInt32() {
        return new UInt32(this.value);
    }

    public static MMModemBand toMMModemBand(UInt32 band) {
        int intBand = band.intValue();
        if (intBand > 0 && intBand <= 20) {
            return getMMGsmBand(intBand);
        } else if (intBand >= 31 && intBand <= 101) {
            return getMMLteBand(intBand);
        } else if (intBand >= 128 && intBand <= 148) {
            return getMMCdmaBand(intBand);
        } else if (intBand >= 210 && intBand <= 232) {
            return getMMUmtsBand(intBand);
        } else if (intBand == 256) {
            return MMModemBand.MM_MODEM_BAND_ANY;
        } else {
            return MMModemBand.MM_MODEM_BAND_UNKNOWN;
        }
    }

    private static MMModemBand getMMGsmBand(int band) {
        switch (band) {
        case 1:
            return MMModemBand.MM_MODEM_BAND_EGSM;
        case 2:
            return MMModemBand.MM_MODEM_BAND_DCS;
        case 3:
            return MMModemBand.MM_MODEM_BAND_PCS;
        case 4:
            return MMModemBand.MM_MODEM_BAND_G850;
        case 5:
            return MMModemBand.MM_MODEM_BAND_UTRAN_1;
        case 6:
            return MMModemBand.MM_MODEM_BAND_UTRAN_3;
        case 7:
            return MMModemBand.MM_MODEM_BAND_UTRAN_4;
        case 8:
            return MMModemBand.MM_MODEM_BAND_UTRAN_6;
        case 9:
            return MMModemBand.MM_MODEM_BAND_UTRAN_5;
        case 10:
            return MMModemBand.MM_MODEM_BAND_UTRAN_8;
        case 11:
            return MMModemBand.MM_MODEM_BAND_UTRAN_9;
        case 12:
            return MMModemBand.MM_MODEM_BAND_UTRAN_2;
        case 13:
            return MMModemBand.MM_MODEM_BAND_UTRAN_7;
        case 14:
            return MMModemBand.MM_MODEM_BAND_G450;
        case 15:
            return MMModemBand.MM_MODEM_BAND_G480;
        case 16:
            return MMModemBand.MM_MODEM_BAND_G750;
        case 17:
            return MMModemBand.MM_MODEM_BAND_G380;
        case 18:
            return MMModemBand.MM_MODEM_BAND_G410;
        case 19:
            return MMModemBand.MM_MODEM_BAND_G710;
        case 20:
            return MMModemBand.MM_MODEM_BAND_G810;
        default:
            return MMModemBand.MM_MODEM_BAND_UNKNOWN;
        }
    }

    private static MMModemBand getMMLteBand(int band) {
        switch (band) {
        case 31:
            return MMModemBand.MM_MODEM_BAND_EUTRAN_1;
        case 32:
            return MMModemBand.MM_MODEM_BAND_EUTRAN_2;
        case 33:
            return MMModemBand.MM_MODEM_BAND_EUTRAN_3;
        case 34:
            return MMModemBand.MM_MODEM_BAND_EUTRAN_4;
        case 35:
            return MMModemBand.MM_MODEM_BAND_EUTRAN_5;
        case 36:
            return MMModemBand.MM_MODEM_BAND_EUTRAN_6;
        case 37:
            return MMModemBand.MM_MODEM_BAND_EUTRAN_7;
        case 38:
            return MMModemBand.MM_MODEM_BAND_EUTRAN_8;
        case 39:
            return MMModemBand.MM_MODEM_BAND_EUTRAN_9;
        case 40:
            return MMModemBand.MM_MODEM_BAND_EUTRAN_10;
        case 41:
            return MMModemBand.MM_MODEM_BAND_EUTRAN_11;
        case 42:
            return MMModemBand.MM_MODEM_BAND_EUTRAN_12;
        case 43:
            return MMModemBand.MM_MODEM_BAND_EUTRAN_13;
        case 44:
            return MMModemBand.MM_MODEM_BAND_EUTRAN_14;
        case 47:
            return MMModemBand.MM_MODEM_BAND_EUTRAN_17;
        case 48:
            return MMModemBand.MM_MODEM_BAND_EUTRAN_18;
        case 49:
            return MMModemBand.MM_MODEM_BAND_EUTRAN_19;
        case 50:
            return MMModemBand.MM_MODEM_BAND_EUTRAN_20;
        case 51:
            return MMModemBand.MM_MODEM_BAND_EUTRAN_21;
        case 52:
            return MMModemBand.MM_MODEM_BAND_EUTRAN_22;
        case 53:
            return MMModemBand.MM_MODEM_BAND_EUTRAN_23;
        case 54:
            return MMModemBand.MM_MODEM_BAND_EUTRAN_24;
        case 55:
            return MMModemBand.MM_MODEM_BAND_EUTRAN_25;
        case 56:
            return MMModemBand.MM_MODEM_BAND_EUTRAN_26;
        case 57:
            return MMModemBand.MM_MODEM_BAND_EUTRAN_27;
        case 58:
            return MMModemBand.MM_MODEM_BAND_EUTRAN_28;
        case 59:
            return MMModemBand.MM_MODEM_BAND_EUTRAN_29;
        case 60:
            return MMModemBand.MM_MODEM_BAND_EUTRAN_30;
        case 61:
            return MMModemBand.MM_MODEM_BAND_EUTRAN_31;
        case 62:
            return MMModemBand.MM_MODEM_BAND_EUTRAN_32;
        case 63:
            return MMModemBand.MM_MODEM_BAND_EUTRAN_33;
        case 64:
            return MMModemBand.MM_MODEM_BAND_EUTRAN_34;
        case 65:
            return MMModemBand.MM_MODEM_BAND_EUTRAN_35;
        case 66:
            return MMModemBand.MM_MODEM_BAND_EUTRAN_36;
        case 67:
            return MMModemBand.MM_MODEM_BAND_EUTRAN_37;
        case 68:
            return MMModemBand.MM_MODEM_BAND_EUTRAN_38;
        case 69:
            return MMModemBand.MM_MODEM_BAND_EUTRAN_39;
        case 70:
            return MMModemBand.MM_MODEM_BAND_EUTRAN_40;
        case 71:
            return MMModemBand.MM_MODEM_BAND_EUTRAN_41;
        case 72:
            return MMModemBand.MM_MODEM_BAND_EUTRAN_42;
        case 73:
            return MMModemBand.MM_MODEM_BAND_EUTRAN_43;
        case 74:
            return MMModemBand.MM_MODEM_BAND_EUTRAN_44;
        case 75:
            return MMModemBand.MM_MODEM_BAND_EUTRAN_45;
        case 76:
            return MMModemBand.MM_MODEM_BAND_EUTRAN_46;
        case 77:
            return MMModemBand.MM_MODEM_BAND_EUTRAN_47;
        case 78:
            return MMModemBand.MM_MODEM_BAND_EUTRAN_48;
        case 79:
            return MMModemBand.MM_MODEM_BAND_EUTRAN_49;
        case 80:
            return MMModemBand.MM_MODEM_BAND_EUTRAN_50;
        case 81:
            return MMModemBand.MM_MODEM_BAND_EUTRAN_51;
        case 82:
            return MMModemBand.MM_MODEM_BAND_EUTRAN_52;
        case 83:
            return MMModemBand.MM_MODEM_BAND_EUTRAN_53;
        case 84:
            return MMModemBand.MM_MODEM_BAND_EUTRAN_54;
        case 85:
            return MMModemBand.MM_MODEM_BAND_EUTRAN_55;
        case 86:
            return MMModemBand.MM_MODEM_BAND_EUTRAN_56;
        case 87:
            return MMModemBand.MM_MODEM_BAND_EUTRAN_57;
        case 88:
            return MMModemBand.MM_MODEM_BAND_EUTRAN_58;
        case 89:
            return MMModemBand.MM_MODEM_BAND_EUTRAN_59;
        case 90:
            return MMModemBand.MM_MODEM_BAND_EUTRAN_60;
        case 91:
            return MMModemBand.MM_MODEM_BAND_EUTRAN_61;
        case 92:
            return MMModemBand.MM_MODEM_BAND_EUTRAN_62;
        case 93:
            return MMModemBand.MM_MODEM_BAND_EUTRAN_63;
        case 94:
            return MMModemBand.MM_MODEM_BAND_EUTRAN_64;
        case 95:
            return MMModemBand.MM_MODEM_BAND_EUTRAN_65;
        case 96:
            return MMModemBand.MM_MODEM_BAND_EUTRAN_66;
        case 97:
            return MMModemBand.MM_MODEM_BAND_EUTRAN_67;
        case 98:
            return MMModemBand.MM_MODEM_BAND_EUTRAN_68;
        case 99:
            return MMModemBand.MM_MODEM_BAND_EUTRAN_69;
        case 100:
            return MMModemBand.MM_MODEM_BAND_EUTRAN_70;
        case 101:
            return MMModemBand.MM_MODEM_BAND_EUTRAN_71;
        default:
            return MMModemBand.MM_MODEM_BAND_UNKNOWN;
        }
    }

    private static MMModemBand getMMCdmaBand(int band) {
        switch (band) {
        case 128:
            return MMModemBand.MM_MODEM_BAND_CDMA_BC0;
        case 129:
            return MMModemBand.MM_MODEM_BAND_CDMA_BC1;
        case 130:
            return MMModemBand.MM_MODEM_BAND_CDMA_BC2;
        case 131:
            return MMModemBand.MM_MODEM_BAND_CDMA_BC3;
        case 132:
            return MMModemBand.MM_MODEM_BAND_CDMA_BC4;
        case 134:
            return MMModemBand.MM_MODEM_BAND_CDMA_BC5;
        case 135:
            return MMModemBand.MM_MODEM_BAND_CDMA_BC6;
        case 136:
            return MMModemBand.MM_MODEM_BAND_CDMA_BC7;
        case 137:
            return MMModemBand.MM_MODEM_BAND_CDMA_BC8;
        case 138:
            return MMModemBand.MM_MODEM_BAND_CDMA_BC9;
        case 139:
            return MMModemBand.MM_MODEM_BAND_CDMA_BC10;
        case 140:
            return MMModemBand.MM_MODEM_BAND_CDMA_BC11;
        case 141:
            return MMModemBand.MM_MODEM_BAND_CDMA_BC12;
        case 142:
            return MMModemBand.MM_MODEM_BAND_CDMA_BC13;
        case 143:
            return MMModemBand.MM_MODEM_BAND_CDMA_BC14;
        case 144:
            return MMModemBand.MM_MODEM_BAND_CDMA_BC15;
        case 145:
            return MMModemBand.MM_MODEM_BAND_CDMA_BC16;
        case 146:
            return MMModemBand.MM_MODEM_BAND_CDMA_BC17;
        case 147:
            return MMModemBand.MM_MODEM_BAND_CDMA_BC18;
        case 148:
            return MMModemBand.MM_MODEM_BAND_CDMA_BC19;
        default:
            return MMModemBand.MM_MODEM_BAND_UNKNOWN;
        }
    }

    private static MMModemBand getMMUmtsBand(int band) {
        switch (band) {
        case 210:
            return MMModemBand.MM_MODEM_BAND_UTRAN_10;
        case 211:
            return MMModemBand.MM_MODEM_BAND_UTRAN_11;
        case 212:
            return MMModemBand.MM_MODEM_BAND_UTRAN_12;
        case 213:
            return MMModemBand.MM_MODEM_BAND_UTRAN_13;
        case 214:
            return MMModemBand.MM_MODEM_BAND_UTRAN_14;
        case 219:
            return MMModemBand.MM_MODEM_BAND_UTRAN_19;
        case 220:
            return MMModemBand.MM_MODEM_BAND_UTRAN_20;
        case 221:
            return MMModemBand.MM_MODEM_BAND_UTRAN_21;
        case 222:
            return MMModemBand.MM_MODEM_BAND_UTRAN_22;
        case 225:
            return MMModemBand.MM_MODEM_BAND_UTRAN_25;
        case 226:
            return MMModemBand.MM_MODEM_BAND_UTRAN_26;
        case 232:
            return MMModemBand.MM_MODEM_BAND_UTRAN_32;
        default:
            return MMModemBand.MM_MODEM_BAND_UNKNOWN;
        }
    }

    public static ModemBand toModemBands(UInt32 band) {
        int intBand = band.intValue();
        if (intBand > 0 && intBand <= 20) {
            return getGsmBand(intBand);
        } else if (intBand >= 31 && intBand <= 101) {
            return getLteBand(intBand);
        } else if (intBand >= 128 && intBand <= 148) {
            return getCdmaBand(intBand);
        } else if (intBand >= 210 && intBand <= 232) {
            return getUmtsBand(intBand);
        } else if (intBand == 256) {
            return ModemBand.ANY;
        } else {
            return ModemBand.UNKNOWN;
        }
    }

    private static ModemBand getGsmBand(int band) {
        switch (band) {
        case 1:
            return ModemBand.EGSM;
        case 2:
            return ModemBand.DCS;
        case 3:
            return ModemBand.PCS;
        case 4:
            return ModemBand.G850;
        case 5:
            return ModemBand.UTRAN_1;
        case 6:
            return ModemBand.UTRAN_3;
        case 7:
            return ModemBand.UTRAN_4;
        case 8:
            return ModemBand.UTRAN_6;
        case 9:
            return ModemBand.UTRAN_5;
        case 10:
            return ModemBand.UTRAN_8;
        case 11:
            return ModemBand.UTRAN_9;
        case 12:
            return ModemBand.UTRAN_2;
        case 13:
            return ModemBand.UTRAN_7;
        case 14:
            return ModemBand.G450;
        case 15:
            return ModemBand.G480;
        case 16:
            return ModemBand.G750;
        case 17:
            return ModemBand.G380;
        case 18:
            return ModemBand.G410;
        case 19:
            return ModemBand.G710;
        case 20:
            return ModemBand.G810;
        default:
            return ModemBand.UNKNOWN;
        }
    }

    private static ModemBand getLteBand(int band) {
        switch (band) {
        case 31:
            return ModemBand.EUTRAN_1;
        case 32:
            return ModemBand.EUTRAN_2;
        case 33:
            return ModemBand.EUTRAN_3;
        case 34:
            return ModemBand.EUTRAN_4;
        case 35:
            return ModemBand.EUTRAN_5;
        case 36:
            return ModemBand.EUTRAN_6;
        case 37:
            return ModemBand.EUTRAN_7;
        case 38:
            return ModemBand.EUTRAN_8;
        case 39:
            return ModemBand.EUTRAN_9;
        case 40:
            return ModemBand.EUTRAN_10;
        case 41:
            return ModemBand.EUTRAN_11;
        case 42:
            return ModemBand.EUTRAN_12;
        case 43:
            return ModemBand.EUTRAN_13;
        case 44:
            return ModemBand.EUTRAN_14;
        case 47:
            return ModemBand.EUTRAN_17;
        case 48:
            return ModemBand.EUTRAN_18;
        case 49:
            return ModemBand.EUTRAN_19;
        case 50:
            return ModemBand.EUTRAN_20;
        case 51:
            return ModemBand.EUTRAN_21;
        case 52:
            return ModemBand.EUTRAN_22;
        case 53:
            return ModemBand.EUTRAN_23;
        case 54:
            return ModemBand.EUTRAN_24;
        case 55:
            return ModemBand.EUTRAN_25;
        case 56:
            return ModemBand.EUTRAN_26;
        case 57:
            return ModemBand.EUTRAN_27;
        case 58:
            return ModemBand.EUTRAN_28;
        case 59:
            return ModemBand.EUTRAN_29;
        case 60:
            return ModemBand.EUTRAN_30;
        case 61:
            return ModemBand.EUTRAN_31;
        case 62:
            return ModemBand.EUTRAN_32;
        case 63:
            return ModemBand.EUTRAN_33;
        case 64:
            return ModemBand.EUTRAN_34;
        case 65:
            return ModemBand.EUTRAN_35;
        case 66:
            return ModemBand.EUTRAN_36;
        case 67:
            return ModemBand.EUTRAN_37;
        case 68:
            return ModemBand.EUTRAN_38;
        case 69:
            return ModemBand.EUTRAN_39;
        case 70:
            return ModemBand.EUTRAN_40;
        case 71:
            return ModemBand.EUTRAN_41;
        case 72:
            return ModemBand.EUTRAN_42;
        case 73:
            return ModemBand.EUTRAN_43;
        case 74:
            return ModemBand.EUTRAN_44;
        case 75:
            return ModemBand.EUTRAN_45;
        case 76:
            return ModemBand.EUTRAN_46;
        case 77:
            return ModemBand.EUTRAN_47;
        case 78:
            return ModemBand.EUTRAN_48;
        case 79:
            return ModemBand.EUTRAN_49;
        case 80:
            return ModemBand.EUTRAN_50;
        case 81:
            return ModemBand.EUTRAN_51;
        case 82:
            return ModemBand.EUTRAN_52;
        case 83:
            return ModemBand.EUTRAN_53;
        case 84:
            return ModemBand.EUTRAN_54;
        case 85:
            return ModemBand.EUTRAN_55;
        case 86:
            return ModemBand.EUTRAN_56;
        case 87:
            return ModemBand.EUTRAN_57;
        case 88:
            return ModemBand.EUTRAN_58;
        case 89:
            return ModemBand.EUTRAN_59;
        case 90:
            return ModemBand.EUTRAN_60;
        case 91:
            return ModemBand.EUTRAN_61;
        case 92:
            return ModemBand.EUTRAN_62;
        case 93:
            return ModemBand.EUTRAN_63;
        case 94:
            return ModemBand.EUTRAN_64;
        case 95:
            return ModemBand.EUTRAN_65;
        case 96:
            return ModemBand.EUTRAN_66;
        case 97:
            return ModemBand.EUTRAN_67;
        case 98:
            return ModemBand.EUTRAN_68;
        case 99:
            return ModemBand.EUTRAN_69;
        case 100:
            return ModemBand.EUTRAN_70;
        case 101:
            return ModemBand.EUTRAN_71;
        default:
            return ModemBand.UNKNOWN;
        }
    }

    private static ModemBand getCdmaBand(int band) {
        switch (band) {
        case 128:
            return ModemBand.CDMA_BC0;
        case 129:
            return ModemBand.CDMA_BC1;
        case 130:
            return ModemBand.CDMA_BC2;
        case 131:
            return ModemBand.CDMA_BC3;
        case 132:
            return ModemBand.CDMA_BC4;
        case 134:
            return ModemBand.CDMA_BC5;
        case 135:
            return ModemBand.CDMA_BC6;
        case 136:
            return ModemBand.CDMA_BC7;
        case 137:
            return ModemBand.CDMA_BC8;
        case 138:
            return ModemBand.CDMA_BC9;
        case 139:
            return ModemBand.CDMA_BC10;
        case 140:
            return ModemBand.CDMA_BC11;
        case 141:
            return ModemBand.CDMA_BC12;
        case 142:
            return ModemBand.CDMA_BC13;
        case 143:
            return ModemBand.CDMA_BC14;
        case 144:
            return ModemBand.CDMA_BC15;
        case 145:
            return ModemBand.CDMA_BC16;
        case 146:
            return ModemBand.CDMA_BC17;
        case 147:
            return ModemBand.CDMA_BC18;
        case 148:
            return ModemBand.CDMA_BC19;
        default:
            return ModemBand.UNKNOWN;
        }
    }

    private static ModemBand getUmtsBand(int band) {
        switch (band) {
        case 210:
            return ModemBand.UTRAN_10;
        case 211:
            return ModemBand.UTRAN_11;
        case 212:
            return ModemBand.UTRAN_12;
        case 213:
            return ModemBand.UTRAN_13;
        case 214:
            return ModemBand.UTRAN_14;
        case 219:
            return ModemBand.UTRAN_19;
        case 220:
            return ModemBand.UTRAN_20;
        case 221:
            return ModemBand.UTRAN_21;
        case 222:
            return ModemBand.UTRAN_22;
        case 225:
            return ModemBand.UTRAN_25;
        case 226:
            return ModemBand.UTRAN_26;
        case 232:
            return ModemBand.UTRAN_32;
        default:
            return ModemBand.UNKNOWN;
        }
    }
}

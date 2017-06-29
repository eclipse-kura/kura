/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/

package org.eclipse.kura.driver.binary;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class BinaryDataTypes {

    public static final BinaryData<Integer> UINT8 = new UInt8();
    public static final BinaryData<Integer> INT8 = new Int8();

    public static final BinaryData<Integer> UINT16_LE = new UInt16(Endianness.LITTLE_ENDIAN);
    public static final BinaryData<Integer> UINT16_BE = new UInt16(Endianness.BIG_ENDIAN);

    public static final BinaryData<Integer> INT16_LE = new Int16(Endianness.LITTLE_ENDIAN);
    public static final BinaryData<Integer> INT16_BE = new Int16(Endianness.BIG_ENDIAN);

    public static final BinaryData<Long> UINT32_LE = new UInt32(Endianness.LITTLE_ENDIAN);
    public static final BinaryData<Long> UINT32_BE = new UInt32(Endianness.BIG_ENDIAN);

    public static final BinaryData<Integer> INT32_LE = new Int32(Endianness.LITTLE_ENDIAN);
    public static final BinaryData<Integer> INT32_BE = new Int32(Endianness.BIG_ENDIAN);

    public static final BinaryData<Long> INT64_LE = new Int64(Endianness.LITTLE_ENDIAN);
    public static final BinaryData<Long> INT64_BE = new Int64(Endianness.BIG_ENDIAN);

    public static final BinaryData<java.lang.Float> FLOAT_LE = new Float(Endianness.LITTLE_ENDIAN);
    public static final BinaryData<java.lang.Float> FLOAT_BE = new Float(Endianness.BIG_ENDIAN);

    public static final BinaryData<java.lang.Double> DOUBLE_LE = new Double(Endianness.LITTLE_ENDIAN);
    public static final BinaryData<java.lang.Double> DOUBLE_BE = new Double(Endianness.BIG_ENDIAN);

    public static final List<String> NAMES = Collections.unmodifiableList(
            Arrays.asList("UINT8", "INT8", "UINT16_LE", "UINT16_BE", "INT16_LE", "INT16_BE", "UINT32_LE", "UINT32_BE",
                    "INT32_LE", "INT32_BE", "INT64_LE", "INT64_BE", "FLOAT_LE", "FLOAT_BE", "DOUBLE_LE", "DOUBLE_BE"));

    public static final List<BinaryData<?>> VALUES = Collections
            .unmodifiableList(Arrays.asList(UINT8, INT8, UINT16_LE, UINT16_BE, INT16_LE, INT16_BE, UINT32_LE, UINT32_BE,
                    INT32_LE, INT32_BE, INT64_LE, INT64_BE, FLOAT_LE, FLOAT_BE, DOUBLE_LE, DOUBLE_BE));

    public static BinaryData<?> parse(String s) {
        for (int i = 0; i < NAMES.size(); i++) {
            if (NAMES.get(i).equals(s)) {
                return VALUES.get(i);
            }
        }
        throw new IllegalArgumentException();
    }

    private BinaryDataTypes() {
    }
}

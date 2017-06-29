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

package org.eclipse.kura.driver.block.test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.function.BiFunction;

import org.eclipse.kura.driver.binary.BinaryData;
import org.eclipse.kura.driver.binary.BinaryDataTypes;
import org.eclipse.kura.driver.binary.ByteArrayBuffer;
import org.eclipse.kura.driver.binary.Endianness;
import org.junit.Test;

public class BinaryDataTest {

    @Test
    public void shouldSupportUInt8() {
        testReadWrite(BinaryDataTypes.UINT8, (endianness, size) -> new byte[] { (byte) 0xab }, 171);
    }

    @Test
    public void shouldSupportUInt16() {
        final byte[] testBuf = new byte[] { (byte) 0xa3, (byte) 0xc4 };
        testReadWrite(BinaryDataTypes.UINT16_LE, (endianness, size) -> testBuf, (Integer) 0xc4a3);
        testReadWrite(BinaryDataTypes.UINT16_BE, (endianness, size) -> testBuf, (Integer) 0xa3c4);
    }

    @Test
    public void shouldSupportUInt32() {
        final byte[] testBuf = new byte[] { (byte) 0xa3, (byte) 0xc4, (byte) 0x45, (byte) 0x83 };
        testReadWrite(BinaryDataTypes.UINT32_LE, (endianness, size) -> testBuf, 0x8345c4a3L);
        testReadWrite(BinaryDataTypes.UINT32_BE, (endianness, size) -> testBuf, 0xa3c44583L);
    }

    @Test
    public void shouldSupportInt8() {
        testReadWrite(BinaryDataTypes.INT8, (endianness, size) -> new byte[] { (byte) 0xab }, -85);
    }

    @Test
    public void shouldSupportInt16() {
        final byte[] testBuf = new byte[] { (byte) 0xa3, (byte) 0xc4 };
        testReadWrite(BinaryDataTypes.INT16_LE, (endianness, size) -> testBuf, -15197);
        testReadWrite(BinaryDataTypes.INT16_BE, (endianness, size) -> testBuf, -23612);
    }

    @Test
    public void shouldSupportInt32() {
        final byte[] testBuf = new byte[] { (byte) 0xa3, (byte) 0xc4, (byte) 0x45, (byte) 0x83 };
        testReadWrite(BinaryDataTypes.INT32_LE, (endianness, size) -> testBuf, -2092579677);
        testReadWrite(BinaryDataTypes.INT32_BE, (endianness, size) -> testBuf, -1547418237);
    }

    @Test
    public void shouldSupportInt64() {
        final byte[] testBuf = new byte[] { (byte) 0xa3, (byte) 0xc4, (byte) 0x45, (byte) 0x83, (byte) 0xa3,
                (byte) 0xc4, (byte) 0x45, (byte) 0x83 };
        testReadWrite(BinaryDataTypes.INT64_LE, (endianness, size) -> testBuf, -8987561274786855773L);
        testReadWrite(BinaryDataTypes.INT64_BE, (endianness, size) -> testBuf, -6646110718401428093L);
    }

    @Test
    public void shouldSupportFloat() {
        final byte[] testBuf = new byte[] { (byte) 0xa3, (byte) 0xc4, (byte) 0x45, (byte) 0x83 };
        testReadWrite(BinaryDataTypes.FLOAT_LE, (endianness, size) -> testBuf, (float) -5.8118825e-37);
        testReadWrite(BinaryDataTypes.FLOAT_BE, (endianness, size) -> testBuf, (float) -2.1279802e-17);
    }

    @Test
    public void shouldSupportDouble() {
        final byte[] testBuf = new byte[] { (byte) 0xa3, (byte) 0xc4, (byte) 0x45, (byte) 0x83, (byte) 0xa3,
                (byte) 0xc4, (byte) 0x45, (byte) 0x83 };
        testReadWrite(BinaryDataTypes.DOUBLE_LE, (endianness, size) -> testBuf, -6.816715214851188e-293);
        testReadWrite(BinaryDataTypes.DOUBLE_BE, (endianness, size) -> testBuf, -2.178907098281569e-136);
    }

    @Test
    public void shouldSupportSignedIntegerMax() {
        testReadWrite(BinaryDataTypes.INT8, BinaryDataTest::createSignedMax, (Integer) (int) Byte.MAX_VALUE);
        testReadWrite(BinaryDataTypes.INT16_BE, BinaryDataTest::createSignedMax, (Integer) (int) Short.MAX_VALUE);
        testReadWrite(BinaryDataTypes.INT16_LE, BinaryDataTest::createSignedMax, (Integer) (int) Short.MAX_VALUE);
        testReadWrite(BinaryDataTypes.INT32_BE, BinaryDataTest::createSignedMax, (Integer) Integer.MAX_VALUE);
        testReadWrite(BinaryDataTypes.INT32_LE, BinaryDataTest::createSignedMax, (Integer) Integer.MAX_VALUE);
        testReadWrite(BinaryDataTypes.INT64_BE, BinaryDataTest::createSignedMax, (Long) Long.MAX_VALUE);
        testReadWrite(BinaryDataTypes.INT64_LE, BinaryDataTest::createSignedMax, (Long) Long.MAX_VALUE);
    }

    @Test
    public void shouldSupportSignedIntegerMin() {
        testReadWrite(BinaryDataTypes.INT8, BinaryDataTest::createSignedMin, (Integer) (int) Byte.MIN_VALUE);
        testReadWrite(BinaryDataTypes.INT16_BE, BinaryDataTest::createSignedMin, (Integer) (int) Short.MIN_VALUE);
        testReadWrite(BinaryDataTypes.INT16_LE, BinaryDataTest::createSignedMin, (Integer) (int) Short.MIN_VALUE);
        testReadWrite(BinaryDataTypes.INT32_BE, BinaryDataTest::createSignedMin, (Integer) Integer.MIN_VALUE);
        testReadWrite(BinaryDataTypes.INT32_LE, BinaryDataTest::createSignedMin, (Integer) Integer.MIN_VALUE);
        testReadWrite(BinaryDataTypes.INT64_BE, BinaryDataTest::createSignedMin, (Long) Long.MIN_VALUE);
        testReadWrite(BinaryDataTypes.INT64_LE, BinaryDataTest::createSignedMin, (Long) Long.MIN_VALUE);
    }

    @Test
    public void shouldSupportUnsignedIntegerMax() {
        testReadWrite(BinaryDataTypes.UINT8, BinaryDataTest::createUnsignedMax, 0xff);
        testReadWrite(BinaryDataTypes.UINT16_BE, BinaryDataTest::createUnsignedMax, 0x0000ffff);
        testReadWrite(BinaryDataTypes.UINT16_LE, BinaryDataTest::createUnsignedMax, 0x0000ffff);
        testReadWrite(BinaryDataTypes.UINT32_BE, BinaryDataTest::createUnsignedMax, 0xffffffffL);
        testReadWrite(BinaryDataTypes.UINT32_LE, BinaryDataTest::createUnsignedMax, 0xffffffffL);
    }

    @Test
    public void shouldSupportUnsignedIntegerMin() {
        testReadWrite(BinaryDataTypes.UINT8, BinaryDataTest::createUnsignedMin, 0);
        testReadWrite(BinaryDataTypes.UINT16_BE, BinaryDataTest::createUnsignedMin, 0);
        testReadWrite(BinaryDataTypes.UINT16_LE, BinaryDataTest::createUnsignedMin, 0);
        testReadWrite(BinaryDataTypes.UINT32_BE, BinaryDataTest::createUnsignedMin, 0L);
        testReadWrite(BinaryDataTypes.UINT32_LE, BinaryDataTest::createUnsignedMin, 0L);
    }

    @Test
    public void shouldSupportFloatMax() {
        testReadWrite(BinaryDataTypes.FLOAT_BE, BinaryDataTest::createFloatMax, Float.MAX_VALUE);
        testReadWrite(BinaryDataTypes.FLOAT_LE, BinaryDataTest::createFloatMax, Float.MAX_VALUE);
    }

    @Test
    public void shouldSupportFloatPositiveMin() {
        testReadWrite(BinaryDataTypes.FLOAT_BE, BinaryDataTest::createFloatPositiveMin, Float.MIN_VALUE);
        testReadWrite(BinaryDataTypes.FLOAT_LE, BinaryDataTest::createFloatPositiveMin, Float.MIN_VALUE);
    }

    @Test
    public void shouldSupportDoubleMax() {
        testReadWrite(BinaryDataTypes.DOUBLE_BE, BinaryDataTest::createDoubleMax, Double.MAX_VALUE);
        testReadWrite(BinaryDataTypes.DOUBLE_LE, BinaryDataTest::createDoubleMax, Double.MAX_VALUE);
    }

    @Test
    public void shouldSupportDoublePositiveMin() {
        testReadWrite(BinaryDataTypes.DOUBLE_BE, BinaryDataTest::createFloatPositiveMin, Double.MIN_VALUE);
        testReadWrite(BinaryDataTypes.DOUBLE_LE, BinaryDataTest::createFloatPositiveMin, Double.MIN_VALUE);
    }

    private static void apply(byte[] data, Endianness endianness, BiFunction<byte[], Integer, Byte> func) {
        int start;
        int inc;
        int end;
        if (endianness == Endianness.BIG_ENDIAN) {
            start = 0;
            inc = 1;
            end = data.length;
        } else {
            start = data.length - 1;
            inc = -1;
            end = -1;
        }
        int j = 0;
        for (int i = start; i != end; i += inc) {
            data[i] = func.apply(data, j);
            j++;
        }
    }

    private static byte[] createSignedMax(Endianness endianness, int size) {
        byte[] result = new byte[size];
        apply(result, endianness, (data, i) -> {
            return (i == 0) ? (byte) 0x7f : (byte) 0xff;
        });

        return result;
    }

    private static byte[] createSignedMin(Endianness endianness, int size) {
        byte[] result = new byte[size];
        apply(result, endianness, (data, i) -> {
            return (i == 0) ? (byte) 0x80 : (byte) 0;
        });
        return result;
    }

    private static byte[] createUnsignedMax(Endianness endianness, int size) {
        byte[] result = new byte[size];
        Arrays.fill(result, (byte) 0xff);
        return result;
    }

    private static byte[] createUnsignedMin(Endianness endianness, int size) {
        return new byte[size];
    }

    private static byte[] createFloatMax(Endianness endianness, int size) {
        byte[] result = new byte[size];
        apply(result, endianness, (data, i) -> {
            if (i == 0) {
                return (byte) 0x7f;
            }
            if (i == 1) {
                return (byte) 0x7f;
            }
            return (byte) 0xff;
        });
        return result;
    }

    private static byte[] createFloatPositiveMin(Endianness endianness, int size) {
        byte[] result = new byte[size];
        apply(result, endianness, (data, i) -> {
            return (i == data.length - 1) ? (byte) 0x01 : (byte) 0x00;
        });
        return result;
    }

    private static byte[] createDoubleMax(Endianness endianness, int size) {
        byte[] result = new byte[size];
        apply(result, endianness, (data, i) -> {
            if (i == 0) {
                return (byte) 0x7f;
            }
            if (i == 1) {
                return (byte) 0xef;
            }
            return (byte) 0xff;
        });
        return result;
    }

    private <T> void testReadWrite(BinaryData<T> data, BiFunction<Endianness, Integer, byte[]> bufferProvider,
            T expectedValue) {
        ByteArrayBuffer testBuf = new ByteArrayBuffer(bufferProvider.apply(data.getEndianness(), data.getSize()));
        assertEquals(expectedValue, data.read(testBuf, 0));
        ByteArrayBuffer writeBuf = new ByteArrayBuffer(new byte[data.getSize()]);
        data.write(writeBuf, 0, expectedValue);
        assertArrayEquals(testBuf.getBackingArray(), writeBuf.getBackingArray());
    }

}

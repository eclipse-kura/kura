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

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.eclipse.kura.driver.binary.BinaryData;
import org.eclipse.kura.driver.binary.BinaryDataTypes;
import org.eclipse.kura.driver.binary.Buffer;
import org.eclipse.kura.driver.binary.ByteArray;
import org.eclipse.kura.driver.binary.ByteArrayBuffer;
import org.eclipse.kura.driver.binary.Endianness;
import org.eclipse.kura.driver.binary.UnsignedIntegerLE;
import org.eclipse.kura.driver.binary.adapter.GainOffset;
import org.eclipse.kura.driver.binary.adapter.StringData;
import org.eclipse.kura.driver.binary.adapter.ToBoolean;
import org.junit.Assert;
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

    @Test
    public void shouldSupportUnsignedInteger() {
        final byte[] testBuf = new byte[] { 1, 2, 3, 4 };
        testRead(new UnsignedIntegerLE(32, 0), (endiannes, size) -> testBuf, BigInteger.valueOf(67305985));

        final byte[] testBuf2 = new byte[] { (byte) 0xAA, (byte) 0xAA, (byte) 0xAA, (byte) 0xAA, (byte) 0xAA,
                (byte) 0xAA, (byte) 0xAA, (byte) 0xAA };
        testRead(new UnsignedIntegerLE(3, 0), (endiannes, size) -> testBuf2, BigInteger.valueOf(2));
        testRead(new UnsignedIntegerLE(3, 3), (endiannes, size) -> testBuf2, BigInteger.valueOf(5));
        testRead(new UnsignedIntegerLE(6, 2), (endiannes, size) -> testBuf2, BigInteger.valueOf(42));
        testRead(new UnsignedIntegerLE(2, 7), (endiannes, size) -> testBuf2, BigInteger.valueOf(1));
        testRead(new UnsignedIntegerLE(12, 7), (endiannes, size) -> testBuf2, BigInteger.valueOf(1365));
        testRead(new UnsignedIntegerLE(17, 7), (endiannes, size) -> testBuf2, BigInteger.valueOf(87381));
    }

    @Test
    public void shouldSupportByteArray() {
        final byte[] testBuf = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 };
        testRead(new ByteArray(testBuf.length), (size, endianness) -> testBuf, testBuf, Assert::assertArrayEquals);
        testRead(new ByteArray(4), (size, endianness) -> testBuf, new byte[] { 1, 2, 3, 4 }, Assert::assertArrayEquals);
        testRead(new ByteArray(40), (size, endianness) -> testBuf, testBuf, Assert::assertArrayEquals);
    }

    @Test
    public void shouldSupportGainOffset() {
        testAdapterRead(a -> new GainOffset(a, 1.0f, 0.0f), (byte) 10, 10.0d);
        testAdapterRead(a -> new GainOffset(a, 1.0f, 0.0f), (short) 10, 10.0d);
        testAdapterRead(a -> new GainOffset(a, 1.0f, 0.0f), (int) 10, 10.0d);
        testAdapterRead(a -> new GainOffset(a, 1.0f, 0.0f), (long) 10, 10.0d);
        testAdapterRead(a -> new GainOffset(a, 1.0f, 0.0f), (short) 10, 10.0d);
        testAdapterRead(a -> new GainOffset(a, 1.0f, 0.0f), 12.5f, 12.5d);
        testAdapterRead(a -> new GainOffset(a, 1.0f, 0.0f), 10.123d, 10.123d);
        testAdapterRead(a -> new GainOffset(a, 1.0f, 0.0f), BigInteger.valueOf(10), 10.0d);

        testAdapterWrite(a -> new GainOffset(a, 1.0f, 0.0f), 10.0d, (short) 10);
        testAdapterWrite(a -> new GainOffset(a, 1.0f, 0.0f), 10.0d, (short) 10);
        testAdapterWrite(a -> new GainOffset(a, 1.0f, 0.0f), 10.0d, (int) 10);
        testAdapterWrite(a -> new GainOffset(a, 1.0f, 0.0f), 10.0d, (long) 10);
        testAdapterWrite(a -> new GainOffset(a, 1.0f, 0.0f), 10.0d, (short) 10);
        testAdapterWrite(a -> new GainOffset(a, 1.0f, 0.0f), 10.0d, (float) 10);
        testAdapterWrite(a -> new GainOffset(a, 1.0f, 0.0f), 10.0d, (double) 10);
        testAdapterWrite(a -> new GainOffset(a, 1.0f, 0.0f), 10.0d, BigInteger.valueOf(10));

        testAdapterRead(a -> new GainOffset(a, 2.0f, 0.0f), (byte) 10, 20.0d);
        testAdapterRead(a -> new GainOffset(a, 3.0f, 0.0f), (short) 10, 30.0d);
        testAdapterRead(a -> new GainOffset(a, 5.5f, 0.0f), (int) 10, 55.0d);
        testAdapterRead(a -> new GainOffset(a, 15.0f, 0.0f), (long) 10, 150.0d);
        testAdapterRead(a -> new GainOffset(a, -1.5f, 0.0f), (short) 10, -15.0d);
        testAdapterRead(a -> new GainOffset(a, -2.0f, 0.0f), (float) 10, -20.0d);
        testAdapterRead(a -> new GainOffset(a, -5.0f, 0.0f), (double) 10, -50.0d);
        testAdapterRead(a -> new GainOffset(a, -11.0f, 0.0f), BigInteger.valueOf(10), -110.0d);

        testAdapterRead(a -> new GainOffset(a, 2.0f, 1.0f), (byte) 10, (double) (20 + 1));
        testAdapterRead(a -> new GainOffset(a, 3.0f, 4.0f), (short) 10, (double) (30 + 4));
        testAdapterRead(a -> new GainOffset(a, 5.0f, -2.0f), (int) 10, (double) (50 - 2));
        testAdapterRead(a -> new GainOffset(a, 15.0f, -5.0f), (long) 10, (double) (150 - 5));
        testAdapterRead(a -> new GainOffset(a, -1.0f, 10.0f), (short) 10, (double) (-10 + 10));
        testAdapterRead(a -> new GainOffset(a, -2.0f, 54.0f), (float) 10, (double) (-20 + 54));
        testAdapterRead(a -> new GainOffset(a, -5.0f, 1.0f), (double) 10, (double) (-50 + 1));
        testAdapterRead(a -> new GainOffset(a, -11.0f, 22.0f), BigInteger.valueOf(10), (double) (-110 + 22));

        testAdapterWrite(a -> new GainOffset(a, 2.0f, 0.0f), 10.0d, (byte) 20);
        testAdapterWrite(a -> new GainOffset(a, 3.0f, 0.0f), 10.0d, (short) 30);
        testAdapterWrite(a -> new GainOffset(a, 5.0f, 0.0f), 10.0d, (int) 50);
        testAdapterWrite(a -> new GainOffset(a, 15.0f, 0.0f), 10.0d, (long) 150);
        testAdapterWrite(a -> new GainOffset(a, -1.0f, 0.0f), 10.0d, (short) -10);
        testAdapterWrite(a -> new GainOffset(a, -2.0f, 0.0f), 10.0d, (float) -20);
        testAdapterWrite(a -> new GainOffset(a, -5.0f, 0.0f), 10.0d, (double) -50);
        testAdapterWrite(a -> new GainOffset(a, -11.0f, 0.0f), 10.0d, BigInteger.valueOf(-110));

        testAdapterWrite(a -> new GainOffset(a, 2.0f, 1.0f), 10.0d, (byte) (20 + 1));
        testAdapterWrite(a -> new GainOffset(a, 3.0f, 4.0f), 10.0d, (short) (30 + 4));
        testAdapterWrite(a -> new GainOffset(a, 5.0f, -2.0f), 10.0d, (int) (50 - 2));
        testAdapterWrite(a -> new GainOffset(a, 15.0f, -5.0f), 10.0d, (long) (150 - 5));
        testAdapterWrite(a -> new GainOffset(a, -1.0f, 10.0f), 10.0d, (short) (-10 + 10));
        testAdapterWrite(a -> new GainOffset(a, -2.0f, 54.0f), 10.0d, (float) (-20 + 54));
        testAdapterWrite(a -> new GainOffset(a, -5.0f, 1.0f), 10.0d, (double) (-50 + 1));
        testAdapterWrite(a -> new GainOffset(a, -11.0f, 22.0f), 10.0d, BigInteger.valueOf(-110 + 22));
    }

    @Test
    public void shouldSupportStringData() {
        testAdapterRead(a -> new StringData(a, StandardCharsets.US_ASCII), new byte[] { 0x74, 0x65, 0x73, 0x74 },
                "test");
        testAdapterWrite(a -> new StringData(a, StandardCharsets.US_ASCII), "test",
                new byte[] { 0x74, 0x65, 0x73, 0x74 }, Assert::assertArrayEquals);
    }

    @Test
    public void shouldSupportToBooolean() {
        testAdapterRead(ToBoolean::new, (byte) 10, true);
        testAdapterRead(ToBoolean::new, (short) 10, true);
        testAdapterRead(ToBoolean::new, (int) 10, true);
        testAdapterRead(ToBoolean::new, (long) 10, true);
        testAdapterRead(ToBoolean::new, (short) 10, true);
        testAdapterRead(ToBoolean::new, (float) 10, true);
        testAdapterRead(ToBoolean::new, (double) 10, true);
        testAdapterRead(ToBoolean::new, BigInteger.valueOf(10), true);

        testAdapterRead(ToBoolean::new, (byte) 0, false);
        testAdapterRead(ToBoolean::new, (short) 0, false);
        testAdapterRead(ToBoolean::new, (int) 0, false);
        testAdapterRead(ToBoolean::new, (long) 0, false);
        testAdapterRead(ToBoolean::new, (short) 0, false);
        testAdapterRead(ToBoolean::new, (float) 0, false);
        testAdapterRead(ToBoolean::new, (double) 0, false);
        testAdapterRead(ToBoolean::new, BigInteger.valueOf(0), false);
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

    private <T> void testRead(BinaryData<T> data, BiFunction<Endianness, Integer, byte[]> bufferProvider,
            T expectedValue) {
        testRead(data, bufferProvider, expectedValue, Assert::assertEquals);
    }

    private <T> void testRead(BinaryData<T> data, BiFunction<Endianness, Integer, byte[]> bufferProvider,
            T expectedValue, final BiConsumer<T, T> validator) {
        ByteArrayBuffer testBuf = new ByteArrayBuffer(bufferProvider.apply(data.getEndianness(), data.getSize()));
        validator.accept(expectedValue, data.read(testBuf, 0));
    }

    private <T> void testReadWrite(BinaryData<T> data, BiFunction<Endianness, Integer, byte[]> bufferProvider,
            T expectedValue) {
        testReadWrite(data, bufferProvider, expectedValue, Assert::assertEquals);
    }

    private <T> void testReadWrite(BinaryData<T> data, BiFunction<Endianness, Integer, byte[]> bufferProvider,
            T expectedValue, final BiConsumer<T, T> readValidator) {
        ByteArrayBuffer testBuf = new ByteArrayBuffer(bufferProvider.apply(data.getEndianness(), data.getSize()));
        readValidator.accept(expectedValue, data.read(testBuf, 0));
        ByteArrayBuffer writeBuf = new ByteArrayBuffer(new byte[data.getSize()]);
        data.write(writeBuf, 0, expectedValue);
        assertArrayEquals(testBuf.getBackingArray(), writeBuf.getBackingArray());
    }

    private <T, U> void testAdapterRead(final Function<BinaryData<T>, BinaryData<U>> supplier, final T suppliedValue,
            final U expectedValue) {
        testAdapterRead(supplier, suppliedValue, expectedValue, Assert::assertEquals);
    }

    private <T, U> void testAdapterWrite(final Function<BinaryData<T>, BinaryData<U>> supplier, final U writtenValue,
            final T forwardedValue) {
        testAdapterWrite(supplier, writtenValue, forwardedValue, Assert::assertEquals);
    }

    private <T, U> void testAdapterRead(final Function<BinaryData<T>, BinaryData<U>> supplier, final T suppliedValue,
            final U expectedValue, final BiConsumer<U, U> validator) {
        final AdapterHelper<T> helper = new AdapterHelper<>(suppliedValue);
        final BinaryData<U> underTest = supplier.apply(helper);
        helper.assertIsWrapper(underTest);
        validator.accept(expectedValue, underTest.read(new ByteArrayBuffer(new byte[0]), 0));
    }

    private <T, U> void testAdapterWrite(final Function<BinaryData<T>, BinaryData<U>> supplier, final U writtenValue,
            final T forwardedValue, final BiConsumer<T, T> validator) {
        @SuppressWarnings("unchecked")
        final AdapterHelper<T> helper = new AdapterHelper<>((Class<T>) forwardedValue.getClass());
        final BinaryData<U> underTest = supplier.apply(helper);
        helper.assertIsWrapper(underTest);
        underTest.write(new ByteArrayBuffer(new byte[0]), 0, writtenValue);
        validator.accept(forwardedValue, helper.getValue());
    }

    private static final class AdapterHelper<T> implements BinaryData<T> {

        private T value;
        private final Class<T> valueType;
        private final Endianness endianness;
        private final int size;

        public AdapterHelper(T value, Class<T> valueType, Endianness endianness, int size) {
            this.value = value;
            this.valueType = valueType;
            this.endianness = endianness;
            this.size = size;
        }

        @SuppressWarnings("unchecked")
        public AdapterHelper(final T value) {
            this(value, (Class<T>) value.getClass(), Endianness.LITTLE_ENDIAN, 1);
        }

        public AdapterHelper(final Class<T> valueType) {
            this(null, valueType, Endianness.LITTLE_ENDIAN, 1);
        }

        @Override
        public Endianness getEndianness() {
            return endianness;
        }

        @Override
        public int getSize() {
            return size;
        }

        @Override
        public void write(Buffer buf, int offset, T value) {
            this.value = value;
        }

        @Override
        public T read(Buffer buf, int offset) {
            return value;
        }

        public T getValue() {
            return value;
        }

        @Override
        public Class<T> getValueType() {
            return valueType;
        }

        public <U> void assertIsWrapper(final BinaryData<U> wrapper) {
            assertEquals(getEndianness(), wrapper.getEndianness());
            assertEquals(getSize(), wrapper.getSize());
        }
    }

}

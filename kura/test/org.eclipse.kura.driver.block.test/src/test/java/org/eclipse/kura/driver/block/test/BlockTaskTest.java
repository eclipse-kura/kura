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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.eclipse.kura.channel.ChannelFlag;
import org.eclipse.kura.channel.ChannelRecord;
import org.eclipse.kura.driver.binary.BinaryDataTypes;
import org.eclipse.kura.driver.binary.Buffer;
import org.eclipse.kura.driver.binary.ByteArrayBuffer;
import org.eclipse.kura.driver.block.task.BinaryDataTask;
import org.eclipse.kura.driver.block.task.BitTask;
import org.eclipse.kura.driver.block.task.ByteArrayTask;
import org.eclipse.kura.driver.block.task.ChannelBlockTask;
import org.eclipse.kura.driver.block.task.Mode;
import org.eclipse.kura.driver.block.task.StringTask;
import org.eclipse.kura.driver.block.task.ToplevelBlockTask;
import org.eclipse.kura.type.DataType;
import org.eclipse.kura.type.TypedValues;
import org.junit.Test;

public class BlockTaskTest {

    @Test
    public void shouldSupportBinaryDataTask() throws IOException {
        testReadWrite((record, offset, mode) -> new BinaryDataTask<>(record, offset, BinaryDataTypes.UINT32_BE, mode),
                new byte[] { 1, 2, 3, 4 }, 16909060L);
    }

    @Test
    public void shouldSupportBinaryDataTaskTypeConversions() throws IOException {
        testReadWrite((record, offset, mode) -> new BinaryDataTask<>(record, offset, BinaryDataTypes.INT32_BE,
                DataType.DOUBLE, mode), new byte[] { 1, 2, 3, 4 }, ((Number) 16909060).doubleValue());
        testReadWrite((record, offset, mode) -> new BinaryDataTask<>(record, offset, BinaryDataTypes.INT32_BE,
                DataType.FLOAT, mode), new byte[] { 1, 2, 3, 4 }, ((Number) 16909060).floatValue());
        testReadWrite((record, offset, mode) -> new BinaryDataTask<>(record, offset, BinaryDataTypes.INT32_BE,
                DataType.LONG, mode), new byte[] { 1, 2, 3, 4 }, ((Number) 16909060).longValue());
        testReadWrite((record, offset, mode) -> new BinaryDataTask<>(record, offset, BinaryDataTypes.UINT32_BE,
                DataType.INTEGER, mode), new byte[] { 1, 2, 3, 4 }, 16909060);

        testReadWrite((record, offset, mode) -> new BinaryDataTask<>(record, offset, BinaryDataTypes.UINT32_BE,
                DataType.STRING, mode), new byte[] { 1, 2, 3, 4 }, "16909060");
        testReadWrite((record, offset, mode) -> new BinaryDataTask<>(record, offset, BinaryDataTypes.INT32_BE,
                DataType.STRING, mode), new byte[] { 1, 2, 3, 4 }, "16909060");

        testReadWrite((record, offset, mode) -> new BinaryDataTask<>(record, offset, BinaryDataTypes.FLOAT_BE,
                DataType.INTEGER, mode), new byte[] { 0x47, (byte) 0x80, 0, 0 }, 65536);
        testReadWrite((record, offset, mode) -> new BinaryDataTask<>(record, offset, BinaryDataTypes.DOUBLE_BE,
                DataType.INTEGER, mode), new byte[] { 0x40, (byte) 0xf0, 0, 0, 0, 0, 0, 0 }, 65536);
        testReadWrite((record, offset, mode) -> new BinaryDataTask<>(record, offset, BinaryDataTypes.FLOAT_BE,
                DataType.LONG, mode), new byte[] { 0x47, (byte) 0x80, 0, 0 }, 65536L);
        testReadWrite((record, offset, mode) -> new BinaryDataTask<>(record, offset, BinaryDataTypes.DOUBLE_BE,
                DataType.LONG, mode), new byte[] { 0x40, (byte) 0xf0, 0, 0, 0, 0, 0, 0 }, 65536L);

        testReadWrite((record, offset, mode) -> new BinaryDataTask<>(record, offset, BinaryDataTypes.FLOAT_BE,
                DataType.STRING, mode), new byte[] { 0x47, (byte) 0x80, 0, 0 }, "65536.0");
        testReadWrite((record, offset, mode) -> new BinaryDataTask<>(record, offset, BinaryDataTypes.DOUBLE_BE,
                DataType.STRING, mode), new byte[] { 0x40, (byte) 0xf0, 0, 0, 0, 0, 0, 0 }, "65536.0");
    }

    @Test
    public void shouldSupportBitTask() throws IOException {
        testRead((record, offset, mode) -> new BitTask(record, offset, 0, mode), new byte[] { 1 << 0 }, true);
        testRead((record, offset, mode) -> new BitTask(record, offset, 1, mode), new byte[] { 1 << 1 }, true);
        testRead((record, offset, mode) -> new BitTask(record, offset, 2, mode), new byte[] { 1 << 2 }, true);
        testRead((record, offset, mode) -> new BitTask(record, offset, 3, mode), new byte[] { 1 << 3 }, true);
        testRead((record, offset, mode) -> new BitTask(record, offset, 4, mode), new byte[] { 1 << 4 }, true);
        testRead((record, offset, mode) -> new BitTask(record, offset, 5, mode), new byte[] { 1 << 5 }, true);
        testRead((record, offset, mode) -> new BitTask(record, offset, 6, mode), new byte[] { 1 << 6 }, true);
        testRead((record, offset, mode) -> new BitTask(record, offset, 7, mode), new byte[] { (byte) (1 << 7) }, true);

        testRead((record, offset, mode) -> new BitTask(record, offset, 0, mode), new byte[] { (byte) (1 << 7) }, false);

        testUpdate((record, offset, mode) -> new BitTask(record, offset, 0, mode), new byte[] { 1 << 0 }, true);
        testUpdate((record, offset, mode) -> new BitTask(record, offset, 1, mode), new byte[] { 1 << 1 }, true);
        testUpdate((record, offset, mode) -> new BitTask(record, offset, 2, mode), new byte[] { 1 << 2 }, true);
        testUpdate((record, offset, mode) -> new BitTask(record, offset, 3, mode), new byte[] { 1 << 3 }, true);
        testUpdate((record, offset, mode) -> new BitTask(record, offset, 4, mode), new byte[] { 1 << 4 }, true);
        testUpdate((record, offset, mode) -> new BitTask(record, offset, 5, mode), new byte[] { 1 << 5 }, true);
        testUpdate((record, offset, mode) -> new BitTask(record, offset, 6, mode), new byte[] { 1 << 6 }, true);
        testUpdate((record, offset, mode) -> new BitTask(record, offset, 7, mode), new byte[] { (byte) (1 << 7) },
                true);
    }

    @Test
    public void shouldSupportStringTask() throws IOException {
        final String testString = "test string";
        testReadWrite((record, offset, mode) -> new StringTask(record, offset, offset + testString.length(), mode),
                testString.getBytes(StandardCharsets.US_ASCII), testString);
    }

    @Test
    public void shouldSupportByteArrayTask() throws IOException {
        final byte[] testByteArray = new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
        testReadWrite((record, offset, mode) -> new ByteArrayTask(record, offset, offset + testByteArray.length, mode),
                testByteArray, Arrays.copyOf(testByteArray, testByteArray.length));
    }

    private ToplevelBlockTask getToplevelBlockTask(byte[] buf, Mode mode) {
        return new ToplevelBlockTask(0, buf.length, mode) {

            @Override
            public void processBuffer() throws IOException {
            }

            @Override
            public Buffer getBuffer() {
                return new ByteArrayBuffer(buf);
            }
        };
    }

    private interface TaskProvider {

        public ChannelBlockTask get(ChannelRecord record, int offset, Mode mode);
    }

    private void testReadWrite(TaskProvider taskProvider, byte[] raw, Object javaValue) throws IOException {
        testRead(taskProvider, raw, javaValue);
        testWrite(taskProvider, raw, javaValue);
    }

    private void testRead(TaskProvider taskProvider, byte[] raw, Object javaValue) throws IOException {
        testRead(taskProvider, raw, javaValue, DataType.BOOLEAN);
    }

    private void testRead(TaskProvider taskProvider, byte[] raw, Object javaValue, DataType valueType)
            throws IOException {
        int[] offsets = new int[] { 0, 7 };
        for (final int offset : offsets) {
            ChannelRecord record = ChannelRecord.createReadRecord("test", valueType);
            byte[] buf = new byte[offset + raw.length];
            System.arraycopy(raw, 0, buf, offset, raw.length);
            ChannelBlockTask task = taskProvider.get(record, offset, Mode.READ);
            ToplevelBlockTask parent = getToplevelBlockTask(buf, Mode.READ);
            parent.addChild(task);
            parent.run();
            assertEquals(ChannelFlag.SUCCESS, task.getRecord().getChannelStatus().getChannelFlag());
            if (!javaValue.getClass().isArray()) {
                assertEquals(javaValue, task.getRecord().getValue().getValue());
            } else {
                assertArrayEquals((byte[]) javaValue, (byte[]) task.getRecord().getValue().getValue());
            }
        }
    }

    private void testWrite(TaskProvider taskProvider, byte[] raw, Object javaValue) throws IOException {
        int[] offsets = new int[] { 0, 7 };
        for (final int offset : offsets) {
            ChannelRecord record = ChannelRecord.createWriteRecord("test", TypedValues.newTypedValue(javaValue));
            byte[] buf = new byte[offset + raw.length];
            ChannelBlockTask task = taskProvider.get(record, offset, Mode.WRITE);
            ToplevelBlockTask parent = getToplevelBlockTask(buf, Mode.WRITE);
            parent.addChild(task);
            parent.run();
            assertEquals(ChannelFlag.SUCCESS, record.getChannelStatus().getChannelFlag());
            byte[] result = new byte[raw.length];
            System.arraycopy(buf, offset, result, 0, raw.length);
            assertArrayEquals(raw, result);
        }
    }

    private void testUpdate(TaskProvider taskProvider, byte[] raw, Object javaValue) throws IOException {
        int[] offsets = new int[] { 0, 7 };
        for (final int offset : offsets) {
            ChannelRecord record = ChannelRecord.createWriteRecord("test", TypedValues.newTypedValue(javaValue));
            byte[] buf = new byte[offset + raw.length];
            ChannelBlockTask task = taskProvider.get(record, offset, Mode.UPDATE);
            ToplevelBlockTask readParent = getToplevelBlockTask(buf, Mode.READ);
            readParent.addChild(task);
            ToplevelBlockTask writeParent = getToplevelBlockTask(buf, Mode.WRITE);
            writeParent.addChild(task);
            readParent.run();
            writeParent.run();
            assertEquals(ChannelFlag.SUCCESS, record.getChannelStatus().getChannelFlag());
            byte[] result = new byte[raw.length];
            System.arraycopy(buf, offset, result, 0, raw.length);
            assertArrayEquals(raw, result);
        }
    }
}

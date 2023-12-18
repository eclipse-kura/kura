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
package org.eclipse.kura.cloudconnection.sparkplug.mqtt.message.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.stream.Collectors;

import org.eclipse.kura.cloudconnection.sparkplug.mqtt.message.SparkplugBProtobufPayloadBuilder;
import org.eclipse.kura.cloudconnection.sparkplug.mqtt.message.protobuf.SparkplugBProto.DataType;
import org.eclipse.kura.cloudconnection.sparkplug.mqtt.message.protobuf.SparkplugBProto.Payload;
import org.eclipse.kura.cloudconnection.sparkplug.mqtt.message.protobuf.SparkplugBProto.Payload.Metric;
import org.eclipse.kura.type.BooleanValue;
import org.eclipse.kura.type.ByteArrayValue;
import org.eclipse.kura.type.DoubleValue;
import org.eclipse.kura.type.FloatValue;
import org.eclipse.kura.type.IntegerValue;
import org.eclipse.kura.type.StringValue;
import org.eclipse.kura.type.TypedValue;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Enclosed.class)
public class SparkplugBProtobufPayloadBuilderTest {

    /*
     * Scenarios
     */

    @RunWith(Parameterized.class)
    public static class SupportedDataTypesTest extends Steps {

        @Parameters
        public static Collection<Object[]> parameters() {
            return Arrays.asList(new Object[][] {
                    { "name", true, DataType.Boolean, 1L }, { "name", "hello".getBytes(), DataType.Bytes, 1L },
                    { "name", new Double(12), DataType.Double, 1L },
                    { "name", 1.1f, DataType.Float, 1L },
                    { "name", (byte) 8, DataType.Int8, 1L },
                    { "name", (short) 16, DataType.Int16, 1L },
                    { "name", 32, DataType.Int32, 1L },
                    { "name", 64L, DataType.Int64, 1L },
                    { "name", "a string", DataType.String, 1L }, { "name", "a string", DataType.Text, 1L },
                    { "name", "a string", DataType.UUID, 1L },
                    { "name", new Date(), DataType.DateTime, 1L },
                    { "name", (short) 8, DataType.UInt8, 1L }, { "name", 16, DataType.UInt16, 1L },
                    { "name", 32L, DataType.UInt32, 1L }, { "name", BigInteger.valueOf(64L), DataType.UInt64, 1L }
            });
        }

        private String name;
        private Object value;
        private DataType type;
        private long timestamp;

        public SupportedDataTypesTest(Object name, Object value, Object type, Object timestamp) {
            this.name = (String) name;
            this.value = value;
            this.type = (DataType) type;
            this.timestamp = (long) timestamp;
        }

        @Test
        public void shouldCorrectlySetMetricValue() {
            givenMetric(this.name, this.value, this.type, this.timestamp);

            whenBuildPayload();

            thenPayloadContainsMetric(this.name, this.value);
        }

    }

    @RunWith(Parameterized.class)
    public static class UnsupportedDataTypesTest extends Steps {

        @Parameters
        public static Collection<Object[]> parameters() {
            return Arrays.asList(new Object[][] { { "name", true, DataType.DataSet, 1L },
                    { "name", true, DataType.Template, 1L }, { "name", true, DataType.PropertySet, 1L },
                    { "name", true, DataType.PropertySetList, 1L }, { "name", true, DataType.File, 1L },
                    { "name", true, DataType.BooleanArray, 1L }, { "name", true, DataType.DateTimeArray, 1L },
                    { "name", true, DataType.UInt8Array, 1L }, { "name", true, DataType.UInt64Array, 1L },
                    { "name", true, DataType.UInt32Array, 1L }, { "name", true, DataType.UInt16Array, 1L },
                    { "name", true, DataType.StringArray, 1L }, { "name", true, DataType.Int8Array, 1L },
                    { "name", true, DataType.Int64Array, 1L }, { "name", true, DataType.Int32Array, 1L },
                    { "name", true, DataType.Int16Array, 1L }, { "name", true, DataType.FloatArray, 1L },
                    { "name", true, DataType.DoubleArray, 1L }, { "name", true, DataType.Unknown, 1L } });
        }

        private String name;
        private Object value;
        private DataType type;
        private long timestamp;

        public UnsupportedDataTypesTest(Object name, Object value, Object type, Object timestamp) {
            this.name = (String) name;
            this.value = value;
            this.type = (DataType) type;
            this.timestamp = (long) timestamp;
        }

        @Test
        public void shouldCorrectlySetMetricValue() {
            whenGivenMetric(this.name, this.value, this.type, this.timestamp);

            thenExceptionOccurred(UnsupportedOperationException.class);
        }

    }

    public static class SparkplugPropertiesTest extends Steps {

        @Test
        public void shouldReturnCorrectBdSeq() {
            givenBdSeq(12L, 120L);
            
            whenBuildPayload();
            
            thenPayloadContainsMetric("bdSeq", 12L);
        }

        @Test
        public void shouldReturnCorrectSeq() {
            givenSeq(13L);

            whenBuildPayload();

            thenSeqEquals(13L);
        }

        @Test
        public void shouldReturnCorrectTimestamp() {
            givenTimestamp(13123L);

            whenBuildPayload();

            thenTimestampEquals(13123L);
        }

    }

    @RunWith(Parameterized.class)
    public static class TypedValuesMappingTest extends Steps {

        @Parameters
        public static Collection<Object[]> parameters() {
            return Arrays.asList(new Object[][] { { "name", new BooleanValue(true), 1L },
                    { "name", new ByteArrayValue("test".getBytes()), 1L }, { "name", new FloatValue(1.12f), 1L },
                    { "name", new DoubleValue((double) 12), 1L }, { "name", new IntegerValue(11), 1L },
                    { "name", new StringValue("a string"), 1L }
            });
        }

        private String name;
        private TypedValue<?> value;
        private long timestamp;

        public TypedValuesMappingTest(Object name, Object value, Object timestamp) {
            this.name = (String) name;
            this.value = (TypedValue<?>) value;
            this.timestamp = (long) timestamp;
        }

        @Test
        public void shouldCorrectlySetMetricValue() {
            givenMetricWithTypedValue(this.name, this.value, this.timestamp);

            whenBuildPayload();

            thenPayloadContainsMetric(this.name, this.value.getValue());
        }

    }

    /*
     * Steps
     */

    public abstract static class Steps {

        private SparkplugBProtobufPayloadBuilder builder;
        private Payload payload;
        private Exception occurredException;

        /*
         * Given
         */

        void givenMetric(String name, Object value, DataType type, long timestamp) {
            this.builder = new SparkplugBProtobufPayloadBuilder().withMetric(name, value, type, timestamp);
        }

        <T> void givenMetricWithTypedValue(String name, TypedValue<T> value, long timestamp) {
            this.builder = new SparkplugBProtobufPayloadBuilder().withMetric(name, value, timestamp);
        }

        void givenBdSeq(long bdSeq, long timestamp) {
            this.builder = new SparkplugBProtobufPayloadBuilder().withBdSeq(bdSeq, timestamp);
        }

        void givenSeq(long seq) {
            this.builder = new SparkplugBProtobufPayloadBuilder().withSeq(seq);
        }

        void givenTimestamp(long timestamp) {
            this.builder = new SparkplugBProtobufPayloadBuilder().withTimestamp(timestamp);
        }

        /*
         * When
         */

        void whenBuildPayload() {
            this.payload = this.builder.buildPayload();
        }

        void whenGivenMetric(String name, Object value, DataType type, long timestamp) {
            try {
                givenMetric(name, value, type, timestamp);
            } catch (Exception e) {
                this.occurredException = e;
            }
        }

        /*
         * Then
         */

        void thenPayloadContainsMetric(String name, Object expectedValue) {
            Metric metric = this.payload.getMetricsList().stream().filter(m -> m.getName().equals(name))
                    .collect(Collectors.toList()).get(0);

            switch (metric.getValueCase()) {
            case BOOLEAN_VALUE:
                assertEquals((Boolean) expectedValue, metric.getBooleanValue());
                break;
            case BYTES_VALUE:
                assertTrue(Arrays.equals((byte[]) expectedValue, metric.getBytesValue().toByteArray()));
                break;
            case DOUBLE_VALUE:
                assertEquals((Double) expectedValue, (Double) metric.getDoubleValue());
                break;
            case FLOAT_VALUE:
                assertEquals((Float) expectedValue, (Float) metric.getFloatValue());
                break;
            case INT_VALUE:
                if (expectedValue instanceof Byte) {
                    assertEquals(Byte.toUnsignedInt((byte) expectedValue), metric.getIntValue());
                } else if (expectedValue instanceof Short) {
                    assertEquals(Short.toUnsignedInt((short) expectedValue), metric.getIntValue());
                } else {
                    assertEquals((int) expectedValue, metric.getIntValue());
                }
                break;
            case LONG_VALUE:
                if (expectedValue instanceof BigInteger) {
                    assertEquals(((BigInteger) expectedValue).longValue(), metric.getLongValue());
                } else if (expectedValue instanceof Date) {
                    assertEquals(((Date) expectedValue).getTime(), metric.getLongValue());
                } else {
                    assertEquals((Long) expectedValue, (Long) metric.getLongValue());
                }
                break;
            case STRING_VALUE:
                assertEquals((String) expectedValue, metric.getStringValue());
                break;
            case DATASET_VALUE:
            case EXTENSION_VALUE:
            case TEMPLATE_VALUE:
            case VALUE_NOT_SET:
            default:
                assertFalse("The set value is not supported", false);
                break;

            }
        }

        <E extends Exception> void thenExceptionOccurred(Class<E> expectedException) {
            assertNotNull(this.occurredException);
            assertEquals(expectedException.getName(), this.occurredException.getClass().getName());
        }

        void thenSeqEquals(long expectedSeq) {
            assertEquals(expectedSeq, this.payload.getSeq());
        }

        void thenTimestampEquals(long expectedTimestamp) {
            assertEquals(expectedTimestamp, this.payload.getTimestamp());
        }
    }


}

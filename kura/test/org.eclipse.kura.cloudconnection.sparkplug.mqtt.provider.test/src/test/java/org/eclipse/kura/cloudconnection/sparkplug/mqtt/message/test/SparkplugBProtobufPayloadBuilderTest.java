/*******************************************************************************
 * Copyright (c) 2023, 2024 Eurotech and/or its affiliates and others
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.kura.cloudconnection.sparkplug.mqtt.message.SparkplugBProtobufPayloadBuilder;
import org.eclipse.tahu.protobuf.SparkplugBProto.DataType;
import org.eclipse.tahu.protobuf.SparkplugBProto.Payload;
import org.eclipse.tahu.protobuf.SparkplugBProto.Payload.Metric;
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
    public static class TypeMapperTest extends Steps {

        @Parameters
        public static Collection<TypeMapperCase> parameters() {
            long timestamp = new Date().getTime();

            List<TypeMapperCase> supportedTypes = Arrays.asList(
                    new TypeMapperCase("metric.boolean", false, timestamp, DataType.Boolean),
                    new TypeMapperCase("metric.bytes", "somebytes".getBytes(), timestamp, DataType.Bytes),
                    new TypeMapperCase("metric.double", (double) 11.2, timestamp, DataType.Double),
                    new TypeMapperCase("metric.float", (float) 99.1, timestamp, DataType.Float),
                    new TypeMapperCase("metric.int8", 1, timestamp, DataType.Int8),
                    new TypeMapperCase("metric.int16", 16, timestamp, DataType.Int16),
                    new TypeMapperCase("metric.int32", 32, timestamp, DataType.Int32),
                    new TypeMapperCase("metric.int64", 64L, timestamp, DataType.Int64),
                    new TypeMapperCase("metric.uint8", 8, timestamp, DataType.UInt8),
                    new TypeMapperCase("metric.uint16", 16, timestamp, DataType.UInt16),
                    new TypeMapperCase("metric.uint32", 322L, timestamp, DataType.UInt32),
                    new TypeMapperCase("metric.uint64", 9999L, timestamp, DataType.UInt64),
                    new TypeMapperCase("metric.string", "a string", timestamp, DataType.String),
                    new TypeMapperCase("metric.text", "a text", timestamp, DataType.Text),
                    new TypeMapperCase("metric.uuid", "a uuid", timestamp, DataType.UUID));

            Object randomData = new Object();
            Exception ex = new UnsupportedOperationException();
            List<TypeMapperCase> unsupportedTypes = Arrays.asList(
                    new TypeMapperCase("metric.dataset", randomData, timestamp, DataType.DataSet, ex),
                    new TypeMapperCase("metric.template", randomData, timestamp, DataType.Template, ex),
                    new TypeMapperCase("metric.propertyset", randomData, timestamp, DataType.PropertySet, ex),
                    new TypeMapperCase("metric.propertysetlist", randomData, timestamp, DataType.PropertySetList,
                            ex),
                    new TypeMapperCase("metric.file", randomData, timestamp, DataType.File, ex),
                    new TypeMapperCase("metric.booleanarray", randomData, timestamp, DataType.BooleanArray, ex),
                    new TypeMapperCase("metric.datetimearray", randomData, timestamp, DataType.DateTimeArray, ex),
                    new TypeMapperCase("metric.unit8array", randomData, timestamp, DataType.UInt8Array, ex),
                    new TypeMapperCase("metric.uint64array", randomData, timestamp, DataType.UInt64Array, ex),
                    new TypeMapperCase("metric.uint32array", randomData, timestamp, DataType.UInt32Array, ex),
                    new TypeMapperCase("metric.uint16array", randomData, timestamp, DataType.UInt16Array, ex),
                    new TypeMapperCase("metric.stringarray", randomData, timestamp, DataType.StringArray, ex),
                    new TypeMapperCase("metric.int8array", randomData, timestamp, DataType.Int8Array, ex),
                    new TypeMapperCase("metric.int64array", randomData, timestamp, DataType.Int64Array, ex),
                    new TypeMapperCase("metric.int32array", randomData, timestamp, DataType.Int32Array, ex),
                    new TypeMapperCase("metric.int16array", randomData, timestamp, DataType.Int16Array, ex),
                    new TypeMapperCase("metric.floatarray", randomData, timestamp, DataType.FloatArray, ex),
                    new TypeMapperCase("metric.doubleArray", randomData, timestamp, DataType.DoubleArray, ex),
                    new TypeMapperCase("metric.unknown", randomData, timestamp, DataType.Unknown, ex));

            return Stream.concat(supportedTypes.stream(), unsupportedTypes.stream()).collect(Collectors.toList());
        }

        private TypeMapperCase testCase;

        public TypeMapperTest(TypeMapperCase testCase) {
            this.testCase = testCase;
        }

        @Test
        public void shouldBuildMetric() {
            givenMetric(this.testCase.getName(), this.testCase.getValue(), this.testCase.getTimestamp());
            
            whenBuildPayload();
            
            thenPayloadContainsMetric(this.testCase.getName(), this.testCase.getValue(), this.testCase.getTimestamp(),
                    this.testCase.getExpectedDataType(), this.testCase.getExpectedException());
        }
    }

    public static class SparkplugPropertiesTest extends Steps {

        @Test
        public void shouldReturnCorrectBdSeq() {
            givenBdSeq(12L, 120L);
            
            whenBuildPayload();
            
            thenPayloadContainsMetric("bdSeq", 12L, 120L, DataType.Int64, Optional.empty());
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

        @Test
        public void shouldReturnCorrectBody() {
            givenBody("example.body".getBytes());

            whenBuildPayload();

            thenBodyEquals("example.body".getBytes());
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

        void givenMetric(String name, Object value, long timestamp) {
            try {
                this.builder = new SparkplugBProtobufPayloadBuilder().withMetric(name, value, timestamp);
            } catch (Exception e) {
                this.occurredException = e;
            }
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

        void givenBody(byte[] body) {
            this.builder = new SparkplugBProtobufPayloadBuilder().withBody(body);
        }

        /*
         * When
         */

        void whenBuildPayload() {
            if (Objects.isNull(this.occurredException)) {
                this.payload = this.builder.buildPayload();
            }
        }

        /*
         * Then
         */

        void thenPayloadContainsMetric(String expectedName, Object expectedValue, long expectedTimestamp,
                DataType expectedDataType, Optional<Exception> expectedException) {
            if (expectedException.isPresent()) {
                thenExceptionOccurred(expectedException.get().getClass());
            } else {
                Metric metric = this.payload.getMetricsList().stream().filter(m -> m.getName().equals(expectedName))
                        .collect(Collectors.toList()).get(0);

                Object actualValue = null;

                switch (expectedDataType) {
                case Boolean:
                    actualValue = metric.getBooleanValue();
                    break;
                case Bytes:
                    actualValue = metric.getBytesValue().toByteArray();
                    break;
                case DateTime:
                case Int64:
                case UInt32:
                case UInt64:
                    actualValue = metric.getLongValue();
                    break;
                case Double:
                    actualValue = metric.getDoubleValue();
                    break;
                case Int8:
                case Int16:
                case Int32:
                case UInt8:
                case UInt16:
                    actualValue = metric.getIntValue();
                    break;
                case Float:
                    actualValue = metric.getFloatValue();
                    break;
                case String:
                case Text:
                case UUID:
                    actualValue = metric.getStringValue();
                    break;
                case File:
                case DataSet:
                case DateTimeArray:
                case Int16Array:
                case Int32Array:
                case Int64Array:
                case Int8Array:
                case PropertySet:
                case PropertySetList:
                case StringArray:
                case Template:
                case UInt16Array:
                case FloatArray:
                case UInt32Array:
                case UInt64Array:
                case DoubleArray:
                case UInt8Array:
                case BooleanArray:
                case Unknown:
                default:
                    break;
                }

                assertEquals(expectedName, metric.getName());
                assertEquals(expectedTimestamp, metric.getTimestamp());

                if (actualValue instanceof byte[]) {
                    assertTrue(Arrays.equals((byte[]) expectedValue, (byte[]) actualValue));
                } else {
                    assertEquals(expectedValue, actualValue);
                }
            }
        }

        <E extends Exception> void thenExceptionOccurred(Class<E> expectedException) {
            assertNotNull("No exception thrown", this.occurredException);
            assertEquals(expectedException.getName(), this.occurredException.getClass().getName());
        }

        void thenSeqEquals(long expectedSeq) {
            assertEquals(expectedSeq, this.payload.getSeq());
        }

        void thenTimestampEquals(long expectedTimestamp) {
            assertEquals(expectedTimestamp, this.payload.getTimestamp());
        }

        void thenBodyEquals(byte[] expectedBody) {
            assertTrue(Arrays.equals(expectedBody, this.payload.getBody().toByteArray()));
        }
    }


}

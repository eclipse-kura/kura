/*******************************************************************************
 * Copyright (c) 2022 Eurotech and/or its affiliates and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Eurotech
 ******************************************************************************/

package org.eclipse.kura.wire.ai.component.provider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.KuraIOException;
import org.eclipse.kura.ai.inference.Tensor;
import org.eclipse.kura.ai.inference.TensorDescriptor;
import org.eclipse.kura.type.BooleanValue;
import org.eclipse.kura.type.ByteArrayValue;
import org.eclipse.kura.type.DoubleValue;
import org.eclipse.kura.type.FloatValue;
import org.eclipse.kura.type.IntegerValue;
import org.eclipse.kura.type.LongValue;
import org.eclipse.kura.type.StringValue;
import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.wire.WireRecord;
import org.junit.Before;
import org.junit.Test;

public class TensorListAdapterTest {

    private Map<String, TypedValue<?>> wireRecordProperties;
    private WireRecord inputRecord;

    private List<Tensor> inputTensors;
    private List<TensorDescriptor> inputDescriptors;

    private List<Tensor> outputTensors;
    private List<WireRecord> outputRecords;

    private boolean exceptionOccurred = false;

    /*
     * Scenarios
     */
    @Test
    public void adapterShouldWorkWithBooleanWiredRecord() {
        givenWireRecordPropWith("INPUT0", new BooleanValue(true));
        givenWireRecord();
        givenTensorDescriptorWith("INPUT0", "BOOL", Arrays.asList(1L, 1L));

        whenTensorListAdapterConvertsFromWireRecord();

        thenNoExceptionOccurred();
        thenResultingTensorListIsSize(1);
        thenResultingNamedTensorIsEqualTo("INPUT0", Boolean.class, Arrays.asList(true));
    }

    @Test
    public void adapterShouldWorkWithByteArrayWiredRecord() {
        givenWireRecordPropWith("INPUT0", new ByteArrayValue(new byte[] { 1, 2, 3, 4 }));
        givenWireRecord();
        givenTensorDescriptorWith("INPUT0", "BYTES", Arrays.asList(1L, 1L));

        whenTensorListAdapterConvertsFromWireRecord();

        thenNoExceptionOccurred();
        thenResultingTensorListIsSize(1);
        thenResultingNamedTensorIsEqualTo("INPUT0", Byte.class, Arrays.asList((byte) 1, (byte) 2, (byte) 3, (byte) 4));
    }

    @Test
    public void adapterShouldWorkWithFloatWiredRecord() {
        givenWireRecordPropWith("INPUT0", new FloatValue(1.0F));
        givenWireRecord();
        givenTensorDescriptorWith("INPUT0", "FP32", Arrays.asList(1L, 1L));

        whenTensorListAdapterConvertsFromWireRecord();

        thenNoExceptionOccurred();
        thenResultingTensorListIsSize(1);
        thenResultingNamedTensorIsEqualTo("INPUT0", Float.class, Arrays.asList(1.0F));
    }

    @Test
    public void adapterShouldWorkWithDoubleWiredRecord() {
        givenWireRecordPropWith("INPUT0", new DoubleValue(3.0D));
        givenWireRecord();
        givenTensorDescriptorWith("INPUT0", "FP32", Arrays.asList(1L, 1L));

        whenTensorListAdapterConvertsFromWireRecord();

        thenNoExceptionOccurred();
        thenResultingTensorListIsSize(1);
        thenResultingNamedTensorIsEqualTo("INPUT0", Double.class, Arrays.asList(3.0D));
    }

    @Test
    public void adapterShouldWorkWithIntegerWiredRecord() {
        givenWireRecordPropWith("INPUT0", new IntegerValue(6));
        givenWireRecord();
        givenTensorDescriptorWith("INPUT0", "INT32", Arrays.asList(1L, 1L));

        whenTensorListAdapterConvertsFromWireRecord();

        thenNoExceptionOccurred();
        thenResultingTensorListIsSize(1);
        thenResultingNamedTensorIsEqualTo("INPUT0", Integer.class, Arrays.asList(6));
    }

    @Test
    public void adapterShouldWorkWithLongWiredRecord() {
        givenWireRecordPropWith("INPUT0", new LongValue(6555L));
        givenWireRecord();
        givenTensorDescriptorWith("INPUT0", "INT32", Arrays.asList(1L, 1L));

        whenTensorListAdapterConvertsFromWireRecord();

        thenNoExceptionOccurred();
        thenResultingTensorListIsSize(1);
        thenResultingNamedTensorIsEqualTo("INPUT0", Long.class, Arrays.asList(6555L));
    }

    @Test
    public void adapterShouldWorkWithStringWiredRecord() {
        givenWireRecordPropWith("INPUT0", new StringValue("This is a test"));
        givenWireRecord();
        givenTensorDescriptorWith("INPUT0", "STRING", Arrays.asList(1L, 1L));

        whenTensorListAdapterConvertsFromWireRecord();

        thenNoExceptionOccurred();
        thenResultingTensorListIsSize(1);
        thenResultingNamedTensorIsEqualTo("INPUT0", String.class, Arrays.asList("This is a test"));
    }

    @Test
    public void adapterShouldThrowIfChannelAndTensorNamesDontMatch() {
        givenWireRecordPropWith("INPUT0", new BooleanValue(true));
        givenWireRecord();
        givenTensorDescriptorWith("INPUT1", "BOOL", Arrays.asList(1L, 1L));

        whenTensorListAdapterConvertsFromWireRecord();

        thenExceptionOccurred();
    }

    @Test
    public void adapterShouldWorkWithMultipleDifferentTypeWiredRecord() {
        givenWireRecordPropWith("INPUT0", new FloatValue(1.0F));
        givenWireRecordPropWith("INPUT1", new BooleanValue(true));
        givenWireRecordPropWith("INPUT2", new IntegerValue(64));
        givenWireRecordPropWith("INPUT3", new LongValue(65535));
        givenWireRecord();
        givenTensorDescriptorWith("INPUT0", "FP32", Arrays.asList(1L, 1L));
        givenTensorDescriptorWith("INPUT1", "BOOL", Arrays.asList(1L, 1L));
        givenTensorDescriptorWith("INPUT2", "INT32", Arrays.asList(1L, 1L));
        givenTensorDescriptorWith("INPUT3", "INT32", Arrays.asList(1L, 1L));

        whenTensorListAdapterConvertsFromWireRecord();

        thenNoExceptionOccurred();
        thenResultingTensorListIsSize(4);
        thenResultingNamedTensorIsEqualTo("INPUT0", Float.class, Arrays.asList(1.0F));
        thenResultingNamedTensorIsEqualTo("INPUT1", Boolean.class, Arrays.asList(true));
        thenResultingNamedTensorIsEqualTo("INPUT2", Integer.class, Arrays.asList(64));
        thenResultingNamedTensorIsEqualTo("INPUT3", Long.class, Arrays.asList(65535L));
    }

    @Test
    public void adapterShouldWorkWithBooleanTensor() {
        givenTensorDescriptorWith("OUTPUT0", "BOOL", Arrays.asList(1L, 1L));
        givenTensorWith("OUTPUT0", "BOOL", Arrays.asList(1L, 1L), Boolean.class, Arrays.asList(true));

        whenTensorListAdapterConvertsFromTensorList();

        thenNoExceptionOccurred();
        thenResultingWireRecordIsSize(1);
        thenAllWireRecordsHaveSingleProperty();
        thenResultingNamedWireRecordPropertiesAreEqualTo("OUTPUT0", new BooleanValue(true));
    }

    @Test
    public void adapterShouldWorkWithByteArrayTensor() {
        givenTensorDescriptorWith("OUTPUT0", "BYTES", Arrays.asList(1L, 1L));
        givenTensorWith("OUTPUT0", "BYTES", Arrays.asList(1L, 1L), Byte.class,
                Arrays.asList((byte) 1, (byte) 2, (byte) 3));

        whenTensorListAdapterConvertsFromTensorList();

        thenNoExceptionOccurred();
        thenResultingWireRecordIsSize(1);
        thenAllWireRecordsHaveSingleProperty();
        thenResultingNamedWireRecordPropertiesAreEqualTo("OUTPUT0", new ByteArrayValue(new byte[] { 1, 2, 3 }));
    }

    @Test
    public void adapterShouldWorkWithFloatTensor() {
        givenTensorDescriptorWith("OUTPUT0", "FP32", Arrays.asList(1L, 1L));
        givenTensorWith("OUTPUT0", "FP32", Arrays.asList(1L, 1L), Float.class, Arrays.asList(3.2F));

        whenTensorListAdapterConvertsFromTensorList();

        thenNoExceptionOccurred();
        thenResultingWireRecordIsSize(1);
        thenAllWireRecordsHaveSingleProperty();
        thenResultingNamedWireRecordPropertiesAreEqualTo("OUTPUT0", new FloatValue(3.2F));
    }

    @Test
    public void adapterShouldWorkWithDoubleTensor() {
        givenTensorDescriptorWith("OUTPUT0", "FP32", Arrays.asList(1L, 1L));
        givenTensorWith("OUTPUT0", "FP32", Arrays.asList(1L, 1L), Double.class, Arrays.asList(5.464D));

        whenTensorListAdapterConvertsFromTensorList();

        thenNoExceptionOccurred();
        thenResultingWireRecordIsSize(1);
        thenAllWireRecordsHaveSingleProperty();
        thenResultingNamedWireRecordPropertiesAreEqualTo("OUTPUT0", new DoubleValue(5.464D));
    }

    @Test
    public void adapterShouldWorkWithIntegerTensor() {
        givenTensorDescriptorWith("OUTPUT0", "INT32", Arrays.asList(1L, 1L));
        givenTensorWith("OUTPUT0", "INT32", Arrays.asList(1L, 1L), Integer.class, Arrays.asList(42));

        whenTensorListAdapterConvertsFromTensorList();

        thenNoExceptionOccurred();
        thenResultingWireRecordIsSize(1);
        thenAllWireRecordsHaveSingleProperty();
        thenResultingNamedWireRecordPropertiesAreEqualTo("OUTPUT0", new IntegerValue(42));
    }

    @Test
    public void adapterShouldWorkWithLongTensor() {
        givenTensorDescriptorWith("OUTPUT0", "INT32", Arrays.asList(1L, 1L));
        givenTensorWith("OUTPUT0", "INT32", Arrays.asList(1L, 1L), Long.class, Arrays.asList(36L));

        whenTensorListAdapterConvertsFromTensorList();

        thenNoExceptionOccurred();
        thenResultingWireRecordIsSize(1);
        thenAllWireRecordsHaveSingleProperty();
        thenResultingNamedWireRecordPropertiesAreEqualTo("OUTPUT0", new LongValue(36L));
    }

    @Test
    public void adapterShouldWorkWithStringTensor() {
        givenTensorDescriptorWith("OUTPUT0", "STRING", Arrays.asList(1L, 1L));
        givenTensorWith("OUTPUT0", "STRING", Arrays.asList(1L, 1L), String.class,
                Arrays.asList("This is a test string"));

        whenTensorListAdapterConvertsFromTensorList();

        thenNoExceptionOccurred();
        thenResultingWireRecordIsSize(1);
        thenAllWireRecordsHaveSingleProperty();
        thenResultingNamedWireRecordPropertiesAreEqualTo("OUTPUT0", new StringValue("This is a test string"));
    }

    @Test
    public void adapterShouldWorkWithMultipleDifferentTypeTensor() {
        givenTensorDescriptorWith("OUTPUT0", "FP32", Arrays.asList(1L, 1L));
        givenTensorDescriptorWith("OUTPUT1", "INT32", Arrays.asList(1L, 1L));
        givenTensorDescriptorWith("OUTPUT2", "STRING", Arrays.asList(1L, 1L));
        givenTensorDescriptorWith("OUTPUT3", "INT32", Arrays.asList(1L, 1L));

        givenTensorWith("OUTPUT0", "FP32", Arrays.asList(1L, 1L), Float.class, Arrays.asList(6.9F));
        givenTensorWith("OUTPUT1", "INT32", Arrays.asList(1L, 1L), Integer.class, Arrays.asList(100));
        givenTensorWith("OUTPUT2", "STRING", Arrays.asList(1L, 1L), String.class,
                Arrays.asList("May the force be with you"));
        givenTensorWith("OUTPUT3", "INT32", Arrays.asList(1L, 1L), Long.class, Arrays.asList(254678L));

        whenTensorListAdapterConvertsFromTensorList();

        thenNoExceptionOccurred();
        thenResultingWireRecordIsSize(4);
        thenAllWireRecordsHaveSingleProperty();
        thenResultingNamedWireRecordPropertiesAreEqualTo("OUTPUT0", new FloatValue(6.9F));
        thenResultingNamedWireRecordPropertiesAreEqualTo("OUTPUT1", new IntegerValue(100));
        thenResultingNamedWireRecordPropertiesAreEqualTo("OUTPUT2", new StringValue("May the force be with you"));
        thenResultingNamedWireRecordPropertiesAreEqualTo("OUTPUT3", new LongValue(254678L));
    }

    public void adapterShouldThrowWithUnsupportedTensorShape() {
        givenTensorDescriptorWith("OUTPUT0", "FP32", Arrays.asList(1L, 5L));

        givenTensorWith("OUTPUT0", "FP32", Arrays.asList(1L, 5L), Float.class,
                Arrays.asList(6.9F, 1.0F, 1.0F, 1.0F, 1.0F));

        whenTensorListAdapterConvertsFromTensorList();

        thenExceptionOccurred();
    }

    /*
     * Given
     */
    private void givenWireRecordPropWith(String name, TypedValue<?> value) {
        this.wireRecordProperties.put(name, value);
    }

    private void givenWireRecord() {
        this.inputRecord = new WireRecord(this.wireRecordProperties);
    }

    private void givenTensorDescriptorWith(String name, String type, List<Long> shape) {
        Optional<String> format = Optional.empty();
        Map<String, Object> parameters = new HashMap<>();

        TensorDescriptor descriptor = new TensorDescriptor(name, type, format, shape, parameters);

        this.inputDescriptors.add(descriptor);
    }

    private <T> void givenTensorWith(String name, String type, List<Long> shape, Class<T> classType, List<T> data) {
        Optional<String> format = Optional.empty();
        Map<String, Object> parameters = new HashMap<>();

        TensorDescriptor descriptor = new TensorDescriptor(name, type, format, shape, parameters);

        Tensor tensor = new Tensor(classType, descriptor, data);

        this.inputTensors.add(tensor);
    }

    /*
     * When
     */
    private void whenTensorListAdapterConvertsFromWireRecord() {
        try {
            this.outputTensors = TensorListAdapter.givenDescriptors(this.inputDescriptors).fromWireRecord(inputRecord);
        } catch (KuraException e) {
            e.printStackTrace();
            this.exceptionOccurred = true;
        }
    }

    private void whenTensorListAdapterConvertsFromTensorList() {
        try {
            this.outputRecords = TensorListAdapter.givenDescriptors(this.inputDescriptors).fromTensorList(inputTensors);
        } catch (KuraIOException e) {
            e.printStackTrace();
            this.exceptionOccurred = true;
        }
    }

    /*
     * Then
     */
    private void thenNoExceptionOccurred() {
        assertFalse(this.exceptionOccurred);
    }

    private void thenExceptionOccurred() {
        assertTrue(this.exceptionOccurred);
    }

    private void thenResultingTensorListIsSize(int size) {
        assertFalse(this.outputTensors.isEmpty());
        assertEquals(size, this.outputTensors.size());
    }

    private void thenResultingWireRecordIsSize(int size) {
        assertFalse(this.outputRecords.isEmpty());
        assertEquals(size, this.outputRecords.size());
    }

    private void thenAllWireRecordsHaveSingleProperty() {
        assertFalse(this.outputRecords.isEmpty());

        for (WireRecord record : outputRecords) {
            Map<String, TypedValue<?>> properties = record.getProperties();

            assertEquals(1, properties.size());
        }
    }

    private <T> void thenResultingNamedTensorIsEqualTo(String name, Class<T> type, List<T> expectedData) {
        Tensor tensor = findTensorByName(name, outputTensors);

        assertNotNull(tensor);

        Optional<List<T>> data = tensor.getData(type);

        assertTrue(data.isPresent());
        assertEquals(expectedData, data.get());
    }

    private Tensor findTensorByName(String name, List<Tensor> tensorList) {
        for (Tensor currTensor : tensorList) {
            String currTensorName = currTensor.getDescriptor().getName();

            if (currTensorName.equals(name)) {
                return currTensor;
            }
        }

        return null;
    }

    private void thenResultingNamedWireRecordPropertiesAreEqualTo(String channelName, TypedValue<?> data) {
        TypedValue<?> value = findWireRecordPropByChannelName(channelName, this.outputRecords);

        assertNotNull(value);
        assertEquals(data, value);
    }

    private TypedValue<?> findWireRecordPropByChannelName(String channelName, List<WireRecord> records) {
        for (WireRecord record : records) {
            Map<String, TypedValue<?>> properties = record.getProperties();

            if (properties.containsKey(channelName)) {
                return properties.get(channelName);
            }
        }

        return null;
    }

    /*
     * Utils
     */
    @Before
    public void cleanup() {
        this.wireRecordProperties = new HashMap<String, TypedValue<?>>();
        this.inputDescriptors = new ArrayList<>();
        this.inputTensors = new ArrayList<>();
    }

}

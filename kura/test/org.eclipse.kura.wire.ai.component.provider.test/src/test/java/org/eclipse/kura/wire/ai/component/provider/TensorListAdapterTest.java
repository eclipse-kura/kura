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

import org.eclipse.kura.KuraIOException;
import org.eclipse.kura.ai.inference.Tensor;
import org.eclipse.kura.ai.inference.TensorDescriptor;
import org.eclipse.kura.type.BooleanValue;
import org.eclipse.kura.type.DoubleValue;
import org.eclipse.kura.type.FloatValue;
import org.eclipse.kura.type.IntegerValue;
import org.eclipse.kura.type.LongValue;
import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.wire.WireRecord;
import org.junit.Before;
import org.junit.Test;

public class TensorListAdapterTest {

    private TensorListAdapter adapterInstance = new TensorListAdapter();

    private Map<String, TypedValue<?>> wireRecordProperties;
    private WireRecord inputRecord;

    private List<TensorDescriptor> inputDescriptors;

    private List<Tensor> outputTensors;

    private boolean exceptionOccurred = false;

    /*
     * Scenarios
     */
    @Test
    public void adapterShouldWorkWithBooleanScalar() {
        givenWireRecordPropWith("INPUT0", new BooleanValue(true));
        givenWireRecord();

        givenTensorDescriptorWith("INPUT0", "BOOL", Arrays.asList(1L, 1L));
        givenDescriptorToTensorListAdapter();

        whenTensorListAdapterConvertsFromWireRecord();

        thenNoExceptionOccurred();
        thenResultingScalarTensorIsIstanceOf(Boolean.class);
        thenResultingScalarTensorIsEqualTo(Boolean.class, new Boolean(true));
    }

    @Test
    public void adapterShouldWorkWithByteArray() {
        // TODO
        assertTrue(true);
    }

    @Test
    public void adapterShouldWorkWithFloatScalar() {
        givenWireRecordPropWith("INPUT0", new FloatValue(1.0F));
        givenWireRecord();

        givenTensorDescriptorWith("INPUT0", "FP32", Arrays.asList(1L, 1L));
        givenDescriptorToTensorListAdapter();

        whenTensorListAdapterConvertsFromWireRecord();

        thenNoExceptionOccurred();
        thenResultingScalarTensorIsIstanceOf(Float.class);
        thenResultingScalarTensorIsEqualTo(Float.class, new Float(1.0F));
    }

    @Test
    public void adapterShouldWorkWithDoubleScalar() {
        givenWireRecordPropWith("INPUT0", new DoubleValue(3.0F));
        givenWireRecord();

        givenTensorDescriptorWith("INPUT0", "FP32", Arrays.asList(1L, 1L));
        givenDescriptorToTensorListAdapter();

        whenTensorListAdapterConvertsFromWireRecord();

        thenNoExceptionOccurred();
        thenResultingScalarTensorIsIstanceOf(Double.class);
        thenResultingScalarTensorIsEqualTo(Double.class, new Double(3.0F));
    }

    @Test
    public void adapterShouldWorkWithIntegerScalar() {
        givenWireRecordPropWith("INPUT0", new IntegerValue(6));
        givenWireRecord();

        givenTensorDescriptorWith("INPUT0", "INT32", Arrays.asList(1L, 1L));
        givenDescriptorToTensorListAdapter();

        whenTensorListAdapterConvertsFromWireRecord();

        thenNoExceptionOccurred();
        thenResultingScalarTensorIsIstanceOf(Integer.class);
        thenResultingScalarTensorIsEqualTo(Integer.class, new Integer(6));
    }

    @Test
    public void adapterShouldWorkWithLongScalar() {
        givenWireRecordPropWith("INPUT0", new LongValue(6555));
        givenWireRecord();

        givenTensorDescriptorWith("INPUT0", "INT32", Arrays.asList(1L, 1L));
        givenDescriptorToTensorListAdapter();

        whenTensorListAdapterConvertsFromWireRecord();

        thenNoExceptionOccurred();
        thenResultingScalarTensorIsIstanceOf(Long.class);
        thenResultingScalarTensorIsEqualTo(Long.class, new Long(6555));
    }

    @Test
    public void adapterShouldWorkWithStringScalar() {
        // TODO
        assertTrue(true);
    }

    @Test
    public void adapterShouldThrowIfChannelAndTensorNamesDontMatch() {
        givenWireRecordPropWith("INPUT0", new BooleanValue(true));
        givenWireRecord();

        givenTensorDescriptorWith("INPUT1", "BOOL", Arrays.asList(1L, 1L));
        givenDescriptorToTensorListAdapter();

        whenTensorListAdapterConvertsFromWireRecord();

        thenExceptionOccurred();
    }

    @Test
    public void adapterShouldThrowIfChannelAndTensorShapeDontMatch() {
        givenWireRecordPropWith("INPUT0", new BooleanValue(true));
        givenWireRecord();

        givenTensorDescriptorWith("INPUT0", "BOOL", Arrays.asList(5L, 5L));
        givenDescriptorToTensorListAdapter();

        whenTensorListAdapterConvertsFromWireRecord();

        thenExceptionOccurred();
    }

    @Test
    public void adapterShouldWorkWithMultipleBoolean() {
        givenWireRecordPropWith("INPUT0", new BooleanValue(true));
        givenWireRecordPropWith("INPUT1", new BooleanValue(false));
        givenWireRecordPropWith("INPUT3", new BooleanValue(true));
        givenWireRecord();

        givenTensorDescriptorWith("INPUT0", "BOOL", Arrays.asList(1L, 1L));
        givenTensorDescriptorWith("INPUT1", "BOOL", Arrays.asList(1L, 1L));
        givenTensorDescriptorWith("INPUT3", "BOOL", Arrays.asList(1L, 1L));
        givenDescriptorToTensorListAdapter();

        whenTensorListAdapterConvertsFromWireRecord();

        thenNoExceptionOccurred();
        thenResultingTensorIsSize(3);
        thenAllResultingTensorAreIstanceOf(Boolean.class);
        thenResultingNamedScalarTensorIsEqualTo("INPUT0", Boolean.class, new Boolean(true));
        thenResultingNamedScalarTensorIsEqualTo("INPUT1", Boolean.class, new Boolean(false));
        thenResultingNamedScalarTensorIsEqualTo("INPUT3", Boolean.class, new Boolean(true));
    }

    @Test
    public void adapterShouldWorkWithMultipleByteArrays() {
        // TODO
        assertTrue(true);
    }

    @Test
    public void adapterShouldWorkWithMultipleFloat() {
        givenWireRecordPropWith("INPUT0", new FloatValue(1.0F));
        givenWireRecordPropWith("INPUT1", new FloatValue(2.0F));
        givenWireRecordPropWith("INPUT2", new FloatValue(3.0F));
        givenWireRecordPropWith("INPUT3", new FloatValue(4.0F));
        givenWireRecord();

        givenTensorDescriptorWith("INPUT0", "FP32", Arrays.asList(1L, 1L));
        givenTensorDescriptorWith("INPUT1", "FP32", Arrays.asList(1L, 1L));
        givenTensorDescriptorWith("INPUT2", "FP32", Arrays.asList(1L, 1L));
        givenTensorDescriptorWith("INPUT3", "FP32", Arrays.asList(1L, 1L));
        givenDescriptorToTensorListAdapter();

        whenTensorListAdapterConvertsFromWireRecord();

        thenNoExceptionOccurred();
        thenResultingTensorIsSize(4);
        thenAllResultingTensorAreIstanceOf(Float.class);
        thenResultingNamedScalarTensorIsEqualTo("INPUT0", Float.class, new Float(1.0F));
        thenResultingNamedScalarTensorIsEqualTo("INPUT1", Float.class, new Float(2.0F));
        thenResultingNamedScalarTensorIsEqualTo("INPUT2", Float.class, new Float(3.0F));
        thenResultingNamedScalarTensorIsEqualTo("INPUT3", Float.class, new Float(4.0F));
    }

    @Test
    public void adapterShouldWorkWithMultipleDouble() {
        givenWireRecordPropWith("INPUT2", new DoubleValue(3.0F));
        givenWireRecordPropWith("INPUT3", new DoubleValue(4.0F));
        givenWireRecord();

        givenTensorDescriptorWith("INPUT2", "FP32", Arrays.asList(1L, 1L));
        givenTensorDescriptorWith("INPUT3", "FP32", Arrays.asList(1L, 1L));
        givenDescriptorToTensorListAdapter();

        whenTensorListAdapterConvertsFromWireRecord();

        thenNoExceptionOccurred();
        thenResultingTensorIsSize(2);
        thenAllResultingTensorAreIstanceOf(Double.class);
        thenResultingNamedScalarTensorIsEqualTo("INPUT2", Double.class, new Double(3.0F));
        thenResultingNamedScalarTensorIsEqualTo("INPUT3", Double.class, new Double(4.0F));
    }

    @Test
    public void adapterShouldWorkWithMultipleInteger() {
        givenWireRecordPropWith("INPUT2", new IntegerValue(30));
        givenWireRecordPropWith("INPUT3", new IntegerValue(42));
        givenWireRecord();

        givenTensorDescriptorWith("INPUT2", "INT32", Arrays.asList(1L, 1L));
        givenTensorDescriptorWith("INPUT3", "INT32", Arrays.asList(1L, 1L));
        givenDescriptorToTensorListAdapter();

        whenTensorListAdapterConvertsFromWireRecord();

        thenNoExceptionOccurred();
        thenResultingTensorIsSize(2);
        thenAllResultingTensorAreIstanceOf(Integer.class);
        thenResultingNamedScalarTensorIsEqualTo("INPUT2", Integer.class, new Integer(30));
        thenResultingNamedScalarTensorIsEqualTo("INPUT3", Integer.class, new Integer(42));
    }

    @Test
    public void adapterShouldWorkWithMultipleLong() {
        givenWireRecordPropWith("INPUT2", new LongValue(30));
        givenWireRecordPropWith("INPUT3", new LongValue(42));
        givenWireRecord();

        givenTensorDescriptorWith("INPUT2", "INT32", Arrays.asList(1L, 1L));
        givenTensorDescriptorWith("INPUT3", "INT32", Arrays.asList(1L, 1L));
        givenDescriptorToTensorListAdapter();

        whenTensorListAdapterConvertsFromWireRecord();

        thenNoExceptionOccurred();
        thenResultingTensorIsSize(2);
        thenAllResultingTensorAreIstanceOf(Long.class);
        thenResultingNamedScalarTensorIsEqualTo("INPUT2", Long.class, new Long(30));
        thenResultingNamedScalarTensorIsEqualTo("INPUT3", Long.class, new Long(42));
    }

    @Test
    public void adapterShouldWorkWithMultipleStrings() {
        // TODO
        assertTrue(true);
    }

    @Test
    public void adapterShouldWorkWithMultipleInputWithDifferentType() {
        givenWireRecordPropWith("INPUT0", new FloatValue(1.0F));
        givenWireRecordPropWith("INPUT1", new BooleanValue(true));
        givenWireRecordPropWith("INPUT2", new IntegerValue(64));
        givenWireRecordPropWith("INPUT3", new LongValue(65535));
        givenWireRecord();

        givenTensorDescriptorWith("INPUT0", "FP32", Arrays.asList(1L, 1L));
        givenTensorDescriptorWith("INPUT1", "BOOL", Arrays.asList(1L, 1L));
        givenTensorDescriptorWith("INPUT2", "INT32", Arrays.asList(1L, 1L));
        givenTensorDescriptorWith("INPUT3", "INT32", Arrays.asList(1L, 1L));
        givenDescriptorToTensorListAdapter();

        whenTensorListAdapterConvertsFromWireRecord();

        thenNoExceptionOccurred();
        thenResultingTensorIsSize(4);
        thenResultingNamedScalarTensorIsEqualTo("INPUT0", Float.class, new Float(1.0F));
        thenResultingNamedScalarTensorIsEqualTo("INPUT1", Boolean.class, new Boolean(true));
        thenResultingNamedScalarTensorIsEqualTo("INPUT2", Integer.class, new Integer(64));
        thenResultingNamedScalarTensorIsEqualTo("INPUT3", Long.class, new Long(65535));
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

    private void givenDescriptorToTensorListAdapter() {
        TensorListAdapter.givenDescriptors(this.inputDescriptors);
    }

    /*
     * When
     */
    private void whenTensorListAdapterConvertsFromWireRecord() {
        try {
            this.outputTensors = adapterInstance.fromWireRecord(inputRecord);
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

    private <T> void thenResultingTensorIsSize(int size) {
        assertFalse(this.outputTensors.isEmpty());
        assertEquals(size, this.outputTensors.size());
    }

    private <T> void thenResultingScalarTensorIsIstanceOf(Class<T> type) {
        assertEquals(1, this.outputTensors.size());
        Optional<List<T>> data = this.outputTensors.get(0).getData(type);

        assertTrue(data.isPresent());
        assertEquals(1, data.get().size());
    }

    private <T> void thenResultingScalarTensorIsEqualTo(Class<T> type, T value) {
        assertEquals(1, this.outputTensors.size());
        Optional<List<T>> data = this.outputTensors.get(0).getData(type);

        assertTrue(data.isPresent());
        assertEquals(value, data.get().get(0));
    }

    private <T> void thenAllResultingTensorAreIstanceOf(Class<T> type) {
        for (Tensor resultingTensor : outputTensors) {
            Optional<List<T>> data = resultingTensor.getData(type);

            assertTrue(data.isPresent());
            assertEquals(1, data.get().size());
        }
    }

    private <T> void thenResultingNamedScalarTensorIsEqualTo(String name, Class<T> type, T value) {
        Tensor tensor = findTensorByName(name, outputTensors);

        assertNotNull(tensor);

        Optional<List<T>> data = tensor.getData(type);

        assertTrue(data.isPresent());
        assertEquals(value, data.get().get(0));
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

    /*
     * Utils
     */
    @Before
    public void cleanup() {
        this.wireRecordProperties = new HashMap();
        this.inputDescriptors = new ArrayList<>();
    }

}

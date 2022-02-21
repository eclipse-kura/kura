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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.KuraIOException;
import org.eclipse.kura.ai.inference.Tensor;
import org.eclipse.kura.ai.inference.TensorDescriptor;
import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.type.TypedValues;
import org.eclipse.kura.wire.WireRecord;

public class TensorListAdapter {

    private static TensorListAdapter instance;
    private List<TensorDescriptor> descriptors;

    private TensorListAdapter() {
    }

    private void setDescriptors(List<TensorDescriptor> descriptors) {
        instance.descriptors = descriptors;
    }

    /**
     * 
     * @param descriptors
     *            the list of {@link TensorDescriptor} to use in this instance
     * @return the {@link TensorListAdapter} with the descriptors set
     */
    public static TensorListAdapter givenDescriptors(List<TensorDescriptor> descriptors) {
        if (instance == null) {
            instance = new TensorListAdapter();
        }
        instance.setDescriptors(descriptors);
        return instance;
    }

    /**
     *
     * @param wireRecord
     * @return a list of {@link Tensor} of shape (1, n), where n:
     *         n=1 if record type is Boolean, Double, Float, Integer, Long
     *         n=length(x) if record type is a String x or a byte[] x
     * @throws KuraException
     *             when the expected shapes are not matching the actual ones of the record or
     *             if no descriptor matches the record name
     */
    public List<Tensor> fromWireRecord(WireRecord wireRecord) throws KuraException {
        List<Tensor> output = new LinkedList<>();

        // each descriptor name must have an entry in the record
        for (TensorDescriptor descriptor : instance.descriptors) {
            TypedValue<?> value = getTypedValueByNameFromMap(descriptor.getName(), wireRecord.getProperties());
            output.add(createTensorFromTypedValue(value, descriptor));
        }

        return output;
    }

    /**
     * Each tensor of shape ({@code m}, {@code l}) will be converted to a {@link WireRecord} that will have {@code m} x
     * {@code l} entries in its properties
     * 
     * @param tensors
     *            the list of {@link Tensor} to convert to a list of {@link WireRecord}
     * @return a list {@link WireRecord}, one for each tensor
     */
    public List<WireRecord> fromTensorList(List<Tensor> tensors) {

        List<WireRecord> result = new ArrayList<>();

        for (Tensor tensor : tensors) {
            Map<String, TypedValue<?>> properties = new HashMap<>();
            String name = tensor.getDescriptor().getName();
            Class<?> tensorType = tensor.getType();

            if (tensor.getData(tensorType).isPresent()) {

                List<?> tensorData = tensor.getData(tensorType).get();

                // unwrap tensor data to map entries
                for (int dimension = 0; dimension < tensor.getDescriptor().getShape().size(); dimension++) {
                    for (int i = 0; i < tensor.getDescriptor().getShape().get(dimension); i++) {
                        TypedValue<?> typedValue = TypedValues.newTypedValue(tensorData.get(i));
                        properties.put(name, typedValue);
                    }
                }
            }

            result.add(new WireRecord(properties));
        }

        return result;
    }

    private TypedValue<?> getTypedValueByNameFromMap(String name, Map<String, TypedValue<?>> properties)
            throws KuraException {

        for (Entry<String, TypedValue<?>> entry : properties.entrySet()) {
            if (entry.getKey().equals(name)) {
                return entry.getValue();
            }
        }
        throw new KuraException(KuraErrorCode.NOT_FOUND);
    }

    private Tensor createTensorFromTypedValue(TypedValue<?> typedValue, TensorDescriptor descriptor)
            throws KuraIOException {
        Object value = typedValue.getValue();
        switch (typedValue.getType()) {
        case BOOLEAN:
            List<Boolean> boolData = new ArrayList<>();
            boolData.add((Boolean) value);

            return new Tensor(Boolean.class, descriptor, boolData);
        case BYTE_ARRAY:
            byte[] byteArrayValue = (byte[]) value;

            List<Byte> byteArrayData = new ArrayList<>();
            for (byte b : byteArrayValue) {
                byteArrayData.add(b);
            }

            return new Tensor(Byte.class, descriptor, byteArrayData);
        case DOUBLE:
            List<Double> doubleData = new ArrayList<>();
            doubleData.add((Double) value);

            return new Tensor(Double.class, descriptor, doubleData);
        case FLOAT:
            List<Float> floatData = new ArrayList<>();
            floatData.add((Float) value);

            return new Tensor(Float.class, descriptor, floatData);
        case INTEGER:
            List<Integer> intData = new ArrayList<>();
            intData.add((Integer) value);

            return new Tensor(Integer.class, descriptor, intData);
        case LONG:
            List<Long> longData = new ArrayList<>();
            longData.add((Long) value);

            return new Tensor(Long.class, descriptor, longData);
        case STRING:
            String stringValue = (String) value;

            List<Byte> bytesData = new ArrayList<>();
            for (char c : stringValue.toCharArray()) {
                bytesData.add(Byte.parseByte(Character.toString(c)));
            }

            return new Tensor(Byte.class, descriptor, bytesData);
        default:
            throw new KuraIOException("Unable to create Tensor: unsupported type.");
        }
    }
}

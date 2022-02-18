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

import org.eclipse.kura.KuraIOException;
import org.eclipse.kura.ai.inference.Tensor;
import org.eclipse.kura.ai.inference.TensorDescriptor;
import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.type.TypedValues;
import org.eclipse.kura.wire.WireRecord;

public class TensorListAdapter {

    private static TensorListAdapter instance;
    private List<TensorDescriptor> descriptors;

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
     * @throws KuraIOException
     *             when the expected shapes are not matching the actual ones of the record or
     *             if no descriptor matches the record name
     */
    public List<Tensor> fromWireRecord(WireRecord wireRecord) throws KuraIOException {
        List<Tensor> output = new LinkedList<>();

        Map<String, TypedValue<?>> properties = wireRecord.getProperties();

        // each entry (=channel) becomes a tensor
        for (Entry<String, TypedValue<?>> entry : properties.entrySet()) {

            TensorDescriptor descriptor = getDescriptorByName(entry.getKey());

            output.add(createTensorFromTypedValue(entry.getValue(), descriptor));
        }

        return output;
    }

    /**
     * 
     * @param tensors
     *            of shape (1, n), where n:
     *            n=1 if record type is Boolean, Double, Float, Integer, Long
     *            n=length(x) if record type is a String x or a byte[] x
     * @return a {@link WireRecord}, where each properties entry corresponds to an output tensor
     */
    public WireRecord fromTensorList(List<Tensor> tensors) {
        Map<String, TypedValue<?>> properties = new HashMap<>();
        for (Tensor tensor : tensors) {
            String name = tensor.getDescriptor().getName();
            TypedValue<?> typedValue = TypedValues.newTypedValue(null);
            properties.put(name, typedValue);
        }

        return new WireRecord(properties);
    }

    private TensorDescriptor getDescriptorByName(String name) throws KuraIOException {
        TensorDescriptor descriptor = null;
        for (int i = 0; i < instance.descriptors.size(); i++) {
            if (instance.descriptors.get(i).getName().equals(name)) {
                descriptor = instance.descriptors.get(i);
                break;
            }
        }

        if (descriptor == null) {
            throw new KuraIOException("No TensorDescriptor found that matches name: " + name + ".");
        }

        return descriptor;
    }

    private void checkShapes(long expectedX, long expectedY, long actualX, long actualY) throws KuraIOException {
        if (actualX != expectedX || actualY != expectedY) {
            throw new KuraIOException("Incorrect shape: expected (" + expectedX + ", " + expectedY + ") but found "
                    + "(" + actualX + ", " + actualY + ").");
        }
    }

    private Tensor createTensorFromTypedValue(TypedValue<?> typedValue, TensorDescriptor descriptor)
            throws KuraIOException {
        Object value = typedValue.getValue();
        long shapeX = descriptor.getShape().get(0);
        long shapeY = descriptor.getShape().get(1);

        switch (typedValue.getType()) {
        case BOOLEAN:
            checkShapes(1, 1, shapeX, shapeY);

            List<Boolean> boolData = new ArrayList<>();
            boolData.add((Boolean) value);

            return new Tensor(Boolean.class, descriptor, boolData);
        case BYTE_ARRAY:
            byte[] byteArrayValue = (byte[]) value;

            checkShapes(1, byteArrayValue.length, shapeX, shapeY);

            List<Byte> byteArrayData = new ArrayList<>();
            for (byte b : byteArrayValue) {
                byteArrayData.add(b);
            }

            return new Tensor(Byte.class, descriptor, byteArrayData);
        case DOUBLE:
            checkShapes(1, 1, shapeX, shapeY);

            List<Double> doubleData = new ArrayList<>();
            doubleData.add((Double) value);

            return new Tensor(Double.class, descriptor, doubleData);
        case FLOAT:
            checkShapes(1, 1, shapeX, shapeY);

            List<Float> floatData = new ArrayList<>();
            floatData.add((Float) value);

            return new Tensor(Float.class, descriptor, floatData);
        case INTEGER:
            checkShapes(1, 1, shapeX, shapeY);

            List<Integer> intData = new ArrayList<>();
            intData.add((Integer) value);

            return new Tensor(Integer.class, descriptor, intData);
        case LONG:
            checkShapes(1, 1, shapeX, shapeY);

            List<Long> longData = new ArrayList<>();
            longData.add((Long) value);

            return new Tensor(Long.class, descriptor, longData);
        case STRING:
            String stringValue = (String) value;

            checkShapes(1, stringValue.length(), shapeX, shapeY);

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

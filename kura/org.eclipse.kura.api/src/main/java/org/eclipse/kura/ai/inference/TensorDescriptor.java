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
package org.eclipse.kura.ai.inference;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.osgi.annotation.versioning.ProviderType;

/**
 * The TensorDescriptor class describes the {@link Tensor} used as input or output
 * of a model for Artificial Intelligence and Machine Learning algorithms
 *
 * @since 2.3
 */
@ProviderType
public class TensorDescriptor {

    private final String name;
    private final String type;
    private final Optional<String> format;
    private final List<Long> shape;
    private final Map<String, Object> parameters;

    /**
     * Instantiates a tensor descriptor
     *
     * @param name
     *            the name of the tensor
     * @param type
     *            a string representing the type of data contained in the tensor.
     *            Its value is implementation specific, so a user should refer to
     *            the implementation documentation to figure out the allowed values.
     * @param shape
     *            the shape of the data
     */
    public TensorDescriptor(String name, String type, Optional<String> format, List<Long> shape,
            Map<String, Object> parameters) {
        this.name = name;
        this.type = type;
        this.format = format;
        this.shape = Collections.unmodifiableList(shape);
        this.parameters = Collections.unmodifiableMap(parameters);
    }

    /**
     * Instantiates a builder for a {@link TensorDescriptor}
     *
     * @param name
     *            the name of the tensor
     * @param type
     *            a string representing the type of data contained in the tensor.
     *            Its value is implementation specific, so a user should refer to
     *            the implementation documentation to figure out the allowed values.
     * @param shape
     *            the shape of the data as the size of a multi-dimensional matrix
     */
    public static TensorDescriptorBuilder builder(String name, String type, List<Long> shape) {
        return new TensorDescriptorBuilder(name, type, shape);
    }

    /**
     * Creates a new {@link TensorDescriptorBuilder} and initialises it from this {@link TensorDescriptor} instance.
     *
     * @return a new {@link TensorDescriptorBuilder}
     */
    public TensorDescriptorBuilder toBuilder() {
        return TensorDescriptorBuilder.fromTensorDescriptor(this);
    }

    /**
     * Return the name of the tensor
     *
     * @return a string representing the tensor name
     */
    public String getName() {
        return this.name;
    }

    /**
     * Return the type of data contained in the tensor.
     * Its value is implementation specific, so a user should refer to
     * the implementation documentation to figure out the allowed values.
     *
     * @return a string representing the type of data contained in the tensor
     */
    public String getType() {
        return this.type;
    }

    /**
     * Return the format of the data.
     * It represents how the data are organised or grouped in the tensor.
     * Its value is implementation specific, so a user should refer to
     * the implementation documentation to figure out the allowed values.
     *
     * @return an optional string representing the format of the data in the tensor
     */
    public Optional<String> getFormat() {
        return this.format;
    }

    /**
     * Return the shape of the data as the size of a multi-dimensional matrix.
     *
     * @return an unmodifiable list of longs representing the shape of the data
     */
    public List<Long> getShape() {
        return this.shape;
    }

    /**
     * Return the optional parameters assign to the tensor
     *
     * @return an unmodifiable map containing the tensor parameters
     */
    public Map<String, Object> getParameters() {
        return this.parameters;
    }

}

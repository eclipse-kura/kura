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

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.osgi.annotation.versioning.ProviderType;

/**
 * The TensorDescriptorBuilder class is a builder of {@link TensorDescriptor}
 *
 * @since 2.3
 */
@ProviderType
public class TensorDescriptorBuilder {

    private final String name;
    private final String type;
    private Optional<String> format;
    private final List<Long> shape;
    private final Map<String, Object> parameters;

    TensorDescriptorBuilder(String name, String type, List<Long> shape) {
        this.name = name;
        this.type = type;
        this.format = Optional.empty();
        this.shape = shape;
        this.parameters = new HashMap<>();
    }

    /**
     * Instantiates a builder for a {@link TensorDescriptor} and initialises it from the supplied argument.
     *
     * @param descriptor
     * @return a {@link TensorDescriptorBuilder}.
     */
    public static TensorDescriptorBuilder fromTensorDescriptor(TensorDescriptor descriptor) {
        final TensorDescriptorBuilder result = new TensorDescriptorBuilder(descriptor.getName(), descriptor.getType(),
                descriptor.getShape());
        result.parameters.putAll(descriptor.getParameters());
        result.format = descriptor.getFormat();
        return result;
    }

    /**
     * Set the format of the data contained in the tensor.
     * It represents how the data are organised or grouped in the tensor
     * (e.g. in row-major or column-major order).
     * Its value is implementation specific, so a user should refer to
     * the implementation documentation to figure out the allowed values.
     *
     * @param format
     *            a string representing the format of the data in the tensor
     * @return a TensorDescriptorBuilder
     */
    public TensorDescriptorBuilder format(String format) {
        if (nonNull(format) && !format.isEmpty()) {
            this.format = Optional.of(format);
        }
        return this;
    }

    /**
     * Add a parameter to the tensor descriptor
     *
     * @param name
     *            the name of the parameter
     * @param parameter
     *            an Object representing the value of the parameter
     * @return a TensorDescriptorBuilder
     */
    public TensorDescriptorBuilder addParameter(String name, Object parameter) {
        this.parameters.put(name, parameter);
        return this;
    }

    /**
     * Remove a parameter from the tensor descriptor
     *
     * @param name
     *            the name of the parameter
     * @return a TensorDescriptorBuilder
     */
    public TensorDescriptorBuilder removeParameter(String name) {
        this.parameters.remove(name);
        return this;
    }

    /**
     * Create an instance of TensorDescriptor
     *
     * @return a {TensorDescriptor}
     */
    public TensorDescriptor build() {
        if (isNull(this.name) || this.name.isEmpty()) {
            throw new IllegalArgumentException("The name of the tensor cannot be empty or null");
        }
        if (isNull(this.type) || this.type.isEmpty()) {
            throw new IllegalArgumentException("The type of the tensor cannot be empty or null");
        }
        if (isNull(this.shape) || this.shape.isEmpty()) {
            throw new IllegalArgumentException("The shape of the tensor cannot be empty or null");
        }
        return new TensorDescriptor(this.name, this.type, this.format, this.shape, this.parameters);
    }
}

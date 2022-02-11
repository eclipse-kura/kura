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

import java.util.HashMap;
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
    private Optional<String> format;
    private final List<Long> shape;
    private final Map<String, Object> parameters;

    public TensorDescriptor(String name, String type, List<Long> shape) {
        this.name = name;
        this.type = type;
        this.format = Optional.empty();
        this.shape = shape;
        this.parameters = new HashMap<>();
    }

    public String getName() {
        return this.name;
    }

    public String getType() {
        return this.type;
    }

    public Optional<String> getFormat() {
        return this.format;
    }

    public void setFormat(String format) {
        this.format = Optional.of(format);
    }

    public List<Long> getShape() {
        return this.shape;
    }

    public Map<String, Object> getParameters() {
        return this.parameters;
    }

    public void putParameter(String name, Object parameter) {
        this.parameters.put(name, parameter);
    }

    public void deleteParameter(String name) {
        this.parameters.remove(name);
    }
}

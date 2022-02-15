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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.osgi.annotation.versioning.ProviderType;

/**
 * The ModelInfoBuilder class is a builder of {@link ModelInfo}
 *
 * @since 2.3
 */
@ProviderType
public class ModelInfoBuilder {

    private final String modelName;
    private Optional<String> modelPlatform;
    private Optional<String> version;
    private final Map<String, Object> parameters;
    private final List<TensorDescriptor> inputDescriptors;
    private final List<TensorDescriptor> outputDescriptors;

    private ModelInfoBuilder(String modelName) {
        this.modelName = modelName;
        this.modelPlatform = Optional.empty();
        this.version = Optional.empty();
        this.parameters = new HashMap<>();
        this.inputDescriptors = new ArrayList<>();
        this.outputDescriptors = new ArrayList<>();
    }

    /**
     * Instantiates a builder for a {@link ModelInfo}
     * 
     * @param modelName
     *            the name of the model
     */
    public static ModelInfoBuilder builder(String modelName) {
        return new ModelInfoBuilder(modelName);
    }

    /**
     * Set the model platform
     * 
     * @param modelPlatform
     *            a string representing the platform used for running the model
     * @return a ModelInfoBuilder
     */
    public ModelInfoBuilder modelPlatform(String modelPlatform) {
        this.modelPlatform = Optional.of(modelPlatform);
        return this;
    }

    /**
     * Set the version of the model
     * 
     * @param version
     *            a string representing the version of the model
     * @return a ModelInfoBuilder
     */
    public ModelInfoBuilder version(String version) {
        this.version = Optional.of(version);
        return this;
    }

    /**
     * Add a parameter to the model
     * 
     * @param name
     *            the name of the parameter
     * @param parameter
     *            an Object representing the value of the parameter
     * @return a ModelInfoBuilder
     */
    public ModelInfoBuilder parameter(String name, Object parameter) {
        this.parameters.put(name, parameter);
        return this;
    }

    /**
     * Add a descriptor of an input tensor
     * 
     * @param inputDescriptor
     *            a {@link TensorDescriptor} for the input tensor
     * @return a ModelInfoBuilder
     */
    public ModelInfoBuilder inputDescriptor(TensorDescriptor inputDescriptor) {
        this.inputDescriptors.add(inputDescriptor);
        return this;
    }

    /**
     * Add a descriptor of an output tensor
     * 
     * @param outputDescriptor
     *            a {@link TensorDescriptor} for the output tensor
     * @return a ModelInfoBuilder
     */
    public ModelInfoBuilder outputDescriptor(TensorDescriptor outputDescriptor) {
        this.outputDescriptors.add(outputDescriptor);
        return this;
    }

    /**
     * Create an instance of ModelInfo
     * 
     * @return a {@ModelInfo}
     */
    public ModelInfo build() {
        if (this.inputDescriptors.isEmpty()) {
            throw new IllegalArgumentException("The input descriptors list cannot be empty");
        }
        if (this.outputDescriptors.isEmpty()) {
            throw new IllegalArgumentException("The output descriptors list cannot be empty");
        }
        ModelInfo modelInfo = new ModelInfo(this.modelName, this.inputDescriptors, this.outputDescriptors);
        modelInfo.setModelPlatform(this.modelPlatform.get());
        modelInfo.setVersion(this.version.get());
        this.parameters.forEach(modelInfo::putParameter);
        return modelInfo;
    }
}

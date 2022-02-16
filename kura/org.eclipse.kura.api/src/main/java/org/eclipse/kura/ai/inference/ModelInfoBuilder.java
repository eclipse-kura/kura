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

import static java.util.Objects.nonNull;

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

    private final String name;
    private Optional<String> platform;
    private Optional<String> version;
    private final Map<String, Object> parameters;
    private final List<TensorDescriptor> inputDescriptors;
    private final List<TensorDescriptor> outputDescriptors;

    private ModelInfoBuilder(String name) {
        this.name = name;
        this.platform = Optional.empty();
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
    public static ModelInfoBuilder builder(String name) {
        return new ModelInfoBuilder(name);
    }

    /**
     * Instantiates a builder for a {@link ModelInfo}
     * 
     * @param modelInfo
     * @return a ModelInfoBuilder
     */
    public static ModelInfoBuilder fromModelInfo(ModelInfo modelInfo) {
        ModelInfoBuilder builder = new ModelInfoBuilder(modelInfo.getName());
        modelInfo.getParameters().forEach(builder::addParameter);
        modelInfo.getInputs().forEach(builder::addInputDescriptor);
        modelInfo.getOutputs().forEach(builder::addOutputDescriptor);
        modelInfo.getPlatform().ifPresent(builder::platform);
        modelInfo.getVersion().ifPresent(builder::version);
        return builder;
    }

    /**
     * Set the model platform
     * 
     * @param platform
     *            a string representing the platform used for running the model
     * @return a ModelInfoBuilder
     */
    public ModelInfoBuilder platform(String modelPlatform) {
        this.platform = Optional.of(modelPlatform);
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
    public ModelInfoBuilder addParameter(String name, Object parameter) {
        this.parameters.put(name, parameter);
        return this;
    }

    /**
     * Remove a parameter from the model
     * 
     * @param name
     *            the name of the parameter
     * @return a ModelInfoBuilder
     */
    public ModelInfoBuilder removeParameter(String name) {
        this.parameters.remove(name);
        return this;
    }

    /**
     * Add a descriptor of an input tensor
     * 
     * @param inputDescriptor
     *            a {@link TensorDescriptor} for the input tensor
     * @return a ModelInfoBuilder
     */
    public ModelInfoBuilder addInputDescriptor(TensorDescriptor inputDescriptor) {
        this.inputDescriptors.add(inputDescriptor);
        return this;
    }

    /**
     * Remove a descriptor from the input tensor list
     * 
     * @param inputDescriptor
     *            a {@link TensorDescriptor} for the input tensor
     * @return a ModelInfoBuilder
     */
    public ModelInfoBuilder removeInputDescriptor(TensorDescriptor inputDescriptor) {
        this.inputDescriptors.remove(inputDescriptor);
        return this;
    }

    /**
     * Add a descriptor of an output tensor
     * 
     * @param outputDescriptor
     *            a {@link TensorDescriptor} for the output tensor
     * @return a ModelInfoBuilder
     */
    public ModelInfoBuilder addOutputDescriptor(TensorDescriptor outputDescriptor) {
        this.outputDescriptors.add(outputDescriptor);
        return this;
    }

    /**
     * Remove a descriptor from the output tensor list
     * 
     * @param ioututDescriptor
     *            a {@link TensorDescriptor} for the input tensor
     * @return a ModelInfoBuilder
     */
    public ModelInfoBuilder removeOutputDescriptor(TensorDescriptor outputDescriptor) {
        this.outputDescriptors.remove(outputDescriptor);
        return this;
    }

    /**
     * Create an instance of ModelInfo
     * 
     * @return a {@ModelInfo}
     */
    public ModelInfo build() {
        if (nonNull(this.name) && !this.name.isEmpty()) {
            throw new IllegalArgumentException("The name of the model cannot be empty or null");
        }
        if (this.inputDescriptors.isEmpty()) {
            throw new IllegalArgumentException("The input descriptors list cannot be empty");
        }
        if (this.outputDescriptors.isEmpty()) {
            throw new IllegalArgumentException("The output descriptors list cannot be empty");
        }
        return new ModelInfo(this.name, this.platform.get(), this.version.get(), this.parameters, this.inputDescriptors,
                this.outputDescriptors);
    }
}

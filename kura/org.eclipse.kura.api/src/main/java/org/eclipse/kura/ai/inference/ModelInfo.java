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
 * The ModelInfo class represents the metadata of a model
 * for Artificial Intelligence and Machine Learning algorithms
 *
 * @since 2.3
 */
@ProviderType
public class ModelInfo {

    /** The name of the model */
    private final String modelName;
    /** An optional string representing the platform used for this model */
    private Optional<String> modelPlatform;
    /** An optional string representing the version of the model */
    private Optional<String> version;
    /** A set of key pairs describing the model parameters */
    private final Map<String, Object> parameters;
    /** The list of {@link TensorDescriptor} of the input tensors */
    private final List<TensorDescriptor> inputDescriptors;
    /** The list of {@link TensorDescriptor} of the output tensors */
    private final List<TensorDescriptor> outputDescriptors;

    /**
     * Instantiates a new ModelInfo
     * 
     * @param modelName
     *            the name of the model
     * @param inputDescriptors
     *            a list of {@link TensorDescriptor} of the input tensors
     * @param outputDescriptors
     *            a list of {@link TensorDescriptor} of the output tensors
     */
    protected ModelInfo(String modelName, List<TensorDescriptor> inputDescriptors,
            List<TensorDescriptor> outputDescriptors) {
        this.modelName = modelName;
        this.modelPlatform = Optional.empty();
        this.version = Optional.empty();
        this.parameters = new HashMap<>();
        this.inputDescriptors = inputDescriptors;
        this.outputDescriptors = outputDescriptors;
    }

    public String getModelName() {
        return this.modelName;
    }

    public Optional<String> getModelPlatform() {
        return this.modelPlatform;
    }

    public void setModelPlatform(String modelPlatform) {
        this.modelPlatform = Optional.of(modelPlatform);
    }

    public Optional<String> getVersion() {
        return this.version;
    }

    public void setVersion(String version) {
        this.version = Optional.of(version);
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

    public List<TensorDescriptor> getInputs() {
        return inputDescriptors;
    }

    public List<TensorDescriptor> getOutputs() {
        return outputDescriptors;
    }
}

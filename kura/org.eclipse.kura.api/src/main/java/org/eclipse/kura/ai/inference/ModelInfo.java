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

    private final String modelName;
    private Optional<String> modelPlatform;
    private Optional<String> version;
    private final Map<String, Object> parameters;
    private final List<TensorDescriptor> inputDescriptors;
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

    /**
     * Return the name of the model
     * 
     * @return a string representing the model name
     */
    public String getModelName() {
        return this.modelName;
    }

    /**
     * Return the platform used for running this model
     * 
     * @return an optional string representing the model platform
     */
    public Optional<String> getModelPlatform() {
        return this.modelPlatform;
    }

    /**
     * Set the model platform
     * 
     * @param modelPlatform
     *            a string representing the platform used for running this model
     */
    public void setModelPlatform(String modelPlatform) {
        this.modelPlatform = Optional.of(modelPlatform);
    }

    /**
     * Return the version of the model
     * 
     * @return an optional string representing the version of the model
     */
    public Optional<String> getVersion() {
        return this.version;
    }

    /**
     * Set the version of the model
     * 
     * @param version
     *            a string representing the version of the model
     */
    public void setVersion(String version) {
        this.version = Optional.of(version);
    }

    /**
     * Return the optional parameters assign to the model
     * 
     * @return a map containing the model parameters
     */
    public Map<String, Object> getParameters() {
        return this.parameters;
    }

    /**
     * Add a parameter to the model
     * 
     * @param name
     *            the name of the parameter
     * @param parameter
     *            an Object representing the value of the parameter
     */
    public void putParameter(String name, Object parameter) {
        this.parameters.put(name, parameter);
    }

    /**
     * Remove the given parameter
     * 
     * @param name
     *            the name of the parameter to be removed
     */
    public void deleteParameter(String name) {
        this.parameters.remove(name);
    }

    /**
     * Return the descriptors of the input tensors
     * 
     * @return a list of {@link TensorDescriptor} of the input tensors
     */
    public List<TensorDescriptor> getInputs() {
        return inputDescriptors;
    }

    /**
     * Return the descriptors of the output tensors
     * 
     * @return a list of {@link TensorDescriptor} of the output tensors
     */
    public List<TensorDescriptor> getOutputs() {
        return outputDescriptors;
    }
}

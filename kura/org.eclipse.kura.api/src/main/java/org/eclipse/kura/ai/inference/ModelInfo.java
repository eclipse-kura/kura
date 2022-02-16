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
 * The ModelInfo class represents the metadata of a model
 * for Artificial Intelligence and Machine Learning algorithms
 *
 * @since 2.3
 */
@ProviderType
public class ModelInfo {

    private final String name;
    private final Optional<String> platform;
    private final Optional<String> version;
    private final Map<String, Object> parameters;
    private final List<TensorDescriptor> inputDescriptors;
    private final List<TensorDescriptor> outputDescriptors;

    /**
     * Instantiates a new ModelInfo
     * 
     * @param modelName
     *            the name of the model
     * @param platform
     *            an optional string representing the model platform
     * @param version
     *            an optional string representing the model version
     * @param parameters
     *            a map containing the model parameters. It can be empty.
     * @param inputDescriptors
     *            a list of {@link TensorDescriptor} of the input tensors
     * @param outputDescriptors
     *            a list of {@link TensorDescriptor} of the output tensors
     */
    protected ModelInfo(String modelName, Optional<String> platform, Optional<String> version,
            Map<String, Object> parameters, List<TensorDescriptor> inputDescriptors,
            List<TensorDescriptor> outputDescriptors) {
        this.name = modelName;
        this.platform = platform;
        this.version = version;
        this.parameters = parameters;
        this.inputDescriptors = inputDescriptors;
        this.outputDescriptors = outputDescriptors;
    }

    /**
     * Return the name of the model
     * 
     * @return a string representing the model name
     */
    public String getName() {
        return this.name;
    }

    /**
     * Return the platform used for running this model
     * 
     * @return an optional string representing the model platform
     */
    public Optional<String> getPlatform() {
        return this.platform;
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
     * Return the optional parameters assigned to the model
     * 
     * @return a map containing the model parameters
     */
    public Map<String, Object> getParameters() {
        Map<String, Object> parametersCopy = new HashMap<>();
        this.parameters.forEach(parametersCopy::put);
        return parametersCopy;
    }

    /**
     * Return the descriptors of the input tensors
     * 
     * @return a list of {@link TensorDescriptor} of the input tensors
     */
    public List<TensorDescriptor> getInputs() {
        List<TensorDescriptor> inputDescriptorCopy = new ArrayList<>();
        this.inputDescriptors.forEach(inputDescriptorCopy::add);
        return inputDescriptorCopy;
    }

    /**
     * Return the descriptors of the output tensors
     * 
     * @return a list of {@link TensorDescriptor} of the output tensors
     */
    public List<TensorDescriptor> getOutputs() {
        List<TensorDescriptor> outputDescriptorCopy = new ArrayList<>();
        this.outputDescriptors.forEach(outputDescriptorCopy::add);
        return outputDescriptorCopy;
    }
}

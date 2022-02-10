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
package org.eclipse.kura.ai.inference.engine;

import java.util.List;
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
    private final List<TensorDescriptor> inputDescriptors;
    private final List<TensorDescriptor> outputDescriptors;

    protected ModelInfo(String modelName, List<TensorDescriptor> inputDescriptors,
            List<TensorDescriptor> outputDescriptors) {
        this.modelName = modelName;
        this.modelPlatform = Optional.empty();
        this.version = Optional.empty();
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

    public List<TensorDescriptor> getInputs() {
        return inputDescriptors;
    }

    public List<TensorDescriptor> getOutputs() {
        return outputDescriptors;
    }
}

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
import java.util.List;
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
    private final List<TensorDescriptor> inputDescriptors;
    private final List<TensorDescriptor> outputDescriptors;

    private ModelInfoBuilder(String modelName) {
        this.modelName = modelName;
        this.modelPlatform = Optional.empty();
        this.version = Optional.empty();
        this.inputDescriptors = new ArrayList<>();
        this.outputDescriptors = new ArrayList<>();
    }

    public static ModelInfoBuilder builder(String modelName) {
        return new ModelInfoBuilder(modelName);
    }

    public ModelInfoBuilder modelPlatform(String modelPlatform) {
        this.modelPlatform = Optional.of(modelPlatform);
        return this;
    }

    public ModelInfoBuilder version(String version) {
        this.version = Optional.of(version);
        return this;
    }

    public ModelInfoBuilder inputDescriptor(TensorDescriptor inputDescriptor) {
        this.inputDescriptors.add(inputDescriptor);
        return this;
    }

    public ModelInfoBuilder outputDescriptor(TensorDescriptor outputDescriptor) {
        this.outputDescriptors.add(outputDescriptor);
        return this;
    }

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
        return modelInfo;
    }
}

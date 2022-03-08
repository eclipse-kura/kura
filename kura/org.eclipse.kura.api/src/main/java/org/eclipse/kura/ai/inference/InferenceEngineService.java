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

import java.util.List;
import java.util.Optional;

import org.eclipse.kura.KuraException;
import org.osgi.annotation.versioning.ProviderType;

/**
 * An Inference Engine is a library or a server that accepts multiple files
 * describing an Artificial Intelligence and Machine Learning models
 * and allows to perform inference on data.
 * 
 * The InferenceEngineService interface is a service API for managing an Inference Engine.
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 * @since 2.3
 */
@ProviderType
public interface InferenceEngineService {

    /**
     * Run an inference for the given model and inputs.
     * The input and output type and size must match the
     * ones in the provided {@link ModelInfo}.
     *
     * This will fail if the model is not loaded or the engine is not ready.
     * 
     * @param modelInfo
     *            the {@link ModelInfo} of the model to be used
     * @param inputData
     *            a list of input {@link Tensor}
     * @return a list of output {@link Tensor}
     * @throws KuraIOException
     */
    public List<Tensor> infer(ModelInfo modelInfo, List<Tensor> inputData) throws KuraException;

    /**
     * Load the given model in the inference engine.
     * If the path of the file containing the model is not provided,
     * the engine will load it from a standard location in the filesystem.
     *
     * @param modelName
     *            the name of the model
     * @param model
     *            an optional String representing the path on the filesystem where the model is stored
     * @throws KuraIOException
     */
    public void loadModel(String modelName, Optional<String> modelPath) throws KuraException;

    /**
     * Remove a model from the inference engine
     *
     * @param modelName
     *            the name of the model
     * @throws KuraIOException
     */
    public void unloadModel(String modelName) throws KuraException;

    /**
     * Return true if the model is loaded and ready
     *
     * @param modelName
     *            the name of the model
     * @throws KuraIOException
     */
    public boolean isModelLoaded(String modelName) throws KuraException;

    /*
     * Return the names of the available models
     *
     * @return a List of model names
     *
     * @throws KuraIOException
     */
    public List<String> getModelNames() throws KuraException;

    /**
     * Return informations about a specified model
     *
     * @return a {@link ModelInfo} that describes a model
     * @throws KuraIOException
     */
    public Optional<ModelInfo> getModelInfo(String modelName) throws KuraException;

    /**
     * Check if the inference engine is ready for inferencing
     *
     * @return true if the server is ready
     * @throws KuraIOException
     */
    public boolean isEngineReady() throws KuraException;
}

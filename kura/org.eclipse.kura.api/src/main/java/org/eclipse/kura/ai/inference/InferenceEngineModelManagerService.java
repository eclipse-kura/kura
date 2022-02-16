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

import org.eclipse.kura.KuraIOException;
import org.osgi.annotation.versioning.ProviderType;

/**
 * The InferenceEngineModelManagerService interface is a service API for managing models
 * for Artificial Intelligence and Machine Learning algorithms
 *
 * @noimplement This interface is not intended to be implemented by clients.
 * @since 2.3
 */
@ProviderType
public interface InferenceEngineModelManagerService {

    /**
     * Load the given model in the inference engine.
     * If the model is not provided, the engine will load it
     * from a standard location in the filesystem.
     *
     * @param modelName
     *            the name of the model
     * @param model
     *            an optional String representing the model
     * @throws KuraIOException
     */
    public void loadModel(String modelName, Optional<String> model) throws KuraIOException;

    /**
     * Load a model in the inference engine from the specified path
     *
     * @param modelName
     *            the name of the model
     * @param modelPath
     *            the path on the filesystem where the model is stored
     * @throws KuraIOException
     */
    public void loadModel(String modelName, String modelPath) throws KuraIOException;

    /**
     * Remove a model from the inference engine
     *
     * @param modelName
     *            the name of the model
     * @throws KuraIOException
     */
    public void unloadModel(String modelName) throws KuraIOException;

    /**
     * Return true if the model is loaded and ready
     *
     * @param modelName
     *            the name of the model
     * @throws KuraIOException
     */
    public boolean isModelLoaded(String modelName) throws KuraIOException;

    /*
     * Return the names of the available models
     *
     * @return a List of model names
     *
     * @throws KuraIOException
     */
    public List<String> getModelNames() throws KuraIOException;

    /**
     * Return informations about a specified model
     *
     * @return a {@link ModelInfo} that describes a model
     * @throws KuraIOException
     */
    public Optional<ModelInfo> getModelInfo(String modelName) throws KuraIOException;
}

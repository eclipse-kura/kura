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

import org.eclipse.kura.KuraIOException;
import org.osgi.annotation.versioning.ProviderType;

/**
 * The InferenceEngineService interface is a service API for running inference
 * for Artificial Intelligence and Machine Learning algorithms
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 * @since 2.3
 */
@ProviderType
public interface InferenceEngineService {

    /**
     * Run an inference for the given model and inputs.
     * The input and output type and size must match the
     * ones in the provided {@link ModelInfo}
     * 
     * @param modelInfo
     *            the {@link ModelInfo} of the model to be used
     * @param inputData
     *            a list of input {@link Tensor}
     * @return a list of output {@link Tensor}
     */
    public List<Tensor> infer(ModelInfo modelInfo, List<Tensor> inputData) throws KuraIOException;
}

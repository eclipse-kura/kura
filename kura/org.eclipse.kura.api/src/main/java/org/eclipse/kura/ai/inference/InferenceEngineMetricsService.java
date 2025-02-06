/*******************************************************************************
 * Copyright (c) 2025 Eurotech and/or its affiliates and others
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

import java.util.Map;
import java.util.Optional;

import org.eclipse.kura.KuraException;
import org.osgi.annotation.versioning.ProviderType;

/**
 * 
 * The InferenceEngineMetricsService interface provides APIs to get performance
 * and status metrics from an Inference Engine.
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 * @since 3.0
 */
@ProviderType
public interface InferenceEngineMetricsService extends InferenceEngineService {

    /**
     * Retrieve the performance and status metrics from the Inference Engine
     * as a map of key-value pairs. Typically the keys are the names of the metrics.
     * 
     * @return a Map containing the metrics. The key of the entries are the metric names.
     * @throws KuraException
     */
    public Map<String, String> getMetrics() throws KuraException;

    /**
     * Retrieve the performance and status metrics from the Inference Engine
     * as they are emitted by the engine.
     * 
     * @return an optional String representing the raw metrics.
     * @throws KuraException
     */
    public Optional<String> getRawMetrics() throws KuraException;

}

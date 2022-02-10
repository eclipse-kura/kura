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

import org.eclipse.kura.KuraIOException;
import org.osgi.annotation.versioning.ProviderType;

/**
 * The InferenceEngineStatusService interface is a service API for getting
 * status information from inference engines
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 * @since 2.3
 */
@ProviderType
public interface InferenceEngineStatusService {

    /**
     * Check if the inference engine is ready for inferencing
     * 
     * @return true if the server is ready
     */
    public boolean isEngineReady() throws KuraIOException;
}

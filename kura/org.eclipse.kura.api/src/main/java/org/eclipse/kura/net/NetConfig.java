/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.net;

import org.osgi.annotation.versioning.ProviderType;

/**
 * Marker interface for all network configuration classes
 *
 * @noimplement This interface is not intended to be implemented by clients.
 */
@ProviderType
public interface NetConfig {

    /**
     * Checks whether or not this configuration is valid.
     *
     * @return true if the configuration is valid, otherwise false
     */
    public boolean isValid();
}

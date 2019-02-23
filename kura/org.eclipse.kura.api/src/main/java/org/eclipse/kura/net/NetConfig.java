/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
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

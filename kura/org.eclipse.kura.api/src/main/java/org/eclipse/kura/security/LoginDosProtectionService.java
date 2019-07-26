/*******************************************************************************
 * Copyright (c) 2019 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.security;

import org.eclipse.kura.KuraException;
import org.osgi.annotation.versioning.ProviderType;

/**
 * This interface allows to manage services that protect the framework from DoS attacks to the exposed services.
 *
 * @since 2.2
 * @noimplement This interface is not intended to be implemented by clients.
 */
@ProviderType
public interface LoginDosProtectionService {

    /**
     * Enables the protection
     * 
     * @throws KuraException
     *             if the operation fails
     */
    public void enable() throws KuraException;

    /**
     * Disables the protection
     * 
     * @throws KuraException
     *             if the operation fails
     */
    public void disable() throws KuraException;

    /**
     * Returns the service status
     * 
     * @return true if the protection is enabled
     * @throws KuraException
     *             if the operation fails
     */
    public boolean isEnabled() throws KuraException;

}

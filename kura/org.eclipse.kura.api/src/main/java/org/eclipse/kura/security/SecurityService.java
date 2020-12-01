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
package org.eclipse.kura.security;

import org.eclipse.kura.KuraException;
import org.osgi.annotation.versioning.ProviderType;

/**
 * @noimplement This interface is not intended to be implemented by clients.
 */
@ProviderType
public interface SecurityService {

    /**
     * This method allows the reload of the security policy's fingerprint
     *
     * @throws KuraException
     *             in case an error is raised during the calculation of the fingerprint
     *             and the consequent storage.
     */
    public void reloadSecurityPolicyFingerprint() throws KuraException;

    /**
     * This method allows the reload of the command line fingerprint
     *
     * @throws KuraException
     *             in case an error is raised during the calculation of the fingerprint
     *             and the consequent storage.
     */
    public void reloadCommandLineFingerprint() throws KuraException;

    /**
     * This method returns a boolean that specifies if the debugging is permitted
     *
     * @return true if the debug is permitted. False otherwise.
     */
    public boolean isDebugEnabled();

}

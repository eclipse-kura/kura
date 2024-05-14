/*******************************************************************************
 * Copyright (c) 2011, 2024 Eurotech and/or its affiliates and others
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
     *         in case an error is raised during the calculation of the fingerprint and the consequent storage.
     */
    public void reloadSecurityPolicyFingerprint() throws KuraException;

    /**
     * This method allows the reload of the command line fingerprint
     *
     * @throws KuraException
     *         in case an error is raised during the calculation of the fingerprint and the consequent storage.
     */
    public void reloadCommandLineFingerprint() throws KuraException;

    /**
     * This method returns a boolean that specifies if the debugging is permitted
     *
     * @return true if the debug is permitted. False otherwise.
     */
    public boolean isDebugEnabled();

    /**
     * This method allows to apply the default production security policy available in the system. The changes are
     * persistent and remain in effect after a framework restart.
     *
     * @throws KuraException
     *         in case an error is raised during the application of the default production security policy.
     * @since 2.7
     */
    public void applyDefaultProductionSecurityPolicy() throws KuraException;

    /**
     * This method allows to apply the user provided security policy. The changes are persistent and remain in effect
     * after a framework restart.
     *
     * @param securityPolicy
     *         the security policy to be applied
     * @throws KuraException
     *         in case an error is raised during the application of the security policy.
     * @since 2.7
     */
    public void applySecurityPolicy(String securityPolicy) throws KuraException;
}

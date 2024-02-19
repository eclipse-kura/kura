/*******************************************************************************
 * Copyright (c) 2024 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.identity.configuration.extension;

import java.util.Optional;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ComponentConfiguration;
import org.osgi.annotation.versioning.ProviderType;

/**
 * A service interface that can be implemented to provide additional
 * configuration for Kura identities. The additional configuration can be
 * retrieved and updated by clients through the {@code IdentityService} using
 * the {@code AdditionalConfigurations} class.
 * <br>
 * <br>
 * Implementing service must be registered with the kura.service.pid property
 * set to an unique identifier.
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 * @since 2.7
 */
@ProviderType
public interface IdentityConfigurationExtension {

    /**
     * Retrieves the additional configuration managed by this extension for the
     * given identity, if any.
     * <br>
     * <br>
     * The {@link ComponentConfiguration#getPid()} method of the returned
     * configuration must be set to the value of the kura.service.pid property of
     * the extension service.
     * 
     * 
     * @param identityName the name of the identity.
     * @return the additional configuration, or an empty optional.
     * @throws KuraException if a failure occurs while retrieving the configuration.
     */
    public Optional<ComponentConfiguration> getConfiguration(String identityName) throws KuraException;

    /**
     * Updates the additional configuration managed by this extension for the
     * given identity.
     * 
     * @param identityName  the name of the identity.
     * @param configuration the configuration to be applied.
     * @throws KuraException if a failure occurs while updating the configuration.
     */
    public void updateConfiguration(String identityName, ComponentConfiguration configuration) throws KuraException;
}

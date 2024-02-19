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
package org.eclipse.kura.identity;

import static java.util.Objects.requireNonNull;

import java.util.List;
import java.util.Objects;

import org.eclipse.kura.configuration.ComponentConfiguration;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Represents a set of additional configurations associated with an identity
 * managed by {@code IdentityConfigurationExtension} implementations.
 * <br>
 * This class contains a list of {@link ComponentConfiguration} instances, the
 * {@link ComponentConfiguration#getPid()} method of each configuration should
 * return the kura.service.pid of the associated
 * {@code IdentityConfigurationExtension}.
 * <br>
 * <br>
 * The {@link IdentityService#getIdentitiesConfiguration(List)} and
 * {@link IdentityService#getIdentityConfiguration(String, List)} method will
 * return the {@link ComponentConfiguration}s provided by all
 * {@code IdentityConfigurationExtension}s registered in the framework.
 * <br>
 * <br>
 * The
 * {@link IdentityService#updateIdentityConfiguration(IdentityConfiguration)}
 * method
 * will call
 * {@code IdentityConfigurationExtension.updateConfiguration(String, ComponentConfiguration)}
 * method of the {@code IdentityConfigurationExtension} instances whose
 * kura.service.pid is referenced by the provided configurations.
 *
 * @noextend This class is not intended to be subclassed by clients.
 * @since 2.7.0
 */
@ProviderType
public class AdditionalConfigurations implements IdentityConfigurationComponent {

    private final List<ComponentConfiguration> configurations;

    /**
     * Creates a new instance containing the provided configuration list.
     *
     * @param configurations the configuration list.
     */
    public AdditionalConfigurations(final List<ComponentConfiguration> configurations) {
        this.configurations = requireNonNull(configurations, "configuration list cannot be null");
    }

    /**
     * Returns the list of component configurations.
     *
     * @return the list of component configurations.
     */
    public List<ComponentConfiguration> getConfigurations() {
        return this.configurations;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.configurations);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof AdditionalConfigurations)) {
            return false;
        }
        AdditionalConfigurations other = (AdditionalConfigurations) obj;
        return Objects.equals(this.configurations, other.configurations);
    }

}

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
import java.util.Optional;

import org.osgi.annotation.versioning.ProviderType;

/**
 * Describes the configuration for an identity. It is composed by different
 * {@link IdentityConfigurationComponent}s that can be retrieved and updated
 * separately using the {@link IdentityService}.
 * 
 * @noextend This class is not intended to be subclassed by clients.
 * @since 2.7.0
 */
@ProviderType
public class IdentityConfiguration {

    private final String name;
    private final List<IdentityConfigurationComponent> components;

    /**
     * Creates a new identity configuration with the given name and components.
     * 
     * @param name       the identity name.
     * @param components the {@link IdentityConfigurationComponent} list.
     */
    public IdentityConfiguration(String name, List<IdentityConfigurationComponent> components) {
        this.name = requireNonNull(name, "name cannot be null");

        if (this.name.trim().isEmpty()) {
            throw new IllegalArgumentException("name cannot be empty");
        }

        this.components = requireNonNull(components, "components cannot be null");
    }

    /**
     * Returns the identity name.
     * 
     * @return the identity name.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the list of {@link IdentityConfigurationComponent}s.
     * 
     * @return the list of {@link IdentityConfigurationComponent}s.
     */
    public List<IdentityConfigurationComponent> getComponents() {
        return components;
    }

    public <T extends IdentityConfigurationComponent> Optional<T> getComponent(final Class<T> clazz) {
        for (final IdentityConfigurationComponent component : components) {
            if (clazz.isInstance(component)) {
                return Optional.of(clazz.cast(component));
            }
        }

        return Optional.empty();
    }

    @Override
    public int hashCode() {
        return Objects.hash(components, name);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof IdentityConfiguration)) {
            return false;
        }
        IdentityConfiguration other = (IdentityConfiguration) obj;
        return Objects.equals(components, other.components) && Objects.equals(name, other.name);
    }

}

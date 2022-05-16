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
 *******************************************************************************/

package org.eclipse.kura.container.orchestration;

import static java.util.Objects.requireNonNull;

import java.util.Objects;
import java.util.Optional;

import org.osgi.annotation.versioning.ProviderType;

/**
 * Object which represents a container network configuration used to when
 * requesting the generation of a new container instance.
 *
 * @noimplement This interface is not intended to be implemented by clients.
 * @since 2.4
 *
 */
@ProviderType
public class ContainerNetworkConfiguration {

    private Optional<String> networkMode;

    private ContainerNetworkConfiguration() {
    }

    /**
     * 
     * Returns the network mode a container will be created with (e.g. 'bridge',
     * 'none', 'container:', 'host').
     * 
     * @return
     */
    public Optional<String> getNetworkMode() {
        return this.networkMode;
    }

    /**
     * Creates a builder for creating a new {@link ContainerNetworkConfiguration}
     * instance.
     *
     * @return the builder.
     */
    public static ContainerNetworkConfigurationBuilder builder() {
        return new ContainerNetworkConfigurationBuilder();
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.networkMode);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ContainerNetworkConfiguration)) {
            return false;
        }
        ContainerNetworkConfiguration other = (ContainerNetworkConfiguration) obj;
        return Objects.equals(this.networkMode, other.networkMode);
    }

    public static final class ContainerNetworkConfigurationBuilder {

        private Optional<String> networkMode = Optional.empty();

        public ContainerNetworkConfigurationBuilder setNetworkMode(Optional<String> networkMode) {
            this.networkMode = networkMode;
            return this;
        }

        public ContainerNetworkConfiguration build() {
            ContainerNetworkConfiguration result = new ContainerNetworkConfiguration();

            result.networkMode = requireNonNull(this.networkMode,
                    "Requested Container Network Mode Name cannot be null");

            return result;
        }

    }
}

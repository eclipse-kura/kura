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
package org.eclipse.kura.container.orchestration.provider.impl;

import static java.util.Objects.isNull;

import java.util.Map;
import java.util.Objects;

import org.eclipse.kura.util.configuration.Property;

public class ContainerOrchestrationServiceOptions {

    private static final Property<Boolean> IS_ENABLED = new Property<>("enabled", false);
    private static final Property<String> DOCKER_HOST_URL = new Property<>("container.engine.host",
            "unix:///var/run/docker.sock");
    
    private final boolean enabled;
    private final String hostUrl;

    public ContainerOrchestrationServiceOptions(final Map<String, Object> properties) {

        if (isNull(properties)) {
            throw new IllegalArgumentException("Properties cannot be null!");
        }

        this.enabled = IS_ENABLED.get(properties);
        this.hostUrl = DOCKER_HOST_URL.get(properties);
        
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public String getHostUrl() {
        return this.hostUrl;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.enabled, this.hostUrl);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        ContainerOrchestrationServiceOptions other = (ContainerOrchestrationServiceOptions) obj;
        return isEnabled() == other.isEnabled() && Objects.equals(getHostUrl(), other.getHostUrl());
    }
}

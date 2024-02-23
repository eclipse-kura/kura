/*******************************************************************************
 * Copyright (c) 2022, 2024 Eurotech and/or its affiliates and others
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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.eclipse.kura.util.configuration.Property;

public class ContainerOrchestrationServiceOptions {

    private static final Property<Boolean> IS_ENABLED = new Property<>("enabled", false);
    private static final Property<String> DOCKER_HOST_URL = new Property<>("container.engine.host",
            "unix:///var/run/docker.sock");
    private static final Property<Boolean> ALLOWLIST_ENABLED = new Property<>("allowlist.enabled", false);
    private static final Property<String> ALLOWLIST_CONTENT = new Property<>("allowlist.content", "");

    private final boolean enabled;
    private final String hostUrl;
    private final boolean enforcementEnabled;
    private final String enforcementAllowlistContent;

    public ContainerOrchestrationServiceOptions(final Map<String, Object> properties) {

        if (isNull(properties)) {
            throw new IllegalArgumentException("Properties cannot be null!");
        }

        this.enabled = IS_ENABLED.get(properties);
        this.hostUrl = DOCKER_HOST_URL.get(properties);
        this.enforcementEnabled = ALLOWLIST_ENABLED.get(properties);
        this.enforcementAllowlistContent = ALLOWLIST_CONTENT.get(properties);

    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public String getHostUrl() {
        return this.hostUrl;
    }

    public boolean isEnforcementEnabled() {
        return this.enforcementEnabled;
    }

    public List<String> getEnforcementAllowlistContent() {
        return Arrays.asList(this.enforcementAllowlistContent.trim().split(","));
    }

    @Override
    public int hashCode() {
        return Objects.hash(enforcementAllowlistContent, enforcementEnabled, enabled, hostUrl);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ContainerOrchestrationServiceOptions other = (ContainerOrchestrationServiceOptions) obj;
        return Objects.equals(enforcementAllowlistContent, other.enforcementAllowlistContent) && enforcementEnabled == other.enforcementEnabled
                && enabled == other.enabled && Objects.equals(hostUrl, other.hostUrl);
    }

}

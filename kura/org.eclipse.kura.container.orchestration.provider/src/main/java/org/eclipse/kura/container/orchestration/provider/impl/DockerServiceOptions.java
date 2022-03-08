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

public class DockerServiceOptions {

    private static final Property<Boolean> IS_ENABLED = new Property<>("dockerService.enabled", false);
    private static final Property<String> DOCKER_HOST_URL = new Property<>("dockerService.dockerHost",
            "unix:///var/run/docker.sock");
    private static final Property<Boolean> REPOSITORY_ENABLED = new Property<>("dockerService.repository.enabled",
            false);
    private static final Property<String> REPOSITORY_URL = new Property<>("dockerService.repository.hostname", "");
    private static final Property<String> REPOSITORY_USERNAME = new Property<>("dockerService.repository.username", "");
    private static final Property<String> REPOSITORY_PASSWORD = new Property<>("dockerService.repository.password", "");

    private final boolean enabled;
    private final String hostUrl;
    private final boolean repositoryEnabled;
    private final String repositoryURL;
    private final String repositoryUsername;
    private final String repositoryPassword;

    public DockerServiceOptions(final Map<String, Object> properties) {

        if (isNull(properties)) {
            throw new IllegalArgumentException("Properties cannot be null!");
        }

        this.enabled = IS_ENABLED.get(properties);
        this.hostUrl = DOCKER_HOST_URL.get(properties);
        this.repositoryEnabled = REPOSITORY_ENABLED.get(properties);
        this.repositoryURL = REPOSITORY_URL.get(properties);
        this.repositoryUsername = REPOSITORY_USERNAME.get(properties);
        this.repositoryPassword = REPOSITORY_PASSWORD.get(properties);
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public String getHostUrl() {
        return this.hostUrl;
    }

    public boolean isRepositoryEnabled() {
        return this.repositoryEnabled;
    }

    public String getRepositoryUrl() {
        return this.repositoryURL;
    }

    public String getRepositoryUsername() {
        return this.repositoryUsername;
    }

    public String getRepositoryPassword() {
        return this.repositoryPassword;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.enabled, this.hostUrl, this.repositoryEnabled, this.repositoryURL,
                this.repositoryUsername, this.repositoryPassword);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        DockerServiceOptions other = (DockerServiceOptions) obj;
        return isEnabled() == other.isEnabled() && Objects.equals(getHostUrl(), other.getHostUrl())
                && isRepositoryEnabled() == other.isRepositoryEnabled()
                && Objects.equals(getRepositoryUrl(), other.getRepositoryUrl())
                && Objects.equals(getRepositoryUsername(), other.getRepositoryUsername())
                && Objects.equals(getRepositoryPassword(), other.getRepositoryPassword());
    }
}

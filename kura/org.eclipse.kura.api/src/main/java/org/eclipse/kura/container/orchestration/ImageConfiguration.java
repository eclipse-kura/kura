/***************************************************************************
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
 * Object which represents an Image configuration used to request the generation
 * of a new image instance
 *
 * @noimplement This interface is not intended to be implemented by clients.
 * @since 2.4
 *
 */
@ProviderType
public class ImageConfiguration {

    private String imageName;
    private String imageTag;
    private Optional<RegistryCredentials> registryCredentials;
    private int imageDownloadTimeoutSeconds = 500;

    private ImageConfiguration() {
    }

    /**
     * Returns an Image's name as a String.
     * 
     * @return
     */
    public String getImageName() {
        return this.imageName;
    }

    /**
     * Returns an Image's tag as a String.
     * 
     * @return
     */
    public String getImageTag() {
        return this.imageTag;
    }

    /**
     * Returns an Image's download timeout time as a int.
     * 
     * @return
     */
    public int getimageDownloadTimeoutSeconds() {
        return this.imageDownloadTimeoutSeconds;
    }

    /**
     * Returns the Registry credentials
     *
     * @return
     */
    public Optional<RegistryCredentials> getRegistryCredentials() {
        return this.registryCredentials;
    }

    /**
     * Creates a builder for creating a new {@link ImageConfiguration} instance.
     *
     * @return the builder.
     */
    public static ImageConfigurationBuilder builder() {
        return new ImageConfigurationBuilder();
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.imageName, this.imageTag, this.registryCredentials);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ImageConfiguration)) {
            return false;
        }
        ImageConfiguration other = (ImageConfiguration) obj;
        return Objects.equals(this.imageName, other.imageName) && Objects.equals(this.imageTag, other.imageTag)
                && Objects.equals(this.registryCredentials, other.registryCredentials);
    }

    public static final class ImageConfigurationBuilder {

        private String imageName;
        private String imageTag = "latest";
        private Optional<RegistryCredentials> registryCredentials;
        private int imageDownloadTimeoutSeconds = 500;

        public ImageConfigurationBuilder setImageName(String imageName) {
            this.imageName = imageName;
            return this;
        }

        public ImageConfigurationBuilder setImageTag(String imageTag) {
            this.imageTag = imageTag;
            return this;
        }

        public ImageConfigurationBuilder setRegistryCredentials(Optional<RegistryCredentials> registryCredentials) {
            this.registryCredentials = registryCredentials;
            return this;
        }

        public ImageConfigurationBuilder setImageDownloadTimeoutSeconds(int imageDownloadTimeoutSeconds) {
            this.imageDownloadTimeoutSeconds = imageDownloadTimeoutSeconds;
            return this;
        }

        public ImageConfiguration build() {
            ImageConfiguration result = new ImageConfiguration();

            result.imageName = requireNonNull(this.imageName, "Request Image Name cannot be null");
            result.imageTag = this.imageTag;
            result.registryCredentials = requireNonNull(this.registryCredentials,
                    "Request Registry Credentials object cannot be null.");
            result.imageDownloadTimeoutSeconds = this.imageDownloadTimeoutSeconds;

            return result;
        }

    }
}
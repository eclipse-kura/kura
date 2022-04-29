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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.osgi.annotation.versioning.ProviderType;

/**
 * Object which represents an image. Used to track created images, and images
 * that exist in the container engine.
 *
 * @noimplement This interface is not intended to be implemented by clients.
 * @since 2.4
 *
 */
@ProviderType
public class ImageInstanceDescriptor {

    private String imageName = "";
    private String imageTag = "";
    private String imageId = "";
    private String imageAuthor = "";
    private String imageArch = "";
    private long imageSize = 0;
    private Map<String, String> imageLabels = new HashMap<>();

    private ImageInstanceDescriptor() {
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
     * Returns an Image's id as a String.
     * 
     * @return
     */
    public String getImageId() {
        return imageId;
    }

    /**
     * Returns an Image's author as a String.
     * 
     * @return
     */
    public String getImageAuthor() {
        return imageAuthor;
    }

    /**
     * Returns an Image's architecture as a String.
     * 
     * @return
     */
    public String getImageArch() {
        return imageArch;
    }

    /**
     * Returns an Image's size as a long.
     * 
     * @return
     */
    public long getImageSize() {
        return imageSize;
    }

    /**
     * Returns all of an Image's tags as a Map<String,String>.
     * 
     * @return
     */
    public Map<String, String> getImageLabels() {
        return imageLabels;
    }

    /**
     * Creates a builder for creating a new {@link ImageInstanceDescriptor}
     * instance.
     *
     * @return the builder.
     */
    public static ImageInstanceDescriptorBuilder builder() {
        return new ImageInstanceDescriptorBuilder();
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.imageName, this.imageTag, this.imageId, this.imageAuthor, this.imageArch,
                this.imageSize, this.imageLabels);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ImageInstanceDescriptor)) {
            return false;
        }
        ImageInstanceDescriptor other = (ImageInstanceDescriptor) obj;
        return Objects.equals(this.imageName, other.imageName) && Objects.equals(this.imageTag, other.imageTag)
                && Objects.equals(this.imageId, other.imageId) && Objects.equals(this.imageAuthor, other.imageAuthor)
                && Objects.equals(this.imageArch, other.imageArch) && Objects.equals(this.imageSize, other.imageSize)
                && Objects.equals(this.imageLabels, other.imageLabels);
    }

    public static final class ImageInstanceDescriptorBuilder {

        private String imageName;
        private String imageTag;
        private String imageId;
        private String imageAuthor;
        private String imageArch;
        private long imageSize;
        private Map<String, String> imageLabels = new HashMap<>();

        public ImageInstanceDescriptorBuilder setImageName(String imageName) {
            this.imageName = nullToEmpty(imageName);
            return this;
        }

        public ImageInstanceDescriptorBuilder setImageTag(String imageTag) {
            this.imageTag = nullToEmpty(imageTag);
            return this;
        }

        public ImageInstanceDescriptorBuilder setImageId(String imageId) {
            this.imageId = nullToEmpty(imageId);
            return this;
        }

        public ImageInstanceDescriptorBuilder setImageAuthor(String imageAuthor) {
            this.imageAuthor = nullToEmpty(imageAuthor);
            return this;
        }

        public ImageInstanceDescriptorBuilder setImageArch(String imageArch) {
            this.imageArch = nullToEmpty(imageArch);
            return this;
        }

        public ImageInstanceDescriptorBuilder setimageSize(long imageSize) {
            this.imageSize = imageSize;
            return this;
        }

        public ImageInstanceDescriptorBuilder setImageLabels(Map<String, String> imageLabels) {
            this.imageLabels = imageLabels;
            return this;
        }

        public ImageInstanceDescriptor build() {
            ImageInstanceDescriptor result = new ImageInstanceDescriptor();

            result.imageName = this.imageName;
            result.imageTag = this.imageTag;
            result.imageId = requireNonNull(this.imageId, "Image ID cannot be null");
            result.imageAuthor = this.imageAuthor;
            result.imageArch = this.imageArch;
            result.imageSize = this.imageSize;
            result.imageLabels = requireNonNull(this.imageLabels, "ImageLabels must not be null");

            return result;
        }

        private String nullToEmpty(String input) {
            if (input == null) {
                return "";
            } else {
                return input;
            }
        }

    }
}
/*******************************************************************************
 * Copyright (c) 2022 Eurotech and/or its affiliates and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https:www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Eurotech
 *******************************************************************************/

package org.eclipse.kura.core.inventory.resources;

import org.eclipse.kura.container.orchestration.ImageInstanceDescriptor;
import org.eclipse.kura.system.SystemResourceInfo;
import org.eclipse.kura.system.SystemResourceType;

public class ContainerImage extends SystemResourceInfo {

    private String imageName = "";
    private String imageTag = "";
    private String imageId = "";
    private String imageAuthor = "";
    private String imageArch = "";
    private long imageSize = 0;

    public ContainerImage(String name, String version) {
        super(name, version, SystemResourceType.CONTAINER_IMAGE);
        this.imageName = name;
        this.imageTag = version;
    }

    public ContainerImage(ImageInstanceDescriptor image) {
        super(image.getImageName(), image.getImageTag(), SystemResourceType.CONTAINER_IMAGE);

        this.imageName = image.getImageName();
        this.imageTag = image.getImageTag();
        this.imageId = image.getImageId();
        this.imageAuthor = image.getImageAuthor();
        this.imageArch = image.getImageArch();
        this.imageSize = image.getImageSize();
    }

    public String getImageName() {
        return imageName;
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
    }

    public String getImageTag() {
        return imageTag;
    }

    public void setImageTag(String imageTag) {
        this.imageTag = imageTag;
    }

    public String getImageId() {
        return imageId;
    }

    public void setImageId(String imageId) {
        this.imageId = imageId;
    }

    public String getImageAuthor() {
        return imageAuthor;
    }

    public void setImageAuthor(String imageAuthor) {
        this.imageAuthor = imageAuthor;
    }

    public String getImageArch() {
        return imageArch;
    }

    public void setImageArch(String imageArch) {
        this.imageArch = imageArch;
    }

    public long getImageSize() {
        return imageSize;
    }

    public void setImageSize(long imageSize) {
        this.imageSize = imageSize;
    }

}

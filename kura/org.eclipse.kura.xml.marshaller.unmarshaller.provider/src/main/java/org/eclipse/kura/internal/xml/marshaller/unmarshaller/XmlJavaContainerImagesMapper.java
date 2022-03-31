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
package org.eclipse.kura.internal.xml.marshaller.unmarshaller;

import java.util.List;

import org.eclipse.kura.core.inventory.resources.ContainerImage;
import org.eclipse.kura.core.inventory.resources.ContainerImages;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class XmlJavaContainerImagesMapper implements XmlJavaDataMapper {

    private static final String IMAGES = "images";
    private static final String CONTAINERS_IMAGES = "image";
    private static final String CONTAINERS_IMAGES_NAME = "name";
    private static final String CONTAINERS_IMAGES_VERSION = "version";

    @Override
    public Element marshal(Document doc, Object object) throws Exception {
        Element packages = doc.createElement(IMAGES);
        doc.appendChild(packages);

        ContainerImages images = (ContainerImages) object;
        List<ContainerImage> imageList = images.getContainerImages();

        for (ContainerImage ximage : imageList) {
            Element packageInstalled = doc.createElement(CONTAINERS_IMAGES);
            marshalContainer(doc, ximage, packageInstalled);
            packages.appendChild(packageInstalled);
        }
        return packages;
    }

    @Override
    public <T> T unmarshal(Document doc) throws Exception {
        return null;
    }

    //
    // Marshaller's private methods
    //
    private static void marshalContainer(Document doc, ContainerImage image, Element packageInstalled) {
        // Extract data from XmlDeploymentPackage
        String imageName = image.getName();
        String imageVersion = image.getVersion();

        // Create xml elements
        if (imageName != null && !imageName.trim().isEmpty()) {
            Element name = doc.createElement(CONTAINERS_IMAGES_NAME);
            name.setTextContent(imageName);
            packageInstalled.appendChild(name);
        }

        if (imageVersion != null && !imageVersion.trim().isEmpty()) {
            Element version = doc.createElement(CONTAINERS_IMAGES_VERSION);
            version.setTextContent(imageVersion);
            packageInstalled.appendChild(version);
        }
    }
}

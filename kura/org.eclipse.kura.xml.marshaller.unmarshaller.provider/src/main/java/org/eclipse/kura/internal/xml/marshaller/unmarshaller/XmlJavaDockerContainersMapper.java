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

import org.eclipse.kura.core.inventory.resources.DockerContainer;
import org.eclipse.kura.core.inventory.resources.DockerContainers;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class XmlJavaDockerContainersMapper implements XmlJavaDataMapper {

    private static final String CONTAINERS = "containers";
    private static final String CONTAINERS_CONTAINER = "container";
    private static final String CONTAINERS_CONTAINER_NAME = "name";
    private static final String CONTAINERS_CONTAINER_VERSION = "version";
    private static final String CONTAINERS_CONTAINER_STATE = "state";

    @Override
    public Element marshal(Document doc, Object object) throws Exception {
        Element packages = doc.createElement(CONTAINERS);
        doc.appendChild(packages);

        DockerContainers containers = (DockerContainers) object;
        List<DockerContainer> containerList = containers.getDockerContainers();

        for (DockerContainer xcontainer : containerList) {
            Element packageInstalled = doc.createElement(CONTAINERS_CONTAINER);
            marshalContainer(doc, xcontainer, packageInstalled);
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
    private static void marshalContainer(Document doc, DockerContainer container, Element packageInstalled) {
        // Extract data from XmlDeploymentPackage
        String containerName = container.getName();
        String containerVersion = container.getVersion();
        String containerState = container.getFrameworkContainerState();

        // Create xml elements
        if (containerName != null && !containerName.trim().isEmpty()) {
            Element name = doc.createElement(CONTAINERS_CONTAINER_NAME);
            name.setTextContent(containerName);
            packageInstalled.appendChild(name);
        }

        if (containerVersion != null && !containerVersion.trim().isEmpty()) {
            Element version = doc.createElement(CONTAINERS_CONTAINER_VERSION);
            version.setTextContent(containerVersion);
            packageInstalled.appendChild(version);
        }

        if (containerState != null && !containerState.trim().isEmpty()) {
            Element state = doc.createElement(CONTAINERS_CONTAINER_STATE);
            state.setTextContent(containerState);
            packageInstalled.appendChild(state);
        }
    }
}

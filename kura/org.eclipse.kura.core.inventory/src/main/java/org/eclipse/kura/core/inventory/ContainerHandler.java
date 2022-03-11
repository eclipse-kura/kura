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
package org.eclipse.kura.core.inventory;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloudconnection.message.KuraMessage;
import org.eclipse.kura.container.orchestration.provider.ContainerDescriptor;
import org.eclipse.kura.container.orchestration.provider.DockerService;
import org.eclipse.kura.core.inventory.resources.DockerContainer;
import org.eclipse.kura.core.inventory.resources.DockerContainers;
import org.eclipse.kura.marshalling.Marshaller;
import org.eclipse.kura.marshalling.Unmarshaller;
import org.eclipse.kura.message.KuraPayload;
import org.eclipse.kura.message.KuraResponsePayload;
import org.eclipse.kura.system.SystemResourceInfo;
import org.eclipse.kura.system.SystemResourceType;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ContainerHelper {

    private static final Logger logger = LoggerFactory.getLogger(ContainerHelper.class);

    private final DockerService dockerService;
    private final Marshaller marshaller;
    private final Unmarshaller unmarshaller;

    public ContainerHelper(Object dockerService, final Marshaller marshaller, final Unmarshaller unmarshaller) {
        this.dockerService = (DockerService) dockerService;
        this.marshaller = marshaller;
        this.unmarshaller = unmarshaller;
    }

    void fillContainerInventoryData(List<SystemResourceInfo> inventory) {
        try {
            logger.info("Creating docker invenetory");
            List<ContainerDescriptor> containers = this.dockerService.listRegisteredContainers();
            containers.stream().forEach(
                    container -> inventory.add(new SystemResourceInfo(container.getContainerName().replace("/", ""),
                            container.getContainerImage() + ":" + container.getContainerImageTag().split(":")[0],
                            SystemResourceType.DOCKER)));
        } catch (Exception e) {
            logger.error("Could not connect to docker");
        }
    }

    KuraPayload doGetDockerContainers(final BundleContext bundleContext) {

        KuraResponsePayload respPayload = new KuraResponsePayload(KuraResponsePayload.RESPONSE_CODE_OK);
        try {
            List<ContainerDescriptor> containers = this.dockerService.listRegisteredContainers();

            List<DockerContainer> containersList = new ArrayList<>();
            containers.stream().forEach(p -> containersList.add(new DockerContainer(p)));

            DockerContainers dockerContainers = new DockerContainers(containersList);
            String s = this.marshaller.marshal(dockerContainers);
            respPayload.setTimestamp(new Date());
            respPayload.setBody(s.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            logger.error(InventoryHandlerV1.ERROR_GETTING_RESOURCE, InventoryHandlerV1.RESOURCE_SYSTEM_PACKAGES, e);
            respPayload.setResponseCode(KuraResponsePayload.RESPONSE_CODE_ERROR);
        }

        return respPayload;
    }

    KuraMessage doStopDockerContainer(KuraMessage reqMessage) throws KuraException {
        this.dockerService.stopContainer(findFirstMatchingContainer(extractContainerRef(reqMessage)));
        return InventoryHandlerV1.success();
    }

    KuraMessage doStartDockerContainer(KuraMessage reqMessage) throws KuraException {
        this.dockerService.startContainer(findFirstMatchingContainer(extractContainerRef(reqMessage)));

        return InventoryHandlerV1.success();
    }

    private ContainerDescriptor extractContainerRef(final KuraMessage message) throws KuraException {
        final KuraPayload payload = message.getPayload();

        final byte[] body = payload.getBody();

        if (body == null) {
            logger.warn("missing message body");
            throw new KuraException(KuraErrorCode.BAD_REQUEST);
        }

        DockerContainer dc = this.unmarshaller
                .unmarshal(new String(message.getPayload().getBody(), StandardCharsets.UTF_8), DockerContainer.class);

        try {
            List<ContainerDescriptor> containerList = dockerService.listRegisteredContainers();

            for (ContainerDescriptor container : containerList) {
                if (container.getContainerName().equals(dc.getContainerName())) {
                    return container;
                }
            }
            logger.warn("Failed to find container");
            throw new KuraException(KuraErrorCode.BAD_REQUEST);
        } catch (Exception e) {
            logger.warn("failed to access docker service");
            throw new KuraException(KuraErrorCode.BAD_REQUEST);
        }

    }

    private ContainerDescriptor findFirstMatchingContainer(final ContainerDescriptor ref) throws KuraException {
        for (final ContainerDescriptor container : dockerService.listRegisteredContainers()) {
            if (container.getContainerName().equals(ref.getContainerName())) {
                return container;
            }
        }

        throw new KuraException(KuraErrorCode.NOT_FOUND);
    }

}

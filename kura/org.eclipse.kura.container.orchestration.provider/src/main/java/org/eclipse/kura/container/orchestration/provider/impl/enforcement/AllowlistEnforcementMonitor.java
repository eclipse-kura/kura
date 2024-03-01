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

package org.eclipse.kura.container.orchestration.provider.impl.enforcement;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.container.orchestration.provider.impl.ContainerOrchestrationServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dockerjava.api.async.ResultCallbackTemplate;
import com.github.dockerjava.api.model.Event;

public class AllowlistEnforcementMonitor extends ResultCallbackTemplate<AllowlistEnforcementMonitor, Event> {

    private static final Logger logger = LoggerFactory.getLogger(AllowlistEnforcementMonitor.class);
    private static final String ENFORCEMENT_SUCCESS = "Enforcement allowlist contains image digests {}...container {} is starting";
    private static final String ENFORCEMENT_FAILURE = "Enforcement allowlist doesn't contain image digests...container {} will be stopped";
    private final List<String> enforcementAllowlistContent;
    private final ContainerOrchestrationServiceImpl orchestrationServiceImpl;

    public AllowlistEnforcementMonitor(String allowlistContent,
            ContainerOrchestrationServiceImpl containerOrchestrationService) {

        this.enforcementAllowlistContent = Arrays
                .asList(allowlistContent.replaceAll("\\s", "").replace("\n", "").trim().split(","));
        this.orchestrationServiceImpl = containerOrchestrationService;
    }

    @Override
    public void onNext(Event item) {
        try {
            implementAllowlistEnforcement(item.getId());
        } catch (KuraException e) {
            logger.error("Error during container stopping process");
        }
    }

    private void implementAllowlistEnforcement(String id) throws KuraException {

        List<String> digestsList = this.orchestrationServiceImpl
                .getImageDigestsByContainerName(getContainerNameById(id));

        List<String> digestIntersection = this.enforcementAllowlistContent.stream().distinct()
                .filter(digestsList::contains).collect(Collectors.toList());

        if (!digestIntersection.isEmpty()) {
            logger.info(ENFORCEMENT_SUCCESS, digestIntersection, id);
        } else {
            logger.error(ENFORCEMENT_FAILURE, id);
            this.orchestrationServiceImpl.stopContainer(id);
            this.orchestrationServiceImpl.deleteContainer(id);
        }
    }

    private String getContainerNameById(String id) {
        return this.orchestrationServiceImpl.listContainerDescriptors().stream()
                .filter(container -> container.getContainerId().equals(id)).findFirst()
                .map(container -> container.getContainerName()).orElse(null);
    }

}

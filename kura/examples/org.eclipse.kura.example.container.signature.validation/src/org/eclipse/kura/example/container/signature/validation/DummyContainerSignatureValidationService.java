/*******************************************************************************
 * Copyright (c) 2024 Eurotech and/or its affiliates and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Eurotech
 ******************************************************************************/
package org.eclipse.kura.example.container.signature.validation;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.configuration.Password;
import org.eclipse.kura.container.orchestration.ImageInstanceDescriptor;
import org.eclipse.kura.container.signature.ContainerSignatureValidationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DummyContainerSignatureValidationService
        implements ContainerSignatureValidationService, ConfigurableComponent {

    private static final Logger logger = LoggerFactory.getLogger(DummyContainerSignatureValidationService.class);
    private static final String SERVICE_NAME = "DummyContainerSignatureValidationService";
    private static final String PROPERTY_NAME = "manual.setValidationOutcome";
    private Map<String, String> validationResults = new HashMap<>();

    protected void activate(Map<String, Object> properties) {
        logger.info("Activate {}...", SERVICE_NAME);
        updated(properties);
    }

    public void updated(Map<String, Object> properties) {
        logger.info("Update {}...", SERVICE_NAME);

        if (Objects.nonNull(properties) && !properties.isEmpty() && properties.containsKey(PROPERTY_NAME)) {
            populateValidationResults((String) properties.get(PROPERTY_NAME));
        }
    }

    protected void deactivate() {
        logger.info("Deactivate {}...", SERVICE_NAME);
    }

    @Override
    public boolean verify(String imageName, String imageReference, String trustAnchor, boolean verifyInTransparencyLog)
            throws KuraException {
        logger.info("Validating signature for {}:{}", imageName, imageReference);
        return verify(imageName, imageReference);
    }

    @Override
    public boolean verify(String imageName, String imageReference, String trustAnchor, boolean verifyInTransparencyLog,
            String registryUsername, Password registryPassword) throws KuraException {
        logger.info("Validating signature for {}:{} using authenticated registry", imageName, imageReference);
        return verify(imageName, imageReference);
    }

    @Override
    public boolean verify(ImageInstanceDescriptor imageDescriptor, String trustAnchor, boolean verifyInTransparencyLog)
            throws KuraException {
        logger.info("Validating signature for {}:{}", imageDescriptor.getImageName(), imageDescriptor.getImageId());
        return verify(imageDescriptor.getImageName(), imageDescriptor.getImageTag());
    }

    @Override
    public boolean verify(ImageInstanceDescriptor imageDescriptor, String trustAnchor, boolean verifyInTransparencyLog,
            String registryUsername, Password registryPassword) throws KuraException {
        logger.info("Validating signature for {}:{} using authenticated registry", imageDescriptor.getImageName(),
                imageDescriptor.getImageTag());
        return verify(imageDescriptor.getImageName(), imageDescriptor.getImageTag());
    }

    public int getValidationResultsSize() {
        return this.validationResults.size();
    }

    public String getValidationResultsFor(String imageName, String imageTag) {
        return this.validationResults.get(String.format("%s:%s", imageName, imageTag));
    }

    private boolean verify(String imageName, String imageTag) {
        String imageKey = String.format("%s:%s", imageName, imageTag);

        // WIP: When PR#5136 gets merged we need to return the image digest too

        return validationResults.containsKey(imageKey);
    }

    private void populateValidationResults(String raw) {
        if (raw.isEmpty()) {
            this.validationResults = new HashMap<>();
            return;
        }

        Map<String, String> newValidationResults = new HashMap<>();

        String[] entries = raw.split("\\r?\\n|\\r"); // Split on newlines
        int count = 0;
        for (String entry : entries) {
            count++;
            String[] params = entry.split("@");
            if (params.length != 2) {
                throw new IllegalArgumentException(String.format("Error parsing line %2d: \"%s\"", count, entry));
            }
            newValidationResults.put(params[0], params[1]);
        }

        this.validationResults = newValidationResults;
    }
}

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
import org.eclipse.kura.container.orchestration.ImageInstanceDescriptor;
import org.eclipse.kura.container.orchestration.RegistryCredentials;
import org.eclipse.kura.container.signature.ContainerSignatureValidationService;
import org.eclipse.kura.container.signature.ValidationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DummyContainerSignatureValidationService
        implements ContainerSignatureValidationService, ConfigurableComponent {

    private static final Logger logger = LoggerFactory.getLogger(DummyContainerSignatureValidationService.class);

    private static final String SERVICE_NAME = "DummyContainerSignatureValidationService";
    private static final String PROPERTY_NAME = "manual.setValidationOutcome";
    private static final ValidationResult FAILED_VALIDATION = new ValidationResult();

    private Map<String, String> configuredValidationResults = new HashMap<>();

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
    public ValidationResult verify(String imageName, String imageReference, String trustAnchor,
            boolean verifyInTransparencyLog) throws KuraException {
        logger.info("Validating signature for {}:{}", imageName, imageReference);
        return verify(imageName, imageReference);
    }

    @Override
    public ValidationResult verify(String imageName, String imageReference, String trustAnchor,
            boolean verifyInTransparencyLog, RegistryCredentials credentials) throws KuraException {
        logger.info("Validating signature for {}:{} using authenticated registry", imageName, imageReference);
        return verify(imageName, imageReference);
    }

    @Override
    public ValidationResult verify(ImageInstanceDescriptor imageDescriptor, String trustAnchor,
            boolean verifyInTransparencyLog) throws KuraException {
        logger.info("Validating signature for {}:{}", imageDescriptor.getImageName(), imageDescriptor.getImageId());
        return verify(imageDescriptor.getImageName(), imageDescriptor.getImageTag());
    }

    @Override
    public ValidationResult verify(ImageInstanceDescriptor imageDescriptor, String trustAnchor,
            boolean verifyInTransparencyLog, RegistryCredentials credentials) throws KuraException {
        logger.info("Validating signature for {}:{} using authenticated registry", imageDescriptor.getImageName(),
                imageDescriptor.getImageTag());
        return verify(imageDescriptor.getImageName(), imageDescriptor.getImageTag());
    }

    public int getConfiguredValidationResultsSize() {
        return this.configuredValidationResults.size();
    }

    public String getConfiguredValidationResultsFor(String imageName, String imageTag) {
        return this.configuredValidationResults.get(String.format("%s:%s", imageName, imageTag));
    }

    private ValidationResult verify(String imageName, String imageTag) {
        String imageKey = String.format("%s:%s", imageName, imageTag);

        if (!this.configuredValidationResults.containsKey(imageKey)) {
            return FAILED_VALIDATION;
        }

        return new ValidationResult(true, this.configuredValidationResults.get(imageKey));
    }

    private void populateValidationResults(String raw) {
        if (raw.isEmpty()) {
            this.configuredValidationResults = new HashMap<>();
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

        this.configuredValidationResults = newValidationResults;
    }
}

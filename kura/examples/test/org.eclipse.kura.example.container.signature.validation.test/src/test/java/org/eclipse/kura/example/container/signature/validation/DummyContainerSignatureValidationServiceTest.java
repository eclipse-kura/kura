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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.Password;
import org.eclipse.kura.container.orchestration.ImageInstanceDescriptor;
import org.junit.Test;

public class DummyContainerSignatureValidationServiceTest {

    private DummyContainerSignatureValidationService containerSignatureValidationService = new DummyContainerSignatureValidationService();
    private Map<String, Object> properties = new HashMap<>();

    private static final String PROPERTY_NAME = "manual.setValidationOutcome";
    private static final String IMAGE_NAME = "imageName";
    private static final String IMAGE_TAG = "imageTag";
    private static final String IMAGE_ID = "imageId";
    private static final String TRUST_ANCHOR = "trustAnchor";
    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";

    private boolean validationResult = false;
    private Exception occurredException;
    private ImageInstanceDescriptor imageDescriptor;

    @Test
    public void verifyReturnsFailureWithEmptyConfiguration() {
        givenContainerSignatureValidationServiceWith(this.properties);

        whenVerifyIsCalledWith(IMAGE_NAME, IMAGE_TAG, TRUST_ANCHOR, false);

        thenNoExceptionOccurred();
        thenVerificationResultIs(false);
    }

    @Test
    public void verifyReturnsFailureWithFalseConfiguration() {
        givenPropertyWith(PROPERTY_NAME, false);
        givenContainerSignatureValidationServiceWith(this.properties);

        whenVerifyIsCalledWith(IMAGE_NAME, IMAGE_TAG, TRUST_ANCHOR, false);

        thenNoExceptionOccurred();
        thenVerificationResultIs(false);
    }

    @Test
    public void verifyReturnsSuccessWithTrueConfiguration() {
        givenPropertyWith(PROPERTY_NAME, true);
        givenContainerSignatureValidationServiceWith(this.properties);

        whenVerifyIsCalledWith(IMAGE_NAME, IMAGE_TAG, TRUST_ANCHOR, false);

        thenNoExceptionOccurred();
        thenVerificationResultIs(true);
    }

    @Test
    public void verifyWithAuthReturnsFailureWithFalseConfiguration() {
        givenPropertyWith(PROPERTY_NAME, false);
        givenContainerSignatureValidationServiceWith(this.properties);

        whenVerifyWithAuthIsCalledWith(IMAGE_NAME, IMAGE_TAG, TRUST_ANCHOR, false, USERNAME, PASSWORD);

        thenNoExceptionOccurred();
        thenVerificationResultIs(false);
    }

    @Test
    public void verifyWithAuthReturnsSuccessWithTrueConfiguration() {
        givenPropertyWith(PROPERTY_NAME, true);
        givenContainerSignatureValidationServiceWith(this.properties);

        whenVerifyWithAuthIsCalledWith(IMAGE_NAME, IMAGE_TAG, TRUST_ANCHOR, false, USERNAME, PASSWORD);

        thenNoExceptionOccurred();
        thenVerificationResultIs(true);
    }

    @Test
    public void verifyWithImageReturnsFailureWithFalseConfiguration() {
        givenPropertyWith(PROPERTY_NAME, false);
        givenImageInstanceDescriptorWith(IMAGE_NAME, IMAGE_TAG, IMAGE_ID);
        givenContainerSignatureValidationServiceWith(this.properties);

        whenVerifyImageInstanceDescriptorIsCalledWith(this.imageDescriptor, TRUST_ANCHOR, false);

        thenNoExceptionOccurred();
        thenVerificationResultIs(false);
    }

    @Test
    public void verifyWithImageReturnsFailureWithTrueConfiguration() {
        givenPropertyWith(PROPERTY_NAME, true);
        givenImageInstanceDescriptorWith(IMAGE_NAME, IMAGE_TAG, IMAGE_ID);
        givenContainerSignatureValidationServiceWith(this.properties);

        whenVerifyImageInstanceDescriptorIsCalledWith(this.imageDescriptor, TRUST_ANCHOR, false);

        thenNoExceptionOccurred();
        thenVerificationResultIs(true);
    }

    @Test
    public void verifyWithImageWithAuthReturnsFailureWithFalseConfiguration() {
        givenPropertyWith(PROPERTY_NAME, false);
        givenImageInstanceDescriptorWith(IMAGE_NAME, IMAGE_TAG, IMAGE_ID);
        givenContainerSignatureValidationServiceWith(this.properties);

        whenVerifyImageInstanceDescriptorWithAuthIsCalledWith(this.imageDescriptor, TRUST_ANCHOR, false, USERNAME,
                PASSWORD);

        thenNoExceptionOccurred();
        thenVerificationResultIs(false);
    }

    @Test
    public void verifyWithImageReturnsWithAuthFailureWithTrueConfiguration() {
        givenPropertyWith(PROPERTY_NAME, true);
        givenImageInstanceDescriptorWith(IMAGE_NAME, IMAGE_TAG, IMAGE_ID);
        givenContainerSignatureValidationServiceWith(this.properties);

        whenVerifyImageInstanceDescriptorWithAuthIsCalledWith(this.imageDescriptor, TRUST_ANCHOR, false, USERNAME,
                PASSWORD);

        thenNoExceptionOccurred();
        thenVerificationResultIs(true);
    }

    /*
     * GIVEN
     */
    private void givenContainerSignatureValidationServiceWith(Map<String, Object> configuration) {
        this.containerSignatureValidationService.activate(configuration);
    }

    private void givenPropertyWith(String propertyName, Object value) {
        this.properties.put(propertyName, value);
    }

    private void givenImageInstanceDescriptorWith(String imageName, String imageTag, String imageId) {
        this.imageDescriptor = ImageInstanceDescriptor.builder().setImageName(imageName).setImageTag(imageTag)
                .setImageId(imageId).setImageLabels(new HashMap<>()).build();
    }

    /*
     * WHEN
     */
    private void whenVerifyIsCalledWith(String imageName, String imageTag, String trustAnchor, boolean isVerify) {
        try {
            this.validationResult = this.containerSignatureValidationService.verify(imageName, imageTag, trustAnchor,
                    isVerify);
        } catch (KuraException e) {
            this.occurredException = e;
        }
    }

    private void whenVerifyWithAuthIsCalledWith(String imageName, String imageTag, String trustAnchor, boolean isVerify,
            String user, String pass) {
        try {
            this.validationResult = this.containerSignatureValidationService.verify(imageName, imageTag, trustAnchor,
                    isVerify, user, new Password(pass));
        } catch (KuraException e) {
            this.occurredException = e;
        }
    }

    private void whenVerifyImageInstanceDescriptorIsCalledWith(ImageInstanceDescriptor descriptor, String trustAnchor,
            boolean isVerify) {
        try {
            this.validationResult = this.containerSignatureValidationService.verify(descriptor, trustAnchor, isVerify);
        } catch (KuraException e) {
            this.occurredException = e;
        }
    }

    private void whenVerifyImageInstanceDescriptorWithAuthIsCalledWith(ImageInstanceDescriptor descriptor,
            String trustAnchor, boolean isVerify, String user, String pass) {
        try {
            this.validationResult = this.containerSignatureValidationService.verify(descriptor, trustAnchor, isVerify,
                    user, new Password(pass));
        } catch (KuraException e) {
            this.occurredException = e;
        }
    }

    /*
     * THEN
     */
    private void thenVerificationResultIs(boolean expectedResult) {
        assertEquals(expectedResult, this.validationResult);
    }

    private void thenNoExceptionOccurred() {
        String errorMessage = "Empty message";
        if (Objects.nonNull(this.occurredException)) {
            StringWriter sw = new StringWriter();
            this.occurredException.printStackTrace(new PrintWriter(sw));

            errorMessage = String.format("No exception expected, \"%s\" found. Caused by: %s",
                    this.occurredException.getClass().getName(), sw.toString());
        }

        assertNull(errorMessage, this.occurredException);
    }
}

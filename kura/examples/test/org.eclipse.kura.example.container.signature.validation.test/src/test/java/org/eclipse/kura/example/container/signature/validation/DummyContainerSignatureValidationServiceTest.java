package org.eclipse.kura.example.container.signature.validation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.eclipse.kura.KuraException;
import org.junit.Test;

public class DummyContainerSignatureValidationServiceTest {

    private DummyContainerSignatureValidationService containerSignatureValidationService = new DummyContainerSignatureValidationService();
    private Map<String, Object> properties = new HashMap<>();

    private boolean validationResult = false;
    private Exception occurredException;

    @Test
    public void verifyReturnsFailureWithEmptyConfiguration() {
        givenContainerSignatureValidationServiceWith(this.properties);

        whenVerifyIsCalledWith("image", "tag", "trustAnchor", false);

        thenNoExceptionOccurred();
        thenVerificationResultIs(false);
    }

    @Test
    public void verifyReturnsFailureWithFalseConfiguration() {
        givenPropertyWith("manual.setValidationOutcome", false);
        givenContainerSignatureValidationServiceWith(this.properties);

        whenVerifyIsCalledWith("image", "tag", "trustAnchor", false);

        thenNoExceptionOccurred();
        thenVerificationResultIs(false);
    }

    @Test
    public void verifyReturnsSuccessWithTrueConfiguration() {
        givenPropertyWith("manual.setValidationOutcome", true);
        givenContainerSignatureValidationServiceWith(this.properties);

        whenVerifyIsCalledWith("image", "tag", "trustAnchor", false);

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

    /*
     * WHEN
     */
    private void whenVerifyIsCalledWith(String string, String string2, String string3, boolean isVerify) {
        try {
            this.validationResult = this.containerSignatureValidationService.verify(string, string2, string3, isVerify);
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
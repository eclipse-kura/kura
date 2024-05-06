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
 *******************************************************************************/
package org.eclipse.kura.core.identity.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.testutil.service.ServiceUtil;
import org.eclipse.kura.identity.PasswordStrengthRequirements;
import org.eclipse.kura.identity.PasswordStrengthVerificationService;
import org.junit.Test;

public class PasswordStrengthVerificationServiceImplTest extends IdentityServiceTestBase {

    @Test
    public void shouldRejectTooShortPassword() {
        givenConsoleOptions("new.password.min.length", 5, "new.password.require.digits", true,
                "new.password.require.special.characters", true, "new.password.require.both.cases", true);

        whenPasswordIsValidated("As#1");

        thenExceptionIsThrown(KuraException.class);
    }

    @Test
    public void shouldAcceptPasswordLongEnough() {
        givenConsoleOptions("new.password.min.length", 3, "new.password.require.digits", false,
                "new.password.require.special.characters", false, "new.password.require.both.cases", false);

        whenPasswordIsValidated("abcd");

        thenNoExceptionIsThrown();
    }

    @Test
    public void shouldRejectPasswordWithoutDigits() {
        givenConsoleOptions("new.password.min.length", 3, "new.password.require.digits", true,
                "new.password.require.special.characters", true, "new.password.require.both.cases", true);

        whenPasswordIsValidated("Abc#");

        thenExceptionIsThrown(KuraException.class);
    }

    @Test
    public void shouldAcceptPasswordWithDigits() {
        givenConsoleOptions("new.password.min.length", 3, "new.password.require.digits", false,
                "new.password.require.special.characters", false, "new.password.require.both.cases", false);

        whenPasswordIsValidated("abc1");

        thenNoExceptionIsThrown();
    }

    @Test
    public void shouldRejectPasswordWithoutSpecialCharacters() {
        givenConsoleOptions("new.password.min.length", 3, "new.password.require.digits", true,
                "new.password.require.special.characters", true, "new.password.require.both.cases", true);

        whenPasswordIsValidated("Abc1");

        thenExceptionIsThrown(KuraException.class);
    }

    @Test
    public void shouldAcceptPasswordWitSpecialCharacters() {
        givenConsoleOptions("new.password.min.length", 3, "new.password.require.digits", false,
                "new.password.require.special.characters", true, "new.password.require.both.cases", false);

        whenPasswordIsValidated("abc@");

        thenNoExceptionIsThrown();
    }

    @Test
    public void shouldRejectPasswordWithoutBothCases() {
        givenConsoleOptions("new.password.min.length", 3, "new.password.require.digits", true,
                "new.password.require.special.characters", true, "new.password.require.both.cases", true);

        whenPasswordIsValidated("ab#1");

        thenExceptionIsThrown(KuraException.class);
    }

    @Test
    public void shouldAcceptPasswordWithBothCases() {
        givenConsoleOptions("new.password.min.length", 3, "new.password.require.digits", false,
                "new.password.require.special.characters", false, "new.password.require.both.cases", true);

        whenPasswordIsValidated("aBcD");

        thenNoExceptionIsThrown();
    }

    @Test
    public void shouldAcceptPasswordSatisfyingAllRequirements() {
        givenConsoleOptions("new.password.min.length", 4, "new.password.require.digits", true,
                "new.password.require.special.characters", true, "new.password.require.both.cases", true);

        whenPasswordIsValidated("aBcD1#");

        thenNoExceptionIsThrown();
    }

    @Test
    public void shouldReturnMinimumPasswordLength() {
        givenConsoleOptions("new.password.min.length", 3, "new.password.require.digits", false,
                "new.password.require.special.characters", false, "new.password.require.both.cases", false);

        whenPasswordRequirementsAreObtained();

        thenNoExceptionIsThrown();
        thenMinimumPasswordLengthIs(3);
    }

    @Test
    public void shouldReturnRequireDigitsTrue() {
        givenConsoleOptions("new.password.min.length", 3, "new.password.require.digits", true,
                "new.password.require.special.characters", false, "new.password.require.both.cases", false);

        whenPasswordRequirementsAreObtained();

        thenNoExceptionIsThrown();
        thenDigitsAreRequired(true);
    }

    @Test
    public void shouldReturnRequireDigitsFalse() {
        givenConsoleOptions("new.password.min.length", 3, "new.password.require.digits", false,
                "new.password.require.special.characters", false, "new.password.require.both.cases", false);

        whenPasswordRequirementsAreObtained();

        thenNoExceptionIsThrown();
        thenDigitsAreRequired(false);
    }

    @Test
    public void shouldReturnRequireSpecialCharactersTrue() {
        givenConsoleOptions("new.password.min.length", 3, "new.password.require.digits", false,
                "new.password.require.special.characters", true, "new.password.require.both.cases", false);

        whenPasswordRequirementsAreObtained();

        thenNoExceptionIsThrown();
        thenSpecialCharactersAreRequired(true);
    }

    @Test
    public void shouldReturnRequireSpecialCharactersFalse() {
        givenConsoleOptions("new.password.min.length", 3, "new.password.require.digits", false,
                "new.password.require.special.characters", false, "new.password.require.both.cases", false);

        whenPasswordRequirementsAreObtained();

        thenNoExceptionIsThrown();
        thenSpecialCharactersAreRequired(false);
    }

    @Test
    public void shouldReturnRequireBothCasesTrue() {
        givenConsoleOptions("new.password.min.length", 3, "new.password.require.digits", false,
                "new.password.require.special.characters", false, "new.password.require.both.cases", true);

        whenPasswordRequirementsAreObtained();

        thenNoExceptionIsThrown();
        thenBothCasesAreRequired(true);
    }

    @Test
    public void shouldReturnRequireBothCasesFalse() {
        givenConsoleOptions("new.password.min.length", 3, "new.password.require.digits", false,
                "new.password.require.special.characters", false, "new.password.require.both.cases", false);

        whenPasswordRequirementsAreObtained();

        thenNoExceptionIsThrown();
        thenBothCasesAreRequired(false);
    }

    private final PasswordStrengthVerificationService passwordStrengthVerificationService;

    private Optional<Exception> exception = Optional.empty();
    private Optional<PasswordStrengthRequirements> requirements = Optional.empty();

    public PasswordStrengthVerificationServiceImplTest() {
        super();
        try {

            this.passwordStrengthVerificationService = ServiceUtil
                    .trackService(PasswordStrengthVerificationService.class, Optional.empty())
                    .get(30, TimeUnit.SECONDS);

        } catch (final Exception e) {
            fail("failed to track ConfigurationService");
            throw new IllegalStateException("unreachable");
        }
    }

    private void whenPasswordIsValidated(final String password) {
        try {
            this.passwordStrengthVerificationService.checkPasswordStrength(password.toCharArray());
        } catch (KuraException e) {
            this.exception = Optional.of(e);
        }
    }

    private void whenPasswordRequirementsAreObtained() {
        try {
            this.requirements = Optional.of(this.passwordStrengthVerificationService.getPasswordStrengthRequirements());
        } catch (KuraException e) {
            this.exception = Optional.of(e);
        }
    }

    private void thenMinimumPasswordLengthIs(final int length) {
        assertEquals(Optional.of(length),
                this.requirements.map(PasswordStrengthRequirements::getPasswordMinimumLength));
    }

    private void thenDigitsAreRequired(final boolean required) {
        assertEquals(Optional.of(required), this.requirements.map(PasswordStrengthRequirements::digitsRequired));
    }

    private void thenSpecialCharactersAreRequired(final boolean required) {
        assertEquals(Optional.of(required),
                this.requirements.map(PasswordStrengthRequirements::specialCharactersRequired));
    }

    private void thenBothCasesAreRequired(final boolean required) {
        assertEquals(Optional.of(required), this.requirements.map(PasswordStrengthRequirements::bothCasesRequired));
    }

    private void thenNoExceptionIsThrown() {
        assertEquals(Optional.empty(), this.exception);
    }

    private void thenExceptionIsThrown(final Class<? extends Exception> claszz) {
        assertEquals(Optional.of(claszz), this.exception.map(Object::getClass));
    }

}

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

import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.configuration.SelfConfiguringComponent;
import org.eclipse.kura.configuration.metatype.OCD;
import org.eclipse.kura.core.configuration.metatype.Tocd;
import org.eclipse.kura.core.testutil.service.ServiceUtil;
import org.eclipse.kura.identity.PasswordStrengthRequirements;
import org.eclipse.kura.identity.PasswordStrengthVerificationService;
import org.junit.After;
import org.junit.Test;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;

public class PasswordStrengthVerificationServiceImplTest {

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

    private static final String CONSOLE_PID = "org.eclipse.kura.web.Console";

    private final PasswordStrengthVerificationService passwordStrengthVerificationService;

    private Map<String, Object> consoleProperties = new HashMap<>();
    private Optional<Exception> exception = Optional.empty();
    private Optional<PasswordStrengthRequirements> requirements = Optional.empty();

    private final ServiceRegistration<SelfConfiguringComponent> reg;

    public PasswordStrengthVerificationServiceImplTest() {
        try {
            this.passwordStrengthVerificationService = ServiceUtil
                    .trackService(PasswordStrengthVerificationService.class, Optional.empty())
                    .get(30, TimeUnit.SECONDS);
            this.reg = registerConsoleComponent();
            ServiceUtil
                    .trackService(SelfConfiguringComponent.class, Optional.of("(kura.service.pid=" + CONSOLE_PID + ")"))
                    .get(30, TimeUnit.SECONDS);
        } catch (final Exception e) {
            fail("failed to track ConfigurationService");
            throw new IllegalStateException("unreachable");
        }
    }

    @After
    public void unregisterConsoleComponent() {
        this.reg.unregister();
    }

    private ServiceRegistration<SelfConfiguringComponent> registerConsoleComponent() {
        final Dictionary<String, Object> properties = new Hashtable<>();
        properties.put(ConfigurationService.KURA_SERVICE_PID, CONSOLE_PID);
        properties.put("service.pid", CONSOLE_PID);

        return FrameworkUtil.getBundle(PasswordStrengthVerificationServiceImplTest.class).getBundleContext()
                .registerService(SelfConfiguringComponent.class, new MockConsole(), properties);
    }

    private void givenConsoleOptions(final Object... values) {

        final Iterator<Object> iter = Arrays.asList(values).iterator();

        final Map<String, Object> properties = new HashMap<>();
        properties.put(ConfigurationService.KURA_SERVICE_PID, CONSOLE_PID);

        while (iter.hasNext()) {
            properties.put((String) iter.next(), iter.next());
        }

        this.consoleProperties = properties;
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

    private class MockConsole implements SelfConfiguringComponent {

        @Override
        public ComponentConfiguration getConfiguration() throws KuraException {

            return new ComponentConfiguration() {

                @Override
                public String getPid() {
                    return CONSOLE_PID;
                }

                @Override
                public OCD getDefinition() {
                    return new Tocd();
                }

                @Override
                public Map<String, Object> getConfigurationProperties() {
                    PasswordStrengthVerificationServiceImplTest.this.consoleProperties
                            .put(ConfigurationService.KURA_SERVICE_PID, CONSOLE_PID);
                    PasswordStrengthVerificationServiceImplTest.this.consoleProperties.put("service.pid", CONSOLE_PID);

                    return PasswordStrengthVerificationServiceImplTest.this.consoleProperties;
                }
            };
        }

    }

}

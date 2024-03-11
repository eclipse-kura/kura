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
package org.eclipse.kura.core.identity;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.identity.PasswordStrengthRequirements;
import org.eclipse.kura.identity.PasswordStrengthVerificationService;
import org.eclipse.kura.util.validation.PasswordStrengthValidators;
import org.eclipse.kura.util.validation.Validator;
import org.eclipse.kura.util.validation.ValidatorOptions;

@SuppressWarnings("restriction")
public class PasswordStrengthVerificationServiceImpl implements PasswordStrengthVerificationService {

    private static final String KURA_WEB_CONSOLE_SERVICE_PID = "org.eclipse.kura.web.Console";

    private ConfigurationService configurationService;

    public void setConfigurationService(final ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    @Override
    public void checkPasswordStrength(char[] password) throws KuraException {
        ValidatorOptions validatorOptions = getValidatorOptions();

        final List<Validator<String>> validators = PasswordStrengthValidators.fromConfig(validatorOptions);

        final List<String> errors = new ArrayList<>();

        for (final Validator<String> validator : validators) {
            validator.validate(new String(password), errors::add);
        }

        if (!errors.isEmpty()) {
            throw new KuraException(KuraErrorCode.INVALID_PARAMETER, "Password strenght requirements not satisfied: "
                    + errors.stream().collect(Collectors.joining("; ")));
        }
    }

    public ValidatorOptions getValidatorOptions() throws KuraException {
        ComponentConfiguration consoleConfig = this.configurationService
                .getComponentConfiguration(KURA_WEB_CONSOLE_SERVICE_PID);

        if (consoleConfig == null) {
            throw new KuraException(KuraErrorCode.SERVICE_UNAVAILABLE, "Console is not registered");
        }

        return new ValidatorOptions(consoleConfig.getConfigurationProperties());
    }

    @Override
    public PasswordStrengthRequirements getPasswordStrengthRequirements() throws KuraException {
        final ValidatorOptions validatorOptions = getValidatorOptions();
        return new PasswordStrengthRequirements(validatorOptions.isPasswordMinimumLength(),
                validatorOptions.isPasswordRequireDigits(), validatorOptions.isPasswordRequireSpecialChars(),
                validatorOptions.isPasswordRequireBothCases());
    }
}

/*******************************************************************************
 * Copyright (c) 2024, 2025 Eurotech and/or its affiliates and others
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
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.identity.PasswordStrengthRequirements;
import org.eclipse.kura.identity.PasswordStrengthVerificationService;
import org.eclipse.kura.util.validation.PasswordStrengthValidators;
import org.eclipse.kura.util.validation.Validator;
import org.eclipse.kura.util.validation.ValidatorOptions;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.Designate;

@SuppressWarnings("restriction")
@Component(immediate = true, name = PasswordStrengthVerificationServiceOptions.PID, //
        configurationPolicy = ConfigurationPolicy.REQUIRE, property = "kura.ui.service.hide:Boolean=true")
@Designate(ocd = PasswordStrengthVerificationServiceOptions.class)
public class PasswordStrengthVerificationServiceImpl
        implements PasswordStrengthVerificationService, ConfigurableComponent {

    private final AtomicReference<PasswordStrengthRequirements> requirements;

    @Activate
    public PasswordStrengthVerificationServiceImpl(final PasswordStrengthVerificationServiceOptions options) {
        this.requirements = new AtomicReference<>(buildPasswordStrengthRequirements(options));
    }

    @Modified
    public void updated(final PasswordStrengthVerificationServiceOptions options) {
        this.requirements.set(buildPasswordStrengthRequirements(options));
    }

    @Override
    public PasswordStrengthRequirements getPasswordStrengthRequirements() {
        return this.requirements.get();
    }

    @Override
    public void checkPasswordStrength(char[] password) throws KuraException {
        final PasswordStrengthRequirements currentRequirements = getPasswordStrengthRequirements();

        ValidatorOptions validatorOptions = new ValidatorOptions(currentRequirements.getPasswordMinimumLength(),
                currentRequirements.digitsRequired(), currentRequirements.bothCasesRequired(),
                currentRequirements.specialCharactersRequired());

        final List<Validator<String>> validators = PasswordStrengthValidators.fromConfig(validatorOptions);

        final List<String> errors = new ArrayList<>();

        for (final Validator<String> validator : validators) {
            validator.validate(new String(password), errors::add);
        }

        if (!errors.isEmpty()) {
            throw new KuraException(KuraErrorCode.INVALID_PARAMETER, "Password strength requirements not satisfied: "
                    + errors.stream().collect(Collectors.joining("; ")));
        }
    }

    private static PasswordStrengthRequirements buildPasswordStrengthRequirements(
            final PasswordStrengthVerificationServiceOptions options) {
        return new PasswordStrengthRequirements(options.new_password_min_length(),
                options.new_password_require_digits(), options.new_password_require_special_characters(),
                options.new_password_require_both_cases());
    }

}

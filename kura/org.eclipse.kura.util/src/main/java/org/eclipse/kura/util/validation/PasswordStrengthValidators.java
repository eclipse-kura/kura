/*******************************************************************************
 * Copyright (c) 2023 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.util.validation;

import java.util.ArrayList;
import java.util.List;

public class PasswordStrengthValidators {

    private static final String CONTAINS_DIGITS = ".*\\d.*";
    private static final String CONTAINS_NOT_ALPHANUMERIC = ".*[^a-zA-Z0-9].*";
    private static final String LOWERCASE = ".*[a-z].*";
    private static final String UPPERCASE = ".*[A-Z].*";

    private PasswordStrengthValidators() {
    }

    public static List<Validator<String>> fromConfig(final ValidatorOptions userOptions) {
        return fromConfig(userOptions, new DefaultMessages());
    }

    public static List<Validator<String>> fromConfig(final ValidatorOptions userOptions, final Messages messages) {
        final List<Validator<String>> result = new ArrayList<>();

        final int minPasswordLength = userOptions.isPasswordMinimumLength();

        if (minPasswordLength > 0) {
            result.add(passwordLengthValidator(minPasswordLength, messages));
        }

        if (userOptions.isPasswordRequireDigits()) {
            result.add(containsDigitsValidator(messages));
        }

        if (userOptions.isPasswordRequireBothCases()) {
            result.add(containsBothCases(messages));
        }

        if (userOptions.isPasswordRequireSpecialChars()) {
            result.add(containsSpecialChars(messages));
        }

        return result;
    }

    private static Validator<String> passwordLengthValidator(final int minPasswordLength, final Messages messages) {
        return new PredicateValidator(v -> {
            final int passwordLength = v == null ? 0 : v.length();
            return passwordLength >= minPasswordLength;
        }, messages.pwdStrengthMinLength(minPasswordLength));
    }

    private static Validator<String> containsDigitsValidator(final Messages messages) {
        return new RegexValidator(CONTAINS_DIGITS, messages.pwdStrengthDigitsRequired()) {
        };
    }

    private static Validator<String> containsSpecialChars(final Messages messages) {
        return new RegexValidator(CONTAINS_NOT_ALPHANUMERIC, messages.pwdStrengthNonAlphanumericRequired()) {
        };
    }

    private static Validator<String> containsBothCases(final Messages messages) {
        final RegexValidator containsLowercase = new RegexValidator(LOWERCASE, messages.pwdStrengthBothCasesRequired());
        final RegexValidator containsUppercase = new RegexValidator(UPPERCASE, messages.pwdStrengthBothCasesRequired());

        return (v, c) -> {
            containsLowercase.validate(v, c);
            containsUppercase.validate(v, c);
        };
    }

    public interface Messages {

        public String pwdStrengthDigitsRequired();

        public String pwdStrengthNonAlphanumericRequired();

        public String pwdStrengthBothCasesRequired();

        public String pwdStrengthMinLength(final int value);
    }

    private static class DefaultMessages implements Messages {

        @Override
        public String pwdStrengthDigitsRequired() {
            return "Password must contain at least one digit";
        }

        @Override
        public String pwdStrengthNonAlphanumericRequired() {
            return "Password must contain at least one non alphanumeric character";
        }

        @Override
        public String pwdStrengthBothCasesRequired() {
            return "Password must contain both uppercase and lowercase characters";
        }

        @Override
        public String pwdStrengthMinLength(final int value) {
            return "Password length must be at least " + value + " characters";
        }
    }

}

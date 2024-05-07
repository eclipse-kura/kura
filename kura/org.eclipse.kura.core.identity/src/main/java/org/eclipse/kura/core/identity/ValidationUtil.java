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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.identity.PasswordStrengthVerificationService;

public class ValidationUtil {

    private static final String NEW_PASSWORD = "New password";
    private static final String PERMISSION_NAME = "Permission name";
    private static final String IDENTITY_NAME = "Identity name";

    private ValidationUtil() {
    }

    public static void validateNewPassword(final char[] password,
            final PasswordStrengthVerificationService passwordStrengthVerificationService) throws KuraException {
        if (password.length == 0) {
            throw new KuraException(KuraErrorCode.INVALID_PARAMETER, "New password cannot be empty");
        }

        final String asString = new String(password);

        requireMaximumLength(NEW_PASSWORD, asString, 255);

        requireNoWhitespaceCharacters(NEW_PASSWORD, asString);

        passwordStrengthVerificationService.checkPasswordStrength(password);
    }

    public static void validateNewIdentityName(final String identityName) throws KuraException {
        requireMinimumLength(IDENTITY_NAME, identityName, 3);

        requireMaximumLength(IDENTITY_NAME, identityName, 255);

        new PunctuatedAlphanumericSequenceValidator(IDENTITY_NAME, Arrays.asList('.', '_')).validate(identityName);
    }

    public static void validateNewPermissionName(final String permissionName) throws KuraException {
        requireMinimumLength(PERMISSION_NAME, permissionName, 3);

        requireMaximumLength(PERMISSION_NAME, permissionName, 255);

        new PunctuatedAlphanumericSequenceValidator(PERMISSION_NAME, Collections.singletonList('.'))
                .validate(permissionName);
    }

    private static void requireMinimumLength(final String parameterName, final String value, final int length)
            throws KuraException {
        if (value.length() < length) {
            throw new KuraException(KuraErrorCode.INVALID_PARAMETER,
                    parameterName + " name must be at least " + length + " characters long");
        }
    }

    private static void requireMaximumLength(final String parameterName, final String value, final int length)
            throws KuraException {
        if (value.length() > length) {
            throw new KuraException(KuraErrorCode.INVALID_PARAMETER,
                    parameterName + " must be at most " + length + " characters long");
        }
    }

    private static void requireNoWhitespaceCharacters(final String parameterName, final String value)
            throws KuraException {
        for (int i = 0; i < value.length(); i++) {
            if (Character.isWhitespace(value.codePointAt(i))) {
                throw new KuraException(KuraErrorCode.INVALID_PARAMETER,
                        parameterName + " cannot contain whitespace characters");
            }
        }
    }

    private static class PunctuatedAlphanumericSequenceValidator {

        private final String parameterName;
        private final List<Character> delimiters;

        private static final Pattern ALPHANUMERIC_PATTERN = Pattern.compile("[a-zA-Z0-9]+");

        public PunctuatedAlphanumericSequenceValidator(final String parameterName, final List<Character> delimiters) {
            this.parameterName = parameterName;
            this.delimiters = delimiters;
        }

        private boolean isDelimiter(final char value) {
            for (final char delimiter : delimiters) {
                if (value == delimiter) {
                    return true;
                }
            }

            return false;
        }

        private void requireNonEmptyAlphanumericString(final String value) throws KuraException {
            if (!ALPHANUMERIC_PATTERN.matcher(value).matches()) {
                throw new KuraException(KuraErrorCode.INVALID_PARAMETER, parameterName
                        + " must be composed of one or more non empty alphanumeric characters sequences separated by the following characters: "
                        + delimiters.stream().map(c -> "\'" + c + "\'").collect(Collectors.joining(" ")));
            }
        }

        public void validate(final String value) throws KuraException {
            if (value.isEmpty()) {
                return;
            }

            final StringBuilder component = new StringBuilder();

            for (int i = 0; i < value.length(); i++) {
                final char c = value.charAt(i);

                if (isDelimiter(c)) {
                    requireNonEmptyAlphanumericString(component.toString());
                    component.setLength(0);
                } else {
                    component.append(c);
                }
            }

            requireNonEmptyAlphanumericString(component.toString());
        }
    }
}

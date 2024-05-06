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

import java.util.regex.Pattern;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.identity.PasswordStrengthVerificationService;

public class ValidationUtil {

    private ValidationUtil() {
    }

    private static final Pattern PERMISSION_NAME_PATTERN = Pattern.compile("([a-zA-Z0-9]+[.]?)*[a-zA-Z0-9]");

    public static void validateNewPassword(final char[] password,
            final PasswordStrengthVerificationService passwordStrengthVerificationService) throws KuraException {
        if (password.length == 0) {
            throw new KuraException(KuraErrorCode.INVALID_PARAMETER, "New password cannot be empty");
        }

        final String asString = new String(password);

        if (asString.length() > 255) {
            throw new KuraException(KuraErrorCode.INVALID_PARAMETER, "New password can be at most 255 characters long");
        }

        requireNoWhitespaceCharacters(asString, "New password cannot contain whitespace characters");

        passwordStrengthVerificationService.checkPasswordStrength(password);
    }

    public static void validateNewIdentityName(final String identityName) throws KuraException {
        if (identityName.isEmpty()) {
            throw new KuraException(KuraErrorCode.INVALID_PARAMETER, "Identity name cannot be empty");
        }

        if (identityName.length() > 255) {
            throw new KuraException(KuraErrorCode.INVALID_PARAMETER,
                    "Identity name can be at most 255 characters long");
        }

        requireNoWhitespaceCharacters(new String(identityName), "Identity name cannot contain whitespace characters");
    }

    public static void validateNewPermissionName(final String permissionName) throws KuraException {
        if (permissionName.length() > 255) {
            throw new KuraException(KuraErrorCode.INVALID_PARAMETER,
                    "Permission name can be at most 255 characters long");
        }

        if (!PERMISSION_NAME_PATTERN.matcher(permissionName).matches()) {
            throw new KuraException(KuraErrorCode.INVALID_PARAMETER,
                    "Permission name must be a composed of one or more alphanumeric characters sequences separated by the dot character");
        }
    }

    private static void requireNoWhitespaceCharacters(final String value, final String message) throws KuraException {
        for (int i = 0; i < value.length(); i++) {
            if (Character.isWhitespace(value.codePointAt(i))) {
                throw new KuraException(KuraErrorCode.INVALID_PARAMETER, message);
            }
        }
    }
}

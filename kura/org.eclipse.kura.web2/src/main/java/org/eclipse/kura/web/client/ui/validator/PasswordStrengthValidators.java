/*******************************************************************************
 * Copyright (c) 2020, 2021 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.web.client.ui.validator;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.shared.model.GwtConsoleUserOptions;
import org.gwtbootstrap3.client.ui.form.validator.Validator;

import com.google.gwt.core.client.GWT;
import com.google.gwt.regexp.shared.RegExp;

public class PasswordStrengthValidators {

    private static final Messages MSGS = GWT.create(Messages.class);

    private static final String CONTAINS_DIGITS = ".*\\d.*";
    private static final String CONTAINS_NOT_ALPHANUMERIC = ".*[^a-zA-Z0-9].*";
    private static final String LOWERCASE = "[a-z]";
    private static final String UPPERCASE = "[A-Z]";

    private PasswordStrengthValidators() {
    }

    public static List<Validator<String>> fromConfig(final GwtConsoleUserOptions userOptions) {
        final List<Validator<String>> result = new ArrayList<>();

        final int minPasswordLength = userOptions.getPasswordMinimumLength();

        if (minPasswordLength > 0) {
            result.add(passwordLengthValidator(minPasswordLength));
        }

        if (userOptions.getPasswordRequireDigits()) {
            result.add(containsDigitsValidator());
        }

        if (userOptions.getPasswordRequireBothCases()) {
            result.add(containsBothCases());
        }

        if (userOptions.getPasswordRequireSpecialChars()) {
            result.add(containsSpecialChars());
        }

        return result;
    }

    private static Validator<String> passwordLengthValidator(final int minPasswordLength) {
        return new PredicateValidator(v -> {
            final int passwordLength = v == null ? 0 : v.length();
            return passwordLength >= minPasswordLength;
        }, MSGS.pwdStrengthMinLength(Integer.toString(minPasswordLength)));
    }

    private static Validator<String> containsDigitsValidator() {
        return new RegexValidator(CONTAINS_DIGITS, MSGS.pwdStrengthDigitsRequired()) {
        };
    }

    private static Validator<String> containsSpecialChars() {
        return new RegexValidator(CONTAINS_NOT_ALPHANUMERIC, MSGS.pwdStrengthNonAlphanumericRequired()) {
        };
    }

    private static Validator<String> containsBothCases() {
        return new PredicateValidator(
                v -> RegExp.compile(LOWERCASE, "g").exec(v) != null && RegExp.compile(UPPERCASE, "g").exec(v) != null,
                MSGS.pwdStrengthBothCasesRequired()) {
        };
    }

}

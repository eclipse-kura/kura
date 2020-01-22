/*******************************************************************************
 * Copyright (c) 2019, 2020 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.web.client.ui;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.shared.model.GwtConsoleUserOptions;
import org.gwtbootstrap3.client.ui.Input;
import org.gwtbootstrap3.client.ui.form.validator.Validator;

import com.google.gwt.core.client.GWT;
import com.google.gwt.regexp.shared.RegExp;

public class NewPasswordInput extends Input {

    private static final Messages MSGS = GWT.create(Messages.class);

    private static final String DIGITS = "\\d";
    private static final String NOT_ALPHANUMERIC = "[^a-zA-Z0-9]";
    private static final String LOWERCASE = "[a-z]";
    private static final String UPPERCASE = "[A-Z]";

    public NewPasswordInput() {
        super();

        setValidatorsFrom(EntryClassUi.getUserOptions());
    }

    @SuppressWarnings("unchecked")
    public void setValidatorsFrom(final GwtConsoleUserOptions userOptions) {
        setValidators();

        final int minPasswordLength = userOptions.getPasswordMinimumLength();

        if (minPasswordLength > 0) {
            addValidator(passwordLengthValidator(minPasswordLength));
        }

        if (userOptions.getPasswordRequireDigits()) {
            addValidator(containsDigitsValidator());
        }

        if (userOptions.getPasswordRequireBothCases()) {
            addValidator(containsBothCases());
        }

        if (userOptions.getPasswordRequireSpecialChars()) {
            addValidator(containsSpecialChars());
        }
    }

    private Validator<String> passwordLengthValidator(final int minPasswordLength) {
        return new PredicateValidator(v -> v.length() >= minPasswordLength,
                MSGS.pwdStrengthMinLength(Integer.toString(minPasswordLength)));
    }

    private Validator<String> containsDigitsValidator() {
        return new RegexValidator(DIGITS, MSGS.pwdStrengthDigitsRequired()) {
        };
    }

    private Validator<String> containsSpecialChars() {
        return new RegexValidator(NOT_ALPHANUMERIC, MSGS.pwdStrengthNonAlphanumericRequired()) {
        };
    }

    private Validator<String> containsBothCases() {
        return new PredicateValidator(
                v -> RegExp.compile(LOWERCASE, "g").exec(v) != null && RegExp.compile(UPPERCASE, "g").exec(v) != null,
                MSGS.pwdStrengthBothCasesRequired()) {
        };
    }

}

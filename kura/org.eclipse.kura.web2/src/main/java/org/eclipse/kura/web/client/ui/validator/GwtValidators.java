/*******************************************************************************
 * Copyright (c) 2021, 2024 Eurotech and/or its affiliates and others
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
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.shared.model.GwtConsoleUserOptions;
import org.eclipse.kura.web.shared.validator.NotEmptyValidator;
import org.eclipse.kura.web.shared.validator.NotInListValidator;
import org.eclipse.kura.web.shared.validator.PEMValidator;
import org.eclipse.kura.web.shared.validator.SinglePEMValidator;
import org.eclipse.kura.web.shared.validator.PKCS8Validator;
import org.eclipse.kura.web.shared.validator.PasswordStrengthValidators;
import org.eclipse.kura.web.shared.validator.PredicateValidator;
import org.eclipse.kura.web.shared.validator.RegexValidator;
import org.eclipse.kura.web.shared.validator.StringLengthValidator;
import org.eclipse.kura.web.shared.validator.StringNotInListValidator;
import org.gwtbootstrap3.client.ui.form.error.BasicEditorError;
import org.gwtbootstrap3.client.ui.form.validator.Validator;
import org.gwtbootstrap3.client.ui.form.validator.Validator.Priority;

import com.google.gwt.core.client.GWT;
import com.google.gwt.editor.client.Editor;
import com.google.gwt.editor.client.EditorError;

public class GwtValidators {

    private static final Messages MSGS = GWT.create(Messages.class);

    private GwtValidators() {
    }

    public static List<Validator<String>> passwordStrength(final GwtConsoleUserOptions userOptions) {
        return PasswordStrengthValidators.fromConfig(userOptions, new PasswordStrengthValidators.Messages() {

            @Override
            public String pwdStrengthDigitsRequired() {
                return MSGS.pwdStrengthDigitsRequired();
            }

            @Override
            public String pwdStrengthNonAlphanumericRequired() {
                return MSGS.pwdStrengthNonAlphanumericRequired();
            }

            @Override
            public String pwdStrengthBothCasesRequired() {
                return MSGS.pwdStrengthBothCasesRequired();
            }

            @Override
            public String pwdStrengthMinLength(int value) {
                return MSGS.pwdStrengthMinLength(Integer.toString(value));
            }

        }).stream().map(v -> new ValidatorWrapper<>(v, Priority.MEDIUM)).collect(Collectors.toList());
    }

    public static Validator<String> nonEmpty(final String message) {
        return new ValidatorWrapper<String>(new NotEmptyValidator(message), Priority.HIGHEST) {
        };
    }

    public static <T> Validator<T> notInList(final List<T> values, final String message) {
        return new ValidatorWrapper<T>(new NotInListValidator<>(values, message), Priority.MEDIUM) {
        };
    }

    public static Validator<String> stringNotInList(final List<String> values, final String message) {
        return new ValidatorWrapper<String>(new StringNotInListValidator(values, message), Priority.MEDIUM) {
        };
    }

    public static Validator<String> pem(final String message) {
        return new ValidatorWrapper<String>(new PEMValidator(message), Priority.MEDIUM) {
        };
    }
    
    public static Validator<String> singlePem(final String message) {
        return new ValidatorWrapper<String>(new SinglePEMValidator(message), Priority.MEDIUM) {
        };
    }

    public static Validator<String> pkcs8(final String message) {
        return new ValidatorWrapper<String>(new PKCS8Validator(message), Priority.MEDIUM) {
        };
    }

    public static Validator<String> predicate(final Predicate<String> predicate, final String message) {
        return new ValidatorWrapper<String>(new PredicateValidator(predicate, message), Priority.MEDIUM) {
        };
    }

    public static Validator<String> regex(final String regex, final String message) {
        return new ValidatorWrapper<String>(new RegexValidator(regex, message), Priority.MEDIUM) {
        };
    }

    public static Validator<String> stringLength(int maxSize, String message) {
        return new ValidatorWrapper<String>(new StringLengthValidator(maxSize, message), Priority.MEDIUM) {
        };
    }

    public static Validator<String> stringLength(final int minSize, final int maxSize, final String message) {
        return new ValidatorWrapper<String>(new StringLengthValidator(minSize, maxSize, message), Priority.MEDIUM) {
        };
    }

    private static class ValidatorWrapper<T> implements Validator<T> {

        private final org.eclipse.kura.web.shared.validator.Validator<T> wrapped;
        private final int priority;

        private ValidatorWrapper(org.eclipse.kura.web.shared.validator.Validator<T> wrapped, int priority) {
            this.wrapped = wrapped;
            this.priority = priority;
        }

        @Override
        public int getPriority() {
            return priority;
        }

        @Override
        public List<EditorError> validate(Editor<T> editor, T value) {
            final List<EditorError> result = new ArrayList<>();

            wrapped.validate(value, message -> result.add(new BasicEditorError(editor, value, message)));

            return result;
        }
    }
}

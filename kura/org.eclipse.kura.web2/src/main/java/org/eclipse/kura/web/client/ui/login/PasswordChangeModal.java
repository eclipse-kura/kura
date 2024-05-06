/*******************************************************************************
 * Copyright (c) 2022, 2024 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.web.client.ui.login;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.ui.validator.GwtValidators;
import org.eclipse.kura.web.shared.model.GwtConsoleUserOptions;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.Input;
import org.gwtbootstrap3.client.ui.Modal;
import org.gwtbootstrap3.client.ui.form.error.BasicEditorError;
import org.gwtbootstrap3.client.ui.form.validator.Validator;

import com.google.gwt.core.client.GWT;
import com.google.gwt.editor.client.Editor;
import com.google.gwt.editor.client.EditorError;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.Widget;

public class PasswordChangeModal extends Composite {

    interface PasswordChangeModalUiBinder extends UiBinder<Widget, PasswordChangeModal> {
    }

    private static final PasswordChangeModalUiBinder uiBinder = GWT.create(PasswordChangeModalUiBinder.class);
    private static final Messages MSGS = GWT.create(Messages.class);

    @UiField
    Modal passwordChangeModal;
    @UiField
    Input oldPassword;
    @UiField
    Input newPassword;
    @UiField
    Input confirmNewPassword;
    @UiField
    Button okButton;
    @UiField
    FormPanel passwordChangeForm;

    private Optional<Callback> callback = Optional.empty();

    public PasswordChangeModal() {
        initWidget(uiBinder.createAndBindUi(this));

        this.oldPassword.addChangeHandler(e -> validate());
        this.newPassword.addChangeHandler(e -> validate());
        this.confirmNewPassword.addChangeHandler(e -> validate());
        this.oldPassword.addKeyUpHandler(e -> validate());
        this.newPassword.addKeyUpHandler(e -> validate());
        this.confirmNewPassword.addKeyUpHandler(e -> validate());

        this.oldPassword.addValidator(new Validator<String>() {

            @Override
            public int getPriority() {
                return 0;
            }

            @Override
            public List<EditorError> validate(Editor<String> editor, String value) {
                if (value == null || value.length() <= 0) {
                    return Collections
                            .singletonList(new BasicEditorError(editor, value, MSGS.loginEnterCurrentPassword()));
                }
                return Collections.emptyList();
            }

        });

        this.confirmNewPassword.addValidator(new Validator<String>() {

            @Override
            public List<EditorError> validate(final Editor<String> editor, final String value) {
                if (!Objects.equals(value, PasswordChangeModal.this.newPassword.getValue())) {
                    return Collections
                            .singletonList(new BasicEditorError(editor, value, MSGS.loginNotMatchingPasswords()));
                }
                return Collections.emptyList();
            }

            @Override
            public int getPriority() {
                return 0;
            }

        });

        this.passwordChangeForm.addSubmitHandler(e -> {
            e.cancel();
            trySubmit();
        });
        this.okButton.addClickHandler(e -> {
            e.preventDefault();
            trySubmit();
        });
    }

    private void trySubmit() {
        if (!this.oldPassword.validate() || !this.newPassword.validate() || !this.confirmNewPassword.validate()) {
            return;
        }

        this.passwordChangeModal.hide();
        this.callback.ifPresent(c -> c.onPasswordChanged(this.oldPassword.getValue(), this.newPassword.getValue()));
    }

    private void validate() {
        this.oldPassword.validate();
        this.newPassword.validate();
        this.confirmNewPassword.validate();
    }

    @SuppressWarnings("unchecked")
    private void setUserOptions(final GwtConsoleUserOptions options) {
        this.newPassword.setValidators(new Validator<String>() {

            @Override
            public int getPriority() {
                return 0;
            }

            @Override
            public List<EditorError> validate(final Editor<String> editor, final String value) {
                final List<EditorError> result = new ArrayList<>();

                for (final Validator<String> validator : GwtValidators.newPassword(options)) {
                    result.addAll(validator.validate(editor, value));
                }

                return result;
            }

        });
    }

    public void pickPassword(final GwtConsoleUserOptions options, final Callback callback) {
        this.oldPassword.setValue("");
        this.newPassword.setValue("");
        this.confirmNewPassword.setValue("");
        setUserOptions(options);
        this.callback = Optional.of(callback);

        this.passwordChangeModal.show();
    }

    public interface Callback {

        public void onPasswordChanged(final String oldPassword, final String newPassword);
    }
}

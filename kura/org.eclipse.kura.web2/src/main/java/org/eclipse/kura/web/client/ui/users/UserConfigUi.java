/*******************************************************************************
 * Copyright (c) 2020 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.web.client.ui.users;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.ui.EntryClassUi;
import org.eclipse.kura.web.client.ui.Picker;
import org.eclipse.kura.web.client.ui.drivers.assets.BooleanInputCell;
import org.eclipse.kura.web.client.util.PasswordStrengthValidators;
import org.eclipse.kura.web.shared.model.GwtUserConfig;
import org.eclipse.kura.web.shared.model.GwtUserData;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.InlineRadio;
import org.gwtbootstrap3.client.ui.constants.InputType;
import org.gwtbootstrap3.client.ui.form.validator.Validator;

import com.google.gwt.core.client.GWT;
import com.google.gwt.editor.client.EditorError;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;

public class UserConfigUi extends Composite {

    protected static final Messages MSGS = GWT.create(Messages.class);

    private static UserConfigUiUiBinder uiBinder = GWT.create(UserConfigUiUiBinder.class);

    interface UserConfigUiUiBinder extends UiBinder<Widget, UserConfigUi> {
    }

    @UiField
    InlineRadio passwordEnabled;
    @UiField
    InlineRadio passwordDisabled;
    @UiField
    Button changePassword;
    @UiField
    Picker picker;
    @UiField
    CellTable<AssignedPermission> permissionTable;

    private final Listener listener;
    private final GwtUserConfig userData;

    private final ListDataProvider<AssignedPermission> dataProvider = new ListDataProvider<>();
    private final boolean hasPassword;
    private final List<Validator<String>> passwordStrengthValidators;

    public UserConfigUi(final GwtUserConfig userData, final Set<String> definedPermissions, final Listener listener) {
        this.listener = listener;
        this.userData = userData;
        this.hasPassword = userData.isPasswordAuthEnabled();
        this.passwordStrengthValidators = PasswordStrengthValidators.fromConfig(EntryClassUi.getUserOptions());
        initWidget(uiBinder.createAndBindUi(this));
        initTable(userData, definedPermissions);
        initPasswordWidgets();
    }

    private void initTable(final GwtUserData userData, final Set<String> definedPermissions) {
        final Column<AssignedPermission, String> assignedColumn = new Column<AssignedPermission, String>(
                new BooleanInputCell()) {

            @Override
            public String getValue(final AssignedPermission object) {
                return Boolean.toString(object.isAssigned());
            }
        };

        assignedColumn.setFieldUpdater((index, object, value) -> {
            final boolean assigned = Boolean.parseBoolean(value);
            final String permissionName = object.getName();

            object.setAssigned(assigned);

            if (assigned) {
                userData.getPermissions().add(permissionName);
            } else {
                userData.getPermissions().remove(permissionName);
            }

            this.listener.onUserDataChanged(userData);
        });

        final TextColumn<AssignedPermission> nameColumn = new TextColumn<AssignedPermission>() {

            @Override
            public String getValue(AssignedPermission object) {
                return object.getName();
            }
        };

        this.permissionTable.addColumn(assignedColumn, MSGS.usersPermissionAssigned());
        this.permissionTable.addColumn(nameColumn, MSGS.usersPermissionName());
        this.dataProvider.addDataDisplay(this.permissionTable);

        final List<AssignedPermission> tableContent = this.dataProvider.getList();

        for (final String permission : definedPermissions) {
            tableContent.add(new AssignedPermission(permission, userData.getPermissions().contains(permission)));
        }
    }

    public void initPasswordWidgets() {
        updatePasswordWidgetState();

        this.passwordEnabled.addChangeHandler(e -> {
            this.userData.setPasswordAuthEnabled(true);
            this.listener.onUserDataChanged(this.userData);
            updatePasswordWidgetState();
            if (!this.hasPassword) {
                pickPassword();
            }
        });

        this.passwordDisabled.addChangeHandler(e -> {
            this.userData.setPasswordAuthEnabled(false);
            this.userData.setNewPassword(Optional.empty());
            this.listener.onUserDataChanged(this.userData);
            updatePasswordWidgetState();
        });

        this.changePassword.addClickHandler(e -> pickPassword());
    }

    private void pickPassword() {
        final Runnable onDismiss = () -> {
            if (!this.hasPassword) {
                this.passwordEnabled.setValue(false);
                this.passwordDisabled.setValue(true);
                this.userData.setPasswordAuthEnabled(false);
                updatePasswordWidgetState();
            }
        };

        this.picker.builder(String.class) //
                .setTitle(MSGS.usersSetPassword()) //
                .setMessage(MSGS.usersDefineNewPassword()) //
                .setInputCustomizer(input -> input.setType(InputType.PASSWORD)) //
                .setOnCancel(onDismiss) //
                .setValidator((editor, password) -> {
                    if (password == null || password.trim().isEmpty()) {
                        throw new IllegalArgumentException(MSGS.usersPasswordEmpty());
                    }
                    for (final Validator<String> validator : this.passwordStrengthValidators) {
                        final List<EditorError> errors = validator.validate(editor, password);
                        if (!errors.isEmpty()) {
                            throw new IllegalArgumentException(errors.get(0).getMessage());
                        }
                    }

                    return password;
                }).setOnPick(newPassword -> this.picker.builder(String.class) //
                        .setTitle(MSGS.usersConfirmPassword()) //
                        .setMessage(MSGS.usersRepeatPassword()) //
                        .setInputCustomizer(input -> input.setType(InputType.PASSWORD)) //
                        .setOnCancel(onDismiss) //
                        .setValidator((e, confirm) -> {
                            if (!newPassword.equals(confirm)) {
                                throw new IllegalArgumentException(MSGS.usersPasswordMismatch());
                            }

                            return confirm;
                        }).setOnPick(p -> {
                            this.userData.setNewPassword(Optional.of(p));
                            this.listener.onUserDataChanged(this.userData);
                        }).pick())
                .pick();
    }

    public void updatePasswordWidgetState() {
        final boolean isPasswordEnabled = this.userData.isPasswordAuthEnabled();

        this.passwordEnabled.setValue(isPasswordEnabled);
        this.passwordDisabled.setValue(!isPasswordEnabled);

        this.changePassword.setEnabled(isPasswordEnabled);
    }

    private class AssignedPermission {

        private final String name;
        private boolean assigned;

        public AssignedPermission(String name, boolean assigned) {
            this.name = name;
            this.assigned = assigned;
        }

        public boolean isAssigned() {
            return this.assigned;
        }

        public void setAssigned(boolean assigned) {
            this.assigned = assigned;
        }

        public String getName() {
            return this.name;
        }
    }

    public interface Listener {

        public void onUserDataChanged(final GwtUserData userData);
    }
}

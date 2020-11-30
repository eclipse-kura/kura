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
        hasPassword = userData.isPasswordAuthEnabled();
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
            final boolean assigned = Boolean.valueOf(value);
            final String permissionName = object.getName();

            object.setAssigned(assigned);

            if (assigned) {
                userData.getPermissions().add(permissionName);
            } else {
                userData.getPermissions().remove(permissionName);
            }

            listener.onUserDataChanged(userData);
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

        final List<AssignedPermission> tableContent = dataProvider.getList();

        for (final String permission : definedPermissions) {
            tableContent.add(new AssignedPermission(permission, userData.getPermissions().contains(permission)));
        }
    }

    public void initPasswordWidgets() {
        updatePasswordWidgetState();

        this.passwordEnabled.addChangeHandler(e -> {
            userData.setPasswordAuthEnabled(true);
            listener.onUserDataChanged(userData);
            updatePasswordWidgetState();
            if (!hasPassword) {
                pickPassword();
            }
        });

        this.passwordDisabled.addChangeHandler(e -> {
            userData.setPasswordAuthEnabled(false);
            userData.setNewPassword(Optional.empty());
            listener.onUserDataChanged(userData);
            updatePasswordWidgetState();
        });

        this.changePassword.addClickHandler(e -> pickPassword());
    }

    private void pickPassword() {
        final Runnable onDismiss = () -> {
            if (!hasPassword) {
                passwordEnabled.setValue(false);
                passwordDisabled.setValue(true);
                userData.setPasswordAuthEnabled(false);
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
                    for (final Validator<String> validator : passwordStrengthValidators) {
                        final List<EditorError> errors = validator.validate(editor, password);
                        if (!errors.isEmpty()) {
                            throw new IllegalArgumentException(errors.get(0).getMessage());
                        }
                    }

                    return password;
                }).setOnPick(newPassword -> {
                    this.picker.builder(String.class) //
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
                                userData.setNewPassword(Optional.of(p));
                                listener.onUserDataChanged(userData);
                            }).pick();
                }).pick();
    }

    public void updatePasswordWidgetState() {
        final boolean isPasswordEnabled = userData.isPasswordAuthEnabled();

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
            return assigned;
        }

        public void setAssigned(boolean assigned) {
            this.assigned = assigned;
        }

        public String getName() {
            return name;
        }
    }

    public interface Listener {

        public void onUserDataChanged(final GwtUserData userData);
    }
}

package org.eclipse.kura.web.client.ui.login;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

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
import com.google.gwt.user.client.ui.Widget;

public class PasswordChangeModal extends Composite {

    interface PasswordChangeModalUiBinder extends UiBinder<Widget, PasswordChangeModal> {
    }

    private static final PasswordChangeModalUiBinder uiBinder = GWT.create(PasswordChangeModalUiBinder.class);

    @UiField
    Modal passwordChangeModal;
    @UiField
    Input newPassword;
    @UiField
    Input confirmNewPassword;
    @UiField
    Button okButton;

    private Optional<Callback> callback = Optional.empty();

    public PasswordChangeModal() {
        initWidget(uiBinder.createAndBindUi(this));

        this.newPassword.addChangeHandler(e -> validate());
        this.confirmNewPassword.addChangeHandler(e -> validate());
        this.newPassword.addKeyUpHandler(e -> validate());
        this.confirmNewPassword.addKeyUpHandler(e -> validate());

        this.confirmNewPassword.addValidator(new Validator<String>() {

            @Override
            public List<EditorError> validate(final Editor<String> editor, final String value) {
                if (!Objects.equals(value, newPassword.getValue())) {
                    return Collections.singletonList(new BasicEditorError(editor, value, "passwords do not match"));
                }
                return Collections.emptyList();
            }

            @Override
            public int getPriority() {
                return 0;
            }

        });

        this.okButton.addClickHandler(e -> {
            if (!newPassword.validate() || !confirmNewPassword.validate()) {
                return;
            }

            passwordChangeModal.hide();
            callback.ifPresent(c -> c.onPasswordChanged(newPassword.getValue()));
        });
    }

    private void validate() {
        newPassword.validate();
        confirmNewPassword.validate();
    }

    @SuppressWarnings("unchecked")
    private void setUserOptions(final GwtConsoleUserOptions options) {
        this.newPassword.setValidators(GwtValidators.passwordStrength(options).toArray(new Validator[0]));
    }

    public void pickPassword(final GwtConsoleUserOptions options, final Callback callback) {
        this.newPassword.setValue("");
        this.confirmNewPassword.setValue("");
        setUserOptions(options);
        this.callback = Optional.of(callback);

        this.passwordChangeModal.show();
    }

    public interface Callback {

        public void onPasswordChanged(final String newPassword);
    }
}

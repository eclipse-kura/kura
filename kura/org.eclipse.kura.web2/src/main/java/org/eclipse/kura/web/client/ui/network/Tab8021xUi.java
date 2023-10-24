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
 *  Sterwen-Technology
 *******************************************************************************/
package org.eclipse.kura.web.client.ui.network;

import java.util.logging.Logger;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.ui.NewPasswordInput;
import org.eclipse.kura.web.client.util.HelpButton;
import org.eclipse.kura.web.client.util.MessageUtils;
import org.eclipse.kura.web.shared.GwtSafeHtmlUtils;
import org.eclipse.kura.web.shared.model.Gwt8021xConfig;
import org.eclipse.kura.web.shared.model.Gwt8021xEap;
import org.eclipse.kura.web.shared.model.Gwt8021xInnerAuth;
import org.eclipse.kura.web.shared.model.GwtNetInterfaceConfig;
import org.eclipse.kura.web.shared.model.GwtSession;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.Form;
import org.gwtbootstrap3.client.ui.FormLabel;
import org.gwtbootstrap3.client.ui.ListBox;
import org.gwtbootstrap3.client.ui.TextBox;
import org.gwtbootstrap3.client.ui.constants.ValidationState;
import org.gwtbootstrap3.client.ui.html.Span;
import org.gwtbootstrap3.client.ui.html.Text;
import org.gwtbootstrap3.client.ui.PanelHeader;
import org.gwtbootstrap3.client.ui.FormGroup;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.ScrollPanel;

public class Tab8021xUi extends Composite implements NetworkTab {

    private static Tab8021xUiUiBinder uiBinder = GWT.create(Tab8021xUiUiBinder.class);

    private static final Messages MSGS = GWT.create(Messages.class);

    interface Tab8021xUiUiBinder extends UiBinder<Widget, Tab8021xUi> {
    }

    private final NetworkTabsUi netTabs;

    Gwt8021xConfig activeConfig;
    GwtSession currentSession;

    private boolean dirty;

    // Labels
    @UiField
    FormLabel labelEap;

    @UiField
    FormLabel labelInnerAuth;

    @UiField
    FormLabel labelUsername;

    @UiField
    FormLabel labelPassword;

    @UiField
    FormLabel labelKeystorePid;

    @UiField
    FormLabel labelCaCertName;

    @UiField
    FormLabel labelPublicPrivateKeyPairName;

    @UiField
    Form form;

    // Fields
    @UiField
    Button buttonTestPassword;

    @UiField
    ListBox eap;

    @UiField
    ListBox innerAuth;

    @UiField
    TextBox username;

    @UiField
    NewPasswordInput password;

    @UiField
    TextBox keystorePid;

    @UiField
    TextBox caCertName;

    @UiField
    TextBox publicPrivateKeyPairName;

    // Help
    @UiField
    HelpButton helpEap;

    @UiField
    HelpButton helpInnerAuth;

    @UiField
    HelpButton helpUsername;

    @UiField
    HelpButton helpPassword;

    @UiField
    HelpButton helpKeystorePid;

    @UiField
    HelpButton helpCaCertName;

    @UiField
    HelpButton helpPublicPrivateKeyPairName;

    @UiField
    PanelHeader helpTitle;

    @UiField
    ScrollPanel helpText;

    @UiField
    FormGroup formgroupIdentityUsername;

    @UiField
    FormGroup formgroupPassword;

    @UiField
    FormGroup identityKeystorePid;

    @UiField
    FormGroup identityCaCertName;

    @UiField
    FormGroup identityPublicPrivateKeyPairName;

    public Tab8021xUi(GwtSession currentSession, NetworkTabsUi tabs) {
        initWidget(uiBinder.createAndBindUi(this));

        this.currentSession = currentSession;
        this.netTabs = tabs;

        initLabels();
        initHelpButtons();
        initListBoxes();
        initTextBoxes();

        this.buttonTestPassword.setVisible(false);
    }

    private void initLabels() {
        labelEap.setText(MSGS.net8021xEap());
        labelInnerAuth.setText(MSGS.net8021xInnerAuth());
        labelUsername.setText(MSGS.net8021xUsername());
        labelPassword.setText(MSGS.net8021xPassword());
        labelKeystorePid.setText(MSGS.net8021xKeystorePid());
        labelCaCertName.setText(MSGS.net8021xCaCert());
        labelPublicPrivateKeyPairName.setText(MSGS.net8021xPublicPrivateKeyPair());
    }

    private void initHelpButtons() {
        this.helpEap.setHelpText(MSGS.net8021xEapHelp());
        this.helpInnerAuth.setHelpText(MSGS.net8021xInnerAuthHelp());
        this.helpUsername.setHelpText(MSGS.net8021xUsernameHelp());
        this.helpPassword.setHelpText(MSGS.net8021xPasswordHelp());
        this.helpKeystorePid.setHelpText(MSGS.net8021xKeystorePidHelp());
        this.helpCaCertName.setHelpText(MSGS.net8021xCaCertHelp());
        this.helpPublicPrivateKeyPairName.setHelpText(MSGS.net8021xPublicPrivateKeyPairHelp());
    }

    private void initListBoxes() {
        initEapListBox();
        initInnerAuthListBox();
    }

    private void initTextBoxes() {
        initUsernameTextBox();
        initPasswordTextBox();
        initKeystorePidTextBox();
        initCaCertNameTextBox();
        initPrivateKeyNameTextBox();
    }

    private void initEapListBox() {
        for (Gwt8021xEap eapValue : Gwt8021xEap.values()) {
            this.eap.addItem(eapValue.name());
        }

        this.eap.addMouseOverHandler(event -> {
            if (this.eap.isEnabled()) {
                setHelpText(MSGS.net8021xEapHelp());
            }
        });

        this.eap.addMouseOutHandler(event -> resetHelpText());

        this.eap.addChangeHandler(event -> {
            setDirty(true);
            this.netTabs.updateTabs();
            refreshForm();
            resetValidations();
        });
    }

    private void initInnerAuthListBox() {
        for (Gwt8021xInnerAuth auth : Gwt8021xInnerAuth.values()) {
            this.innerAuth.addItem(auth.name());
        }

        this.innerAuth.addMouseOverHandler(event -> {
            if (this.innerAuth.isEnabled()) {
                setHelpText(MSGS.net8021xInnerAuthHelp());
            }
        });

        this.innerAuth.addMouseOutHandler(event -> resetHelpText());

        this.innerAuth.addChangeHandler(event -> {
            setDirty(true);
            this.netTabs.updateTabs();
            refreshForm();
            resetValidations();
        });
    }

    private void initUsernameTextBox() {
        this.username.addMouseOverHandler(event -> {
            if (this.username.isEnabled()) {
                setHelpText(MSGS.net8021xUsernameHelp());
            }
        });

        this.username.addBlurHandler(e -> this.username.validate());
        this.username.setAllowBlank(true);
        this.username.addMouseOutHandler(event -> resetHelpText());

        this.username.addChangeHandler(event -> {
            setDirty(true);

            if (this.username.getValue().isEmpty()) {
                this.formgroupIdentityUsername.setValidationState(ValidationState.ERROR);
            } else {
                this.formgroupIdentityUsername.setValidationState(ValidationState.NONE);
            }

        });
    }

    private void initPasswordTextBox() {
        this.password.addMouseOverHandler(event -> {
            if (this.password.isEnabled()) {
                setHelpText(MSGS.net8021xPasswordHelp());
            }
        });

        this.password.addBlurHandler(e -> this.password.validate());
        this.password.setAllowBlank(false);
        this.password.addMouseOutHandler(event -> resetHelpText());

        this.password.addChangeHandler(event -> {

            if (!this.password.validate() && this.password.isEnabled()) {
                this.formgroupPassword.setValidationState(ValidationState.ERROR);
            } else {
                this.formgroupPassword.setValidationState(ValidationState.NONE);
            }

        });
    }

    private void initKeystorePidTextBox() {
        this.keystorePid.addMouseOverHandler(event -> {
            if (this.keystorePid.isEnabled()) {
                setHelpText(MSGS.net8021xKeystorePidHelp());
            }
        });

        this.keystorePid.addBlurHandler(e -> this.keystorePid.validate());
        this.keystorePid.setAllowBlank(false);
        this.keystorePid.addMouseOutHandler(event -> resetHelpText());

        this.keystorePid.addChangeHandler(event -> {
            setDirty(true);

            if (this.keystorePid.getValue().isEmpty() && this.keystorePid.isEnabled()) {
                this.identityKeystorePid.setValidationState(ValidationState.ERROR);
            } else {
                this.identityKeystorePid.setValidationState(ValidationState.NONE);
            }

        });
    }

    private void initCaCertNameTextBox() {
        this.caCertName.addMouseOverHandler(event -> {
            if (this.caCertName.isEnabled()) {
                setHelpText(MSGS.net8021xCaCertHelp());
            }
        });

        this.caCertName.addBlurHandler(e -> this.caCertName.validate());
        this.caCertName.setAllowBlank(true);
        this.caCertName.addMouseOutHandler(event -> resetHelpText());

        this.caCertName.addChangeHandler(event -> {
            setDirty(true);
        });
    }

    private void initPrivateKeyNameTextBox() {
        this.publicPrivateKeyPairName.addMouseOverHandler(event -> {
            if (this.publicPrivateKeyPairName.isEnabled()) {
                setHelpText(MSGS.net8021xPublicPrivateKeyPairHelp());
            }
        });

        this.publicPrivateKeyPairName.addBlurHandler(e -> this.publicPrivateKeyPairName.validate());
        this.publicPrivateKeyPairName.setAllowBlank(false);
        this.publicPrivateKeyPairName.addMouseOutHandler(event -> resetHelpText());

        this.publicPrivateKeyPairName.addChangeHandler(event -> {
            setDirty(true);

            if (this.publicPrivateKeyPairName.getValue().isEmpty() && this.publicPrivateKeyPairName.isEnabled()) {
                this.identityPublicPrivateKeyPairName.setValidationState(ValidationState.ERROR);
            } else {
                this.identityPublicPrivateKeyPairName.setValidationState(ValidationState.NONE);
            }

        });
    }

    private void resetValidations() {
        this.formgroupIdentityUsername.setValidationState(ValidationState.NONE);
        this.formgroupPassword.setValidationState(ValidationState.NONE);
        this.identityKeystorePid.setValidationState(ValidationState.NONE);
        this.identityCaCertName.setValidationState(ValidationState.NONE);
        this.identityPublicPrivateKeyPairName.setValidationState(ValidationState.NONE);
    }

    private void refreshForm() {
        this.eap.setEnabled(true);
        this.innerAuth.setEnabled(true);
        this.username.setEnabled(true);
        this.password.setEnabled(true);
        this.keystorePid.setEnabled(true);
        this.caCertName.setEnabled(true);
        this.publicPrivateKeyPairName.setEnabled(true);

        refreshFieldsBasedOnSelectedValues();

    }

    private void reset() {
        for (int i = 0; i < this.eap.getItemCount(); i++) {
            if (this.eap.getSelectedItemText().equals(Gwt8021xEap.TTLS.name())) {
                this.eap.setSelectedIndex(i);
                break;
            }
        }

        for (int i = 0; i < this.innerAuth.getItemCount(); i++) {
            if (this.innerAuth.getSelectedItemText().equals(Gwt8021xInnerAuth.MSCHAPV2.name())) {
                this.innerAuth.setSelectedIndex(i);
                break;
            }
        }

        this.username.setValue("");
        this.password.setValue("");

        this.keystorePid.setValue("");
        this.caCertName.setValue("");
        this.publicPrivateKeyPairName.setValue("");
    }

    private void setValues() {

        for (int i = 0; i < this.eap.getItemCount(); i++) {
            if (this.eap.getValue(i).equals(this.activeConfig.getEapEnum().name())) {
                this.eap.setSelectedIndex(i);
                break;
            }
        }

        for (int i = 0; i < this.innerAuth.getItemCount(); i++) {
            if (this.innerAuth.getValue(i).equals(this.activeConfig.getInnerAuthEnum().name())) {
                this.innerAuth.setSelectedIndex(i);
                break;
            }
        }

        this.username.setValue(this.activeConfig.getUsername());
        this.password.setValue(this.activeConfig.getPassword());

        this.keystorePid.setValue(this.activeConfig.getKeystorePid());
        this.caCertName.setValue(this.activeConfig.getCaCertName());
        this.publicPrivateKeyPairName.setValue(this.activeConfig.getPublicPrivateKeyPairName());

    }

    @Override
    public void clear() {
        // Not needed
    }

    @Override
    public void refresh() {
        if (isDirty()) {
            setDirty(false);
            resetValidations();

            if (this.activeConfig == null) {
                reset();
            } else {
                setValues();
                refreshForm();
                this.netTabs.updateTabs();
            }
        }
    }

    @Override
    public boolean isDirty() {
        return this.dirty;
    }

    @Override
    public boolean isValid() {
        boolean isTLS = (Gwt8021xEap.valueOf(this.eap.getSelectedValue()) == Gwt8021xEap.TLS);
        boolean isPEAP = (Gwt8021xEap.valueOf(this.eap.getSelectedValue()) == Gwt8021xEap.PEAP);
        boolean isTTLS = (Gwt8021xEap.valueOf(this.eap.getSelectedValue()) == Gwt8021xEap.TTLS);

        if (isTLS) {

            if (this.username.getValue().isEmpty()) {
                this.formgroupIdentityUsername.setValidationState(ValidationState.ERROR);
                return false;
            }

            if (this.keystorePid.getValue().isEmpty()) {
                this.identityKeystorePid.setValidationState(ValidationState.ERROR);
                return false;
            }

            if (this.publicPrivateKeyPairName.getValue().isEmpty()) {
                this.identityPublicPrivateKeyPairName.setValidationState(ValidationState.ERROR);
                return false;
            }
        }

        if (isPEAP || isTTLS) {
            if (this.username.getValue().isEmpty()) {
                this.formgroupIdentityUsername.setValidationState(ValidationState.ERROR);
                return false;
            }

            if (this.password.getValue().isEmpty()) {
                this.formgroupPassword.setValidationState(ValidationState.ERROR);
                return false;
            }
        }

        return true;
    }

    @Override
    public void setDirty(boolean dirty) {
        this.dirty = dirty;
        if (this.netTabs.getButtons() != null) {
            this.netTabs.getButtons().setButtonsDirty(dirty);
        }
    }

    @Override
    public void getUpdatedNetInterface(GwtNetInterfaceConfig updatedNetIf) {
        Gwt8021xConfig updated8021xConfig = new Gwt8021xConfig();

        updated8021xConfig.setIdentity(this.username.getText());

        updated8021xConfig.setPassword(this.password.getText());

        if (!this.eap.getSelectedValue().isEmpty() && this.eap.getSelectedValue() != null) {
            updated8021xConfig.setEap(Gwt8021xEap.valueOf(this.eap.getSelectedValue()));
        }

        if (!this.innerAuth.getSelectedValue().isEmpty() && this.innerAuth.getSelectedValue() != null) {
            updated8021xConfig.setInnerAuthEnum(Gwt8021xInnerAuth.valueOf(this.innerAuth.getSelectedValue()));
        }

        updated8021xConfig.setKeystorePid(this.keystorePid.getText());
        updated8021xConfig.setCaCertName(this.caCertName.getText());
        updated8021xConfig.setPublicPrivateKeyPairName(this.publicPrivateKeyPairName.getText());

        updatedNetIf.setEnterpriseConfig(updated8021xConfig);
    }

    @Override
    public void setNetInterface(GwtNetInterfaceConfig config) {
        setDirty(true);
        this.activeConfig = config.get8021xConfig();
    }

    private void refreshFieldsBasedOnSelectedValues() {
        switch (Gwt8021xEap.valueOf(this.eap.getSelectedValue())) {
        case PEAP:
        case TTLS:
            this.keystorePid.setEnabled(false);
            setInnerAuthTo(Gwt8021xInnerAuth.MSCHAPV2);
            this.publicPrivateKeyPairName.setEnabled(false);
            break;
        case TLS:
            this.innerAuth.setEnabled(false);
            setInnerAuthTo(Gwt8021xInnerAuth.NONE);
            this.password.setEnabled(false);
            break;
        default:
            break;
        }
    }

    private void setInnerAuthTo(Gwt8021xInnerAuth auth) {
        for (int i = 0; i < this.innerAuth.getItemCount(); i++) {
            if (this.innerAuth.getItemText(i).equals(auth.name())) {
                this.innerAuth.setSelectedIndex(i);
                break;
            }
        }
    }

    private void setHelpText(String message) {
        this.helpText.clear();
        this.helpText.add(new Span(message));
    }

    private void resetHelpText() {
        this.helpText.clear();
        this.helpText.add(new Span(MSGS.netHelpDefaultHint()));
    }

}

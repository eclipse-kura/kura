/*******************************************************************************
 * Copyright (c) 2024 Areti and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Areti
 *******************************************************************************/
package org.eclipse.kura.web.client.ui.network;

import java.util.Objects;
import java.util.Optional;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.util.HelpButton;
import org.eclipse.kura.web.client.util.MessageUtils;
import org.eclipse.kura.web.shared.model.GwtNetIfType;
import org.eclipse.kura.web.shared.model.GwtNetInterfaceConfig;
import org.eclipse.kura.web.shared.model.GwtSession;
import org.gwtbootstrap3.client.ui.Form;
import org.gwtbootstrap3.client.ui.FormControlStatic;
import org.gwtbootstrap3.client.ui.FormGroup;
import org.gwtbootstrap3.client.ui.FormLabel;
import org.gwtbootstrap3.client.ui.HelpBlock;
import org.gwtbootstrap3.client.ui.IntegerBox;
import org.gwtbootstrap3.client.ui.ListBox;
import org.gwtbootstrap3.client.ui.PanelHeader;
import org.gwtbootstrap3.client.ui.TextArea;
import org.gwtbootstrap3.client.ui.TextBox;
import org.gwtbootstrap3.client.ui.constants.ValidationState;
import org.gwtbootstrap3.client.ui.html.Span;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;

public class TabAdvancedUi extends Composite implements NetworkTab {
	
    private static final String PROMISC_DEFAULT = "netAdvPromiscDefault";
    private static final String PROMISC_ENABLED = "netAdvPromiscEnabled";
    private static final String PROMISC_DISABLED = "netAdvPromiscDisabled";

    interface TabAdvancedUiUiBinder extends UiBinder<Widget, TabAdvancedUi> {
    }

    @UiField
    FormGroup groupMtu;
    @UiField
    FormGroup groupIp6Mtu;
    @UiField
    FormGroup groupPromisc;

    @UiField
    FormLabel labelMtu;
    @UiField
    FormLabel labelIp6Mtu;
    @UiField
    FormLabel labelPromisc;

    @UiField
    HelpBlock wrongInputMtu;
    @UiField
    HelpBlock wrongInputIp6Mtu;

    @UiField
    HelpButton helpButtonMtu;
    @UiField
    HelpButton helpButtonIp6Mtu;
    @UiField
    HelpButton helpButtonPromisc;

    
    @UiField
    IntegerBox mtu;
    @UiField
    IntegerBox ip6Mtu;
    @UiField
    ListBox promisc;

    @UiField
    PanelHeader helpTitle;
    @UiField
    ScrollPanel helpText;

    @UiField
    Form form;

    private static TabAdvancedUiUiBinder uiBinder = GWT.create(TabAdvancedUiUiBinder.class);
    private static final Messages MSGS = GWT.create(Messages.class);

    private boolean dirty = false;
    private final NetworkTabsUi tabs;
    private Optional<GwtNetInterfaceConfig> selectedNetIfConfig = Optional.empty();

    public TabAdvancedUi(GwtSession currentSession, NetworkTabsUi netTabs) {
        initWidget(uiBinder.createAndBindUi(this));

        this.helpTitle.setText(MSGS.netHelpTitle());
        this.tabs = netTabs;

        initLabels();
        initHelpButtons();
        initListBoxes();
        initTextBoxes();
    }

    private void initLabels() {
        this.labelMtu.setText(MSGS.netAdvIPv4Mtu());
        this.labelIp6Mtu.setText(MSGS.netAdvIPv6Mtu());
        this.labelPromisc.setText(MSGS.netAdvPromisc());
    }

    private void initHelpButtons() {
        this.helpButtonMtu.setHelpText(MSGS.netAdvIPv4ToolTipMtu());
        this.helpButtonIp6Mtu.setHelpText(MSGS.netAdvIPv6ToolTipMtu());
        this.helpButtonPromisc.setHelpText(MSGS.netAdvToolTipPromisc());
    }

    private void initListBoxes() {
        initPromiscField();
    }

    private void initPromiscField() {
        this.promisc.clear();
        this.promisc.addItem(MessageUtils.get(PROMISC_DEFAULT), "-1");
        this.promisc.addItem(MessageUtils.get(PROMISC_ENABLED), "1");
        this.promisc.addItem(MessageUtils.get(PROMISC_DISABLED), "0");

        this.promisc.addMouseOverHandler(event -> {
            if (this.promisc.isEnabled()) {
                setHelpText(MSGS.netAdvToolTipPromisc());
            }
        });

        this.promisc.addMouseOutHandler(event -> resetHelpText());

        this.promisc.addChangeHandler(event -> {
            setDirty(true);
            this.tabs.updateTabs();

            refreshForm();
            resetValidations();
        });
    }

    private void initTextBoxes() {
    	initMtuField();
    	initIp6MtuField();
    }
    
    private void initMtuField() {
        this.mtu.addMouseOverHandler(event -> {
            this.helpText.clear();
            this.helpText.add(new Span(MSGS.netAdvIPv4ToolTipMtu()));
        });
        this.mtu.addMouseOutHandler(event -> resetHelpText());
        this.mtu.addValueChangeHandler(valChangeEvent -> {
            setDirty(true);

            String inputText = this.mtu.getText();
            boolean isValidValue = false;

            if (inputText != null) {
                if (inputText.trim().isEmpty()) {
                    isValidValue = true;
                } else {
                    isValidValue = isValidIntegerInRange(inputText, 0, Integer.MAX_VALUE);
                }
            }

            if (isValidValue) {
                this.groupMtu.setValidationState(ValidationState.NONE);
                this.wrongInputMtu.setText("");
            } else {
                this.groupMtu.setValidationState(ValidationState.ERROR);
                this.wrongInputMtu.setText(MSGS.netAdvIPv4InvalidMtu());
            }
        });
    }

    private void initIp6MtuField() {
        this.ip6Mtu.addMouseOverHandler(event -> {
            if (this.ip6Mtu.isEnabled()) {
                setHelpText(MSGS.netAdvIPv6ToolTipMtu());
            }
        });
        this.ip6Mtu.addMouseOutHandler(event -> resetHelpText());

        this.ip6Mtu.addValueChangeHandler(valChangeEvent -> {
            setDirty(true);

            String inputText = this.ip6Mtu.getText();
            boolean isValidValue = false;

            if (inputText != null) {
                if (inputText.trim().isEmpty()) {
                    isValidValue = true;
                } else {
                    isValidValue = isValidIntegerInRange(inputText, 0, Integer.MAX_VALUE);
                }
            }

            if (isValidValue) {
                this.groupIp6Mtu.setValidationState(ValidationState.NONE);
                this.wrongInputIp6Mtu.setText("");
            } else {
                this.groupIp6Mtu.setValidationState(ValidationState.ERROR);
                this.wrongInputIp6Mtu.setText(MSGS.netAdvIPv6InvalidMtu());
            }
        });
    }

    private boolean isValidIntegerInRange(String integerText, int min, int max) {
        try {
            int value = Integer.parseInt(integerText.trim());
            return value >= min && value <= max;
        } catch (NumberFormatException e) {
            return false;
        }
    }



    private void setHelpText(String message) {
        this.helpText.clear();
        this.helpText.add(new Span(message));
    }

    private void resetHelpText() {
        this.helpText.clear();
        setHelpText(MSGS.netHelpDefaultHint());
    }

    private void resetValidations() {
        this.groupMtu.setValidationState(ValidationState.NONE);
        this.wrongInputMtu.setText("");
        this.groupIp6Mtu.setValidationState(ValidationState.NONE);
        this.wrongInputIp6Mtu.setText("");
    }

    private void refreshForm() {
        this.mtu.setEnabled(true);
        this.ip6Mtu.setEnabled(true);
        this.promisc.setEnabled(false);

        if (this.selectedNetIfConfig.isPresent()) {
            refreshFieldsBasedOnInterface(this.selectedNetIfConfig.get());
        }
    }

    private void refreshFieldsBasedOnInterface(GwtNetInterfaceConfig config) {
        switch (config.getHwTypeEnum()) {
        case ETHERNET:
        	this.promisc.setEnabled(true);
            break;
        case LOOPBACK:
            this.mtu.setEnabled(false);
            this.ip6Mtu.setEnabled(false);
            break;
        case MODEM:
            break;
        case WIFI:
            break;
        default:
            break;

        }
    }

    @Override
    public void setDirty(boolean isDirty) {
        this.dirty = isDirty;
        if (this.tabs.getButtons() != null) {
            this.tabs.getButtons().setButtonsDirty(isDirty);
        }
    }

    @Override
    public boolean isDirty() {
        return this.dirty;
    }

    @Override
    public void setNetInterface(GwtNetInterfaceConfig config) {
        setDirty(true);
        this.selectedNetIfConfig = Optional.of(config);
    }

    @Override
    public void getUpdatedNetInterface(GwtNetInterfaceConfig updatedNetIf) {
        if (this.form != null) {
            updateConfigWithSelectedValues(updatedNetIf);
        }
    }

    private void updateConfigWithSelectedValues(GwtNetInterfaceConfig updatedNetIf) {
    	if (this.mtu.getValue() != null) {
            updatedNetIf.setMtu(this.mtu.getValue());
        }

    	if (this.ip6Mtu.getValue() != null) {
            updatedNetIf.setIpv6Mtu(this.ip6Mtu.getValue());
        }
        
        if(!nullOrEmpty(this.promisc.getSelectedValue())) {
            updatedNetIf.setPromisc(Integer.parseInt(this.promisc.getSelectedValue()));
        }
    }

    private boolean nullOrEmpty(String value) {
        return Objects.isNull(value) || value.trim().isEmpty();
    }

    @Override
    public boolean isValid() {
        if (this.groupMtu.getValidationState().equals(ValidationState.ERROR)
                || this.groupIp6Mtu.getValidationState().equals(ValidationState.ERROR)) {
            return false;
        }

        return true;
    }

    @Override
    public void refresh() {
        if (isDirty()) {
            setDirty(false);
            resetValidations();

            if (this.selectedNetIfConfig.isPresent()) {
                fillFormWithCachedConfig();
            } else {
                reset();
            }
        }
    }

    private void reset() {
        this.mtu.setText("");
        this.ip6Mtu.setText("");
        this.promisc.setSelectedIndex(0);
    }

    private void fillFormWithCachedConfig() {
        this.mtu.setValue(this.selectedNetIfConfig.get().getMtu());
        this.ip6Mtu.setValue(this.selectedNetIfConfig.get().getIpv6Mtu());
        for (int i = 0; i < this.promisc.getItemCount(); i++) {
            if (this.promisc.getValue(i).equals(this.selectedNetIfConfig.get().getPromisc().toString())) {
                this.promisc.setSelectedIndex(i);
                break;
            }
        }

        this.tabs.updateTabs();
        refreshForm();
    }

    @Override
    public void clear() {
        // Not needed
    }

}

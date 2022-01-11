/*******************************************************************************
 * Copyright (c) 2011, 2022 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.web.client.ui.firewall;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.ui.AlertDialog;
import org.eclipse.kura.web.client.ui.AlertDialog.ConfirmListener;
import org.eclipse.kura.web.client.ui.EntryClassUi;
import org.eclipse.kura.web.client.ui.Tab;
import org.eclipse.kura.web.client.util.FailureHandler;
import org.eclipse.kura.web.client.util.TextFieldValidator.FieldType;
import org.eclipse.kura.web.shared.model.GwtFirewallNatMasquerade;
import org.eclipse.kura.web.shared.model.GwtFirewallPortForwardEntry;
import org.eclipse.kura.web.shared.model.GwtNetProtocol;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.service.GwtNetworkService;
import org.eclipse.kura.web.shared.service.GwtNetworkServiceAsync;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenService;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenServiceAsync;
import org.gwtbootstrap3.client.shared.event.ModalHideHandler;
import org.gwtbootstrap3.client.ui.Alert;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.FormGroup;
import org.gwtbootstrap3.client.ui.FormLabel;
import org.gwtbootstrap3.client.ui.ListBox;
import org.gwtbootstrap3.client.ui.Modal;
import org.gwtbootstrap3.client.ui.TextBox;
import org.gwtbootstrap3.client.ui.Tooltip;
import org.gwtbootstrap3.client.ui.constants.ValidationState;
import org.gwtbootstrap3.client.ui.form.error.BasicEditorError;
import org.gwtbootstrap3.client.ui.form.validator.Validator;
import org.gwtbootstrap3.client.ui.gwt.CellTable;

import com.google.gwt.core.client.GWT;
import com.google.gwt.editor.client.Editor;
import com.google.gwt.editor.client.EditorError;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SingleSelectionModel;
import com.google.web.bindery.event.shared.HandlerRegistration;

public class PortForwardingTabUi extends Composite implements Tab, ButtonBar.Listener {

    private static final String STATUS_TABLE_ROW = "status-table-row";

    private static PortForwardingTabUiUiBinder uiBinder = GWT.create(PortForwardingTabUiUiBinder.class);

    interface PortForwardingTabUiUiBinder extends UiBinder<Widget, PortForwardingTabUi> {
    }

    private static final Messages MSGS = GWT.create(Messages.class);

    private final GwtSecurityTokenServiceAsync gwtXSRFService = GWT.create(GwtSecurityTokenService.class);
    private final GwtNetworkServiceAsync gwtNetworkService = GWT.create(GwtNetworkService.class);

    private final ListDataProvider<GwtFirewallPortForwardEntry> portForwardDataProvider = new ListDataProvider<>();
    private final SingleSelectionModel<GwtFirewallPortForwardEntry> selectionModel = new SingleSelectionModel<>();

    private GwtFirewallPortForwardEntry newPortForwardEntry;
    private GwtFirewallPortForwardEntry editPortForwardEntry;

    private boolean dirty;

    @UiField
    ButtonBar buttonBar;
    @UiField
    Alert notification;
    @UiField
    CellTable<GwtFirewallPortForwardEntry> portForwardGrid = new CellTable<>();

    @UiField
    AlertDialog alertDialog;

    @UiField
    Modal portForwardingForm;

    @UiField
    FormLabel labelInput;
    @UiField
    FormLabel labelOutput;
    @UiField
    FormLabel labelLan;
    @UiField
    FormLabel labelProtocol;
    @UiField
    FormLabel labelExternal;
    @UiField
    FormLabel labelInternal;
    @UiField
    FormLabel labelEnable;
    @UiField
    FormLabel labelPermitttedNw;
    @UiField
    FormLabel labelPermitttedMac;
    @UiField
    FormLabel labelSource;

    @UiField
    FormGroup groupInput;
    @UiField
    FormGroup groupOutput;
    @UiField
    FormGroup groupLan;
    @UiField
    FormGroup groupExternal;
    @UiField
    FormGroup groupInternal;
    @UiField
    FormGroup groupPermittedNw;
    @UiField
    FormGroup groupPermittedMac;
    @UiField
    FormGroup groupSource;

    @UiField
    Tooltip tooltipInput;
    @UiField
    Tooltip tooltipOutput;
    @UiField
    Tooltip tooltipLan;
    @UiField
    Tooltip tooltipProtocol;
    @UiField
    Tooltip tooltipExternal;
    @UiField
    Tooltip tooltipInternal;
    @UiField
    Tooltip tooltipEnable;
    @UiField
    Tooltip tooltipPermittedNw;
    @UiField
    Tooltip tooltipPermittedMac;
    @UiField
    Tooltip tooltipSource;

    @UiField
    TextBox input;
    @UiField
    TextBox output;
    @UiField
    TextBox lan;
    @UiField
    TextBox external;
    @UiField
    TextBox internal;
    @UiField
    TextBox permittedNw;
    @UiField
    TextBox permittedMac;
    @UiField
    TextBox source;

    @UiField
    ListBox protocol;
    @UiField
    ListBox enable;

    @UiField
    Button submit;
    @UiField
    Button cancel;

    @UiField
    Modal existingRule;
    @UiField
    Button close;

    private HandlerRegistration modalHideHandlerRegistration;

    public PortForwardingTabUi() {
        initWidget(uiBinder.createAndBindUi(this));
        this.selectionModel.addSelectionChangeHandler(event -> PortForwardingTabUi.this.buttonBar
                .setEditDeleteButtonsDirty(PortForwardingTabUi.this.selectionModel.getSelectedObject() != null));
        this.portForwardGrid.setSelectionModel(this.selectionModel);

        this.buttonBar.setListener(this);

        // Initialize fixed fields for modal
        setModalFieldsLabels();
        setModalFieldsTooltips();
        setModalFieldsHandlers();

        initTable();
        initModal();
        initDuplicateRuleModal();

    }

    private void initDuplicateRuleModal() {
        this.close.addClickHandler(event -> this.existingRule.hide());
    }

    //
    // Public methods
    //
    @Override
    public void refresh() {
        EntryClassUi.showWaitModal();
        clear();
        this.notification.setVisible(false);

        this.gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

            @Override
            public void onFailure(Throwable ex) {
                EntryClassUi.hideWaitModal();
                FailureHandler.handle(ex);
            }

            @Override
            public void onSuccess(GwtXSRFToken token) {
                PortForwardingTabUi.this.setDirty(false);
                PortForwardingTabUi.this.gwtNetworkService.findDeviceFirewallPortForwards(token,
                        new AsyncCallback<List<GwtFirewallPortForwardEntry>>() {

                            @Override
                            public void onFailure(Throwable caught) {
                                EntryClassUi.hideWaitModal();
                                FailureHandler.handle(caught,
                                        PortForwardingTabUi.this.gwtNetworkService.getClass().getSimpleName());
                            }

                            @Override
                            public void onSuccess(List<GwtFirewallPortForwardEntry> result) {
                                for (GwtFirewallPortForwardEntry pair : result) {
                                    PortForwardingTabUi.this.portForwardDataProvider.getList().add(pair);
                                }
                                setVisibility();
                                refreshTable();
                                EntryClassUi.hideWaitModal();
                            }
                        });
            }

        });
    }

    @Override
    public boolean isDirty() {
        return this.dirty;
    }

    @Override
    public void setDirty(boolean b) {
        this.dirty = b;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public void clear() {
        this.portForwardDataProvider.getList().clear();
        PortForwardingTabUi.this.buttonBar.setApplyResetButtonsDirty(false);
        PortForwardingTabUi.this.buttonBar.setEditDeleteButtonsDirty(false);
        setVisibility();
        refreshTable();
    }

    //
    // Private methods
    //
    private void initTable() {

        TextColumn<GwtFirewallPortForwardEntry> col1 = new TextColumn<GwtFirewallPortForwardEntry>() {

            @Override
            public String getValue(GwtFirewallPortForwardEntry object) {
                if (object.getInboundInterface() != null) {
                    return String.valueOf(object.getInboundInterface());
                } else {
                    return "";
                }
            }
        };
        col1.setCellStyleNames(STATUS_TABLE_ROW);
        this.portForwardGrid.addColumn(col1, MSGS.firewallPortForwardInboundInterface());

        TextColumn<GwtFirewallPortForwardEntry> col2 = new TextColumn<GwtFirewallPortForwardEntry>() {

            @Override
            public String getValue(GwtFirewallPortForwardEntry object) {
                if (object.getOutboundInterface() != null) {
                    return String.valueOf(object.getOutboundInterface());
                } else {
                    return "";
                }
            }
        };
        col2.setCellStyleNames(STATUS_TABLE_ROW);
        this.portForwardGrid.addColumn(col2, MSGS.firewallPortForwardOutboundInterface());

        TextColumn<GwtFirewallPortForwardEntry> col3 = new TextColumn<GwtFirewallPortForwardEntry>() {

            @Override
            public String getValue(GwtFirewallPortForwardEntry object) {
                if (object.getAddress() != null) {
                    return String.valueOf(object.getAddress());
                } else {
                    return "";
                }
            }
        };
        col3.setCellStyleNames(STATUS_TABLE_ROW);
        this.portForwardGrid.addColumn(col3, MSGS.firewallPortForwardAddress());

        TextColumn<GwtFirewallPortForwardEntry> col4 = new TextColumn<GwtFirewallPortForwardEntry>() {

            @Override
            public String getValue(GwtFirewallPortForwardEntry object) {
                if (object.getProtocol() != null) {
                    return String.valueOf(object.getProtocol());
                } else {
                    return "";
                }
            }
        };
        col4.setCellStyleNames(STATUS_TABLE_ROW);
        this.portForwardGrid.addColumn(col4, MSGS.firewallPortForwardProtocol());

        TextColumn<GwtFirewallPortForwardEntry> col5 = new TextColumn<GwtFirewallPortForwardEntry>() {

            @Override
            public String getValue(GwtFirewallPortForwardEntry object) {
                if (object.getOutPort() != null) {
                    return String.valueOf(object.getOutPort());
                } else {
                    return "";
                }
            }
        };
        col5.setCellStyleNames(STATUS_TABLE_ROW);
        this.portForwardGrid.addColumn(col5, MSGS.firewallPortForwardOutPort());

        TextColumn<GwtFirewallPortForwardEntry> col6 = new TextColumn<GwtFirewallPortForwardEntry>() {

            @Override
            public String getValue(GwtFirewallPortForwardEntry object) {
                if (object.getInPort() != null) {
                    return String.valueOf(object.getInPort());
                } else {
                    return "";
                }
            }
        };
        col6.setCellStyleNames(STATUS_TABLE_ROW);
        this.portForwardGrid.addColumn(col6, MSGS.firewallPortForwardInPort());

        TextColumn<GwtFirewallPortForwardEntry> col7 = new TextColumn<GwtFirewallPortForwardEntry>() {

            @Override
            public String getValue(GwtFirewallPortForwardEntry object) {
                if (object.getMasquerade() != null) {
                    return String.valueOf(object.getMasquerade());
                } else {
                    return "";
                }
            }
        };
        col7.setCellStyleNames(STATUS_TABLE_ROW);
        this.portForwardGrid.addColumn(col7, MSGS.firewallPortForwardMasquerade());

        TextColumn<GwtFirewallPortForwardEntry> col8 = new TextColumn<GwtFirewallPortForwardEntry>() {

            @Override
            public String getValue(GwtFirewallPortForwardEntry object) {
                if (object.getPermittedNetwork() != null) {
                    return String.valueOf(object.getPermittedNetwork());
                } else {
                    return "";
                }
            }
        };
        col8.setCellStyleNames(STATUS_TABLE_ROW);
        this.portForwardGrid.addColumn(col8, MSGS.firewallPortForwardPermittedNetwork());

        TextColumn<GwtFirewallPortForwardEntry> col9 = new TextColumn<GwtFirewallPortForwardEntry>() {

            @Override
            public String getValue(GwtFirewallPortForwardEntry object) {
                if (object.getPermittedMAC() != null) {
                    return String.valueOf(object.getPermittedMAC());
                } else {
                    return "";
                }
            }
        };
        col9.setCellStyleNames(STATUS_TABLE_ROW);
        this.portForwardGrid.addColumn(col9, MSGS.firewallPortForwardPermittedMac());

        TextColumn<GwtFirewallPortForwardEntry> col10 = new TextColumn<GwtFirewallPortForwardEntry>() {

            @Override
            public String getValue(GwtFirewallPortForwardEntry object) {
                if (object.getSourcePortRange() != null) {
                    return String.valueOf(object.getSourcePortRange());
                } else {
                    return "";
                }
            }
        };
        col10.setCellStyleNames(STATUS_TABLE_ROW);
        this.portForwardGrid.addColumn(col10, MSGS.firewallPortForwardSourcePortRange());

        this.portForwardDataProvider.addDataDisplay(this.portForwardGrid);
    }

    private void refreshTable() {
        int size = this.portForwardDataProvider.getList().size();
        this.portForwardGrid.setVisibleRange(0, size);
        this.portForwardDataProvider.flush();
        this.portForwardGrid.redraw();
        this.selectionModel.setSelected(this.selectionModel.getSelectedObject(), false);
    }

    @Override
    public void onApply() {
        List<GwtFirewallPortForwardEntry> intermediateList = PortForwardingTabUi.this.portForwardDataProvider.getList();

        final List<GwtFirewallPortForwardEntry> updatedPortForwardConf = new ArrayList<>();
        for (GwtFirewallPortForwardEntry entry : intermediateList) {
            updatedPortForwardConf.add(entry);
        }

        EntryClassUi.showWaitModal();
        PortForwardingTabUi.this.gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

            @Override
            public void onFailure(Throwable ex) {
                EntryClassUi.hideWaitModal();
                FailureHandler.handle(ex);
            }

            @Override
            public void onSuccess(GwtXSRFToken token) {
                PortForwardingTabUi.this.gwtNetworkService.updateDeviceFirewallPortForwards(token,
                        updatedPortForwardConf, new AsyncCallback<Void>() {

                            @Override
                            public void onFailure(Throwable ex) {
                                FailureHandler.handle(ex);
                                EntryClassUi.hideWaitModal();
                            }

                            @Override
                            public void onSuccess(Void result) {
                                PortForwardingTabUi.this.buttonBar.setApplyResetButtonsDirty(false);
                                EntryClassUi.hideWaitModal();

                                setDirty(false);
                            }
                        });
            }
        });

    }

    @Override
    public void onCancel() {
        PortForwardingTabUi.this.alertDialog.show(MSGS.deviceConfigDirty(), PortForwardingTabUi.this::refresh);
    }

    @Override
    public void onCreate() {
        replaceModalHideHandler(evt -> {
            if (PortForwardingTabUi.this.newPortForwardEntry != null) {
                // Avoid duplicates
                if (!duplicateEntry(PortForwardingTabUi.this.newPortForwardEntry)) {
                    PortForwardingTabUi.this.portForwardDataProvider.getList()
                            .add(PortForwardingTabUi.this.newPortForwardEntry);
                    setVisibility();
                    refreshTable();
                    PortForwardingTabUi.this.buttonBar.setApplyResetButtonsDirty(true);
                    PortForwardingTabUi.this.newPortForwardEntry = null;
                } else {
                    this.existingRule.show();
                }
            }
            resetFields();
        });
        showModal(null);
    }

    @Override
    public void onEdit() {

        GwtFirewallPortForwardEntry selection = PortForwardingTabUi.this.selectionModel.getSelectedObject();

        if (selection == null) {
            return;
        }

        replaceModalHideHandler(evt -> {
            if (PortForwardingTabUi.this.editPortForwardEntry != null) {
                GwtFirewallPortForwardEntry oldEntry = PortForwardingTabUi.this.selectionModel.getSelectedObject();
                PortForwardingTabUi.this.portForwardDataProvider.getList().remove(oldEntry);
                refreshTable();
                if (!duplicateEntry(PortForwardingTabUi.this.editPortForwardEntry)) {
                    PortForwardingTabUi.this.portForwardDataProvider.getList()
                            .add(PortForwardingTabUi.this.editPortForwardEntry);
                    PortForwardingTabUi.this.portForwardDataProvider.flush();
                    PortForwardingTabUi.this.buttonBar.setApplyResetButtonsDirty(true);
                    PortForwardingTabUi.this.editPortForwardEntry = null;
                    setVisibility();
                } else {    // end duplicate
                    this.existingRule.show();
                    PortForwardingTabUi.this.portForwardDataProvider.getList().add(oldEntry);
                    PortForwardingTabUi.this.portForwardDataProvider.flush();
                }
                refreshTable();
                PortForwardingTabUi.this.buttonBar.setEditDeleteButtonsDirty(false);
                PortForwardingTabUi.this.selectionModel.setSelected(selection, false);
            }
            resetFields();
        });

        showModal(selection);
    }

    @Override
    public void onDelete() {
        GwtFirewallPortForwardEntry selection = PortForwardingTabUi.this.selectionModel.getSelectedObject();
        if (selection != null) {
            PortForwardingTabUi.this.alertDialog
                    .show(MSGS.firewallOpenPortDeleteConfirmation(String.valueOf(selection.getInPort())), () -> {
                        PortForwardingTabUi.this.portForwardDataProvider.getList().remove(selection);
                        PortForwardingTabUi.this.buttonBar.setApplyResetButtonsDirty(true);
                        PortForwardingTabUi.this.buttonBar.setEditDeleteButtonsDirty(false);
                        PortForwardingTabUi.this.selectionModel.setSelected(selection, false);
                        setVisibility();
                        refreshTable();

                        setDirty(true);
                    });
        }
    }

    private void initModal() {

        // handle buttons
        this.cancel.setText(MSGS.cancelButton());
        this.cancel.addClickHandler(event -> {
            PortForwardingTabUi.this.portForwardingForm.hide();
            resetFields();
        });

        this.submit.setText(MSGS.submitButton());
        this.submit.addClickHandler(event -> {

            if (!checkEntries()) {
                return;
            }

            final GwtFirewallPortForwardEntry portForwardEntry = new GwtFirewallPortForwardEntry();
            portForwardEntry.setInboundInterface(PortForwardingTabUi.this.input.getText());
            portForwardEntry.setOutboundInterface(PortForwardingTabUi.this.output.getText());
            portForwardEntry.setAddress(PortForwardingTabUi.this.lan.getText());
            portForwardEntry.setProtocol(PortForwardingTabUi.this.protocol.getSelectedItemText());
            portForwardEntry.setOutPort(Integer.parseInt(PortForwardingTabUi.this.internal.getText()));
            portForwardEntry.setInPort(Integer.parseInt(PortForwardingTabUi.this.external.getText()));
            portForwardEntry.setMasquerade(PortForwardingTabUi.this.enable.getSelectedItemText());
            if (PortForwardingTabUi.this.permittedNw.getText() != null
                    && !"".equals(PortForwardingTabUi.this.permittedNw.getText().trim())) {
                portForwardEntry.setPermittedNetwork(PortForwardingTabUi.this.permittedNw.getText());
            } else {
                portForwardEntry.setPermittedNetwork("0.0.0.0/0");
            }
            if (PortForwardingTabUi.this.permittedMac.getText() != null
                    && !"".equals(PortForwardingTabUi.this.permittedMac.getText().trim())) {
                portForwardEntry.setPermittedMAC(PortForwardingTabUi.this.permittedMac.getText());
                PortForwardingTabUi.this.alertDialog.setTitle(MSGS.warning());
                PortForwardingTabUi.this.alertDialog.show(MSGS.firewallPortForwardFormNotificationMacFiltering(),
                        (ConfirmListener) null);
            }
            if (PortForwardingTabUi.this.source.getText() != null
                    && !"".equals(PortForwardingTabUi.this.source.getText().trim())) {
                portForwardEntry.setSourcePortRange(PortForwardingTabUi.this.source.getText());
            }

            if (PortForwardingTabUi.this.submit.getId().equals("new")) {
                PortForwardingTabUi.this.newPortForwardEntry = portForwardEntry;
                PortForwardingTabUi.this.editPortForwardEntry = null;
            } else if (PortForwardingTabUi.this.submit.getId().equals("edit")) {
                PortForwardingTabUi.this.editPortForwardEntry = portForwardEntry;
                PortForwardingTabUi.this.newPortForwardEntry = null;
            }

            setDirty(true);

            PortForwardingTabUi.this.portForwardingForm.hide();
        });// end submit click handler
    }

    private void showModal(final GwtFirewallPortForwardEntry existingEntry) {
        resetValidationStates();

        if (existingEntry == null) {
            // new
            this.portForwardingForm.setTitle(MSGS.firewallPortForwardFormInformation());
        } else {
            // edit existing entry
            this.portForwardingForm
                    .setTitle(MSGS.firewallPortForwardFormUpdate(String.valueOf(existingEntry.getInPort())));
        }

        setModalFieldsValues(existingEntry);

        if (existingEntry == null) {
            this.submit.setId("new");
        } else {
            this.submit.setId("edit");
        }

        this.portForwardingForm.show();
    }// end initModal

    private void resetValidationStates() {
        PortForwardingTabUi.this.groupInput.setValidationState(ValidationState.NONE);
        PortForwardingTabUi.this.groupOutput.setValidationState(ValidationState.NONE);
        PortForwardingTabUi.this.groupLan.setValidationState(ValidationState.NONE);
        PortForwardingTabUi.this.groupInternal.setValidationState(ValidationState.NONE);
        PortForwardingTabUi.this.groupExternal.setValidationState(ValidationState.NONE);
        PortForwardingTabUi.this.groupPermittedNw.setValidationState(ValidationState.NONE);
        PortForwardingTabUi.this.groupPermittedMac.setValidationState(ValidationState.NONE);
        PortForwardingTabUi.this.groupSource.setValidationState(ValidationState.NONE);
    }

    private void setModalFieldsHandlers() {
        // Set validations
        this.input.addValidator(newInputValidator());
        this.input.addBlurHandler(event -> this.input.validate());

        this.output.addValidator(newOutputValidator());
        this.output.addBlurHandler(event -> this.output.validate());

        this.lan.addValidator(newLanValidator());
        this.lan.addBlurHandler(event -> this.lan.validate());

        this.internal.addValidator(newInternalValidator());
        this.internal.addBlurHandler(event -> this.internal.validate());

        this.external.addValidator(newExternalValidator());
        this.external.addBlurHandler(event -> this.external.validate());

        this.permittedNw.addValidator(newPermittedNwValidator());
        this.permittedNw.addBlurHandler(event -> this.permittedNw.validate());

        this.permittedMac.addValidator(newPermittedMacValidator());
        this.permittedMac.addBlurHandler(event -> this.permittedMac.validate());

        this.source.addValidator(newSourceValidator());
        this.source.addBlurHandler(event -> this.source.validate());
    }

    private Validator<String> newInputValidator() {
        return new Validator<String>() {

            @Override
            public List<EditorError> validate(Editor<String> editor, String value) {
                List<EditorError> result = new ArrayList<>();
                if (PortForwardingTabUi.this.input.getText().trim().isEmpty()
                        || !PortForwardingTabUi.this.input.getText().trim().matches(FieldType.ALPHANUMERIC.getRegex())
                        || PortForwardingTabUi.this.input.getText().trim()
                                .length() > FirewallPanelUtils.INTERFACE_NAME_MAX_LENGTH) {
                    result.add(new BasicEditorError(PortForwardingTabUi.this.input, value,
                            MSGS.firewallPortForwardFormInboundInterfaceErrorMessage()));
                }
                return result;
            }

            @Override
            public int getPriority() {
                return 0;
            }
        };
    }

    private Validator<String> newOutputValidator() {
        return new Validator<String>() {

            @Override
            public List<EditorError> validate(Editor<String> editor, String value) {
                List<EditorError> result = new ArrayList<>();
                if (PortForwardingTabUi.this.output.getText().trim().isEmpty()
                        || !PortForwardingTabUi.this.output.getText().trim().matches(FieldType.ALPHANUMERIC.getRegex())
                        || PortForwardingTabUi.this.output.getText().trim()
                                .length() > FirewallPanelUtils.INTERFACE_NAME_MAX_LENGTH) {
                    result.add(new BasicEditorError(PortForwardingTabUi.this.output, value,
                            MSGS.firewallPortForwardFormOutboundInterfaceErrorMessage()));
                }
                return result;
            }

            @Override
            public int getPriority() {
                return 0;
            }
        };
    }

    private Validator<String> newLanValidator() {
        return new Validator<String>() {

            @Override
            public List<EditorError> validate(Editor<String> editor, String value) {
                List<EditorError> result = new ArrayList<>();
                if (PortForwardingTabUi.this.lan.getText().trim().isEmpty()
                        || !PortForwardingTabUi.this.lan.getText().trim().matches(FieldType.IPv4_ADDRESS.getRegex())) {
                    result.add(new BasicEditorError(PortForwardingTabUi.this.lan, value,
                            MSGS.firewallPortForwardFormLanAddressErrorMessage()));
                }
                return result;
            }

            @Override
            public int getPriority() {
                return 0;
            }
        };
    }

    private Validator<String> newInternalValidator() {
        return new Validator<String>() {

            @Override
            public List<EditorError> validate(Editor<String> editor, String value) {
                List<EditorError> result = new ArrayList<>();
                if (PortForwardingTabUi.this.internal.getText().trim().isEmpty()
                        || !FirewallPanelUtils.checkPortRegex(PortForwardingTabUi.this.internal.getText())
                        || !FirewallPanelUtils.isPortInRange(PortForwardingTabUi.this.internal.getText())) {
                    result.add(new BasicEditorError(PortForwardingTabUi.this.internal, value,
                            MSGS.firewallPortForwardFormInternalPortErrorMessage()));
                }
                return result;
            }

            @Override
            public int getPriority() {
                return 0;
            }
        };
    }

    private Validator<String> newExternalValidator() {
        return new Validator<String>() {

            @Override
            public List<EditorError> validate(Editor<String> editor, String value) {
                List<EditorError> result = new ArrayList<>();
                if (PortForwardingTabUi.this.external.getText().trim().isEmpty()
                        || !FirewallPanelUtils.checkPortRegex(PortForwardingTabUi.this.external.getText())
                        || !FirewallPanelUtils.isPortInRange(PortForwardingTabUi.this.external.getText())) {
                    result.add(new BasicEditorError(PortForwardingTabUi.this.external, value,
                            MSGS.firewallPortForwardFormExternalPortErrorMessage()));
                }
                return result;
            }

            @Override
            public int getPriority() {
                return 0;
            }
        };
    }

    private Validator<String> newPermittedNwValidator() {
        return new Validator<String>() {

            @Override
            public List<EditorError> validate(Editor<String> editor, String value) {
                List<EditorError> result = new ArrayList<>();
                if (!PortForwardingTabUi.this.permittedNw.getText().trim().isEmpty()
                        && !PortForwardingTabUi.this.permittedNw.getText().trim()
                                .matches(FieldType.NETWORK.getRegex())) {
                    result.add(new BasicEditorError(PortForwardingTabUi.this.permittedNw, value,
                            MSGS.firewallPortForwardFormPermittedNetworkErrorMessage()));
                }
                return result;
            }

            @Override
            public int getPriority() {
                return 0;
            }
        };
    }

    private Validator<String> newPermittedMacValidator() {
        return new Validator<String>() {

            @Override
            public List<EditorError> validate(Editor<String> editor, String value) {
                List<EditorError> result = new ArrayList<>();
                if (!PortForwardingTabUi.this.permittedMac.getText().trim().isEmpty()
                        && !PortForwardingTabUi.this.permittedMac.getText().trim()
                                .matches(FieldType.MAC_ADDRESS.getRegex())) {
                    result.add(new BasicEditorError(PortForwardingTabUi.this.permittedMac, value,
                            MSGS.firewallPortForwardFormPermittedMacAddressErrorMessage()));
                }
                return result;
            }

            @Override
            public int getPriority() {
                return 0;
            }
        };
    }

    private Validator<String> newSourceValidator() {
        return new Validator<String>() {

            @Override
            public List<EditorError> validate(Editor<String> editor, String value) {
                List<EditorError> result = new ArrayList<>();
                if (!PortForwardingTabUi.this.source.getText().trim().isEmpty()
                        && (!(FirewallPanelUtils.checkPortRegex(PortForwardingTabUi.this.source.getText())
                                || FirewallPanelUtils.checkPortRangeRegex(PortForwardingTabUi.this.source.getText()))
                                || !FirewallPanelUtils.isPortInRange(PortForwardingTabUi.this.source.getText()))) {
                    result.add(new BasicEditorError(PortForwardingTabUi.this.source, value,
                            MSGS.firewallPortForwardFormSourcePortRangeErrorMessage()));
                }
                return result;
            }

            @Override
            public int getPriority() {
                return 0;
            }
        };
    }

    private void setModalFieldsValues(final GwtFirewallPortForwardEntry existingEntry) {
        // set ListBoxes
        this.protocol.clear();
        for (GwtNetProtocol prot : GwtNetProtocol.values()) {
            this.protocol.addItem(prot.name());
        }
        this.enable.clear();
        for (GwtFirewallNatMasquerade masquerade : GwtFirewallNatMasquerade.values()) {
            this.enable.addItem(masquerade.name());
        }

        // populate Existing Values
        if (existingEntry != null) {
            this.input.setText(existingEntry.getInboundInterface());
            this.output.setText(existingEntry.getOutboundInterface());
            this.lan.setText(existingEntry.getAddress());
            this.external.setText(String.valueOf(existingEntry.getInPort()));
            this.internal.setText(String.valueOf(existingEntry.getOutPort()));
            this.permittedNw.setText(existingEntry.getPermittedNetwork());
            this.permittedMac.setText(existingEntry.getPermittedMAC());
            this.source.setText(existingEntry.getSourcePortRange());

            for (int i = 0; i < this.protocol.getItemCount(); i++) {
                if (existingEntry.getProtocol().equals(this.protocol.getItemText(i))) {
                    this.protocol.setSelectedIndex(i);
                    break;
                }
            }

            for (int i = 0; i < this.enable.getItemCount(); i++) {
                if (existingEntry.getMasquerade().equals(this.enable.getItemText(i))) {
                    this.enable.setSelectedIndex(i);
                    break;
                }
            }
        } else {
            this.input.reset();
            this.output.reset();
            this.lan.reset();
            this.external.reset();
            this.internal.reset();
            this.permittedNw.reset();
            this.permittedMac.reset();
            this.source.reset();

            this.protocol.setSelectedIndex(0);
            this.enable.setSelectedIndex(0);
        }
    }

    private void setModalFieldsTooltips() {
        // set Tooltips
        this.tooltipInput.setTitle(MSGS.firewallPortForwardFormInboundInterfaceToolTip());
        this.tooltipOutput.setTitle(MSGS.firewallPortForwardFormOutboundInterfaceToolTip());
        this.tooltipLan.setTitle(MSGS.firewallPortForwardFormLanAddressToolTip());
        this.tooltipProtocol.setTitle(MSGS.firewallPortForwardFormProtocolToolTip());
        this.tooltipInternal.setTitle(MSGS.firewallPortForwardFormInternalPortToolTip());
        this.tooltipExternal.setTitle(MSGS.firewallPortForwardFormExternalPortToolTip());
        this.tooltipEnable.setTitle(MSGS.firewallPortForwardFormMasqueradingToolTip());
        this.tooltipPermittedNw.setTitle(MSGS.firewallPortForwardFormPermittedNetworkToolTip());
        this.tooltipPermittedMac.setTitle(MSGS.firewallPortForwardFormPermittedMacAddressToolTip());
        this.tooltipSource.setTitle(MSGS.firewallPortForwardFormSourcePortRangeToolTip());
        this.tooltipInput.reconfigure();
        this.tooltipOutput.reconfigure();
        this.tooltipLan.reconfigure();
        this.tooltipProtocol.reconfigure();
        this.tooltipExternal.reconfigure();
        this.tooltipInternal.reconfigure();
        this.tooltipEnable.reconfigure();
        this.tooltipPermittedNw.reconfigure();
        this.tooltipPermittedMac.reconfigure();
        this.tooltipSource.reconfigure();
    }

    private void setModalFieldsLabels() {
        // setLabels
        this.labelInput.setText(MSGS.firewallPortForwardFormInboundInterface() + "*");
        this.labelOutput.setText(MSGS.firewallPortForwardFormOutboundInterface() + "*");
        this.labelLan.setText(MSGS.firewallPortForwardFormAddress() + "*");
        this.labelProtocol.setText(MSGS.firewallPortForwardFormProtocol());
        this.labelExternal.setText(MSGS.firewallPortForwardFormInPort() + "*");
        this.labelInternal.setText(MSGS.firewallPortForwardFormOutPort() + "*");
        this.labelEnable.setText(MSGS.firewallNatFormMasquerade());
        this.labelPermitttedNw.setText(MSGS.firewallPortForwardFormPermittedNetwork());
        this.labelPermitttedMac.setText(MSGS.firewallPortForwardFormPermittedMac());
        this.labelSource.setText(MSGS.firewallPortForwardFormSourcePortRange());
    }

    private boolean duplicateEntry(GwtFirewallPortForwardEntry portForwardEntry) {
        boolean isDuplicateEntry = false;
        List<GwtFirewallPortForwardEntry> entries = this.portForwardDataProvider.getList();
        if (entries != null && portForwardEntry != null) {
            for (GwtFirewallPortForwardEntry entry : entries) {
                if (entry.getInboundInterface().equals(portForwardEntry.getInboundInterface())
                        && entry.getOutboundInterface().equals(portForwardEntry.getOutboundInterface())
                        && entry.getAddress().equals(portForwardEntry.getAddress())
                        && entry.getProtocol().equals(portForwardEntry.getProtocol())
                        && entry.getOutPort().equals(portForwardEntry.getOutPort())
                        && entry.getInPort().equals(portForwardEntry.getInPort())) {

                    String permittedNetwork = entry.getPermittedNetwork() != null ? entry.getPermittedNetwork()
                            : "0.0.0.0/0";
                    String newPermittedNetwork = portForwardEntry.getPermittedNetwork() != null
                            ? portForwardEntry.getPermittedNetwork()
                            : "0.0.0.0/0";
                    String permittedMAC = entry.getPermittedMAC() != null ? entry.getPermittedMAC().toUpperCase() : "";
                    String newPermittedMAC = portForwardEntry.getPermittedMAC() != null
                            ? portForwardEntry.getPermittedMAC().toUpperCase()
                            : "";
                    String sourcePortRange = entry.getSourcePortRange() != null ? entry.getSourcePortRange() : "";
                    String newSourcePortRange = portForwardEntry.getSourcePortRange() != null
                            ? portForwardEntry.getSourcePortRange()
                            : "";

                    if (permittedNetwork.equals(newPermittedNetwork) && permittedMAC.equals(newPermittedMAC)
                            && sourcePortRange.equals(newSourcePortRange)) {
                        isDuplicateEntry = true;
                        break;
                    }
                }
            }
        }
        return isDuplicateEntry;
    }

    private void setVisibility() {
        if (this.portForwardDataProvider.getList().isEmpty()) {
            this.portForwardGrid.setVisible(false);
            this.notification.setVisible(true);
            this.notification.setText(MSGS.firewallPortForwardTableNoPorts());
        } else {
            this.portForwardGrid.setVisible(true);
            this.notification.setVisible(false);
        }
    }

    private void replaceModalHideHandler(ModalHideHandler hideHandler) {
        if (this.modalHideHandlerRegistration != null) {
            this.modalHideHandlerRegistration.removeHandler();
        }
        this.modalHideHandlerRegistration = this.portForwardingForm.addHideHandler(hideHandler);
    }

    private void resetFields() {
        PortForwardingTabUi.this.newPortForwardEntry = null;
        PortForwardingTabUi.this.editPortForwardEntry = null;
        PortForwardingTabUi.this.input.clear();
        PortForwardingTabUi.this.output.clear();
        PortForwardingTabUi.this.lan.clear();
        PortForwardingTabUi.this.external.clear();
        PortForwardingTabUi.this.internal.clear();
        PortForwardingTabUi.this.permittedNw.clear();
        PortForwardingTabUi.this.permittedMac.clear();
        PortForwardingTabUi.this.source.clear();
    }

    private boolean checkEntries() {
        boolean valid = true;

        if (PortForwardingTabUi.this.groupInput.getValidationState().equals(ValidationState.ERROR)
                || PortForwardingTabUi.this.input.getText() == null
                || "".equals(PortForwardingTabUi.this.input.getText().trim())) {
            PortForwardingTabUi.this.groupInput.setValidationState(ValidationState.ERROR);
            valid = false;
        }

        if (PortForwardingTabUi.this.groupOutput.getValidationState().equals(ValidationState.ERROR)
                || PortForwardingTabUi.this.output.getText() == null
                || "".equals(PortForwardingTabUi.this.output.getText().trim())) {
            PortForwardingTabUi.this.groupOutput.setValidationState(ValidationState.ERROR);
            valid = false;
        }

        if (PortForwardingTabUi.this.groupLan.getValidationState().equals(ValidationState.ERROR)
                || PortForwardingTabUi.this.lan.getText() == null
                || "".equals(PortForwardingTabUi.this.lan.getText().trim())) {
            PortForwardingTabUi.this.groupLan.setValidationState(ValidationState.ERROR);
            valid = false;
        }

        if (PortForwardingTabUi.this.groupInternal.getValidationState().equals(ValidationState.ERROR)
                || PortForwardingTabUi.this.internal.getText() == null
                || "".equals(PortForwardingTabUi.this.internal.getText().trim())) {
            PortForwardingTabUi.this.groupInternal.setValidationState(ValidationState.ERROR);
            valid = false;
        }

        if (PortForwardingTabUi.this.groupExternal.getValidationState().equals(ValidationState.ERROR)
                || PortForwardingTabUi.this.external.getText() == null
                || "".equals(PortForwardingTabUi.this.external.getText().trim())) {
            PortForwardingTabUi.this.groupExternal.setValidationState(ValidationState.ERROR);
            valid = false;
        }

        if (PortForwardingTabUi.this.groupPermittedNw.getValidationState().equals(ValidationState.ERROR)
                || PortForwardingTabUi.this.groupPermittedMac.getValidationState().equals(ValidationState.ERROR)
                || PortForwardingTabUi.this.groupSource.getValidationState().equals(ValidationState.ERROR)) {
            valid = false;
        }

        return valid;
    }

}
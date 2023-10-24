/*******************************************************************************
 * Copyright (c) 2011, 2023 Eurotech and/or its affiliates and others
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
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.ui.AlertDialog;
import org.eclipse.kura.web.client.ui.EntryClassUi;
import org.eclipse.kura.web.client.ui.Tab;
import org.eclipse.kura.web.client.util.FailureHandler;
import org.eclipse.kura.web.client.util.TextFieldValidator.FieldType;
import org.eclipse.kura.web.shared.model.GwtFirewallOpenPortEntry;
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

public class OpenPortsIPv6TabUi extends Composite implements Tab, ButtonBar.Listener {

    private static final String STATUS_TABLE_ROW = "status-table-row";

    private static OpenPortsTabUiUiBinder uiBinder = GWT.create(OpenPortsTabUiUiBinder.class);

    private final GwtSecurityTokenServiceAsync gwtXSRFService = GWT.create(GwtSecurityTokenService.class);
    private final GwtNetworkServiceAsync gwtNetworkService = GWT.create(GwtNetworkService.class);

    private static final Messages MSGS = GWT.create(Messages.class);

    interface OpenPortsTabUiUiBinder extends UiBinder<Widget, OpenPortsIPv6TabUi> {
    }

    private final ListDataProvider<GwtFirewallOpenPortEntry> openPortsDataProvider = new ListDataProvider<>();
    private final SingleSelectionModel<GwtFirewallOpenPortEntry> selectionModel = new SingleSelectionModel<>();

    private boolean dirty;

    GwtFirewallOpenPortEntry editOpenPortEntry;
    GwtFirewallOpenPortEntry newOpenPortEntry;
    GwtFirewallOpenPortEntry openPortEntry;

    @UiField
    ButtonBar buttonBar;
    @UiField
    Alert notification;

    @UiField
    AlertDialog alertDialog;

    @UiField
    Modal openPortsForm;

    @UiField
    FormGroup groupPort;
    @UiField
    FormGroup groupPermittedNw;
    @UiField
    FormGroup groupPermittedI;
    @UiField
    FormGroup groupUnpermittedI;
    @UiField
    FormGroup groupPermittedMac;
    @UiField
    FormGroup groupSource;

    @UiField
    FormLabel labelPort;
    @UiField
    FormLabel labelProtocol;
    @UiField
    FormLabel labelPermitttedNw;
    @UiField
    FormLabel labelPermitttedI;
    @UiField
    FormLabel labelUnPermitttedI;
    @UiField
    FormLabel labelPermitttedMac;
    @UiField
    FormLabel labelSource;

    @UiField
    TextBox port;
    @UiField
    TextBox permittedNw;
    @UiField
    TextBox permittedI;
    @UiField
    TextBox unpermittedI;
    @UiField
    TextBox permittedMac;
    @UiField
    TextBox source;

    @UiField
    Tooltip tooltipPort;
    @UiField
    Tooltip tooltipProtocol;
    @UiField
    Tooltip tooltipPermittedNw;
    @UiField
    Tooltip tooltipPermittedI;
    @UiField
    Tooltip tooltipUnpermittedI;
    @UiField
    Tooltip tooltipPermittedMac;
    @UiField
    Tooltip tooltipSource;

    @UiField
    Button submit;
    @UiField
    Button cancel;

    @UiField
    Modal existingRule;
    @UiField
    Button close;

    @UiField
    ListBox protocol;

    private HandlerRegistration modalHideHandlerRegistration;

    @UiField
    CellTable<GwtFirewallOpenPortEntry> openPortsGrid = new CellTable<>();

    public OpenPortsIPv6TabUi() {
        initWidget(uiBinder.createAndBindUi(this));
        this.selectionModel.addSelectionChangeHandler(event -> OpenPortsIPv6TabUi.this.buttonBar
                .setEditDeleteButtonsDirty(OpenPortsIPv6TabUi.this.selectionModel.getSelectedObject() != null));
        this.openPortsGrid.setSelectionModel(this.selectionModel);

        initTable();
        initNewRuleModal();
        initDuplicateRuleModal();
        this.buttonBar.setListener(this);

        // Initialize fixed fields for modal
        setModalFieldsLabels();
        setModalFieldsTooltips();
        setModalFieldsHandlers();
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
                OpenPortsIPv6TabUi.this.setDirty(false);
                OpenPortsIPv6TabUi.this.gwtNetworkService.findDeviceFirewallOpenPortsIPv6(token,
                        new AsyncCallback<List<GwtFirewallOpenPortEntry>>() {

                            @Override
                            public void onFailure(Throwable caught) {
                                EntryClassUi.hideWaitModal();
                                FailureHandler.handle(caught,
                                        OpenPortsIPv6TabUi.this.gwtNetworkService.getClass().getSimpleName());
                            }

                            @Override
                            public void onSuccess(List<GwtFirewallOpenPortEntry> result) {
                                for (GwtFirewallOpenPortEntry pair : result) {
                                    OpenPortsIPv6TabUi.this.openPortsDataProvider.getList().add(pair);
                                }
                                refreshTable();
                                setVisibility();
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
        this.openPortsDataProvider.getList().clear();
        OpenPortsIPv6TabUi.this.buttonBar.setApplyResetButtonsDirty(false);
        OpenPortsIPv6TabUi.this.buttonBar.setEditDeleteButtonsDirty(false);
        setVisibility();
        refreshTable();
    }

    //
    // Private methods
    //
    private void initTable() {

        TextColumn<GwtFirewallOpenPortEntry> col1 = new TextColumn<GwtFirewallOpenPortEntry>() {

            @Override
            public String getValue(GwtFirewallOpenPortEntry object) {
                if (object.getPortRange() != null) {
                    return String.valueOf(object.getPortRange());
                } else {
                    return "";
                }
            }
        };
        col1.setCellStyleNames(STATUS_TABLE_ROW);
        this.openPortsGrid.addColumn(col1, MSGS.firewallOpenPort());

        TextColumn<GwtFirewallOpenPortEntry> col2 = new TextColumn<GwtFirewallOpenPortEntry>() {

            @Override
            public String getValue(GwtFirewallOpenPortEntry object) {
                if (object.getProtocol() != null) {
                    return String.valueOf(object.getProtocol());
                } else {
                    return "";
                }
            }
        };
        col2.setCellStyleNames(STATUS_TABLE_ROW);
        this.openPortsGrid.addColumn(col2, MSGS.firewallOpenPortProtocol());

        TextColumn<GwtFirewallOpenPortEntry> col3 = new TextColumn<GwtFirewallOpenPortEntry>() {

            @Override
            public String getValue(GwtFirewallOpenPortEntry object) {
                if (object.getPermittedNetwork() != null) {
                    return String.valueOf(object.getPermittedNetwork());
                } else {
                    return "";
                }
            }
        };
        col3.setCellStyleNames(STATUS_TABLE_ROW);
        this.openPortsGrid.addColumn(col3, MSGS.firewallOpenPortPermittedNetwork());

        TextColumn<GwtFirewallOpenPortEntry> col4 = new TextColumn<GwtFirewallOpenPortEntry>() {

            @Override
            public String getValue(GwtFirewallOpenPortEntry object) {
                if (object.getPermittedInterfaceName() != null) {
                    return String.valueOf(object.getPermittedInterfaceName());
                } else {
                    return "";
                }
            }
        };
        col4.setCellStyleNames(STATUS_TABLE_ROW);
        this.openPortsGrid.addColumn(col4, MSGS.firewallOpenPortPermittedInterfaceName());

        TextColumn<GwtFirewallOpenPortEntry> col5 = new TextColumn<GwtFirewallOpenPortEntry>() {

            @Override
            public String getValue(GwtFirewallOpenPortEntry object) {
                if (object.getUnpermittedInterfaceName() != null) {
                    return String.valueOf(object.getUnpermittedInterfaceName());
                } else {
                    return "";
                }
            }
        };
        col5.setCellStyleNames(STATUS_TABLE_ROW);
        this.openPortsGrid.addColumn(col5, MSGS.firewallOpenPortUnpermittedInterfaceName());

        TextColumn<GwtFirewallOpenPortEntry> col6 = new TextColumn<GwtFirewallOpenPortEntry>() {

            @Override
            public String getValue(GwtFirewallOpenPortEntry object) {
                if (object.getPermittedMAC() != null) {
                    return String.valueOf(object.getPermittedMAC());
                } else {
                    return "";
                }
            }
        };
        col6.setCellStyleNames(STATUS_TABLE_ROW);
        this.openPortsGrid.addColumn(col6, MSGS.firewallOpenPortPermittedMac());

        TextColumn<GwtFirewallOpenPortEntry> col7 = new TextColumn<GwtFirewallOpenPortEntry>() {

            @Override
            public String getValue(GwtFirewallOpenPortEntry object) {
                if (object.getSourcePortRange() != null) {
                    return String.valueOf(object.getSourcePortRange());
                } else {
                    return "";
                }
            }
        };
        col7.setCellStyleNames(STATUS_TABLE_ROW);
        this.openPortsGrid.addColumn(col7, MSGS.firewallOpenPortSourcePortRange());

        this.openPortsDataProvider.addDataDisplay(this.openPortsGrid);
    }

    private void refreshTable() {
        Collections.sort(OpenPortsIPv6TabUi.this.openPortsDataProvider.getList(), new FirewallPanelUtils.PortSorting());
        int size = this.openPortsDataProvider.getList().size();
        this.openPortsGrid.setVisibleRange(0, size);
        this.openPortsDataProvider.flush();
        this.openPortsGrid.redraw();
        this.selectionModel.setSelected(this.selectionModel.getSelectedObject(), false);
    }

    @Override
    public void onApply() {
        List<GwtFirewallOpenPortEntry> intermediateList = OpenPortsIPv6TabUi.this.openPortsDataProvider.getList();
        final List<GwtFirewallOpenPortEntry> updatedOpenPortConf = new ArrayList<>();
        for (GwtFirewallOpenPortEntry entry : intermediateList) {
            updatedOpenPortConf.add(entry);
        }

        EntryClassUi.showWaitModal();
        OpenPortsIPv6TabUi.this.gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

            @Override
            public void onFailure(Throwable ex) {
                EntryClassUi.hideWaitModal();
                FailureHandler.handle(ex, OpenPortsIPv6TabUi.this.gwtXSRFService.getClass().getName());
            }

            @Override
            public void onSuccess(GwtXSRFToken token) {
                OpenPortsIPv6TabUi.this.gwtNetworkService.updateDeviceFirewallOpenPortsIPv6(token, updatedOpenPortConf,
                        new AsyncCallback<Void>() {

                            @Override
                            public void onFailure(Throwable caught) {
                                EntryClassUi.hideWaitModal();
                                FailureHandler.handle(caught,
                                        OpenPortsIPv6TabUi.this.gwtNetworkService.getClass().getSimpleName());
                            }

                            @Override
                            public void onSuccess(Void result) {
                                OpenPortsIPv6TabUi.this.buttonBar.setApplyResetButtonsDirty(false);
                                EntryClassUi.hideWaitModal();
                                setDirty(false);
                            }
                        });
            }
        });

    }

    @Override
    public void onCancel() {
        OpenPortsIPv6TabUi.this.alertDialog.show(MSGS.deviceConfigDirty(), OpenPortsIPv6TabUi.this::refresh);
    }

    @Override
    public void onCreate() {
        replaceModalHideHandler(evt -> {
            if (OpenPortsIPv6TabUi.this.newOpenPortEntry != null) {
                // Avoid duplicates
                OpenPortsIPv6TabUi.this.openPortsDataProvider.getList()
                        .remove(OpenPortsIPv6TabUi.this.newOpenPortEntry);
                if (!duplicateEntry(OpenPortsIPv6TabUi.this.newOpenPortEntry)) {
                    OpenPortsIPv6TabUi.this.openPortsDataProvider.getList()
                            .add(OpenPortsIPv6TabUi.this.newOpenPortEntry);
                    setVisibility();
                    refreshTable();
                    OpenPortsIPv6TabUi.this.buttonBar.setApplyResetButtonsDirty(true);
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
        GwtFirewallOpenPortEntry selection = OpenPortsIPv6TabUi.this.selectionModel.getSelectedObject();

        if (selection == null) {
            return;
        }

        replaceModalHideHandler(evt -> {
            if (OpenPortsIPv6TabUi.this.editOpenPortEntry != null) {
                GwtFirewallOpenPortEntry oldEntry = OpenPortsIPv6TabUi.this.selectionModel.getSelectedObject();
                OpenPortsIPv6TabUi.this.openPortsDataProvider.getList().remove(oldEntry);
                refreshTable();
                if (!duplicateEntry(OpenPortsIPv6TabUi.this.editOpenPortEntry)) {
                    OpenPortsIPv6TabUi.this.openPortsDataProvider.getList()
                            .add(OpenPortsIPv6TabUi.this.editOpenPortEntry);
                    OpenPortsIPv6TabUi.this.openPortsDataProvider.flush();
                    OpenPortsIPv6TabUi.this.buttonBar.setApplyResetButtonsDirty(true);
                    OpenPortsIPv6TabUi.this.editOpenPortEntry = null;
                    setVisibility();
                } else {
                    this.existingRule.show();
                    OpenPortsIPv6TabUi.this.openPortsDataProvider.getList().add(oldEntry);
                    OpenPortsIPv6TabUi.this.openPortsDataProvider.flush();
                }
                refreshTable();
                OpenPortsIPv6TabUi.this.buttonBar.setEditDeleteButtonsDirty(false);
                OpenPortsIPv6TabUi.this.selectionModel.setSelected(selection, false);
            }
            resetFields();
        });
        final AlertDialog.ConfirmListener listener = () -> showModal(
                OpenPortsIPv6TabUi.this.selectionModel.getSelectedObject());
        if (selection.getPortRange().equals("22")) {
            // show warning
            OpenPortsIPv6TabUi.this.alertDialog.show(MSGS.firewallOpenPorts22(), listener);
        } else if (selection.getPortRange().equals("80")) {
            // show warning
            OpenPortsIPv6TabUi.this.alertDialog.show(MSGS.firewallOpenPorts80(), listener);
        } else {
            showModal(selection);
        }

    }

    @Override
    public void onDelete() {
        GwtFirewallOpenPortEntry selection = OpenPortsIPv6TabUi.this.selectionModel.getSelectedObject();
        if (selection != null) {
            OpenPortsIPv6TabUi.this.alertDialog
                    .show(MSGS.firewallOpenPortDeleteConfirmation(String.valueOf(selection.getPortRange())), () -> {
                        OpenPortsIPv6TabUi.this.openPortsDataProvider.getList().remove(selection);
                        OpenPortsIPv6TabUi.this.buttonBar.setApplyResetButtonsDirty(true);
                        OpenPortsIPv6TabUi.this.buttonBar.setEditDeleteButtonsDirty(false);
                        OpenPortsIPv6TabUi.this.selectionModel.setSelected(selection, false);
                        setVisibility();
                        refreshTable();

                        setDirty(true);
                    });
        }
    }

    private void initNewRuleModal() {
        this.cancel.setText(MSGS.cancelButton());
        this.cancel.addClickHandler(event -> {
            this.openPortsForm.hide();
            resetFields();
        });

        this.submit.setText(MSGS.submitButton());
        this.submit.addClickHandler(event -> {

            if (!checkEntries()) {
                return;
            }

            // create a new entry
            this.openPortEntry = new GwtFirewallOpenPortEntry();
            this.openPortEntry.setPortRange(this.port.getText().trim());
            this.openPortEntry.setProtocol(this.protocol.getSelectedItemText());

            this.openPortEntry.setPermittedNetwork(validOrDefault(this.permittedNw.getText(), "::/0"));
            this.openPortEntry.setPermittedInterfaceName(validOrDefault(this.permittedI.getText(), null));
            this.openPortEntry.setUnpermittedInterfaceName(validOrDefault(this.unpermittedI.getText(), null));
            this.openPortEntry.setPermittedMAC(validOrDefault(this.permittedMac.getText(), null));
            this.openPortEntry.setSourcePortRange(validOrDefault(this.source.getText().trim(), null));

            if (OpenPortsIPv6TabUi.this.submit.getId().equals("new")) {
                OpenPortsIPv6TabUi.this.newOpenPortEntry = OpenPortsIPv6TabUi.this.openPortEntry;
                OpenPortsIPv6TabUi.this.editOpenPortEntry = null;
            } else if (OpenPortsIPv6TabUi.this.submit.getId().equals("edit")) {
                OpenPortsIPv6TabUi.this.editOpenPortEntry = OpenPortsIPv6TabUi.this.openPortEntry;
                OpenPortsIPv6TabUi.this.newOpenPortEntry = null;
            }

            setDirty(true);

            this.openPortsForm.hide();
        });
    }

    private static String validOrDefault(final String str, final String defaultValue) {
        if (str == null || str.trim().isEmpty()) {
            return defaultValue;
        }
        return str;
    }

    private void showModal(final GwtFirewallOpenPortEntry existingEntry) {
        resetValidationStates();

        if (existingEntry == null) {
            // new
            this.openPortsForm.setTitle(MSGS.firewallOpenPortFormInformation());
        } else {
            // edit existing entry
            this.openPortsForm.setTitle(MSGS.firewallOpenPortFormUpdate(String.valueOf(existingEntry.getPortRange())));
        }

        setModalFieldsValues(existingEntry);

        if (existingEntry == null) {
            this.submit.setId("new");
        } else {
            setEnableUnpermittedInterface();
            setEnablePermittedInterface();

            this.submit.setId("edit");
        }

        this.openPortsForm.show();
    }

    private void setEnablePermittedInterface() {
        if (!OpenPortsIPv6TabUi.this.unpermittedI.getText().trim().isEmpty()) {
            OpenPortsIPv6TabUi.this.permittedI.clear();
            OpenPortsIPv6TabUi.this.permittedI.setEnabled(false);
        } else {
            OpenPortsIPv6TabUi.this.permittedI.setEnabled(true);
        }
    }

    private void setEnableUnpermittedInterface() {
        if (!OpenPortsIPv6TabUi.this.permittedI.getText().trim().isEmpty()) {
            OpenPortsIPv6TabUi.this.unpermittedI.clear();
            OpenPortsIPv6TabUi.this.unpermittedI.setEnabled(false);
        } else {
            OpenPortsIPv6TabUi.this.unpermittedI.setEnabled(true);
        }
    }

    private void resetValidationStates() {
        OpenPortsIPv6TabUi.this.groupPort.setValidationState(ValidationState.NONE);
        OpenPortsIPv6TabUi.this.groupPermittedNw.setValidationState(ValidationState.NONE);
        OpenPortsIPv6TabUi.this.groupPermittedI.setValidationState(ValidationState.NONE);
        OpenPortsIPv6TabUi.this.groupPermittedI.setValidationState(ValidationState.NONE);
        OpenPortsIPv6TabUi.this.groupUnpermittedI.setValidationState(ValidationState.NONE);
        OpenPortsIPv6TabUi.this.groupPermittedMac.setValidationState(ValidationState.NONE);
        OpenPortsIPv6TabUi.this.groupSource.setValidationState(ValidationState.NONE);
    }

    private void setModalFieldsHandlers() {
        this.permittedI.addChangeHandler(event -> setEnableUnpermittedInterface());

        this.unpermittedI.addChangeHandler(event -> setEnablePermittedInterface());

        // set up validation
        this.port.addValidator(newPortValidator());
        this.port.addBlurHandler(event -> this.port.validate());

        this.permittedNw.addValidator(newPermittedNwValidator());
        this.permittedNw.addBlurHandler(event -> this.permittedNw.validate());

        this.permittedI.addValidator(newPermittedIValidator());
        this.permittedI.addBlurHandler(event -> this.permittedI.validate());

        this.unpermittedI.addValidator(newUnpermittedIValidator());
        this.unpermittedI.addBlurHandler(event -> this.unpermittedI.validate());

        this.permittedMac.addValidator(newPermittedMacValidator());
        this.permittedMac.addBlurHandler(event -> this.permittedMac.validate());

        this.source.addValidator(newSourceValidator());
        this.source.addBlurHandler(event -> this.source.validate());
    }

    private Validator<String> newSourceValidator() {
        return new Validator<String>() {

            @Override
            public List<EditorError> validate(Editor<String> editor, String value) {
                List<EditorError> result = new ArrayList<>();
                if (OpenPortsIPv6TabUi.this.source.getText().trim().length() > 0 && (!(FirewallPanelUtils
                        .checkPortRegex(OpenPortsIPv6TabUi.this.source.getText().trim())
                        || FirewallPanelUtils.checkPortRangeRegex(OpenPortsIPv6TabUi.this.source.getText().trim()))
                        || !FirewallPanelUtils.isPortInRange(OpenPortsIPv6TabUi.this.source.getText().trim()))) {
                    result.add(new BasicEditorError(OpenPortsIPv6TabUi.this.source, value,
                            MSGS.firewallOpenPortFormSourcePortRangeErrorMessage()));
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
                if (!OpenPortsIPv6TabUi.this.permittedMac.getText().trim().matches(FieldType.MAC_ADDRESS.getRegex())
                        && OpenPortsIPv6TabUi.this.permittedMac.getText().trim().length() > 0) {
                    result.add(new BasicEditorError(OpenPortsIPv6TabUi.this.permittedMac, value,
                            MSGS.firewallOpenPortFormPermittedMacAddressErrorMessage()));
                }
                return result;
            }

            @Override
            public int getPriority() {
                return 0;
            }
        };
    }

    private Validator<String> newUnpermittedIValidator() {
        return new Validator<String>() {

            @Override
            public List<EditorError> validate(Editor<String> editor, String value) {
                List<EditorError> result = new ArrayList<>();
                if (!OpenPortsIPv6TabUi.this.unpermittedI.getText().trim().matches(FieldType.ALPHANUMERIC.getRegex())
                        && OpenPortsIPv6TabUi.this.unpermittedI.getText().trim().length() > 0
                        || OpenPortsIPv6TabUi.this.unpermittedI.getText().trim()
                                .length() > FirewallPanelUtils.INTERFACE_NAME_MAX_LENGTH) {
                    result.add(new BasicEditorError(OpenPortsIPv6TabUi.this.unpermittedI, value,
                            MSGS.firewallOpenPortFormUnpermittedInterfaceErrorMessage()));
                }
                return result;
            }

            @Override
            public int getPriority() {
                return 0;
            }
        };
    }

    private Validator<String> newPermittedIValidator() {
        return new Validator<String>() {

            @Override
            public List<EditorError> validate(Editor<String> editor, String value) {
                List<EditorError> result = new ArrayList<>();
                if (!OpenPortsIPv6TabUi.this.permittedI.getText().trim().matches(FieldType.ALPHANUMERIC.getRegex())
                        && OpenPortsIPv6TabUi.this.permittedI.getText().trim().length() > 0
                        || OpenPortsIPv6TabUi.this.permittedI.getText().trim()
                                .length() > FirewallPanelUtils.INTERFACE_NAME_MAX_LENGTH) {
                    result.add(new BasicEditorError(OpenPortsIPv6TabUi.this.permittedI, value,
                            MSGS.firewallOpenPortFormPermittedInterfaceErrorMessage()));
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
                if (!OpenPortsIPv6TabUi.this.permittedNw.getText().trim().matches(FieldType.IPV6_CIDR_NOTATION.getRegex())
                        && OpenPortsIPv6TabUi.this.permittedNw.getText().trim().length() > 0) {
                    result.add(new BasicEditorError(OpenPortsIPv6TabUi.this.permittedNw, value,
                            MSGS.firewallOpenPortFormPermittedNetworkIPv6ErrorMessage()));
                }
                return result;
            }

            @Override
            public int getPriority() {
                return 0;
            }
        };
    }

    private Validator<String> newPortValidator() {
        return new Validator<String>() {

            @Override
            public List<EditorError> validate(Editor<String> editor, String value) {
                List<EditorError> result = new ArrayList<>();
                if (OpenPortsIPv6TabUi.this.port.getText() == null
                        || "".equals(OpenPortsIPv6TabUi.this.port.getText().trim())
                        || OpenPortsIPv6TabUi.this.port.getText().trim().length() == 0
                        || !(FirewallPanelUtils.checkPortRegex(OpenPortsIPv6TabUi.this.port.getText().trim())
                                || FirewallPanelUtils
                                        .checkPortRangeRegex(OpenPortsIPv6TabUi.this.port.getText().trim()))
                        || !FirewallPanelUtils.isPortInRange(OpenPortsIPv6TabUi.this.port.getText().trim())) {
                    result.add(new BasicEditorError(OpenPortsIPv6TabUi.this.port, value,
                            MSGS.firewallOpenPortFormPortErrorMessage()));
                }
                return result;
            }

            @Override
            public int getPriority() {
                return 0;
            }
        };
    }

    private void setModalFieldsTooltips() {
        // Port config
        this.tooltipPort.setTitle(MSGS.firewallOpenPortFormPortToolTip());
        this.tooltipPort.reconfigure();

        // Protocol config
        this.tooltipProtocol.setTitle(MSGS.firewallOpenPortFormProtocolToolTip());
        this.tooltipProtocol.reconfigure();

        // Permitted Network config
        this.tooltipPermittedNw.setTitle(MSGS.firewallOpenPortFormPermittedNetworkIPv6ToolTip());
        this.tooltipPermittedNw.reconfigure();

        // Permitted Interface config
        this.tooltipPermittedI.setTitle(MSGS.firewallOpenPortFormPermittedInterfaceToolTip());
        this.tooltipPermittedI.reconfigure();

        // UnPermitted Interface config
        this.tooltipUnpermittedI.setTitle(MSGS.firewallOpenPortFormUnpermittedInterfaceToolTip());
        this.tooltipUnpermittedI.reconfigure();

        // Permitted Mac Address config
        this.tooltipPermittedMac.setTitle(MSGS.firewallOpenPortFormPermittedMacAddressToolTip());
        this.tooltipPermittedMac.reconfigure();

        // Source config
        this.tooltipSource.setTitle(MSGS.firewallOpenPortFormSourcePortRangeToolTip());
        this.tooltipSource.reconfigure();
    }

    private void setModalFieldsValues(final GwtFirewallOpenPortEntry existingEntry) {
        // populate existing values
        if (existingEntry != null) {
            this.port.setText(String.valueOf(existingEntry.getPortRange()));
            this.protocol.setSelectedIndex(existingEntry.getProtocol().equals(GwtNetProtocol.tcp.name()) ? 0 : 1);

            this.permittedNw.setText(existingEntry.getPermittedNetwork());
            this.permittedI.setText(existingEntry.getPermittedInterfaceName());
            this.unpermittedI.setText(existingEntry.getUnpermittedInterfaceName());
            this.permittedMac.setText(existingEntry.getPermittedMAC());
            this.source.setText(existingEntry.getSourcePortRange());
        } else {
            this.port.setText("");
            this.protocol.setSelectedIndex(0);

            this.permittedNw.setText("");
            this.permittedI.setText("");
            this.permittedI.setEnabled(true);
            this.unpermittedI.setText("");
            this.unpermittedI.setEnabled(true);
            this.permittedMac.setText("");
            this.source.setText("");
        }
    }

    private void setModalFieldsLabels() {
        // set Labels
        this.labelPort.setText(MSGS.firewallOpenPortFormPort() + "*");
        this.labelProtocol.setText(MSGS.firewallOpenPortFormProtocol());
        this.protocol.clear();
        this.protocol.addItem(GwtNetProtocol.tcp.name());
        this.protocol.addItem(GwtNetProtocol.udp.name());
        this.labelPermitttedNw.setText(MSGS.firewallOpenPortFormPermittedIPv6Network());
        this.labelPermitttedI.setText(MSGS.firewallOpenPortFormPermittedInterfaceName());
        this.labelUnPermitttedI.setText(MSGS.firewallOpenPortFormUnpermittedInterfaceName());
        this.labelPermitttedMac.setText(MSGS.firewallOpenPortFormPermittedMac());
        this.labelSource.setText(MSGS.firewallOpenPortFormSourcePortRange());
    }

    private boolean duplicateEntry(GwtFirewallOpenPortEntry openPortEntry) {
        List<GwtFirewallOpenPortEntry> entries = this.openPortsDataProvider.getList();
        if (entries != null && openPortEntry != null) {
            for (GwtFirewallOpenPortEntry entry : entries) {
                Map<String, Object> savedEntry = entry.getProperties();
                Map<String, Object> newEntry = openPortEntry.getProperties();

                if (newEntry.equals(savedEntry)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void setVisibility() {
        if (this.openPortsDataProvider.getList().isEmpty()) {
            this.openPortsGrid.setVisible(false);
            this.notification.setVisible(true);
            this.notification.setText(MSGS.firewallOpenPortTableNoPorts());
        } else {
            this.openPortsGrid.setVisible(true);
            this.notification.setVisible(false);
        }
    }

    private void replaceModalHideHandler(ModalHideHandler hideHandler) {
        if (this.modalHideHandlerRegistration != null) {
            this.modalHideHandlerRegistration.removeHandler();
        }
        this.modalHideHandlerRegistration = this.openPortsForm.addHideHandler(hideHandler);
    }

    private void resetFields() {
        this.openPortEntry = null;
        this.editOpenPortEntry = null;
        this.newOpenPortEntry = null;
        this.port.clear();
        this.permittedNw.clear();
        this.permittedI.clear();
        this.unpermittedI.clear();
        this.permittedMac.clear();
        this.source.clear();
    }

    private boolean checkEntries() {
        boolean valid = true;

        if (this.groupPort.getValidationState() == ValidationState.ERROR || this.port.getText() == null
                || "".equals(this.port.getText().trim())) {
            this.groupPort.setValidationState(ValidationState.ERROR);
            valid = false;
        }

        if (this.groupPermittedNw.getValidationState() == ValidationState.ERROR
                || this.groupPermittedI.getValidationState() == ValidationState.ERROR
                || this.groupUnpermittedI.getValidationState() == ValidationState.ERROR
                || this.groupPermittedMac.getValidationState() == ValidationState.ERROR
                || this.groupSource.getValidationState() == ValidationState.ERROR) {
            valid = false;
        }

        return valid;
    }

}

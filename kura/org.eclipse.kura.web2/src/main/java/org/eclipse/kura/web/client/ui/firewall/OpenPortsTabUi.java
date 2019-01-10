/*******************************************************************************
 * Copyright (c) 2011, 2018 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.web.client.ui.firewall;

import java.util.ArrayList;
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
import org.gwtbootstrap3.client.ui.gwt.CellTable;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SingleSelectionModel;
import com.google.web.bindery.event.shared.HandlerRegistration;

public class OpenPortsTabUi extends Composite implements Tab, ButtonBar.Listener {

    private static final String STATUS_TABLE_ROW = "status-table-row";

    private static OpenPortsTabUiUiBinder uiBinder = GWT.create(OpenPortsTabUiUiBinder.class);

    private final GwtSecurityTokenServiceAsync gwtXSRFService = GWT.create(GwtSecurityTokenService.class);
    private final GwtNetworkServiceAsync gwtNetworkService = GWT.create(GwtNetworkService.class);

    private static final Messages MSGS = GWT.create(Messages.class);

    interface OpenPortsTabUiUiBinder extends UiBinder<Widget, OpenPortsTabUi> {
    }

    private final ListDataProvider<GwtFirewallOpenPortEntry> openPortsDataProvider = new ListDataProvider<>();
    final SingleSelectionModel<GwtFirewallOpenPortEntry> selectionModel = new SingleSelectionModel<>();

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
    FormLabel labelsource;

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
    Tooltip tooltipPermittedI;
    @UiField
    Tooltip tooltipUnpermittedI;

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

    public OpenPortsTabUi() {
        initWidget(uiBinder.createAndBindUi(this));
        this.openPortsGrid.setSelectionModel(this.selectionModel);

        initTable();
        initNewRuleModal();
        initDuplicateRuleModal();
        this.buttonBar.setListener(this);
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
        this.openPortsDataProvider.getList().clear();
        this.notification.setVisible(false);
        this.gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

            @Override
            public void onFailure(Throwable ex) {
                EntryClassUi.hideWaitModal();
                FailureHandler.handle(ex);
            }

            @Override
            public void onSuccess(GwtXSRFToken token) {
                OpenPortsTabUi.this.gwtNetworkService.findDeviceFirewallOpenPorts(token,
                        new AsyncCallback<List<GwtFirewallOpenPortEntry>>() {

                            @Override
                            public void onFailure(Throwable caught) {
                                EntryClassUi.hideWaitModal();
                                FailureHandler.handle(caught,
                                        OpenPortsTabUi.this.gwtNetworkService.getClass().getSimpleName());
                            }

                            @Override
                            public void onSuccess(List<GwtFirewallOpenPortEntry> result) {
                                for (GwtFirewallOpenPortEntry pair : result) {
                                    OpenPortsTabUi.this.openPortsDataProvider.getList().add(pair);
                                }
                                refreshTable();
                                setVisibility();
                                OpenPortsTabUi.this.buttonBar.setDirty(false);
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
        int size = this.openPortsDataProvider.getList().size();
        this.openPortsGrid.setVisibleRange(0, size);
        this.openPortsDataProvider.flush();
        this.openPortsGrid.redraw();
    }

    @Override
    public void onApply() {
        List<GwtFirewallOpenPortEntry> intermediateList = OpenPortsTabUi.this.openPortsDataProvider.getList();
        final List<GwtFirewallOpenPortEntry> updatedOpenPortConf = new ArrayList<>();
        for (GwtFirewallOpenPortEntry entry : intermediateList) {
            updatedOpenPortConf.add(entry);
        }

        EntryClassUi.showWaitModal();
        OpenPortsTabUi.this.gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

            @Override
            public void onFailure(Throwable ex) {
                EntryClassUi.hideWaitModal();
                FailureHandler.handle(ex, OpenPortsTabUi.this.gwtXSRFService.getClass().getName());
            }

            @Override
            public void onSuccess(GwtXSRFToken token) {
                OpenPortsTabUi.this.gwtNetworkService.updateDeviceFirewallOpenPorts(token, updatedOpenPortConf,
                        new AsyncCallback<Void>() {

                            @Override
                            public void onFailure(Throwable caught) {
                                EntryClassUi.hideWaitModal();
                                FailureHandler.handle(caught,
                                        OpenPortsTabUi.this.gwtNetworkService.getClass().getSimpleName());
                            }

                            @Override
                            public void onSuccess(Void result) {
                                OpenPortsTabUi.this.buttonBar.setDirty(false);
                                EntryClassUi.hideWaitModal();
                                setDirty(false);
                            }
                        });
            }
        });

    }

    @Override
    public void onCancel() {
        OpenPortsTabUi.this.alertDialog.show(MSGS.deviceConfigDirty(), OpenPortsTabUi.this::refresh);
    }

    @Override
    public void onCreate() {
        replaceModalHideHandler(evt -> {

            if (OpenPortsTabUi.this.newOpenPortEntry != null) {
                if (!duplicateEntry(OpenPortsTabUi.this.newOpenPortEntry)) {
                    OpenPortsTabUi.this.openPortsDataProvider.getList().add(OpenPortsTabUi.this.newOpenPortEntry);
                    refreshTable();
                    setVisibility();
                    OpenPortsTabUi.this.buttonBar.setDirty(true);
                } else {
                    existingRule.show();
                }
            }
        });
        showModal(null);
    }

    @Override
    public void onEdit() {
        GwtFirewallOpenPortEntry selection = OpenPortsTabUi.this.selectionModel.getSelectedObject();

        if (selection == null) {
            return;
        }

        replaceModalHideHandler(evt -> {

            if (OpenPortsTabUi.this.editOpenPortEntry != null) {
                GwtFirewallOpenPortEntry oldEntry = OpenPortsTabUi.this.selectionModel.getSelectedObject();

                OpenPortsTabUi.this.openPortsDataProvider.getList()
                        .remove(OpenPortsTabUi.this.selectionModel.getSelectedObject());
                refreshTable();
                if (!duplicateEntry(OpenPortsTabUi.this.editOpenPortEntry)) {
                    OpenPortsTabUi.this.openPortsDataProvider.getList().add(OpenPortsTabUi.this.editOpenPortEntry);
                    OpenPortsTabUi.this.openPortsDataProvider.flush();
                    OpenPortsTabUi.this.buttonBar.setDirty(true);
                    OpenPortsTabUi.this.editOpenPortEntry = null;
                    setVisibility();
                } else {
                    existingRule.show();
                    OpenPortsTabUi.this.openPortsDataProvider.getList().add(oldEntry);
                    OpenPortsTabUi.this.openPortsDataProvider.flush();
                }
                refreshTable();
            }
        });
        final AlertDialog.Listener listener = () -> showModal(OpenPortsTabUi.this.selectionModel.getSelectedObject());
        if (selection.getPortRange().equals("22")) {
            // show warning
            OpenPortsTabUi.this.alertDialog.show(MSGS.firewallOpenPorts22(), listener);
        } else if (selection.getPortRange().equals("80")) {
            // show warning
            OpenPortsTabUi.this.alertDialog.show(MSGS.firewallOpenPorts80(), listener);
        } else {
            showModal(selection);
        }

    }

    @Override
    public void onDelete() {
        GwtFirewallOpenPortEntry selection = OpenPortsTabUi.this.selectionModel.getSelectedObject();
        if (selection != null) {
            OpenPortsTabUi.this.alertDialog
                    .show(MSGS.firewallOpenPortDeleteConfirmation(String.valueOf(selection.getPortRange())), () -> {
                        OpenPortsTabUi.this.openPortsDataProvider.getList()
                                .remove(OpenPortsTabUi.this.selectionModel.getSelectedObject());
                        refreshTable();
                        OpenPortsTabUi.this.buttonBar.setDirty(true);
                        setVisibility();

                        setDirty(true);
                    });
        }
    }

    private void initNewRuleModal() {
        this.cancel.setText(MSGS.cancelButton());
        this.cancel.addClickHandler(event -> {
            this.openPortsForm.hide();
            this.openPortEntry = null;
            this.editOpenPortEntry = null;
            this.newOpenPortEntry = null;
        });

        this.submit.setText(MSGS.submitButton());
        this.submit.addClickHandler(event -> {

            checkFieldsValues();

            if (this.groupPort.getValidationState() == ValidationState.ERROR
                    || this.groupPermittedNw.getValidationState() == ValidationState.ERROR
                    || this.groupPermittedI.getValidationState() == ValidationState.ERROR
                    || this.groupUnpermittedI.getValidationState() == ValidationState.ERROR
                    || this.groupPermittedMac.getValidationState() == ValidationState.ERROR
                    || this.groupSource.getValidationState() == ValidationState.ERROR) {
                return;
            }

            // create a new entry
            this.openPortEntry = new GwtFirewallOpenPortEntry();
            this.openPortEntry.setPortRange(this.port.getText());
            this.openPortEntry.setProtocol(this.protocol.getSelectedItemText());

            this.openPortEntry.setPermittedNetwork(validOrDefault(this.permittedNw.getText(), "0.0.0.0/0"));
            this.openPortEntry.setPermittedInterfaceName(validOrDefault(this.permittedI.getText(), null));
            this.openPortEntry.setUnpermittedInterfaceName(validOrDefault(this.unpermittedI.getText(), null));
            this.openPortEntry.setPermittedMAC(validOrDefault(this.permittedMac.getText(), null));
            this.openPortEntry.setSourcePortRange(validOrDefault(this.source.getText(), null));

            if (OpenPortsTabUi.this.submit.getId().equals("new")) {
                OpenPortsTabUi.this.newOpenPortEntry = OpenPortsTabUi.this.openPortEntry;
                OpenPortsTabUi.this.editOpenPortEntry = null;
            } else if (OpenPortsTabUi.this.submit.getId().equals("edit")) {
                OpenPortsTabUi.this.editOpenPortEntry = OpenPortsTabUi.this.openPortEntry;
                OpenPortsTabUi.this.newOpenPortEntry = null;
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
        if (existingEntry == null) {
            // new
            this.openPortsForm.setTitle(MSGS.firewallOpenPortFormInformation());
        } else {
            // edit existing entry
            this.openPortsForm.setTitle(MSGS.firewallOpenPortFormUpdate(String.valueOf(existingEntry.getPortRange())));
        }

        setModalFieldsLabels();

        setModalFieldsValues(existingEntry);

        setModalFieldsTooltips();

        setModalFieldsHandlers();

        if (existingEntry == null) {
            this.submit.setId("new");
        } else {
            this.submit.setId("edit");
        }

        this.openPortsForm.show();
    }

    private void setModalFieldsHandlers() {
        this.permittedI.addChangeHandler(event -> {
            if (!OpenPortsTabUi.this.permittedI.getText().trim().isEmpty()) {
                OpenPortsTabUi.this.unpermittedI.clear();
                OpenPortsTabUi.this.unpermittedI.setEnabled(false);
            } else {
                OpenPortsTabUi.this.unpermittedI.setEnabled(true);
            }
        });

        this.unpermittedI.addChangeHandler(event -> {
            if (!OpenPortsTabUi.this.unpermittedI.getText().trim().isEmpty()) {
                OpenPortsTabUi.this.permittedI.clear();
                OpenPortsTabUi.this.permittedI.setEnabled(false);
            } else {
                OpenPortsTabUi.this.permittedI.setEnabled(true);
            }
        });

        // set up validation
        this.port.addBlurHandler(event -> {
            if (!OpenPortsTabUi.this.port.getText().trim().matches(FieldType.PORT_RANGE.getRegex())
                    && OpenPortsTabUi.this.port.getText().trim().length() != 0
                    || OpenPortsTabUi.this.port.getText() == null
                    || "".equals(OpenPortsTabUi.this.port.getText().trim())) {
                OpenPortsTabUi.this.groupPort.setValidationState(ValidationState.ERROR);
            } else {
                OpenPortsTabUi.this.groupPort.setValidationState(ValidationState.NONE);
            }
        });

        this.permittedNw.addBlurHandler(event -> {
            if (!OpenPortsTabUi.this.permittedNw.getText().trim().matches(FieldType.NETWORK.getRegex())
                    && OpenPortsTabUi.this.permittedNw.getText().trim().length() > 0) {
                OpenPortsTabUi.this.groupPermittedNw.setValidationState(ValidationState.ERROR);
            } else {
                OpenPortsTabUi.this.groupPermittedNw.setValidationState(ValidationState.NONE);
            }
        });
        this.permittedI.addBlurHandler(event -> {
            if (!OpenPortsTabUi.this.permittedI.getText().trim().matches(FieldType.ALPHANUMERIC.getRegex())
                    && OpenPortsTabUi.this.permittedI.getText().trim().length() > 0) {
                OpenPortsTabUi.this.groupPermittedI.setValidationState(ValidationState.ERROR);
            } else {
                OpenPortsTabUi.this.groupPermittedI.setValidationState(ValidationState.NONE);
            }
        });
        this.unpermittedI.addBlurHandler(event -> {
            if (!OpenPortsTabUi.this.unpermittedI.getText().trim().matches(FieldType.ALPHANUMERIC.getRegex())
                    && OpenPortsTabUi.this.unpermittedI.getText().trim().length() > 0) {
                OpenPortsTabUi.this.groupUnpermittedI.setValidationState(ValidationState.ERROR);
            } else {
                OpenPortsTabUi.this.groupPermittedI.setValidationState(ValidationState.NONE);
            }
        });
        this.permittedMac.addBlurHandler(event -> {
            if (!OpenPortsTabUi.this.permittedMac.getText().trim().matches(FieldType.MAC_ADDRESS.getRegex())
                    && OpenPortsTabUi.this.permittedMac.getText().trim().length() > 0) {
                OpenPortsTabUi.this.groupPermittedMac.setValidationState(ValidationState.ERROR);
            } else {
                OpenPortsTabUi.this.groupPermittedMac.setValidationState(ValidationState.NONE);
            }
        });
        this.source.addBlurHandler(event -> {
            if (!OpenPortsTabUi.this.source.getText().trim().matches(FieldType.PORT_RANGE.getRegex())
                    && OpenPortsTabUi.this.source.getText().trim().length() > 0) {
                OpenPortsTabUi.this.groupSource.setValidationState(ValidationState.ERROR);
            } else {
                OpenPortsTabUi.this.groupSource.setValidationState(ValidationState.NONE);
            }
        });
    }

    private void setModalFieldsTooltips() {
        // Permitted Interface config
        this.tooltipPermittedI.setTitle(MSGS.firewallOpenPortFormPermittedInterfaceToolTip());
        this.tooltipPermittedI.reconfigure();

        // UnPermitted Interface config
        this.tooltipUnpermittedI.setTitle(MSGS.firewallOpenPortFormUnpermittedInterfaceToolTip());
        this.tooltipUnpermittedI.reconfigure();
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
        this.labelPermitttedNw.setText(MSGS.firewallOpenPortFormPermittedNetwork());
        this.labelPermitttedI.setText(MSGS.firewallOpenPortFormPermittedInterfaceName());
        this.labelUnPermitttedI.setText(MSGS.firewallOpenPortFormUnpermittedInterfaceName());
        this.labelPermitttedMac.setText(MSGS.firewallOpenPortFormPermittedMac());
        this.labelsource.setText(MSGS.firewallOpenPortFormSourcePortRange());
    }

    private void checkFieldsValues() {
        if (this.port.getText().trim().isEmpty()) {
            this.groupPort.setValidationState(ValidationState.ERROR);
        }
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
}

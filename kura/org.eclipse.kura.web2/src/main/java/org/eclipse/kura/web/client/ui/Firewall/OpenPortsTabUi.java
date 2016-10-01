/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.web.client.ui.Firewall;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.kura.web.client.messages.Messages;
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
import org.gwtbootstrap3.client.shared.event.ModalHideEvent;
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
import org.gwtbootstrap3.client.ui.html.Span;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SingleSelectionModel;

public class OpenPortsTabUi extends Composite implements Tab {

    private static OpenPortsTabUiUiBinder uiBinder = GWT.create(OpenPortsTabUiUiBinder.class);

    private final GwtSecurityTokenServiceAsync gwtXSRFService = GWT.create(GwtSecurityTokenService.class);
    private final GwtNetworkServiceAsync gwtNetworkService = GWT.create(GwtNetworkService.class);

    private static final Messages MSGS = GWT.create(Messages.class);

    interface OpenPortsTabUiUiBinder extends UiBinder<Widget, OpenPortsTabUi> {
    }

    private final ListDataProvider<GwtFirewallOpenPortEntry> openPortsDataProvider = new ListDataProvider<GwtFirewallOpenPortEntry>();
    final SingleSelectionModel<GwtFirewallOpenPortEntry> selectionModel = new SingleSelectionModel<GwtFirewallOpenPortEntry>();

    private boolean m_dirty;

    GwtFirewallOpenPortEntry editOpenPortEntry, newOpenPortEntry, openPortEntry;

    @UiField
    Button apply, create, edit, delete;
    @UiField
    Alert notification;

    @UiField
    Modal openPortsForm, alert;
    @UiField
    FormGroup groupPort, groupPermittedNw, groupPermittedI, groupUnpermittedI, groupPermittedMac, groupSource;
    @UiField
    FormLabel labelPort, labelProtocol, labelPermitttedNw, labelPermitttedI, labelUnPermitttedI, labelPermitttedMac,
            labelsource;
    @UiField
    TextBox port, permittedNw, permittedI, unpermittedI, permittedMac, source;
    @UiField
    Tooltip tooltipPermittedI, tooltipUnpermittedI;
    @UiField
    Button submit, cancel, yes, no;
    @UiField
    ListBox protocol;

    @UiField
    Span alertBody;

    @UiField
    CellTable<GwtFirewallOpenPortEntry> openPortsGrid = new CellTable<GwtFirewallOpenPortEntry>();

    public OpenPortsTabUi() {
        initWidget(uiBinder.createAndBindUi(this));
        this.apply.setText(MSGS.firewallApply());
        this.create.setText(MSGS.newButton());
        this.edit.setText(MSGS.editButton());
        this.delete.setText(MSGS.deleteButton());
        this.openPortsGrid.setSelectionModel(this.selectionModel);

        initButtons();
        initTable();
        initModal();
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
                        FailureHandler.handle(caught, OpenPortsTabUi.this.gwtNetworkService.getClass().getSimpleName());
                    }

                    @Override
                    public void onSuccess(List<GwtFirewallOpenPortEntry> result) {
                        for (GwtFirewallOpenPortEntry pair : result) {
                            OpenPortsTabUi.this.openPortsDataProvider.getList().add(pair);
                        }
                        refreshTable();
                        setVisibility();
                        OpenPortsTabUi.this.apply.setEnabled(false);
                        EntryClassUi.hideWaitModal();
                    }
                });
            }

        });
    }

    @Override
    public boolean isDirty() {
        return this.m_dirty;
    }

    @Override
    public void setDirty(boolean b) {
        this.m_dirty = b;
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
        col1.setCellStyleNames("status-table-row");
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
        col2.setCellStyleNames("status-table-row");
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
        col3.setCellStyleNames("status-table-row");
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
        col4.setCellStyleNames("status-table-row");
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
        col5.setCellStyleNames("status-table-row");
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
        col6.setCellStyleNames("status-table-row");
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
        col7.setCellStyleNames("status-table-row");
        this.openPortsGrid.addColumn(col7, MSGS.firewallOpenPortSourcePortRange());

        this.openPortsDataProvider.addDataDisplay(this.openPortsGrid);
    }

    private void refreshTable() {
        int size = this.openPortsDataProvider.getList().size();
        this.openPortsGrid.setVisibleRange(0, size);
        this.openPortsDataProvider.flush();
        this.openPortsGrid.redraw();
    }

    // Initialize tab buttons
    private void initButtons() {
        initApplyButton();

        initCreateButton();

        initEditButton();

        initDeleteButton();
    }

    private void initApplyButton() {
        this.apply.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                List<GwtFirewallOpenPortEntry> intermediateList = OpenPortsTabUi.this.openPortsDataProvider.getList();
                ArrayList<GwtFirewallOpenPortEntry> tempList = new ArrayList<GwtFirewallOpenPortEntry>();
                final List<GwtFirewallOpenPortEntry> updatedOpenPortConf = tempList;
                for (GwtFirewallOpenPortEntry entry : intermediateList) {
                    tempList.add(entry);
                }

                if (updatedOpenPortConf != null) {
                    EntryClassUi.showWaitModal();
                    OpenPortsTabUi.this.gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

                        @Override
                        public void onFailure(Throwable ex) {
                            EntryClassUi.hideWaitModal();
                            FailureHandler.handle(ex, OpenPortsTabUi.this.gwtXSRFService.getClass().getName());
                        }

                        @Override
                        public void onSuccess(GwtXSRFToken token) {
                            OpenPortsTabUi.this.gwtNetworkService.updateDeviceFirewallOpenPorts(token,
                                    updatedOpenPortConf, new AsyncCallback<Void>() {

                                @Override
                                public void onFailure(Throwable caught) {
                                    EntryClassUi.hideWaitModal();
                                    FailureHandler.handle(caught,
                                            OpenPortsTabUi.this.gwtNetworkService.getClass().getSimpleName());
                                }

                                @Override
                                public void onSuccess(Void result) {
                                    OpenPortsTabUi.this.apply.setEnabled(false);
                                    EntryClassUi.hideWaitModal();
                                    setDirty(false);
                                }
                            });
                        }
                    });
                }
            }
        });
    }

    private void initCreateButton() {
        this.create.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                showModal(null);
            }
        });
        // TODO add warnings for port 80 and 22
        this.openPortsForm.addHideHandler(new ModalHideHandler() {

            @Override
            public void onHide(ModalHideEvent evt) {

                if (OpenPortsTabUi.this.newOpenPortEntry != null) {
                    if (!duplicateEntry(OpenPortsTabUi.this.newOpenPortEntry)) {
                        OpenPortsTabUi.this.openPortsDataProvider.getList().add(OpenPortsTabUi.this.newOpenPortEntry);
                        refreshTable();
                        setVisibility();
                        OpenPortsTabUi.this.apply.setEnabled(true);
                    } else {
                        // Growl.growl(MSGS.firewallOpenPortFormError()
                        // + ": ",
                        // MSGS.firewallOpenPortFormDuplicate());
                    }
                }
            }
        });
    }

    private void initEditButton() {
        this.edit.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                GwtFirewallOpenPortEntry selection = OpenPortsTabUi.this.selectionModel.getSelectedObject();
                if (selection != null) {
                    if (selection.getPortRange().equals("22")) {
                        // show warning
                        OpenPortsTabUi.this.alertBody.setText(MSGS.firewallOpenPorts22());
                        OpenPortsTabUi.this.yes.setText(MSGS.yesButton());
                        OpenPortsTabUi.this.no.setText(MSGS.noButton());
                        OpenPortsTabUi.this.no.addClickHandler(new ClickHandler() {

                            @Override
                            public void onClick(ClickEvent event) {
                                OpenPortsTabUi.this.alert.hide();
                            }
                        });
                        OpenPortsTabUi.this.yes.addClickHandler(new ClickHandler() {

                            @Override
                            public void onClick(ClickEvent event) {
                                showModal(OpenPortsTabUi.this.selectionModel.getSelectedObject());
                                OpenPortsTabUi.this.alert.hide();
                            }
                        });
                        OpenPortsTabUi.this.alert.show();

                    } else if (selection.getPortRange().equals("80")) {
                        // show warning
                        OpenPortsTabUi.this.alertBody.setText(MSGS.firewallOpenPorts80());
                        OpenPortsTabUi.this.yes.setText(MSGS.yesButton());
                        OpenPortsTabUi.this.no.setText(MSGS.noButton());
                        OpenPortsTabUi.this.no.addClickHandler(new ClickHandler() {

                            @Override
                            public void onClick(ClickEvent event) {
                                OpenPortsTabUi.this.alert.hide();
                            }
                        });
                        OpenPortsTabUi.this.yes.addClickHandler(new ClickHandler() {

                            @Override
                            public void onClick(ClickEvent event) {
                                showModal(OpenPortsTabUi.this.selectionModel.getSelectedObject());
                                OpenPortsTabUi.this.alert.hide();
                            }
                        });
                        OpenPortsTabUi.this.alert.show();

                    } else {
                        showModal(selection);
                    }
                }
            }
        });
        this.openPortsForm.addHideHandler(new ModalHideHandler() {

            @Override
            public void onHide(ModalHideEvent evt) {

                if (OpenPortsTabUi.this.editOpenPortEntry != null) {
                    GwtFirewallOpenPortEntry oldEntry = OpenPortsTabUi.this.selectionModel.getSelectedObject();

                    OpenPortsTabUi.this.openPortsDataProvider.getList()
                            .remove(OpenPortsTabUi.this.selectionModel.getSelectedObject());
                    refreshTable();
                    if (!duplicateEntry(OpenPortsTabUi.this.editOpenPortEntry)) {
                        OpenPortsTabUi.this.openPortsDataProvider.getList().add(OpenPortsTabUi.this.editOpenPortEntry);
                        OpenPortsTabUi.this.openPortsDataProvider.flush();
                        OpenPortsTabUi.this.apply.setEnabled(true);
                        OpenPortsTabUi.this.editOpenPortEntry = null;
                        setVisibility();
                    } else {	// end duplicate
                        OpenPortsTabUi.this.openPortsDataProvider.getList().add(oldEntry);
                        OpenPortsTabUi.this.openPortsDataProvider.flush();
                    }
                    refreshTable();
                }  // end !=null
            }// end onHide
        });
    }

    private void initDeleteButton() {
        this.delete.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                GwtFirewallOpenPortEntry selection = OpenPortsTabUi.this.selectionModel.getSelectedObject();
                if (selection != null) {
                    OpenPortsTabUi.this.alert.setTitle(MSGS.confirm());
                    OpenPortsTabUi.this.alertBody
                            .setText(MSGS.firewallOpenPortDeleteConfirmation(String.valueOf(selection.getPortRange())));
                    OpenPortsTabUi.this.alert.show();
                }
            }
        });
        this.yes.setText(MSGS.yesButton());
        this.no.setText(MSGS.noButton());
        this.no.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                OpenPortsTabUi.this.alert.hide();
            }
        });
        this.yes.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                OpenPortsTabUi.this.alert.hide();
                OpenPortsTabUi.this.openPortsDataProvider.getList()
                        .remove(OpenPortsTabUi.this.selectionModel.getSelectedObject());
                refreshTable();
                OpenPortsTabUi.this.apply.setEnabled(true);
                setVisibility();

                setDirty(true);
            }
        });
    }

    private void initModal() {
        this.cancel.setText(MSGS.cancelButton());
        this.cancel.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                OpenPortsTabUi.this.openPortsForm.hide();
                OpenPortsTabUi.this.openPortEntry = null;
                OpenPortsTabUi.this.editOpenPortEntry = null;
                OpenPortsTabUi.this.newOpenPortEntry = null;
            }
        });

        this.submit.setText(MSGS.submitButton());
        this.submit.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                checkFieldsValues();

                if (OpenPortsTabUi.this.groupPort.getValidationState() == ValidationState.ERROR
                        || OpenPortsTabUi.this.groupPermittedNw.getValidationState() == ValidationState.ERROR
                        || OpenPortsTabUi.this.groupPermittedI.getValidationState() == ValidationState.ERROR
                        || OpenPortsTabUi.this.groupUnpermittedI.getValidationState() == ValidationState.ERROR
                        || OpenPortsTabUi.this.groupPermittedMac.getValidationState() == ValidationState.ERROR
                        || OpenPortsTabUi.this.groupSource.getValidationState() == ValidationState.ERROR) {
                    return;
                }

                // create a new entry
                OpenPortsTabUi.this.openPortEntry = new GwtFirewallOpenPortEntry();
                OpenPortsTabUi.this.openPortEntry.setPortRange(OpenPortsTabUi.this.port.getText());
                OpenPortsTabUi.this.openPortEntry.setProtocol(OpenPortsTabUi.this.protocol.getSelectedItemText());
                if (OpenPortsTabUi.this.permittedNw.getText() != null
                        && !"".equals(OpenPortsTabUi.this.permittedNw.getText().trim())) {
                    OpenPortsTabUi.this.openPortEntry.setPermittedNetwork(OpenPortsTabUi.this.permittedNw.getText());
                } else {
                    OpenPortsTabUi.this.openPortEntry.setPermittedNetwork("0.0.0.0/0");
                }
                if (OpenPortsTabUi.this.permittedI.getText() != null
                        && !"".equals(OpenPortsTabUi.this.permittedI.getText().trim())) {
                    OpenPortsTabUi.this.openPortEntry
                            .setPermittedInterfaceName(OpenPortsTabUi.this.permittedI.getText());
                }
                if (OpenPortsTabUi.this.unpermittedI.getText() != null
                        && !"".equals(OpenPortsTabUi.this.unpermittedI.getText().trim())) {
                    OpenPortsTabUi.this.openPortEntry
                            .setUnpermittedInterfaceName(OpenPortsTabUi.this.unpermittedI.getText());
                }
                if (OpenPortsTabUi.this.permittedMac.getText() != null
                        && !"".equals(OpenPortsTabUi.this.permittedMac.getText().trim())) {
                    OpenPortsTabUi.this.openPortEntry.setPermittedMAC(OpenPortsTabUi.this.permittedMac.getText());
                }
                if (OpenPortsTabUi.this.source.getText() != null
                        && !"".equals(OpenPortsTabUi.this.source.getText().trim())) {
                    OpenPortsTabUi.this.openPortEntry.setSourcePortRange(OpenPortsTabUi.this.source.getText());
                }

                if (OpenPortsTabUi.this.submit.getId().equals("new")) {
                    OpenPortsTabUi.this.newOpenPortEntry = OpenPortsTabUi.this.openPortEntry;
                    OpenPortsTabUi.this.editOpenPortEntry = null;
                } else if (OpenPortsTabUi.this.submit.getId().equals("edit")) {
                    OpenPortsTabUi.this.editOpenPortEntry = OpenPortsTabUi.this.openPortEntry;
                    OpenPortsTabUi.this.newOpenPortEntry = null;
                }

                setDirty(true);

                OpenPortsTabUi.this.openPortsForm.hide();
            }
        });
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
        this.permittedI.addChangeHandler(new ChangeHandler() {

            @Override
            public void onChange(ChangeEvent event) {
                if (!OpenPortsTabUi.this.permittedI.getText().trim().isEmpty()) {
                    OpenPortsTabUi.this.unpermittedI.clear();
                    OpenPortsTabUi.this.unpermittedI.setEnabled(false);
                } else {
                    OpenPortsTabUi.this.unpermittedI.setEnabled(true);
                }
            }
        });

        this.unpermittedI.addChangeHandler(new ChangeHandler() {

            @Override
            public void onChange(ChangeEvent event) {
                if (!OpenPortsTabUi.this.unpermittedI.getText().trim().isEmpty()) {
                    OpenPortsTabUi.this.permittedI.clear();
                    OpenPortsTabUi.this.permittedI.setEnabled(false);
                } else {
                    OpenPortsTabUi.this.permittedI.setEnabled(true);
                }
            }
        });

        // set up validation
        // groupPort, groupPermittedNw,groupPermittedI,grourUnpermittedI,groupPermittedMac,groupSource;
        this.port.addBlurHandler(new BlurHandler() {

            @Override
            public void onBlur(BlurEvent event) {
                if (!OpenPortsTabUi.this.port.getText().trim().matches(FieldType.PORT_RANGE.getRegex())
                        && OpenPortsTabUi.this.port.getText().trim().length() != 0
                        || OpenPortsTabUi.this.port.getText() == null
                        || "".equals(OpenPortsTabUi.this.port.getText().trim())) {
                    OpenPortsTabUi.this.groupPort.setValidationState(ValidationState.ERROR);
                } else {
                    OpenPortsTabUi.this.groupPort.setValidationState(ValidationState.NONE);
                }
            }
        });

        this.permittedNw.addBlurHandler(new BlurHandler() {

            @Override
            public void onBlur(BlurEvent event) {
                if (!OpenPortsTabUi.this.permittedNw.getText().trim().matches(FieldType.NETWORK.getRegex())
                        && OpenPortsTabUi.this.permittedNw.getText().trim().length() > 0) {
                    OpenPortsTabUi.this.groupPermittedNw.setValidationState(ValidationState.ERROR);
                } else {
                    OpenPortsTabUi.this.groupPermittedNw.setValidationState(ValidationState.NONE);
                }
            }
        });
        this.permittedI.addBlurHandler(new BlurHandler() {

            @Override
            public void onBlur(BlurEvent event) {
                if (!OpenPortsTabUi.this.permittedI.getText().trim().matches(FieldType.ALPHANUMERIC.getRegex())
                        && OpenPortsTabUi.this.permittedI.getText().trim().length() > 0) {
                    OpenPortsTabUi.this.groupPermittedI.setValidationState(ValidationState.ERROR);
                } else {
                    OpenPortsTabUi.this.groupPermittedI.setValidationState(ValidationState.NONE);
                }
            }
        });
        this.unpermittedI.addBlurHandler(new BlurHandler() {

            @Override
            public void onBlur(BlurEvent event) {
                if (!OpenPortsTabUi.this.unpermittedI.getText().trim().matches(FieldType.ALPHANUMERIC.getRegex())
                        && OpenPortsTabUi.this.unpermittedI.getText().trim().length() > 0) {
                    OpenPortsTabUi.this.groupUnpermittedI.setValidationState(ValidationState.ERROR);
                } else {
                    OpenPortsTabUi.this.groupPermittedI.setValidationState(ValidationState.NONE);
                }
            }
        });
        this.permittedMac.addBlurHandler(new BlurHandler() {

            @Override
            public void onBlur(BlurEvent event) {
                if (!OpenPortsTabUi.this.permittedMac.getText().trim().matches(FieldType.MAC_ADDRESS.getRegex())
                        && OpenPortsTabUi.this.permittedMac.getText().trim().length() > 0) {
                    OpenPortsTabUi.this.groupPermittedMac.setValidationState(ValidationState.ERROR);
                } else {
                    OpenPortsTabUi.this.groupPermittedMac.setValidationState(ValidationState.NONE);
                }
            }
        });
        this.source.addBlurHandler(new BlurHandler() {

            @Override
            public void onBlur(BlurEvent event) {
                if (!OpenPortsTabUi.this.source.getText().trim().matches(FieldType.PORT_RANGE.getRegex())
                        && OpenPortsTabUi.this.source.getText().trim().length() > 0) {
                    OpenPortsTabUi.this.groupSource.setValidationState(ValidationState.ERROR);
                } else {
                    OpenPortsTabUi.this.groupSource.setValidationState(ValidationState.NONE);
                }
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
                if (entry.getPortRange().equals(openPortEntry.getPortRange())
                        && entry.getProtocol().equals(openPortEntry.getProtocol())
                        && entry.getPermittedNetwork().equals(openPortEntry.getPermittedNetwork())
                        && entry.getPermittedInterfaceName().equals(openPortEntry.getPermittedInterfaceName())
                        && entry.getUnpermittedInterfaceName().equals(openPortEntry.getUnpermittedInterfaceName())
                        && entry.getPermittedMAC().equals(openPortEntry.getPermittedMAC())
                        && entry.getSourcePortRange().equals(openPortEntry.getSourcePortRange())) {
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
}

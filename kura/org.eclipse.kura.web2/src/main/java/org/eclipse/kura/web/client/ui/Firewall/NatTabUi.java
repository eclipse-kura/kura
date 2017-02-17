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
import org.eclipse.kura.web.shared.model.GwtFirewallNatEntry;
import org.eclipse.kura.web.shared.model.GwtFirewallNatMasquerade;
import org.eclipse.kura.web.shared.model.GwtFirewallNatProtocol;
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
import org.gwtbootstrap3.client.ui.ModalBody;
import org.gwtbootstrap3.client.ui.ModalFooter;
import org.gwtbootstrap3.client.ui.TextBox;
import org.gwtbootstrap3.client.ui.Tooltip;
import org.gwtbootstrap3.client.ui.constants.ValidationState;
import org.gwtbootstrap3.client.ui.gwt.CellTable;
import org.gwtbootstrap3.client.ui.html.Span;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
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

public class NatTabUi extends Composite implements Tab {

    private static NatTabUiUiBinder uiBinder = GWT.create(NatTabUiUiBinder.class);

    interface NatTabUiUiBinder extends UiBinder<Widget, NatTabUi> {
    }

    private static final Messages MSGS = GWT.create(Messages.class);

    private final GwtSecurityTokenServiceAsync gwtXSRFService = GWT.create(GwtSecurityTokenService.class);
    private final GwtNetworkServiceAsync gwtNetworkService = GWT.create(GwtNetworkService.class);

    private final ListDataProvider<GwtFirewallNatEntry> natDataProvider = new ListDataProvider<GwtFirewallNatEntry>();
    final SingleSelectionModel<GwtFirewallNatEntry> selectionModel = new SingleSelectionModel<GwtFirewallNatEntry>();

    private boolean m_dirty;

    private GwtFirewallNatEntry newNatEntry;
    private GwtFirewallNatEntry editNatEntry;

    @UiField
    Button apply, create, edit, delete;
    @UiField
    Alert notification;
    @UiField
    CellTable<GwtFirewallNatEntry> natGrid = new CellTable<GwtFirewallNatEntry>();

    @UiField
    Modal natForm;
    @UiField
    FormGroup groupInput, groupOutput, groupProtocol, groupSource, groupDestination, groupEnable;
    @UiField
    FormLabel labelInput, labelOutput, labelProtocol, labelSource, labelDestination, labelEnable;
    @UiField
    Tooltip tooltipInput, tooltipOutput, tooltipProtocol, tooltipSource, tooltipDestination, tooltipEnable;
    @UiField
    TextBox input, output, source, destination;
    @UiField
    ListBox protocol, enable;
    @UiField
    Button submit, cancel;

    public NatTabUi() {
        initWidget(uiBinder.createAndBindUi(this));
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
        this.natDataProvider.getList().clear();
        this.gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

            @Override
            public void onFailure(Throwable ex) {
                EntryClassUi.hideWaitModal();
                FailureHandler.handle(ex);
            }

            @Override
            public void onSuccess(GwtXSRFToken token) {
                NatTabUi.this.gwtNetworkService.findDeviceFirewallNATs(token,
                        new AsyncCallback<List<GwtFirewallNatEntry>>() {

                    @Override
                    public void onFailure(Throwable caught) {
                        EntryClassUi.hideWaitModal();
                        FailureHandler.handle(caught);
                    }

                    @Override
                    public void onSuccess(List<GwtFirewallNatEntry> result) {
                        for (GwtFirewallNatEntry pair : result) {
                            NatTabUi.this.natDataProvider.getList().add(pair);
                        }
                        refreshTable();

                        NatTabUi.this.apply.setEnabled(false);
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
        // TODO Auto-generated method stub
        return false;
    }

    //
    // Private methods
    //
    private void initTable() {

        TextColumn<GwtFirewallNatEntry> col1 = new TextColumn<GwtFirewallNatEntry>() {

            @Override
            public String getValue(GwtFirewallNatEntry object) {
                if (object.getInInterface() != null) {
                    return String.valueOf(object.getInInterface());
                } else {
                    return "";
                }
            }
        };
        col1.setCellStyleNames("status-table-row");
        this.natGrid.addColumn(col1, MSGS.firewallNatInInterface());

        TextColumn<GwtFirewallNatEntry> col2 = new TextColumn<GwtFirewallNatEntry>() {

            @Override
            public String getValue(GwtFirewallNatEntry object) {
                if (object.getOutInterface() != null) {
                    return String.valueOf(object.getOutInterface());
                } else {
                    return "";
                }
            }
        };
        col2.setCellStyleNames("status-table-row");
        this.natGrid.addColumn(col2, MSGS.firewallNatOutInterface());

        TextColumn<GwtFirewallNatEntry> col3 = new TextColumn<GwtFirewallNatEntry>() {

            @Override
            public String getValue(GwtFirewallNatEntry object) {
                if (object.getProtocol() != null) {
                    return String.valueOf(object.getProtocol());
                } else {
                    return "";
                }
            }
        };
        col3.setCellStyleNames("status-table-row");
        this.natGrid.addColumn(col3, MSGS.firewallNatProtocol());

        TextColumn<GwtFirewallNatEntry> col4 = new TextColumn<GwtFirewallNatEntry>() {

            @Override
            public String getValue(GwtFirewallNatEntry object) {
                if (object.getSourceNetwork() != null) {
                    return String.valueOf(object.getSourceNetwork());
                } else {
                    return "";
                }
            }
        };
        col4.setCellStyleNames("status-table-row");
        this.natGrid.addColumn(col4, MSGS.firewallNatSourceNetwork());

        TextColumn<GwtFirewallNatEntry> col5 = new TextColumn<GwtFirewallNatEntry>() {

            @Override
            public String getValue(GwtFirewallNatEntry object) {
                if (object.getDestinationNetwork() != null) {
                    return String.valueOf(object.getDestinationNetwork());
                } else {
                    return "";
                }
            }
        };
        col5.setCellStyleNames("status-table-row");
        this.natGrid.addColumn(col5, MSGS.firewallNatDestinationNetwork());

        TextColumn<GwtFirewallNatEntry> col6 = new TextColumn<GwtFirewallNatEntry>() {

            @Override
            public String getValue(GwtFirewallNatEntry object) {
                if (object.getMasquerade() != null) {
                    return String.valueOf(object.getMasquerade());
                } else {
                    return "";
                }
            }
        };
        col6.setCellStyleNames("status-table-row");
        this.natGrid.addColumn(col6, MSGS.firewallNatMasquerade());

        this.natDataProvider.addDataDisplay(this.natGrid);
        this.natGrid.setSelectionModel(this.selectionModel);
    }

    private void refreshTable() {
        int size = this.natDataProvider.getList().size();
        this.natGrid.setVisibleRange(0, size);
        this.natDataProvider.flush();

        if (this.natDataProvider.getList().isEmpty()) {
            this.natGrid.setVisible(false);
            this.notification.setVisible(true);
            this.notification.setText(MSGS.firewallPortForwardTableNoPorts());
        } else {
            this.natGrid.setVisible(true);
            this.notification.setVisible(false);
        }
        this.natGrid.redraw();
    }

    // Initialize tab buttons
    private void initButtons() {
        initApplyButton();

        initCreateButton();

        initEditButton();

        initDeleteButton();
    }

    private void initDeleteButton() {
        this.delete.setText(MSGS.deleteButton());
        this.delete.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                final GwtFirewallNatEntry selection = NatTabUi.this.selectionModel.getSelectedObject();
                if (selection != null) { // TODO: this part should be structured the same as the other firewall tabs
                    final Modal confirm = new Modal();
                    ModalBody confirmBody = new ModalBody();
                    ModalFooter confirmFooter = new ModalFooter();

                    confirm.setTitle(MSGS.confirm());
                    confirmBody.add(new Span(MSGS.firewallNatDeleteConfirmation(selection.getInInterface())));
                    Button yes = new Button(MSGS.yesButton(), new ClickHandler() {

                        @Override
                        public void onClick(ClickEvent event) {
                            NatTabUi.this.natDataProvider.getList().remove(selection);
                            refreshTable();
                            NatTabUi.this.apply.setEnabled(true);
                            confirm.hide();
                            setDirty(true);
                        }
                    });

                    Button no = new Button(MSGS.noButton(), new ClickHandler() {

                        @Override
                        public void onClick(ClickEvent event) {
                            confirm.hide();
                        }
                    });
                    confirmFooter.add(no);
                    confirmFooter.add(yes);

                    confirm.add(confirmBody);
                    confirm.add(confirmFooter);
                    confirm.show();
                    no.setFocus(true);
                }
            }
        });
    }

    private void initEditButton() {
        this.edit.setText(MSGS.editButton());
        this.edit.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                GwtFirewallNatEntry selection = NatTabUi.this.selectionModel.getSelectedObject();
                if (selection != null) {
                    showModal(selection);
                }
            }
        });
        this.natForm.addHideHandler(new ModalHideHandler() {

            @Override
            public void onHide(ModalHideEvent evt) {
                if (NatTabUi.this.editNatEntry != null) {
                    GwtFirewallNatEntry oldEntry = NatTabUi.this.selectionModel.getSelectedObject();
                    NatTabUi.this.natDataProvider.getList().remove(oldEntry);
                    if (!duplicateEntry(NatTabUi.this.editNatEntry)) {
                        NatTabUi.this.natDataProvider.getList().add(NatTabUi.this.editNatEntry);
                        NatTabUi.this.natDataProvider.flush();
                        NatTabUi.this.apply.setEnabled(true);
                        NatTabUi.this.editNatEntry = null;
                    } else {	// end duplicate
                        NatTabUi.this.natDataProvider.getList().add(oldEntry);
                        NatTabUi.this.natDataProvider.flush();
                    }
                }
            }
        });
    }

    private void initCreateButton() {
        this.create.setText(MSGS.newButton());
        this.create.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                showModal(null);
            }
        });
        this.natForm.addHideHandler(new ModalHideHandler() {

            @Override
            public void onHide(ModalHideEvent evt) {
                if (NatTabUi.this.newNatEntry != null && !duplicateEntry(NatTabUi.this.newNatEntry)) {
                    NatTabUi.this.natDataProvider.getList().add(NatTabUi.this.newNatEntry);
                    refreshTable();
                    NatTabUi.this.apply.setEnabled(true);
                    NatTabUi.this.newNatEntry = null;
                }
            }
        });
    }

    private void initApplyButton() {
        this.apply.setText(MSGS.firewallApply());
        this.apply.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                List<GwtFirewallNatEntry> intermediateList = NatTabUi.this.natDataProvider.getList();
                ArrayList<GwtFirewallNatEntry> tempList = new ArrayList<GwtFirewallNatEntry>();
                final List<GwtFirewallNatEntry> updatedNatConf = tempList;
                for (GwtFirewallNatEntry entry : intermediateList) {
                    tempList.add(entry);
                }

                if (updatedNatConf != null) {
                    EntryClassUi.showWaitModal();
                    NatTabUi.this.gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

                        @Override
                        public void onFailure(Throwable ex) {
                            EntryClassUi.hideWaitModal();
                            FailureHandler.handle(ex);
                        }

                        @Override
                        public void onSuccess(GwtXSRFToken token) {
                            NatTabUi.this.gwtNetworkService.updateDeviceFirewallNATs(token, updatedNatConf,
                                    new AsyncCallback<Void>() {

                                @Override
                                public void onFailure(Throwable caught) {
                                    EntryClassUi.hideWaitModal();
                                    FailureHandler.handle(caught);
                                }

                                @Override
                                public void onSuccess(Void result) {
                                    setDirty(false);
                                    NatTabUi.this.apply.setEnabled(false);
                                    EntryClassUi.hideWaitModal();
                                }
                            });
                        }
                    });
                }
            }
        });
    }

    private void initModal() {
        // Handle Buttons
        this.cancel.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                NatTabUi.this.natForm.hide();
            }
        });

        this.submit.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                checkFieldsValues();

                if (NatTabUi.this.groupInput.getValidationState() == ValidationState.ERROR
                        || NatTabUi.this.groupOutput.getValidationState() == ValidationState.ERROR
                        || NatTabUi.this.groupSource.getValidationState() == ValidationState.ERROR
                        || NatTabUi.this.groupDestination.getValidationState() == ValidationState.ERROR) {
                    return;
                }
                // Fetch form data
                GwtFirewallNatEntry natEntry = new GwtFirewallNatEntry();
                natEntry.setInInterface(NatTabUi.this.input.getText());
                natEntry.setOutInterface(NatTabUi.this.output.getText());
                natEntry.setProtocol(NatTabUi.this.protocol.getSelectedItemText());
                natEntry.setSourceNetwork(NatTabUi.this.source.getText());
                natEntry.setDestinationNetwork(NatTabUi.this.destination.getText());
                natEntry.setMasquerade(NatTabUi.this.enable.getSelectedItemText());

                if (NatTabUi.this.submit.getId().equals("new")) {
                    NatTabUi.this.newNatEntry = natEntry;
                    NatTabUi.this.editNatEntry = null;
                } else if (NatTabUi.this.submit.getId().equals("edit")) {
                    NatTabUi.this.editNatEntry = natEntry;
                    NatTabUi.this.newNatEntry = null;
                }
                NatTabUi.this.natForm.hide();

                setDirty(true);
            }

        });
    }

    private void showModal(final GwtFirewallNatEntry existingEntry) {
        if (existingEntry == null) {
            this.natForm.setTitle(MSGS.firewallNatFormInformation());
        } else {
            this.natForm.setTitle(MSGS.firewallNatFormUpdate(existingEntry.getOutInterface()));
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

        this.natForm.show();
    }

    private void setModalFieldsHandlers() {
        // Set up validation
        this.input.addBlurHandler(new BlurHandler() {

            @Override
            public void onBlur(BlurEvent event) {
                if (!NatTabUi.this.input.getText().trim().matches(FieldType.ALPHANUMERIC.getRegex())
                        || NatTabUi.this.input.getText().trim().isEmpty()) {
                    NatTabUi.this.groupInput.setValidationState(ValidationState.ERROR);
                } else {
                    NatTabUi.this.groupInput.setValidationState(ValidationState.NONE);
                }
            }
        });
        this.output.addBlurHandler(new BlurHandler() {

            @Override
            public void onBlur(BlurEvent event) {
                if (!NatTabUi.this.output.getText().trim().matches(FieldType.ALPHANUMERIC.getRegex())
                        || NatTabUi.this.output.getText().trim().isEmpty()) {
                    NatTabUi.this.groupOutput.setValidationState(ValidationState.ERROR);
                } else {
                    NatTabUi.this.groupOutput.setValidationState(ValidationState.NONE);
                }
            }
        });
        this.source.addBlurHandler(new BlurHandler() {

            @Override
            public void onBlur(BlurEvent event) {
                if (!NatTabUi.this.source.getText().trim().isEmpty()
                        && !NatTabUi.this.source.getText().trim().matches(FieldType.NETWORK.getRegex())) {
                    NatTabUi.this.groupSource.setValidationState(ValidationState.ERROR);
                } else {
                    NatTabUi.this.groupSource.setValidationState(ValidationState.NONE);
                }
            }
        });
        this.destination.addBlurHandler(new BlurHandler() {

            @Override
            public void onBlur(BlurEvent event) {
                if (!NatTabUi.this.destination.getText().trim().isEmpty()
                        && !NatTabUi.this.destination.getText().trim().matches(FieldType.NETWORK.getRegex())) {
                    NatTabUi.this.groupDestination.setValidationState(ValidationState.ERROR);
                } else {
                    NatTabUi.this.groupDestination.setValidationState(ValidationState.NONE);
                }
            }
        });
    }

    private void setModalFieldsValues(final GwtFirewallNatEntry existingEntry) {
        // populate existing values
        if (existingEntry != null) {
            this.input.setText(existingEntry.getInInterface());
            this.output.setText(existingEntry.getOutInterface());
            this.source.setText(existingEntry.getSourceNetwork());
            this.destination.setText(existingEntry.getDestinationNetwork());
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
            this.input.setText("");
            this.output.setText("");
            this.source.setText("");
            this.destination.setText("");

            this.protocol.setSelectedIndex(0);
            this.enable.setSelectedIndex(0);
        }
    }

    private void setModalFieldsTooltips() {
        // set Tooltips
        this.tooltipInput.setTitle(MSGS.firewallNatFormInputInterfaceToolTip());
        this.tooltipOutput.setTitle(MSGS.firewallNatFormOutputInterfaceToolTip());
        this.tooltipProtocol.setTitle(MSGS.firewallNatFormProtocolToolTip());
        this.tooltipSource.setTitle(MSGS.firewallNatFormSourceNetworkToolTip());
        this.tooltipDestination.setTitle(MSGS.firewallNatFormDestinationNetworkToolTip());
        this.tooltipEnable.setTitle(MSGS.firewallNatFormMasqueradingToolTip());
        this.tooltipInput.reconfigure();
        this.tooltipOutput.reconfigure();
        this.tooltipProtocol.reconfigure();
        this.tooltipSource.reconfigure();
        this.tooltipDestination.reconfigure();
        this.tooltipEnable.reconfigure();
    }

    private void setModalFieldsLabels() {
        // set Labels
        this.labelInput.setText(MSGS.firewallNatFormInInterfaceName() + "*");
        this.labelOutput.setText(MSGS.firewallNatFormOutInterfaceName() + "*");
        this.labelProtocol.setText(MSGS.firewallNatFormProtocol());
        this.labelSource.setText(MSGS.firewallNatFormSourceNetwork());
        this.labelDestination.setText(MSGS.firewallNatFormDestinationNetwork());
        this.labelEnable.setText(MSGS.firewallNatFormMasquerade());
        this.submit.setText(MSGS.submitButton());
        this.cancel.setText(MSGS.cancelButton());

        // set ListBox
        this.protocol.clear();
        for (GwtFirewallNatProtocol prot : GwtFirewallNatProtocol.values()) {
            this.protocol.addItem(prot.name());
        }
        this.enable.clear();
        for (GwtFirewallNatMasquerade masquerade : GwtFirewallNatMasquerade.values()) {
            this.enable.addItem(masquerade.name());
        }
    }

    private boolean duplicateEntry(GwtFirewallNatEntry firewallNatEntry) {
        boolean isDuplicateEntry = false;
        List<GwtFirewallNatEntry> entries = this.natDataProvider.getList();
        if (entries != null && firewallNatEntry != null) {
            for (GwtFirewallNatEntry entry : entries) {
                String sourceNetwork = entry.getSourceNetwork() != null ? entry.getSourceNetwork() : "0.0.0.0/0";
                String destinationNetwork = entry.getDestinationNetwork() != null ? entry.getDestinationNetwork()
                        : "0.0.0.0/0";
                String newSourceNetwork = firewallNatEntry.getSourceNetwork() != null
                        ? firewallNatEntry.getSourceNetwork() : "0.0.0.0/0";
                String newDestinationNetwork = firewallNatEntry.getDestinationNetwork() != null
                        ? firewallNatEntry.getDestinationNetwork() : "0.0.0.0/0";

                if (entry.getInInterface().equals(firewallNatEntry.getInInterface())
                        && entry.getOutInterface().equals(firewallNatEntry.getOutInterface())
                        && entry.getProtocol().equals(firewallNatEntry.getProtocol())
                        && sourceNetwork.equals(newSourceNetwork) && destinationNetwork.equals(newDestinationNetwork)) {
                    isDuplicateEntry = true;
                    break;
                }
            }
        }

        return isDuplicateEntry;
    }

    private void checkFieldsValues() {
        // make the required fields in error state by default
        if (this.input.getText() == null || "".equals(this.input.getText().trim())) {
            this.groupInput.setValidationState(ValidationState.ERROR);
        }
        if (this.output.getText() == null || "".equals(this.output.getText().trim())) {
            this.groupOutput.setValidationState(ValidationState.ERROR);
        }
    }
}

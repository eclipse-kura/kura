/*******************************************************************************
 * Copyright (c) 2016 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.web.client.ui.CloudServices;

import java.util.List;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.ui.EntryClassUi;
import org.eclipse.kura.web.client.util.FailureHandler;
import org.eclipse.kura.web.shared.model.GwtCloudConnectionEntry;
import org.eclipse.kura.web.shared.model.GwtGroupedNVPair;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.service.GwtCloudService;
import org.eclipse.kura.web.shared.service.GwtCloudServiceAsync;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenService;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenServiceAsync;
import org.eclipse.kura.web.shared.service.GwtStatusService;
import org.eclipse.kura.web.shared.service.GwtStatusServiceAsync;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.ListBox;
import org.gwtbootstrap3.client.ui.Modal;
import org.gwtbootstrap3.client.ui.ModalBody;
import org.gwtbootstrap3.client.ui.ModalFooter;
import org.gwtbootstrap3.client.ui.ModalHeader;
import org.gwtbootstrap3.client.ui.TextBox;
import org.gwtbootstrap3.client.ui.Well;
import org.gwtbootstrap3.client.ui.form.validator.RegExValidator;
import org.gwtbootstrap3.client.ui.gwt.CellTable;
import org.gwtbootstrap3.client.ui.html.Span;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;

public class CloudInstancesUi extends Composite {

    private static CloudConnectionsUiUiBinder uiBinder = GWT.create(CloudConnectionsUiUiBinder.class);
    private static final Messages MSG = GWT.create(Messages.class);

    private final SingleSelectionModel<GwtCloudConnectionEntry> selectionModel = new SingleSelectionModel<GwtCloudConnectionEntry>();
    private final ListDataProvider<GwtCloudConnectionEntry> cloudServicesDataProvider = new ListDataProvider<GwtCloudConnectionEntry>();
    private final GwtSecurityTokenServiceAsync gwtXSRFService = GWT.create(GwtSecurityTokenService.class);
    private final GwtCloudServiceAsync gwtCloudService = GWT.create(GwtCloudService.class);
    private final GwtStatusServiceAsync gwtStatusService = GWT.create(GwtStatusService.class);

    private final CloudServicesUi cloudServicesUi;

    private RegExValidator regexValidator;

    interface CloudConnectionsUiUiBinder extends UiBinder<Widget, CloudInstancesUi> {
    }

    @UiField
    Well connectionsWell;
    @UiField
    Button connectionRefresh;
    @UiField
    Button newConnection;
    @UiField
    Button deleteConnection;
    @UiField
    Button statusConnect;
    @UiField
    Button statusDisconnect;
    @UiField
    Button btnCreateComp;
    @UiField
    Modal newConnectionModal;
    @UiField
    ListBox cloudFactoriesPids;
    @UiField
    FlowPanel cloudServiceFlowPanel;
    @UiField
    CellTable<GwtCloudConnectionEntry> connectionsGrid = new CellTable<GwtCloudConnectionEntry>();

    TextBox cloudServicePid;

    public CloudInstancesUi(final CloudServicesUi cloudServicesUi) {
        initWidget(uiBinder.createAndBindUi(this));
        this.cloudServicesUi = cloudServicesUi;

        // Set text for buttons
        this.connectionRefresh.setText(MSG.refresh());
        this.newConnection.setText(MSG.newButton());
        this.deleteConnection.setText(MSG.deleteButton());
        this.statusConnect.setText(MSG.connectButton());
        this.statusDisconnect.setText(MSG.disconnectButton());
        this.connectionsGrid.setSelectionModel(this.selectionModel);
        this.cloudServicePid = new TextBox();

        this.btnCreateComp.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                if (CloudInstancesUi.this.cloudServicePid.validate()
                        && !CloudInstancesUi.this.cloudServicePid.getText().trim().isEmpty()) {
                    createComponent();
                }
            }
        });

        this.selectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {

            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                cloudServicesUi.onSelectionChange();
            }
        });

        this.cloudFactoriesPids.addChangeHandler(new ChangeHandler() {

            @Override
            public void onChange(ChangeEvent event) {
                final String factoryPid = CloudInstancesUi.this.cloudFactoriesPids.getSelectedValue();
                getSuggestedCloudServicePid(factoryPid);
            }
        });

        initConnectionButtons();

        initConnectionsTable();
    }

    public void loadData() {
        this.cloudServicesDataProvider.getList().clear();

        this.gwtCloudService.getCloudServices(new AsyncCallback<List<GwtCloudConnectionEntry>>() {

            @Override
            public void onFailure(Throwable caught) {
                FailureHandler.handle(caught, CloudInstancesUi.this.gwtCloudService.getClass().getSimpleName());
            }

            @Override
            public void onSuccess(List<GwtCloudConnectionEntry> result) {
                for (GwtCloudConnectionEntry pair : result) {
                    CloudInstancesUi.this.cloudServicesDataProvider.getList().add(pair);
                }
                CloudInstancesUi.this.cloudServicesUi.refreshInternal();
            }
        });
    }

    public int getTableSize() {
        return this.cloudServicesDataProvider.getList().size();
    }

    public void setVisibility(boolean isVisible) {
        this.connectionsGrid.setVisible(isVisible);
    }

    public GwtCloudConnectionEntry getSelectedObject() {
        return this.selectionModel.getSelectedObject();
    }

    public void setSelected(GwtCloudConnectionEntry cloudEntry) {
        this.selectionModel.setSelected(cloudEntry, true);
    }

    private void initConnectionButtons() {
        this.connectionRefresh.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                CloudInstancesUi.this.cloudServicesUi.refresh();
            }
        });

        this.newConnection.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                showNewConnectionModal();
            }
        });

        this.deleteConnection.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                if (getTableSize() > 0) {
                    showDeleteModal();
                }
            }
        });

        this.statusConnect.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                GwtCloudConnectionEntry selection = CloudInstancesUi.this.selectionModel.getSelectedObject();
                final String selectedCloudServicePid = selection.getCloudServicePid();
                connectDataService(selectedCloudServicePid);
            }
        });

        this.statusDisconnect.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                GwtCloudConnectionEntry selection = CloudInstancesUi.this.selectionModel.getSelectedObject();
                final String selectedCloudServicePid = selection.getCloudServicePid();
                disconnectDataService(selectedCloudServicePid);
            }
        });
    }

    private void initConnectionsTable() {

        TextColumn<GwtCloudConnectionEntry> col1 = new TextColumn<GwtCloudConnectionEntry>() {

            @Override
            public String getValue(GwtCloudConnectionEntry object) {
                if (object.isConnected()) {
                    return MSG.connected();
                } else {
                    return MSG.disconnected();
                }
            }
        };
        col1.setCellStyleNames("status-table-row");
        this.connectionsGrid.addColumn(col1, MSG.netIPv4Status());

        TextColumn<GwtCloudConnectionEntry> col2 = new TextColumn<GwtCloudConnectionEntry>() {

            @Override
            public String getValue(GwtCloudConnectionEntry object) {
                if (object.getCloudFactoryPid() != null) {
                    return String.valueOf(object.getCloudFactoryPid());
                } else {
                    return "";
                }
            }
        };
        col2.setCellStyleNames("status-table-row");
        this.connectionsGrid.addColumn(col2, MSG.connectionCloudFactoryLabel());

        TextColumn<GwtCloudConnectionEntry> col3 = new TextColumn<GwtCloudConnectionEntry>() {

            @Override
            public String getValue(GwtCloudConnectionEntry object) {
                if (object.getCloudServicePid() != null) {
                    return String.valueOf(object.getCloudServicePid());
                } else {
                    return "";
                }
            }
        };
        col3.setCellStyleNames("status-table-row");
        this.connectionsGrid.addColumn(col3, MSG.connectionCloudServiceLabel());

        this.cloudServicesDataProvider.addDataDisplay(this.connectionsGrid);
    }

    private void createComponent() {
        final String factoryPid = this.cloudFactoriesPids.getSelectedValue();
        final String newCloudServicePid = this.cloudServicePid.getValue();

        this.gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

            @Override
            public void onFailure(Throwable ex) {
                EntryClassUi.hideWaitModal();
                FailureHandler.handle(ex);
            }

            @Override
            public void onSuccess(GwtXSRFToken token) {
                CloudInstancesUi.this.gwtCloudService.createCloudServiceFromFactory(token, factoryPid,
                        newCloudServicePid, new AsyncCallback<Void>() {

                    @Override
                    public void onFailure(Throwable caught) {
                        EntryClassUi.hideWaitModal();
                        FailureHandler.handle(caught, CloudInstancesUi.this.gwtCloudService.getClass().getSimpleName());
                        CloudInstancesUi.this.newConnectionModal.hide();
                    }

                    @Override
                    public void onSuccess(Void result) {
                        CloudInstancesUi.this.newConnectionModal.hide();
                        EntryClassUi.hideWaitModal();
                        CloudInstancesUi.this.cloudServicesUi.refresh(2000);
                    }
                });
            }

        });
    }

    public void refresh() {
        int size = this.cloudServicesDataProvider.getList().size();
        this.connectionsGrid.setVisibleRange(0, size);
        this.cloudServicesDataProvider.flush();

        if (size > 0) {
            GwtCloudConnectionEntry firstEntry = this.cloudServicesDataProvider.getList().get(0);
            this.selectionModel.setSelected(firstEntry, true);
        }
        this.connectionsGrid.redraw();
    }

    private void showNewConnectionModal() {
        EntryClassUi.showWaitModal();
        // cloudServicePid = new TextBox();
        this.cloudServicePid.clear();
        this.cloudFactoriesPids.clear();

        this.gwtCloudService.getCloudServiceFactories(new AsyncCallback<List<GwtGroupedNVPair>>() {

            @Override
            public void onFailure(Throwable caught) {
                EntryClassUi.hideWaitModal();
                FailureHandler.handle(caught, CloudInstancesUi.this.gwtCloudService.getClass().getSimpleName());
                CloudInstancesUi.this.newConnectionModal.hide();
            }

            @Override
            public void onSuccess(List<GwtGroupedNVPair> result) {
                for (GwtGroupedNVPair pair : result) {
                    CloudInstancesUi.this.cloudFactoriesPids.addItem(pair.getValue());
                }
                String selectedFactoryPid = CloudInstancesUi.this.cloudFactoriesPids.getSelectedValue();
                getSuggestedCloudServicePid(selectedFactoryPid);
                EntryClassUi.hideWaitModal();
            }
        });

        this.newConnectionModal.show();
    }

    private void connectDataService(final String connectionId) {
        EntryClassUi.showWaitModal();
        this.gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

            @Override
            public void onFailure(Throwable ex) {
                EntryClassUi.hideWaitModal();
                FailureHandler.handle(ex);
            }

            @Override
            public void onSuccess(GwtXSRFToken token) {
                CloudInstancesUi.this.gwtStatusService.connectDataService(token, connectionId,
                        new AsyncCallback<Void>() {

                    @Override
                    public void onSuccess(Void result) {
                        EntryClassUi.hideWaitModal();
                        CloudInstancesUi.this.cloudServicesUi.refresh(1000);
                    }

                    @Override
                    public void onFailure(Throwable caught) {
                        EntryClassUi.hideWaitModal();
                        FailureHandler.handle(caught);
                    }
                });
            }
        });
    }

    private void disconnectDataService(final String connectionId) {
        EntryClassUi.showWaitModal();
        this.gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

            @Override
            public void onFailure(Throwable ex) {
                EntryClassUi.hideWaitModal();
                FailureHandler.handle(ex);
            }

            @Override
            public void onSuccess(GwtXSRFToken token) {
                CloudInstancesUi.this.gwtStatusService.disconnectDataService(token, connectionId,
                        new AsyncCallback<Void>() {

                    @Override
                    public void onSuccess(Void result) {
                        EntryClassUi.hideWaitModal();
                        CloudInstancesUi.this.cloudServicesUi.refresh(1000);
                    }

                    @Override
                    public void onFailure(Throwable caught) {
                        EntryClassUi.hideWaitModal();
                        FailureHandler.handle(caught);
                    }
                });
            }
        });
    }

    private void deleteConnection(final String factoryPid, final String cloudServicePid) {
        EntryClassUi.showWaitModal();
        this.gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

            @Override
            public void onFailure(Throwable ex) {
                EntryClassUi.hideWaitModal();
                FailureHandler.handle(ex);
            }

            @Override
            public void onSuccess(GwtXSRFToken token) {
                CloudInstancesUi.this.gwtCloudService.deleteCloudServiceFromFactory(token, factoryPid, cloudServicePid,
                        new AsyncCallback<Void>() {

                    @Override
                    public void onSuccess(Void result) {
                        EntryClassUi.hideWaitModal();
                        CloudInstancesUi.this.cloudServicesUi.refresh(2000);
                    }

                    @Override
                    public void onFailure(Throwable caught) {
                        EntryClassUi.hideWaitModal();
                        FailureHandler.handle(caught);
                    }
                });
            }
        });
    }

    private void showDeleteModal() {
        final Modal modal = new Modal();

        ModalHeader header = new ModalHeader();
        header.setTitle(MSG.warning());
        modal.add(header);

        ModalBody body = new ModalBody();
        body.add(new Span(MSG.cloudServiceDeleteConfirmation()));
        modal.add(body);

        ModalFooter footer = new ModalFooter();
        footer.add(new Button(MSG.yesButton(), new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                GwtCloudConnectionEntry selection = CloudInstancesUi.this.selectionModel.getSelectedObject();
                final String selectedFactoryPid = selection.getCloudFactoryPid();
                final String selectedCloudServicePid = selection.getCloudServicePid();
                deleteConnection(selectedFactoryPid, selectedCloudServicePid);
                modal.hide();
            }
        }));
        footer.add(new Button(MSG.noButton(), new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                modal.hide();
            }
        }));
        modal.add(footer);
        modal.show();
    }

    private void getSuggestedCloudServicePid(final String factoryPid) {
        this.gwtCloudService.getSuggestedCloudServicePid(factoryPid, new AsyncCallback<String>() {

            @Override
            public void onFailure(Throwable caught) {
                FailureHandler.handle(caught, CloudInstancesUi.this.gwtCloudService.getClass().getSimpleName());
            }

            @Override
            public void onSuccess(String result) {
                CloudInstancesUi.this.cloudServiceFlowPanel.clear();

                CloudInstancesUi.this.cloudServicePid = new TextBox();
                CloudInstancesUi.this.cloudServicePid.setAutoComplete(false);
                if (result != null) {
                    CloudInstancesUi.this.cloudServicePid.setPlaceholder(MSG.exampleGiven() + " " + result);
                }
                getCloudServicePidRegex(factoryPid);

                CloudInstancesUi.this.cloudServiceFlowPanel.add(CloudInstancesUi.this.cloudServicePid);
            }
        });
    }

    private void getCloudServicePidRegex(final String factoryPid) {
        this.gwtCloudService.getCloudServicePidRegex(factoryPid, new AsyncCallback<String>() {

            @Override
            public void onFailure(Throwable caught) {
                FailureHandler.handle(caught, CloudInstancesUi.this.gwtCloudService.getClass().getSimpleName());
            }

            @Override
            public void onSuccess(String result) {
                if (result != null) {
                    CloudInstancesUi.this.regexValidator = new RegExValidator(result);
                } else {
                    CloudInstancesUi.this.regexValidator = new RegExValidator(".+");
                }
                CloudInstancesUi.this.cloudServicePid.addValidator(CloudInstancesUi.this.regexValidator);
                CloudInstancesUi.this.cloudServicePid.addKeyUpHandler(new KeyUpHandler() {

                    @Override
                    public void onKeyUp(KeyUpEvent event) {
                        CloudInstancesUi.this.cloudServicePid.validate();
                    }
                });
                CloudInstancesUi.this.cloudServicePid.addBlurHandler(new BlurHandler() {

                    @Override
                    public void onBlur(BlurEvent event) {
                        CloudInstancesUi.this.cloudServicePid.validate();
                    }
                });
            }
        });
    }
}

/*******************************************************************************
 * Copyright (c) 2016, 2018 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *     Red Hat Inc
 *******************************************************************************/
package org.eclipse.kura.web.client.ui.CloudServices;

import java.util.List;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.util.FailureHandler;
import org.eclipse.kura.web.client.util.PidTextBox;
import org.eclipse.kura.web.client.util.request.Request;
import org.eclipse.kura.web.client.util.request.RequestContext;
import org.eclipse.kura.web.client.util.request.RequestQueue;
import org.eclipse.kura.web.client.util.request.SuccessCallback;
import org.eclipse.kura.web.shared.model.GwtCloudConnectionEntry;
import org.eclipse.kura.web.shared.model.GwtCloudConnectionState;
import org.eclipse.kura.web.shared.model.GwtGroupedNVPair;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.service.GwtCloudService;
import org.eclipse.kura.web.shared.service.GwtCloudServiceAsync;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenService;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenServiceAsync;
import org.eclipse.kura.web.shared.service.GwtStatusService;
import org.eclipse.kura.web.shared.service.GwtStatusServiceAsync;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.Icon;
import org.gwtbootstrap3.client.ui.ListBox;
import org.gwtbootstrap3.client.ui.Modal;
import org.gwtbootstrap3.client.ui.ModalBody;
import org.gwtbootstrap3.client.ui.ModalFooter;
import org.gwtbootstrap3.client.ui.ModalHeader;
import org.gwtbootstrap3.client.ui.Well;
import org.gwtbootstrap3.client.ui.form.validator.RegExValidator;
import org.gwtbootstrap3.client.ui.gwt.CellTable;
import org.gwtbootstrap3.client.ui.html.Span;

import com.google.gwt.core.client.GWT;
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
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;

public class CloudInstancesUi extends Composite {

    private static CloudConnectionsUiUiBinder uiBinder = GWT.create(CloudConnectionsUiUiBinder.class);
    private static final Messages MSGS = GWT.create(Messages.class);

    private final SingleSelectionModel<GwtCloudConnectionEntry> selectionModel = new SingleSelectionModel<>();
    private final ListDataProvider<GwtCloudConnectionEntry> cloudServicesDataProvider = new ListDataProvider<>();
    private final GwtSecurityTokenServiceAsync gwtXSRFService = GWT.create(GwtSecurityTokenService.class);
    private final GwtCloudServiceAsync gwtCloudService = GWT.create(GwtCloudService.class);
    private final GwtStatusServiceAsync gwtStatusService = GWT.create(GwtStatusService.class);

    private final CloudServicesUi cloudServicesUi;

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
    Button btnCancel;
    @UiField
    Modal newConnectionModal;
    @UiField
    ListBox cloudFactoriesPids;
    @UiField
    PidTextBox cloudServicePid;
    @UiField
    Icon cloudServicePidSpinner;

    @UiField
    CellTable<GwtCloudConnectionEntry> connectionsGrid = new CellTable<>();

    public CloudInstancesUi(final CloudServicesUi cloudServicesUi) {
        initWidget(uiBinder.createAndBindUi(this));
        this.cloudServicesUi = cloudServicesUi;

        this.connectionsGrid.setSelectionModel(this.selectionModel);

        this.btnCreateComp.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                createComponent();
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

    public void setData(final List<GwtCloudConnectionEntry> cloudConnections) {
        this.cloudServicesDataProvider.getList().clear();
        for (GwtCloudConnectionEntry pair : cloudConnections) {
            CloudInstancesUi.this.cloudServicesDataProvider.getList().add(pair);
        }
        refresh();
    }

    public boolean setStatus(final String pid, final GwtCloudConnectionState state) {
        final List<GwtCloudConnectionEntry> entries = cloudServicesDataProvider.getList();

        for (final GwtCloudConnectionEntry entry : entries) {
            if (pid.equals(entry.getCloudServicePid())) {
                entry.setState(state);
                connectionsGrid.redraw();
                return true;
            }
        }

        return false;
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

        {
            TextColumn<GwtCloudConnectionEntry> col = new TextColumn<GwtCloudConnectionEntry>() {

                @Override
                public String getValue(GwtCloudConnectionEntry object) {
                    switch (object.getState()) {
                    case UNREGISTERED:
                        return MSGS.unregistered();
                    case CONNECTED:
                        return MSGS.connected();
                    case DISCONNECTED:
                        return MSGS.disconnected();
                    default:
                        return object.getState().toString();
                    }
                }
            };
            col.setCellStyleNames("status-table-row");
            this.connectionsGrid.addColumn(col, MSGS.netIPv4Status());
        }

        {
            TextColumn<GwtCloudConnectionEntry> col = new TextColumn<GwtCloudConnectionEntry>() {

                @Override
                public String getValue(GwtCloudConnectionEntry object) {
                    if (object.getCloudFactoryPid() != null) {
                        return String.valueOf(object.getCloudFactoryPid());
                    } else {
                        return "";
                    }
                }
            };
            col.setCellStyleNames("status-table-row");
            this.connectionsGrid.addColumn(col, MSGS.connectionCloudFactoryLabel());
        }

        {
            TextColumn<GwtCloudConnectionEntry> col = new TextColumn<GwtCloudConnectionEntry>() {

                @Override
                public String getValue(GwtCloudConnectionEntry object) {
                    if (object.getCloudServicePid() != null) {
                        return String.valueOf(object.getCloudServicePid());
                    } else {
                        return "";
                    }
                }
            };
            col.setCellStyleNames("status-table-row");
            this.connectionsGrid.addColumn(col, MSGS.connectionCloudServiceLabel());
        }

        this.cloudServicesDataProvider.addDataDisplay(this.connectionsGrid);
    }

    private void createComponent() {
        final String factoryPid = this.cloudFactoriesPids.getSelectedValue();
        final String newCloudServicePid = this.cloudServicePid.getPid();

        if (newCloudServicePid == null) {
            return;
        }

        RequestQueue.submit(new Request() {

            @Override
            public void run(final RequestContext context) {
                gwtXSRFService.generateSecurityToken(context.callback(new SuccessCallback<GwtXSRFToken>() {

                    @Override
                    public void onSuccess(GwtXSRFToken token) {
                        CloudInstancesUi.this.gwtCloudService.createCloudServiceFromFactory(token, factoryPid,
                                newCloudServicePid, context.callback(new AsyncCallback<Void>() {

                                    @Override
                                    public void onFailure(Throwable caught) {
                                        FailureHandler.handle(caught,
                                                CloudInstancesUi.this.gwtCloudService.getClass().getSimpleName());
                                        CloudInstancesUi.this.newConnectionModal.hide();
                                    }

                                    @Override
                                    public void onSuccess(Void result) {
                                        CloudInstancesUi.this.newConnectionModal.hide();
                                        CloudInstancesUi.this.cloudServicesUi.refresh();
                                    }
                                }));
                    }

                }));
            }
        });
    }

    private void refresh() {
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
        this.cloudServicePid.clear();
        this.cloudFactoriesPids.clear();

        RequestQueue.submit(new Request() {

            @Override
            public void run(final RequestContext context) {
                gwtCloudService
                        .findCloudServiceFactories(context.callback(new SuccessCallback<List<GwtGroupedNVPair>>() {

                            @Override
                            public void onSuccess(List<GwtGroupedNVPair> result) {
                                for (GwtGroupedNVPair pair : result) {
                                    CloudInstancesUi.this.cloudFactoriesPids.addItem(pair.getValue());
                                }
                                String selectedFactoryPid = CloudInstancesUi.this.cloudFactoriesPids.getSelectedValue();
                                getSuggestedCloudServicePid(selectedFactoryPid);
                                newConnectionModal.show();
                            }
                        }));
            }
        });
    }

    private void connectDataService(final String connectionId) {

        RequestQueue.submit(new Request() {

            @Override
            public void run(final RequestContext context) {
                gwtXSRFService.generateSecurityToken(context.callback(new SuccessCallback<GwtXSRFToken>() {

                    @Override
                    public void onSuccess(GwtXSRFToken token) {
                        CloudInstancesUi.this.gwtStatusService.connectDataService(token, connectionId,
                                context.<Void> callback());
                    }
                }));
            }
        });
    }

    private void disconnectDataService(final String connectionId) {

        RequestQueue.submit(new Request() {

            @Override
            public void run(final RequestContext context) {
                gwtXSRFService.generateSecurityToken(context.callback(new SuccessCallback<GwtXSRFToken>() {

                    @Override
                    public void onSuccess(GwtXSRFToken token) {
                        CloudInstancesUi.this.gwtStatusService.disconnectDataService(token, connectionId,
                                context.<Void> callback());
                    }
                }));
            }
        });

    }

    private void deleteConnection(final String factoryPid, final String cloudServicePid) {
        RequestQueue.submit(new Request() {

            @Override
            public void run(final RequestContext context) {
                gwtXSRFService.generateSecurityToken(context.callback(new SuccessCallback<GwtXSRFToken>() {

                    @Override
                    public void onSuccess(GwtXSRFToken token) {
                        CloudInstancesUi.this.gwtCloudService.deleteCloudServiceFromFactory(token, factoryPid,
                                cloudServicePid, context.callback(new SuccessCallback<Void>() {

                                    @Override
                                    public void onSuccess(Void result) {
                                        CloudInstancesUi.this.cloudServicesUi.refresh();
                                    }
                                }));
                    }
                }));
            }
        });
    }

    private void showDeleteModal() {
        final Modal modal = new Modal();

        ModalHeader header = new ModalHeader();
        header.setTitle(MSGS.warning());
        modal.add(header);

        ModalBody body = new ModalBody();
        body.add(new Span(MSGS.cloudServiceDeleteConfirmation()));
        modal.add(body);

        ModalFooter footer = new ModalFooter();
        Button yes = new Button(MSGS.yesButton(), new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                GwtCloudConnectionEntry selection = CloudInstancesUi.this.selectionModel.getSelectedObject();
                final String selectedFactoryPid = selection.getCloudFactoryPid();
                final String selectedCloudServicePid = selection.getCloudServicePid();
                deleteConnection(selectedFactoryPid, selectedCloudServicePid);
                modal.hide();
            }
        });
        Button no = new Button(MSGS.noButton(), new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                modal.hide();
            }
        });
        footer.add(no);
        footer.add(yes);
        modal.add(footer);
        modal.show();
        no.setFocus(true);
    }

    private void getSuggestedCloudServicePid(final String factoryPid) {
        RequestQueue.submit(new Request() {

            @Override
            public void run(final RequestContext context) {
                cloudServicePid.clear();
                cloudServicePid.setEnabled(false);
                cloudServicePidSpinner.setVisible(true);
                gwtCloudService.findSuggestedCloudServicePid(factoryPid,
                        context.callback(new SuccessCallback<String>() {

                            @Override
                            public void onSuccess(String result) {
                                getCloudServicePidRegex(context, factoryPid, result);
                            }
                        }));
            }
        }, false);
    }

    private void getCloudServicePidRegex(final RequestContext context, final String factoryPid, final String example) {
        this.gwtCloudService.findCloudServicePidRegex(factoryPid, context.callback(new SuccessCallback<String>() {

            @SuppressWarnings("unchecked")
            @Override
            public void onSuccess(String result) {
                final PidTextBox pidTextBox = CloudInstancesUi.this.cloudServicePid;
                pidTextBox.reset();

                String placeholder = null;
                String validationMessage = null;

                if (example != null) {
                    placeholder = MSGS.exampleGiven(example);
                    validationMessage = MSGS.mustBeLike(example);
                }

                if (result != null) {
                    pidTextBox.setValidators(new RegExValidator(result, validationMessage));
                } else {
                    pidTextBox.setValidators();
                }
                pidTextBox.setPlaceholder(placeholder);
                pidTextBox.setEnabled(true);
                cloudServicePidSpinner.setVisible(false);
            }
        }));
    }
}
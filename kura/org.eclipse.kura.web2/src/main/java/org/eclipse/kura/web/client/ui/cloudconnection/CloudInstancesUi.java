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
package org.eclipse.kura.web.client.ui.cloudconnection;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.ui.AlertDialog;
import org.eclipse.kura.web.client.util.FailureHandler;
import org.eclipse.kura.web.client.util.PidTextBox;
import org.eclipse.kura.web.client.util.request.RequestContext;
import org.eclipse.kura.web.client.util.request.RequestQueue;
import org.eclipse.kura.web.shared.model.GwtCloudComponentFactories;
import org.eclipse.kura.web.shared.model.GwtCloudConnectionEntry;
import org.eclipse.kura.web.shared.model.GwtCloudConnectionEntry.GwtCloudConnectionType;
import org.eclipse.kura.web.shared.model.GwtCloudEntry;
import org.eclipse.kura.web.shared.model.GwtCloudPubSubEntry;
import org.eclipse.kura.web.shared.service.GwtCloudConnectionService;
import org.eclipse.kura.web.shared.service.GwtCloudConnectionServiceAsync;
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

import com.google.gwt.cell.client.Cell.Context;
import com.google.gwt.core.client.GWT;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SingleSelectionModel;

public class CloudInstancesUi extends Composite {

    private static CloudConnectionsUiUiBinder uiBinder = GWT.create(CloudConnectionsUiUiBinder.class);
    private static final Messages MSGS = GWT.create(Messages.class);

    private final SingleSelectionModel<GwtCloudEntry> selectionModel = new SingleSelectionModel<>();
    private final ListDataProvider<GwtCloudEntry> cloudServicesDataProvider = new ListDataProvider<>();
    private final GwtSecurityTokenServiceAsync gwtXSRFService = GWT.create(GwtSecurityTokenService.class);
    private final GwtCloudConnectionServiceAsync gwtCloudConnection = GWT.create(GwtCloudConnectionService.class);
    private final GwtStatusServiceAsync gwtStatusService = GWT.create(GwtStatusService.class);

    private final CloudConnectionsUi cloudServicesUi;

    interface CloudConnectionsUiUiBinder extends UiBinder<Widget, CloudInstancesUi> {
    }

    private static final Comparator<GwtCloudEntry> CLOUD_ENTRY_COMPARATOR = (o1, o2) -> o1.getPid()
            .compareTo(o2.getPid());

    @UiField
    Well connectionsWell;
    @UiField
    Button connectionRefresh;
    @UiField
    Button newConnection;
    @UiField
    Button newPubSub;
    @UiField
    Button deleteConnection;
    @UiField
    Button statusConnectDisconnect;
    @UiField
    Button btnCreateComp;
    @UiField
    Button btnCancel;
    @UiField
    Modal newConnectionModal;
    @UiField
    ListBox cloudFactoriesPids;
    @UiField
    PidTextBox cloudConnectionPid;
    @UiField
    Icon cloudConnectionPidSpinner;
    @UiField
    Modal newPubSubModal;
    @UiField
    ListBox pubSubFactoriesPids;
    @UiField
    PidTextBox pubSubPid;
    @UiField
    Icon pubSubPidSpinner;
    @UiField
    Button btnPubSubCreateComp;
    @UiField
    Button btnPubSubCancel;
    @UiField
    AlertDialog alertDialog;

    List<String> cloudConnectionFactoryPids;
    Map<String, List<GwtCloudEntry>> pubSubFactoryEntries;

    @UiField
    CellTable<GwtCloudEntry> connectionsGrid = new CellTable<>();
    private GwtCloudComponentFactories cloudComponentFactories;

    public CloudInstancesUi(final CloudConnectionsUi cloudServicesUi) {
        initWidget(uiBinder.createAndBindUi(this));
        this.cloudServicesUi = cloudServicesUi;

        this.connectionsGrid.setSelectionModel(this.selectionModel);

        this.btnCreateComp.addClickHandler(event -> createCloudConnectionServiceFactory());

        this.btnPubSubCreateComp.addClickHandler(event -> createPubSub());

        this.selectionModel.addSelectionChangeHandler(event -> {
            Object selected = getSelectedObject();
            boolean isEndpoint = false;
            boolean isConnection = false;

            if (selected instanceof GwtCloudConnectionEntry) {
                GwtCloudConnectionEntry cloudConnection = (GwtCloudConnectionEntry) selected;
                isEndpoint = true;
                isConnection = cloudConnection.getConnectionType() == GwtCloudConnectionType.CONNECTION;
            }

            this.newPubSub.setEnabled(isEndpoint);
            this.statusConnectDisconnect.setEnabled(isConnection);
            this.cloudServicesUi.onSelectionChange();
        });

        this.cloudFactoriesPids.addChangeHandler(event -> {
            final String factoryPid = this.cloudFactoriesPids.getSelectedValue();
            getSuggestedCloudConnectionPid(factoryPid);
        });
        
        this.pubSubFactoriesPids.addChangeHandler(event -> {
            final String factoryPid = this.pubSubFactoriesPids.getSelectedValue();
            getSuggestedCloudPubSubPid(factoryPid);
        });

        initConnectionButtons();

        initConnectionsTable();
    }

    public void setData(final List<GwtCloudEntry> data) {

        final List<GwtCloudEntry> cloudConnections = new ArrayList<>();
        final Map<String, List<GwtCloudEntry>> groupedPubSub = new HashMap<>();

        for (final GwtCloudEntry entry : data) {
            if (entry instanceof GwtCloudConnectionEntry) {
                cloudConnections.add(entry);
                groupedPubSub.put(entry.getPid(), new ArrayList<>());
                continue;
            }

            final GwtCloudPubSubEntry pubSub = (GwtCloudPubSubEntry) entry;

            List<GwtCloudEntry> entries = groupedPubSub.get(pubSub.getCloudConnectionPid());

            if (entries != null) {
                entries.add(pubSub);
            }
        }

        cloudConnections.sort(CLOUD_ENTRY_COMPARATOR);

        final List<GwtCloudEntry> providerList = this.cloudServicesDataProvider.getList();

        providerList.clear();
        for (final GwtCloudEntry entry : cloudConnections) {
            providerList.add(entry);

            final List<GwtCloudEntry> pubSubs = groupedPubSub.get(entry.getPid());

            if (pubSubs != null) {
                pubSubs.sort(CLOUD_ENTRY_COMPARATOR);
                providerList.addAll(pubSubs);
            }
        }

        refresh();
    }

    public void setFactoryInfo(final GwtCloudComponentFactories factories) {
        this.cloudComponentFactories = factories;
        this.cloudConnectionFactoryPids = factories.getCloudConnectionFactoryPids();

        this.pubSubFactoryEntries = new HashMap<>();

        for (final GwtCloudEntry pubSubEntry : factories.getPubSubFactories()) {
            final String factoryPid = pubSubEntry.getFactoryPid();

            this.pubSubFactoryEntries.computeIfAbsent(factoryPid, p -> new ArrayList<>()).add(pubSubEntry);
        }
    }

    public boolean setStatus(final String pid, final GwtCloudConnectionEntry.GwtCloudConnectionState state) {
        final List<GwtCloudEntry> entries = this.cloudServicesDataProvider.getList();

        for (final GwtCloudEntry entry : entries) {
            if (pid.equals(entry.getPid()) && entry instanceof GwtCloudConnectionEntry) {

                final GwtCloudConnectionEntry connEntry = (GwtCloudConnectionEntry) entry;

                connEntry.setState(state);
                this.connectionsGrid.redraw();
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

    public GwtCloudEntry getSelectedObject() {
        return this.selectionModel.getSelectedObject();
    }

    public GwtCloudEntry getObjectAfterSelection() {
        final List<GwtCloudEntry> entries = this.cloudServicesDataProvider.getList();

        final int index = entries.indexOf(getSelectedObject());

        if (index == -1) {
            return null;
        }

        final int nextIndex = index + 1;

        if (nextIndex >= entries.size()) {
            return null;
        }

        return entries.get(nextIndex);
    }

    public void setSelected(GwtCloudEntry cloudEntry) {
        this.selectionModel.setSelected(cloudEntry, true);
    }

    private void initConnectionButtons() {
        this.connectionRefresh.addClickHandler(event -> this.cloudServicesUi.refresh());

        this.newConnection.addClickHandler(event -> showNewConnectionModal());

        this.newPubSub.addClickHandler(event -> {
            final GwtCloudEntry entry = getSelectedObject();

            if (!(entry instanceof GwtCloudConnectionEntry)) {
                return;
            }

            showNewPubSubModal(((GwtCloudConnectionEntry) entry).getCloudConnectionFactoryPid());
        });

        this.deleteConnection.addClickHandler(event -> {

            if (getSelectedObject() instanceof GwtCloudConnectionEntry
                    && getObjectAfterSelection() instanceof GwtCloudPubSubEntry) {
                this.alertDialog.show(MSGS.cannotDeleteConnection(), AlertDialog.Severity.ALERT, null);
                return;
            }

            if (getTableSize() > 0) {
                showDeleteModal();
            }
        });

        this.statusConnectDisconnect.addClickHandler(event -> {
            GwtCloudEntry selection = this.selectionModel.getSelectedObject();
            final String selectedCloudServicePid = selection.getPid();
            connectDisconnectDataService(selectedCloudServicePid);
        });
    }

    private void initConnectionsTable() {

        {

            TextColumn<GwtCloudEntry> col = new TextColumn<GwtCloudEntry>() {

                @Override
                public String getValue(GwtCloudEntry object) {
                    final String pid = object.getPid();

                    if (object instanceof GwtCloudPubSubEntry) {
                        return " -> " + pid;
                    } else {
                        return pid;
                    }
                }

                @Override
                public void render(Context context, GwtCloudEntry object, SafeHtmlBuilder sb) {
                    final String pid = object.getPid();

                    if (object instanceof GwtCloudPubSubEntry) {

                        final String iconStyle = ((GwtCloudPubSubEntry) object)
                                .getType() == GwtCloudPubSubEntry.Type.PUBLISHER ? "fa-arrow-up" : "fa-arrow-down";

                        sb.append(() -> "&ensp;<i class=\"fa assets-status-icon " + iconStyle + "\"></i>" + pid);
                    } else {
                        sb.append(() -> "<i class=\"fa assets-status-icon fa-cloud\"></i>" + pid);
                    }
                }
            };
            col.setCellStyleNames("status-table-row");
            this.connectionsGrid.addColumn(col, MSGS.connectionCloudConnectionPidHeader());
        }

        {

            TextColumn<GwtCloudEntry> col = new TextColumn<GwtCloudEntry>() {

                @Override
                public String getValue(GwtCloudEntry object) {

                    if (object instanceof GwtCloudConnectionEntry) {
                        return "Cloud connection";
                    }

                    return ((GwtCloudPubSubEntry) object).getType() == GwtCloudPubSubEntry.Type.PUBLISHER ? "Publisher"
                            : "Subscriber";
                }

            };
            col.setCellStyleNames("status-table-row");
            this.connectionsGrid.addColumn(col, MSGS.typeLabel());
        }

        {
            TextColumn<GwtCloudEntry> col = new TextColumn<GwtCloudEntry>() {

                @Override
                public String getValue(GwtCloudEntry object) {

                    if (!(object instanceof GwtCloudConnectionEntry)) {
                        return "";
                    }

                    final GwtCloudConnectionEntry entry = (GwtCloudConnectionEntry) object;

                    switch (entry.getState()) {
                    case UNREGISTERED:
                        return MSGS.unregistered();
                    case CONNECTED:
                        return MSGS.connected();
                    case DISCONNECTED:
                        return MSGS.disconnected();
                    default:
                        return entry.getState().toString();
                    }
                }

                @Override
                public String getCellStyleNames(Context context, GwtCloudEntry object) {
                    final String defaultStyle = "status-table-row";

                    if (!(object instanceof GwtCloudConnectionEntry)) {
                        return defaultStyle;
                    }

                    final GwtCloudConnectionEntry entry = (GwtCloudConnectionEntry) object;

                    switch (entry.getState()) {
                    case CONNECTED:
                        return defaultStyle + " text-success";
                    case DISCONNECTED:
                        return defaultStyle + " text-danger";
                    default:
                        return defaultStyle;
                    }
                }
            };

            this.connectionsGrid.addColumn(col, MSGS.netIPv4Status());
        }

        {
            TextColumn<GwtCloudEntry> col = new TextColumn<GwtCloudEntry>() {

                @Override
                public String getValue(GwtCloudEntry object) {

                    if (object instanceof GwtCloudConnectionEntry) {
                        return ((GwtCloudConnectionEntry) object).getCloudConnectionFactoryPid();
                    }

                    return object.getFactoryPid();
                }
            };
            col.setCellStyleNames("status-table-row");
            this.connectionsGrid.addColumn(col, MSGS.connectionCloudConnectionFactoryPidHeader());
        }

        this.cloudServicesDataProvider.addDataDisplay(this.connectionsGrid);
    }

    private void createPubSub() {
        final String kuraServicePid = this.pubSubPid.getPid();

        if (kuraServicePid == null) {
            return;
        }

        this.newPubSubModal.hide();

        final GwtCloudEntry entry = getSelectedObject();

        if (!(entry instanceof GwtCloudConnectionEntry)) {
            return;
        }

        final String cloudConnectionPid = entry.getPid();
        final String factoryPid = this.pubSubFactoriesPids.getSelectedValue();

        RequestQueue.submit(context -> this.gwtXSRFService.generateSecurityToken(
                context.callback(token -> this.gwtCloudConnection.createPubSubInstance(token, kuraServicePid, factoryPid,
                        cloudConnectionPid, context.callback(v -> this.cloudServicesUi.refresh())))));
    }

    private void deletePubSub(final String pid) {

        RequestQueue.submit(
                context -> this.gwtXSRFService.generateSecurityToken(context.callback(token -> this.gwtCloudConnection
                        .deletePubSubInstance(token, pid, context.callback(v -> this.cloudServicesUi.refresh())))));
    }

    private void createCloudConnectionServiceFactory() {
        final String factoryPid = this.cloudFactoriesPids.getSelectedValue();
        final String newCloudServicePid = this.cloudConnectionPid.getPid();

        if (newCloudServicePid == null) {
            return;
        }

        RequestQueue.submit(context -> this.gwtXSRFService.generateSecurityToken(
                context.callback(token -> this.gwtCloudConnection.createCloudServiceFromFactory(token, factoryPid,
                        newCloudServicePid, context.callback(new AsyncCallback<Void>() {

                            @Override
                            public void onFailure(Throwable caught) {
                                FailureHandler.handle(caught,
                                        CloudInstancesUi.this.gwtCloudConnection.getClass().getSimpleName());
                                CloudInstancesUi.this.newConnectionModal.hide();
                            }

                            @Override
                            public void onSuccess(Void result) {
                                CloudInstancesUi.this.cloudServicesUi.refresh();
                                CloudInstancesUi.this.newConnectionModal.hide();
                            }
                        })))));
    }

    private void refresh() {
        int size = this.cloudServicesDataProvider.getList().size();
        this.connectionsGrid.setVisibleRange(0, size);
        this.cloudServicesDataProvider.flush();

        if (size > 0) {
            GwtCloudEntry firstEntry = this.cloudServicesDataProvider.getList().get(0);
            this.selectionModel.setSelected(firstEntry, true);
        }
        this.connectionsGrid.redraw();
    }

    private void showNewPubSubModal(final String connectionFactoryPid) {
        this.pubSubFactoriesPids.clear();
        this.pubSubPid.clear();

        final List<GwtCloudEntry> entries = this.pubSubFactoryEntries.get(connectionFactoryPid);

        if (entries == null || entries.isEmpty()) {
            this.alertDialog.show(MSGS.noPubSubFactoriesFound(), AlertDialog.Severity.ALERT, null);
            return;
        }

        for (final GwtCloudEntry entry : entries) {
            this.pubSubFactoriesPids.addItem(entry.getPid());
        }
        
        String selectedPubSubPid = CloudInstancesUi.this.pubSubFactoriesPids.getSelectedValue();
        getSuggestedCloudPubSubPid(selectedPubSubPid);

        this.newPubSubModal.show();
    }

    private void showNewConnectionModal() {
        this.cloudConnectionPid.clear();
        this.cloudFactoriesPids.clear();

        for (final String cloudConnectionFactoryPid : this.cloudConnectionFactoryPids) {
            this.cloudFactoriesPids.addItem(cloudConnectionFactoryPid);
        }
        String selectedFactoryPid = CloudInstancesUi.this.cloudFactoriesPids.getSelectedValue();
        getSuggestedCloudConnectionPid(selectedFactoryPid);
        this.newConnectionModal.show();
    }

    private void connectDisconnectDataService(final String connectionId) {
        RequestQueue.submit(context -> this.gwtXSRFService
                .generateSecurityToken(context.callback(token -> CloudInstancesUi.this.gwtStatusService
                        .isConnected(token, connectionId, context.callback(isConnected -> {
                            if (isConnected) {
                                CloudInstancesUi.this.disconnectDataService(connectionId);
                            } else {
                                CloudInstancesUi.this.connectDataService(connectionId);
                            }
                        })))));
    }

    private void connectDataService(final String connectionId) {

        RequestQueue.submit(context -> this.gwtXSRFService
                .generateSecurityToken(context.callback(token -> CloudInstancesUi.this.gwtStatusService
                        .connectDataService(token, connectionId, context.<Void> callback()))));
    }

    private void disconnectDataService(final String connectionId) {

        RequestQueue.submit(context -> this.gwtXSRFService
                .generateSecurityToken(context.callback(token -> CloudInstancesUi.this.gwtStatusService
                        .disconnectDataService(token, connectionId, context.<Void> callback()))));

    }

    private void deleteConnection(final String factoryPid, final String cloudServicePid) {

        RequestQueue.submit(context -> this.gwtXSRFService.generateSecurityToken(
                context.callback(token -> CloudInstancesUi.this.gwtCloudConnection.deleteCloudServiceFromFactory(token,
                        factoryPid, cloudServicePid,
                        context.callback(result -> CloudInstancesUi.this.cloudServicesUi.refresh())))));

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
        Button yes = new Button(MSGS.yesButton(), event -> {
            GwtCloudEntry selection = CloudInstancesUi.this.selectionModel.getSelectedObject();

            if (selection instanceof GwtCloudConnectionEntry) {
                final GwtCloudConnectionEntry entry = (GwtCloudConnectionEntry) selection;

                final String selectedFactoryPid = entry.getCloudConnectionFactoryPid();
                final String selectedCloudServicePid = entry.getPid();
                deleteConnection(selectedFactoryPid, selectedCloudServicePid);
            } else {
                deletePubSub(selection.getPid());
            }
            modal.hide();
        });

        Button no = new Button(MSGS.noButton(), event -> modal.hide());

        footer.add(no);
        footer.add(yes);
        modal.add(footer);
        modal.show();
        no.setFocus(true);
    }

    private void getSuggestedCloudConnectionPid(final String factoryPid) {
        RequestQueue.submit(context -> {
            this.cloudConnectionPid.clear();
            this.cloudConnectionPid.setEnabled(false);
            this.cloudConnectionPidSpinner.setVisible(true);
            this.gwtCloudConnection.findSuggestedCloudServicePid(factoryPid,
                    context.callback(result -> getCloudConnectionPidRegex(context, factoryPid, result)));
        }, false);
    }

    @SuppressWarnings("unchecked")
    private void getCloudConnectionPidRegex(final RequestContext context, final String factoryPid, final String example) {
        this.gwtCloudConnection.findCloudServicePidRegex(factoryPid, context.callback(result -> {
            final PidTextBox pidTextBox = CloudInstancesUi.this.cloudConnectionPid;
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
            this.cloudConnectionPidSpinner.setVisible(false);
        }));
    }
    
    @SuppressWarnings("unchecked")
    private void getSuggestedCloudPubSubPid(final String pubSubPid) {
        this.pubSubPid.clear();
        this.pubSubPid.setEnabled(false);
        this.pubSubPid.setVisible(true);
       
        final List<GwtCloudEntry> providerList = cloudComponentFactories.getPubSubFactories();

        for (final GwtCloudEntry entry : providerList) {
            if (pubSubPid.equals(entry.getPid())) {
                String example = entry.getDefaultFactoryPid();
                String regex = entry.getDefaultFactoryPidRegex();
                
                String placeholder = null;
                String validationMessage = null;
                if (example != null) {
                    placeholder = MSGS.exampleGiven(example);
                    validationMessage = MSGS.mustBeLike(example);
                }

                final PidTextBox pidTextBox = CloudInstancesUi.this.pubSubPid;
                pidTextBox.reset();
                if (regex != null) {
                    pidTextBox.setValidators(new RegExValidator(regex, validationMessage));
                } else {
                    pidTextBox.setValidators();
                }
                pidTextBox.setPlaceholder(placeholder);
                pidTextBox.setEnabled(true);
                this.pubSubPidSpinner.setVisible(false);
            }
        }
    }
}
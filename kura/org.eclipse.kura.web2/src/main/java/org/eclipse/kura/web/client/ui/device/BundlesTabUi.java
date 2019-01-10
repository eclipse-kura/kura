/*******************************************************************************
 * Copyright (c) 2011, 2019 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.web.client.ui.device;

import java.util.ArrayList;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.messages.ValidationMessages;
import org.eclipse.kura.web.client.ui.EntryClassUi;
import org.eclipse.kura.web.client.ui.Tab;
import org.eclipse.kura.web.client.util.EventService;
import org.eclipse.kura.web.client.util.FailureHandler;
import org.eclipse.kura.web.shared.ForwardedEventTopic;
import org.eclipse.kura.web.shared.model.GwtGroupedNVPair;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.service.GwtDeviceService;
import org.eclipse.kura.web.shared.service.GwtDeviceServiceAsync;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenService;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenServiceAsync;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.gwt.CellTable;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.cellview.client.TextHeader;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SingleSelectionModel;

public class BundlesTabUi extends Composite implements Tab {

    private static BundlesTabUiUiBinder uiBinder = GWT.create(BundlesTabUiUiBinder.class);

    interface BundlesTabUiUiBinder extends UiBinder<Widget, BundlesTabUi> {
    }

    private static final Messages MSGS = GWT.create(Messages.class);
    private static final ValidationMessages validationMessages = GWT.create(ValidationMessages.class);
    private static final String ROW_HEADER_STYLE = "rowHeader";
    private static final String STATUS_TABLE_ROW_STYLE = "status-table-row";

    private final GwtSecurityTokenServiceAsync gwtXSRFService = GWT.create(GwtSecurityTokenService.class);
    private final GwtDeviceServiceAsync gwtDeviceService = GWT.create(GwtDeviceService.class);

    private boolean isRequestRunning = false;

    @UiField
    Button bundlesRefresh;
    @UiField
    Button bundleStart;
    @UiField
    Button bundleStop;

    @UiField
    CellTable<GwtGroupedNVPair> bundlesGrid = new CellTable<>();
    private final ListDataProvider<GwtGroupedNVPair> bundlesDataProvider = new ListDataProvider<>();
    private final SingleSelectionModel<GwtGroupedNVPair> selectionModel = new SingleSelectionModel<>();

    private final GwtDeviceServiceAsync deviceService = GWT.create(GwtDeviceService.class);
    private final GwtSecurityTokenServiceAsync securityTokenService = GWT.create(GwtSecurityTokenService.class);

    public BundlesTabUi() {
        initWidget(uiBinder.createAndBindUi(this));
        loadBundlesTable(this.bundlesGrid, this.bundlesDataProvider);

        this.bundlesRefresh.setText("Refresh");
        this.bundleStart.setText("Start Bundle");
        this.bundleStop.setText("Stop Bundle");

        updateButtons();

        this.bundlesGrid.setSelectionModel(this.selectionModel);
        this.selectionModel.addSelectionChangeHandler(event -> updateButtons());
        this.bundlesRefresh.addClickHandler(event -> refresh());
        this.bundleStart.addClickHandler(event -> startSelectedBundle());
        this.bundleStop.addClickHandler(event -> stopSelectedBundle());

        EventService.Handler onBundleUpdatedHandler = eventInfo -> {
            if (BundlesTabUi.this.isVisible() && BundlesTabUi.this.isAttached()) {
                refresh();
            }
        };

        EventService.subscribe(ForwardedEventTopic.BUNDLE_INSTALLED, onBundleUpdatedHandler);
        EventService.subscribe(ForwardedEventTopic.BUNDLE_RESOLVED, onBundleUpdatedHandler);
        EventService.subscribe(ForwardedEventTopic.BUNDLE_STARTED, onBundleUpdatedHandler);
        EventService.subscribe(ForwardedEventTopic.BUNDLE_STOPPED, onBundleUpdatedHandler);
        EventService.subscribe(ForwardedEventTopic.BUNDLE_UNINSTALLED, onBundleUpdatedHandler);
        EventService.subscribe(ForwardedEventTopic.BUNDLE_UNRESOLVED, onBundleUpdatedHandler);
    }

    private void updateButtons() {
        GwtGroupedNVPair selected = this.selectionModel.getSelectedObject();

        this.bundleStart.setEnabled(false);
        this.bundleStop.setEnabled(false);

        String status;

        if (selected == null || (status = selected.getStatus()) == null) {
            return;
        }

        boolean isActive = "bndActive".equals(status);

        this.bundleStart.setEnabled(!isActive);
        this.bundleStop.setEnabled(isActive);
    }

    private void startSelectedBundle() {
        EntryClassUi.showWaitModal();

        this.securityTokenService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

            @Override
            public void onSuccess(GwtXSRFToken token) {
                BundlesTabUi.this.deviceService.startBundle(token,
                        BundlesTabUi.this.selectionModel.getSelectedObject().getId(), new AsyncCallback<Void>() {

                    @Override
                    public void onFailure(Throwable caught) {
                        EntryClassUi.hideWaitModal();
                        FailureHandler.handle(caught);

                    }

                    @Override
                    public void onSuccess(Void result) {
                        EntryClassUi.hideWaitModal();
                    }
                });

            }

            @Override
            public void onFailure(Throwable caught) {
                EntryClassUi.hideWaitModal();
                FailureHandler.handle(caught);
            }
        });
    }

    private void stopSelectedBundle() {
        EntryClassUi.showWaitModal();
        this.securityTokenService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

            @Override
            public void onFailure(Throwable caught) {
                EntryClassUi.hideWaitModal();
                FailureHandler.handle(caught);
            }

            @Override
            public void onSuccess(GwtXSRFToken token) {
                BundlesTabUi.this.deviceService.stopBundle(token,
                        BundlesTabUi.this.selectionModel.getSelectedObject().getId(), new AsyncCallback<Void>() {

                    @Override
                    public void onFailure(Throwable caught) {
                        EntryClassUi.hideWaitModal();
                        FailureHandler.handle(caught);

                    }

                    @Override
                    public void onSuccess(Void result) {
                        EntryClassUi.hideWaitModal();
                    }
                });
            }
        });
    }

    private void loadBundlesTable(CellTable<GwtGroupedNVPair> bundlesGrid2,
            ListDataProvider<GwtGroupedNVPair> dataProvider) {

        TextColumn<GwtGroupedNVPair> col1 = new TextColumn<GwtGroupedNVPair>() {

            @Override
            public String getValue(GwtGroupedNVPair object) {
                return object.getId();
            }
        };
        col1.setCellStyleNames(STATUS_TABLE_ROW_STYLE);
        TextHeader id = new TextHeader(MSGS.deviceBndId());
        id.setHeaderStyleNames(ROW_HEADER_STYLE);
        bundlesGrid2.addColumn(col1, id);

        TextColumn<GwtGroupedNVPair> col2 = new TextColumn<GwtGroupedNVPair>() {

            @Override
            public String getValue(GwtGroupedNVPair object) {
                return object.getName();
            }
        };
        col2.setCellStyleNames(STATUS_TABLE_ROW_STYLE);
        TextHeader name = new TextHeader(MSGS.deviceBndName());
        name.setHeaderStyleNames(ROW_HEADER_STYLE);
        bundlesGrid2.addColumn(col2, name);

        TextColumn<GwtGroupedNVPair> col3 = new TextColumn<GwtGroupedNVPair>() {

            @Override
            public String getValue(GwtGroupedNVPair object) {
                return validationMessages.getString(object.getStatus());
            }
        };
        col3.setCellStyleNames(STATUS_TABLE_ROW_STYLE);
        TextHeader state = new TextHeader(MSGS.deviceBndState());
        state.setHeaderStyleNames(ROW_HEADER_STYLE);
        bundlesGrid2.addColumn(col3, state);

        TextColumn<GwtGroupedNVPair> col4 = new TextColumn<GwtGroupedNVPair>() {

            @Override
            public String getValue(GwtGroupedNVPair object) {
                return object.getVersion();
            }
        };
        col4.setCellStyleNames(STATUS_TABLE_ROW_STYLE);
        TextHeader version = new TextHeader(MSGS.deviceBndVersion());
        version.setHeaderStyleNames(ROW_HEADER_STYLE);
        bundlesGrid2.addColumn(col4, version);

        dataProvider.addDataDisplay(bundlesGrid2);
    }

    @Override
    public void setDirty(boolean flag) {
        // Not needed
    }

    @Override
    public boolean isDirty() {
        return true;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public void refresh() {
        if (this.isRequestRunning) {
            return;
        }

        this.isRequestRunning = true;
        EntryClassUi.showWaitModal();

        this.bundlesDataProvider.getList().clear();
        this.gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

            @Override
            public void onFailure(Throwable ex) {
                BundlesTabUi.this.isRequestRunning = false;
                EntryClassUi.hideWaitModal();
                FailureHandler.handle(ex);
            }

            @Override
            public void onSuccess(GwtXSRFToken token) {
                BundlesTabUi.this.gwtDeviceService.findBundles(token, new AsyncCallback<ArrayList<GwtGroupedNVPair>>() {

                    @Override
                    public void onFailure(Throwable caught) {
                        BundlesTabUi.this.isRequestRunning = false;
                        EntryClassUi.hideWaitModal();
                        FailureHandler.handle(caught);
                        BundlesTabUi.this.bundlesDataProvider.flush();
                    }

                    @Override
                    public void onSuccess(ArrayList<GwtGroupedNVPair> result) {
                        EntryClassUi.hideWaitModal();
                        BundlesTabUi.this.isRequestRunning = false;
                        for (GwtGroupedNVPair resultPair : result) {
                            BundlesTabUi.this.bundlesDataProvider.getList().add(resultPair);
                        }
                        int size = BundlesTabUi.this.bundlesDataProvider.getList().size();
                        BundlesTabUi.this.bundlesGrid.setVisibleRange(0, size);
                        BundlesTabUi.this.bundlesDataProvider.flush();
                    }
                });
            }

        });
    }

}

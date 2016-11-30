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
package org.eclipse.kura.web.client.ui.Device;

import java.util.ArrayList;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.messages.ValidationMessages;
import org.eclipse.kura.web.client.ui.EntryClassUi;
import org.eclipse.kura.web.client.util.EventService;
import org.eclipse.kura.web.client.util.FailureHandler;
import org.eclipse.kura.web.shared.ForwardedEventTopic;
import org.eclipse.kura.web.shared.model.GwtEventInfo;
import org.eclipse.kura.web.shared.model.GwtGroupedNVPair;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.service.GwtDeviceService;
import org.eclipse.kura.web.shared.service.GwtDeviceServiceAsync;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenService;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenServiceAsync;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.gwt.CellTable;

import com.google.gwt.core.client.GWT;
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

public class BundlesTabUi extends Composite {

    private static BundlesTabUiUiBinder uiBinder = GWT.create(BundlesTabUiUiBinder.class);

    interface BundlesTabUiUiBinder extends UiBinder<Widget, BundlesTabUi> {
    }

    private boolean isRequestRunning = false;

    private static final Messages MSGS = GWT.create(Messages.class);
    private static final ValidationMessages msgs = GWT.create(ValidationMessages.class);

    private final GwtSecurityTokenServiceAsync gwtXSRFService = GWT.create(GwtSecurityTokenService.class);
    private final GwtDeviceServiceAsync gwtDeviceService = GWT.create(GwtDeviceService.class);

    @UiField
    Button bundlesRefresh;
    @UiField
    Button bundleStart;
    @UiField
    Button bundleStop;

    @UiField
    CellTable<GwtGroupedNVPair> bundlesGrid = new CellTable<GwtGroupedNVPair>();
    private final ListDataProvider<GwtGroupedNVPair> bundlesDataProvider = new ListDataProvider<GwtGroupedNVPair>();
    private final SingleSelectionModel<GwtGroupedNVPair> selectionModel = new SingleSelectionModel<GwtGroupedNVPair>();

    private GwtDeviceServiceAsync deviceService = GWT.create(GwtDeviceService.class);
    private GwtSecurityTokenServiceAsync securityTokenService = GWT.create(GwtSecurityTokenService.class);

    public BundlesTabUi() {
        initWidget(uiBinder.createAndBindUi(this));
        loadBundlesTable(this.bundlesGrid, this.bundlesDataProvider);

        bundlesRefresh.setText("Refresh");
        bundleStart.setText("Start Bundle");
        bundleStop.setText("Stop Bundle");

        updateButtons();

        bundlesGrid.setSelectionModel(selectionModel);
        selectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {

            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                updateButtons();
            }
        });
        bundlesRefresh.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                loadBundlesData();

            }
        });
        bundleStart.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                startSelectedBundle();

            }
        });
        bundleStop.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                stopSelectedBundle();

            }
        });
        // loadBundlesData();

        EventService.Handler onBundleUpdatedHandler = new EventService.Handler() {

            @Override
            public void handleEvent(GwtEventInfo eventInfo) {
                if (BundlesTabUi.this.isVisible() && BundlesTabUi.this.isAttached()) {
                    loadBundlesData();
                }
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
        GwtGroupedNVPair selected = selectionModel.getSelectedObject();

        bundleStart.setEnabled(false);
        bundleStop.setEnabled(false);

        String status;

        if (selected == null || (status = selected.getStatus()) == null) {
            return;
        }

        boolean isActive = "bndActive".equals(status);

        bundleStart.setEnabled(!isActive);
        bundleStop.setEnabled(isActive);
    }

    private void startSelectedBundle() {
        EntryClassUi.showWaitModal();

        securityTokenService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

            @Override
            public void onSuccess(GwtXSRFToken token) {
                deviceService.startBundle(token, selectionModel.getSelectedObject().getId(), new AsyncCallback<Void>() {

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
        securityTokenService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

            @Override
            public void onFailure(Throwable caught) {
                EntryClassUi.hideWaitModal();
                FailureHandler.handle(caught);
            }

            @Override
            public void onSuccess(GwtXSRFToken token) {
                deviceService.stopBundle(token, selectionModel.getSelectedObject().getId(), new AsyncCallback<Void>() {

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
        col1.setCellStyleNames("status-table-row");
        bundlesGrid2.addColumn(col1, MSGS.deviceBndId());

        TextColumn<GwtGroupedNVPair> col2 = new TextColumn<GwtGroupedNVPair>() {

            @Override
            public String getValue(GwtGroupedNVPair object) {
                return object.getName();
            }
        };
        col2.setCellStyleNames("status-table-row");
        bundlesGrid2.addColumn(col2, MSGS.deviceBndName());

        TextColumn<GwtGroupedNVPair> col3 = new TextColumn<GwtGroupedNVPair>() {

            @Override
            public String getValue(GwtGroupedNVPair object) {
                return msgs.getString(object.getStatus());
            }
        };
        col3.setCellStyleNames("status-table-row");
        bundlesGrid2.addColumn(col3, MSGS.deviceBndState());

        TextColumn<GwtGroupedNVPair> col4 = new TextColumn<GwtGroupedNVPair>() {

            @Override
            public String getValue(GwtGroupedNVPair object) {
                return object.getVersion();
            }
        };
        col4.setCellStyleNames("status-table-row");
        bundlesGrid2.addColumn(col4, MSGS.deviceBndVersion());

        dataProvider.addDataDisplay(bundlesGrid2);
    }

    public void loadBundlesData() {

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

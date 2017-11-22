/*******************************************************************************
 * Copyright (c) 2011, 2017 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.web.client.ui.DeviceScanner;

import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.ui.EntryClassUi;
import org.eclipse.kura.web.client.util.FailureHandler;
import org.eclipse.kura.web.shared.GwtKuraException;
import org.eclipse.kura.web.shared.model.GwtDeviceScannerModel;
import org.eclipse.kura.web.shared.model.GwtSession;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.service.GwtDeviceService;
import org.eclipse.kura.web.shared.service.GwtDeviceServiceAsync;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenService;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenServiceAsync;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.TextBox;
import org.gwtbootstrap3.client.ui.Well;
import org.gwtbootstrap3.client.ui.gwt.CellTable;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;

public class DeviceScannerPanelUi extends Composite {

    private static final Logger logger = Logger.getLogger(DeviceScannerPanelUi.class.getSimpleName());
    private static DeviceScannerPanelUiUiBinder uiBinder = GWT.create(DeviceScannerPanelUiUiBinder.class);
    private static final String DEV_INFO = "devInfo";

    interface DeviceScannerPanelUiUiBinder extends UiBinder<Widget, DeviceScannerPanelUi> {
    }

    private final GwtSecurityTokenServiceAsync gwtXSRFService = GWT.create(GwtSecurityTokenService.class);
    private final GwtDeviceServiceAsync gwtDeviceService = GWT.create(GwtDeviceService.class);
    private static final Messages MSG = GWT.create(Messages.class);

    private GwtSession currentSession;
    private EntryClassUi parent;

    @UiField
    Well deviceScannerWell;
    @UiField
    Button deviceScannerRefresh;
    @UiField
    TextBox formPeriod = new TextBox();
    @UiField
    TextBox formMaxScan = new TextBox();
    @UiField
    ListBox formAdapter;

    @UiField
    CellTable<GwtDeviceScannerModel> deviceScannerGrid = new CellTable<GwtDeviceScannerModel>();
    private final ListDataProvider<GwtDeviceScannerModel> profileDataProvider = new ListDataProvider<>();

    public DeviceScannerPanelUi() {
        logger.log(Level.FINER, "Initializing DeviceScannerPanelUi...");
        initWidget(uiBinder.createAndBindUi(this));
        // Set text for buttons
        this.deviceScannerRefresh.setText(MSG.refresh());
        this.deviceScannerGrid.getEmptyTableWidget();
        // Set List Box for buttons
        this.formAdapter.addItem("hci0");
        this.formAdapter.addItem("ublox");
        if (this.formAdapter.isItemSelected(1)) {
            this.formPeriod.setEnabled(false);
        }
        loadDeviceScannerTable(this.deviceScannerGrid, this.profileDataProvider);
    }

    // get current session from UI parent
    public void setSession(GwtSession gwtBSSession) {
        this.currentSession = gwtBSSession;
    }

    public void loadDeviceScannerTable(CellTable<GwtDeviceScannerModel> profileGrid2,
            ListDataProvider<GwtDeviceScannerModel> dataProvider) {

        this.deviceScannerRefresh.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                try {
                    loadDeviceScannerData();
                } catch (GwtKuraException e) {
                    e.printStackTrace();
                }
            }
        });

        TextColumn<GwtDeviceScannerModel> macAddr = new TextColumn<GwtDeviceScannerModel>() {

            @Override
            public String getValue(GwtDeviceScannerModel gwtDeviceScannerModel) {
                return gwtDeviceScannerModel.getMacAddr();
            }
        };
        macAddr.setCellStyleNames("status-table-row");
        this.deviceScannerGrid.addColumn(macAddr, "Mac address");

        TextColumn<GwtDeviceScannerModel> deviceName = new TextColumn<GwtDeviceScannerModel>() {

            @Override
            public String getValue(GwtDeviceScannerModel gwtDeviceScannerModel) {
                return gwtDeviceScannerModel.getDeviceName();
            }
        };
        deviceName.setCellStyleNames("status-table-row");
        this.deviceScannerGrid.addColumn(deviceName, "Device name");

        TextColumn<GwtDeviceScannerModel> timeStamp = new TextColumn<GwtDeviceScannerModel>() {

            @Override
            public String getValue(GwtDeviceScannerModel gwtDeviceScannerModel) {
                return String.valueOf(gwtDeviceScannerModel.getTimeStamp());
            }
        };
        timeStamp.setCellStyleNames("status-table-row");
        this.deviceScannerGrid.addColumn(timeStamp, "Time Stamp");

        TextColumn<GwtDeviceScannerModel> rss = new TextColumn<GwtDeviceScannerModel>() {

            @Override
            public String getValue(GwtDeviceScannerModel gwtDeviceScannerModel) {
                return String.valueOf(gwtDeviceScannerModel.getRSSI());
            }
        };
        rss.setCellStyleNames("status-table-row");
        this.deviceScannerGrid.addColumn(rss, "RSSI");

        this.deviceScannerWell.add(this.deviceScannerGrid);
        dataProvider.addDataDisplay(profileGrid2);
    }

    public void setParent(EntryClassUi parent) {
        this.parent = parent;
    }

    private void loadDeviceScannerData() throws GwtKuraException {

        EntryClassUi.showWaitModal();
        this.profileDataProvider.getList().clear();
        this.gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

            @Override
            public void onFailure(Throwable ex) {
                EntryClassUi.hideWaitModal();
                FailureHandler.handle(ex);
            }

            @Override
            public void onSuccess(GwtXSRFToken token) {
                HashSet<GwtDeviceScannerModel> listTest = new HashSet<GwtDeviceScannerModel>();
                DeviceScannerPanelUi.this.gwtDeviceService.findDeviceScanner(token,
                        DeviceScannerPanelUi.this.formPeriod.getValue(),
                        DeviceScannerPanelUi.this.formMaxScan.getValue(),
                        DeviceScannerPanelUi.this.formAdapter.getSelectedItemText(),

                        new AsyncCallback<HashSet<GwtDeviceScannerModel>>() {

                            private Object deviceScannerGrid;

                            @Override
                            public void onFailure(Throwable caught) {
                                EntryClassUi.hideWaitModal();
                                DeviceScannerPanelUi.this.profileDataProvider.getList().clear();
                                FailureHandler.handle(caught);
                                DeviceScannerPanelUi.this.profileDataProvider.flush();
                            }

                            @Override
                            public void onSuccess(HashSet<GwtDeviceScannerModel> result) {
                                EntryClassUi.hideWaitModal();
                                DeviceScannerPanelUi.this.profileDataProvider.getList().clear();
                                if (result.isEmpty()) {
                                    // Window.alert("Device scanner failed !");
                                    FailureHandler.handle("Device scanner failed !");
                                } else {
                                    for (GwtDeviceScannerModel resultPair : result) {
                                        DeviceScannerPanelUi.this.profileDataProvider.getList().add(resultPair);
                                    }
                                }
                                int size = DeviceScannerPanelUi.this.profileDataProvider.getList().size();
                                DeviceScannerPanelUi.this.deviceScannerGrid.setVisibleRange(0, size);
                                DeviceScannerPanelUi.this.profileDataProvider.flush();
                            }
                        });
            }

        });
    }
}
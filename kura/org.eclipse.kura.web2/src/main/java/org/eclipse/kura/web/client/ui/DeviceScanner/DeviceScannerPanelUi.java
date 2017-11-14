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

import java.util.ArrayList;
import java.util.List;
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
import org.gwtbootstrap3.client.ui.Well;
import org.gwtbootstrap3.client.ui.gwt.CellTable;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
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
    CellTable<GwtDeviceScannerModel> deviceScannerGrid = new CellTable<GwtDeviceScannerModel>();
    private final ListDataProvider<GwtDeviceScannerModel> profileDataProvider = new ListDataProvider<>();

    public DeviceScannerPanelUi() {
        logger.log(Level.FINER, "Initializing TestPanelUi...");
        initWidget(uiBinder.createAndBindUi(this));
        // Set text for buttons
        this.deviceScannerRefresh.setText(MSG.refresh());
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
                return gwtDeviceScannerModel.getDeviceName();
            }
        };
        timeStamp.setCellStyleNames("status-table-row");
        this.deviceScannerGrid.addColumn(timeStamp, "Time Stamp");

        TextColumn<GwtDeviceScannerModel> rss = new TextColumn<GwtDeviceScannerModel>() {

            @Override
            public String getValue(GwtDeviceScannerModel gwtDeviceScannerModel) {
                return gwtDeviceScannerModel.getDeviceName();
            }
        };
        rss.setCellStyleNames("status-table-row");
        this.deviceScannerGrid.addColumn(rss, "RSS");
        this.deviceScannerWell.add(this.deviceScannerGrid);
        dataProvider.addDataDisplay(profileGrid2);
    }

    public void setParent(EntryClassUi parent) {
        this.parent = parent;
    }

    private void loadDeviceScannerData() throws GwtKuraException {
        this.profileDataProvider.getList().clear();
        // EntryClassUi.showWaitModal();
        this.gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

            @Override
            public void onFailure(Throwable ex) {
                EntryClassUi.hideWaitModal();
                FailureHandler.handle(ex);
            }

            @Override
            public void onSuccess(GwtXSRFToken token) {
                List<GwtDeviceScannerModel> listTest = new ArrayList<GwtDeviceScannerModel>();
                DeviceScannerPanelUi.this.gwtDeviceService.findDeviceScanner(token,
                        new AsyncCallback<ArrayList<GwtDeviceScannerModel>>() {

                            private Object deviceScannerGrid;

                            @Override
                            public void onFailure(Throwable caught) {
                                EntryClassUi.hideWaitModal();
                                DeviceScannerPanelUi.this.profileDataProvider.getList().clear();
                                FailureHandler.handle(caught);
                                DeviceScannerPanelUi.this.profileDataProvider.flush();
                                Window.alert("Error");
                            }

                            @Override
                            public void onSuccess(ArrayList<GwtDeviceScannerModel> result) {
                                // String oldGroup = DEV_INFO;
                                // DeviceScannerPanelUi.this.profileDataProvider.getList()
                                // .add(new GwtGroupedNVPair(DEV_INFO, DEV_INFO, " "));
                                EntryClassUi.hideWaitModal();
                                for (GwtDeviceScannerModel resultPair : result) {

                                    /*
                                     * if (!oldGroup.equals(resultPair.getGroup())) {
                                     * DeviceScannerPanelUi.this.profileDataProvider.getList()
                                     * .add(new GwtGroupedNVPair(resultPair.getGroup(), resultPair.getGroup(),
                                     * "  "));
                                     * oldGroup = resultPair.getGroup();
                                     * }
                                     */

                                    // Window.alert(resultPair.getDeviceName());
                                    DeviceScannerPanelUi.this.profileDataProvider.getList().add(resultPair);
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
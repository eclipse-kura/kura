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
import org.eclipse.kura.web.shared.model.GwtDeviceScanner;
import org.eclipse.kura.web.shared.model.GwtSession;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.Well;
import org.gwtbootstrap3.client.ui.gwt.CellTable;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

public class DeviceScannerPanelUi extends Composite {

    private static final Logger logger = Logger.getLogger(DeviceScannerPanelUi.class.getSimpleName());
    private static DeviceScannerPanelUiUiBinder uiBinder = GWT.create(DeviceScannerPanelUiUiBinder.class);

    interface DeviceScannerPanelUiUiBinder extends UiBinder<Widget, DeviceScannerPanelUi> {
    }

    private static final Messages MSG = GWT.create(Messages.class);

    private GwtSession currentSession;
    private EntryClassUi parent;

    @UiField
    Well deviceScannerWell;
    @UiField
    Button deviceScannerRefresh;
    @UiField
    CellTable<GwtDeviceScanner> deviceScannerGrid = new CellTable<GwtDeviceScanner>();

    public DeviceScannerPanelUi() {
        logger.log(Level.FINER, "Initializing TestPanelUi...");
        initWidget(uiBinder.createAndBindUi(this));
        // Set text for buttons
        this.deviceScannerRefresh.setText(MSG.refresh());
        loadDeviceScannerTable();
    }

    // get current session from UI parent
    public void setSession(GwtSession gwtBSSession) {
        this.currentSession = gwtBSSession;
    }

    public void loadDeviceScannerTable() {
        this.deviceScannerRefresh.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                deviceScannerGrid.redraw();
                loadTestData();
            }
        });

        TextColumn<GwtDeviceScanner> macAddr = new TextColumn<GwtDeviceScanner>() {

            @Override
            public String getValue(GwtDeviceScanner gwtDeviceScanner) {
                return gwtDeviceScanner.getMacAddr();
            }
        };
        macAddr.setCellStyleNames("status-table-row");
        this.deviceScannerGrid.addColumn(macAddr, "Mac address");

        TextColumn<GwtDeviceScanner> deviceName = new TextColumn<GwtDeviceScanner>() {

            @Override
            public String getValue(GwtDeviceScanner gwtDeviceScanner) {
                return gwtDeviceScanner.getDeviceName();
            }
        };
        deviceName.setCellStyleNames("status-table-row");
        this.deviceScannerGrid.addColumn(deviceName, "Device name");

        TextColumn<GwtDeviceScanner> timeStamp = new TextColumn<GwtDeviceScanner>() {

            @Override
            public String getValue(GwtDeviceScanner gwtDeviceScanner) {
                return gwtDeviceScanner.getDeviceName();
            }
        };
        timeStamp.setCellStyleNames("status-table-row");
        this.deviceScannerGrid.addColumn(timeStamp, "Time stamp");

        TextColumn<GwtDeviceScanner> dataType = new TextColumn<GwtDeviceScanner>() {

            @Override
            public String getValue(GwtDeviceScanner gwtDeviceScanner) {
                return gwtDeviceScanner.getDeviceName();
            }
        };
        dataType.setCellStyleNames("status-table-row");
        this.deviceScannerGrid.addColumn(dataType, "Data type");

        this.deviceScannerWell.add(this.deviceScannerGrid);
    }

    public void loadTestData() {
        List<GwtDeviceScanner> listTest = new ArrayList<GwtDeviceScanner>();
        listTest.add(new GwtDeviceScanner("test1", "test1", "test1", "test1"));
        listTest.add(new GwtDeviceScanner("test2", "test2", "test2", "test1"));

        this.deviceScannerGrid.setRowCount(listTest.size(), true);
        this.deviceScannerGrid.setRowData(0, listTest);
    }

    public void setParent(EntryClassUi parent) {
        this.parent = parent;
    }
}
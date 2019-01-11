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
package org.eclipse.kura.web.client.ui.device;

import java.util.ArrayList;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.ui.EntryClassUi;
import org.eclipse.kura.web.client.ui.Tab;
import org.eclipse.kura.web.client.util.FailureHandler;
import org.eclipse.kura.web.shared.model.GwtGroupedNVPair;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.service.GwtDeviceService;
import org.eclipse.kura.web.shared.service.GwtDeviceServiceAsync;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenService;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenServiceAsync;
import org.gwtbootstrap3.client.ui.gwt.CellTable;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.RowStyles;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.cellview.client.TextHeader;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;

public class SystemPropertiesTabUi extends Composite implements Tab {

    private static SystemPropertiesTabUiUiBinder uiBinder = GWT.create(SystemPropertiesTabUiUiBinder.class);

    interface SystemPropertiesTabUiUiBinder extends UiBinder<Widget, SystemPropertiesTabUi> {
    }

    private static final Messages MSGS = GWT.create(Messages.class);
    private static final String ROW_HEADER_STYLE = "rowHeader";

    private final GwtSecurityTokenServiceAsync gwtXSRFService = GWT.create(GwtSecurityTokenService.class);
    private final GwtDeviceServiceAsync gwtDeviceService = GWT.create(GwtDeviceService.class);

    @UiField
    CellTable<GwtGroupedNVPair> systemPropertiesGrid = new CellTable<GwtGroupedNVPair>();
    private final ListDataProvider<GwtGroupedNVPair> systemPropertiesDataProvider = new ListDataProvider<GwtGroupedNVPair>();

    public SystemPropertiesTabUi() {
        initWidget(uiBinder.createAndBindUi(this));

        this.systemPropertiesGrid.setRowStyles(new RowStyles<GwtGroupedNVPair>() {

            @Override
            public String getStyleNames(GwtGroupedNVPair row, int rowIndex) {
                return row.getValue().contains("  ") ? "rowHeader" : " ";
            }
        });

        loadSystemPropertiesTable(this.systemPropertiesGrid, this.systemPropertiesDataProvider);
    }

    private void loadSystemPropertiesTable(CellTable<GwtGroupedNVPair> grid,
            ListDataProvider<GwtGroupedNVPair> dataProvider) {
        TextColumn<GwtGroupedNVPair> col1 = new TextColumn<GwtGroupedNVPair>() {

            @Override
            public String getValue(GwtGroupedNVPair object) {
                return String.valueOf(object.getName());
            }
        };
        col1.setCellStyleNames("status-table-row");
        TextHeader name = new TextHeader(MSGS.devicePropName());
        name.setHeaderStyleNames(ROW_HEADER_STYLE);
        grid.addColumn(col1, name);

        TextColumn<GwtGroupedNVPair> col2 = new TextColumn<GwtGroupedNVPair>() {

            @Override
            public String getValue(GwtGroupedNVPair object) {
                return String.valueOf(object.getValue());
            }
        };
        col2.setCellStyleNames("status-table-row");
        TextHeader value = new TextHeader(MSGS.devicePropValue());
        value.setHeaderStyleNames(ROW_HEADER_STYLE);
        grid.addColumn(col2, value);

        dataProvider.addDataDisplay(grid);
    }

    @Override
    public void setDirty(boolean flag) {
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
        this.systemPropertiesDataProvider.getList().clear();

        EntryClassUi.showWaitModal();
        this.gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

            @Override
            public void onFailure(Throwable ex) {
                EntryClassUi.hideWaitModal();
                FailureHandler.handle(ex);
            }

            @Override
            public void onSuccess(GwtXSRFToken token) {
                SystemPropertiesTabUi.this.gwtDeviceService.findSystemProperties(token,
                        new AsyncCallback<ArrayList<GwtGroupedNVPair>>() {

                    @Override
                    public void onFailure(Throwable caught) {
                        EntryClassUi.hideWaitModal();
                        SystemPropertiesTabUi.this.systemPropertiesDataProvider.getList().clear();
                        FailureHandler.handle(caught);
                        SystemPropertiesTabUi.this.systemPropertiesDataProvider.flush();
                    }

                    @Override
                    public void onSuccess(ArrayList<GwtGroupedNVPair> result) {
                        for (GwtGroupedNVPair resultPair : result) {
                            SystemPropertiesTabUi.this.systemPropertiesDataProvider.getList().add(resultPair);
                        }
                        int size = SystemPropertiesTabUi.this.systemPropertiesDataProvider.getList().size();
                        SystemPropertiesTabUi.this.systemPropertiesGrid.setVisibleRange(0, size);
                        SystemPropertiesTabUi.this.systemPropertiesDataProvider.flush();
                        EntryClassUi.hideWaitModal();
                    }

                });
            }

        });
    }

}

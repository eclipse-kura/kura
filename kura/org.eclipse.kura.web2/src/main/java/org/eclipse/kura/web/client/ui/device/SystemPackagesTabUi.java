/*******************************************************************************
 * Copyright (c) 2021 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 *******************************************************************************/
package org.eclipse.kura.web.client.ui.device;

import java.util.Collections;
import java.util.List;

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

public class SystemPackagesTabUi extends Composite implements Tab {

    private static SystemPackagesTabUiUiBinder uiBinder = GWT.create(SystemPackagesTabUiUiBinder.class);

    interface SystemPackagesTabUiUiBinder extends UiBinder<Widget, SystemPackagesTabUi> {
    }

    private static final Messages MSGS = GWT.create(Messages.class);
    private static final String ROW_HEADER_STYLE = "rowHeader";
    private static final String STATUS_TABLE_ROW_STYLE = "status-table-row";

    private final GwtSecurityTokenServiceAsync gwtXSRFService = GWT.create(GwtSecurityTokenService.class);
    private final GwtDeviceServiceAsync gwtDeviceService = GWT.create(GwtDeviceService.class);

    private boolean isRequestRunning = false;

    @UiField
    Button systemPackagesRefresh;

    @UiField
    CellTable<GwtGroupedNVPair> systemPackagesGrid = new CellTable<>();
    private final ListDataProvider<GwtGroupedNVPair> systemPackagesDataProvider = new ListDataProvider<>();

    public SystemPackagesTabUi() {
        initWidget(uiBinder.createAndBindUi(this));
        loadSystemPackagesTable(this.systemPackagesGrid, this.systemPackagesDataProvider);

        this.systemPackagesRefresh.setText(MSGS.refresh());
        this.systemPackagesRefresh.addClickHandler(event -> refresh());
    }

    private void loadSystemPackagesTable(CellTable<GwtGroupedNVPair> systemPackagesGrid2,
            ListDataProvider<GwtGroupedNVPair> dataProvider) {

        TextColumn<GwtGroupedNVPair> col1 = new TextColumn<GwtGroupedNVPair>() {

            @Override
            public String getValue(GwtGroupedNVPair object) {
                return object.getName();
            }
        };
        col1.setCellStyleNames(STATUS_TABLE_ROW_STYLE);
        TextHeader name = new TextHeader(MSGS.devicePkgName());
        name.setHeaderStyleNames(ROW_HEADER_STYLE);
        systemPackagesGrid2.addColumn(col1, name);

        TextColumn<GwtGroupedNVPair> col2 = new TextColumn<GwtGroupedNVPair>() {

            @Override
            public String getValue(GwtGroupedNVPair object) {
                return object.getVersion();
            }
        };
        col2.setCellStyleNames(STATUS_TABLE_ROW_STYLE);
        TextHeader version = new TextHeader(MSGS.devicePkgVersion());
        version.setHeaderStyleNames(ROW_HEADER_STYLE);
        systemPackagesGrid2.addColumn(col2, version);

        TextColumn<GwtGroupedNVPair> col3 = new TextColumn<GwtGroupedNVPair>() {

            @Override
            public String getValue(GwtGroupedNVPair object) {
                return object.getType();
            }
        };
        col3.setCellStyleNames(STATUS_TABLE_ROW_STYLE);
        TextHeader type = new TextHeader(MSGS.devicePkgType());
        type.setHeaderStyleNames(ROW_HEADER_STYLE);
        systemPackagesGrid2.addColumn(col3, type);

        dataProvider.addDataDisplay(systemPackagesGrid2);
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

        this.systemPackagesDataProvider.getList().clear();
        this.gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

            @Override
            public void onFailure(Throwable ex) {
                SystemPackagesTabUi.this.isRequestRunning = false;
                EntryClassUi.hideWaitModal();
                FailureHandler.handle(ex);
            }

            @Override
            public void onSuccess(GwtXSRFToken token) {
                SystemPackagesTabUi.this.gwtDeviceService.findSystemPackages(token,
                        new AsyncCallback<List<GwtGroupedNVPair>>() {

                            @Override
                            public void onFailure(Throwable caught) {
                                SystemPackagesTabUi.this.isRequestRunning = false;
                                EntryClassUi.hideWaitModal();
                                FailureHandler.handle(caught);
                                SystemPackagesTabUi.this.systemPackagesDataProvider.flush();
                            }

                            @Override
                            public void onSuccess(List<GwtGroupedNVPair> result) {
                                EntryClassUi.hideWaitModal();
                                SystemPackagesTabUi.this.isRequestRunning = false;
                                for (GwtGroupedNVPair resultPair : result) {
                                    SystemPackagesTabUi.this.systemPackagesDataProvider.getList().add(resultPair);
                                }
                                Collections.sort(SystemPackagesTabUi.this.systemPackagesDataProvider.getList(),
                                        (o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName()));
                                int size = SystemPackagesTabUi.this.systemPackagesDataProvider.getList().size();
                                SystemPackagesTabUi.this.systemPackagesGrid.setVisibleRange(0, size);
                                SystemPackagesTabUi.this.systemPackagesDataProvider.flush();
                            }
                        });
            }

        });
    }

    @Override
    public void clear() {
        // Not needed
    }

}

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
package org.eclipse.kura.web.client.ui.Status;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.messages.ValidationMessages;
import org.eclipse.kura.web.client.ui.EntryClassUi;
import org.eclipse.kura.web.client.util.FailureHandler;
import org.eclipse.kura.web.shared.model.GwtGroupedNVPair;
import org.eclipse.kura.web.shared.model.GwtSession;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenService;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenServiceAsync;
import org.eclipse.kura.web.shared.service.GwtStatusService;
import org.eclipse.kura.web.shared.service.GwtStatusServiceAsync;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.Well;
import org.gwtbootstrap3.client.ui.gwt.CellTable;

import com.google.gwt.cell.client.SafeHtmlCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.RowStyles;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;

public class StatusPanelUi extends Composite {

    private static final Logger logger = Logger.getLogger(StatusPanelUi.class.getSimpleName());
    private static StatusPanelUiUiBinder uiBinder = GWT.create(StatusPanelUiUiBinder.class);

    interface StatusPanelUiUiBinder extends UiBinder<Widget, StatusPanelUi> {
    }

    private static final ValidationMessages msgs = GWT.create(ValidationMessages.class);
    private static final Messages MSG = GWT.create(Messages.class);

    private final GwtStatusServiceAsync gwtStatusService = GWT.create(GwtStatusService.class);
    private final GwtSecurityTokenServiceAsync gwtXSRFService = GWT.create(GwtSecurityTokenService.class);

    private GwtSession currentSession;
    private final ListDataProvider<GwtGroupedNVPair> statusGridProvider = new ListDataProvider<GwtGroupedNVPair>();
    private EntryClassUi parent;

    @UiField
    Well statusWell;
    @UiField
    Button statusRefresh;
    @UiField
    CellTable<GwtGroupedNVPair> statusGrid = new CellTable<GwtGroupedNVPair>();

    public StatusPanelUi() {
        logger.log(Level.FINER, "Initializing StatusPanelUi...");
        initWidget(uiBinder.createAndBindUi(this));
        // Set text for buttons
        this.statusRefresh.setText(MSG.refresh());

        this.statusGrid.setRowStyles(new RowStyles<GwtGroupedNVPair>() {

            @Override
            public String getStyleNames(GwtGroupedNVPair row, int rowIndex) {
                if ("Cloud Services".equals(row.getName()) || "Connection Name".equals(row.getName())
                        || "Ethernet Settings".equals(row.getName()) || "Wireless Settings".equals(row.getName())
                        || "Cellular Settings".equals(row.getName()) || "Position Status".equals(row.getName())) {
                    return "rowHeader";
                } else {
                    return " ";
                }
            }
        });

        loadStatusTable(this.statusGrid, this.statusGridProvider);

        this.statusRefresh.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                loadStatusData();
            }
        });
    }

    // get current session from UI parent
    public void setSession(GwtSession gwtBSSession) {
        this.currentSession = gwtBSSession;
    }

    // create table layout
    public void loadStatusTable(CellTable<GwtGroupedNVPair> grid, ListDataProvider<GwtGroupedNVPair> dataProvider) {
        TextColumn<GwtGroupedNVPair> col1 = new TextColumn<GwtGroupedNVPair>() {

            @Override
            public String getValue(GwtGroupedNVPair object) {
                return String.valueOf(object.getName());
            }
        };
        col1.setCellStyleNames("status-table-row");
        grid.addColumn(col1);

        Column<GwtGroupedNVPair, SafeHtml> col2 = new Column<GwtGroupedNVPair, SafeHtml>(new SafeHtmlCell()) {

            @Override
            public SafeHtml getValue(GwtGroupedNVPair object) {
                return SafeHtmlUtils.fromTrustedString(String.valueOf(object.getValue()));
            }
        };

        col2.setCellStyleNames("status-table-row");
        grid.addColumn(col2);
        dataProvider.addDataDisplay(grid);
    }

    // fetch table data
    public void loadStatusData() {
        this.statusGridProvider.getList().clear();
        EntryClassUi.showWaitModal();
        this.gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

            @Override
            public void onFailure(Throwable ex) {
                EntryClassUi.hideWaitModal();
                FailureHandler.handle(ex);
            }

            @Override
            public void onSuccess(GwtXSRFToken token) {
                StatusPanelUi.this.gwtStatusService.getDeviceConfig(token,
                        StatusPanelUi.this.currentSession.isNetAdminAvailable(),
                        new AsyncCallback<ArrayList<GwtGroupedNVPair>>() {

                    @Override
                    public void onFailure(Throwable caught) {
                        FailureHandler.handle(caught);
                        StatusPanelUi.this.statusGridProvider.flush();
                        EntryClassUi.hideWaitModal();
                    }

                    @Override
                    public void onSuccess(ArrayList<GwtGroupedNVPair> result) {
                        String title = "cloudStatus";
                        StatusPanelUi.this.statusGridProvider.getList()
                                .add(new GwtGroupedNVPair(" ", msgs.getString(title), " "));

                        StatusPanelUi.this.parent.updateConnectionStatusImage(false);

                        int connectionNameIndex = 0;

                        for (GwtGroupedNVPair resultPair : result) {
                            if ("Connection Name".equals(resultPair.getName())
                                    && resultPair.getValue().endsWith("CloudService")) {
                                // done based on the idea that in the pairs data connection name is before connection
                                // status
                                GwtGroupedNVPair connectionStatus = result.get(connectionNameIndex + 1);

                                if ("Service Status".equals(connectionStatus.getName())
                                        && "CONNECTED".equals(connectionStatus.getValue())) {
                                    StatusPanelUi.this.parent.updateConnectionStatusImage(true);
                                } else {
                                    StatusPanelUi.this.parent.updateConnectionStatusImage(false);
                                }
                            }
                            connectionNameIndex++;

                            if (!title.equals(resultPair.getGroup())) {
                                title = resultPair.getGroup();
                                StatusPanelUi.this.statusGridProvider.getList()
                                        .add(new GwtGroupedNVPair(" ", msgs.getString(title), " "));
                            }
                            StatusPanelUi.this.statusGridProvider.getList().add(resultPair);
                        }
                        int size = StatusPanelUi.this.statusGridProvider.getList().size();
                        StatusPanelUi.this.statusGrid.setVisibleRange(0, size);
                        StatusPanelUi.this.statusGridProvider.flush();
                        EntryClassUi.hideWaitModal();
                    }
                });
            }
        });
    }

    public void setParent(EntryClassUi parent) {
        this.parent = parent;
    }
}
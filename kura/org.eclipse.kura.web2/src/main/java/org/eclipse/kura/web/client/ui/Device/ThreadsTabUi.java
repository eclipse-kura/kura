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
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.cellview.client.TextHeader;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;

public class ThreadsTabUi extends Composite implements Tab {

    private static ThreadsTabUiUiBinder uiBinder = GWT.create(ThreadsTabUiUiBinder.class);

    interface ThreadsTabUiUiBinder extends UiBinder<Widget, ThreadsTabUi> {
    }

    private static final Messages MSGS = GWT.create(Messages.class);

    private final GwtSecurityTokenServiceAsync gwtXSRFService = GWT.create(GwtSecurityTokenService.class);
    private final GwtDeviceServiceAsync gwtDeviceService = GWT.create(GwtDeviceService.class);

    @UiField
    CellTable<GwtGroupedNVPair> threadsGrid = new CellTable<>();
    private final ListDataProvider<GwtGroupedNVPair> threadsDataProvider = new ListDataProvider<>();

    public ThreadsTabUi() {
        initWidget(uiBinder.createAndBindUi(this));

        loadProfileTable(this.threadsGrid, this.threadsDataProvider);
    }

    private void loadProfileTable(CellTable<GwtGroupedNVPair> threadsGrid2,

    ListDataProvider<GwtGroupedNVPair> dataProvider) {

        TextColumn<GwtGroupedNVPair> col1 = new TextColumn<GwtGroupedNVPair>() {

            @Override
            public String getValue(GwtGroupedNVPair object) {
                return object.getName();
            }
        };
        col1.setCellStyleNames("status-table-row");
        TextHeader name = new TextHeader(MSGS.deviceThreadName());
        name.setHeaderStyleNames("rowHeader");
        threadsGrid2.addColumn(col1, name);

        TextColumn<GwtGroupedNVPair> col2 = new TextColumn<GwtGroupedNVPair>() {

            @Override
            public String getValue(GwtGroupedNVPair object) {
                return String.valueOf(object.getValue());
            }
        };
        col2.setCellStyleNames("status-table-row");
        TextHeader info = new TextHeader(MSGS.deviceThreadInfo());
        info.setHeaderStyleNames("rowHeader");
        threadsGrid2.addColumn(col2, info);

        dataProvider.addDataDisplay(threadsGrid2);
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
        this.threadsDataProvider.getList().clear();

        EntryClassUi.showWaitModal();
        this.gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

            @Override
            public void onFailure(Throwable ex) {
                EntryClassUi.hideWaitModal();
                FailureHandler.handle(ex);
            }

            @Override
            public void onSuccess(GwtXSRFToken token) {
                ThreadsTabUi.this.gwtDeviceService.findThreads(token, new AsyncCallback<ArrayList<GwtGroupedNVPair>>() {

                    @Override
                    public void onFailure(Throwable caught) {
                        EntryClassUi.hideWaitModal();
                        ThreadsTabUi.this.threadsDataProvider.getList().clear();
                        FailureHandler.handle(caught);
                        ThreadsTabUi.this.threadsDataProvider.flush();

                    }

                    @Override
                    public void onSuccess(ArrayList<GwtGroupedNVPair> result) {
                        for (GwtGroupedNVPair resultPair : result) {
                            ThreadsTabUi.this.threadsDataProvider.getList().add(resultPair);
                        }
                        int size = ThreadsTabUi.this.threadsDataProvider.getList().size();
                        ThreadsTabUi.this.threadsGrid.setVisibleRange(0, size);
                        ThreadsTabUi.this.threadsDataProvider.flush();
                        EntryClassUi.hideWaitModal();
                    }
                });
            }
        });
    }
}

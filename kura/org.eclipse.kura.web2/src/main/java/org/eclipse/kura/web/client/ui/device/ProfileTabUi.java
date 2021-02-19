/*******************************************************************************
 * Copyright (c) 2011, 2021 Eurotech and/or its affiliates and others
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

import java.util.List;

import org.eclipse.kura.web.client.messages.ValidationMessages;
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
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;

public class ProfileTabUi extends Composite implements Tab {

    private static final String DEV_INFO = "devInfo";

    private static ProfileTabUiUiBinder uiBinder = GWT.create(ProfileTabUiUiBinder.class);

    interface ProfileTabUiUiBinder extends UiBinder<Widget, ProfileTabUi> {
    }

    private static final ValidationMessages msgs = GWT.create(ValidationMessages.class);

    private final GwtSecurityTokenServiceAsync gwtXSRFService = GWT.create(GwtSecurityTokenService.class);
    private final GwtDeviceServiceAsync gwtDeviceService = GWT.create(GwtDeviceService.class);

    @UiField
    CellTable<GwtGroupedNVPair> profileGrid = new CellTable<>();
    private final ListDataProvider<GwtGroupedNVPair> profileDataProvider = new ListDataProvider<>();

    public ProfileTabUi() {
        initWidget(uiBinder.createAndBindUi(this));

        this.profileGrid.setRowStyles((row, rowIndex) -> row.getValue().contains("  ") ? "rowHeader" : " ");

        loadProfileTable(this.profileGrid, this.profileDataProvider);
    }

    private void loadProfileTable(CellTable<GwtGroupedNVPair> profileGrid2,
            ListDataProvider<GwtGroupedNVPair> dataProvider) {

        TextColumn<GwtGroupedNVPair> col1 = new TextColumn<GwtGroupedNVPair>() {

            @Override
            public String getValue(GwtGroupedNVPair object) {
                return msgs.getString(object.getName());
            }
        };
        col1.setCellStyleNames("status-table-row");
        profileGrid2.addColumn(col1);

        TextColumn<GwtGroupedNVPair> col2 = new TextColumn<GwtGroupedNVPair>() {

            @Override
            public String getValue(GwtGroupedNVPair object) {
                return String.valueOf(object.getValue());
            }
        };
        col2.setCellStyleNames("status-table-row");
        profileGrid2.addColumn(col2);

        dataProvider.addDataDisplay(profileGrid2);
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
        this.profileDataProvider.getList().clear();

        EntryClassUi.showWaitModal();
        this.gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

            @Override
            public void onFailure(Throwable ex) {
                EntryClassUi.hideWaitModal();
                FailureHandler.handle(ex);
            }

            @Override
            public void onSuccess(GwtXSRFToken token) {
                ProfileTabUi.this.gwtDeviceService.findDeviceConfiguration(token,
                        new AsyncCallback<List<GwtGroupedNVPair>>() {

                            @Override
                            public void onFailure(Throwable caught) {
                                EntryClassUi.hideWaitModal();
                                ProfileTabUi.this.profileDataProvider.getList().clear();
                                FailureHandler.handle(caught);
                                ProfileTabUi.this.profileDataProvider.flush();

                            }

                            @Override
                            public void onSuccess(List<GwtGroupedNVPair> result) {
                                String oldGroup = DEV_INFO;
                                ProfileTabUi.this.profileDataProvider.getList()
                                        .add(new GwtGroupedNVPair(DEV_INFO, DEV_INFO, "  "));
                                for (GwtGroupedNVPair resultPair : result) {
                                    if (!oldGroup.equals(resultPair.getGroup())) {
                                        ProfileTabUi.this.profileDataProvider.getList().add(new GwtGroupedNVPair(
                                                resultPair.getGroup(), resultPair.getGroup(), "  "));
                                        oldGroup = resultPair.getGroup();
                                    }
                                    ProfileTabUi.this.profileDataProvider.getList().add(resultPair);
                                }
                                int size = ProfileTabUi.this.profileDataProvider.getList().size();
                                ProfileTabUi.this.profileGrid.setVisibleRange(0, size);
                                ProfileTabUi.this.profileDataProvider.flush();
                                EntryClassUi.hideWaitModal();
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

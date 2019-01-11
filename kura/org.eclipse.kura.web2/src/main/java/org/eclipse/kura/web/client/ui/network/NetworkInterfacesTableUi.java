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
package org.eclipse.kura.web.client.ui.network;

import java.util.Comparator;
import java.util.List;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.ui.EntryClassUi;
import org.eclipse.kura.web.client.util.FailureHandler;
import org.eclipse.kura.web.shared.model.GwtNetInterfaceConfig;
import org.eclipse.kura.web.shared.model.GwtSession;
import org.eclipse.kura.web.shared.service.GwtNetworkService;
import org.eclipse.kura.web.shared.service.GwtNetworkServiceAsync;
import org.gwtbootstrap3.client.ui.Alert;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.Modal;
import org.gwtbootstrap3.client.ui.ModalBody;
import org.gwtbootstrap3.client.ui.ModalFooter;
import org.gwtbootstrap3.client.ui.gwt.CellTable;
import org.gwtbootstrap3.client.ui.html.Span;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.ColumnSortEvent;
import com.google.gwt.user.cellview.client.ColumnSortEvent.ListHandler;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;

public class NetworkInterfacesTableUi extends Composite {

    private static NetworkInterfacesTableUiUiBinder uiBinder = GWT.create(NetworkInterfacesTableUiUiBinder.class);

    interface NetworkInterfacesTableUiUiBinder extends UiBinder<Widget, NetworkInterfacesTableUi> {
    }

    private static final Messages MSGS = GWT.create(Messages.class);
    private static final String SELECTED_INTERFACE = "ui.selected.interface";
    private final GwtNetworkServiceAsync gwtNetworkService = GWT.create(GwtNetworkService.class);

    GwtSession session;
    NetworkTabsUi tabs;
    GwtNetInterfaceConfig selection;

    @UiField
    Alert notification;
    @UiField

    CellTable<GwtNetInterfaceConfig> interfacesGrid = new CellTable<GwtNetInterfaceConfig>();

    private final ListDataProvider<GwtNetInterfaceConfig> interfacesProvider = new ListDataProvider<GwtNetInterfaceConfig>();
    final SingleSelectionModel<GwtNetInterfaceConfig> selectionModel = new SingleSelectionModel<GwtNetInterfaceConfig>();
    TextColumn<GwtNetInterfaceConfig> col1;

    public NetworkInterfacesTableUi(GwtSession s, NetworkTabsUi tabsPanel) {
        initWidget(uiBinder.createAndBindUi(this));
        this.session = s;
        this.tabs = tabsPanel;
        initTable();
        loadData();

        this.selectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {

            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                if (NetworkInterfacesTableUi.this.selection == NetworkInterfacesTableUi.this.selectionModel
                        .getSelectedObject()) {
                    return;
                }
                if (NetworkInterfacesTableUi.this.selection != null && NetworkInterfacesTableUi.this.tabs.isDirty()) {
                    // there was an earlier selection, changes have not
                    // been saved
                    final Modal confirm = new Modal();
                    ModalBody confirmBody = new ModalBody();
                    ModalFooter confirmFooter = new ModalFooter();

                    confirm.setTitle(MSGS.confirm());
                    confirmBody.add(new Span(MSGS.deviceConfigDirty()));
                    Button yes = new Button(MSGS.yesButton(), new ClickHandler() {

                        @Override
                        public void onClick(ClickEvent event) {
                            confirm.hide();
                            NetworkInterfacesTableUi.this.selection = NetworkInterfacesTableUi.this.selectionModel
                                    .getSelectedObject();
                            if (NetworkInterfacesTableUi.this.selection != null) {
                                NetworkInterfacesTableUi.this.session.set(SELECTED_INTERFACE,
                                        NetworkInterfacesTableUi.this.selection.getName());
                                NetworkInterfacesTableUi.this.tabs
                                        .setNetInterface(NetworkInterfacesTableUi.this.selection);
                                NetworkInterfacesTableUi.this.tabs.setDirty(false);
                            }
                        }
                    });

                    Button no = new Button(MSGS.noButton(), new ClickHandler() {

                        @Override
                        public void onClick(ClickEvent event) {
                            confirm.hide();
                            NetworkInterfacesTableUi.this.selectionModel
                                    .setSelected(NetworkInterfacesTableUi.this.selection, true);
                        }
                    });
                    confirmFooter.add(no);
                    confirmFooter.add(yes);
                    confirm.add(confirmBody);
                    confirm.add(confirmFooter);
                    confirm.show();
                    no.setFocus(true);
                } else {
                    // no unsaved changes
                    NetworkInterfacesTableUi.this.selection = NetworkInterfacesTableUi.this.selectionModel
                            .getSelectedObject();
                    if (NetworkInterfacesTableUi.this.selection != null) {
                        NetworkInterfacesTableUi.this.session.set(SELECTED_INTERFACE,
                                NetworkInterfacesTableUi.this.selection.getName());
                        NetworkInterfacesTableUi.this.tabs.setNetInterface(NetworkInterfacesTableUi.this.selection);
                    }
                }
            }
        });

    }

    public void refresh() {
        if (this.selection != null && this.tabs.isDirty()) {
            // there was an earlier selection, changes have not been saved
            final Modal confirm = new Modal();
            ModalBody confirmBody = new ModalBody();
            ModalFooter confirmFooter = new ModalFooter();

            confirm.setTitle(MSGS.confirm());
            confirmBody.add(new Span(MSGS.deviceConfigDirty()));
            confirmFooter.add(new Button(MSGS.yesButton(), new ClickHandler() {

                @Override
                public void onClick(ClickEvent event) {
                    confirm.hide();
                    // selection = null;
                    NetworkInterfacesTableUi.this.tabs.setDirty(false);
                    loadData();
                }
            }));

            confirmFooter.add(new Button(MSGS.noButton(), new ClickHandler() {

                @Override
                public void onClick(ClickEvent event) {
                    confirm.hide();
                }
            }));
            confirm.add(confirmBody);
            confirm.add(confirmFooter);
            confirm.show();
        } else {
            this.tabs.setDirty(false);
            loadData();
        }
    }

    /*--------------------------------------
     * -------Private methods---------------
     --------------------------------------*/

    private void initTable() {
        this.col1 = new TextColumn<GwtNetInterfaceConfig>() {

            @Override
            public String getValue(GwtNetInterfaceConfig object) {
                return object.getName();
            }
        };
        this.col1.setCellStyleNames("status-table-row");
        this.col1.setSortable(true);
        this.interfacesGrid.addColumn(this.col1, MSGS.netInterfaceName());

        this.interfacesProvider.addDataDisplay(this.interfacesGrid);
        this.interfacesGrid.setSelectionModel(this.selectionModel);

        this.interfacesGrid.getColumnSortList().push(this.col1);
    }

    private void loadData() {
        EntryClassUi.showWaitModal();
        this.interfacesProvider.getList().clear();
        this.gwtNetworkService.findNetInterfaceConfigurations(new AsyncCallback<List<GwtNetInterfaceConfig>>() {

            @Override
            public void onFailure(Throwable caught) {
                EntryClassUi.hideWaitModal();
                FailureHandler.handle(caught);
            }

            @Override
            public void onSuccess(List<GwtNetInterfaceConfig> result) {
                ListHandler<GwtNetInterfaceConfig> columnSortHandler = new ListHandler<GwtNetInterfaceConfig>(
                        NetworkInterfacesTableUi.this.interfacesProvider.getList());
                columnSortHandler.setComparator(NetworkInterfacesTableUi.this.col1,
                        new Comparator<GwtNetInterfaceConfig>() {

                    @Override
                    public int compare(GwtNetInterfaceConfig o1, GwtNetInterfaceConfig o2) {
                        if (o1 == o2) {
                            return 0;
                        }

                        // Compare the name columns.
                        if (o1 != null) {
                            return o2 != null ? compareFromName(o1.getName(), o2.getName()) : 1;
                        }
                        return -1;
                    }
                });
                NetworkInterfacesTableUi.this.interfacesGrid.addColumnSortHandler(columnSortHandler);

                NetworkInterfacesTableUi.this.interfacesProvider.getList().addAll(result);
                ColumnSortEvent.fire(NetworkInterfacesTableUi.this.interfacesGrid,
                        NetworkInterfacesTableUi.this.interfacesGrid.getColumnSortList());
                NetworkInterfacesTableUi.this.interfacesProvider.flush();

                if (!NetworkInterfacesTableUi.this.interfacesProvider.getList().isEmpty()) {
                    NetworkInterfacesTableUi.this.interfacesGrid.setVisible(true);
                    NetworkInterfacesTableUi.this.notification.setVisible(false);

                    // Check session to see if interface was already chosen
                    // Must select based on name, as keys in SelectionModel change across RPC calls
                    String sessionSelection = (String) NetworkInterfacesTableUi.this.session.get(SELECTED_INTERFACE);
                    if (sessionSelection != null) {
                        for (GwtNetInterfaceConfig gc : NetworkInterfacesTableUi.this.interfacesProvider.getList()) {
                            if (gc.getName().equals(sessionSelection)) {
                                NetworkInterfacesTableUi.this.selectionModel.setSelected(gc, true);
                                break;
                            }
                        }
                    } else {
                        NetworkInterfacesTableUi.this.selectionModel
                                .setSelected(NetworkInterfacesTableUi.this.interfacesProvider.getList().get(0), true);
                    }

                } else {
                    NetworkInterfacesTableUi.this.interfacesGrid.setVisible(false);
                    NetworkInterfacesTableUi.this.notification.setVisible(true);
                    NetworkInterfacesTableUi.this.notification.setText(MSGS.netTableNoInterfaces());
                }
                EntryClassUi.hideWaitModal();
            }
        });
    }

    private int compareFromName(String name1, String name2) {
        int result = 1;
        if (name1.equals(name2)) {
            result = 0;
        }
        if ("lo".equals(name1)) {
            result = -1;
        }
        if (name1.startsWith("eth") && !"lo".equals(name2)) {
            if (name2.startsWith("eth")) {
                // compare eths
                result = name1.compareTo(name2);
            } else {
                result = -1;
            }
        }
        if (name1.startsWith("wlan") && !name2.startsWith("lo") && !name2.startsWith("eth")) {
            if (name2.startsWith("wlan")) {
                // compare wlans
                result = name1.compareTo(name2);
            } else {
                result = -1;
            }
        }
        if (name1.startsWith("ppp") && !name2.startsWith("wlan") && !name2.startsWith("lo")
                && !name2.startsWith("eth")) {
            if (name2.startsWith("ppp")) {
                // compare ppps
                result = name1.compareTo(name2);
            } else {
                result = -1;
            }
        }
        return result;
    }
}

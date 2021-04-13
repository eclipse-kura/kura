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
package org.eclipse.kura.web.client.ui.security;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.ui.AlertDialog;
import org.eclipse.kura.web.client.ui.AlertDialog.ConfirmListener;
import org.eclipse.kura.web.client.ui.ServicesUi;
import org.eclipse.kura.web.client.ui.Tab;
import org.eclipse.kura.web.client.util.PidTextBox;
import org.eclipse.kura.web.client.util.request.RequestQueue;
import org.eclipse.kura.web.shared.model.GwtComponentInstanceInfo;
import org.eclipse.kura.web.shared.service.GwtComponentService;
import org.eclipse.kura.web.shared.service.GwtComponentServiceAsync;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenService;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenServiceAsync;
import org.eclipse.kura.web.shared.service.GwtSslManagerService;
import org.eclipse.kura.web.shared.service.GwtSslManagerServiceAsync;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.ListBox;
import org.gwtbootstrap3.client.ui.Modal;
import org.gwtbootstrap3.client.ui.Panel;
import org.gwtbootstrap3.client.ui.PanelHeader;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SingleSelectionModel;

public class SslManagerServicesUi extends Composite implements Tab {

    private static SslManagerServicesUiUiBinder uiBinder = GWT.create(SslManagerServicesUiUiBinder.class);

    interface SslManagerServicesUiUiBinder extends UiBinder<Widget, SslManagerServicesUi> {
    }

    private final GwtSecurityTokenServiceAsync gwtSecurityTokenService = GWT.create(GwtSecurityTokenService.class);
    private final GwtSslManagerServiceAsync gwtSslServiceAsync = GWT.create(GwtSslManagerService.class);
    private final GwtComponentServiceAsync gwtComponentService = GWT.create(GwtComponentService.class);

    private static final Messages MSGS = GWT.create(Messages.class);

    @UiField
    Button newServiceButton;
    @UiField
    Button deleteButton;

    @UiField
    Modal newServiceModal;
    @UiField
    ListBox sslManagerServiceFactoriesList;
    @UiField
    PidTextBox serviceName;
    @UiField
    Button buttonNewServiceCancel;
    @UiField
    Button buttonNewServiceApply;

    @UiField
    Panel configurationArea;
    @UiField
    PanelHeader contentPanelHeader;
    @UiField
    Panel sslManagerServicesMgmtPanel;

    @UiField
    AlertDialog confirmDialog;

    @UiField
    Label emptyListLabel;
    @UiField
    CellTable<GwtComponentInstanceInfo> sslManagerServicesListTable;

    private Optional<GwtComponentInstanceInfo> lastSelectedObject = Optional.empty();

    private final SingleSelectionModel<GwtComponentInstanceInfo> selectionModel = new SingleSelectionModel<>();
    private final ListDataProvider<GwtComponentInstanceInfo> dataProvider = new ListDataProvider<>();

    private Set<String> allTrackedPids;

    public SslManagerServicesUi() {
        initWidget(uiBinder.createAndBindUi(this));

        initTable();
        initButtonBar();
        initNewServiceModal();
    }

    @Override
    public void refresh() {
        clear();

        RequestQueue.submit(c -> this.gwtSslServiceAsync.listSslManagerServiceInstances(c.callback(result -> {

            this.dataProvider.getList().addAll(result);
            this.emptyListLabel.setVisible(result.isEmpty());
            this.sslManagerServicesListTable.setVisible(!result.isEmpty());

        })));

        RequestQueue.submit(
                c -> this.gwtSecurityTokenService.generateSecurityToken(c.callback(token -> this.gwtComponentService
                        .findTrackedPids(token, c.callback(pids -> this.allTrackedPids = new HashSet<>(pids))))));

        RequestQueue.submit(c -> this.gwtSslServiceAsync.listSslManagerServiceFactoryPids(c.callback(result -> {
            this.sslManagerServiceFactoriesList.clear();

            for (final String factoryPid : result) {
                this.sslManagerServiceFactoriesList.addItem(factoryPid);
            }
        })));
    }

    private void initTable() {
        TextColumn<GwtComponentInstanceInfo> col1 = new TextColumn<GwtComponentInstanceInfo>() {

            @Override
            public String getValue(GwtComponentInstanceInfo object) {
                return object.getPid();
            }
        };
        this.sslManagerServicesListTable.addColumn(col1, MSGS.servicePidLabel());

        TextColumn<GwtComponentInstanceInfo> col2 = new TextColumn<GwtComponentInstanceInfo>() {

            @Override
            public String getValue(GwtComponentInstanceInfo object) {
                return object.getFactoryPid().orElse("Singleton Component");
            }
        };
        this.sslManagerServicesListTable.addColumn(col2, MSGS.factoryPidLabel());

        this.selectionModel.addSelectionChangeHandler(e -> {

            final Optional<ServicesUi> configurationUi = getConfigurationUi();

            if (configurationUi.isPresent()) {

                if (Optional.ofNullable(this.selectionModel.getSelectedObject()).equals(lastSelectedObject)) {
                    return;
                }

                final ServicesUi currentConfig = configurationUi.get();

                if (currentConfig.isDirty() && this.lastSelectedObject.isPresent()) {
                    confirmDialog.show(MSGS.deviceConfigDirty(), ok -> {
                        if (ok) {
                            updateSelection();
                        } else {
                            this.selectionModel.setSelected(this.lastSelectedObject.get(), true);
                        }
                    });
                    return;
                }
            }

            updateSelection();
        });

        this.dataProvider.addDataDisplay(this.sslManagerServicesListTable);
        this.sslManagerServicesListTable.setSelectionModel(this.selectionModel);
    }

    private void updateSelection() {
        final GwtComponentInstanceInfo selected = this.selectionModel.getSelectedObject();

        this.deleteButton.setEnabled(selected != null && selected.getFactoryPid().isPresent());

        this.sslManagerServicesMgmtPanel.clear();

        if (selected != null) {
            RequestQueue.submit(
                    c -> this.gwtSecurityTokenService.generateSecurityToken(c.callback(token -> this.gwtSslServiceAsync
                            .getSslManagerServiceConfiguration(token, selected.getPid(), c.callback(result -> {
                                this.contentPanelHeader.setText(
                                        selected.getPid() + selected.getFactoryPid().map(p -> " - " + p).orElse(""));

                                final ServicesUi servicesUi = new ServicesUi(result);

                                servicesUi.setDeleteButtonVisible(false);

                                this.sslManagerServicesMgmtPanel.add(servicesUi);
                            })))));
        }

        this.deleteButton.setEnabled(selected != null);

        this.lastSelectedObject = Optional.ofNullable(selected);
    }

    @Override
    public boolean isDirty() {
        return this.getConfigurationUi().map(ServicesUi::isDirty).orElse(false);
    }

    private void initButtonBar() {

        this.newServiceButton.addClickHandler(event -> {
            this.serviceName.setText("");
            this.newServiceModal.show();
        });

        this.deleteButton.addClickHandler(event -> {
            final GwtComponentInstanceInfo info = this.selectionModel.getSelectedObject();

            if (info == null) {
                return;
            }

            this.confirmDialog.show(MSGS.deleteWarning(), ok -> {
                if (ok) {
                    deleteService(info.getPid());
                }
            });
        });
    }

    private void deleteService(final String pid) {
        RequestQueue.submit(
                c -> this.gwtSecurityTokenService.generateSecurityToken(c.callback(token -> this.gwtSslServiceAsync
                        .deleteSslManagerService(token, pid, c.callback(ok -> this.refresh())))));
    }

    private Optional<ServicesUi> getConfigurationUi() {
        if (this.sslManagerServicesMgmtPanel.getWidgetCount() == 0) {
            return Optional.empty();
        }

        return Optional.of((ServicesUi) this.sslManagerServicesMgmtPanel.getWidget(0));
    }

    private void initNewServiceModal() {

        this.buttonNewServiceApply.addClickHandler(event -> {

            final String pid = this.serviceName.getPid();

            if (pid == null) {
                return;
            }

            final String factoryPid = this.sslManagerServiceFactoriesList.getSelectedValue();

            if (factoryPid == null) {
                return;
            }

            if (this.allTrackedPids.contains(pid)) {
                this.confirmDialog.show(MSGS.wiresComponentNameAlreadyUsed(pid), AlertDialog.Severity.ALERT,
                        (ConfirmListener) null);
                return;
            }

            this.newServiceModal.hide();

            RequestQueue.submit(c -> this.gwtSecurityTokenService.generateSecurityToken(
                    c.callback(token -> this.gwtSslServiceAsync.createSslManagerServiceFactoryConfiguration(token, pid,
                            factoryPid, c.callback(ok -> this.refresh())))));
        });
    }

    @Override
    public void setDirty(boolean flag) {
        getConfigurationUi().ifPresent(c -> c.setDirty(flag));
    }

    @Override
    public boolean isValid() {

        return getConfigurationUi().map(ServicesUi::isValid).orElse(true);
    }

    @Override
    public void clear() {
        this.dataProvider.getList().clear();
        this.selectionModel.clear();
        this.lastSelectedObject = Optional.empty();
        this.sslManagerServicesMgmtPanel.clear();
        this.emptyListLabel.setVisible(true);
        this.sslManagerServicesListTable.setVisible(false);
        this.contentPanelHeader.setText("");
        this.deleteButton.setEnabled(false);
    }

}

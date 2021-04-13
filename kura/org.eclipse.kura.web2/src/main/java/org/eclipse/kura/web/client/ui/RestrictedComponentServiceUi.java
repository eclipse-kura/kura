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
package org.eclipse.kura.web.client.ui;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.ui.AlertDialog.ConfirmListener;
import org.eclipse.kura.web.client.util.PidTextBox;
import org.eclipse.kura.web.client.util.request.RequestQueue;
import org.eclipse.kura.web.shared.model.GwtComponentInstanceInfo;
import org.eclipse.kura.web.shared.model.GwtConfigComponent;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.service.GwtComponentService;
import org.eclipse.kura.web.shared.service.GwtComponentServiceAsync;
import org.eclipse.kura.web.shared.service.GwtRestrictedComponentServiceAsync;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenService;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenServiceAsync;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.FormLabel;
import org.gwtbootstrap3.client.ui.ListBox;
import org.gwtbootstrap3.client.ui.Modal;
import org.gwtbootstrap3.client.ui.Panel;
import org.gwtbootstrap3.client.ui.PanelHeader;
import org.gwtbootstrap3.client.ui.html.Paragraph;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SingleSelectionModel;

public class RestrictedComponentServiceUi extends Composite implements Tab {

    private static RestrictedComponentServiceUiUiBinder uiBinder = GWT
            .create(RestrictedComponentServiceUiUiBinder.class);

    interface RestrictedComponentServiceUiUiBinder extends UiBinder<Widget, RestrictedComponentServiceUi> {
    }

    private final GwtSecurityTokenServiceAsync gwtSecurityTokenService = GWT.create(GwtSecurityTokenService.class);
    private final GwtComponentServiceAsync gwtComponentService = GWT.create(GwtComponentService.class);

    private static final Messages MSGS = GWT.create(Messages.class);
    private GwtRestrictedComponentServiceAsync backend;

    @UiField
    Button newServiceButton;
    @UiField
    Button deleteButton;

    @UiField
    Modal newServiceModal;
    @UiField
    ListBox factoriesList;
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
    Panel mgmtPanel;

    @UiField
    AlertDialog confirmDialog;

    @UiField
    Label emptyListLabel;
    @UiField
    CellTable<GwtComponentInstanceInfo> table;

    @UiField
    Paragraph introParagraph;
    @UiField
    Paragraph newServiceModalIntro;

    @UiField
    FormLabel factoriesListLabel;
    @UiField
    FormLabel serviceNameLabel;

    private Optional<GwtComponentInstanceInfo> lastSelectedObject = Optional.empty();

    private final SingleSelectionModel<GwtComponentInstanceInfo> selectionModel = new SingleSelectionModel<>();
    private final ListDataProvider<GwtComponentInstanceInfo> dataProvider = new ListDataProvider<>();

    private Set<String> allTrackedPids;

    public RestrictedComponentServiceUi() {
        initWidget(uiBinder.createAndBindUi(this));

        initTable();
        initButtonBar();
        initNewServiceModal();
    }

    public void setBackend(final GwtRestrictedComponentServiceAsync backend) {
        this.backend = backend;
    }

    public void setIntro(final String intro) {
        introParagraph.setText(intro);
    }

    public void setEmptyLabelText(final String text) {
        this.emptyListLabel.setText(text);
    }

    public void setNewServiceModalTitle(final String title) {
        this.newServiceModal.setTitle(title);
    }

    public void setNewServiceModalIntro(final String intro) {
        this.newServiceModalIntro.setText(intro);
    }

    public void setIds(final String id) {
        this.factoriesListLabel.setFor(id + "FactoriesList");
        this.factoriesList.setId(id + "FactoriesList");

        this.serviceNameLabel.setFor(id + "ServiceName");
        this.serviceName.setId(id + "ServiceName");

        this.confirmDialog.setId(id + "ConfirmDialog");
    }

    @Override
    public void refresh() {
        clear();

        RequestQueue.submit(c -> this.backend.listServiceInstances(c.callback(result -> {

            this.dataProvider.getList().addAll(result);
            this.emptyListLabel.setVisible(result.isEmpty());
            this.table.setVisible(!result.isEmpty());

        })));

        RequestQueue.submit(
                c -> this.gwtSecurityTokenService.generateSecurityToken(c.callback(token -> this.gwtComponentService
                        .findTrackedPids(token, c.callback(pids -> this.allTrackedPids = new HashSet<>(pids))))));

        RequestQueue.submit(c -> this.backend.listFactoryPids(c.callback(result -> {
            this.factoriesList.clear();

            for (final String factoryPid : result) {
                this.factoriesList.addItem(factoryPid);
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
        this.table.addColumn(col1, MSGS.servicePidLabel());

        TextColumn<GwtComponentInstanceInfo> col2 = new TextColumn<GwtComponentInstanceInfo>() {

            @Override
            public String getValue(GwtComponentInstanceInfo object) {
                return object.getFactoryPid().orElse("Singleton Component");
            }
        };
        this.table.addColumn(col2, MSGS.factoryPidLabel());

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

        this.dataProvider.addDataDisplay(this.table);
        this.table.setSelectionModel(this.selectionModel);
    }

    private void updateSelection() {
        final GwtComponentInstanceInfo selected = this.selectionModel.getSelectedObject();

        this.deleteButton.setEnabled(selected != null && selected.getFactoryPid().isPresent());

        this.mgmtPanel.clear();
        this.configurationArea.setVisible(selected != null);

        if (selected != null) {
            RequestQueue.submit(c -> this.gwtSecurityTokenService.generateSecurityToken(
                    c.callback(token -> this.backend.getConfiguration(token, selected.getPid(), c.callback(result -> {
                        this.contentPanelHeader
                                .setText(selected.getPid() + selected.getFactoryPid().map(p -> " - " + p).orElse(""));

                        final ServicesUi servicesUi = new ServicesUi(result);
                        servicesUi.setBackend(new ServicesUi.Backend() {

                            @Override
                            public void updateComponentConfiguration(GwtXSRFToken token, GwtConfigComponent component,
                                    AsyncCallback<Void> callback) {
                                RestrictedComponentServiceUi.this.backend.updateConfiguration(token, component,
                                        callback);

                            }

                            @Override
                            public void deleteFactoryConfiguration(GwtXSRFToken token, String pid,
                                    AsyncCallback<Void> callback) {
                                RestrictedComponentServiceUi.this.backend.deleteFactoryConfiguration(token, pid,
                                        callback);
                            }
                        });

                        servicesUi.setDeleteButtonVisible(false);

                        this.mgmtPanel.add(servicesUi);
                        this.configurationArea.setVisible(true);
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
        RequestQueue.submit(c -> this.gwtSecurityTokenService.generateSecurityToken(c.callback(
                token -> this.backend.deleteFactoryConfiguration(token, pid, c.callback(ok -> this.refresh())))));
    }

    private Optional<ServicesUi> getConfigurationUi() {
        if (this.mgmtPanel.getWidgetCount() == 0) {
            return Optional.empty();
        }

        return Optional.of((ServicesUi) this.mgmtPanel.getWidget(0));
    }

    private void initNewServiceModal() {

        this.buttonNewServiceApply.addClickHandler(event -> {

            final String pid = this.serviceName.getPid();

            if (pid == null) {
                return;
            }

            final String factoryPid = this.factoriesList.getSelectedValue();

            if (factoryPid == null) {
                return;
            }

            if (this.allTrackedPids.contains(pid)) {
                this.confirmDialog.show(MSGS.wiresComponentNameAlreadyUsed(pid), AlertDialog.Severity.ALERT,
                        (ConfirmListener) null);
                return;
            }

            this.newServiceModal.hide();

            RequestQueue.submit(c -> this.gwtSecurityTokenService.generateSecurityToken(c.callback(token -> this.backend
                    .createFactoryConfiguration(token, pid, factoryPid, c.callback(ok -> this.refresh())))));
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
        this.mgmtPanel.clear();
        this.configurationArea.setVisible(false);
        this.emptyListLabel.setVisible(true);
        this.table.setVisible(false);
        this.contentPanelHeader.setText("");
        this.deleteButton.setEnabled(false);
    }

}

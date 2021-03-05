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
package org.eclipse.kura.web.client.ui.network;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.ui.AlertDialog;
import org.eclipse.kura.web.client.ui.EntryClassUi;
import org.eclipse.kura.web.client.util.FailureHandler;
import org.eclipse.kura.web.shared.model.GwtNetInterfaceConfig;
import org.eclipse.kura.web.shared.model.GwtSession;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.service.GwtNetworkService;
import org.eclipse.kura.web.shared.service.GwtNetworkServiceAsync;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenService;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenServiceAsync;
import org.gwtbootstrap3.client.ui.Alert;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.Modal;
import org.gwtbootstrap3.client.ui.html.Text;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

public class NetworkButtonBarUi extends Composite {

    private static NetworkButtonBarUiUiBinder uiBinder = GWT.create(NetworkButtonBarUiUiBinder.class);
    private static final Logger logger = Logger.getLogger(NetworkButtonBarUi.class.getSimpleName());

    interface NetworkButtonBarUiUiBinder extends UiBinder<Widget, NetworkButtonBarUi> {
    }

    private final GwtSecurityTokenServiceAsync gwtXSRFService = GWT.create(GwtSecurityTokenService.class);
    private final GwtNetworkServiceAsync gwtNetworkService = GWT.create(GwtNetworkService.class);

    private static final Messages MSGS = GWT.create(Messages.class);

    GwtSession session;
    NetworkInterfacesTableUi table;
    NetworkTabsUi tabs;

    @UiField
    Button apply;
    @UiField
    Button refresh;

    @UiField
    Modal incompleteFieldsModal;
    @UiField
    Alert incompleteFields;
    @UiField
    Text incompleteFieldsText;
    @UiField
    AlertDialog alertDialog;

    public NetworkButtonBarUi(GwtSession currentSession, NetworkTabsUi tabsPanel, NetworkInterfacesTableUi interfaces) {
        initWidget(uiBinder.createAndBindUi(this));
        this.session = currentSession;
        this.table = interfaces;
        this.tabs = tabsPanel;
        initButtons();
        initModal();
    }

    private void initButtons() {
        initApplyButton();
        initRefreshButton();
    }

    public void setApplyButtonDirty(boolean dirty) {
        this.apply.setEnabled(dirty);
    }

    protected void initRefreshButton() {
        // Refresh Button
        this.refresh.setText(MSGS.refresh());
        this.refresh.addClickHandler(event -> {
            NetworkButtonBarUi.this.table.refresh();
        });
    }

    protected void initApplyButton() {
        // Apply Button
        this.apply.setText(MSGS.apply());
        this.apply.setEnabled(false);
        this.apply.addClickHandler(event -> {
            if (!NetworkButtonBarUi.this.tabs.visibleTabs.isEmpty() && NetworkButtonBarUi.this.tabs.isValid()) {
                GwtNetInterfaceConfig prevNetIf = NetworkButtonBarUi.this.table.selectionModel.getSelectedObject();
                final GwtNetInterfaceConfig updatedNetIf = NetworkButtonBarUi.this.tabs.getUpdatedInterface();

                // submit updated netInterfaceConfig and priorities
                if (prevNetIf != null && prevNetIf.equals(updatedNetIf)) {
                    NetworkButtonBarUi.this.table.refresh();
                    NetworkButtonBarUi.this.apply.setEnabled(false);
                } else {
                    alertDialog.show(MSGS.confirm(), MSGS.netConfigChangeConfirm(), AlertDialog.Severity.INFO,
                            confirmed -> {
                                if (confirmed) {
                                    EntryClassUi.showWaitModal();
                                    updateNetConfiguration(updatedNetIf);
                                }
                            }, MSGS.netConfigConsoleUnavailable(), MSGS.netConfigURLChange());

                }
            } else {
                logger.log(Level.FINER, MSGS.information() + ": " + MSGS.deviceConfigError());
                NetworkButtonBarUi.this.incompleteFieldsModal.show();
            }
        });
    }

    private void updateNetConfiguration(final GwtNetInterfaceConfig updatedNetIf) {
        this.tabs.setDirty(false);
        this.gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

            @Override
            public void onFailure(Throwable ex) {
                EntryClassUi.hideWaitModal();
                FailureHandler.handle(ex, NetworkButtonBarUi.class.getSimpleName());
            }

            @Override
            public void onSuccess(GwtXSRFToken token) {
                NetworkButtonBarUi.this.gwtNetworkService.updateNetInterfaceConfigurations(token, updatedNetIf,
                        new AsyncCallback<Void>() {

                            @Override
                            public void onFailure(Throwable ex) {
                                EntryClassUi.hideWaitModal();
                                FailureHandler.handle(ex, NetworkButtonBarUi.class.getSimpleName());
                            }

                            @Override
                            public void onSuccess(Void result) {
                                EntryClassUi.hideWaitModal();
                                NetworkButtonBarUi.this.tabs.setDirty(false);
                                NetworkButtonBarUi.this.table.refresh();
                                NetworkButtonBarUi.this.tabs.refresh();
                                NetworkButtonBarUi.this.apply.setEnabled(false);
                            }

                        });
            }

        });
    }

    private void initModal() {
        this.incompleteFieldsModal.setTitle(MSGS.warning());
        this.incompleteFieldsText.setText(MSGS.formWithErrorsOrIncomplete());
    }

}

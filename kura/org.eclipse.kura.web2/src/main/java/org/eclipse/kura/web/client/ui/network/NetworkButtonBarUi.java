/*******************************************************************************
 * Copyright (c) 2011, 2018 Eurotech and/or its affiliates
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

import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.ui.EntryClassUi;
import org.eclipse.kura.web.client.util.FailureHandler;
import org.eclipse.kura.web.shared.model.GwtNetIfConfigMode;
import org.eclipse.kura.web.shared.model.GwtNetInterfaceConfig;
import org.eclipse.kura.web.shared.model.GwtSession;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.service.GwtNetworkService;
import org.eclipse.kura.web.shared.service.GwtNetworkServiceAsync;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenService;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenServiceAsync;
import org.gwtbootstrap3.client.ui.Alert;
import org.gwtbootstrap3.client.ui.AnchorButton;
import org.gwtbootstrap3.client.ui.Modal;
import org.gwtbootstrap3.client.ui.html.Text;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

public class NetworkButtonBarUi extends Composite {

    private static final String IPV4_MODE_MANUAL_NAME = GwtNetIfConfigMode.netIPv4ConfigModeManual.name();

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
    AnchorButton apply;
    @UiField
    AnchorButton refresh;

    @UiField
    Modal incompleteFieldsModal;
    @UiField
    Alert incompleteFields;
    @UiField
    Text incompleteFieldsText;

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

        this.table.interfacesGrid.getSelectionModel()
                .addSelectionChangeHandler(event -> NetworkButtonBarUi.this.apply.setEnabled(true));

        // TODO ?? how to detect changes
    }

    protected void initRefreshButton() {
        // Refresh Button
        this.refresh.setText(MSGS.refresh());
        this.refresh.addClickHandler(event -> {
            NetworkButtonBarUi.this.table.refresh();
            NetworkButtonBarUi.this.tabs.setDirty(false);
            NetworkButtonBarUi.this.tabs.refresh();
            NetworkButtonBarUi.this.tabs.adjustInterfaceTabs();
        });
    }

    protected void initApplyButton() {
        // Apply Button
        this.apply.setText(MSGS.apply());
        this.apply.addClickHandler(event -> {
            if (!NetworkButtonBarUi.this.tabs.visibleTabs.isEmpty() && NetworkButtonBarUi.this.tabs.isValid()) {
                GwtNetInterfaceConfig prevNetIf = NetworkButtonBarUi.this.table.selectionModel.getSelectedObject();
                final GwtNetInterfaceConfig updatedNetIf = NetworkButtonBarUi.this.tabs.getUpdatedInterface();

                // submit updated netInterfaceConfig and priorities
                if (prevNetIf != null && prevNetIf.equals(updatedNetIf)) {
                    NetworkButtonBarUi.this.table.refresh();
                    NetworkButtonBarUi.this.apply.setEnabled(false);
                } else {
                    String newNetwork = null;
                    String prevNetwork = null;
                    try {
                        newNetwork = calculateNetwork(updatedNetIf.getIpAddress(), updatedNetIf.getSubnetMask());
                        prevNetwork = calculateNetwork(Window.Location.getHost(), updatedNetIf.getSubnetMask());
                    } catch (Exception e) {

                    }

                    scheduleRefresh(prevNetIf, updatedNetIf, newNetwork, prevNetwork);

                    EntryClassUi.showWaitModal();
                    updateNetConfiguration(updatedNetIf);
                }
            } else {
                logger.log(Level.FINER, MSGS.information() + ": " + MSGS.deviceConfigError());
                NetworkButtonBarUi.this.incompleteFieldsModal.show();
            }
        });
    }

    private void scheduleRefresh(GwtNetInterfaceConfig prevNetIf, final GwtNetInterfaceConfig updatedNetIf,
            String newNetwork, String prevNetwork) {
        if (isRefreshNeeded(prevNetIf, updatedNetIf, newNetwork, prevNetwork)) {
            Timer t = new Timer() {

                @Override
                public void run() {
                    Window.Location.replace("http://" + updatedNetIf.getIpAddress());
                }
            };
            t.schedule(500);
        }
    }

    private void updateNetConfiguration(final GwtNetInterfaceConfig updatedNetIf) {
        NetworkButtonBarUi.this.gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

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

    private String calculateNetwork(String ipAddress, String netmask) {
        if (ipAddress == null || ipAddress.isEmpty() || netmask == null || netmask.isEmpty()) {
            return null;
        }

        String network = null;

        try {
            int ipAddressValue = 0;
            int netmaskValue = 0;

            String[] sa = splitIp(ipAddress);

            for (int i = 24, t = 0; i >= 0; i -= 8, t++) {
                ipAddressValue = ipAddressValue | Integer.parseInt(sa[t]) << i;
            }

            sa = splitIp(netmask);
            for (int i = 24, t = 0; i >= 0; i -= 8, t++) {
                netmaskValue = netmaskValue | Integer.parseInt(sa[t]) << i;
            }

            network = dottedQuad(ipAddressValue & netmaskValue);
        } catch (Exception e) {
            logger.warning(e.getLocalizedMessage());
        }
        return network;
    }

    private String dottedQuad(int ip) {
        StringBuilder sb = new StringBuilder(15);
        for (int shift = 24; shift > 0; shift -= 8) {
            // process 3 bytes, from high order byte down.
            sb.append(Integer.toString(ip >>> shift & 0xff));
            sb.append('.');
        }
        sb.append(Integer.toString(ip & 0xff));
        return sb.toString();
    }

    private String[] splitIp(String ip) {

        String sIp = ip;
        String[] ret = new String[4];

        int ind = 0;
        for (int i = 0; i < 3; i++) {
            if ((ind = sIp.indexOf('.')) >= 0) {
                ret[i] = sIp.substring(0, ind);
                sIp = sIp.substring(ind + 1);
                if (i == 2) {
                    ret[3] = sIp;
                }
            }
        }
        return ret;
    }

    private void initModal() {
        this.incompleteFieldsModal.setTitle(MSGS.warning());
        this.incompleteFieldsText.setText(MSGS.formWithErrorsOrIncomplete());
    }

    private boolean isRefreshNeeded(GwtNetInterfaceConfig prevNetIf, final GwtNetInterfaceConfig updatedNetIf,
            String newNetwork, String prevNetwork) {
        return newNetwork != null && prevNetIf != null && updatedNetIf.getConfigMode().equals(IPV4_MODE_MANUAL_NAME)
                && newNetwork.equals(prevNetwork) && Window.Location.getHost().equals(prevNetIf.getIpAddress());
    }

}

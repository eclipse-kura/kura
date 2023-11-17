/*******************************************************************************
 * Copyright (c) 2011, 2023 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.web.client.ui.firewall;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.ui.Tab;
import org.eclipse.kura.web.client.ui.Tab.RefreshHandler;
import org.eclipse.kura.web.client.util.FailureHandler;
import org.eclipse.kura.web.shared.service.GwtNetworkService;
import org.eclipse.kura.web.shared.service.GwtNetworkServiceAsync;
import org.gwtbootstrap3.client.ui.Anchor;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.Modal;
import org.gwtbootstrap3.client.ui.TabListItem;
import org.gwtbootstrap3.client.ui.html.Span;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Widget;

public class FirewallPanelUi extends Composite {

    private static FirewallPanelUiUiBinder uiBinder = GWT.create(FirewallPanelUiUiBinder.class);

    interface FirewallPanelUiUiBinder extends UiBinder<Widget, FirewallPanelUi> {
    }

    private final GwtNetworkServiceAsync gwtNetworkService = GWT.create(GwtNetworkService.class);

    @UiField
    OpenPortsTabUi openPortsPanel;
    @UiField
    PortForwardingTabUi portForwardingPanel;
    @UiField
    NatTabUi ipForwardingPanel;
    @UiField
    OpenPortsIPv6TabUi openPortsIPv6Panel;
    @UiField
    PortForwardingIPv6TabUi portForwardingIPv6Panel;
    @UiField
    NatIPv6TabUi ipForwardingIPv6Panel;

    private static final Messages MSGS = GWT.create(Messages.class);

    @UiField
    HTMLPanel firewallIntro;
    @UiField
    TabListItem openPorts;
    @UiField
    TabListItem portForwarding;
    @UiField
    TabListItem ipForwarding;
    @UiField
    TabListItem openPortsIPv6;
    @UiField
    TabListItem portForwardingIPv6;
    @UiField
    TabListItem ipForwardingIPv6;

    @UiField
    Modal dirtyModal;
    @UiField
    Button yes;
    @UiField
    Button no;

    private TabListItem currentlySelectedTab;
    private final Tab.RefreshHandler openPortsHandler;
    private final Tab.RefreshHandler portForwardingHandler;
    private final Tab.RefreshHandler ipForwardingHandler;
    private final Tab.RefreshHandler openPortsIPv6Handler;
    private final Tab.RefreshHandler portForwardingIPv6Handler;
    private final Tab.RefreshHandler ipForwardingIPv6Handler;
    private boolean isNet2;

    public FirewallPanelUi() {

        initWidget(uiBinder.createAndBindUi(this));

        this.firewallIntro.add(new Span("<p>" + MSGS.firewallIntro() + "</p>"));

        this.openPorts.setText(MSGS.firewallOpenPorts());
        this.portForwarding.setText(MSGS.firewallPortForwarding());
        this.ipForwarding.setText(MSGS.firewallNat());

        this.openPortsIPv6.setText(MSGS.firewallOpenPortsIPv6());
        this.portForwardingIPv6.setText(MSGS.firewallPortForwardingIPv6());
        this.ipForwardingIPv6.setText(MSGS.firewallNatIPv6());

        this.openPortsHandler = new Tab.RefreshHandler(this.openPortsPanel);
        this.openPorts.addClickHandler(event -> handleEvent(event, this.openPortsHandler));
        this.portForwardingHandler = new Tab.RefreshHandler(this.portForwardingPanel);
        this.portForwarding.addClickHandler(event -> handleEvent(event, this.portForwardingHandler));
        this.ipForwardingHandler = new Tab.RefreshHandler(this.ipForwardingPanel);
        this.ipForwarding.addClickHandler(event -> handleEvent(event, this.ipForwardingHandler));

        this.openPortsIPv6Handler = new Tab.RefreshHandler(this.openPortsIPv6Panel);
        this.openPortsIPv6.addClickHandler(event -> handleEvent(event, this.openPortsIPv6Handler));
        this.portForwardingIPv6Handler = new Tab.RefreshHandler(this.portForwardingIPv6Panel);
        this.portForwardingIPv6.addClickHandler(event -> handleEvent(event, this.portForwardingIPv6Handler));
        this.ipForwardingIPv6Handler = new Tab.RefreshHandler(this.ipForwardingIPv6Panel);
        this.ipForwardingIPv6.addClickHandler(event -> handleEvent(event, this.ipForwardingIPv6Handler));
    }

    public void initFirewallPanel() {
        FirewallPanelUi.this.currentlySelectedTab = this.openPorts;

        this.portForwardingPanel.clear();
        this.ipForwardingPanel.clear();
        this.openPortsPanel.refresh();

        this.portForwardingIPv6Panel.clear();
        this.ipForwardingIPv6Panel.clear();
        if (this.isNet2) {
            this.openPortsIPv6Panel.refresh();
        } else {
            this.openPortsIPv6Panel.clear();
        }

        this.openPorts.showTab();

        detectIfNet2();
    }

    public boolean isDirty() {
        boolean ipv4PanelsDirty = this.openPortsPanel.isDirty() || this.portForwardingPanel.isDirty()
                || this.ipForwardingPanel.isDirty();
        boolean ipv6PanelsDirty = this.openPortsIPv6Panel.isDirty() || this.portForwardingIPv6Panel.isDirty()
                || this.ipForwardingIPv6Panel.isDirty();
        return ipv4PanelsDirty || ipv6PanelsDirty;
    }

    public void setDirty(boolean b) {
        this.openPortsPanel.setDirty(b);
        this.portForwardingPanel.setDirty(b);
        this.ipForwardingPanel.setDirty(b);

        this.openPortsIPv6Panel.setDirty(b);
        this.portForwardingIPv6Panel.setDirty(b);
        this.ipForwardingIPv6Panel.setDirty(b);
    }

    private void showDirtyModal(TabListItem newTabListItem, RefreshHandler newTabRefreshHandler) {
        this.yes.addClickHandler(event -> {
            this.dirtyModal.hide();
            FirewallPanelUi.this.getTab(this.currentlySelectedTab).clear();
            FirewallPanelUi.this.currentlySelectedTab = newTabListItem;
            newTabRefreshHandler.onClick(event);
        });
        this.no.addClickHandler(event -> {
            FirewallPanelUi.this.currentlySelectedTab.showTab();
            this.dirtyModal.hide();
        });
        this.no.setFocus(true);
        this.dirtyModal.show();
    }

    private void handleEvent(ClickEvent event, Tab.RefreshHandler handler) {
        TabListItem newTabListItem = (TabListItem) ((Anchor) event.getSource()).getParent();
        if (newTabListItem != FirewallPanelUi.this.currentlySelectedTab) {
            if (getTab(FirewallPanelUi.this.currentlySelectedTab).isDirty()) {
                showDirtyModal(newTabListItem, handler);
            } else {
                FirewallPanelUi.this.currentlySelectedTab = newTabListItem;
                getTab(FirewallPanelUi.this.currentlySelectedTab).setDirty(true);
                handler.onClick(event);
            }
        }
    }

    // This is not very clean...
    private Tab getTab(TabListItem item) {
        if (item.getDataTarget().equals("#openPortsPanel")) {
            return this.openPortsPanel;
        } else if (item.getDataTarget().equals("#portForwardingPanel")) {
            return this.portForwardingPanel;
        } else if (item.getDataTarget().equals("#ipForwardingPanel")) {
            return this.ipForwardingPanel;
        } else if (item.getDataTarget().equals("#openPortsIPv6Panel")) {
            return this.openPortsIPv6Panel;
        } else if (item.getDataTarget().equals("#portForwardingIPv6Panel")) {
            return this.portForwardingIPv6Panel;
        } else if (item.getDataTarget().equals("#ipForwardingIPv6Panel")) {
            return this.ipForwardingIPv6Panel;
        } else {
            return this.openPortsPanel;
        }
    }

    private void detectIfNet2() {
        this.gwtNetworkService.isNet2(new AsyncCallback<Boolean>() {

            @Override
            public void onFailure(Throwable caught) {
                FirewallPanelUi.this.isNet2 = false;
                FailureHandler.handle(caught);
            }

            @Override
            public void onSuccess(Boolean result) {
                FirewallPanelUi.this.isNet2 = result;
                initNet2FeaturesOnly(result);
            }
        });
    }

    private void initNet2FeaturesOnly(boolean isNet2) {
        this.openPortsIPv6.setVisible(isNet2);
        this.portForwardingIPv6.setVisible(isNet2);
        this.ipForwardingIPv6.setVisible(isNet2);
    }
}

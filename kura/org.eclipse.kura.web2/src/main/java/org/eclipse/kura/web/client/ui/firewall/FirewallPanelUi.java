/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.web.client.ui.firewall;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.ui.Tab;
import org.eclipse.kura.web.client.ui.Tab.RefreshHandler;
import org.gwtbootstrap3.client.ui.Anchor;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.ButtonGroup;
import org.gwtbootstrap3.client.ui.Modal;
import org.gwtbootstrap3.client.ui.ModalBody;
import org.gwtbootstrap3.client.ui.ModalFooter;
import org.gwtbootstrap3.client.ui.ModalHeader;
import org.gwtbootstrap3.client.ui.TabListItem;
import org.gwtbootstrap3.client.ui.html.Span;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Widget;

public class FirewallPanelUi extends Composite {

    private static FirewallPanelUiUiBinder uiBinder = GWT.create(FirewallPanelUiUiBinder.class);

    interface FirewallPanelUiUiBinder extends UiBinder<Widget, FirewallPanelUi> {
    }

    @UiField
    OpenPortsTabUi openPortsPanel;
    @UiField
    PortForwardingTabUi portForwardingPanel;
    @UiField
    NatTabUi ipForwardingPanel;

    private static final Messages MSGS = GWT.create(Messages.class);

    @UiField
    HTMLPanel firewallIntro;
    @UiField
    TabListItem openPorts;
    @UiField
    TabListItem portForwarding;
    @UiField
    TabListItem ipForwarding;

    private TabListItem currentlySelectedTab;
    private Tab.RefreshHandler openPortsHandler;
    private Tab.RefreshHandler portForwardingHandler;
    private Tab.RefreshHandler ipForwardingHandler;

    public FirewallPanelUi() {

        initWidget(uiBinder.createAndBindUi(this));
        this.firewallIntro.add(new Span("<p>" + MSGS.firewallIntro() + "</p>"));
        this.openPorts.setText(MSGS.firewallOpenPorts());
        this.portForwarding.setText(MSGS.firewallPortForwarding());
        this.ipForwarding.setText(MSGS.firewallNat());

        this.openPortsHandler = new Tab.RefreshHandler(this.openPortsPanel);
        this.openPorts.addClickHandler(event -> {
            handleEvent(event, this.openPortsHandler);
        });
        this.portForwardingHandler = new Tab.RefreshHandler(this.portForwardingPanel);
        this.portForwarding.addClickHandler(event -> {
            handleEvent(event, this.portForwardingHandler);
        });
        this.ipForwardingHandler = new Tab.RefreshHandler(this.ipForwardingPanel);
        this.ipForwarding.addClickHandler(event -> {
            handleEvent(event, this.ipForwardingHandler);
        });
    }

    public void initFirewallPanel() {
        FirewallPanelUi.this.currentlySelectedTab = openPorts;
        this.portForwardingPanel.clear();
        this.ipForwardingPanel.clear();
        this.openPortsPanel.refresh();
        this.openPorts.showTab();
    }

    public boolean isDirty() {
        return this.openPortsPanel.isDirty() || this.portForwardingPanel.isDirty() || this.ipForwardingPanel.isDirty();
    }

    public void setDirty(boolean b) {
        this.openPortsPanel.setDirty(b);
        this.portForwardingPanel.setDirty(b);
        this.ipForwardingPanel.setDirty(b);
    }

    private void showDirtyModal(TabListItem newTabListItem, RefreshHandler newTabRefreshHandler) {
        final Modal modal = new Modal();

        ModalHeader header = new ModalHeader();
        header.setTitle(MSGS.confirm());
        modal.add(header);

        ModalBody body = new ModalBody();
        body.add(new Span(MSGS.deviceConfigDirty()));
        modal.add(body);

        ModalFooter footer = new ModalFooter();
        ButtonGroup group = new ButtonGroup();
        Button yes = new Button();
        yes.setText(MSGS.yesButton());
        yes.addClickHandler(event -> {
            modal.hide();
            FirewallPanelUi.this.getTab(this.currentlySelectedTab).clear();
            FirewallPanelUi.this.currentlySelectedTab = newTabListItem;
            newTabRefreshHandler.onClick(event);
        });
        Button no = new Button();
        no.setText(MSGS.noButton());
        no.addClickHandler(event -> {
            FirewallPanelUi.this.currentlySelectedTab.showTab();
            modal.hide();
        });
        group.add(no);
        group.add(yes);
        footer.add(group);
        modal.add(footer);
        modal.show();
        no.setFocus(true);
    }

    private void handleEvent(ClickEvent event, Tab.RefreshHandler handler) {
        TabListItem newTabListItem = (TabListItem) ((Anchor) event.getSource()).getParent();
        if (newTabListItem != FirewallPanelUi.this.currentlySelectedTab) {
            if (getTab(FirewallPanelUi.this.currentlySelectedTab).isDirty()) {
                showDirtyModal(newTabListItem, handler);
            } else {
                this.currentlySelectedTab = newTabListItem;
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
        } else {
            return this.openPortsPanel;
        }
    }
}

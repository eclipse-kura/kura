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
package org.eclipse.kura.web.client.ui.Firewall;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.ui.Tab;
import org.gwtbootstrap3.client.ui.TabListItem;
import org.gwtbootstrap3.client.ui.html.Span;

import com.google.gwt.core.client.GWT;
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
    TabListItem openPorts, portForwarding, ipForwarding;

    public FirewallPanelUi() {
        initWidget(uiBinder.createAndBindUi(this));
        this.firewallIntro.add(new Span("<p>" + MSGS.firewallIntro() + "</p>"));
        this.openPorts.setText(MSGS.firewallOpenPorts());
        this.portForwarding.setText(MSGS.firewallPortForwarding());
        this.ipForwarding.setText(MSGS.firewallNat());

        this.openPorts.addClickHandler(new Tab.RefreshHandler(openPortsPanel));
        this.portForwarding.addClickHandler(new Tab.RefreshHandler(portForwardingPanel));
        this.ipForwarding.addClickHandler(new Tab.RefreshHandler(ipForwardingPanel));
    }

    public void initFirewallPanel() {
        openPortsPanel.refresh();
    }

    public boolean isDirty() {
        return openPortsPanel.isDirty() || portForwardingPanel.isDirty() || ipForwardingPanel.isDirty();
    }

    public void setDirty(boolean b) {
        openPortsPanel.setDirty(b);
        portForwardingPanel.setDirty(b);
        ipForwardingPanel.setDirty(b);
    }
}

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
import org.gwtbootstrap3.client.ui.AnchorListItem;
import org.gwtbootstrap3.client.ui.Well;
import org.gwtbootstrap3.client.ui.html.Span;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Widget;

public class FirewallPanelUi extends Composite {

    private static FirewallPanelUiUiBinder uiBinder = GWT.create(FirewallPanelUiUiBinder.class);

    interface FirewallPanelUiUiBinder extends UiBinder<Widget, FirewallPanelUi> {
    }

    private static OpenPortsTabUi openPortsBinder = GWT.create(OpenPortsTabUi.class);
    private static PortForwardingTabUi portForwardingBinder = GWT.create(PortForwardingTabUi.class);
    private static NatTabUi ipForwardingBinder = GWT.create(NatTabUi.class);

    private static final Messages MSGS = GWT.create(Messages.class);

    @UiField
    HTMLPanel firewallIntro;
    @UiField
    AnchorListItem openPorts, portForwarding, ipForwarding;
    @UiField
    Well content;

    public FirewallPanelUi() {
        initWidget(uiBinder.createAndBindUi(this));
        this.firewallIntro.add(new Span("<p>" + MSGS.firewallIntro() + "</p>"));
        this.openPorts.setText(MSGS.firewallOpenPorts());
        this.portForwarding.setText(MSGS.firewallPortForwarding());
        this.ipForwarding.setText(MSGS.firewallNat());

        this.openPorts.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                setSelectedActive(FirewallPanelUi.this.openPorts);
                FirewallPanelUi.this.content.clear();
                FirewallPanelUi.this.content.add(openPortsBinder);
                if (!openPortsBinder.isDirty()) {
                    openPortsBinder.refresh();
                }
            }
        });

        this.portForwarding.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                setSelectedActive(FirewallPanelUi.this.portForwarding);
                FirewallPanelUi.this.content.clear();
                FirewallPanelUi.this.content.add(portForwardingBinder);
                if (!portForwardingBinder.isDirty()) {
                    portForwardingBinder.refresh();
                }
            }
        });

        this.ipForwarding.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                setSelectedActive(FirewallPanelUi.this.ipForwarding);
                FirewallPanelUi.this.content.clear();
                FirewallPanelUi.this.content.add(ipForwardingBinder);
                if (!ipForwardingBinder.isDirty()) {
                    ipForwardingBinder.refresh();
                }
            }
        });
    }

    public void initFirewallPanel() {
        setSelectedActive(this.openPorts);
        this.content.clear();
        this.content.add(openPortsBinder);
        openPortsBinder.refresh();
    }

    public void setSelectedActive(AnchorListItem item) {
        this.openPorts.setActive(false);
        this.portForwarding.setActive(false);
        this.ipForwarding.setActive(false);
        item.setActive(true);
    }

    public boolean isDirty() {
        return openPortsBinder.isDirty() || portForwardingBinder.isDirty() || ipForwardingBinder.isDirty();
    }

    public void setDirty(boolean b) {
        if (openPortsBinder != null) {
            openPortsBinder.setDirty(b);
        }
        if (portForwardingBinder != null) {
            portForwardingBinder.setDirty(b);
        }
        if (ipForwardingBinder != null) {
            ipForwardingBinder.setDirty(b);
        }
    }
}

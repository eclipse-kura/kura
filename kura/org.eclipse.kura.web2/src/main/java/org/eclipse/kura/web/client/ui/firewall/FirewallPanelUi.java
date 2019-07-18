/*******************************************************************************
 * Copyright (c) 2011, 2019 Eurotech and/or its affiliates
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
import org.eclipse.kura.web.shared.service.GwtSecurityService;
import org.eclipse.kura.web.shared.service.GwtSecurityServiceAsync;
import org.gwtbootstrap3.client.ui.TabListItem;
import org.gwtbootstrap3.client.ui.html.Span;

import com.google.gwt.core.client.GWT;
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

    private static final Messages MSGS = GWT.create(Messages.class);
    private final GwtSecurityServiceAsync gwtSecurityService = GWT.create(GwtSecurityService.class);

    @UiField
    OpenPortsTabUi openPortsPanel;
    @UiField
    PortForwardingTabUi portForwardingPanel;
    @UiField
    NatTabUi ipForwardingPanel;
    @UiField
    IdsTabUi idsPanel;

    @UiField
    HTMLPanel firewallIntro;
    @UiField
    TabListItem openPorts;
    @UiField
    TabListItem portForwarding;
    @UiField
    TabListItem ipForwarding;
    @UiField
    TabListItem ids;

    public FirewallPanelUi() {
        initWidget(uiBinder.createAndBindUi(this));
        this.firewallIntro.add(new Span("<p>" + MSGS.firewallIntro() + "</p>"));
        this.openPorts.setText(MSGS.firewallOpenPorts());
        this.portForwarding.setText(MSGS.firewallPortForwarding());
        this.ipForwarding.setText(MSGS.firewallNat());
        this.ids.setText("Ids");

        this.openPorts.addClickHandler(new Tab.RefreshHandler(this.openPortsPanel));
        this.portForwarding.addClickHandler(new Tab.RefreshHandler(this.portForwardingPanel));
        this.ipForwarding.addClickHandler(new Tab.RefreshHandler(this.ipForwardingPanel));
        this.ids.addClickHandler(new Tab.RefreshHandler(this.idsPanel));

        this.gwtSecurityService.isIdsAvailable(new AsyncCallback<Boolean>() {

            @Override
            public void onSuccess(Boolean result) {
                FirewallPanelUi.this.ids.setVisible(result);

            }

            @Override
            public void onFailure(Throwable caught) {
                // TODO Auto-generated method stub

            }
        });
    }

    public void initFirewallPanel() {
        this.openPortsPanel.refresh();
    }

    public boolean isDirty() {
        return this.openPortsPanel.isDirty() || this.portForwardingPanel.isDirty() || this.ipForwardingPanel.isDirty()
                || this.idsPanel.isDirty();
    }

    public void setDirty(boolean b) {
        this.openPortsPanel.setDirty(b);
        this.portForwardingPanel.setDirty(b);
        this.ipForwardingPanel.setDirty(b);
        this.idsPanel.setDirty(b);
    }
}

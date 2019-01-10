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

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.shared.model.GwtSession;
import org.gwtbootstrap3.client.ui.Container;
import org.gwtbootstrap3.client.ui.Panel;
import org.gwtbootstrap3.client.ui.PanelBody;
import org.gwtbootstrap3.client.ui.html.Span;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Widget;

public class NetworkPanelUi extends Composite {

    private static NetworkPanelUiUiBinder uiBinder = GWT.create(NetworkPanelUiUiBinder.class);

    interface NetworkPanelUiUiBinder extends UiBinder<Widget, NetworkPanelUi> {
    }

    private static final Messages MSGS = GWT.create(Messages.class);

    GwtSession session;

    @UiField
    HTMLPanel networkIntro;
    @UiField
    Panel interfacesTable;
    @UiField
    PanelBody tabsPanel;
    @UiField
    Container buttonBar;

    private NetworkTabsUi tabs;

    private boolean isInitialized;

    public NetworkPanelUi() {
        initWidget(uiBinder.createAndBindUi(this));
        this.networkIntro.add(new Span("<p>" + MSGS.netIntro() + "</p>"));
        this.isInitialized = false;
    }

    public void initNetworkPanel() {
        if (!this.isInitialized) {
            this.tabs = new NetworkTabsUi(this.session);
            this.tabsPanel.add(this.tabs);

            NetworkInterfacesTableUi table = new NetworkInterfacesTableUi(this.session, this.tabs);
            this.interfacesTable.add(table);

            NetworkButtonBarUi buttons = new NetworkButtonBarUi(this.session, this.tabs, table);
            this.buttonBar.add(buttons);

            this.tabs.setDirty(false);
            this.isInitialized = true;
        }
    }

    public boolean isDirty() {
        if (this.tabs != null) {
            return this.tabs.isDirty();
        } else {
            return false;
        }
    }

    public void setSession(GwtSession currentSession) {
        this.session = currentSession;
    }

    public void setDirty(boolean b) {
        if (this.tabs != null) {
            this.tabs.setDirty(b);
        }
    }

}

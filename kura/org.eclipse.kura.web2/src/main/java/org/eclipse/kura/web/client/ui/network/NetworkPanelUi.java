/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates and others
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

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.util.FailureHandler;
import org.eclipse.kura.web.shared.model.GwtSession;
import org.eclipse.kura.web.shared.service.GwtNetworkService;
import org.eclipse.kura.web.shared.service.GwtNetworkServiceAsync;
import org.gwtbootstrap3.client.ui.Container;
import org.gwtbootstrap3.client.ui.Panel;
import org.gwtbootstrap3.client.ui.PanelBody;
import org.gwtbootstrap3.client.ui.html.Span;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Widget;

public class NetworkPanelUi extends Composite {

    private static NetworkPanelUiUiBinder uiBinder = GWT.create(NetworkPanelUiUiBinder.class);
    private final GwtNetworkServiceAsync gwtNetworkService = GWT.create(GwtNetworkService.class);

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

    private NetworkInterfacesTableUi table;
    private NetworkTabsUi tabs;

    private boolean isInitialized;

    public NetworkPanelUi() {
        initWidget(uiBinder.createAndBindUi(this));
        this.networkIntro.add(new Span("<p>" + MSGS.netIntro() + "</p>"));
        this.isInitialized = false;
    }

    public void initNetworkPanel() {

        this.gwtNetworkService.isNet2(new AsyncCallback<Boolean>() {

            @Override
            public void onFailure(Throwable caught) {
                initNetworkPanel(false);
                FailureHandler.handle(caught);
            }

            @Override
            public void onSuccess(Boolean result) {
                initNetworkPanel(result);
            }
        });
    }

    private void initNetworkPanel(final boolean isNet2) {

        if (!this.isInitialized) {
            this.tabs = new NetworkTabsUi(this.session, isNet2);
            this.tabsPanel.add(this.tabs);

            table = new NetworkInterfacesTableUi(this.session, this.tabs);
            this.interfacesTable.add(table);

            NetworkButtonBarUi buttons = new NetworkButtonBarUi(this.session, this.tabs, table);
            this.buttonBar.add(buttons);

            this.tabs.setDirty(false);
            this.tabs.setButtons(buttons);
            this.isInitialized = true;
        } else {
            this.tabs.setDirty(true);
            this.tabs.refresh();
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

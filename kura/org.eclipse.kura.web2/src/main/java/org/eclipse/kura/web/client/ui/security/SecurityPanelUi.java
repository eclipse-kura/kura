/*******************************************************************************
 * Copyright (c) 2020 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.web.client.ui.security;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.ui.Tab;
import org.eclipse.kura.web.shared.model.GwtSession;
import org.eclipse.kura.web.shared.service.GwtSecurityService;
import org.eclipse.kura.web.shared.service.GwtSecurityServiceAsync;
import org.eclipse.kura.web2.ext.WidgetFactory;
import org.gwtbootstrap3.client.ui.NavTabs;
import org.gwtbootstrap3.client.ui.TabContent;
import org.gwtbootstrap3.client.ui.TabListItem;
import org.gwtbootstrap3.client.ui.TabPane;
import org.gwtbootstrap3.client.ui.html.Paragraph;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Widget;

public class SecurityPanelUi extends Composite {

    private static SecurityPanelUiUiBinder uiBinder = GWT.create(SecurityPanelUiUiBinder.class);
    private static final Logger logger = Logger.getLogger(SecurityPanelUi.class.getSimpleName());
    private static final Messages MSGS = GWT.create(Messages.class);

    private final GwtSecurityServiceAsync gwtSecurityService = GWT.create(GwtSecurityService.class);

    interface SecurityPanelUiUiBinder extends UiBinder<Widget, SecurityPanelUi> {
    }

    GwtSession session;

    @UiField
    CertificateListTabUi certificateListPanel;
    @UiField
    HttpServiceTabUi httpServicePanel;
    @UiField
    SecurityTabUi securityPanel;

    @UiField
    TabListItem certificateList;
    @UiField
    TabListItem httpService;
    @UiField
    TabListItem security;

    @UiField
    TabContent tabContent;
    @UiField
    NavTabs navTabs;

    @UiField
    HTMLPanel securityIntro;

    public SecurityPanelUi() {
        logger.log(Level.FINER, "Initiating SecurityPanelUI...");

        initWidget(uiBinder.createAndBindUi(this));
        Paragraph description = new Paragraph();
        description.setText(MSGS.securityIntro());
        this.securityIntro.add(description);

        AsyncCallback<Boolean> callback = new AsyncCallback<Boolean>() {

            @Override
            public void onFailure(Throwable caught) {
                SecurityPanelUi.this.security.setVisible(false);
            }

            @Override
            public void onSuccess(Boolean result) {
                SecurityPanelUi.this.security.setVisible(result);
            }
        };
        this.gwtSecurityService.isSecurityServiceAvailable(callback);

        this.certificateList.addClickHandler(new Tab.RefreshHandler(this.certificateListPanel));
        this.httpService.addClickHandler(event -> this.httpServicePanel.load());
        this.security.addClickHandler(new Tab.RefreshHandler(this.securityPanel));
    }

    public void load() {
        this.certificateListPanel.refresh();
    }

    public void setSession(GwtSession currentSession) {
        this.session = currentSession;
    }

    public boolean isDirty() {
        boolean certListDirty = this.certificateListPanel.isDirty();
        boolean securityDirty = this.securityPanel.isDirty();
        boolean httpServiceDirty = this.httpServicePanel.isDirty();

        return certListDirty || httpServiceDirty || securityDirty;
    }

    public void addTab(final String name, final WidgetFactory widgetFactory) {

        final TabPane tabPane = new TabPane();
        tabPane.setId("__extension__" + name);

        final TabListItem item = new TabListItem(name);
        item.setDataTarget("#__extension__" + name);

        item.addClickHandler(e -> {
            tabPane.clear();
            tabPane.add(widgetFactory.buildWidget());
        });

        this.navTabs.add(item);
        this.tabContent.add(tabPane);
    }

    public void setDirty(boolean b) {
        this.certificateListPanel.setDirty(b);
        this.securityPanel.setDirty(b);
        this.httpServicePanel.setDirty(b);
    }
}

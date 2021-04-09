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
package org.eclipse.kura.web.client.ui.settings;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.ui.Tab;
import org.eclipse.kura.web.client.ui.Tab.RefreshHandler;
import org.eclipse.kura.web.shared.model.GwtSession;
import org.eclipse.kura.web2.ext.WidgetFactory;
import org.gwtbootstrap3.client.ui.Anchor;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.ButtonGroup;
import org.gwtbootstrap3.client.ui.Modal;
import org.gwtbootstrap3.client.ui.ModalBody;
import org.gwtbootstrap3.client.ui.ModalFooter;
import org.gwtbootstrap3.client.ui.ModalHeader;
import org.gwtbootstrap3.client.ui.NavTabs;
import org.gwtbootstrap3.client.ui.TabContent;
import org.gwtbootstrap3.client.ui.TabListItem;
import org.gwtbootstrap3.client.ui.TabPane;
import org.gwtbootstrap3.client.ui.html.Paragraph;
import org.gwtbootstrap3.client.ui.html.Span;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Widget;

public class SettingsPanelUi extends Composite {

    private static SettingsPanelUiUiBinder uiBinder = GWT.create(SettingsPanelUiUiBinder.class);
    private static final Logger logger = Logger.getLogger(SettingsPanelUi.class.getSimpleName());
    private static final Messages MSGS = GWT.create(Messages.class);

    interface SettingsPanelUiUiBinder extends UiBinder<Widget, SettingsPanelUi> {
    }

    GwtSession session;

    @UiField
    SnapshotsTabUi snapshotsPanel;

    @UiField
    TabListItem snapshots;
    @UiField
    TabContent tabContent;
    @UiField
    NavTabs navTabs;

    @UiField
    HTMLPanel settingsIntro;

    private TabListItem currentlySelectedTab;
    private Tab.RefreshHandler snapshotsHandler;

    public SettingsPanelUi() {
        logger.log(Level.FINER, "Initiating SettingsPanelUI...");

        initWidget(uiBinder.createAndBindUi(this));
        Paragraph description = new Paragraph();
        description.setText(MSGS.settingsIntro());
        this.settingsIntro.add(description);

        this.snapshots.setVisible(true);

        this.snapshotsHandler = new Tab.RefreshHandler(this.snapshotsPanel);
        this.snapshots.addClickHandler(event -> handleEvent(event, this.snapshotsHandler));

        this.currentlySelectedTab = this.snapshots;
    }

    public void load() {
        this.currentlySelectedTab = this.snapshots;
        this.snapshotsPanel.refresh();
        this.snapshots.showTab();
    }

    public void setSession(GwtSession currentSession) {
        this.session = currentSession;
    }

    public boolean isDirty() {

        return this.snapshotsPanel.isDirty();
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

        this.snapshotsPanel.setDirty(b);
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
        yes.addStyleName("fa fa-check");
        yes.addClickHandler(event -> {
            modal.hide();
            SettingsPanelUi.this.getTab(this.currentlySelectedTab).clear();
            SettingsPanelUi.this.currentlySelectedTab = newTabListItem;
            newTabRefreshHandler.onClick(event);
        });
        Button no = new Button();
        no.addStyleName("fa fa-times");
        no.setText(MSGS.noButton());
        no.addClickHandler(event -> {
            SettingsPanelUi.this.currentlySelectedTab.showTab();
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
        if (newTabListItem != SettingsPanelUi.this.currentlySelectedTab) {
            if (getTab(SettingsPanelUi.this.currentlySelectedTab).isDirty()) {
                showDirtyModal(newTabListItem, handler);
            } else {
                getTab(SettingsPanelUi.this.currentlySelectedTab).clear();
                SettingsPanelUi.this.currentlySelectedTab = newTabListItem;
                getTab(SettingsPanelUi.this.currentlySelectedTab).setDirty(true);
                handler.onClick(event);
            }
        }
    }

    // This is not very clean...
    private Tab getTab(TabListItem item) {
        return this.snapshotsPanel;
    }

}
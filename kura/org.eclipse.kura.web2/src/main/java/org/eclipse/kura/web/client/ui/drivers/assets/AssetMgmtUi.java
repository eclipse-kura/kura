/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.web.client.ui.drivers.assets;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.shared.model.GwtConfigComponent;
import org.gwtbootstrap3.client.ui.PanelHeader;
import org.gwtbootstrap3.client.ui.TabListItem;
import org.gwtbootstrap3.client.ui.TabPane;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

public class AssetMgmtUi extends Composite {

    private static AssetMgmtUiUiBinder uiBinder = GWT.create(AssetMgmtUiUiBinder.class);

    interface AssetMgmtUiUiBinder extends UiBinder<Widget, AssetMgmtUi> {
    }

    private static final Messages MSGS = GWT.create(Messages.class);

    private AssetConfigUi assetConfigUi;
    private AssetDataUi assetDataUi;

    @UiField
    PanelHeader contentPanelHeader;

    @UiField
    TabListItem tab1NavTab;
    @UiField
    TabListItem tab2NavTab;
    @UiField
    TabPane tab1Pane;
    @UiField
    TabPane tab2Pane;

    public AssetMgmtUi(final GwtConfigComponent addedItem) {
        initWidget(uiBinder.createAndBindUi(this));

        this.contentPanelHeader.setText(MSGS.assetLabel(addedItem.getComponentName()));

        tab1NavTab.setText(MSGS.assetConfig());
        tab2NavTab.setText(MSGS.assetData());

        assetConfigUi = new AssetConfigUi(addedItem);
        tab1Pane.add(assetConfigUi);

        assetDataUi = new AssetDataUi(addedItem);
        tab2Pane.add(assetDataUi);
        
        tab2NavTab.addClickHandler(new ClickHandler() {
            
            @Override
            public void onClick(ClickEvent event) {
                assetDataUi.renderForm();
            }
        });
    }

    public void refresh() {
        if (tab1NavTab.isActive()) {
            assetConfigUi.renderForm();
        } else {
            assetDataUi.renderForm();
        }
    }

    public void setDirty(boolean flag) {
        assetConfigUi.setDirty(flag);
        assetDataUi.setDirty(flag);
    }

    public boolean isDirty() {
        return assetConfigUi.isDirty() || assetDataUi.isDirty();
    }

}

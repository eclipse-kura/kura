/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.web.client.ui;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.shared.model.GwtConfigComponent;
import org.gwtbootstrap3.client.ui.AnchorListItem;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.Modal;
import org.gwtbootstrap3.client.ui.ModalBody;
import org.gwtbootstrap3.client.ui.ModalFooter;
import org.gwtbootstrap3.client.ui.ModalHeader;
import org.gwtbootstrap3.client.ui.constants.IconType;
import org.gwtbootstrap3.client.ui.html.Span;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Timer;

public class ServicesAnchorListItem extends AnchorListItem {

    private static final String SERVLET_URL = "/" + GWT.getModuleName() + "/file/icon?pid=";

    EntryClassUi ui;
    GwtConfigComponent item;
    ServicesAnchorListItem instance;
    private static final Messages MSGS = GWT.create(Messages.class);

    public ServicesAnchorListItem(GwtConfigComponent service, EntryClassUi mainUi) {
        super();
        this.ui = mainUi;
        this.item = service;
        this.instance = this;

        IconType icon = getIcon(this.item.getComponentName());
        if (icon == null) {
            String imageURL = getImagePath();
            if (imageURL != null) {
                StringBuilder imageTag = new StringBuilder();
                imageTag.append("<img src='");
                imageTag.append(imageURL);
                imageTag.append("' height='20' width='20'/>");
                imageTag.append(" ");
                imageTag.append(this.item.getComponentName());
                super.anchor.setHTML(imageTag.toString());
            } else {
                super.setIcon(IconType.CHEVRON_CIRCLE_RIGHT);
                super.setText(this.item.getComponentName());
            }
        } else {
            super.setIcon(icon);
            super.setText(this.item.getComponentName());
        }

        super.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                EntryClassUi.setActive(ServicesAnchorListItem.this);
                if (ServicesAnchorListItem.this.ui.selected != null
                        && ServicesAnchorListItem.this.ui.selected != ServicesAnchorListItem.this.item
                        && ServicesAnchorListItem.this.ui.servicesUi.isDirty()
                        || ServicesAnchorListItem.this.ui.isNetworkDirty()
                        || ServicesAnchorListItem.this.ui.isFirewallDirty()
                        || ServicesAnchorListItem.this.ui.isSettingsDirty()) {
                    final Modal modal = new Modal();

                    ModalHeader header = new ModalHeader();
                    header.setTitle(MSGS.warning());
                    modal.add(header);

                    ModalBody body = new ModalBody();
                    body.add(new Span(MSGS.deviceConfigDirty()));
                    modal.add(body);

                    ModalFooter footer = new ModalFooter();
                    footer.add(new Button(MSGS.yesButton(), new ClickHandler() {

                        @Override
                        public void onClick(ClickEvent event) {
                            ServicesAnchorListItem.this.ui.setDirty(false);
                            ServicesAnchorListItem.this.ui.selected = ServicesAnchorListItem.this.item;
                            modal.hide();
                            if (ServicesAnchorListItem.this.instance.getIcon() != null) {
                                ServicesAnchorListItem.this.instance.setIconSpin(true);
                            }
                            ServicesAnchorListItem.this.ui.render(ServicesAnchorListItem.this.item);
                            Timer timer = new Timer() {

                                @Override
                                public void run() {
                                    if (ServicesAnchorListItem.this.instance.getIcon() != null) {
                                        ServicesAnchorListItem.this.instance.setIconSpin(false);
                                    }
                                }
                            };
                            timer.schedule(2000);

                        }
                    }));
                    footer.add(new Button(MSGS.noButton(), new ClickHandler() {

                        @Override
                        public void onClick(ClickEvent event) {
                            modal.hide();
                        }
                    }));
                    modal.add(footer);

                    modal.show();

                } else {
                    ServicesAnchorListItem.this.ui.selected = ServicesAnchorListItem.this.item;
                    if (ServicesAnchorListItem.this.instance.getIcon() != null) {
                        ServicesAnchorListItem.this.instance.setIconSpin(true);
                    }
                    ServicesAnchorListItem.this.ui.render(ServicesAnchorListItem.this.item);
                    Timer timer = new Timer() {

                        @Override
                        public void run() {
                            if (ServicesAnchorListItem.this.instance.getIcon() != null) {
                                ServicesAnchorListItem.this.instance.setIconSpin(false);
                            }
                        }
                    };
                    timer.schedule(2000);
                }
            }
        });

    }

    private IconType getIcon(String name) {
        if (name.startsWith("BluetoothService")) {
            return IconType.BTC;
        } else if (name.startsWith("CloudService")) {
            return IconType.CLOUD;
        } else if (name.startsWith("DiagnosticsService")) {
            return IconType.AMBULANCE;
        } else if (name.startsWith("ClockService")) {
            return IconType.CLOCK_O;
        } else if (name.startsWith("DataService")) {
            return IconType.DATABASE;
        } else if (name.startsWith("MqttDataTransport")) {
            return IconType.FORUMBEE;
        } else if (name.startsWith("PositionService")) {
            return IconType.LOCATION_ARROW;
        } else if (name.startsWith("WatchdogService")) {
            return IconType.HEARTBEAT;
        } else if (name.startsWith("SslManagerService")) {
            return IconType.LOCK;
        } else if (name.startsWith("VpnService")) {
            return IconType.CONNECTDEVELOP;
        } else if (name.startsWith("ProvisioningService")) {
            return IconType.EXCLAMATION_CIRCLE;
        } else if (name.startsWith("CommandPasswordService")) {
            return IconType.CHAIN;
        } else if (name.startsWith("WebConsole")) {
            return IconType.LAPTOP;
        } else if (name.startsWith("CommandService")) {
            return IconType.TERMINAL;
        } else if (name.startsWith("DenaliService")) {
            return IconType.SPINNER;
        } else {
            return null;
        }
    }

    private String getImagePath() {
        String icon = this.item.getComponentIcon();
        String componentId = this.item.getComponentId();
        if (icon != null && (icon.toLowerCase().startsWith("http://") || icon.toLowerCase().startsWith("https://"))
                && isImagePath(icon)) { // Util.isImagePath(icon)
            return icon;
        } else if (icon != null && isImagePath(icon)) { // Util.isImagePath(icon)
            return SERVLET_URL + componentId;
        } else {
            return null;
        }
    }

    private boolean isImagePath(String icon) {
        boolean isPng = icon.toLowerCase().endsWith(".png");
        boolean isJpg = icon.toLowerCase().endsWith(".jpg");
        boolean isGif = icon.toLowerCase().endsWith(".gif");
        return isPng || isJpg || isGif;
    }
}

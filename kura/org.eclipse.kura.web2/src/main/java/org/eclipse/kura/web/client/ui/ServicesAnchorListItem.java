/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Eurotech
 *  Amit Kumar Mondal
 *  Red Hat Inc
 *******************************************************************************/
package org.eclipse.kura.web.client.ui;

import org.eclipse.kura.web.Console;
import org.eclipse.kura.web.shared.model.GwtConfigComponent;
import org.gwtbootstrap3.client.ui.AnchorListItem;
import org.gwtbootstrap3.client.ui.constants.IconType;

import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.URL;

public class ServicesAnchorListItem extends AnchorListItem {

    private static final String SERVLET_URL = Console.ADMIN_ROOT + '/' + GWT.getModuleName() + "/file/icon?";

    GwtConfigComponent item;
    ServicesAnchorListItem instance;

    public ServicesAnchorListItem(GwtConfigComponent service) {
        super();
        this.item = service;
        this.instance = this;

        IconType icon = getIcon(this.item);
        if (icon == null) {
            String imageURL = getImagePath();
            if (imageURL != null) {
                StringBuilder imageTag = new StringBuilder();
                imageTag.append("<img src='");
                imageTag.append(imageURL);
                imageTag.append("' height='14' width='14'/>");
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

        final String description = service.getComponentDescription();
        if (description != null && !description.isEmpty()) {
            super.setTitle(description);
        }

    }

    public String getServiceName() {
        return this.item.getComponentName();
    }

    private IconType getIcon(GwtConfigComponent item) {
        final String name = item.getComponentName();

        if (name.startsWith("BluetoothService")) {
            return IconType.BTC;
        } else if (name.startsWith("BrokerInstance")) {
            return IconType.RSS;
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
            return IconType.CLOUD_DOWNLOAD;
        } else if (name.startsWith("CommandPasswordService")) {
            return IconType.CHAIN;
        } else if (name.startsWith("WebConsole")) {
            return IconType.LAPTOP;
        } else if (name.startsWith("CommandService")) {
            return IconType.TERMINAL;
        } else if (name.startsWith("DenaliService")) {
            return IconType.SPINNER;
        } else if (name.contains("H2Db")) {
            return IconType.DATABASE;
        } else if (name.startsWith("DeploymentService")) {
            return IconType.DOWNLOAD;
        } else if (name.startsWith("RebootService")) {
            return IconType.REFRESH;
        } else if (name.startsWith("VpnClient")) {
            return IconType.ARROWS_H;
        } else if (name.startsWith("TerminalClientService")) {
            return IconType.RANDOM;
        } else if (name.startsWith("TerminalServerService")) {
            return IconType.RANDOM;
        }

        final String id = item.getComponentId();
        if (id.endsWith(".BrokerInstance")) {
            return IconType.RSS;
        }

        return null;
    }

    private String getImagePath() {
        final String icon = this.item.getComponentIcon();

        if (icon == null) {
            return null;
        }

        if (!isImagePath(icon)) {
            return null;
        }

        if (icon.toLowerCase().startsWith("http://") || icon.toLowerCase().startsWith("https://")) {

            return icon;

        } else {

            final String factoryId = this.item.getFactoryId();
            if (factoryId != null) {
                return SERVLET_URL + "factoryId=" + URL.encodeQueryString(factoryId);
            }

            String componentId = this.item.getComponentId();

            if (componentId != null) {
                return SERVLET_URL + "pid=" + URL.encodeQueryString(componentId);
            }

        }

        return null;
    }

    private boolean isImagePath(String icon) {
        boolean isPng = icon.toLowerCase().endsWith(".png");
        boolean isJpg = icon.toLowerCase().endsWith(".jpg");
        boolean isGif = icon.toLowerCase().endsWith(".gif");
        return isPng || isJpg || isGif;
    }
}

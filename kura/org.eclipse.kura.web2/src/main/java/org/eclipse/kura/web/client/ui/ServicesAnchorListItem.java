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
		ui = mainUi;
		item = service;
		instance = this;

		IconType icon= getIcon(item.getComponentName());
		if (icon == null) {
			String imageURL= getImagePath();
			if (imageURL != null) {
				StringBuilder imageTag= new StringBuilder();
				imageTag.append("<img src='");
				imageTag.append(imageURL);
				imageTag.append("' height='20' width='20'/>");
				imageTag.append(" ");
				imageTag.append(item.getComponentName());
				super.anchor.setHTML(imageTag.toString());
			} else {
				super.setIcon(IconType.CHEVRON_CIRCLE_RIGHT);
				super.setText(item.getComponentName());
			}
		} else {
			super.setIcon(icon);
			super.setText(item.getComponentName());
		}

		super.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				if ((ui.selected != null && ui.selected != item && ui.servicesUi.isDirty()) || 
						ui.isNetworkDirty()  ||
						ui.isFirewallDirty() ||
						ui.isSettingsDirty() ) { 
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
							ui.setDirty(false);
							ui.selected = item;
							modal.hide();
							if (instance.getIcon() != null) {
								instance.setIconSpin(true);
							}
							ui.render(item);
							Timer timer = new Timer() {
								@Override
								public void run() {
									if (instance.getIcon() != null) {
										instance.setIconSpin(false);
									}
								}
							};
							timer.schedule(2000);

						}
					}));
					footer.add(new Button(MSGS.noButton(), new ClickHandler(){
						@Override
						public void onClick(ClickEvent event) {
							modal.hide();
						}}));
					modal.add(footer);

					modal.show();

				} else {
					ui.selected = item;
					if (instance.getIcon() != null) {
						instance.setIconSpin(true);
					}
					ui.render(item);
					Timer timer = new Timer() {
						@Override
						public void run() {
							if (instance.getIcon() != null) {
								instance.setIconSpin(false);
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
		} else if (name.startsWith("CloudPublisher")) {
			return IconType.CLOUD_UPLOAD;
		} else if (name.startsWith("Timer")) {
			return IconType.CLOCK_O;
		} else if (name.startsWith("RecordFilter")) {
			return IconType.FILTER;
		} else if (name.startsWith("RecordStore")) {
			return IconType.SAVE;
		} else if (name.startsWith("WireService")) {
			return IconType.CHAIN;
		} else {
			return null;
		}
	}

	private String getImagePath() {									
		String icon= item.getComponentIcon();
		String componentId= item.getComponentId();
		if (icon != null && 
				(icon.toLowerCase().startsWith("http://") || icon.toLowerCase().startsWith("https://")) &&
				isImagePath(icon)) { //Util.isImagePath(icon)
			return icon;
		} else if (icon != null && isImagePath(icon)) { //Util.isImagePath(icon)
			return SERVLET_URL + componentId;
		} else {
			return null;
		}
	}

	private boolean isImagePath(String icon) {
		boolean isPng= icon.toLowerCase().endsWith(".png");
		boolean isJpg= icon.toLowerCase().endsWith(".jpg");
		boolean isGif= icon.toLowerCase().endsWith(".gif");
		return isPng || isJpg || isGif;
	}
}

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
		if (name.equals("BluetoothService")) {
			return IconType.BTC;
		} else if (name.equals("CloudService")) {
			return IconType.CLOUD;
		} else if (name.equals("DiagnosticsService")) {
			return IconType.AMBULANCE;
		} else if (name.equals("ClockService")) {
			return IconType.CLOCK_O;
		} else if (name.equals("DataService")) {
			return IconType.DATABASE;
		} else if (name.equals("MqttDataTransport")) {
			return IconType.FORUMBEE;
		} else if (name.equals("PositionService")) {
			return IconType.LOCATION_ARROW;
		} else if (name.equals("WatchdogService")) {
			return IconType.HEARTBEAT;
		} else if (name.equals("SslManagerService")) {
			return IconType.LOCK;
		} else if (name.equals("VpnService")) {
			return IconType.CONNECTDEVELOP;
		} else if (name.equals("ProvisioningService")) {
			return IconType.EXCLAMATION_CIRCLE;
		} else if (name.equals("CommandPasswordService")) {
			return IconType.CHAIN;
		} else if (name.equals("WebConsole")) {
			return IconType.LAPTOP;
		} else if (name.equals("CommandService")) {
			return IconType.TERMINAL;
		} else if (name.equals("DenaliService")) {
			return IconType.SPINNER;
			//		} else if (icon != null && 
			//				(icon.toLowerCase().startsWith("http://") ||
			//       			     icon.toLowerCase().startsWith("https://")) &&
			//       				Util.isImagePath(icon)) {
			//       			return new ScaledAbstractImagePrototype(IconHelper.createPath(icon, 32, 32));
			//       		}
			//       		else if (icon != null &&
			//       				Util.isImagePath(icon)) {
			//       			return new ScaledAbstractImagePrototype(IconHelper.createPath(SERVLET_URL + model.get("componentId"), 32, 32));
			//       		}
		} else {
			return null; //IconType.CHEVRON_CIRCLE_RIGHT;
		}
	}

	private String getImagePath() {									
		String icon = item.getComponentIcon();
		if (icon != null && 
				(icon.toLowerCase().startsWith("http://") ||
						icon.toLowerCase().startsWith("https://")) //&&
				) { //Util.isImagePath(icon)
			return icon;
		}
		else if (icon != null //&&
				) { //Util.isImagePath(icon)
			return SERVLET_URL + icon;
		}
		else {
			return null;
		}
	}
}

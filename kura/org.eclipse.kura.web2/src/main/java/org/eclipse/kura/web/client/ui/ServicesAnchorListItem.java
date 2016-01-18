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

	EntryClassUi ui;
	GwtConfigComponent item;
	ServicesAnchorListItem instance;
	private static final Messages MSGS = GWT.create(Messages.class);

	public ServicesAnchorListItem(GwtConfigComponent service,
			EntryClassUi mainUi) {
		super();
		ui = mainUi;
		item = service;
		instance = this;

		super.setText(item.getComponentName());
		super.setIcon(getIcon(item.getComponentName()));
		super.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				if ((ui.selected != null && ui.selected != item && ui.servicesUi.isDirty())||(ui.isNetworkDirty())) {
					final Modal modal = new Modal();

					ModalHeader header = new ModalHeader();
					header.setTitle(MSGS.warning());
					modal.add(header);

					ModalBody body = new ModalBody();
					body.add(new Span(MSGS.deviceConfigDirty()));
					modal.add(body);

					
					ModalFooter footer = new ModalFooter();
					footer.add(new Button(MSGS.yesButton(),
							new ClickHandler() {
								@Override
								public void onClick(ClickEvent event) {
									ui.setDirty(false);
									ui.selected = item;
									modal.hide();
									instance.setIconSpin(true);
									ui.render(item);
									Timer timer = new Timer() {
										@Override
										public void run() {
											instance.setIconSpin(false);
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
					instance.setIconSpin(true);
					ui.render(item);
					Timer timer = new Timer() {
						@Override
						public void run() {
							instance.setIconSpin(false);
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
		} else {
			return IconType.CHEVRON_CIRCLE_RIGHT;
		}
	}

}

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
package org.eclipse.kura.web.client.ui.Network;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.ui.EntryClassUi;
import org.eclipse.kura.web.client.util.FailureHandler;
import org.eclipse.kura.web.shared.model.GwtNetIfConfigMode;
import org.eclipse.kura.web.shared.model.GwtNetInterfaceConfig;
import org.eclipse.kura.web.shared.model.GwtSession;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.service.GwtNetworkService;
import org.eclipse.kura.web.shared.service.GwtNetworkServiceAsync;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenService;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenServiceAsync;
import org.gwtbootstrap3.client.ui.Alert;
import org.gwtbootstrap3.client.ui.AnchorButton;
import org.gwtbootstrap3.client.ui.Modal;
import org.gwtbootstrap3.client.ui.html.Text;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.SelectionChangeEvent;

public class NetworkButtonBarUi extends Composite {

	private static final String IPV4_MODE_MANUAL_NAME = GwtNetIfConfigMode.netIPv4ConfigModeManual.name();
	
	private static NetworkButtonBarUiUiBinder uiBinder = GWT.create(NetworkButtonBarUiUiBinder.class);
	private static final Logger logger = Logger.getLogger(NetworkButtonBarUi.class.getSimpleName());

	interface NetworkButtonBarUiUiBinder extends UiBinder<Widget, NetworkButtonBarUi> {
	}

	private final GwtSecurityTokenServiceAsync gwtXSRFService = GWT.create(GwtSecurityTokenService.class);
	private final GwtNetworkServiceAsync gwtNetworkService = GWT.create(GwtNetworkService.class);
	
	private static final Messages MSGS = GWT.create(Messages.class);
	
	GwtSession session;
	NetworkInterfacesTableUi table;
	NetworkTabsUi tabs;

	@UiField
	AnchorButton apply, refresh;
	
	@UiField
	Modal incompleteFieldsModal;
	@UiField
	Alert incompleteFields;
	@UiField
	Text incompleteFieldsText;
	
	

	public NetworkButtonBarUi(GwtSession currentSession,
			NetworkTabsUi tabsPanel, NetworkInterfacesTableUi interfaces) {
		initWidget(uiBinder.createAndBindUi(this));
		this.session = currentSession;
		this.table = interfaces;
		this.tabs = tabsPanel;
		initButtons();
		initModal();
	}

	private void initButtons() {

		// Apply Button
		apply.setText(MSGS.apply());
		apply.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				if (!tabs.visibleTabs.isEmpty() && tabs.isValid()) {
					GwtNetInterfaceConfig prevNetIf = table.selectionModel.getSelectedObject();
					final GwtNetInterfaceConfig updatedNetIf = tabs.getUpdatedInterface();

					// submit updated netInterfaceConfig and priorities
					if (prevNetIf != null && prevNetIf.equals(updatedNetIf)) {
						table.refresh();
						apply.setEnabled(false);
					} else {
						String newNetwork = null;
						String prevNetwork = null;
						try {
							newNetwork = calculateNetwork(updatedNetIf.getIpAddress(), updatedNetIf.getSubnetMask());
							//prevNetwork = Window.Location.getHost();
							prevNetwork = calculateNetwork(Window.Location.getHost(), updatedNetIf.getSubnetMask());
						} catch (Exception e) {
							
						}

						if (newNetwork != null) {
							// if a static ip assigned, re-direct to the new
							// location
							if (    updatedNetIf.getConfigMode().equals(IPV4_MODE_MANUAL_NAME) && 
									newNetwork.equals(prevNetwork) && 
									Window.Location.getHost().equals(prevNetIf.getIpAddress()) ) {
								Timer t = new Timer() {
									@Override
									public void run() {
										Window.Location.replace("http://" + updatedNetIf.getIpAddress());
									}
								};
								t.schedule(500);
							}
						}

						EntryClassUi.showWaitModal();
						gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken> () {

							@Override
							public void onFailure(Throwable ex) {
								EntryClassUi.hideWaitModal();
								FailureHandler.handle(ex, NetworkButtonBarUi.class.getSimpleName());
							}

							@Override
							public void onSuccess(GwtXSRFToken token) {
								gwtNetworkService.updateNetInterfaceConfigurations(token, updatedNetIf, new AsyncCallback<Void>() {
											@Override
											public void onFailure(Throwable ex) {
												EntryClassUi.hideWaitModal();
												FailureHandler.handle(ex, NetworkButtonBarUi.class.getSimpleName());
											}

											@Override
											public void onSuccess(Void result) {
												EntryClassUi.hideWaitModal();
												tabs.setDirty(false);
												table.refresh();
												tabs.refresh();
												apply.setEnabled(false);
											}

										});
							}
							
						});
					}
				} else {
					logger.log(Level.FINER, MSGS.information() + ": " + MSGS.deviceConfigError());
					incompleteFieldsModal.show();
				}
			}

		});

		// Refresh Button
		refresh.setText(MSGS.refresh());
		refresh.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				table.refresh();
				tabs.setDirty(false);
				tabs.refresh();
				tabs.adjustInterfaceTabs();
			}
		});

		table.interfacesGrid.getSelectionModel().addSelectionChangeHandler(
				new SelectionChangeEvent.Handler() {
					@Override
					public void onSelectionChange(SelectionChangeEvent event) {
						apply.setEnabled(true);
					}
				});

		// TODO ?? how to detect changes
	}

	private String calculateNetwork(String ipAddress, String netmask) {
		if (	ipAddress == null   || 
				ipAddress.isEmpty() || 
				netmask == null     || 
				netmask.isEmpty()   ) {
			return null;
		}

		String network = null;

		try {
			int ipAddressValue = 0;
			int netmaskValue = 0;

			String[] sa = this.splitIp(ipAddress);

			for (int i = 24, t = 0; i >= 0; i -= 8, t++) {
				ipAddressValue = ipAddressValue | (Integer.parseInt(sa[t]) << i);
			}

			sa = this.splitIp(netmask);
			for (int i = 24, t = 0; i >= 0; i -= 8, t++) {
				netmaskValue = netmaskValue | (Integer.parseInt(sa[t]) << i);
			}

			network = dottedQuad(ipAddressValue & netmaskValue);
		} catch (Exception e) {
			logger.warning(e.getLocalizedMessage());
		}
		return network;
	}

	private String dottedQuad(int ip) {
		StringBuffer sb = new StringBuffer(15);
		for (int shift = 24; shift > 0; shift -= 8) {
			// process 3 bytes, from high order byte down.
			sb.append(Integer.toString((ip >>> shift) & 0xff));
			sb.append('.');
		}
		sb.append(Integer.toString(ip & 0xff));
		return sb.toString();
	}

	private String[] splitIp(String ip) {

		String sIp = new String(ip);
		String[] ret = new String[4];

		int ind = 0;
		for (int i = 0; i < 3; i++) {
			if ((ind = sIp.indexOf(".")) >= 0) {
				ret[i] = sIp.substring(0, ind);
				sIp = sIp.substring(ind + 1);
				if (i == 2) {
					ret[3] = sIp;
				}
			}
		}
		return ret;
	}
	
	private void initModal() {
		incompleteFieldsModal.setTitle(MSGS.warning());
		incompleteFieldsText.setText(MSGS.formWithErrorsOrIncomplete());
	}

}

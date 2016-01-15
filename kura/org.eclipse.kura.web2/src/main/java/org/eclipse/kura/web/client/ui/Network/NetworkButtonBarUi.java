package org.eclipse.kura.web.client.bootstrap.ui.Network;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.shared.model.GwtBSNetIfConfigMode;
import org.eclipse.kura.web.shared.model.GwtBSNetInterfaceConfig;
import org.eclipse.kura.web.shared.model.GwtBSSession;
import org.eclipse.kura.web.shared.service.GwtBSNetworkService;
import org.eclipse.kura.web.shared.service.GwtBSNetworkServiceAsync;
import org.gwtbootstrap3.client.ui.AnchorButton;
import org.gwtbootstrap3.extras.growl.client.ui.Growl;

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

	private static NetworkButtonBarUiUiBinder uiBinder = GWT
			.create(NetworkButtonBarUiUiBinder.class);

	interface NetworkButtonBarUiUiBinder extends
			UiBinder<Widget, NetworkButtonBarUi> {
	}

	private final GwtBSNetworkServiceAsync gwtNetworkService = GWT
			.create(GwtBSNetworkService.class);
	private static final Messages MSGS = GWT.create(Messages.class);
	GwtBSSession session;
	NetworkInterfacesTableUi table;
	NetworkTabsUi tabs;

	@UiField
	AnchorButton apply, refresh;

	public NetworkButtonBarUi(GwtBSSession currentSession,
			NetworkTabsUi tabsPanel, NetworkInterfacesTableUi interfaces) {
		initWidget(uiBinder.createAndBindUi(this));
		this.session = currentSession;
		this.table = interfaces;
		this.tabs = tabsPanel;
		initButtons();

	}

	private void initButtons() {

		// Apply Button
		apply.setText(MSGS.apply());
		apply.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				if (tabs.visibleTabs.size() > 0 && tabs.isValid()) {
					GwtBSNetInterfaceConfig prevNetIf = table.selectionModel
							.getSelectedObject();
					final GwtBSNetInterfaceConfig updatedNetIf = tabs
							.getUpdatedInterface();

					// submit updated netInterfaceConfig and priorities
					if (prevNetIf != null && prevNetIf.equals(updatedNetIf)) {
						table.refresh();
						apply.setEnabled(false);
					} else {
						String newNetwork = calculateNetwork(
								updatedNetIf.getIpAddress(),
								updatedNetIf.getSubnetMask());
						String prevNetwork = Window.Location.getHost();
						try {
							prevNetwork = calculateNetwork(
									Window.Location.getHost(),
									updatedNetIf.getSubnetMask());
						} catch (Exception e) {
							Growl.growl("Network detection failed for ipAddress: "
									+ Window.Location.getHost()
									+ ", and subnet: "
									+ updatedNetIf.getSubnetMask());
						}

						if (newNetwork != null) {
							// if a static ip assigned, re-direct to the new
							// location
							if (updatedNetIf
									.getConfigMode()
									.equals(GwtBSNetIfConfigMode.netIPv4ConfigModeManual
											.name())
									&& newNetwork.equals(prevNetwork)
									&& Window.Location.getHost().equals(
											prevNetIf.getIpAddress())) {
								Timer t = new Timer() {
									@Override
									public void run() {
										Growl.growl("redirecting to new address: "
												+ updatedNetIf.getIpAddress());
										Window.Location.replace("http://"
												+ updatedNetIf.getIpAddress());
									}
								};
								t.schedule(500);
							}
						}

						gwtNetworkService.updateNetInterfaceConfigurations(
								updatedNetIf, new AsyncCallback<Void>() {
									@Override
									public void onFailure(Throwable caught) {
										Growl.growl(MSGS.error() + ": ",
												caught.getLocalizedMessage());
									}

									@Override
									public void onSuccess(Void result) {
										Growl.growl("successfully updated net interface config");
										table.refresh();
										apply.setEnabled(false);
									}

								});
					}
				} else {
					Growl.growl(MSGS.information() + ": ",
							MSGS.deviceConfigError());
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
		if (ipAddress == null || ipAddress.isEmpty() || netmask == null
				|| netmask.isEmpty()) {
			return null;
		}

		String network = null;

		try {
			int ipAddressValue = 0;
			int netmaskValue = 0;

			String[] sa = this.splitIp(ipAddress);

			for (int i = 24, t = 0; i >= 0; i -= 8, t++) {
				ipAddressValue = ipAddressValue
						| (Integer.parseInt(sa[t]) << i);
			}

			sa = this.splitIp(netmask);
			for (int i = 24, t = 0; i >= 0; i -= 8, t++) {
				netmaskValue = netmaskValue | (Integer.parseInt(sa[t]) << i);
			}

			network = dottedQuad(ipAddressValue & netmaskValue);
		} catch (Exception e) {
			Growl.growl("Error calculating network for ip address: "
					+ ipAddress + " and netmask: " + netmask,
					e.getLocalizedMessage());
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

}

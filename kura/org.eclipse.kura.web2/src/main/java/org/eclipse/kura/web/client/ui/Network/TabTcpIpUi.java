package org.eclipse.kura.web.client.ui.Network;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.messages.ValidationMessages;
import org.eclipse.kura.web.client.ui.EntryClassUi;
import org.eclipse.kura.web.client.util.FailureHandler;
import org.eclipse.kura.web.client.util.MessageUtils;
import org.eclipse.kura.web.client.util.TextFieldValidator.FieldType;
import org.eclipse.kura.web.shared.model.GwtNetIfConfigMode;
import org.eclipse.kura.web.shared.model.GwtNetIfStatus;
import org.eclipse.kura.web.shared.model.GwtNetIfType;
import org.eclipse.kura.web.shared.model.GwtNetInterfaceConfig;
import org.eclipse.kura.web.shared.model.GwtSession;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.service.GwtNetworkService;
import org.eclipse.kura.web.shared.service.GwtNetworkServiceAsync;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenService;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenServiceAsync;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.Form;
import org.gwtbootstrap3.client.ui.FormControlStatic;
import org.gwtbootstrap3.client.ui.FormGroup;
import org.gwtbootstrap3.client.ui.FormLabel;
import org.gwtbootstrap3.client.ui.HelpBlock;
import org.gwtbootstrap3.client.ui.ListBox;
import org.gwtbootstrap3.client.ui.PanelBody;
import org.gwtbootstrap3.client.ui.PanelHeader;
import org.gwtbootstrap3.client.ui.TextBox;
import org.gwtbootstrap3.client.ui.constants.ValidationState;
import org.gwtbootstrap3.client.ui.html.Span;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

public class TabTcpIpUi extends Composite implements Tab {

	private static TabTcpIpUiUiBinder uiBinder = GWT.create(TabTcpIpUiUiBinder.class);
	private static final Logger logger = Logger.getLogger(TabTcpIpUi.class.getSimpleName());
	
	private static final Messages MSGS = GWT.create(Messages.class);
	private static final ValidationMessages VMSGS = GWT.create(ValidationMessages.class);
	
	private final GwtSecurityTokenServiceAsync gwtXSRFService = GWT.create(GwtSecurityTokenService.class);
	private final GwtNetworkServiceAsync gwtNetworkService = GWT.create(GwtNetworkService.class);

	interface TabTcpIpUiUiBinder extends UiBinder<Widget, TabTcpIpUi> {
	}

	GwtSession session;
	boolean m_dirty;
	GwtNetInterfaceConfig selectedNetIfConfig;
	NetworkTabsUi tabs;

	@UiField
	FormGroup groupIp, groupSubnet, groupGateway, groupDns;
	@UiField
	FormLabel labelStatus, labelConfigure, labelIp, labelSubnet, labelGateway,
			labelDns, labelSearch;
	@UiField
	HelpBlock helpIp, helpSubnet, helpGateway, helpDns;
	@UiField
	TextBox ip, subnet, gateway, dns, search;
	@UiField
	ListBox status, configure;
	@UiField
	Button renew;
	@UiField
	PanelHeader helpTitle;
	@UiField
	PanelBody helpText;
	@UiField
	Form form;
	@UiField
	FormControlStatic dnsRead;

	public TabTcpIpUi(GwtSession currentSession, NetworkTabsUi netTabs) {
		initWidget(uiBinder.createAndBindUi(this));
		session = currentSession;
		tabs = netTabs;
		helpTitle.setText("Help Text");
		initForm();
		dnsRead.setVisible(false);
	}

	@Override
	public void setDirty(boolean flag) {
		m_dirty = flag;
	}

	@Override
	public boolean isDirty() {
		return m_dirty;
	}

	@Override
	public void setNetInterface(GwtNetInterfaceConfig config) {

		
		if (config != null && config.getSubnetMask() != null && config.getSubnetMask().equals("255.255.255.255")) {
			config.setSubnetMask("");
		}
		setDirty(true);
		selectedNetIfConfig = config;
		logger.info(selectedNetIfConfig.getName());
		logger.info(selectedNetIfConfig.getConfigMode());
		logger.info(selectedNetIfConfig.getIpAddress());
		

		// Remove LAN option for modems
		if (selectedNetIfConfig != null	&& selectedNetIfConfig.getHwTypeEnum() == GwtNetIfType.MODEM) {
			if (status != null) {
				for (int i = 0; i < status.getItemCount(); i++) {
					if (status.getItemText(i).equals(MessageUtils.get(GwtNetIfStatus.netIPv4StatusEnabledLAN.name()))) {
						status.removeItem(i);

					}
				}
			}
		} else {
			if (status != null) {
				status.clear();
				status.addItem(MessageUtils.get("netIPv4StatusDisabled"));
				status.addItem(MessageUtils.get("netIPv4StatusEnabledLAN"));
				status.addItem(MessageUtils.get("netIPv4StatusEnabledWAN"));
			}
		}
	}

	public void getUpdatedNetInterface(GwtNetInterfaceConfig updatedNetIf) {
		if (form != null) {
			updatedNetIf.setStatus(status.getSelectedItemText());
			if (MessageUtils.get(GwtNetIfConfigMode.netIPv4ConfigModeDHCP.name()).equals(configure.getSelectedItemText())) {
				updatedNetIf.setConfigMode(GwtNetIfConfigMode.netIPv4ConfigModeDHCP.name());
			} else {
				updatedNetIf.setConfigMode(GwtNetIfConfigMode.netIPv4ConfigModeManual.name());
			}

			if (ip.getValue() != null) {
				updatedNetIf.setIpAddress(ip.getValue());
			} else {
				updatedNetIf.setIpAddress("");
			}
			if (subnet.getValue() != null) {
				updatedNetIf.setSubnetMask(subnet.getValue());
			} else {
				updatedNetIf.setSubnetMask("");
			}
			if (gateway.getValue() != null) {
				updatedNetIf.setGateway(gateway.getValue());
			} else {
				updatedNetIf.setGateway("");
			}
			if (dns.getValue() != null) {
				updatedNetIf.setDnsServers(dns.getValue());
			} else {
				updatedNetIf.setDnsServers("");
			}
			if (search.getValue() != null) {
				updatedNetIf.setSearchDomains(search.getValue());
			} else {
				updatedNetIf.setSearchDomains("");
			}
		}
	}

	public boolean isValid() {
		boolean flag = true;
		// check and make sure if 'Enabled for WAN' then either DHCP is selected
		// or STATIC and a gateway is set
		if (GwtNetIfStatus.netIPv4StatusEnabledWAN.equals(status
				.getSelectedValue())) {
			if (configure.getSelectedValue().equals(
					GwtNetIfConfigMode.netIPv4ConfigModeManual)) {
				if (gateway.getValue() == null
						|| gateway.getValue().trim().equals("")) {
					flag = false;
				}
			}
		}
		if (groupIp.getValidationState().equals(ValidationState.ERROR)
				|| groupSubnet.getValidationState().equals(
						ValidationState.ERROR)
				|| groupGateway.getValidationState().equals(
						ValidationState.ERROR)
				|| groupDns.getValidationState().equals(ValidationState.ERROR)) {
			flag = false;
		}
		return flag;
	}

	public boolean isLanEnabled() {
		if (status == null) {
			return false;
		}
		return GwtNetIfStatus.netIPv4StatusEnabledLAN.equals(status
				.getSelectedValue());
	}

	public boolean isWanEnabled() {
		if (status == null) {
			return false;
		}
		return GwtNetIfStatus.netIPv4StatusEnabledWAN.equals(status
				.getSelectedValue());
	}

	public String getStatus() {
		return status.getSelectedValue();
	}

	public boolean isDhcp() {
		if (configure == null) {
			logger.log(Level.FINER, "TcpIpConfigTab.isDhcp() - m_configureCombo is null");
			return true;
		}
		return (MessageUtils.get(GwtNetIfConfigMode.netIPv4ConfigModeDHCP
				.name()).equals(configure.getSelectedValue()));
	}

	@Override
	public void refresh() {
		if (isDirty()) {
			setDirty(false);
			if (selectedNetIfConfig == null) {
				reset();
			} else {
				update();
			}
		}
	}

	// ---------------Private Methods------------
	private void initForm() {

		// Labels
		labelStatus.setText(MSGS.netIPv4Status());
		labelConfigure.setText(MSGS.netIPv4Configure());
		labelIp.setText(MSGS.netIPv4Address());
		labelSubnet.setText(MSGS.netIPv4SubnetMask());
		labelGateway.setText(MSGS.netIPv4Gateway());
		labelDns.setText(MSGS.netIPv4DNSServers());
		labelSearch.setText(MSGS.netIPv4SearchDomains());

		for (GwtNetIfConfigMode mode : GwtNetIfConfigMode.values()) {
			configure.addItem(MessageUtils.get(mode.name()));
		}

		// Populate status list
		if (selectedNetIfConfig != null	&& selectedNetIfConfig.getHwTypeEnum() == GwtNetIfType.MODEM) {
			if (status != null) {
				status.clear();
				status.addItem(MessageUtils.get("netIPv4StatusDisabled"));
				status.addItem(MessageUtils.get("netIPv4StatusEnabledWAN"));
			}
		} else {
			if (status != null) {
				status.clear();
				status.addItem(MessageUtils.get("netIPv4StatusDisabled"));
				status.addItem(MessageUtils.get("netIPv4StatusEnabledLAN"));
				status.addItem(MessageUtils.get("netIPv4StatusEnabledWAN"));
			}
		}

		// SetTooltips

		// Status
		status.addMouseOverHandler(new MouseOverHandler() {
			@Override
			public void onMouseOver(MouseOverEvent event) {
				if (status.isEnabled()) {
					helpText.clear();
					helpText.add(new Span(MSGS.netIPv4ModemToolTipStatus()));
				}
			}
		});
		status.addMouseOutHandler(new MouseOutHandler() {
			@Override
			public void onMouseOut(MouseOutEvent event) {
				resetHelp();
			}
		});
		status.addChangeHandler(new ChangeHandler() {
			@Override
			public void onChange(ChangeEvent event) {
				setDirty(true);
				tabs.adjustInterfaceTabs();
				//refresh();
				// Check for other WAN interfaces if current interface is
				// changed to WAN
				if (isWanEnabled()) {
					EntryClassUi.showWaitModal();
					gwtNetworkService.findNetInterfaceConfigurations(new AsyncCallback<ArrayList<GwtNetInterfaceConfig>>() {
						public void onFailure(Throwable caught) {
							EntryClassUi.hideWaitModal();
							FailureHandler.handle(caught);
						}

						public void onSuccess(ArrayList<GwtNetInterfaceConfig> result) {
							for (GwtNetInterfaceConfig config : result) {
								if (config.getStatusEnum().equals(GwtNetIfStatus.netIPv4StatusEnabledWAN) && !config.getName().equals(selectedNetIfConfig.getName())) {
									logger.log(Level.SEVERE, "Error: Status Invalid");
									}
								}
							EntryClassUi.hideWaitModal();
							}

						});
				}
			}
		});

		// Configure
		configure.addMouseOverHandler(new MouseOverHandler() {
			@Override
			public void onMouseOver(MouseOverEvent event) {
				if (configure.isEnabled()) {
					helpText.clear();
					helpText.add(new Span(MSGS.netIPv4ToolTipConfigure()));
				}
			}
		});
		configure.addMouseOutHandler(new MouseOutHandler() {
			@Override
			public void onMouseOut(MouseOutEvent event) {
				resetHelp();
			}
		});
		configure.addChangeHandler(new ChangeHandler() {
			@Override
			public void onChange(ChangeEvent event) {
				setDirty(true);
				tabs.adjustInterfaceTabs();
				if (configure.getSelectedItemText().equalsIgnoreCase(VMSGS.netIPv4ConfigModeDHCP())) {
					// Using DHCP selected
					ip.setEnabled(false);
					subnet.setEnabled(false);
					gateway.setEnabled(false);
					renew.setEnabled(true);
					dnsRead.setVisible(true);
					dns.setVisible(false);

				} else if (configure.getSelectedItemText().equalsIgnoreCase(VMSGS.netIPv4ConfigModeManual())) {
					// Manually selected
					ip.setEnabled(true);
					subnet.setEnabled(true);
					gateway.setEnabled(true);
					renew.setEnabled(false);
					dns.setVisible(true);
					dnsRead.setVisible(false);
				}
			}
		});
		// Initial view of configure
		if (configure.getSelectedItemText().equalsIgnoreCase(VMSGS.netIPv4ConfigModeDHCP())) {
			// Using DHCP selected
			ip.setEnabled(false);
			subnet.setEnabled(false);
			gateway.setEnabled(false);
			renew.setEnabled(true);

		} else if (configure.getSelectedItemText().equalsIgnoreCase(VMSGS.netIPv4ConfigModeManual())) {
			// Manually selected
			ip.setEnabled(true);
			subnet.setEnabled(true);
			gateway.setEnabled(true);
			renew.setEnabled(false);
		}

		// IP Address
		ip.addMouseOverHandler(new MouseOverHandler() {
			@Override
			public void onMouseOver(MouseOverEvent event) {
				if (ip.isEnabled()) {
					helpText.clear();
					helpText.add(new Span(MSGS.netIPv4ToolTipAddress()));
				}
			}
		});
		ip.addMouseOutHandler(new MouseOutHandler() {
			@Override
			public void onMouseOut(MouseOutEvent event) {
				resetHelp();
			}
		});
		ip.addChangeHandler(new ChangeHandler() {
			@Override
			public void onChange(ChangeEvent event) {
				setDirty(true);
				if (!ip.getText().trim()
						.matches(FieldType.IPv4_ADDRESS.getRegex())
						&& ip.getText().trim().length() > 0) {
					groupIp.setValidationState(ValidationState.ERROR);
					helpIp.setText(MSGS.netIPv4InvalidAddress());
				} else {
					groupIp.setValidationState(ValidationState.NONE);
					helpIp.setText("");
				}
			}
		});

		// Subnet Mask
		subnet.addMouseOverHandler(new MouseOverHandler() {
			@Override
			public void onMouseOver(MouseOverEvent event) {
				if (subnet.isEnabled()) {
					helpText.clear();
					helpText.add(new Span(MSGS.netIPv4ToolTipSubnetMask()));
				}
			}
		});
		subnet.addMouseOutHandler(new MouseOutHandler() {
			@Override
			public void onMouseOut(MouseOutEvent event) {
				resetHelp();
			}
		});
		subnet.addChangeHandler(new ChangeHandler() {
			@Override
			public void onChange(ChangeEvent event) {
				setDirty(true);
				if (!subnet.getText().trim()
						.matches(FieldType.IPv4_ADDRESS.getRegex())
						&& subnet.getText().trim().length() > 0) {
					groupSubnet.setValidationState(ValidationState.ERROR);
					helpSubnet.setText(MSGS.netIPv4InvalidAddress());
				} else {
					groupSubnet.setValidationState(ValidationState.NONE);
					helpSubnet.setText("");
				}
			}
		});

		// Gateway
		gateway.addMouseOverHandler(new MouseOverHandler() {
			@Override
			public void onMouseOver(MouseOverEvent event) {
				if (gateway.isEnabled()) {
					helpText.clear();
					helpText.add(new Span(MSGS.netIPv4ToolTipGateway()));
				}
			}
		});
		gateway.addMouseOutHandler(new MouseOutHandler() {
			@Override
			public void onMouseOut(MouseOutEvent event) {
				resetHelp();
			}
		});
		gateway.addChangeHandler(new ChangeHandler() {
			@Override
			public void onChange(ChangeEvent event) {
				setDirty(true);
				if (!gateway.getText().trim()
						.matches(FieldType.IPv4_ADDRESS.getRegex())
						&& gateway.getText().trim().length() > 0) {
					groupGateway.setValidationState(ValidationState.ERROR);
					helpGateway.setText(MSGS.netIPv4InvalidAddress());
				} else {
					groupGateway.setValidationState(ValidationState.NONE);
					helpGateway.setText("");
				}
			}
		});

		// DNS Servers
		dns.addMouseOverHandler(new MouseOverHandler() {
			@Override
			public void onMouseOver(MouseOverEvent event) {
				if (dns.isEnabled()) {
					helpText.clear();
					helpText.add(new Span(MSGS.netIPv4ToolTipDns()));
				}
			}
		});
		dns.addMouseOutHandler(new MouseOutHandler() {
			@Override
			public void onMouseOut(MouseOutEvent event) {
				resetHelp();
			}
		});
		dns.addChangeHandler(new ChangeHandler() {
			@Override
			public void onChange(ChangeEvent event) {
				setDirty(true);
				if (!dns.getText().trim()
						.matches(FieldType.IPv4_ADDRESS.getRegex())
						&& dns.getText().trim().length() > 0) {
					groupDns.setValidationState(ValidationState.ERROR);
					helpDns.setText(MSGS.netIPv4InvalidAddress());
				} else {
					groupDns.setValidationState(ValidationState.NONE);
					helpDns.setText("");
				}
			}
		});

		// Renew DHCP Lease

		renew.setText(MSGS.netIPv4RenewDHCPLease());
		renew.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				EntryClassUi.showWaitModal();
				gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken> () {

					@Override
					public void onFailure(Throwable ex) {
						EntryClassUi.hideWaitModal();
						FailureHandler.handle(ex);
					}

					@Override
					public void onSuccess(GwtXSRFToken token) {
						gwtNetworkService.renewDhcpLease(token, selectedNetIfConfig.getName(),
								new AsyncCallback<Void>() {
									@Override
									public void onFailure(Throwable ex) {
										EntryClassUi.hideWaitModal();
										FailureHandler.handle(ex);
									}

									@Override
									public void onSuccess(Void result) {
										refresh();
										EntryClassUi.hideWaitModal();
									}
								});
					}
					
				});
			}
		});

		renew.addMouseOverHandler(new MouseOverHandler() {
			@Override
			public void onMouseOver(MouseOverEvent event) {
				if (renew.isEnabled()) {
					helpText.clear();
					helpText.add(new Span(MSGS.netIPv4ToolTipRenew()));
				}
			}
		});
		renew.addMouseOutHandler(new MouseOutHandler() {
			@Override
			public void onMouseOut(MouseOutEvent event) {
				resetHelp();
			}
		});

	}

	private void resetHelp() {
		helpText.clear();
		helpText.add(new Span("Mouse over enabled items on the left to see help text."));
	}

	private void update() {
		if (selectedNetIfConfig != null) {
			// Status
			int i = 0;
			logger.info(selectedNetIfConfig.getStatus());
			for (i = 0; i < status.getItemCount(); i++) {
				if (status.getValue(i).equals(selectedNetIfConfig.getStatus())) {
					status.setSelectedIndex(i);
					break;
				}
			}

			// Configure
			for (i = 0; i < configure.getItemCount(); i++) {
				if (configure.getValue(i).equals(selectedNetIfConfig.getConfigMode())) {
					configure.setSelectedIndex(i);
					break;
				}
			}

			tabs.adjustInterfaceTabs();

			ip.setText(selectedNetIfConfig.getIpAddress());
			subnet.setText(selectedNetIfConfig.getSubnetMask());
			gateway.setText(selectedNetIfConfig.getGateway());
			if (selectedNetIfConfig.getReadOnlyDnsServers() != null) {
				dnsRead.setText(selectedNetIfConfig.getReadOnlyDnsServers());
				dnsRead.setVisible(true);// ???
			} else {
				dnsRead.setText("");
				dnsRead.setVisible(false);
			}

			if (selectedNetIfConfig.getDnsServers() != null) {
				dns.setValue(selectedNetIfConfig.getDnsServers());
				dns.setVisible(true);
			} else {
				dns.setVisible(false);
			}

			if (selectedNetIfConfig.getSearchDomains() != null) {
				search.setText(selectedNetIfConfig.getSearchDomains());
			} else {
				search.setText("");
			}
			
			refreshForm();
		}
	}

	private void refreshForm() {

		if (selectedNetIfConfig != null	&& selectedNetIfConfig.getHwTypeEnum() == GwtNetIfType.MODEM) {
			status.setEnabled(true);
			configure.setEnabled(false);
			ip.setEnabled(false);
			subnet.setEnabled(false);
			gateway.setEnabled(false);
			dns.setEnabled(true);
			search.setEnabled(false);
			configure.setSelectedIndex(configure.getItemText(0).equals(
					GwtNetIfConfigMode.netIPv4ConfigModeDHCP) ? 0 : 1);

		} else {

			if (GwtNetIfStatus.netIPv4StatusDisabled.equals(status
					.getSelectedValue())) {
				configure.setSelectedIndex(configure.getItemText(0).equals(
						GwtNetIfConfigMode.netIPv4ConfigModeDHCP) ? 0 : 1);
				ip.setText("");
				status.setEnabled(false);
				configure.setEnabled(false);
				ip.setEnabled(false);
				subnet.setEnabled(false);
				gateway.setEnabled(false);
				dns.setEnabled(false);
				search.setEnabled(false);
				subnet.setText("");
				gateway.setText("");
				dns.setText("");
				search.setText("");
			} else {
				configure.setEnabled(true);
				String configureValue = configure.getSelectedValue();
				if (configureValue
						.equals(MessageUtils.get(GwtNetIfConfigMode.netIPv4ConfigModeDHCP.name()))) {
					ip.setEnabled(false);
					subnet.setEnabled(false);
					gateway.setEnabled(false);
					renew.setEnabled(true);
					;
				} else {
					ip.setEnabled(true);
					subnet.setEnabled(true);
					gateway.setEnabled(true);

					if (GwtNetIfStatus.netIPv4StatusEnabledWAN.equals(configureValue)) {
						// enable gateway field
						gateway.setEnabled(true);
					} else {
						gateway.setText("");
						gateway.setEnabled(false);
					}
					renew.setEnabled(false);
				}
				dns.setEnabled(true);
				search.setEnabled(true);

			}
		}

		// Show read-only dns field when DHCP is selected and there are no
		// custom DNS entries
		String configureValue = configure.getSelectedItemText();
		if (configureValue.equals(MessageUtils.get(GwtNetIfConfigMode.netIPv4ConfigModeDHCP.name()))
				&& (dns.getValue() == null || dns.getValue().isEmpty())) {
			dnsRead.setVisible(true);
		} else {
			dnsRead.setVisible(false);
		}

	}

	private void reset() {
		status.setSelectedIndex(0);
		configure.setSelectedIndex(0);
		ip.setText("");
		subnet.setText("");
		gateway.setText("");
		dns.setText("");
		search.setText("");
		update();
	}

}
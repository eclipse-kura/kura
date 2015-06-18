package org.eclipse.kura.web.client.bootstrap.ui.Network;

import java.util.ArrayList;
import java.util.Set;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.util.MessageUtils;
import org.eclipse.kura.web.shared.model.GwtBSGroupedNVPair;
import org.eclipse.kura.web.shared.model.GwtBSNetIfStatus;
import org.eclipse.kura.web.shared.model.GwtBSNetInterfaceConfig;
import org.eclipse.kura.web.shared.model.GwtBSSession;
import org.eclipse.kura.web.shared.model.GwtBSWifiBgscanModule;
import org.eclipse.kura.web.shared.model.GwtBSWifiChannelModel;
import org.eclipse.kura.web.shared.model.GwtBSWifiCiphers;
import org.eclipse.kura.web.shared.model.GwtBSWifiConfig;
import org.eclipse.kura.web.shared.model.GwtBSWifiHotspotEntry;
import org.eclipse.kura.web.shared.model.GwtBSWifiNetInterfaceConfig;
import org.eclipse.kura.web.shared.model.GwtBSWifiRadioMode;
import org.eclipse.kura.web.shared.model.GwtBSWifiWirelessMode;
import org.eclipse.kura.web.shared.model.GwtBSWifiSecurity;
import org.eclipse.kura.web.shared.service.GwtBSDeviceService;
import org.eclipse.kura.web.shared.service.GwtBSDeviceServiceAsync;
import org.eclipse.kura.web.shared.service.GwtBSNetworkService;
import org.eclipse.kura.web.shared.service.GwtBSNetworkServiceAsync;
import org.gwtbootstrap3.client.ui.Alert;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.FormGroup;
import org.gwtbootstrap3.client.ui.FormLabel;
import org.gwtbootstrap3.client.ui.HelpBlock;
import org.gwtbootstrap3.client.ui.Input;
import org.gwtbootstrap3.client.ui.ListBox;
import org.gwtbootstrap3.client.ui.Modal;
import org.gwtbootstrap3.client.ui.PanelBody;
import org.gwtbootstrap3.client.ui.PanelHeader;
import org.gwtbootstrap3.client.ui.RadioButton;
import org.gwtbootstrap3.client.ui.TextBox;
import org.gwtbootstrap3.client.ui.constants.ValidationState;
import org.gwtbootstrap3.client.ui.gwt.DataGrid;
import org.gwtbootstrap3.client.ui.html.Span;
import org.gwtbootstrap3.extras.growl.client.ui.Growl;
import org.gwtbootstrap3.extras.slider.client.ui.Slider;
import org.gwtbootstrap3.extras.slider.client.ui.base.event.SlideStartEvent;
import org.gwtbootstrap3.extras.slider.client.ui.base.event.SlideStartHandler;
import org.gwtbootstrap3.extras.slider.client.ui.base.event.SlideStopEvent;
import org.gwtbootstrap3.extras.slider.client.ui.base.event.SlideStopHandler;

import com.google.gwt.cell.client.CheckboxCell;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;

public class TabWirelessUi extends Composite implements Tab {

	private static TabWirelessUiUiBinder uiBinder = GWT
			.create(TabWirelessUiUiBinder.class);

	interface TabWirelessUiUiBinder extends UiBinder<Widget, TabWirelessUi> {
	}

	private static final Messages MSGS = GWT.create(Messages.class);

	private final GwtBSNetworkServiceAsync gwtNetworkService = GWT
			.create(GwtBSNetworkService.class);
	private final GwtBSDeviceServiceAsync gwtDeviceService = GWT
			.create(GwtBSDeviceService.class);

	private static final String REGEX_PASSWORD_ANY = ".*";
	private static final String REGEX_PASSWORD_WPA = "^[ -~]{8,63}$"; // //
																		// Match
																		// all
																		// ASCII
																		// printable
																		// characters
	private static final String REGEX_PASSWORD_WEP = "^(?:\\w{5}|\\w{13}|[a-fA-F0-9]{10}|[a-fA-F0-9]{26})$";
	private static final int MAX_WIFI_CHANNEL = 13;

	GwtBSSession session;
	TabTcpIpUi tcpTab;
	NetworkTabsUi netTabs;
	boolean dirty, ssidInit;
	GwtBSWifiNetInterfaceConfig selectedNetIfConfig;
	GwtBSWifiConfig activeConfig;

	@UiField
	DataGrid<GwtBSWifiChannelModel> channelGrid = new DataGrid<GwtBSWifiChannelModel>();
	private ListDataProvider<GwtBSWifiChannelModel> channelDataProvider = new ListDataProvider<GwtBSWifiChannelModel>();
	final SingleSelectionModel<GwtBSWifiChannelModel> selectionModel = new SingleSelectionModel<GwtBSWifiChannelModel>();
	@UiField
	Alert noChannels;

	@UiField
	FormLabel labelWireless, labelSsid, labelRadio, labelSecurity,
			labelPassword, labelVerify, labelPairwise, labelGroup, labelBgscan,
			labelRssi, labelShortI, labelLongI, labelPing, labelIgnore;
	@UiField
	RadioButton radio1, radio2, radio3, radio4;
	@UiField
	ListBox wireless, radio, security, pairwise, group, bgscan;
	@UiField
	TextBox ssid, shortI, longI;
	@UiField
	Input password, verify;
	@UiField
	Slider rssi;
	@UiField
	PanelHeader helpTitle;
	@UiField
	PanelBody helpText;
	@UiField
	Button buttonSsid, buttonPassword;
	@UiField
	FormGroup groupVerify, groupRssi, groupPassword, groupWireless,groupShortI,groupLongI;
	@UiField
	HelpBlock helpWireless, helpPassword,helpVerify;
	@UiField
	Modal ssidModal;
	@UiField
	PanelHeader ssidTitle;
	@UiField
	DataGrid<GwtBSWifiHotspotEntry> ssidGrid = new DataGrid<GwtBSWifiHotspotEntry>();
	private ListDataProvider<GwtBSWifiHotspotEntry> ssidDataProvider = new ListDataProvider<GwtBSWifiHotspotEntry>();
	final SingleSelectionModel<GwtBSWifiHotspotEntry> ssidSelectionModel = new SingleSelectionModel<GwtBSWifiHotspotEntry>();
	@UiField
	Alert noSsid;

	String passwordRegex, passwordError, tcpStatus;

	public TabWirelessUi(GwtBSSession currentSession, TabTcpIpUi tcp,
			NetworkTabsUi tabs) {
		ssidInit = false;
		initWidget(uiBinder.createAndBindUi(this));
		session = currentSession;
		tcpTab = tcp;
		netTabs = tabs;		
		initForm();
		setPasswordValidation();
		
		tcpTab.status.addChangeHandler(new ChangeHandler(){
			@Override
			public void onChange(ChangeEvent event) {
				if(selectedNetIfConfig!=null){
					//set the default values for wireless mode if tcp/ip status was changed
					String tcpIpStatus=tcpTab.getStatus();
					if(!tcpIpStatus.equals(tcpStatus)){
						if(GwtBSNetIfStatus.netIPv4StatusEnabledLAN.name().equals(tcpIpStatus)){
							activeConfig=selectedNetIfConfig.getAccessPointWifiConfig();
						}else{
							activeConfig=selectedNetIfConfig.getStationWifiConfig();
						}
						tcpStatus=tcpIpStatus;
						netTabs.adjustInterfaceTabs();
					}
				}
				refreshForm();
			}});
	}

	@UiHandler(value = { "wireless", "ssid", "radio", "security", "password",
			"verify", "pairwise", "group", "bgscan", "longI", "shortI",
			"radio1", "radio2", "radio3", "radio4" })
	public void onFormBlur(BlurEvent e) {
		setDirty(true);
	}
	
	@UiHandler(value={"rssi"})
	public void onValueChange(ValueChangeEvent<Double> event){
		setDirty(true);
	}

	public GwtBSWifiWirelessMode getWirelessMode() {		
		if (wireless != null) {
			for (GwtBSWifiWirelessMode mode : GwtBSWifiWirelessMode.values()) {
				if (mode.name().equals(wireless.getSelectedItemText())) {
					return mode;
				}
			}
		} else {
			if (activeConfig != null) {
				return GwtBSWifiWirelessMode.valueOf(activeConfig
						.getWirelessMode());
			}
		}
		return null;
	}

	@Override
	public void setDirty(boolean flag) {
		dirty = flag;
	}


	
	@Override
	public boolean isDirty() {
		return dirty;
	}

	public boolean isValid(){
		if(groupWireless.getValidationState().equals(ValidationState.ERROR)
				||groupPassword.getValidationState().equals(ValidationState.ERROR)
				||groupVerify.getValidationState().equals(ValidationState.ERROR)
				||groupRssi.getValidationState().equals(ValidationState.ERROR)
				||groupShortI.getValidationState().equals(ValidationState.ERROR)
				||groupLongI.getValidationState().equals(ValidationState.ERROR)){
			return false;
		}else{
			return true;
		}
	}
	
	@Override
	public void setNetInterface(GwtBSNetInterfaceConfig config) {

		if(tcpStatus==null || selectedNetIfConfig!=config){
			tcpStatus=tcpTab.getStatus();
		}
		if (config instanceof GwtBSWifiNetInterfaceConfig) {
			selectedNetIfConfig = (GwtBSWifiNetInterfaceConfig) config;
			activeConfig = selectedNetIfConfig.getActiveWifiConfig();
		}

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

	// -----Private methods-------//
	private void update() {
		setValues(true);
		refreshForm();
	}

	private void setValues(boolean b) {
		if (activeConfig != null) {

			for (int i = 0; i < wireless.getItemCount(); i++) {
				if (wireless.getItemText(i).equals(
						MessageUtils.get(activeConfig.getWirelessMode()))) {
					wireless.setSelectedIndex(i);
				}
			}

			ssid.setValue(activeConfig.getWirelessSsid());

			// ------------

			for (int i = 0; i < radio.getItemCount(); i++) {
				if (radio.getItemText(i).equals(
						MessageUtils.get(activeConfig.getRadioMode()))) {
					radio.setSelectedIndex(i);
				}
			}

			ArrayList<Integer> alChannels = activeConfig.getChannels();
			if (alChannels != null && alChannels.size() > 0) {
				// deselect all channels
				for (int i = 0; i <= MAX_WIFI_CHANNEL; i++) {
					// TODO ????
				}
			}

			for (int i = 0; i < security.getItemCount(); i++) {
				if (security.getItemText(i).equals(
						MessageUtils.get(activeConfig.getSecurity()))) {
					security.setSelectedIndex(i);
				}
			}

			for (int i = 0; i < pairwise.getItemCount(); i++) {
				if (pairwise.getItemText(i).equals(
						MessageUtils.get(activeConfig.getPairwiseCiphers()))) {
					pairwise.setSelectedIndex(i);
				}
			}

			for (int i = 0; i < group.getItemCount(); i++) {
				if (group.getItemText(i).equals(
						MessageUtils.get(activeConfig.getGroupCiphers()))) {
					group.setSelectedIndex(i);
				}
			}

			for (int i = 0; i < bgscan.getItemCount(); i++) {
				if (bgscan.getItemText(i).equals(
						MessageUtils.get(activeConfig.getBgscanModule()))) {
					bgscan.setSelectedIndex(i);
				}
			}

			rssi.setValue((double) activeConfig.getBgscanRssiThreshold());
			shortI.setValue(String.valueOf(activeConfig
					.getBgscanShortInterval()));
			longI.setValue(String.valueOf(activeConfig.getBgscanLongInterval()));
			password.setValue(activeConfig.getPassword());
			verify.setValue(activeConfig.getPassword());
			radio1.setActive(activeConfig.pingAccessPoint());
			radio2.setActive(!activeConfig.pingAccessPoint());

			radio3.setActive(activeConfig.ignoreSSID());
			radio4.setActive(!activeConfig.ignoreSSID());

		}
	}

	private void refreshForm() {
		String tcpipStatus = tcpTab.getStatus();

		// Tcp/IP disabled
		if (tcpipStatus.equals(GwtBSNetIfStatus.netIPv4StatusDisabled)) {
			setForm(false);
		} else {
			setForm(true);
			// Station mode
			if (GwtBSWifiWirelessMode.netWifiWirelessModeStation
					.equals(wireless.getSelectedItemText())) {
				setForm(true);
				radio.setEnabled(false);
				groupVerify.setVisible(false);
			} else if (GwtBSWifiWirelessMode.netWifiWirelessModeAccessPoint
					.equals(wireless.getSelectedItemText())) {
				// access point mode
				// disable access point when TCP/IP is set to WAN
				if (tcpipStatus
						.equals(GwtBSNetIfStatus.netIPv4StatusEnabledWAN)) {
					setForm(false);
				}
			}

			// disable Password if security is none
			if (security.getSelectedItemText().equals(
					MessageUtils.get(GwtBSWifiSecurity.netWifiSecurityNONE
							.name()))) {
				password.setEnabled(false);
				verify.setEnabled(false);
				buttonPassword.setEnabled(false);
			}

			if (GwtBSWifiWirelessMode.netWifiWirelessModeStation.name().equals(
					wireless.getSelectedItemText())) {
				ssid.setEnabled(true);
				if (security.getSelectedItemText().equals(
						MessageUtils.get(GwtBSWifiSecurity.netWifiSecurityNONE
								.name()))) {
					if (password.getValue() != null
							&& password.getValue().length() > 0) {
						password.setEnabled(true);
						buttonPassword.setEnabled(true);
					} else {
						password.setEnabled(true);
						buttonPassword.setEnabled(false);
					}
				}

				bgscan.setEnabled(true);

				if (bgscan
						.getSelectedItemText()
						.equals(MessageUtils
								.get(GwtBSWifiBgscanModule.netWifiBgscanMode_SIMPLE
										.name()))
						|| bgscan
								.getSelectedItemText()
								.equals(MessageUtils
										.get(GwtBSWifiBgscanModule.netWifiBgscanMode_LEARN
												.name()))) {
					shortI.setEnabled(true);
					longI.setEnabled(true);
					rssi.setEnabled(true);
				} else {
					shortI.setEnabled(false);
					longI.setEnabled(false);
					rssi.setEnabled(false);
				}
			} else {
				ssid.setEnabled(true);
				buttonSsid.setEnabled(false);
				if (security.getSelectedItemText().equals(
						MessageUtils.get(GwtBSWifiSecurity.netWifiSecurityNONE
								.name()))) {
					password.setEnabled(true);
					buttonPassword.setEnabled(false);
				}
				setForm(true);
				bgscan.setEnabled(false);
				rssi.setEnabled(false);
				shortI.setEnabled(false);
				longI.setEnabled(false);
				radio1.setEnabled(false);
				radio2.setEnabled(false);
			}

			if (security.getSelectedItemText().equals(
					MessageUtils.get(GwtBSWifiSecurity.netWifiSecurityWPA2
							.name()))
					|| security.getSelectedItemText().equals(
							MessageUtils
									.get(GwtBSWifiSecurity.netWifiSecurityWPA
											.name()))
					|| security
							.getSelectedItemText()
							.equals(MessageUtils
									.get(GwtBSWifiSecurity.netWifiSecurityWPA_WPA2
											.name()))) {
				if (GwtBSWifiWirelessMode.netWifiWirelessModeStation.name()
						.equals(wireless.getSelectedItemText())) {
					pairwise.setEnabled(true);
					group.setEnabled(true);
				} else {
					pairwise.setEnabled(false);
					group.setEnabled(false);
				}
			} else {
				pairwise.setEnabled(false);
				group.setEnabled(false);
			}
		}

		loadChannelData();
		netTabs.adjustInterfaceTabs();
	}

	private void reset() {

		for (int i = 0; i < wireless.getItemCount(); i++) {
			if (wireless
					.getSelectedItemText()
					.equals(MessageUtils
							.get(GwtBSWifiWirelessMode.netWifiWirelessModeStation
									.name()))) {
				wireless.setSelectedIndex(i);
			}
		}
		ssid.setText("");
		for (int i = 0; i < radio.getItemCount(); i++) {
			if (radio.getItemText(i).equals(
					MessageUtils.get(GwtBSWifiRadioMode.netWifiRadioModeBGN
							.name()))) {
				radio.setSelectedIndex(i);
			}
		}

		for (int i = 0; i < security.getItemCount(); i++) {
			if (security.getItemText(i).equals(
					MessageUtils.get(GwtBSWifiSecurity.netWifiSecurityWPA2
							.name()))) {
				security.setSelectedIndex(i);
			}
		}

		password.setText("");
		verify.setText("");

		for (int i = 0; i < pairwise.getItemCount(); i++) {
			if (pairwise.getItemText(i).equals(
					MessageUtils.get(GwtBSWifiCiphers.netWifiCiphers_CCMP_TKIP
							.name()))) {
				pairwise.setSelectedIndex(i);
			}
		}

		for (int i = 0; i < group.getItemCount(); i++) {
			if (group.getItemText(i).equals(
					MessageUtils.get(GwtBSWifiCiphers.netWifiCiphers_CCMP_TKIP
							.name()))) {
				group.setSelectedIndex(i);
			}
		}

		for (int i = 0; i < bgscan.getItemCount(); i++) {
			if (bgscan.getItemText(i).equals(
					MessageUtils
							.get(GwtBSWifiBgscanModule.netWifiBgscanMode_NONE
									.name()))) {
				bgscan.setSelectedIndex(i);
			}
		}

		rssi.setValue(0.0);
		shortI.setValue("");
		longI.setValue("");
		radio2.setActive(true);
		radio4.setActive(true);

		update();
	}

	private void initForm() {

		// Wireless Mode
		labelWireless.setText(MSGS.netWifiWirelessMode());
		wireless.addItem(MessageUtils.get("netWifiWirelessModeStation"));
		wireless.addItem(MessageUtils.get("netWifiWirelessModeAccessPoint"));
		wireless.addMouseOverHandler(new MouseOverHandler() {
			@Override
			public void onMouseOver(MouseOverEvent event) {
				if (wireless.getSelectedItemText().equals(
						MessageUtils.get("netWifiWirelessModeStation"))) {
					helpText.clear();
					helpText.add(new Span(MSGS
							.netWifiToolTipWirelessModeStation()));
				} else {
					helpText.clear();
					helpText.add(new Span(MSGS
							.netWifiToolTipWirelessModeAccessPoint()));
				}
			}
		});
		wireless.addMouseOutHandler(new MouseOutHandler() {
			@Override
			public void onMouseOut(MouseOutEvent event) {
				resetHelp();
			}
		});

		wireless.addChangeHandler(new ChangeHandler() {
			@Override
			public void onChange(ChangeEvent event) {
				if (tcpTab.getStatus().equals(
						GwtBSNetIfStatus.netIPv4StatusEnabledWAN)
						&& wireless
								.getSelectedItemText()
								.equals(MessageUtils
										.get(GwtBSWifiWirelessMode.netWifiWirelessModeAccessPoint
												.name()))) {
					helpWireless.setText(MSGS.netWifiWirelessEnabledForWANError());
					groupWireless.setValidationState(ValidationState.ERROR);
				}else{
					helpWireless.setText("");
					groupWireless.setValidationState(ValidationState.NONE);
				}

				if (MessageUtils
						.get(GwtBSWifiWirelessMode.netWifiWirelessModeStation
								.name()).equals(wireless.getSelectedItemText())) {
					// Use Values from station config
					activeConfig = selectedNetIfConfig.getStationWifiConfig();
				} else {
					// use values from access point config
					activeConfig = selectedNetIfConfig
							.getAccessPointWifiConfig();
				}
				setPasswordValidation();
				refreshForm();
			}

		});

		// SSID
		labelSsid.setText(MSGS.netWifiNetworkName());
		ssid.addMouseOverHandler(new MouseOverHandler() {
			@Override
			public void onMouseOver(MouseOverEvent event) {
				if (ssid.isEnabled()) {
					helpText.clear();
					helpText.add(new Span(MSGS.netWifiToolTipNetworkName()));
				}
			}
		});
		ssid.addMouseOutHandler(new MouseOutHandler() {
			@Override
			public void onMouseOut(MouseOutEvent event) {
				resetHelp();
			}
		});
		buttonSsid.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				if (!ssidInit) {
					initSsid();
				}
				initModal();
				loadSsidData();
			}
		});

		// Radio Mode
		labelRadio.setText(MSGS.netWifiRadioMode());
		radio.addMouseOverHandler(new MouseOverHandler() {
			@Override
			public void onMouseOver(MouseOverEvent event) {
				if (radio.isEnabled()) {
					helpText.clear();
					helpText.add(new Span(MSGS.netWifiToolTipRadioMode()));
				}
			}
		});
		radio.addMouseOutHandler(new MouseOutHandler() {
			@Override
			public void onMouseOut(MouseOutEvent event) {
				resetHelp();
			}
		});
		for (GwtBSWifiRadioMode mode : GwtBSWifiRadioMode.values()) {
			if (mode != GwtBSWifiRadioMode.netWifiRadioModeA) {
				// We don't support 802.11a yet
				radio.addItem(MessageUtils.get(mode.name()));
			}
		}

		// Wireless Security
		labelSecurity.setText(MSGS.netWifiWirelessSecurity());
		security.addMouseOverHandler(new MouseOverHandler() {
			@Override
			public void onMouseOver(MouseOverEvent event) {
				if (security.isEnabled()) {
					helpText.clear();
					helpText.add(new Span(MSGS.netWifiToolTipSecurity()));
				}
			}
		});
		security.addMouseOutHandler(new MouseOutHandler() {
			@Override
			public void onMouseOut(MouseOutEvent event) {
				resetHelp();
			}
		});
		for (GwtBSWifiSecurity mode : GwtBSWifiSecurity.values()) {
			security.addItem(MessageUtils.get(mode.name()));
		}
		security.addChangeHandler(new ChangeHandler() {
			@Override
			public void onChange(ChangeEvent event) {
				setPasswordValidation();
				refreshForm();
			}
		});

		// Password
		labelPassword.setText(MSGS.netWifiWirelessPassword());
		password.addMouseOverHandler(new MouseOverHandler() {
			@Override
			public void onMouseOver(MouseOverEvent event) {
				if (password.isEnabled()) {
					helpText.clear();
					helpText.add(new Span(MSGS.netWifiToolTipPassword()));
				}
			}
		});
		password.addMouseOutHandler(new MouseOutHandler() {
			@Override
			public void onMouseOut(MouseOutEvent event) {
				resetHelp();
			}
		});
		buttonPassword.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				buttonPassword.setEnabled(false);
				GwtBSWifiConfig gwtWifiConfig = getGwtWifiConfig();
				gwtNetworkService.verifyWifiCredentials(
						selectedNetIfConfig.getName(), gwtWifiConfig,
						new AsyncCallback<Boolean>() {
							@Override
							public void onFailure(Throwable caught) {
								Growl.growl(MSGS.warning() + ": ",
										caught.getLocalizedMessage());
								buttonPassword.setEnabled(true);
							}

							@Override
							public void onSuccess(Boolean result) {
								if (!result.booleanValue()) {
									password.setText("");
								}
								buttonPassword.setEnabled(true);
							}
						});
			}
		});
		password.addKeyUpHandler(new KeyUpHandler() {
			@Override
			public void onKeyUp(KeyUpEvent event) {
				if (!verify.getText().equals(password.getText())) {
					groupVerify.setValidationState(ValidationState.ERROR);
				} else {
					groupVerify.setValidationState(ValidationState.NONE);
				}
			}
		});
		password.addChangeHandler(new ChangeHandler() {
			@Override
			public void onChange(ChangeEvent event) {
				refreshForm();
				if (!password.getText().matches(passwordRegex)) {
					groupPassword.setValidationState(ValidationState.ERROR);
					helpPassword.setText(passwordError);
				} else {
					groupPassword.setValidationState(ValidationState.NONE);
					helpPassword.setText("");
				}
			}
		});

		// Verify Password
		labelVerify.setText(MSGS.netWifiWirelessVerifyPassword());
		verify.addMouseOverHandler(new MouseOverHandler() {
			@Override
			public void onMouseOver(MouseOverEvent event) {
				if (verify.isEnabled()) {
					helpText.clear();
					helpText.add(new Span(MSGS.netWifiToolTipPassword()));
				}
			}
		});
		verify.addMouseOutHandler(new MouseOutHandler() {
			@Override
			public void onMouseOut(MouseOutEvent event) {
				resetHelp();
			}
		});
		verify.addChangeHandler(new ChangeHandler() {
			@Override
			public void onChange(ChangeEvent event) {
				if (password != null
						&& !verify.getText().equals(password.getText())) {
					 helpVerify.setText(MSGS.netWifiWirelessPasswordDoesNotMatch());
					groupVerify.setValidationState(ValidationState.ERROR);

				} else {
					helpVerify.setText("");
					groupVerify.setValidationState(ValidationState.NONE);
				}
			}
		});

		// Pairwise ciphers
		labelPairwise.setText(MSGS.netWifiWirelessPairwiseCiphers());
		pairwise.addMouseOverHandler(new MouseOverHandler() {
			@Override
			public void onMouseOver(MouseOverEvent event) {
				if (pairwise.isEnabled()) {
					helpText.clear();
					helpText.add(new Span(MSGS.netWifiToolTipCiphers()));
				}
			}
		});
		pairwise.addMouseOutHandler(new MouseOutHandler() {
			@Override
			public void onMouseOut(MouseOutEvent event) {
				resetHelp();
			}
		});
		for (GwtBSWifiCiphers ciphers : GwtBSWifiCiphers.values()) {
			pairwise.addItem(MessageUtils.get(ciphers.name()));
		}
		pairwise.addChangeHandler(new ChangeHandler() {
			@Override
			public void onChange(ChangeEvent event) {
				refreshForm();
			}
		});

		// Groupwise Ciphers
		labelGroup.setText(MSGS.netWifiWirelessGroupCiphers());
		group.addMouseOverHandler(new MouseOverHandler() {
			@Override
			public void onMouseOver(MouseOverEvent event) {
				if (group.isEnabled()) {
					helpText.clear();
					helpText.add(new Span(MSGS.netWifiToolTipCiphers()));
				}
			}
		});
		group.addMouseOutHandler(new MouseOutHandler() {
			@Override
			public void onMouseOut(MouseOutEvent event) {
				resetHelp();
			}
		});
		for (GwtBSWifiCiphers ciphers : GwtBSWifiCiphers.values()) {
			group.addItem(MessageUtils.get(ciphers.name()));
		}
		group.addChangeHandler(new ChangeHandler() {
			@Override
			public void onChange(ChangeEvent event) {
				refreshForm();
			}
		});

		// Bgscan module
		labelBgscan.setText(MSGS.netWifiWirelessBgscanModule());
		bgscan.addMouseOverHandler(new MouseOverHandler() {
			@Override
			public void onMouseOver(MouseOverEvent event) {
				if (bgscan.isEnabled()) {
					helpText.clear();
					helpText.add(new Span(MSGS.netWifiToolTipBgScan()));
				}
			}
		});
		bgscan.addMouseOutHandler(new MouseOutHandler() {
			@Override
			public void onMouseOut(MouseOutEvent event) {
				resetHelp();
			}
		});
		for (GwtBSWifiBgscanModule module : GwtBSWifiBgscanModule.values()) {
			bgscan.addItem(MessageUtils.get(module.name()));
		}
		bgscan.addChangeHandler(new ChangeHandler() {
			@Override
			public void onChange(ChangeEvent event) {
				refreshForm();
			}
		});

		// BgScan RSSI threshold
		labelRssi.setText(MSGS.netWifiWirelessBgscanSignalStrengthThreshold());

		rssi.addSlideStartHandler(new SlideStartHandler<Double>() {
			@Override
			public void onSlideStart(SlideStartEvent<Double> event) {
				if (rssi.isEnabled()) {
					helpText.clear();
					helpText.add(new Span(MSGS.netWifiToolTipBgScanStrength()));
				}
			}
		});
		rssi.addSlideStopHandler(new SlideStopHandler<Double>() {
			@Override
			public void onSlideStop(SlideStopEvent<Double> event) {
				resetHelp();
			}
		});

		// Bgscan short Interval
		labelShortI.setText(MSGS.netWifiWirelessBgscanShortInterval());
		shortI.addMouseOverHandler(new MouseOverHandler() {
			@Override
			public void onMouseOver(MouseOverEvent event) {
				if (shortI.isEnabled()) {
					helpText.clear();
					helpText.add(new Span(MSGS
							.netWifiToolTipBgScanShortInterval()));
				}
			}
		});
		shortI.addMouseOutHandler(new MouseOutHandler() {
			@Override
			public void onMouseOut(MouseOutEvent event) {
				resetHelp();
			}
		});
		shortI.addChangeHandler(new ChangeHandler(){
			@Override
			public void onChange(ChangeEvent event){
				if(shortI.getText().trim().contains(".")||shortI.getText().trim().contains("-") || !shortI.getText().trim().matches("[0-9]+")){
					groupShortI.setValidationState(ValidationState.ERROR);
				}else{
					groupShortI.setValidationState(ValidationState.NONE);
				}
			}
		});

		// Bgscan long interval
		labelLongI.setText(MSGS.netWifiWirelessBgscanLongInterval());
		longI.addMouseOverHandler(new MouseOverHandler() {
			@Override
			public void onMouseOver(MouseOverEvent event) {
				if (longI.isEnabled()) {
					helpText.clear();
					helpText.add(new Span(MSGS
							.netWifiToolTipBgScanLongInterval()));
				}
			}
		});
		longI.addMouseOutHandler(new MouseOutHandler() {
			@Override
			public void onMouseOut(MouseOutEvent event) {
				resetHelp();
			}
		});
		longI.addChangeHandler(new ChangeHandler(){
			@Override
			public void onChange(ChangeEvent event){
				if(longI.getText().trim().contains(".")||longI.getText().trim().contains("-") || !longI.getText().trim().matches("[0-9]+")){
					groupLongI.setValidationState(ValidationState.ERROR);
				}else{
					groupLongI.setValidationState(ValidationState.NONE);
				}
			}
		});
		

		// Ping Access Point ----
		labelPing.setText(MSGS.netWifiWirelessPingAccessPoint());
		radio1.setText(MSGS.trueLabel());
		radio1.addMouseOverHandler(new MouseOverHandler() {
			@Override
			public void onMouseOver(MouseOverEvent event) {
				if (radio1.isEnabled()) {
					helpText.clear();
					helpText.add(new Span(MSGS.netWifiToolTipPingAccessPoint()));
				}
			}
		});
		radio1.addMouseOutHandler(new MouseOutHandler() {
			@Override
			public void onMouseOut(MouseOutEvent event) {
				resetHelp();
			}
		});
		radio2.setText(MSGS.falseLabel());
		radio2.addMouseOverHandler(new MouseOverHandler() {
			@Override
			public void onMouseOver(MouseOverEvent event) {
				if (radio2.isEnabled()) {
					helpText.clear();
					helpText.add(new Span(MSGS.netWifiToolTipPingAccessPoint()));
				}
			}
		});
		radio2.addMouseOutHandler(new MouseOutHandler() {
			@Override
			public void onMouseOut(MouseOutEvent event) {
				resetHelp();
			}
		});

		// Ignore Broadcast SSID
		labelIgnore.setText(MSGS.netWifiWirelessIgnoreSSID());
		radio3.setText(MSGS.trueLabel());
		radio3.addMouseOverHandler(new MouseOverHandler() {
			@Override
			public void onMouseOver(MouseOverEvent event) {
				if (radio3.isEnabled()) {
					helpText.clear();
					helpText.add(new Span(MSGS.netWifiToolTipIgnoreSSID()));
				}
			}
		});
		radio3.addMouseOutHandler(new MouseOutHandler() {
			@Override
			public void onMouseOut(MouseOutEvent event) {
				resetHelp();
			}
		});
		radio4.setText(MSGS.falseLabel());
		radio4.addMouseOverHandler(new MouseOverHandler() {
			@Override
			public void onMouseOver(MouseOverEvent event) {
				if (radio4.isEnabled()) {
					helpText.clear();
					helpText.add(new Span(MSGS.netWifiToolTipIgnoreSSID()));
				}
			}
		});
		radio4.addMouseOutHandler(new MouseOutHandler() {
			@Override
			public void onMouseOut(MouseOutEvent event) {
				resetHelp();
			}
		});

		// Channel Grid
		initGrid();

		helpTitle.setText("Help Text");

	}

	private void resetHelp() {
		helpText.clear();
		helpText.add(new Span(
				"Mouse over enabled items on the left to see help text."));
	}

	private void initGrid() {
		// CHECKBOXES
		Column<GwtBSWifiChannelModel, Boolean> checkColumn = new Column<GwtBSWifiChannelModel, Boolean>(
				new CheckboxCell()) {
			@Override
			public Boolean getValue(GwtBSWifiChannelModel object) {
				return channelGrid.getSelectionModel().isSelected(object);
			}

		};
		checkColumn
				.setFieldUpdater(new FieldUpdater<GwtBSWifiChannelModel, Boolean>() {
					@Override
					public void update(int index, GwtBSWifiChannelModel object,
							Boolean value) {
						channelGrid.getSelectionModel().setSelected(object,
								value);
						channelDataProvider.refresh();
					}
				});

		checkColumn.setCellStyleNames("status-table-row");
		channelGrid.addColumn(checkColumn);

		// ALL AVAILABLE CHANNELS
		TextColumn<GwtBSWifiChannelModel> col1 = new TextColumn<GwtBSWifiChannelModel>() {
			@Override
			public String getValue(GwtBSWifiChannelModel object) {
				return object.getName();
			}
		};
		col1.setCellStyleNames("status-table-row");
		channelGrid.addColumn(col1, "All Available Channels");

		// FREQUENCY
		TextColumn<GwtBSWifiChannelModel> col2 = new TextColumn<GwtBSWifiChannelModel>() {
			@Override
			public String getValue(GwtBSWifiChannelModel object) {
				return String.valueOf(object.getFrequency());
			}
		};
		col2.setCellStyleNames("status-table-row");
		channelGrid.addColumn(col2, "Frequency (MHz)");

		// SPECTRUM BAND
		TextColumn<GwtBSWifiChannelModel> col3 = new TextColumn<GwtBSWifiChannelModel>() {
			@Override
			public String getValue(GwtBSWifiChannelModel object) {
				return String.valueOf(object.getBand());
			}
		};
		col3.setCellStyleNames("status-table-row");
		channelGrid.addColumn(col3, "Frequency (MHz)");

		channelGrid.setSelectionModel(selectionModel);
		channelDataProvider.addDataDisplay(channelGrid);

		loadChannelData();

	}

	private void loadChannelData() {
		channelDataProvider.getList().clear();
		gwtDeviceService
				.findDeviceConfiguration(new AsyncCallback<ArrayList<GwtBSGroupedNVPair>>() {
					@Override
					public void onFailure(Throwable caught) {
						channelGrid.setVisible(false);
						Growl.growl(MSGS.error() + ": "
								+ caught.getLocalizedMessage());
					}

					@Override
					public void onSuccess(ArrayList<GwtBSGroupedNVPair> result) {
						if (result != null) {
							channelGrid.setVisible(true);
							for (GwtBSGroupedNVPair pair : result) {
								String name = pair.getName();
								if (name != null
										&& name.equals("devLastWifiChannel")) {
									int topChannel = Integer.parseInt(pair
											.getValue());
									// Remove channels 12 and 13
									if (topChannel < MAX_WIFI_CHANNEL) {
										channelDataProvider.getList().remove(
												MAX_WIFI_CHANNEL - 1);
										channelDataProvider.getList().remove(
												MAX_WIFI_CHANNEL - 2);
									}
								}
							}
							channelDataProvider.flush();
						}
					}

				});

		if (channelDataProvider.getList().size() > 0) {
			noChannels.setVisible(false);
			channelGrid.setVisible(true);
		} else {
			channelGrid.setVisible(false);
			noChannels.setVisible(true);
		}

	}

	private void setPasswordValidation() {

		if (security.getSelectedItemText() == MessageUtils
				.get(GwtBSWifiSecurity.netWifiSecurityWPA.name())) {
			passwordRegex = REGEX_PASSWORD_WPA;
			passwordError = MSGS.netWifiWirelessInvalidWPAPassword();
		} else if (security.getSelectedItemText() == MessageUtils
				.get(GwtBSWifiSecurity.netWifiSecurityWPA2.name())) {
			passwordRegex = REGEX_PASSWORD_WPA;
			passwordError = MSGS.netWifiWirelessInvalidWPAPassword();
		} else if (security.getSelectedItemText() == MessageUtils
				.get(GwtBSWifiSecurity.netWifiSecurityWEP.name())) {
			passwordRegex = REGEX_PASSWORD_WEP;
			passwordError = MSGS.netWifiWirelessInvalidWEPPassword();
		} else {
			passwordRegex = REGEX_PASSWORD_ANY;
		}

		if (password.getText() != null
				&& !password.getText().matches(passwordRegex)) {
			groupPassword.setValidationState(ValidationState.ERROR);
		} else {
			groupPassword.setValidationState(ValidationState.NONE);
		}
		if (password.getText() != null && verify.getText() != null
				&& !password.getText().equals(verify.getText())) {
			groupVerify.setValidationState(ValidationState.ERROR);
		} else {
			groupVerify.setValidationState(ValidationState.NONE);
		}
	}

	private void initModal() {
		ssidModal.setTitle("Wireless Networks");
		ssidTitle.setText("Available Networks in the Range");
		ssidModal.show();
	}

	private void initSsid() {

		ssidInit = true;
		TextColumn<GwtBSWifiHotspotEntry> col1 = new TextColumn<GwtBSWifiHotspotEntry>() {
			@Override
			public String getValue(GwtBSWifiHotspotEntry object) {
				return object.getSSID();
			}
		};
		col1.setCellStyleNames("status-table-row");
		ssidGrid.addColumn(col1, "SSID");

		TextColumn<GwtBSWifiHotspotEntry> col2 = new TextColumn<GwtBSWifiHotspotEntry>() {
			@Override
			public String getValue(GwtBSWifiHotspotEntry object) {
				return object.getMacAddress();
			}
		};
		col2.setCellStyleNames("status-table-row");
		ssidGrid.addColumn(col2, "MAC Address");

		TextColumn<GwtBSWifiHotspotEntry> col3 = new TextColumn<GwtBSWifiHotspotEntry>() {
			@Override
			public String getValue(GwtBSWifiHotspotEntry object) {
				return String.valueOf(object.getSignalStrength());
			}
		};
		col3.setCellStyleNames("status-table-row");
		ssidGrid.addColumn(col3, "Signal Strength (dBm)");

		TextColumn<GwtBSWifiHotspotEntry> col4 = new TextColumn<GwtBSWifiHotspotEntry>() {
			@Override
			public String getValue(GwtBSWifiHotspotEntry object) {
				return String.valueOf(object.getChannel());
			}
		};
		col4.setCellStyleNames("status-table-row");
		ssidGrid.addColumn(col4, "Channel");

		TextColumn<GwtBSWifiHotspotEntry> col5 = new TextColumn<GwtBSWifiHotspotEntry>() {
			@Override
			public String getValue(GwtBSWifiHotspotEntry object) {
				return String.valueOf(object.getFrequency());
			}
		};
		col5.setCellStyleNames("status-table-row");
		ssidGrid.addColumn(col5, "Frequency");

		TextColumn<GwtBSWifiHotspotEntry> col6 = new TextColumn<GwtBSWifiHotspotEntry>() {
			@Override
			public String getValue(GwtBSWifiHotspotEntry object) {
				return object.getSecurity();
			}
		};
		col6.setCellStyleNames("status-table-row");
		ssidGrid.addColumn(col6, "Security");
		ssidDataProvider.addDataDisplay(ssidGrid);

		ssidGrid.setSelectionModel(ssidSelectionModel);

		selectionModel
				.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
					@Override
					public void onSelectionChange(SelectionChangeEvent event) {
						GwtBSWifiHotspotEntry wifiHotspotEntry = ssidSelectionModel
								.getSelectedObject();
						if (wifiHotspotEntry != null) {
							ssid.setValue(wifiHotspotEntry.getSSID());
							String sec = wifiHotspotEntry.getSecurity();
							for (int i = 0; i < security.getItemCount(); i++) {
								if (sec.contains("WPA2")) {
									if (security.getItemText(i)
											.contains("WPA2")) {
										security.setSelectedIndex(i);
									}
								} else if (sec.equals(security.getItemText(i))) {
									security.setSelectedIndex(i);

								}
							}

							selectionModel.setSelected(
									channelDataProvider.getList().get(
											wifiHotspotEntry.getChannel() - 1),
									true);
							ssidModal.hide();
						}
					}
				});
		loadSsidData();
	}

	private void loadSsidData() {
		ssidDataProvider.getList().clear();
		if (selectedNetIfConfig != null) {
			gwtNetworkService.findWifiHotspots(selectedNetIfConfig.getName(),
					new AsyncCallback<ArrayList<GwtBSWifiHotspotEntry>>() {
						@Override
						public void onFailure(Throwable caught) {
							Growl.growl(MSGS.error() + ": ",
									caught.getLocalizedMessage());
						}

						@Override
						public void onSuccess(
								ArrayList<GwtBSWifiHotspotEntry> result) {
							for (GwtBSWifiHotspotEntry pair : result) {
								ssidDataProvider.getList().add(pair);
							}
							ssidDataProvider.flush();
						}
					});
		}

		if (ssidDataProvider.getList().size() > 0) {
			noSsid.setVisible(false);
			ssidGrid.setVisible(true);
		} else {
			noSsid.setVisible(true);
			ssidGrid.setVisible(false);
		}
	}

	private GwtBSWifiConfig getGwtWifiConfig() {
		GwtBSWifiConfig gwtWifiConfig = new GwtBSWifiConfig();

		// mode
		GwtBSWifiWirelessMode wifiMode;
		if (MessageUtils.get(
				GwtBSWifiWirelessMode.netWifiWirelessModeStation.name())
				.equals(wireless.getSelectedItemText())) {
			wifiMode = GwtBSWifiWirelessMode.netWifiWirelessModeStation;
		} else {
			wifiMode = GwtBSWifiWirelessMode.netWifiWirelessModeAccessPoint;
		}
		gwtWifiConfig.setWirelessMode(wifiMode.name());

		// ssid
		gwtWifiConfig.setWirelessSsid(ssid.getText().trim());

		// driver
		String driver = "";
		if (GwtBSWifiWirelessMode.netWifiWirelessModeAccessPoint
				.equals(wifiMode)) {
			driver = selectedNetIfConfig.getAccessPointWifiConfig().getDriver();
		} else if (GwtBSWifiWirelessMode.netWifiWirelessModeAdHoc
				.equals(wifiMode)) {
			driver = selectedNetIfConfig.getAdhocWifiConfig().getDriver();
		} else if (GwtBSWifiWirelessMode.netWifiWirelessModeStation
				.equals(wifiMode)) {
			driver = selectedNetIfConfig.getStationWifiConfig().getDriver();
		}
		gwtWifiConfig.setDriver(driver); // use previous value

		// radio mode
		String radioValue = radio.getSelectedItemText();
		for (GwtBSWifiRadioMode mode : GwtBSWifiRadioMode.values()) {
			if (MessageUtils.get(mode.name()).equals(radioValue)) {
				gwtWifiConfig.setRadioMode(mode.name());
			}
		}

		// channels
		Set<GwtBSWifiChannelModel> lSelectedChannels = selectionModel
				.getSelectedSet();

		ArrayList<Integer> alChannels = new ArrayList<Integer>();
		for (GwtBSWifiChannelModel item : lSelectedChannels) {
			alChannels.add(new Integer(item.getChannel()));
		}
		gwtWifiConfig.setChannels(alChannels);

		// security
		String secValue = security.getSelectedItemText();
		for (GwtBSWifiSecurity sec : GwtBSWifiSecurity.values()) {
			if (MessageUtils.get(sec.name()).equals(secValue)) {
				gwtWifiConfig.setSecurity(sec.name());
			}
		}

		// Pairwise Ciphers
		String pairWiseCiphersValue = pairwise.getSelectedItemText();
		for (GwtBSWifiCiphers ciphers : GwtBSWifiCiphers.values()) {
			if (MessageUtils.get(ciphers.name()).equals(pairWiseCiphersValue)) {
				gwtWifiConfig.setPairwiseCiphers(ciphers.name());
			}
		}

		// Group Ciphers value
		String groupCiphersValue = group.getSelectedItemText();
		for (GwtBSWifiCiphers ciphers : GwtBSWifiCiphers.values()) {
			if (MessageUtils.get(ciphers.name()).equals(groupCiphersValue)) {
				gwtWifiConfig.setGroupCiphers(ciphers.name());
			}
		}

		// bgscan
		String bgscanModuleValue = bgscan.getSelectedItemText();
		for (GwtBSWifiBgscanModule module : GwtBSWifiBgscanModule.values()) {
			if (MessageUtils.get(module.name()).equals(bgscanModuleValue)) {
				gwtWifiConfig.setBgscanModule(module.name());
			}
		}

		gwtWifiConfig.setBgscanRssiThreshold(rssi.getValue().intValue());
		gwtWifiConfig
				.setBgscanShortInterval(Integer.parseInt(shortI.getText()));
		gwtWifiConfig.setBgscanLongInterval(Integer.parseInt(longI.getText()));

		// password
		if (groupPassword.getValidationState().equals(ValidationState.NONE)) {
			gwtWifiConfig.setPassword(password.getText());
		}

		// ping access point
		gwtWifiConfig.setPingAccessPoint(radio1.isActive());

		// ignore SSID
		gwtWifiConfig.setIgnoreSSID(radio3.isActive());

		return activeConfig;
	}

	private void setForm(boolean b) {
		channelGrid.setVisible(b);
		wireless.setEnabled(b);
		ssid.setEnabled(b);
		buttonSsid.setEnabled(b);
		radio.setEnabled(b);
		security.setEnabled(b);
		password.setEnabled(b);
		buttonPassword.setEnabled(b);
		verify.setEnabled(b);
		pairwise.setEnabled(b);
		group.setEnabled(b);
		bgscan.setEnabled(b);
		rssi.setEnabled(b);
		shortI.setEnabled(b);
		longI.setEnabled(b);
		radio1.setEnabled(b);
		radio2.setEnabled(b);
		radio3.setEnabled(b);
		radio4.setEnabled(b);
		groupVerify.setVisible(false);
	}

	public void getUpdatedNetInterface(GwtBSNetInterfaceConfig updatedNetIf) {
		GwtBSWifiNetInterfaceConfig updatedWifiNetIf = (GwtBSWifiNetInterfaceConfig) updatedNetIf;
		
		if(session!=null){
			GwtBSWifiConfig updatedWifiConfig = getGwtWifiConfig();
			updatedWifiNetIf.setWirelessMode(updatedWifiConfig.getWirelessMode());
			
			//update the wifi config
			updatedWifiNetIf.setWifiConfig(updatedWifiConfig);
		}else{
			if(selectedNetIfConfig!=null){
				updatedWifiNetIf.setAccessPointWifiConfig(selectedNetIfConfig.getAccessPointWifiConfigProps());
				updatedWifiNetIf.setStationWifiConfig(selectedNetIfConfig.getStationWifiConfigProps());
				
				//select the correct mode
				for(GwtBSWifiWirelessMode mode: GwtBSWifiWirelessMode.values()){
					if(mode.name().equals(selectedNetIfConfig.getWirelessMode())){
						updatedWifiNetIf.setWirelessMode(mode.name());
					}
				}
			}
		}
	}
}
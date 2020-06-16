/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.web.client.ui.network;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.ui.EntryClassUi;
import org.eclipse.kura.web.client.ui.NewPasswordInput;
import org.eclipse.kura.web.client.ui.RegexValidator;
import org.eclipse.kura.web.client.util.FailureHandler;
import org.eclipse.kura.web.client.util.HelpButton;
import org.eclipse.kura.web.client.util.MessageUtils;
import org.eclipse.kura.web.shared.GwtSafeHtmlUtils;
import org.eclipse.kura.web.shared.model.GwtConsoleUserOptions;
import org.eclipse.kura.web.shared.model.GwtGroupedNVPair;
import org.eclipse.kura.web.shared.model.GwtNetIfStatus;
import org.eclipse.kura.web.shared.model.GwtNetInterfaceConfig;
import org.eclipse.kura.web.shared.model.GwtSession;
import org.eclipse.kura.web.shared.model.GwtWifiBgscanModule;
import org.eclipse.kura.web.shared.model.GwtWifiChannelModel;
import org.eclipse.kura.web.shared.model.GwtWifiCiphers;
import org.eclipse.kura.web.shared.model.GwtWifiConfig;
import org.eclipse.kura.web.shared.model.GwtWifiHotspotEntry;
import org.eclipse.kura.web.shared.model.GwtWifiNetInterfaceConfig;
import org.eclipse.kura.web.shared.model.GwtWifiRadioMode;
import org.eclipse.kura.web.shared.model.GwtWifiSecurity;
import org.eclipse.kura.web.shared.model.GwtWifiWirelessMode;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.service.GwtDeviceService;
import org.eclipse.kura.web.shared.service.GwtDeviceServiceAsync;
import org.eclipse.kura.web.shared.service.GwtNetworkService;
import org.eclipse.kura.web.shared.service.GwtNetworkServiceAsync;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenService;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenServiceAsync;
import org.gwtbootstrap3.client.ui.Alert;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.Form;
import org.gwtbootstrap3.client.ui.FormGroup;
import org.gwtbootstrap3.client.ui.FormLabel;
import org.gwtbootstrap3.client.ui.HelpBlock;
import org.gwtbootstrap3.client.ui.InlineRadio;
import org.gwtbootstrap3.client.ui.Input;
import org.gwtbootstrap3.client.ui.ListBox;
import org.gwtbootstrap3.client.ui.Modal;
import org.gwtbootstrap3.client.ui.ModalBody;
import org.gwtbootstrap3.client.ui.ModalFooter;
import org.gwtbootstrap3.client.ui.PanelHeader;
import org.gwtbootstrap3.client.ui.TextBox;
import org.gwtbootstrap3.client.ui.constants.ValidationState;
import org.gwtbootstrap3.client.ui.gwt.CellTable;
import org.gwtbootstrap3.client.ui.html.Span;
import org.gwtbootstrap3.client.ui.html.Text;

import com.google.gwt.cell.client.CheckboxCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.DomEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SingleSelectionModel;

public class TabWirelessUi extends Composite implements NetworkTab {

    private static final String NET_WIFI_WIRELESS_MODE_STATION = "netWifiWirelessModeStation";
    private static final String WIFI_MODE_STATION = GwtWifiWirelessMode.netWifiWirelessModeStation.name();
    private static final String WIFI_MODE_STATION_MESSAGE = MessageUtils.get(WIFI_MODE_STATION);
    private static final String WIFI_SECURITY_WEP_MESSAGE = MessageUtils.get(GwtWifiSecurity.netWifiSecurityWEP.name());
    private static final String WIFI_SECURITY_WPA_MESSAGE = MessageUtils.get(GwtWifiSecurity.netWifiSecurityWPA.name());
    private static final String WIFI_SECURITY_WPA2_MESSAGE = MessageUtils
            .get(GwtWifiSecurity.netWifiSecurityWPA2.name());
    private static final String WIFI_SECURITY_WPA_WPA2_MESSAGE = MessageUtils
            .get(GwtWifiSecurity.netWifiSecurityWPA_WPA2.name());
    private static final String WIFI_BGSCAN_NONE_MESSAGE = MessageUtils
            .get(GwtWifiBgscanModule.netWifiBgscanMode_NONE.name());
    private static final String WIFI_CIPHERS_CCMP_TKIP_MESSAGE = MessageUtils
            .get(GwtWifiCiphers.netWifiCiphers_CCMP_TKIP.name());
    private static final String WIFI_RADIO_BGN_MESSAGE = MessageUtils.get(GwtWifiRadioMode.netWifiRadioModeBGN.name());
    private static final String WIFI_SECURITY_NONE_MESSAGE = MessageUtils
            .get(GwtWifiSecurity.netWifiSecurityNONE.name());
    private static final String IPV4_STATUS_WAN_MESSAGE = MessageUtils
            .get(GwtNetIfStatus.netIPv4StatusEnabledWAN.name());
    private static final String WIFI_MODE_ACCESS_POINT_MESSAGE = MessageUtils
            .get(GwtWifiWirelessMode.netWifiWirelessModeAccessPoint.name());

    private static TabWirelessUiUiBinder uiBinder = GWT.create(TabWirelessUiUiBinder.class);
    private static final Logger logger = Logger.getLogger(TabWirelessUi.class.getSimpleName());

    interface TabWirelessUiUiBinder extends UiBinder<Widget, TabWirelessUi> {
    }

    private static final Messages MSGS = GWT.create(Messages.class);

    private final GwtSecurityTokenServiceAsync gwtXSRFService = GWT.create(GwtSecurityTokenService.class);
    private final GwtNetworkServiceAsync gwtNetworkService = GWT.create(GwtNetworkService.class);
    private final GwtDeviceServiceAsync gwtDeviceService = GWT.create(GwtDeviceService.class);

    private static final String REGEX_PASSWORD_WPA = "^[ -~]{8,63}$";
    private static final String REGEX_PASSWORD_WEP = "^(?:[\\x00-\\x7F]{5}|[\\x00-\\x7F]{13}|[a-fA-F0-9]{10}|[a-fA-F0-9]{26})$";
    private static final int MAX_WIFI_CHANNEL = 14;
    private static final int MAX_SSID_LENGTH = 32;

    private final GwtSession session;
    private final TabTcpIpUi tcpTab;
    private final NetworkTabsUi netTabs;
    private final ListDataProvider<GwtWifiHotspotEntry> ssidDataProvider = new ListDataProvider<>();
    private final SingleSelectionModel<GwtWifiHotspotEntry> ssidSelectionModel = new SingleSelectionModel<>();
    private final ListDataProvider<GwtWifiChannelModel> channelDataProvider = new ListDataProvider<>();
    private final SingleSelectionModel<GwtWifiChannelModel> selectionModel = new SingleSelectionModel<>();

    private boolean dirty;
    private boolean ssidInit;
    private GwtWifiNetInterfaceConfig selectedNetIfConfig;
    private String tcpStatus;

    GwtWifiConfig activeConfig;

    @UiField
    CellTable<GwtWifiChannelModel> channelGrid = new CellTable<>();
    @UiField
    Alert noChannels;
    @UiField
    Text noChannelsText;
    
    @UiField
    Form form;

    @UiField
    FormLabel labelWireless;
    @UiField
    FormLabel labelSsid;
    @UiField
    FormLabel labelRadio;
    @UiField
    FormLabel labelSecurity;
    @UiField
    FormLabel labelPassword;
    @UiField
    FormLabel labelVerify;
    @UiField
    FormLabel labelPairwise;
    @UiField
    FormLabel labelGroup;
    @UiField
    FormLabel labelBgscan;
    @UiField
    FormLabel labelRssi;
    @UiField
    FormLabel labelShortI;
    @UiField
    FormLabel labelLongI;
    @UiField
    FormLabel labelPing;
    @UiField
    FormLabel labelIgnore;

    @UiField
    InlineRadio radio1;
    @UiField
    InlineRadio radio2;
    @UiField
    InlineRadio radio3;
    @UiField
    InlineRadio radio4;

    @UiField
    ListBox wireless;
    @UiField
    ListBox radio;
    @UiField
    ListBox security;
    @UiField
    ListBox pairwise;
    @UiField
    ListBox group;
    @UiField
    ListBox bgscan;

    @UiField
    TextBox ssid;
    @UiField
    TextBox shortI;
    @UiField
    TextBox longI;

    @UiField
    NewPasswordInput password;
    @UiField
    Input verify;

    @UiField
    TextBox rssi;

    @UiField
    PanelHeader helpTitle;

    @UiField
    ScrollPanel helpText;

    @UiField
    Button buttonSsid;
    @UiField
    Button buttonPassword;

    @UiField
    FormGroup groupVerify;
    @UiField
    FormGroup groupRssi;
    @UiField
    FormGroup groupPassword;
    @UiField
    FormGroup groupWireless;
    @UiField
    FormGroup groupShortI;
    @UiField
    FormGroup groupLongI;

    @UiField
    HelpBlock helpWireless;
    @UiField
    HelpBlock helpPassword;
    @UiField
    HelpBlock helpVerify;

    @UiField
    Modal ssidModal;

    @UiField
    PanelHeader ssidTitle;

    @UiField
    CellTable<GwtWifiHotspotEntry> ssidGrid = new CellTable<>();

    @UiField
    Alert searching;
    @UiField
    Alert noSsid;
    @UiField
    Alert scanFail;

    @UiField
    Text searchingText;
    @UiField
    Text noSsidText;
    @UiField
    Text scanFailText;

    @UiField
    HelpButton wirelessHelp;
    @UiField
    HelpButton ssidHelp;
    @UiField
    HelpButton radioHelp;
    @UiField
    HelpButton securityHelp;
    @UiField
    HelpButton passwordHelp;
    @UiField
    HelpButton verifyHelp;
    @UiField
    HelpButton pairwiseHelp;
    @UiField
    HelpButton groupHelp;
    @UiField
    HelpButton bgscanHelp;
    @UiField
    HelpButton rssiHelp;
    @UiField
    HelpButton shortIHelp;
    @UiField
    HelpButton longIHelp;
    @UiField
    HelpButton pingHelp;
    @UiField
    HelpButton ignoreHelp;

    public TabWirelessUi(GwtSession currentSession, TabTcpIpUi tcp, NetworkTabsUi tabs) {
        this.ssidInit = false;
        initWidget(uiBinder.createAndBindUi(this));
        this.session = currentSession;
        this.tcpTab = tcp;
        this.netTabs = tabs;
        initForm();
        initHelpButtons();
        setPasswordValidation();

        this.tcpTab.status.addChangeHandler(event -> {
            if (TabWirelessUi.this.selectedNetIfConfig != null) {
                // set the default values for wireless mode if tcp/ip status was changed
                String tcpIpStatus = TabWirelessUi.this.tcpTab.getStatus();
                if (!tcpIpStatus.equals(TabWirelessUi.this.tcpStatus)) {
                    if (tcpIpStatus.equals(MessageUtils.get(GwtNetIfStatus.netIPv4StatusEnabledWAN.name()))) {
                        TabWirelessUi.this.activeConfig = TabWirelessUi.this.selectedNetIfConfig.getStationWifiConfig();
                    } else {
                        TabWirelessUi.this.activeConfig = TabWirelessUi.this.selectedNetIfConfig.getActiveWifiConfig();
                    }
                    TabWirelessUi.this.tcpStatus = tcpIpStatus;
                    TabWirelessUi.this.netTabs.adjustInterfaceTabs();
                }
            }
            update();
        });
    }

    @UiHandler(value = { "wireless", "ssid", "radio", "security", "password", "verify", "pairwise", "group", "bgscan",
            "longI", "shortI", "radio1", "radio2", "radio3", "radio4", "rssi" })
    public void onFormBlur(BlurEvent e) {
        setDirty(true);
    }

    public GwtWifiWirelessMode getWirelessMode() {
        if (this.wireless != null) {
            for (GwtWifiWirelessMode mode : GwtWifiWirelessMode.values()) {
                if (this.wireless.getSelectedItemText().equals(MessageUtils.get(mode.name()))) {
                    return mode;
                }
            }
        } else {
            if (this.activeConfig != null) {
                return GwtWifiWirelessMode.valueOf(this.activeConfig.getWirelessMode());
            }
        }
        return null;
    }

    @Override
    public void setDirty(boolean flag) {
        this.dirty = flag;
    }

    @Override
    public boolean isDirty() {
        return this.dirty;
    }

    @Override
    public boolean isValid() {
        return isValidForm();
    }

    private boolean isValidForm() {
        boolean result = form.validate();
        result = result && !this.groupWireless.getValidationState().equals(ValidationState.ERROR)
                && !this.groupPassword.getValidationState().equals(ValidationState.ERROR)
                && !this.groupVerify.getValidationState().equals(ValidationState.ERROR);

        result = result && !this.groupRssi.getValidationState().equals(ValidationState.ERROR)
                && !this.groupShortI.getValidationState().equals(ValidationState.ERROR)
                && !this.groupLongI.getValidationState().equals(ValidationState.ERROR);
        
        return result;
    }

    @Override
    public void setNetInterface(GwtNetInterfaceConfig config) {
        setDirty(true);
        if (this.tcpStatus == null || this.selectedNetIfConfig != config) {
            this.tcpStatus = this.tcpTab.getStatus();
        }
        if (config instanceof GwtWifiNetInterfaceConfig) {
            this.selectedNetIfConfig = (GwtWifiNetInterfaceConfig) config;
            this.activeConfig = this.selectedNetIfConfig.getActiveWifiConfig();
        }

    }

    @Override
    public void refresh() {
        if (isDirty()) {
            setDirty(false);
            if (this.selectedNetIfConfig == null) {
                reset();
            } else {
                update();
            }
        }

    }

    @Override
    public void getUpdatedNetInterface(GwtNetInterfaceConfig updatedNetIf) {
        GwtWifiNetInterfaceConfig updatedWifiNetIf = (GwtWifiNetInterfaceConfig) updatedNetIf;

        if (this.session != null) {
            GwtWifiConfig updatedWifiConfig = getGwtWifiConfig();
            updatedWifiNetIf.setWirelessMode(updatedWifiConfig.getWirelessMode());

            // update the wifi config
            updatedWifiNetIf.setWifiConfig(updatedWifiConfig);
        } else {
            if (this.selectedNetIfConfig != null) {
                updatedWifiNetIf.setAccessPointWifiConfig(this.selectedNetIfConfig.getAccessPointWifiConfigProps());
                updatedWifiNetIf.setStationWifiConfig(this.selectedNetIfConfig.getStationWifiConfigProps());

                // select the correct mode
                for (GwtWifiWirelessMode mode : GwtWifiWirelessMode.values()) {
                    if (mode.name().equals(this.selectedNetIfConfig.getWirelessMode())) {
                        updatedWifiNetIf.setWirelessMode(mode.name());
                    }
                }
            }
        }
    }

    // -----Private methods-------//

    private void update() {
        setValues();
        refreshForm();
        setPasswordValidation();
    }

    private void setValues() {
        if (this.activeConfig == null) {
            return;
        }

        for (int i = 0; i < this.wireless.getItemCount(); i++) {
            if (this.wireless.getItemText(i).equals(MessageUtils.get(this.activeConfig.getWirelessMode()))) {
                this.wireless.setSelectedIndex(i);
            }
        }

        this.ssid.setValue(GwtSafeHtmlUtils.htmlUnescape(this.activeConfig.getWirelessSsid()));

        // ------------

        String activeRadioMode = this.activeConfig.getRadioMode();
        if (activeRadioMode != null) {
            for (int i = 0; i < this.radio.getItemCount(); i++) {
                if (this.radio.getItemText(i).equals(MessageUtils.get(activeRadioMode))) {
                    this.radio.setSelectedIndex(i);
                    break;
                }
            }
        }

        List<Integer> alChannels = this.activeConfig.getChannels();
        int channelListSize = this.channelDataProvider.getList().size();
        int maxIndex = Math.min(channelListSize, MAX_WIFI_CHANNEL);
        if (alChannels != null && !alChannels.isEmpty()) {
            // deselect all channels
            for (int channel = 1; channel <= maxIndex; channel++) {
                this.selectionModel.setSelected(this.channelDataProvider.getList().get(channel - 1), false);
            }
            // select proper channels
            for (int channel : alChannels) {
                if (channel <= maxIndex) {
                    this.selectionModel.setSelected(this.channelDataProvider.getList().get(channel - 1), true);
                }
            }
        } else {
            logger.info("No channels specified, selecting all ...");
            for (int channel = 1; channel <= maxIndex; channel++) {
                this.selectionModel.setSelected(this.channelDataProvider.getList().get(channel - 1), true);
            }
        }

        String activeSecurity = this.activeConfig.getSecurity();
        if (activeSecurity != null) {
            for (int i = 0; i < this.security.getItemCount(); i++) {
                if (this.security.getItemText(i).equals(MessageUtils.get(activeSecurity))) {
                    this.security.setSelectedIndex(i);
                    break;
                }
            }
        }

        String activePairwiseCiphers = this.activeConfig.getPairwiseCiphers();
        if (activePairwiseCiphers != null) {
            for (int i = 0; i < this.pairwise.getItemCount(); i++) {
                if (this.pairwise.getItemText(i).equals(MessageUtils.get(activePairwiseCiphers))) {
                    this.pairwise.setSelectedIndex(i);
                    break;
                }
            }
        }

        String activeGroupCiphers = this.activeConfig.getPairwiseCiphers();
        if (activeGroupCiphers != null) {
            for (int i = 0; i < this.group.getItemCount(); i++) {
                if (this.group.getItemText(i).equals(MessageUtils.get(activeGroupCiphers))) { // activeConfig.getGroupCiphers()
                    this.group.setSelectedIndex(i);
                    break;
                }
            }
        }

        String activeBgscanModule = this.activeConfig.getBgscanModule();
        if (activeBgscanModule != null) {
            for (int i = 0; i < this.bgscan.getItemCount(); i++) {
                if (this.bgscan.getItemText(i).equals(MessageUtils.get(activeBgscanModule))) {
                    this.bgscan.setSelectedIndex(i);
                    break;
                }
            }
        }

        this.rssi.setValue("90");
        this.shortI.setValue(String.valueOf(this.activeConfig.getBgscanShortInterval()));
        this.longI.setValue(String.valueOf(this.activeConfig.getBgscanLongInterval()));
        this.password.setValue(this.activeConfig.getPassword());
        this.verify.setValue(this.activeConfig.getPassword());
        this.radio1.setValue(this.activeConfig.pingAccessPoint());
        this.radio2.setValue(!this.activeConfig.pingAccessPoint());

        this.radio3.setValue(this.activeConfig.ignoreSSID());
        this.radio4.setValue(!this.activeConfig.ignoreSSID());

    }

    private void refreshForm() {
        logger.info("refreshForm()");
        String tcpipStatus = this.tcpTab.getStatus();

        // Tcp/IP disabled
        if (tcpipStatus.equals(GwtNetIfStatus.netIPv4StatusDisabled.name())) {
            setForm(false);
        } else {
            setForm(true);
            // Station mode
            if (WIFI_MODE_STATION_MESSAGE.equals(this.wireless.getSelectedItemText())) {  // TODO: take a look at the
                // logic
                // here and at next if: couldn't it
                // be unified?
                if (tcpipStatus.equals(IPV4_STATUS_WAN_MESSAGE)) {
                    this.wireless.setEnabled(false);
                }
                this.radio.setEnabled(false);
                this.groupVerify.setVisible(false);
            } else if (WIFI_MODE_ACCESS_POINT_MESSAGE.equals(this.wireless.getSelectedItemText())) {
                // access point mode
                // disable access point when TCP/IP is set to WAN
                if (tcpipStatus.equals(IPV4_STATUS_WAN_MESSAGE)) {
                    setForm(false);
                }
                this.radio.setEnabled(true);
                this.groupVerify.setVisible(true);
            }

            // disable Password if security is none
            if (this.security.getSelectedItemText().equals(WIFI_SECURITY_NONE_MESSAGE)) {
                this.password.setEnabled(false);
                this.verify.setEnabled(false);
                this.buttonPassword.setEnabled(false);
            }

            if (WIFI_MODE_STATION_MESSAGE.equals(this.wireless.getSelectedItemText())) {
                this.ssid.setEnabled(true);
                this.verify.setEnabled(false);
                if (!this.security.getSelectedItemText().equals(WIFI_SECURITY_NONE_MESSAGE)) {
                    if (this.password.getValue() != null && this.password.getValue().length() > 0) {
                        this.password.setEnabled(true);
                        this.buttonPassword.setEnabled(true);
                    } else {
                        this.password.setEnabled(true);
                        this.buttonPassword.setEnabled(false);
                    }
                }

                this.bgscan.setEnabled(true);

                if (this.bgscan.getSelectedItemText()
                        .equals(MessageUtils.get(GwtWifiBgscanModule.netWifiBgscanMode_SIMPLE.name()))
                        || this.bgscan.getSelectedItemText()
                                .equals(MessageUtils.get(GwtWifiBgscanModule.netWifiBgscanMode_LEARN.name()))) {
                    this.shortI.setEnabled(true);
                    this.longI.setEnabled(true);
                } else {
                    this.shortI.setEnabled(false);
                    this.longI.setEnabled(false);
                }
            } else {
                this.ssid.setEnabled(true);
                this.buttonSsid.setEnabled(false);
                if (!this.security.getSelectedItemText().equals(WIFI_SECURITY_NONE_MESSAGE)) {
                    this.password.setEnabled(true);
                    this.buttonPassword.setEnabled(false);
                }
                this.bgscan.setEnabled(false);
                this.rssi.setEnabled(false);
                this.shortI.setEnabled(false);
                this.longI.setEnabled(false);
                this.radio1.setEnabled(false);
                this.radio2.setEnabled(false);
            }

            if (this.security.getSelectedItemText().equals(WIFI_SECURITY_WPA2_MESSAGE)
                    || this.security.getSelectedItemText().equals(WIFI_SECURITY_WPA_MESSAGE)
                    || this.security.getSelectedItemText()
                            .equals(MessageUtils.get(GwtWifiSecurity.netWifiSecurityWPA_WPA2.name()))) {
                if (WIFI_MODE_STATION_MESSAGE.equals(this.wireless.getSelectedItemText())) {
                    this.pairwise.setEnabled(true);
                    this.group.setEnabled(true);
                } else {
                    this.pairwise.setEnabled(true);
                    this.group.setEnabled(false);
                }
            } else {
                this.pairwise.setEnabled(false);
                this.group.setEnabled(false);
            }
        }

        // loadChannelData();
        this.netTabs.adjustInterfaceTabs();
    }

    private void reset() {

        for (int i = 0; i < this.wireless.getItemCount(); i++) {
            if (this.wireless.getSelectedItemText().equals(WIFI_MODE_STATION_MESSAGE)) {
                this.wireless.setSelectedIndex(i);
            }
        }
        this.ssid.setText("");
        for (int i = 0; i < this.radio.getItemCount(); i++) {
            if (this.radio.getItemText(i).equals(WIFI_RADIO_BGN_MESSAGE)) {
                this.radio.setSelectedIndex(i);
            }
        }

        for (int i = 0; i < this.security.getItemCount(); i++) {
            if (this.security.getItemText(i).equals(WIFI_SECURITY_WPA2_MESSAGE)) {
                this.security.setSelectedIndex(i);
            }
        }

        this.password.setText("");
        this.verify.setText("");

        for (int i = 0; i < this.pairwise.getItemCount(); i++) {
            if (this.pairwise.getItemText(i).equals(WIFI_CIPHERS_CCMP_TKIP_MESSAGE)) {
                this.pairwise.setSelectedIndex(i);
            }
        }

        for (int i = 0; i < this.group.getItemCount(); i++) {
            if (this.group.getItemText(i).equals(WIFI_CIPHERS_CCMP_TKIP_MESSAGE)) {
                this.group.setSelectedIndex(i);
            }
        }

        for (int i = 0; i < this.bgscan.getItemCount(); i++) {
            if (this.bgscan.getItemText(i).equals(WIFI_BGSCAN_NONE_MESSAGE)) {
                this.bgscan.setSelectedIndex(i);
            }
        }

        this.rssi.setValue("0.0");
        this.shortI.setValue("");
        this.longI.setValue("");
        this.radio2.setValue(true);
        this.radio4.setValue(true);

        update();
    }

    private void initHelpButtons() {
        this.wirelessHelp.setHelpTextProvider(() -> {
            if (TabWirelessUi.this.wireless.getSelectedItemText()
                    .equals(MessageUtils.get(NET_WIFI_WIRELESS_MODE_STATION))) {
                return MSGS.netWifiToolTipWirelessModeStation();
            } else {
                return MSGS.netWifiToolTipWirelessModeAccessPoint();
            }
        });
        this.ssidHelp.setHelpText(MSGS.netWifiToolTipNetworkName());
        this.radioHelp.setHelpText(MSGS.netWifiToolTipRadioMode());
        this.securityHelp.setHelpText(MSGS.netWifiToolTipSecurity());
        this.passwordHelp.setHelpText(MSGS.netWifiToolTipPassword());
        this.verifyHelp.setHelpText(MSGS.netWifiToolTipPassword());
        this.pairwiseHelp.setHelpText(MSGS.netWifiToolTipPairwiseCiphers());
        this.groupHelp.setHelpText(MSGS.netWifiToolTipGroupCiphers());
        this.bgscanHelp.setHelpText(MSGS.netWifiToolTipBgScan());
        this.rssiHelp.setHelpText(MSGS.netWifiToolTipBgScanStrength());
        this.shortIHelp.setHelpText(MSGS.netWifiToolTipBgScanShortInterval());
        this.longIHelp.setHelpText(MSGS.netWifiToolTipBgScanLongInterval());
        this.pingHelp.setHelpText(MSGS.netWifiToolTipPingAccessPoint());
        this.ignoreHelp.setHelpText(MSGS.netWifiToolTipIgnoreSSID());
    }

    private void initForm() {

        // Wireless Mode
        this.labelWireless.setText(MSGS.netWifiWirelessMode());
        this.wireless.addItem(MessageUtils.get(NET_WIFI_WIRELESS_MODE_STATION));
        this.wireless.addItem(MessageUtils.get("netWifiWirelessModeAccessPoint"));
        this.wireless.addMouseOverHandler(event -> {
            if (TabWirelessUi.this.wireless.getSelectedItemText()
                    .equals(MessageUtils.get(NET_WIFI_WIRELESS_MODE_STATION))) {
                TabWirelessUi.this.helpText.clear();
                TabWirelessUi.this.helpText.add(new Span(MSGS.netWifiToolTipWirelessModeStation()));
            } else {
                TabWirelessUi.this.helpText.clear();
                TabWirelessUi.this.helpText.add(new Span(MSGS.netWifiToolTipWirelessModeAccessPoint()));
            }
        });
        this.wireless.addMouseOutHandler(event -> resetHelp());

        this.wireless.addChangeHandler(event -> {
            TabWirelessUi.this.helpWireless.setText("");
            TabWirelessUi.this.groupWireless.setValidationState(ValidationState.NONE);

            if (TabWirelessUi.this.wireless.getSelectedItemText().equals(WIFI_MODE_STATION_MESSAGE)) {
                TabWirelessUi.this.activeConfig = TabWirelessUi.this.selectedNetIfConfig.getStationWifiConfig();
            } else {
                // use values from access point config
                TabWirelessUi.this.activeConfig = TabWirelessUi.this.selectedNetIfConfig.getAccessPointWifiConfig();
            }
            TabWirelessUi.this.netTabs.adjustInterfaceTabs();
            update();
            checkPassword();
            TabWirelessUi.this.wirelessHelp.updateHelpText();
        });

        // SSID
        this.labelSsid.setText(MSGS.netWifiNetworkName());
        this.labelSsid.setShowRequiredIndicator(true);
        this.ssid.setMaxLength(MAX_SSID_LENGTH);
        this.ssid.setAllowBlank(false);
        this.ssid.setValidateOnBlur(true);
        this.ssid.addMouseOverHandler(event -> {
            if (TabWirelessUi.this.ssid.isEnabled()) {
                TabWirelessUi.this.helpText.clear();
                TabWirelessUi.this.helpText.add(new Span(MSGS.netWifiToolTipNetworkName()));
            }
        });
        this.ssid.addMouseOutHandler(event -> resetHelp());
        this.buttonSsid.addClickHandler(event -> {
            if (!TabWirelessUi.this.ssidInit) {
                initSsid();
                TabWirelessUi.this.ssidDataProvider.getList().clear();
                TabWirelessUi.this.searching.setVisible(true);
                TabWirelessUi.this.noSsid.setVisible(false);
                TabWirelessUi.this.ssidGrid.setVisible(false);
                TabWirelessUi.this.scanFail.setVisible(false);
            }
            initModal();
            loadSsidData();
        });

        // Radio Mode
        this.labelRadio.setText(MSGS.netWifiRadioMode());
        this.radio.addMouseOverHandler(event -> {
            if (TabWirelessUi.this.radio.isEnabled()) {
                TabWirelessUi.this.helpText.clear();
                TabWirelessUi.this.helpText.add(new Span(MSGS.netWifiToolTipRadioMode()));
            }
        });
        this.radio.addMouseOutHandler(event -> resetHelp());
        for (GwtWifiRadioMode mode : GwtWifiRadioMode.values()) {
            if (mode != GwtWifiRadioMode.netWifiRadioModeA) {
                // We don't support 802.11a yet
                this.radio.addItem(MessageUtils.get(mode.name()));
            }
        }

        // Wireless Security
        this.labelSecurity.setText(MSGS.netWifiWirelessSecurity());
        this.security.addMouseOverHandler(event -> {
            if (TabWirelessUi.this.security.isEnabled()) {
                TabWirelessUi.this.helpText.clear();
                TabWirelessUi.this.helpText.add(new Span(MSGS.netWifiToolTipSecurity()));
            }
        });
        this.security.addMouseOutHandler(event -> resetHelp());
        for (GwtWifiSecurity mode : GwtWifiSecurity.values()) {
            this.security.addItem(MessageUtils.get(mode.name()));
        }
        this.security.addChangeHandler(event -> {
            setPasswordValidation();
            refreshForm();
            checkPassword();
        });

        // Password
        this.labelPassword.setText(MSGS.netWifiWirelessPassword());
        this.password.addMouseOverHandler(event -> {
            if (TabWirelessUi.this.password.isEnabled()) {
                TabWirelessUi.this.helpText.clear();
                TabWirelessUi.this.helpText.add(new Span(MSGS.netWifiToolTipPassword()));
            }
        });
        this.password.setAllowBlank(false);
        this.password.addMouseOutHandler(event -> resetHelp());
        this.buttonPassword.addClickHandler(event -> {
            EntryClassUi.showWaitModal();
            TabWirelessUi.this.buttonPassword.setEnabled(false);
            final GwtWifiConfig gwtWifiConfig = getGwtWifiConfig();
            TabWirelessUi.this.gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

                @Override
                public void onFailure(Throwable ex) {
                    FailureHandler.handle(ex);
                }

                @Override
                public void onSuccess(GwtXSRFToken token) {
                    TabWirelessUi.this.gwtNetworkService.verifyWifiCredentials(token,
                            TabWirelessUi.this.selectedNetIfConfig.getName(), gwtWifiConfig,
                            new AsyncCallback<Boolean>() {

                                @Override
                                public void onFailure(Throwable caught) {
                                    FailureHandler.handle(caught);
                                    EntryClassUi.hideWaitModal();
                                    TabWirelessUi.this.buttonPassword.setEnabled(true);
                                    showPasswordVerificationStatus(MSGS.netWifiPasswordVerificationFailed());
                                }

                                @Override
                                public void onSuccess(Boolean result) {
                                    if (!result.booleanValue()) {
                                        showPasswordVerificationStatus(MSGS.netWifiPasswordVerificationFailed());
                                    } else {
                                        showPasswordVerificationStatus(MSGS.netWifiPasswordVerificationSuccess());
                                    }
                                    EntryClassUi.hideWaitModal();
                                    TabWirelessUi.this.buttonPassword.setEnabled(true);
                                }
                            });
                }

            });
        });
        this.password.addKeyUpHandler(event -> {
            if (TabWirelessUi.this.groupVerify.isVisible()
                    && !TabWirelessUi.this.verify.getText().equals(TabWirelessUi.this.password.getText())) {
                TabWirelessUi.this.groupVerify.setValidationState(ValidationState.ERROR);
            } else {
                TabWirelessUi.this.groupVerify.setValidationState(ValidationState.NONE);
            }
        });
        this.password.addChangeHandler(event -> {
            refreshForm();
            checkPassword();
        });

        // Verify Password
        this.labelVerify.setText(MSGS.netWifiWirelessVerifyPassword());
        this.verify.addMouseOverHandler(event -> {
            if (TabWirelessUi.this.verify.isEnabled()) {
                TabWirelessUi.this.helpText.clear();
                TabWirelessUi.this.helpText.add(new Span(MSGS.netWifiToolTipPassword()));
            }
        });
        this.verify.addMouseOutHandler(event -> resetHelp());
        this.verify.addChangeHandler(event -> {
            refreshForm();
            checkPassword();
        });

        // Pairwise ciphers
        this.labelPairwise.setText(MSGS.netWifiWirelessPairwiseCiphers());
        this.pairwise.addMouseOverHandler(event -> {
            if (TabWirelessUi.this.pairwise.isEnabled()) {
                TabWirelessUi.this.helpText.clear();
                TabWirelessUi.this.helpText.add(new Span(MSGS.netWifiToolTipPairwiseCiphers()));
            }
        });
        this.pairwise.addMouseOutHandler(event -> resetHelp());
        for (GwtWifiCiphers cipher : GwtWifiCiphers.values()) {
            if (GwtWifiCiphers.netWifiCiphers_NONE == cipher) {
                continue;
            }
            this.pairwise.addItem(MessageUtils.get(cipher.name()));
        }
        this.pairwise.addChangeHandler(event -> refreshForm());

        // Groupwise Ciphers
        this.labelGroup.setText(MSGS.netWifiWirelessGroupCiphers());
        this.group.addMouseOverHandler(event -> {
            if (TabWirelessUi.this.group.isEnabled()) {
                TabWirelessUi.this.helpText.clear();
                TabWirelessUi.this.helpText.add(new Span(MSGS.netWifiWirelessGroupCiphers()));
            }
        });
        this.group.addMouseOutHandler(event -> resetHelp());
        for (GwtWifiCiphers cipher : GwtWifiCiphers.values()) {
            if (GwtWifiCiphers.netWifiCiphers_NONE == cipher) {
                continue;
            }
            this.group.addItem(MessageUtils.get(cipher.name()));
        }
        this.group.addChangeHandler(event -> refreshForm());

        // Bgscan module
        this.labelBgscan.setText(MSGS.netWifiWirelessBgscanModule());
        this.bgscan.addMouseOverHandler(event -> {
            if (TabWirelessUi.this.bgscan.isEnabled()) {
                TabWirelessUi.this.helpText.clear();
                TabWirelessUi.this.helpText.add(new Span(MSGS.netWifiToolTipBgScan()));
            }
        });
        this.bgscan.addMouseOutHandler(event -> resetHelp());
        for (GwtWifiBgscanModule module : GwtWifiBgscanModule.values()) {
            this.bgscan.addItem(MessageUtils.get(module.name()));
        }
        this.bgscan.addChangeHandler(event -> refreshForm());

        // BgScan RSSI threshold
        this.labelRssi.setText(MSGS.netWifiWirelessBgscanSignalStrengthThreshold());

        // Bgscan short Interval
        this.labelShortI.setText(MSGS.netWifiWirelessBgscanShortInterval());
        this.shortI.addMouseOverHandler(event -> {
            if (TabWirelessUi.this.shortI.isEnabled()) {
                TabWirelessUi.this.helpText.clear();
                TabWirelessUi.this.helpText.add(new Span(MSGS.netWifiToolTipBgScanShortInterval()));
            }
        });
        this.shortI.addMouseOutHandler(event -> resetHelp());
        this.shortI.addChangeHandler(event -> {
            if (TabWirelessUi.this.shortI.getText().trim().contains(".")
                    || TabWirelessUi.this.shortI.getText().trim().contains("-")
                    || !TabWirelessUi.this.shortI.getText().trim().matches("[0-9]+")) {
                TabWirelessUi.this.groupShortI.setValidationState(ValidationState.ERROR);
            } else {
                TabWirelessUi.this.groupShortI.setValidationState(ValidationState.NONE);
            }
        });

        // Bgscan long interval
        this.labelLongI.setText(MSGS.netWifiWirelessBgscanLongInterval());
        this.longI.addMouseOverHandler(event -> {
            if (TabWirelessUi.this.longI.isEnabled()) {
                TabWirelessUi.this.helpText.clear();
                TabWirelessUi.this.helpText.add(new Span(MSGS.netWifiToolTipBgScanLongInterval()));
            }
        });
        this.longI.addMouseOutHandler(event -> resetHelp());
        this.longI.addChangeHandler(event -> {
            if (TabWirelessUi.this.longI.getText().trim().contains(".")
                    || TabWirelessUi.this.longI.getText().trim().contains("-")
                    || !TabWirelessUi.this.longI.getText().trim().matches("[0-9]+")) {
                TabWirelessUi.this.groupLongI.setValidationState(ValidationState.ERROR);
            } else {
                TabWirelessUi.this.groupLongI.setValidationState(ValidationState.NONE);
            }
        });

        // Ping Access Point ----
        this.labelPing.setText(MSGS.netWifiWirelessPingAccessPoint());
        this.radio1.setText(MSGS.trueLabel());
        this.radio1.addMouseOverHandler(event -> {
            if (TabWirelessUi.this.radio1.isEnabled()) {
                TabWirelessUi.this.helpText.clear();
                TabWirelessUi.this.helpText.add(new Span(MSGS.netWifiToolTipPingAccessPoint()));
            }
        });
        this.radio1.addMouseOutHandler(event -> resetHelp());
        this.radio2.setText(MSGS.falseLabel());
        this.radio2.addMouseOverHandler(event -> {
            if (TabWirelessUi.this.radio2.isEnabled()) {
                TabWirelessUi.this.helpText.clear();
                TabWirelessUi.this.helpText.add(new Span(MSGS.netWifiToolTipPingAccessPoint()));
            }
        });
        this.radio2.addMouseOutHandler(event -> resetHelp());

        // Ignore Broadcast SSID
        this.labelIgnore.setText(MSGS.netWifiWirelessIgnoreSSID());
        this.radio3.setText(MSGS.trueLabel());
        this.radio3.addMouseOverHandler(event -> {
            if (TabWirelessUi.this.radio3.isEnabled()) {
                TabWirelessUi.this.helpText.clear();
                TabWirelessUi.this.helpText.add(new Span(MSGS.netWifiToolTipIgnoreSSID()));
            }
        });
        this.radio3.addMouseOutHandler(event -> resetHelp());
        this.radio4.setText(MSGS.falseLabel());
        this.radio4.addMouseOverHandler(event -> {
            if (TabWirelessUi.this.radio4.isEnabled()) {
                TabWirelessUi.this.helpText.clear();
                TabWirelessUi.this.helpText.add(new Span(MSGS.netWifiToolTipIgnoreSSID()));
            }
        });
        this.radio4.addMouseOutHandler(event -> resetHelp());

        // Channel Grid
        initGrid();

        this.helpTitle.setText(MSGS.netHelpTitle());
    }

    private void resetHelp() {
        this.helpText.clear();
        this.helpText.add(new Span(MSGS.netHelpDefaultHint()));
    }

    private void initGrid() {

        // CHECKBOXES
        Column<GwtWifiChannelModel, Boolean> checkColumn = new Column<GwtWifiChannelModel, Boolean>(
                new CheckboxCell()) {

            @Override
            public Boolean getValue(GwtWifiChannelModel object) {
                return TabWirelessUi.this.channelGrid.getSelectionModel().isSelected(object);
            }

        };
        checkColumn.setFieldUpdater((index, object, value) -> {
            TabWirelessUi.this.channelGrid.getSelectionModel().setSelected(object, value);
            TabWirelessUi.this.channelDataProvider.refresh();
        });

        checkColumn.setCellStyleNames("status-table-row");
        this.channelGrid.addColumn(checkColumn);

        // ALL AVAILABLE CHANNELS
        TextColumn<GwtWifiChannelModel> col1 = new TextColumn<GwtWifiChannelModel>() {

            @Override
            public String getValue(GwtWifiChannelModel object) {
                return object.getName();
            }
        };
        col1.setCellStyleNames("status-table-row");
        this.channelGrid.addColumn(col1, "All Available Channels");

        // FREQUENCY
        TextColumn<GwtWifiChannelModel> col2 = new TextColumn<GwtWifiChannelModel>() {

            @Override
            public String getValue(GwtWifiChannelModel object) {
                return String.valueOf(object.getFrequency());
            }
        };
        col2.setCellStyleNames("status-table-row");
        this.channelGrid.addColumn(col2, "Frequency (MHz)");

        // SPECTRUM BAND
        TextColumn<GwtWifiChannelModel> col3 = new TextColumn<GwtWifiChannelModel>() {

            @Override
            public String getValue(GwtWifiChannelModel object) {
                return String.valueOf(object.getBand());
            }
        };
        col3.setCellStyleNames("status-table-row");
        this.channelGrid.addColumn(col3, "Frequency (MHz)");

        this.channelGrid.setSelectionModel(this.selectionModel);
        this.channelDataProvider.addDataDisplay(this.channelGrid);

        loadChannelData();
    }

    private void loadChannelData() {
        this.channelDataProvider.getList().clear();
        this.channelDataProvider.setList(GwtWifiChannelModel.getChannels());

        this.gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

            @Override
            public void onFailure(Throwable ex) {
                FailureHandler.handle(ex);
            }

            @Override
            public void onSuccess(GwtXSRFToken token) {
                TabWirelessUi.this.gwtDeviceService.findDeviceConfiguration(token,
                        new AsyncCallback<ArrayList<GwtGroupedNVPair>>() {

                            @Override
                            public void onFailure(Throwable caught) {
                                TabWirelessUi.this.channelGrid.setVisible(false);
                                FailureHandler.handle(caught);
                            }

                            @Override
                            public void onSuccess(ArrayList<GwtGroupedNVPair> result) {
                                if (result != null) {
                                    TabWirelessUi.this.channelGrid.setVisible(true);
                                    for (GwtGroupedNVPair pair : result) {
                                        String name = pair.getName();
                                        if (name != null && name.equals("devLastWifiChannel")) {
                                            int topChannel = Integer.parseInt(pair.getValue());
                                            // Remove channels 12 and 13
                                            if (topChannel < MAX_WIFI_CHANNEL) {
                                                try {
                                                    TabWirelessUi.this.channelDataProvider.getList()
                                                            .remove(MAX_WIFI_CHANNEL - 1);
                                                    TabWirelessUi.this.channelDataProvider.getList()
                                                            .remove(MAX_WIFI_CHANNEL - 2);
                                                } catch (UnsupportedOperationException | IndexOutOfBoundsException e) {
                                                    logger.info(e.getLocalizedMessage());
                                                }
                                            }
                                        }
                                    }
                                    TabWirelessUi.this.channelDataProvider.flush();
                                }
                            }

                        });
            }

        });

        this.noChannelsText.setText(MSGS.netWifiAlertNoChannels());
        if (!this.channelDataProvider.getList().isEmpty()) {
            this.noChannels.setVisible(false);
            this.channelGrid.setVisible(true);
        } else {
            this.channelGrid.setVisible(false);
            this.noChannels.setVisible(true);
        }

    }

    private void setPasswordValidation() {

        final GwtConsoleUserOptions configUserOptions = EntryClassUi.getUserOptions();

        if (getWirelessMode() != GwtWifiWirelessMode.netWifiWirelessModeAccessPoint) {
            configUserOptions.allowAnyPassword();
        }

        if (this.security.getSelectedItemText().equals(WIFI_SECURITY_WPA_MESSAGE)
                || this.security.getSelectedItemText().equals(WIFI_SECURITY_WPA2_MESSAGE)
                || this.security.getSelectedItemText().contentEquals(WIFI_SECURITY_WPA_WPA2_MESSAGE)) {

            this.password.setValidatorsFrom(configUserOptions);
            configUserOptions.setPasswordMinimumLength(Math.min(configUserOptions.getPasswordMinimumLength(), 63));
            this.password
                    .addValidator(new RegexValidator(REGEX_PASSWORD_WPA, MSGS.netWifiWirelessInvalidWPAPassword()) {
                    });

        } else if (this.security.getSelectedItemText().equals(WIFI_SECURITY_WEP_MESSAGE)) {

            configUserOptions.setPasswordRequireSpecialChars(false);
            configUserOptions.setPasswordMinimumLength(Math.min(configUserOptions.getPasswordMinimumLength(), 26));
            this.password.setValidatorsFrom(configUserOptions);
            this.password
                    .addValidator(new RegexValidator(REGEX_PASSWORD_WEP, MSGS.netWifiWirelessInvalidWEPPassword()) {
                    });

        } else {
            configUserOptions.allowAnyPassword();
            this.password.setValidatorsFrom(configUserOptions);
        }

    }

    private void initModal() {
        this.ssidModal.setTitle("Wireless Networks");
        this.ssidTitle.setText("Available networks in range");
        this.ssidModal.show();

        this.searchingText.setText(MSGS.netWifiAlertScanning());
        this.noSsidText.setText(MSGS.netWifiAlertNoSSID());
        this.scanFailText.setText(MSGS.netWifiAlertScanFail());
    }

    private void initSsid() {

        this.ssidInit = true;
        TextColumn<GwtWifiHotspotEntry> col1 = new TextColumn<GwtWifiHotspotEntry>() {

            @Override
            public String getValue(GwtWifiHotspotEntry object) {
                return object.getSSID();
            }
        };
        col1.setCellStyleNames("status-table-row");
        this.ssidGrid.addColumn(col1, "SSID");
        this.ssidGrid.setColumnWidth(col1, "240px");

        TextColumn<GwtWifiHotspotEntry> col2 = new TextColumn<GwtWifiHotspotEntry>() {

            @Override
            public String getValue(GwtWifiHotspotEntry object) {
                return object.getMacAddress();
            }
        };
        col2.setCellStyleNames("status-table-row");
        this.ssidGrid.addColumn(col2, "MAC Address");
        this.ssidGrid.setColumnWidth(col2, "140px");

        TextColumn<GwtWifiHotspotEntry> col3 = new TextColumn<GwtWifiHotspotEntry>() {

            @Override
            public String getValue(GwtWifiHotspotEntry object) {
                return String.valueOf(object.getSignalStrength());
            }
        };
        col3.setCellStyleNames("status-table-row");
        this.ssidGrid.addColumn(col3, "Signal Strength (dBm)");
        this.ssidGrid.setColumnWidth(col3, "70px");

        TextColumn<GwtWifiHotspotEntry> col4 = new TextColumn<GwtWifiHotspotEntry>() {

            @Override
            public String getValue(GwtWifiHotspotEntry object) {
                return String.valueOf(object.getChannel());
            }
        };
        col4.setCellStyleNames("status-table-row");
        this.ssidGrid.addColumn(col4, "Channel");
        this.ssidGrid.setColumnWidth(col4, "70px");

        TextColumn<GwtWifiHotspotEntry> col5 = new TextColumn<GwtWifiHotspotEntry>() {

            @Override
            public String getValue(GwtWifiHotspotEntry object) {
                return String.valueOf(object.getFrequency());
            }
        };
        col5.setCellStyleNames("status-table-row");
        this.ssidGrid.addColumn(col5, "Frequency");
        this.ssidGrid.setColumnWidth(col5, "70px");

        TextColumn<GwtWifiHotspotEntry> col6 = new TextColumn<GwtWifiHotspotEntry>() {

            @Override
            public String getValue(GwtWifiHotspotEntry object) {
                return object.getSecurity();
            }
        };
        col6.setCellStyleNames("status-table-row");
        this.ssidGrid.addColumn(col6, "Security");
        this.ssidGrid.setColumnWidth(col6, "70px");
        this.ssidDataProvider.addDataDisplay(this.ssidGrid);

        this.ssidGrid.setSelectionModel(this.ssidSelectionModel);

        this.ssidSelectionModel.addSelectionChangeHandler(event -> {
            GwtWifiHotspotEntry wifiHotspotEntry = TabWirelessUi.this.ssidSelectionModel.getSelectedObject();
            if (wifiHotspotEntry != null) {
                TabWirelessUi.this.ssid.setValue(wifiHotspotEntry.getSSID());
                String sec = wifiHotspotEntry.getSecurity();
                for (int i1 = 0; i1 < TabWirelessUi.this.security.getItemCount(); i1++) {
                    if (sec.equals(TabWirelessUi.this.security.getItemText(i1))) {
                        TabWirelessUi.this.security.setSelectedIndex(i1);
                        DomEvent.fireNativeEvent(Document.get().createChangeEvent(), TabWirelessUi.this.security);
                        break;
                    }
                }

                String pairwiseCiphers = wifiHotspotEntry.getPairwiseCiphersEnum().name();
                for (int i2 = 0; i2 < TabWirelessUi.this.pairwise.getItemCount(); i2++) {
                    if (MessageUtils.get(pairwiseCiphers).equals(TabWirelessUi.this.pairwise.getItemText(i2))) {
                        TabWirelessUi.this.pairwise.setSelectedIndex(i2);
                        break;
                    }
                }

                String groupCiphers = wifiHotspotEntry.getGroupCiphersEnum().name();
                for (int i3 = 0; i3 < TabWirelessUi.this.group.getItemCount(); i3++) {
                    if (MessageUtils.get(groupCiphers).equals(TabWirelessUi.this.group.getItemText(i3))) {
                        TabWirelessUi.this.group.setSelectedIndex(i3);
                        break;
                    }
                }

                int channelListSize = TabWirelessUi.this.channelDataProvider.getList().size();
                int maxIndex = Math.min(channelListSize, MAX_WIFI_CHANNEL);
                // deselect all channels
                for (int channel = 1; channel <= maxIndex; channel++) {
                    TabWirelessUi.this.selectionModel
                            .setSelected(TabWirelessUi.this.channelDataProvider.getList().get(channel - 1), false);
                }

                TabWirelessUi.this.selectionModel.setSelected(
                        TabWirelessUi.this.channelDataProvider.getList().get(wifiHotspotEntry.getChannel() - 1), true);
                TabWirelessUi.this.ssidModal.hide();
            }
        });
    }

    private void loadSsidData() {
        this.ssidDataProvider.getList().clear();
        this.searching.setVisible(true);
        this.noSsid.setVisible(false);
        this.ssidGrid.setVisible(false);
        this.scanFail.setVisible(false);
        if (this.selectedNetIfConfig != null) {
            this.gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

                @Override
                public void onFailure(Throwable ex) {
                    FailureHandler.handle(ex);
                }

                @Override
                public void onSuccess(GwtXSRFToken token) {
                    TabWirelessUi.this.gwtNetworkService.findWifiHotspots(token,
                            TabWirelessUi.this.selectedNetIfConfig.getName(),
                            TabWirelessUi.this.selectedNetIfConfig.getAccessPointWifiConfig().getWirelessSsid(),
                            new AsyncCallback<List<GwtWifiHotspotEntry>>() {

                                @Override
                                public void onFailure(Throwable caught) {
                                    TabWirelessUi.this.searching.setVisible(false);
                                    TabWirelessUi.this.noSsid.setVisible(false);
                                    TabWirelessUi.this.ssidGrid.setVisible(false);
                                    TabWirelessUi.this.scanFail.setVisible(true);
                                }

                                @Override
                                public void onSuccess(List<GwtWifiHotspotEntry> result) {
                                    for (GwtWifiHotspotEntry pair : result) {
                                        TabWirelessUi.this.ssidDataProvider.getList().add(pair);
                                    }
                                    TabWirelessUi.this.ssidDataProvider.flush();
                                    if (!TabWirelessUi.this.ssidDataProvider.getList().isEmpty()) {
                                        TabWirelessUi.this.searching.setVisible(false);
                                        TabWirelessUi.this.noSsid.setVisible(false);
                                        int size = TabWirelessUi.this.ssidDataProvider.getList().size();
                                        TabWirelessUi.this.ssidGrid.setVisibleRange(0, size);
                                        TabWirelessUi.this.ssidGrid.setVisible(true);
                                        TabWirelessUi.this.scanFail.setVisible(false);
                                    } else {
                                        TabWirelessUi.this.searching.setVisible(false);
                                        TabWirelessUi.this.noSsid.setVisible(true);
                                        TabWirelessUi.this.ssidGrid.setVisible(false);
                                        TabWirelessUi.this.scanFail.setVisible(false);
                                    }
                                }
                            });
                }

            });
        }
    }

    private GwtWifiConfig getGwtWifiConfig() {
        GwtWifiConfig gwtWifiConfig = new GwtWifiConfig();

        // mode
        GwtWifiWirelessMode wifiMode;
        if (this.wireless.getSelectedItemText().equals(MessageUtils.get(WIFI_MODE_STATION))) {
            wifiMode = GwtWifiWirelessMode.netWifiWirelessModeStation;
        } else {
            wifiMode = GwtWifiWirelessMode.netWifiWirelessModeAccessPoint;
        }
        gwtWifiConfig.setWirelessMode(wifiMode.name());

        // ssid
        gwtWifiConfig.setWirelessSsid(GwtSafeHtmlUtils.htmlUnescape(this.ssid.getText().trim()));

        // driver
        String driver = "";
        if (GwtWifiWirelessMode.netWifiWirelessModeAccessPoint.equals(wifiMode)) {
            driver = this.selectedNetIfConfig.getAccessPointWifiConfig().getDriver();
        } else if (GwtWifiWirelessMode.netWifiWirelessModeAdHoc.equals(wifiMode)) {
            driver = this.selectedNetIfConfig.getAdhocWifiConfig().getDriver();
        } else if (GwtWifiWirelessMode.netWifiWirelessModeStation.equals(wifiMode)) {
            driver = this.selectedNetIfConfig.getStationWifiConfig().getDriver();
        }
        gwtWifiConfig.setDriver(driver); // use previous value

        // radio mode
        String radioValue = this.radio.getSelectedItemText();
        for (GwtWifiRadioMode mode : GwtWifiRadioMode.values()) {
            if (MessageUtils.get(mode.name()).equals(radioValue)) {
                gwtWifiConfig.setRadioMode(mode.name());
            }
        }

        // channels
        Set<GwtWifiChannelModel> lSelectedChannels = this.selectionModel.getSelectedSet();

        ArrayList<Integer> alChannels = new ArrayList<>();
        for (GwtWifiChannelModel item : lSelectedChannels) {
            alChannels.add(item.getChannel());
        }
        if (alChannels.isEmpty()) {
            alChannels.add(1);
        }
        gwtWifiConfig.setChannels(alChannels);

        // security
        String secValue = this.security.getSelectedItemText();
        for (GwtWifiSecurity sec : GwtWifiSecurity.values()) {
            if (MessageUtils.get(sec.name()).equals(secValue)) {
                gwtWifiConfig.setSecurity(sec.name());
            }
        }

        // Pairwise Ciphers
        String pairWiseCiphersValue = this.pairwise.getSelectedItemText();
        for (GwtWifiCiphers ciphers : GwtWifiCiphers.values()) {
            if (MessageUtils.get(ciphers.name()).equals(pairWiseCiphersValue)) {
                gwtWifiConfig.setPairwiseCiphers(ciphers.name());
            }
        }

        // Group Ciphers value
        String groupCiphersValue = this.group.getSelectedItemText();
        for (GwtWifiCiphers ciphers : GwtWifiCiphers.values()) {
            if (MessageUtils.get(ciphers.name()).equals(groupCiphersValue)) {
                gwtWifiConfig.setGroupCiphers(ciphers.name());
            }
        }

        // bgscan
        String bgscanModuleValue = this.bgscan.getSelectedItemText();
        for (GwtWifiBgscanModule module : GwtWifiBgscanModule.values()) {
            if (MessageUtils.get(module.name()).equals(bgscanModuleValue)) {
                gwtWifiConfig.setBgscanModule(module.name());
            }
        }

        gwtWifiConfig.setBgscanShortInterval(Integer.parseInt(this.shortI.getText()));
        gwtWifiConfig.setBgscanLongInterval(Integer.parseInt(this.longI.getText()));

        // password
        if (this.groupPassword.getValidationState().equals(ValidationState.NONE)) {
            gwtWifiConfig.setPassword(this.password.getText());
        }

        // ping access point
        gwtWifiConfig.setPingAccessPoint(this.radio1.getValue());

        // ignore SSID
        gwtWifiConfig.setIgnoreSSID(this.radio3.getValue());

        return gwtWifiConfig;
    }

    private void setForm(boolean b) {
        this.channelGrid.setVisible(b);
        this.wireless.setEnabled(b);
        this.ssid.setEnabled(b);
        this.buttonSsid.setEnabled(b);
        this.radio.setEnabled(b);
        this.security.setEnabled(b);
        this.password.setEnabled(b);
        this.buttonPassword.setEnabled(b);
        this.verify.setEnabled(b);
        this.pairwise.setEnabled(b);
        this.group.setEnabled(b);
        this.bgscan.setEnabled(b);
        this.shortI.setEnabled(b);
        this.longI.setEnabled(b);
        this.radio1.setEnabled(b);
        this.radio2.setEnabled(b);
        this.radio3.setEnabled(b);
        this.radio4.setEnabled(b);
        this.groupVerify.setVisible(b);
    }

    private void checkPassword() {
        if (!this.password.validate() && this.password.isEnabled()) {
            this.groupPassword.setValidationState(ValidationState.ERROR);
        } else {
            this.groupPassword.setValidationState(ValidationState.NONE);
        }

        if (this.verify.isEnabled() && TabWirelessUi.this.password != null
                && !TabWirelessUi.this.verify.getText().equals(TabWirelessUi.this.password.getText())) {
            TabWirelessUi.this.helpVerify.setText(MSGS.netWifiWirelessPasswordDoesNotMatch());
            TabWirelessUi.this.groupVerify.setValidationState(ValidationState.ERROR);

        } else {
            TabWirelessUi.this.helpVerify.setText("");
            TabWirelessUi.this.groupVerify.setValidationState(ValidationState.NONE);
        }

    }

    private void showPasswordVerificationStatus(String statusMessage) {
        final Modal confirm = new Modal();
        ModalBody confirmBody = new ModalBody();
        ModalFooter confirmFooter = new ModalFooter();

        confirm.setTitle(MSGS.netWifiPasswordVerificationStatus());
        confirmBody.add(new Span(statusMessage));

        confirmFooter.add(new Button(MSGS.closeButton(), event -> confirm.hide()));
        confirm.add(confirmBody);
        confirm.add(confirmFooter);
        confirm.show();
    }
}

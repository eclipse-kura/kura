/*******************************************************************************
 * Copyright (c) 2023 Eurotech and/or its affiliates and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Eurotech
 *******************************************************************************/
package org.eclipse.kura.web.server.net2.utils;

import java.util.Optional;

import org.eclipse.kura.net.NetInterfaceStatus;
import org.eclipse.kura.net.modem.ModemConfig.AuthType;
import org.eclipse.kura.net.modem.ModemConfig.PdpType;
import org.eclipse.kura.net.wifi.WifiCiphers;
import org.eclipse.kura.net.wifi.WifiMode;
import org.eclipse.kura.net.wifi.WifiRadioMode;
import org.eclipse.kura.net.wifi.WifiSecurity;
import org.eclipse.kura.web.shared.model.GwtModemAuthType;
import org.eclipse.kura.web.shared.model.GwtModemPdpType;
import org.eclipse.kura.web.shared.model.GwtNetIfStatus;
import org.eclipse.kura.web.shared.model.GwtWifiCiphers;
import org.eclipse.kura.web.shared.model.GwtWifiRadioMode;
import org.eclipse.kura.web.shared.model.GwtWifiSecurity;
import org.eclipse.kura.web.shared.model.GwtWifiWirelessMode;

/**
 * Utility class to convert String values of Enumeration objects used by GWT
 *
 */
public class EnumsParser {

    private EnumsParser() {

    }

    /**
     * Converts values of {@link WifiMode} to {@link GwtWifiWirelessMode} values
     * 
     */
    public static String getGwtWifiWirelessMode(Optional<String> wifiMode) {
        if (wifiMode.isPresent()) {
            if (wifiMode.get().equals(WifiMode.MASTER.name())) {
                return GwtWifiWirelessMode.netWifiWirelessModeAccessPoint.name();
            }

            if (wifiMode.get().equals(WifiMode.INFRA.name())) {
                return GwtWifiWirelessMode.netWifiWirelessModeStation.name();
            }

            if (wifiMode.get().equals(WifiMode.ADHOC.name())) {
                return GwtWifiWirelessMode.netWifiWirelessModeAdHoc.name();
            }
        }

        return GwtWifiWirelessMode.netWifiWirelessModeDisabled.name();
    }

    /**
     * Converts values of {@link GwtWifiWirelessMode} to {@link WifiMode} values
     * 
     */
    public static String getWifiMode(Optional<String> gwtWifiWirelessMode) {
        if (gwtWifiWirelessMode.isPresent()) {
            if (gwtWifiWirelessMode.get().equals(GwtWifiWirelessMode.netWifiWirelessModeAccessPoint.name())) {
                return WifiMode.MASTER.name();
            }

            if (gwtWifiWirelessMode.get().equals(GwtWifiWirelessMode.netWifiWirelessModeStation.name())) {
                return WifiMode.INFRA.name();
            }

            if (gwtWifiWirelessMode.get().equals(GwtWifiWirelessMode.netWifiWirelessModeAdHoc.name())) {
                return WifiMode.ADHOC.name();
            }
        }

        return WifiMode.UNKNOWN.toString();
    }

    /**
     * Converts values of {@link NetInterfaceStatus} to {@link GwtNetIfStatus}
     * values
     * 
     */
    public static String getGwtNetIfStatus(Optional<String> netInterfaceStatus) {
        if (netInterfaceStatus.isPresent()) {
            if (netInterfaceStatus.get().equals(NetInterfaceStatus.netIPv4StatusEnabledLAN.name())) {
                return GwtNetIfStatus.netIPv4StatusEnabledLAN.name();
            }

            if (netInterfaceStatus.get().equals(NetInterfaceStatus.netIPv4StatusEnabledWAN.name())) {
                return GwtNetIfStatus.netIPv4StatusEnabledWAN.name();
            }

            if (netInterfaceStatus.get().equals(NetInterfaceStatus.netIPv4StatusL2Only.name())) {
                return GwtNetIfStatus.netIPv4StatusL2Only.name();
            }

            if (netInterfaceStatus.get().equals(NetInterfaceStatus.netIPv4StatusUnmanaged.name())) {
                return GwtNetIfStatus.netIPv4StatusUnmanaged.name();
            }
        }

        return GwtNetIfStatus.netIPv4StatusDisabled.name();
    }

    /**
     * Converts values of {@link GwtNetIfStatus} to {@link NetInterfaceStatus}
     * values
     * 
     */
    public static String getNetInterfaceStatus(Optional<String> gwtNetIfStatus) {
        if (gwtNetIfStatus.isPresent()) {
            if (gwtNetIfStatus.get().equals(GwtNetIfStatus.netIPv4StatusDisabled.name())) {
                return NetInterfaceStatus.netIPv4StatusDisabled.name();
            }

            if (gwtNetIfStatus.get().equals(GwtNetIfStatus.netIPv4StatusEnabledLAN.name())) {
                return NetInterfaceStatus.netIPv4StatusEnabledLAN.name();
            }

            if (gwtNetIfStatus.get().equals(GwtNetIfStatus.netIPv4StatusEnabledWAN.name())) {
                return NetInterfaceStatus.netIPv4StatusEnabledWAN.name();
            }

            if (gwtNetIfStatus.get().equals(GwtNetIfStatus.netIPv4StatusL2Only.name())) {
                return NetInterfaceStatus.netIPv4StatusL2Only.name();
            }

            if (gwtNetIfStatus.get().equals(GwtNetIfStatus.netIPv4StatusUnmanaged.name())) {
                return NetInterfaceStatus.netIPv4StatusUnmanaged.name();
            }
        }

        return NetInterfaceStatus.netIPv4StatusUnknown.name();
    }

    /**
     * Converts values of {@link WifiSecurity} to {@link GwtWifiSecurity} values
     * 
     */
    public static String getGwtWifiSecurity(Optional<String> wifiSecurity) {
        if (wifiSecurity.isPresent()) {
            if (wifiSecurity.get().equals(WifiSecurity.NONE.name())) {
                return GwtWifiSecurity.netWifiSecurityNONE.name();
            }

            if (wifiSecurity.get().equals(WifiSecurity.SECURITY_WEP.name())) {
                return GwtWifiSecurity.netWifiSecurityWEP.name();
            }

            if (wifiSecurity.get().equals(WifiSecurity.SECURITY_WPA.name())) {
                return GwtWifiSecurity.netWifiSecurityWPA.name();
            }

            if (wifiSecurity.get().equals(WifiSecurity.SECURITY_WPA2.name())) {
                return GwtWifiSecurity.netWifiSecurityWPA2.name();
            }

            if (wifiSecurity.get().equals(WifiSecurity.SECURITY_WPA_WPA2.name())) {
                return GwtWifiSecurity.netWifiSecurityWPA_WPA2.name();
            }
        }

        return GwtWifiSecurity.netWifiSecurityWPA2.name();
    }

    /**
     * Converts values of {@link GwtWifiSecurity} to {@link WifiSecurity} values
     * 
     */
    public static String getWifiSecurity(Optional<String> gwtWifiSecurity) {
        if (gwtWifiSecurity.isPresent()) {
            if (gwtWifiSecurity.get().equals(GwtWifiSecurity.netWifiSecurityWEP.name())) {
                return WifiSecurity.SECURITY_WEP.name();
            }

            if (gwtWifiSecurity.get().equals(GwtWifiSecurity.netWifiSecurityWPA.name())) {
                return WifiSecurity.SECURITY_WPA.name();
            }

            if (gwtWifiSecurity.get().equals(GwtWifiSecurity.netWifiSecurityWPA2.name())) {
                return WifiSecurity.SECURITY_WPA2.name();
            }

            if (gwtWifiSecurity.get().equals(GwtWifiSecurity.netWifiSecurityWPA_WPA2.name())) {
                return WifiSecurity.SECURITY_WPA_WPA2.name();
            }
        }

        return WifiSecurity.NONE.name();
    }

    /**
     * Converts values of {@link WifiCiphers} to {@link GwtWifiCiphers} values
     * 
     */
    public static String getGwtWifiCiphers(Optional<String> wifiCiphers) {
        if (wifiCiphers.isPresent()) {
            if (wifiCiphers.get().equals(WifiCiphers.CCMP.name())) {
                return GwtWifiCiphers.netWifiCiphers_CCMP.name();
            }

            if (wifiCiphers.get().equals(WifiCiphers.TKIP.name())) {
                return GwtWifiCiphers.netWifiCiphers_TKIP.name();
            }

            if (wifiCiphers.get().equals(WifiCiphers.CCMP_TKIP.name())) {
                return GwtWifiCiphers.netWifiCiphers_CCMP_TKIP.name();
            }
        }

        return GwtWifiCiphers.netWifiCiphers_CCMP.name();
    }

    /**
     * Converts values of {@link GwtWifiCiphers} to {@link WifiCiphers} values
     * 
     */
    public static Optional<String> getWifiCiphers(Optional<String> gwtWifiCiphers) {
        if (gwtWifiCiphers.isPresent()) {
            if (gwtWifiCiphers.get().equals(GwtWifiCiphers.netWifiCiphers_CCMP.name())) {
                return Optional.of(WifiCiphers.CCMP.name());
            }

            if (gwtWifiCiphers.get().equals(GwtWifiCiphers.netWifiCiphers_CCMP_TKIP.name())) {
                return Optional.of(WifiCiphers.CCMP_TKIP.name());
            }

            if (gwtWifiCiphers.get().equals(GwtWifiCiphers.netWifiCiphers_TKIP.name())) {
                return Optional.of(WifiCiphers.TKIP.name());
            }
        }

        return Optional.empty();
    }

    /**
     * Converts values of {@link WifiRadioMode} to {@link GwtWifiRadioMode} values
     * 
     */
    public static Optional<String> getGwtWifiRadioMode(Optional<String> wifiRadioMode) {
        if (wifiRadioMode.isPresent()) {
            if (wifiRadioMode.get().equals(WifiRadioMode.RADIO_MODE_80211_AC.name())) {
                return Optional.of(GwtWifiRadioMode.netWifiRadioModeANAC.name());
            }

            if (wifiRadioMode.get().equals(WifiRadioMode.RADIO_MODE_80211a.name())) {
                return Optional.of(GwtWifiRadioMode.netWifiRadioModeA.name());
            }

            if (wifiRadioMode.get().equals(WifiRadioMode.RADIO_MODE_80211b.name())) {
                return Optional.of(GwtWifiRadioMode.netWifiRadioModeB.name());
            }

            if (wifiRadioMode.get().equals(WifiRadioMode.RADIO_MODE_80211g.name())) {
                return Optional.of(GwtWifiRadioMode.netWifiRadioModeBG.name());
            }

            if (wifiRadioMode.get().equals(WifiRadioMode.RADIO_MODE_80211nHT20.name()) ||
                    wifiRadioMode.get().equals(WifiRadioMode.RADIO_MODE_80211nHT40above.name()) ||
                    wifiRadioMode.get().equals(WifiRadioMode.RADIO_MODE_80211nHT40below.name())) {
                return Optional.of(GwtWifiRadioMode.netWifiRadioModeBGN.name());
            }
        }

        return Optional.of(GwtWifiRadioMode.netWifiRadioModeBGN.name());

    }

    /**
     * Converts values of {@link GwtWifiRadioMode} to {@link WifiRadioMode} values
     * 
     */
    public static Optional<String> getWifiRadioMode(Optional<String> gwtWifiRadioMode) {
        if (gwtWifiRadioMode.isPresent()) {
            if (gwtWifiRadioMode.get().equals(GwtWifiRadioMode.netWifiRadioModeA.name())) {
                return Optional.of(WifiRadioMode.RADIO_MODE_80211a.name());
            }

            if (gwtWifiRadioMode.get().equals(GwtWifiRadioMode.netWifiRadioModeANAC.name())) {
                return Optional.of(WifiRadioMode.RADIO_MODE_80211_AC.name());
            }

            if (gwtWifiRadioMode.get().equals(GwtWifiRadioMode.netWifiRadioModeB.name())) {
                return Optional.of(WifiRadioMode.RADIO_MODE_80211b.name());
            }

            if (gwtWifiRadioMode.get().equals(GwtWifiRadioMode.netWifiRadioModeBG.name())) {
                return Optional.of(WifiRadioMode.RADIO_MODE_80211g.name());
            }

            if (gwtWifiRadioMode.get().equals(GwtWifiRadioMode.netWifiRadioModeBGN.name())) {
                return Optional.of(WifiRadioMode.RADIO_MODE_80211nHT20.name());
            }
        }

        return Optional.empty();
    }

    /**
     * Converts values of {@link AuthType} to {@link GwtModemAuthType}
     * 
     */
    public static GwtModemAuthType getGwtModemAuthType(Optional<String> authType) {
        if (authType.isPresent()) {
            if (authType.get().equals(AuthType.AUTO.name())) {
                return GwtModemAuthType.netModemAuthAUTO;
            }

            if (authType.get().equals(AuthType.CHAP.name())) {
                return GwtModemAuthType.netModemAuthCHAP;
            }

            if (authType.get().equals(AuthType.PAP.name())) {
                return GwtModemAuthType.netModemAuthPAP;
            }
        }

        return GwtModemAuthType.netModemAuthNONE;
    }

    /**
     * Converts values of {@link GwtModemAuthType} to {@link AuthType.name()}
     * 
     */
    public static String getAuthType(Optional<GwtModemAuthType> gwtModemAuthType) {
        if (gwtModemAuthType.isPresent()) {
            switch (gwtModemAuthType.get()) {
                case netModemAuthAUTO:
                    return AuthType.AUTO.name();
                case netModemAuthCHAP:
                    return AuthType.CHAP.name();

                case netModemAuthPAP:
                    return AuthType.PAP.name();
                case netModemAuthNONE:
                default:
                    break;
            }
        }

        return AuthType.NONE.name();
    }

    /**
     * Converts values of {@link PdpType} to {@link GwtModemPdpType}
     * 
     */
    public static GwtModemPdpType getGwtModemPdpType(Optional<String> pdpType) {
        if (pdpType.isPresent()) {
            if (pdpType.get().equals(PdpType.IP.name())) {
                return GwtModemPdpType.netModemPdpIP;
            }

            if (pdpType.get().equals(PdpType.PPP.name())) {
                return GwtModemPdpType.netModemPdpPPP;
            }

            if (pdpType.get().equals(PdpType.IPv6.name())) {
                return GwtModemPdpType.netModemPdpIPv6;
            }
        }

        return GwtModemPdpType.netModemPdpUnknown;
    }

    /**
     * Converts values of {@link GwtModemPdpType} to {@link PdpType.name()}
     * 
     */
    public static String getPdpType(Optional<GwtModemPdpType> gwtModemPdpType) {
        if (gwtModemPdpType.isPresent()) {
            switch (gwtModemPdpType.get()) {
                case netModemPdpIP:
                    return PdpType.IP.name();
                case netModemPdpIPv6:
                    return PdpType.IPv6.name();
                case netModemPdpPPP:
                    return PdpType.PPP.name();
                case netModemPdpUnknown:
                default:
                    break;
            }
        }

        return PdpType.UNKNOWN.name();
    }

}

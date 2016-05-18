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
package org.eclipse.kura.net.admin.visitor.linux;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.Password;
import org.eclipse.kura.core.net.NetworkConfiguration;
import org.eclipse.kura.core.net.NetworkConfigurationVisitor;
import org.eclipse.kura.core.net.WifiInterfaceAddressConfigImpl;
import org.eclipse.kura.core.net.WifiInterfaceConfigImpl;
import org.eclipse.kura.crypto.CryptoService;
import org.eclipse.kura.linux.net.wifi.HostapdManager;
import org.eclipse.kura.net.NetConfig;
import org.eclipse.kura.net.NetInterfaceAddressConfig;
import org.eclipse.kura.net.NetInterfaceConfig;
import org.eclipse.kura.net.admin.visitor.linux.util.WifiVisitorUtil;
import org.eclipse.kura.net.wifi.WifiCiphers;
import org.eclipse.kura.net.wifi.WifiConfig;
import org.eclipse.kura.net.wifi.WifiInterfaceAddressConfig;
import org.eclipse.kura.net.wifi.WifiMode;
import org.eclipse.kura.net.wifi.WifiRadioMode;
import org.eclipse.kura.net.wifi.WifiSecurity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HostapdConfigReader implements NetworkConfigurationVisitor {

	private static final Logger s_logger = LoggerFactory.getLogger(HostapdConfigReader.class);

	private static HostapdConfigReader s_instance;

	public static HostapdConfigReader getInstance() {
		if (s_instance == null) {
			s_instance = new HostapdConfigReader();
		}

		return s_instance;
	}

	@Override
	public void visit(NetworkConfiguration config) throws KuraException {
		List<NetInterfaceConfig<? extends NetInterfaceAddressConfig>> netInterfaceConfigs = config
				.getNetInterfaceConfigs();

		for (NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig : netInterfaceConfigs) {
			if (netInterfaceConfig instanceof WifiInterfaceConfigImpl) {
				getConfig((WifiInterfaceConfigImpl) netInterfaceConfig);
			}
		}
	}

	private void getConfig(WifiInterfaceConfigImpl wifiInterfaceConfig)
			throws KuraException {
		String interfaceName = wifiInterfaceConfig.getName();
		s_logger.debug("Getting hostapd config for {}", interfaceName);

		List<WifiInterfaceAddressConfig> wifiInterfaceAddressConfigs = wifiInterfaceConfig
				.getNetInterfaceAddresses();

		if (wifiInterfaceAddressConfigs == null
				|| wifiInterfaceAddressConfigs.size() == 0) {
			wifiInterfaceAddressConfigs = new ArrayList<WifiInterfaceAddressConfig>();
			wifiInterfaceAddressConfigs
					.add(new WifiInterfaceAddressConfigImpl());
			wifiInterfaceConfig
					.setNetInterfaceAddresses(wifiInterfaceAddressConfigs);
		}

		for (WifiInterfaceAddressConfig wifiInterfaceAddressConfig : wifiInterfaceAddressConfigs) {
			if (wifiInterfaceAddressConfig instanceof WifiInterfaceAddressConfigImpl) {
				List<NetConfig> netConfigs = wifiInterfaceAddressConfig
						.getConfigs();

				if (netConfigs == null) {
					netConfigs = new ArrayList<NetConfig>();
					((WifiInterfaceAddressConfigImpl) wifiInterfaceAddressConfig)
							.setNetConfigs(netConfigs);
				}

				netConfigs.add(getWifiHostConfig(interfaceName));
			}
		}
	}

	private static WifiConfig getWifiHostConfig(String ifaceName)
			throws KuraException {
		try {
			WifiConfig wifiConfig = new WifiConfig();
			wifiConfig.setMode(WifiMode.MASTER);

			File configFile = new File(HostapdManager.getHostapdConfigFileName(ifaceName));
			Properties hostapdProps = new Properties();

			s_logger.debug("parsing hostapd config file: "
					+ configFile.getAbsolutePath());
			if (configFile.exists()) {
				FileInputStream fis = null;
				try {
					fis = new FileInputStream(configFile);
					hostapdProps.load(fis);
				} finally {
					if (null != fis) {
						fis.close();
					}
				}
				// remove any quotes around the values
				Enumeration<Object> keys = hostapdProps.keys();
				while (keys.hasMoreElements()) {
					String key = keys.nextElement().toString();
					String val = hostapdProps.getProperty(key);
					if (val.startsWith("\"") && val.endsWith("\"")
							&& val.length() > 1) {
						hostapdProps.setProperty(key,
								val.substring(1, val.length() - 1));
					}
				}

				String iface = hostapdProps.getProperty("interface");

				if (ifaceName != null && ifaceName.equals(iface)) {
					String driver = hostapdProps.getProperty("driver");
					String essid = hostapdProps.getProperty("ssid");
					int channel = Integer.parseInt(hostapdProps
							.getProperty("channel"));
					int ignoreSSID = Integer.parseInt(hostapdProps
							.getProperty("ignore_broadcast_ssid"));

					// Determine radio mode
					WifiRadioMode wifiRadioMode = null;
					String hwModeStr = hostapdProps.getProperty("hw_mode");
					if ("a".equals(hwModeStr)) {
						wifiRadioMode = WifiRadioMode.RADIO_MODE_80211a;
					} else if ("b".equals(hwModeStr)) {
						wifiRadioMode = WifiRadioMode.RADIO_MODE_80211b;
					} else if ("g".equals(hwModeStr)) {
						wifiRadioMode = WifiRadioMode.RADIO_MODE_80211g;
						if ("1".equals(hostapdProps.getProperty("ieee80211n"))) {
							wifiRadioMode = WifiRadioMode.RADIO_MODE_80211nHT20;
							String ht_capab = hostapdProps
									.getProperty("ht_capab");
							if (ht_capab != null) {
								if (ht_capab.contains("HT40+")) {
									wifiRadioMode = WifiRadioMode.RADIO_MODE_80211nHT40above;
								} else if (ht_capab.contains("HT40-")) {
									wifiRadioMode = WifiRadioMode.RADIO_MODE_80211nHT40below;
								}
							}
						}
					} else {
						throw KuraException
								.internalError("malformatted config file, unexpected hw_mode: "
										+ configFile.getAbsolutePath());
					}

					// Determine security and pass
					WifiSecurity security = WifiSecurity.SECURITY_NONE;
					String password = "";

					if (hostapdProps.containsKey("wpa")) {
						if ("1".equals(hostapdProps.getProperty("wpa"))) {
							security = WifiSecurity.SECURITY_WPA;
						} else if ("2".equals(hostapdProps.getProperty("wpa"))) {
							security = WifiSecurity.SECURITY_WPA2;
						} else if ("3".equals(hostapdProps.getProperty("wpa"))) {
							security = WifiSecurity.SECURITY_WPA_WPA2;
						} else {
							throw KuraException
									.internalError("malformatted config file: "
											+ configFile.getAbsolutePath());
						}

						if (hostapdProps.containsKey("wpa_passphrase")) {
							password = hostapdProps
									.getProperty("wpa_passphrase");
						} else if (hostapdProps.containsKey("wpa_psk")) {
							password = hostapdProps.getProperty("wpa_psk");
						} else {
							throw KuraException
									.internalError("malformatted config file, no wpa passphrase: "
											+ configFile.getAbsolutePath());
						}
					} else if (hostapdProps.containsKey("wep_key0")) {
						security = WifiSecurity.SECURITY_WEP;
						password = hostapdProps.getProperty("wep_key0");
					}
					if ((password != null) && !password.isEmpty()) {
						// get encrypted password from the /etc/hostapd.conf and decrypt it
						CryptoService cryptoService = WifiVisitorUtil.getCryptoService();
						if (cryptoService != null) {
							try {
								Password decryptedPassword = new Password(cryptoService.decryptAes(password.toCharArray()));
								password = decryptedPassword.toString();
							} catch (KuraException e) {
								s_logger.error("getWifiHostConfig() :: Failed to decrypt password={} - {}", password, e);
							}
						}
					} else {
						// get password from configuration snapshot and decrypt it
						password = WifiVisitorUtil.getPassphrase(ifaceName, WifiMode.MASTER);
					}
					
					WifiCiphers pairwise = null;
					if (hostapdProps.containsKey("wpa_pairwise")) {
						if ("TKIP".equals(hostapdProps.getProperty("wpa_pairwise"))) {
							pairwise = WifiCiphers.TKIP;
						} else if ("CCMP".equals(hostapdProps.getProperty("wpa_pairwise"))) {
							pairwise = WifiCiphers.CCMP;
						} else if ("CCMP TKIP".equals(hostapdProps.getProperty("wpa_pairwise"))) {
							pairwise = WifiCiphers.CCMP_TKIP;
						} else {
							throw KuraException
							.internalError("malformatted config file: "
									+ configFile.getAbsolutePath());
						}
					}

					// Populate the config
					wifiConfig.setSSID(essid);
					wifiConfig.setDriver(driver);
					wifiConfig.setChannels(new int[] { channel });
					wifiConfig.setPasskey(password);
					wifiConfig.setSecurity(security);
					wifiConfig.setPairwiseCiphers(pairwise);
					wifiConfig.setRadioMode(wifiRadioMode);

					if (ignoreSSID == 0) {
						wifiConfig.setIgnoreSSID(false);
						wifiConfig.setBroadcast(true);
					} else {
						wifiConfig.setIgnoreSSID(true);
						wifiConfig.setBroadcast(false);
					}

					// hw mode
					if (wifiRadioMode == WifiRadioMode.RADIO_MODE_80211b) {
						wifiConfig.setHardwareMode("b");
					} else if (wifiRadioMode == WifiRadioMode.RADIO_MODE_80211g) {
						wifiConfig.setHardwareMode("g");
					} else if (wifiRadioMode == WifiRadioMode.RADIO_MODE_80211nHT20
							|| wifiRadioMode == WifiRadioMode.RADIO_MODE_80211nHT40above
							|| wifiRadioMode == WifiRadioMode.RADIO_MODE_80211nHT40below) {

						// TODO: specify these 'n' modes separately?
						wifiConfig.setHardwareMode("n");
					}
				}
			} else {
				s_logger.warn("getWifiHostConfig() :: {} file doesn't exist, will generate default wifiConfig", configFile.getName());
				wifiConfig.setSSID("kura_gateway");
				wifiConfig.setDriver("nl80211");
				wifiConfig.setChannels(new int[] { 11 });
				wifiConfig.setPasskey("");
				wifiConfig.setSecurity(WifiSecurity.SECURITY_NONE);
				wifiConfig.setPairwiseCiphers(WifiCiphers.CCMP);
				wifiConfig.setRadioMode(WifiRadioMode.RADIO_MODE_80211b);
				wifiConfig.setIgnoreSSID(false);
				wifiConfig.setBroadcast(true);
				wifiConfig.setHardwareMode("b");
			}
			return wifiConfig;
		} catch (Exception e) {
			s_logger.error("Exception getting WiFi configuration", e);
			throw KuraException.internalError(e);
		}
	}
}

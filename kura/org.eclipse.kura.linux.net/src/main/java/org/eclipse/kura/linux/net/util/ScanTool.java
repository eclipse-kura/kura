package org.eclipse.kura.linux.net.util;

import java.util.Collection;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.linux.net.wifi.WifiOptions;

public abstract class ScanTool {
	
	public static IScanTool get(String ifaceName) throws KuraException {
		Collection<String> supportedWifiOptions = WifiOptions.getSupportedOptions(ifaceName);
		IScanTool scanTool = null;
		if ((supportedWifiOptions != null) && (supportedWifiOptions.size() > 0)) {
            if (supportedWifiOptions.contains(WifiOptions.WIFI_MANAGED_DRIVER_NL80211)) {
            	scanTool = new iwScanTool(ifaceName);
            } else if (supportedWifiOptions.contains(WifiOptions.WIFI_MANAGED_DRIVER_WEXT)) {
            	scanTool = new iwlistScanTool(ifaceName);
            }
		}
		return scanTool;
	}
}

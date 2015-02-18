package org.eclipse.kura.net.admin.monitor;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.linux.net.util.LinuxNetworkUtil;
import org.eclipse.kura.linux.net.wifi.HostapdManager;
import org.eclipse.kura.net.wifi.WifiMode;

public class WifiInterfaceState extends InterfaceState {

	/**
	 * WifiInterfaceState
	 * 
	 * @param interfaceName - interface name as {@link String}
	 * @param wifiMode configured wifi mode as {@link WifiMode}
	 * @throws KuraException
	 */
	public WifiInterfaceState(String interfaceName, WifiMode wifiMode) throws KuraException {
		super(interfaceName);
		if (WifiMode.MASTER.equals(wifiMode)) {
			if (HostapdManager.isRunning() && WifiMode.MASTER.equals(LinuxNetworkUtil.getWifiMode(interfaceName))) {
				m_link = true;
			}
		}
	}
}
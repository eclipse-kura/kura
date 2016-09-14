package org.eclipse.kura.linux.net.util;

import java.util.EnumSet;
import org.eclipse.kura.net.wifi.WifiSecurity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Consumes AP property lines from iw scan command.
 */
class IWSecuritySectionParser {
	private static final Logger s_logger = LoggerFactory.getLogger(IWSecuritySectionParser.class);
	
	private boolean foundGroup = false;
	private boolean foundPairwise = false;
	private boolean foundAuthSuites = false;
	
	private final EnumSet<WifiSecurity> security = EnumSet.noneOf(WifiSecurity.class);;
	
	public EnumSet<WifiSecurity> getWifiSecurityFlags() {
		return security;
	}
	
	/**
	 * @param line A trimmed line from a subsection in iw scan.
	 * @return true if we've got all of the security information for this parser
	 */
	public boolean parsePropLine(String line) {
		if(line.contains("Group cipher:")) {
			foundGroup = true;
			if(line.contains("CCMP")) {
				security.add(WifiSecurity.GROUP_CCMP);
			}
			if(line.contains("TKIP")) {
				security.add(WifiSecurity.GROUP_TKIP);
			}
			if(line.contains("WEP104")) {
				security.add(WifiSecurity.GROUP_WEP104);
			}
			if(line.contains("WEP40")) {
				security.add(WifiSecurity.GROUP_WEP40);
			}
		} else if(line.contains("Pairwise ciphers:")) {
			foundPairwise = true;
			if(line.contains("CCMP")) {
				security.add(WifiSecurity.PAIR_CCMP);
			}
			if(line.contains("TKIP")) {
				security.add(WifiSecurity.PAIR_TKIP);
			}
			if(line.contains("WEP104")) {
				security.add(WifiSecurity.PAIR_WEP104);
			}
			if(line.contains("WEP40")) {
				security.add(WifiSecurity.PAIR_WEP40);
			}
		} else if(line.contains("Authentication suites:")) {
			foundAuthSuites = true;
			if(line.contains("802_1X")) {
				security.add(WifiSecurity.KEY_MGMT_802_1X);
			}
			if(line.contains("PSK")) {
				security.add(WifiSecurity.KEY_MGMT_PSK);
			}
		} else {
			s_logger.debug("Ignoring line in section: {}", line);
		}
		
		return foundGroup && foundPairwise && foundAuthSuites;
	}
}

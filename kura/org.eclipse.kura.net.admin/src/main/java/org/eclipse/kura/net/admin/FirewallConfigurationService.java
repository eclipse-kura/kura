package org.eclipse.kura.net.admin;

import java.util.List;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.net.FirewallConfiguration;
import org.eclipse.kura.net.IPAddress;
import org.eclipse.kura.net.firewall.FirewallNatConfig;
import org.eclipse.kura.net.firewall.FirewallOpenPortConfigIP;
import org.eclipse.kura.net.firewall.FirewallPortForwardConfigIP;

public interface FirewallConfigurationService {

	public static final String PID = "org.eclipse.kura.net.admin.FirewallConfigurationService";
	
	public FirewallConfiguration getFirewallConfiguration() throws KuraException;
	
	public void setFirewallOpenPortConfiguration(
			List<FirewallOpenPortConfigIP<? extends IPAddress>> firewallConfiguration)
			throws KuraException;
	
	public void setFirewallPortForwardingConfiguration(
			List<FirewallPortForwardConfigIP<? extends IPAddress>> firewallConfiguration)
			throws KuraException;
	
	public void setFirewallNatConfiguration(List<FirewallNatConfig> natConfigs) throws KuraException;
}

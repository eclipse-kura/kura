package org.eclipse.kura.linux.net.dhcp;

public enum DhcpClientTool {
	NONE("none"),
	DHCLIENT("dhclient"),
	UDHCPC("udhcpc");
	
	private String m_toolName;
	
	private DhcpClientTool(String toolName) {
		m_toolName = toolName;
	}
	
	public String getValue() {
		return m_toolName;
	}
}

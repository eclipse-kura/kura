package org.eclipse.kura.linux.net.dhcp;

public enum DhcpServerTool {
	NONE("none"),
	DHCPD("dhcpd"),
	UDHCPD("udhcpd");
	
	private String m_toolName;
	
	private DhcpServerTool(String toolName) {
		m_toolName = toolName;
	}
	
	public String getValue() {
		return m_toolName;
	}
}

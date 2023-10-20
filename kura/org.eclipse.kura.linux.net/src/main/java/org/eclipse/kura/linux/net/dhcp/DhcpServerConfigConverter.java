package org.eclipse.kura.linux.net.dhcp;

import org.eclipse.kura.net.dhcp.DhcpServerConfig;

public interface DhcpServerConfigConverter {

    public String convert(DhcpServerConfig config);
}

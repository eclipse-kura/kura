package org.eclipse.kura.container.orchestration;

import java.util.LinkedList;
import java.util.List;

public class ContainerPort {
	
	private int internalPort;
	private int externalPort;
	private PortInternetProtocol internetProtocol;
	
	ContainerPort(int internalPort, int externalPort, PortInternetProtocol internetProtocol){
		this.internalPort = internalPort;
		this.externalPort = externalPort;
		this.internetProtocol = internetProtocol;	
	}
	
	ContainerPort(int internalPort, int externalPort){
		this.internalPort = internalPort;
		this.externalPort = externalPort;
		this.internetProtocol = PortInternetProtocol.TCP;	
	}
	
	
	public int getInternalPort() {
		return internalPort;
	}


	public int getExternalPort() {
		return externalPort;
	}


	public PortInternetProtocol getInternetProtocol() {
		return internetProtocol;
	}
	
	public static List<Integer> continerPortsListInternal(List<ContainerPort> ports) {
		List<Integer> portList = new LinkedList<>();
		for(ContainerPort port : ports) {
			portList.add(port.internalPort);
		}
		return portList;
	}
	
	public static List<Integer> continerPortsListExternal(List<ContainerPort> ports) {
		List<Integer> portList = new LinkedList<>();
		for(ContainerPort port : ports) {
			portList.add(port.externalPort);
		}
		return portList;
	}
	
	public static List<PortInternetProtocol> continerPortsListPortProtocols(List<ContainerPort> ports) {
		List<PortInternetProtocol> protocolList = new LinkedList<>();
		for(ContainerPort port : ports) {
			protocolList.add(port.internetProtocol);
		}
		return protocolList;
	}
	
	public static List<ContainerPort> parsePortString(String portString){
		List<ContainerPort> portList = new LinkedList<>();
	
		//expects a string link this "80:80/tcp, 443:443/tcp 888:888/udp".
		for(String portToken : portString.split(",")) {
			PortInternetProtocol pIP = PortInternetProtocol.TCP;
			
			if (portToken.split("/").length > 1) {
				switch(portToken.split("/")[1].toUpperCase().trim()) {
				case "UDP":
					pIP = PortInternetProtocol.UDP;
					break;
				case "SCTP":
					pIP = PortInternetProtocol.SCTP;
					break;
				default:
					pIP = PortInternetProtocol.TCP;
					break;
				}
			}
			int portInternal = Integer.parseInt(portToken.split("/")[0].split(":")[0]);
			int portExternal = Integer.parseInt(portToken.split("/")[0].split(":")[1]);
			
			portList.add(new ContainerPort(portInternal, portExternal, pIP));
		}
		
		return portList;
	}
	
    // Overriding equals() to compare two Complex objects
    @Override
    public boolean equals(Object o) {
    	
        if (o == this) {
            return true;
        }
 
        if (!(o instanceof ContainerPort)) {
            return false;
        }
    	ContainerPort cp = (ContainerPort) o;
    	return this.externalPort == cp.externalPort && this.internalPort == cp.internalPort && this.internetProtocol == cp.internetProtocol;
    }
    
}

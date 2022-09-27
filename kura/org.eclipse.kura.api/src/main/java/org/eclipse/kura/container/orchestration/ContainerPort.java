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

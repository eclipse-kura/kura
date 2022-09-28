package org.eclipse.kura.container.orchestration;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class ContainerPort {

    private int internalPort;
    private int externalPort;
    private PortInternetProtocol internetProtocol;

    public ContainerPort(int internalPort, int externalPort, PortInternetProtocol internetProtocol) {
        this.internalPort = internalPort;
        this.externalPort = externalPort;
        this.internetProtocol = internetProtocol;
    }

    public ContainerPort(int internalPort, int externalPort) {
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
        for (ContainerPort port : ports) {
            portList.add(port.internalPort);
        }
        return portList;
    }

    public static List<Integer> continerPortsListExternal(List<ContainerPort> ports) {
        List<Integer> portList = new LinkedList<>();
        for (ContainerPort port : ports) {
            portList.add(port.externalPort);
        }
        return portList;
    }

    public static List<PortInternetProtocol> continerPortsListPortProtocols(List<ContainerPort> ports) {
        List<PortInternetProtocol> protocolList = new LinkedList<>();
        for (ContainerPort port : ports) {
            protocolList.add(port.internetProtocol);
        }
        return protocolList;
    }

    @Override
    public int hashCode() {
        return Objects.hash(externalPort, internalPort, internetProtocol);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ContainerPort other = (ContainerPort) obj;
        return externalPort == other.externalPort && internalPort == other.internalPort
                && internetProtocol == other.internetProtocol;
    }

}

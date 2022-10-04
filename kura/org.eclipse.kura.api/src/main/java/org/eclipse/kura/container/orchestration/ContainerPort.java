/*******************************************************************************
 * Copyright (c) 2022 Eurotech and/or its affiliates and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Eurotech
 *******************************************************************************/
package org.eclipse.kura.container.orchestration;

import java.util.Objects;

/**
 * 
 * This class is used to represent a port mapping within a container.
 * 
 *
 * @since 2.5
 */
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

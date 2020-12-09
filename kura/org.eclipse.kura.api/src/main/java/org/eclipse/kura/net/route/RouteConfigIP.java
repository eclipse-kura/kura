/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 ******************************************************************************/
package org.eclipse.kura.net.route;

import org.eclipse.kura.net.IPAddress;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Base class for Route configurations
 *
 * @param <T>
 *
 * @noextend This class is not intended to be subclassed by clients.
 */
@ProviderType
public abstract class RouteConfigIP<T extends IPAddress> implements RouteConfig {

    private T destination;
    private T gateway;
    private T netmask;
    private String interfaceName;
    private int metric;

    public RouteConfigIP(T destination, T gateway, T netmask, String interfaceName, int metric) {
        super();

        this.destination = destination;
        this.gateway = gateway;
        this.netmask = netmask;
        this.interfaceName = interfaceName;
        this.metric = metric;
    }

    @Override
    public String getDescription() {
        StringBuffer desc = new StringBuffer();
        String gw;
        if (this.gateway == null) {
            gw = "default";
        } else {
            gw = this.gateway.getHostAddress();
        }
        desc.append("Destination: " + this.destination.getHostAddress() + ", " + "Gateway: " + gw + ", " + "Netmask: "
                + this.netmask.getHostAddress() + ", " + "Interface: " + this.interfaceName + ", " + "Metric: "
                + this.metric);
        return desc.toString();
    }

    @Override
    public T getDestination() {
        return this.destination;
    }

    public void setDestination(T destination) {
        this.destination = destination;
    }

    @Override
    public T getGateway() {
        return this.gateway;
    }

    public void setGateway(T gateway) {
        this.gateway = gateway;
    }

    @Override
    public T getNetmask() {
        return this.netmask;
    }

    public void setNetmask(T netmask) {
        this.netmask = netmask;
    }

    @Override
    public String getInterfaceName() {
        return this.interfaceName;
    }

    public void setInterfaceName(String interfaceName) {
        this.interfaceName = interfaceName;
    }

    @Override
    public int getMetric() {
        return this.metric;
    }

    public void setMetric(int metric) {
        this.metric = metric;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (this.destination == null ? 0 : this.destination.hashCode());
        result = prime * result + (this.gateway == null ? 0 : this.gateway.hashCode());
        result = prime * result + (this.interfaceName == null ? 0 : this.interfaceName.hashCode());
        result = prime * result + this.metric;
        result = prime * result + (this.netmask == null ? 0 : this.netmask.hashCode());
        return result;
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
        @SuppressWarnings("rawtypes")
        RouteConfigIP other = (RouteConfigIP) obj;
        if (this.destination == null) {
            if (other.destination != null) {
                return false;
            }
        } else if (!this.destination.equals(other.destination)) {
            return false;
        }
        if (this.gateway == null) {
            if (other.gateway != null) {
                return false;
            }
        } else if (!this.gateway.equals(other.gateway)) {
            return false;
        }
        if (this.interfaceName == null) {
            if (other.interfaceName != null) {
                return false;
            }
        } else if (!this.interfaceName.equals(other.interfaceName)) {
            return false;
        }
        if (this.metric != other.metric) {
            return false;
        }
        if (this.netmask == null) {
            if (other.netmask != null) {
                return false;
            }
        } else if (!this.netmask.equals(other.netmask)) {
            return false;
        }
        return true;
    }

    @Override
    public boolean isValid() {
        if (this.destination == null || this.gateway == null || this.netmask == null
                || this.interfaceName == null) {
            return false;
        }

        return true;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("RouteConfigIP [m_destination=");
        builder.append(this.destination);
        builder.append(", m_gateway=");
        builder.append(this.gateway);
        builder.append(", m_netmask=");
        builder.append(this.netmask);
        builder.append(", m_interfaceName=");
        builder.append(this.interfaceName);
        builder.append(", m_metric=");
        builder.append(this.metric);
        builder.append("]");
        return builder.toString();
    }
}

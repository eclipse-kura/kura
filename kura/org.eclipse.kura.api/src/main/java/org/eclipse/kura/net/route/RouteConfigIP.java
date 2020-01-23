/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
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

    private T m_destination;
    private T m_gateway;
    private T m_netmask;
    private String m_interfaceName;
    private int m_metric;

    public RouteConfigIP(T destination, T gateway, T netmask, String interfaceName, int metric) {
        super();

        this.m_destination = destination;
        this.m_gateway = gateway;
        this.m_netmask = netmask;
        this.m_interfaceName = interfaceName;
        this.m_metric = metric;
    }

    @Override
    public String getDescription() {
        StringBuffer desc = new StringBuffer();
        String gw;
        if (this.m_gateway == null) {
            gw = "default";
        } else {
            gw = this.m_gateway.getHostAddress();
        }
        desc.append("Destination: " + this.m_destination.getHostAddress() + ", " + "Gateway: " + gw + ", " + "Netmask: "
                + this.m_netmask.getHostAddress() + ", " + "Interface: " + this.m_interfaceName + ", " + "Metric: "
                + this.m_metric);
        return desc.toString();
    }

    @Override
    public T getDestination() {
        return this.m_destination;
    }

    public void setDestination(T destination) {
        this.m_destination = destination;
    }

    @Override
    public T getGateway() {
        return this.m_gateway;
    }

    public void setGateway(T gateway) {
        this.m_gateway = gateway;
    }

    @Override
    public T getNetmask() {
        return this.m_netmask;
    }

    public void setNetmask(T netmask) {
        this.m_netmask = netmask;
    }

    @Override
    public String getInterfaceName() {
        return this.m_interfaceName;
    }

    public void setInterfaceName(String interfaceName) {
        this.m_interfaceName = interfaceName;
    }

    @Override
    public int getMetric() {
        return this.m_metric;
    }

    public void setMetric(int metric) {
        this.m_metric = metric;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (this.m_destination == null ? 0 : this.m_destination.hashCode());
        result = prime * result + (this.m_gateway == null ? 0 : this.m_gateway.hashCode());
        result = prime * result + (this.m_interfaceName == null ? 0 : this.m_interfaceName.hashCode());
        result = prime * result + this.m_metric;
        result = prime * result + (this.m_netmask == null ? 0 : this.m_netmask.hashCode());
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
        if (this.m_destination == null) {
            if (other.m_destination != null) {
                return false;
            }
        } else if (!this.m_destination.equals(other.m_destination)) {
            return false;
        }
        if (this.m_gateway == null) {
            if (other.m_gateway != null) {
                return false;
            }
        } else if (!this.m_gateway.equals(other.m_gateway)) {
            return false;
        }
        if (this.m_interfaceName == null) {
            if (other.m_interfaceName != null) {
                return false;
            }
        } else if (!this.m_interfaceName.equals(other.m_interfaceName)) {
            return false;
        }
        if (this.m_metric != other.m_metric) {
            return false;
        }
        if (this.m_netmask == null) {
            if (other.m_netmask != null) {
                return false;
            }
        } else if (!this.m_netmask.equals(other.m_netmask)) {
            return false;
        }
        return true;
    }

    @Override
    public boolean isValid() {
        if (this.m_destination == null || this.m_gateway == null || this.m_netmask == null
                || this.m_interfaceName == null) {
            return false;
        }

        return true;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("RouteConfigIP [m_destination=");
        builder.append(this.m_destination);
        builder.append(", m_gateway=");
        builder.append(this.m_gateway);
        builder.append(", m_netmask=");
        builder.append(this.m_netmask);
        builder.append(", m_interfaceName=");
        builder.append(this.m_interfaceName);
        builder.append(", m_metric=");
        builder.append(this.m_metric);
        builder.append("]");
        return builder.toString();
    }
}

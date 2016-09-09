/*******************************************************************************
 * Copyright (c) 2016 Red Hat Inc and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Inc - initial API and implementation
 *******************************************************************************/
package org.eclipse.kura.camel.cloud.factory.internal;

public class ServiceConfiguration {
    private String xml;
    private Integer serviceRanking;

    /**
     * Set the router XML
     * @param xml must not be {@code null}
     */
    public void setXml(String xml) {
        this.xml = xml;
    }

    public String getXml() {
        return xml;
    }

    public void setServiceRanking(Integer serviceRanking) {
        this.serviceRanking = serviceRanking;
    }

    public Integer getServiceRanking() {
        return serviceRanking;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((serviceRanking == null) ? 0 : serviceRanking.hashCode());
        result = prime * result + ((xml == null) ? 0 : xml.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ServiceConfiguration other = (ServiceConfiguration) obj;
        if (serviceRanking == null) {
            if (other.serviceRanking != null)
                return false;
        } else if (!serviceRanking.equals(other.serviceRanking))
            return false;
        if (xml == null) {
            if (other.xml != null)
                return false;
        } else if (!xml.equals(other.xml))
            return false;
        return true;
    }

    public boolean isValid() {
        if (this.xml == null || this.xml.trim().isEmpty()) {
            return false;
        }
        return true;
    }

}

/*******************************************************************************
 * Copyright (c) 2016, 2017 Red Hat Inc and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Inc
 *******************************************************************************/
package org.eclipse.kura.camel.cloud.factory.internal;

import org.eclipse.kura.cloud.CloudService;

/**
 * The configuration of a Camel based {@link CloudService} instance
 */
public class ServiceConfiguration {

    private String xml;
    private String initCode;
    private boolean enableJmx;

    /**
     * Set the router XML
     *
     * @param xml
     *            must not be {@code null}
     */
    public void setXml(String xml) {
        this.xml = xml;
    }

    public String getXml() {
        return this.xml;
    }

    public void setInitCode(String initCode) {
        this.initCode = initCode;
    }

    public String getInitCode() {
        return this.initCode;
    }

    public void setEnableJmx(boolean enableJmx) {
        this.enableJmx = enableJmx;
    }

    public boolean isEnableJmx() {
        return enableJmx;
    }

    public boolean isValid() {
        if (this.xml == null || this.xml.trim().isEmpty()) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (this.enableJmx ? 1231 : 1237);
        result = prime * result + (this.initCode == null ? 0 : this.initCode.hashCode());
        result = prime * result + (this.xml == null ? 0 : this.xml.hashCode());
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
        ServiceConfiguration other = (ServiceConfiguration) obj;
        if (this.enableJmx != other.enableJmx) {
            return false;
        }
        if (this.initCode == null) {
            if (other.initCode != null) {
                return false;
            }
        } else if (!this.initCode.equals(other.initCode)) {
            return false;
        }
        if (this.xml == null) {
            if (other.xml != null) {
                return false;
            }
        } else if (!this.xml.equals(other.xml)) {
            return false;
        }
        return true;
    }

}

/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.web.shared.model;

import java.io.Serializable;

import org.eclipse.kura.web.client.util.KuraBaseModel;

public class GwtCloudEntry extends KuraBaseModel implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = -8526545631929926543L;

    public String getPid() {
        return get("pid");
    }

    public void setPid(String pid) {
        set("pid", pid);
    }

    public String getFactoryPid() {
        return get("factoryPid");
    }

    public void setFactoryPid(final String factoryPid) {
        set("factoryPid", factoryPid);
    }
    
    public String getDefaultFactoryPid() {
        return get("defaultFactoryPid");
    }

    public void setDefaultFactoryPid(final String defaultFactoryPid) {
        set("defaultFactoryPid", defaultFactoryPid);
    }
    
    public String getDefaultFactoryPidRegex() {
        return get("defaultFactoryPidRegex");
    }

    public void setDefaultFactoryPidRegex(final String defaultFactoryPidRegex) {
        set("defaultFactoryPidRegex", defaultFactoryPidRegex);
    }

    @Override
    public int hashCode() {
        final String pid = getPid();
        final int prime = 31;
        int result = 1;
        result = prime * result + ((pid == null) ? 0 : pid.hashCode());
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
        GwtCloudEntry other = (GwtCloudEntry) obj;

        final String pid = getPid();
        final String otherPid = other.getPid();

        if (pid == null) {
            if (otherPid != null)
                return false;
        } else if (!pid.equals(otherPid))
            return false;
        return true;
    }

}

/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.web.shared.model;

import java.io.Serializable;

public class GwtSession extends GwtBaseModel implements Serializable {

    private static final long serialVersionUID = -2507268464782812398L;

    private boolean m_netAdminAvailable;
    private String m_kuraVersion;
    private String m_osVersion;
    private boolean m_developMode;

    public GwtSession() {
        this.m_netAdminAvailable = true;
        this.m_kuraVersion = "version-unknown";
    }

    public boolean isNetAdminAvailable() {
        return this.m_netAdminAvailable;
    }

    public void setNetAdminAvailable(boolean haveNetAdmin) {
        this.m_netAdminAvailable = haveNetAdmin;
    }

    public String getKuraVersion() {
        return this.m_kuraVersion;
    }

    public void setKuraVersion(String kuraVersion) {
        this.m_kuraVersion = kuraVersion;
    }

    public String getOsVersion() {
        return this.m_osVersion;
    }

    public void setOsVersion(String osVersion) {
        this.m_osVersion = osVersion;
    }

    public boolean isDevelopMode() {
        return this.m_developMode;
    }

    public void setDevelopMode(boolean developMode) {
        this.m_developMode = developMode;
    }
}

/**
 * Copyright (c) 2011, 2014 Eurotech and/or its affiliates
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Eurotech
 */
package org.eclipse.kura.web.shared.model;

import java.io.Serializable;

import com.extjs.gxt.ui.client.data.BaseModel;

public class GwtSession extends BaseModel implements Serializable 
{
	private static final long serialVersionUID = -2507268464782812398L;

	private boolean m_netAdminAvailable;
	private String m_kuraVersion;
	private String m_osVersion;
	
	public GwtSession()
	{
		m_netAdminAvailable = true;
		m_kuraVersion = "version-unknown";
	}

	public boolean isNetAdminAvailable() {
		return m_netAdminAvailable;
	}

	public void setNetAdminAvailable(boolean haveNetAdmin) {
		this.m_netAdminAvailable = haveNetAdmin;
	}
	
	public String getKuraVersion() {
		return m_kuraVersion;
	}
	
	public void setKuraVersion(String kuraVersion) {
		m_kuraVersion = kuraVersion;
	}
	
	public String getOsVersion() {
		return m_osVersion;
	}
	
	public void setOsVersion(String osVersion) {
		m_osVersion = osVersion;
	}
}

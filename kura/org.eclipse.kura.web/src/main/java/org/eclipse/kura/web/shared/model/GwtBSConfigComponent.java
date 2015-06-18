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
import java.util.ArrayList;

public class GwtBSConfigComponent extends GwtBSBaseModel implements Serializable {

	private static final long serialVersionUID = -6388356998309026758L;
	

	private ArrayList<GwtBSConfigParameter> m_parameters;
	
	public GwtBSConfigComponent() {
		super();
		m_parameters = new ArrayList<GwtBSConfigParameter>();		
	}
	
    public String getComponentId() {
        return (String) get("componentId");
    }

    public void setComponentId(String componentId) {
        set("componentId", componentId);
    }

    public String getComponentName() {
        return (String) get("componentName");
    }

    public void setComponentName(String componentName) {
        set("componentName", componentName);
    }

    public String getComponentDescription() {
        return (String) get("componentDescription");
    }

    public void setComponentDescription(String componentDescription) {
        set("componentDescription", componentDescription);
    }

    public String getComponentIcon() {
        return (String) get("componentIcon");
    }

    public void setComponentIcon(String componentIcon) {
        set("componentIcon", componentIcon);
    }

	public ArrayList<GwtBSConfigParameter> getParameters() {
		return m_parameters;
	}

	public void setParameters(ArrayList<GwtBSConfigParameter> parameters) {
		m_parameters = parameters;
	}
	
	public GwtBSConfigParameter getParameter(String id) {
		for (GwtBSConfigParameter param : m_parameters) {
			if (param.getId().equals(id)) {
				return param;
			}
		}
		return null;
	}
}

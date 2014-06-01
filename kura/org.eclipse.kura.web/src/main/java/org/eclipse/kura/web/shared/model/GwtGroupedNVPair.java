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

import org.eclipse.kura.web.client.messages.ValidationMessages;

import com.extjs.gxt.ui.client.data.BaseModel;
import com.google.gwt.core.client.GWT;

public class GwtGroupedNVPair extends BaseModel implements Serializable 
{
	private static final long serialVersionUID = 6017065568183482351L;

	
	public GwtGroupedNVPair()
	{}
	
	public GwtGroupedNVPair(String group, String name, String value) {
		setGroup(group);
		setName(name);
		setValue(value);
	}
	
	public void setGroup(String group) {
		set("group", group); 	
	}
	
	public String getGroup() {
		return get("group");
	}
	
	public String getGroupLoc() {
		return get("groupLoc");
	}

	public void setId(String id) {
		set("id", id); 	
	}
	
	public String getId() {
		return get("id");
	}

	public void setName(String name) {
		set("name", name); 	
	}
	
	public String getName() {
		return get("name");
	}

	public String getNameLoc() {
		return get("nameLoc");
	}
	
	public void setValue(String value) {
		set("value", value); 	
	}
	
	public String getValue() {
		return get("value");
	}

	public void setStatus(String status) {
		set("status", status); 	
	}
	
	public String getStatus() {
		return get("status");
	}

	public String getStatusLoc() {
		return get("statusLoc");
	}

	public void setVersion(String version) {
		set("version", version); 	
	}
	
	public String getVersion() {
		return get("version");
	}

	@Override
	@SuppressWarnings("unchecked")
    public <X> X get(String property) {
    	if ("groupLoc".equals(property)) {
    		ValidationMessages msgs = GWT.create(ValidationMessages.class);
    		return (X) msgs.getString(getGroup());
    	}
    	else if ("nameLoc".equals(property)) {
    		ValidationMessages msgs = GWT.create(ValidationMessages.class);
    		return (X) msgs.getString(getName());
    	}
    	else if ("statusLoc".equals(property)) {
    		ValidationMessages msgs = GWT.create(ValidationMessages.class);
    		return (X) msgs.getString(getStatus());
    	}
    	else {
    		return super.get(property);
    	}
    }
}

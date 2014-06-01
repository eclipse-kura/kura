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

public class GwtBundleInfo extends BaseModel implements Serializable {

	private static final long serialVersionUID = -7285859217584861659L;

	public void setName(String name) {
		set("name", name);
	}
	
	public String getName() {
		return (String) get("name");
	}
	
	public void setVersion(String version) {
		set("version", version);
	}
	
	public String getVersion() {
		return (String) get("version");
	}
}

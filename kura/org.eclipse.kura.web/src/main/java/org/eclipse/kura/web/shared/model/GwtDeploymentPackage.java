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
import java.util.List;

import com.extjs.gxt.ui.client.data.BaseModel;

public class GwtDeploymentPackage extends BaseModel implements Serializable {

	private static final long serialVersionUID = -7648638193931336835L;
		
	// Needed to prevent serialization errors
	@SuppressWarnings("unused")
	private GwtBundleInfo unused;

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
	
	public List<GwtBundleInfo> getBundleInfos() {
		return get("bundles");
	}

	public void setBundleInfos(List<GwtBundleInfo> bundleInfos) {
		set("bundles", bundleInfos);
	}
}

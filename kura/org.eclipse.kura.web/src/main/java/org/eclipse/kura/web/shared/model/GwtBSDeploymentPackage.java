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
import java.util.HashMap;
import java.util.List;


public class GwtBSDeploymentPackage extends GwtBSBaseModel implements Serializable {

	private static final long serialVersionUID = -7648638193931336835L;
		
	public GwtBSDeploymentPackage() {
		super();
	}

	// Needed to prevent serialization errors
	@SuppressWarnings("unused")
	private GwtBSBundleInfo unused;

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
	
	public List<GwtBSBundleInfo> getBundleInfos() {
		return (List<GwtBSBundleInfo>) get("bundles");
	}

	public void setBundleInfos(List<GwtBSBundleInfo> bundleInfos) {
		set("bundles", bundleInfos);
	}


}

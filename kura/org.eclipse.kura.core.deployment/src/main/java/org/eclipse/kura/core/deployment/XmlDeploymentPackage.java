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
package org.eclipse.kura.core.deployment;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="package")
@XmlAccessorType(XmlAccessType.FIELD)
public class XmlDeploymentPackage {
	
	@XmlElement(name="name")
	public String name;
	
	@XmlElement(name="version")
	public String version;
	
	@XmlElementWrapper(name="bundles")
	@XmlElement(name="bundle")
	public XmlBundleInfo[] bundleInfos;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public XmlBundleInfo[] getBundleInfos() {
		return bundleInfos;
	}

	public void setBundleInfos(XmlBundleInfo[] bundleInfos) {
		this.bundleInfos = bundleInfos;
	}
}

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
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="bundles")
@XmlAccessorType(XmlAccessType.FIELD)
public class XmlBundles {
	
	@XmlElement(name="bundle")
	public XmlBundle[] bundles;

	public XmlBundle[] getBundles() {
		return bundles;
	}

	public void setBundles(XmlBundle[] bundles) {
		this.bundles = bundles;
	}
}

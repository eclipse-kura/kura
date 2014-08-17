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
package org.eclipse.kura.core.configuration;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

/**
 * Helper class to serialize a property in XML.
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class XmlConfigPropertiesAdapted 
{
	@XmlElement(name="property",namespace="http://eurotech.com/esf/2.0")
	private XmlConfigPropertyAdapted[] properties;
	
	public XmlConfigPropertiesAdapted()
	{}

	public XmlConfigPropertyAdapted[] getProperties() {
		return properties;
	}

	public void setProperties(XmlConfigPropertyAdapted[] properties) {
		this.properties = properties;
	}
}

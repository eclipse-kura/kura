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

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Utility class to serialize a set of configurations.
 * This is used to serialize a full snapshot.
 */
@XmlRootElement(name="configurations",namespace="http://eurotech.com/esf/2.0")
@XmlAccessorType(XmlAccessType.FIELD)
public class XmlComponentConfigurations 
{
	@XmlElement(name="configuration",namespace="http://eurotech.com/esf/2.0")
	private List<ComponentConfigurationImpl> configurations;
	
	public XmlComponentConfigurations()
	{}

	public List<ComponentConfigurationImpl> getConfigurations() {
		return configurations;
	}

	public void setConfigurations(List<ComponentConfigurationImpl> configurations) {
		this.configurations = configurations;
	}
}

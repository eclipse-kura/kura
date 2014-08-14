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

import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.core.configuration.metatype.Tocd;

@XmlRootElement(name="configuration")
@XmlAccessorType(XmlAccessType.FIELD)
public class ComponentConfigurationImpl implements ComponentConfiguration 
{
	@XmlAttribute(name="pid")
	protected String pid;
	
	@XmlElementRef
	protected Tocd definition;

	@XmlElement(name="properties")
	@XmlJavaTypeAdapter(XmlConfigPropertiesAdapter.class)
	protected Map<String,Object> properties;

	// Required by JAXB
	public ComponentConfigurationImpl()
	{}
	
	public ComponentConfigurationImpl(String pid,
									  Tocd  definition, 
									  Map<String,Object> properties)
	{
		super();
		this.pid = pid;
		this.definition = definition;
		this.properties = properties;
	}

	public String getPid() {
		return pid;
	}

	public Tocd  getDefinition() {
		return definition;
	}

	public Map<String,Object> getConfigurationProperties() {
		return properties;
	}

	public void setPid(String pid) {
		this.pid = pid;
	}

	public void setDefinition(Tocd definition) {
		this.definition = definition;
	}

	public void setProperties(Map<String, Object> properties) {
		this.properties = properties;
	}	
}

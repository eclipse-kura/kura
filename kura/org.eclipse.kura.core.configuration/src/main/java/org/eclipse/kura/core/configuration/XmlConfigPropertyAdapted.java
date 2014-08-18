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
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;


/**
 * Helper class to serialize a property in XML.
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class XmlConfigPropertyAdapted 
{
	@XmlEnum
	public enum ConfigPropertyType {
		@XmlEnumValue("String")    stringType,
		@XmlEnumValue("Long")      longType,
		@XmlEnumValue("Double")    doubleType,
		@XmlEnumValue("Float")     floatType,
		@XmlEnumValue("Integer")   integerType,
		@XmlEnumValue("Byte")      byteType,
		@XmlEnumValue("Char")      charType,
		@XmlEnumValue("Boolean")   booleanType,
		@XmlEnumValue("Short")     shortType,
		@XmlEnumValue("Password")  passwordType
	}

	@XmlAttribute(name="name")
	private String             name;

	@XmlAttribute(name="array")
	private boolean            array;
	
	@XmlAttribute(name="encrypted")
	private boolean            encrypted;

	@XmlAttribute(name="type")
	private ConfigPropertyType type;

	@XmlElement(name="value",namespace="http://eurotech.com/esf/2.0")
	private String[]           values;
	
	
	public XmlConfigPropertyAdapted()
	{}	
	
	public XmlConfigPropertyAdapted(String name,
									ConfigPropertyType type, 
								    String[] values) 
	{
		super();

		this.type      = type;
		this.values    = values;
		this.encrypted = false;   
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean getArray() {
		return array;
	}

	public void setArray(boolean array) {
		this.array = array;
	}

	public ConfigPropertyType getType() {
		return type;
	}

	public void setType(ConfigPropertyType type) {
		this.type = type;
	}	
	
	public boolean isEncrypted() {
		return encrypted;
	}

	public void setEncrypted(boolean encrypted) {
		this.encrypted = encrypted;
	}

	public String[] getValues() {
		return values;
	}

	public void setValues(String[] values) {
		this.values = values;
	}
}

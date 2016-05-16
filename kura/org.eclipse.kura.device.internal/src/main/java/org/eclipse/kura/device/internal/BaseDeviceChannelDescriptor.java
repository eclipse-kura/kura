/**
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Eurotech
 *   Amit Kumar Mondal (admin@amitinside.com)
 */
package org.eclipse.kura.device.internal;

import java.util.List;

import org.eclipse.kura.core.configuration.metatype.Tad;
import org.eclipse.kura.core.configuration.metatype.Toption;
import org.eclipse.kura.core.configuration.metatype.Tscalar;

import com.google.common.collect.Lists;

/**
 * The Class BaseDeviceChannelDescriptor returns the default channel descriptor
 * required for a base device
 */
public final class BaseDeviceChannelDescriptor {

	/** Name Property to be used in the configuration */
	private static final String NAME = "name";

	/** Type Property to be used in the configuration */
	private static final String TYPE = "type";

	/**
	 * Gets the default descriptor
	 *
	 * @return the default descriptor
	 */
	public static BaseDeviceChannelDescriptor getDefault() {
		return new BaseDeviceChannelDescriptor();
	}

	/** The default elements. */
	private final List<Tad> defaultElements;

	/**
	 * Instantiates a new base device channel descriptor.
	 */
	private BaseDeviceChannelDescriptor() {
		this.defaultElements = Lists.newArrayList();

		final Tad name = new Tad();
		name.setId(NAME);
		name.setName(NAME);
		name.setType(Tscalar.STRING);
		name.setDefault("field name");
		name.setDescription("Name of the Point");
		name.setCardinality(0);
		name.setRequired(true);

		this.defaultElements.add(name);

		final Tad type = new Tad();
		type.setName(TYPE);
		type.setId(TYPE);
		type.setDescription("Primitive type of the Point");
		type.setType(Tscalar.STRING);
		type.setRequired(true);
		type.setDefault("String");

		final Toption oBoolean = new Toption();
		oBoolean.setValue("Boolean");
		oBoolean.setLabel("Boolean");
		type.getOption().add(oBoolean);

		final Toption oByte = new Toption();
		oByte.setValue("Byte");
		oByte.setLabel("Byte");
		type.getOption().add(oByte);

		final Toption oDouble = new Toption();
		oDouble.setValue("Double");
		oDouble.setLabel("Double");
		type.getOption().add(oDouble);

		final Toption oInteger = new Toption();
		oInteger.setValue("Integer");
		oInteger.setLabel("Integer");
		type.getOption().add(oInteger);
		final Toption oLong = new Toption();
		oLong.setValue("Long");
		oLong.setLabel("Long");
		type.getOption().add(oLong);

		final Toption oByteArray = new Toption();
		oByteArray.setValue("Byte Array");
		oByteArray.setLabel("Byte Array");
		type.getOption().add(oByteArray);

		final Toption oShort = new Toption();
		oShort.setValue("Short");
		oShort.setLabel("Short");
		type.getOption().add(oShort);

		final Toption oString = new Toption();
		oString.setValue("String");
		oString.setLabel("String");
		type.getOption().add(oString);

		this.defaultElements.add(type);
	}

	/**
	 * Gets the default configuration.
	 *
	 * @return the default configuration
	 */
	public List<Tad> getDefaultConfiguration() {
		return this.defaultElements;
	}

}

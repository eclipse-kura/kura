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

import org.eclipse.kura.KuraRuntimeException;
import org.eclipse.kura.core.configuration.metatype.Tad;
import org.eclipse.kura.core.configuration.metatype.Toption;
import org.eclipse.kura.core.configuration.metatype.Tscalar;
import org.eclipse.kura.localization.DeviceMessages;
import org.eclipse.kura.localization.LocalizationAdapter;

import com.google.common.collect.Lists;

/**
 * The Class BaseDeviceChannelDescriptor returns the default channel descriptor
 * required for a base device
 */
final class BaseDeviceChannelDescriptor {

	/** Name Property to be used in the configuration */
	private static final String NAME = "name";

	/** Localization Resource */
	private static final DeviceMessages s_message = LocalizationAdapter.adapt(DeviceMessages.class);

	/** Type Property to be used in the configuration */
	private static final String TYPE = "type";

	/** The default elements. */
	private final List<Tad> m_defaultElements;

	/**
	 * Instantiates a new base device channel descriptor.
	 *
	 * @throws KuraRuntimeException
	 *             if argument is null
	 */
	private BaseDeviceChannelDescriptor() {
		this.m_defaultElements = Lists.newArrayList();

		final Tad name = new Tad();
		name.setId(NAME);
		name.setName(NAME);
		name.setType(Tscalar.STRING);
		name.setDefault(s_message.fieldName());
		name.setDescription(s_message.pointName());
		name.setCardinality(0);
		name.setRequired(true);

		this.m_defaultElements.add(name);

		final Tad type = new Tad();
		type.setName(TYPE);
		type.setId(TYPE);
		type.setDescription(s_message.typePoint());
		type.setType(Tscalar.STRING);
		type.setRequired(true);
		type.setDefault(s_message.string());

		final Toption oBoolean = new Toption();
		oBoolean.setValue(s_message.booleanString());
		oBoolean.setLabel(s_message.booleanString());
		type.getOption().add(oBoolean);

		final Toption oByte = new Toption();
		oByte.setValue(s_message.byteStr());
		oByte.setLabel(s_message.byteStr());
		type.getOption().add(oByte);

		final Toption oDouble = new Toption();
		oDouble.setValue(s_message.doubleStr());
		oDouble.setLabel(s_message.doubleStr());
		type.getOption().add(oDouble);

		final Toption oInteger = new Toption();
		oInteger.setValue(s_message.integerStr());
		oInteger.setLabel(s_message.integerStr());
		type.getOption().add(oInteger);
		final Toption oLong = new Toption();
		oLong.setValue(s_message.longStr());
		oLong.setLabel(s_message.longStr());
		type.getOption().add(oLong);

		final Toption oByteArray = new Toption();
		oByteArray.setValue(s_message.byteArray());
		oByteArray.setLabel(s_message.byteArray());
		type.getOption().add(oByteArray);

		final Toption oShort = new Toption();
		oShort.setValue(s_message.shortStr());
		oShort.setLabel(s_message.shortStr());
		type.getOption().add(oShort);

		final Toption oString = new Toption();
		oString.setValue(s_message.string());
		oString.setLabel(s_message.string());
		type.getOption().add(oString);

		this.m_defaultElements.add(type);
	}

	/**
	 * Gets the default configuration.
	 *
	 * @return the default configuration
	 */
	List<Tad> getDefaultConfiguration() {
		return this.m_defaultElements;
	}
	
	/**
	 * Gets the default descriptor
	 *
	 * @return the default descriptor
	 */
	static BaseDeviceChannelDescriptor getDefault() {
		return new BaseDeviceChannelDescriptor();
	}

}

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
package org.eclipse.kura.internal.wire.asset;

import static org.eclipse.kura.asset.AssetConstants.NAME;
import static org.eclipse.kura.asset.AssetConstants.TYPE;
import static org.eclipse.kura.asset.AssetConstants.VALUE_TYPE;

import java.util.List;

import org.eclipse.kura.asset.AssetConfiguration;
import org.eclipse.kura.core.configuration.metatype.Tad;
import org.eclipse.kura.core.configuration.metatype.Toption;
import org.eclipse.kura.core.configuration.metatype.Tscalar;
import org.eclipse.kura.driver.ChannelDescriptor;
import org.eclipse.kura.localization.LocalizationAdapter;
import org.eclipse.kura.localization.resources.WireMessages;
import org.eclipse.kura.util.collection.CollectionUtil;

/**
 * The Class BaseChannelDescriptor returns the basic channel descriptor required
 * for a channel<br/>
 * <br/>
 *
 * The basic descriptions include the following
 * <ul>
 * <li>name</li> denotes the name of the channel
 * <li>type</li>
 * <li>value.type</li>
 * </ul>
 *
 * The <b><i>type</i></b> would be one of the following:
 * <ul>
 * <li>READ</li>
 * <li>WRITE</li>
 * <li>READ_WRITE</li>
 * </ul>
 *
 * The <b><i>value.type</i></b> would be one of the following:
 * <ul>
 * <li>INTEGER</li>
 * <li>DOUBLE</li>
 * <li>BYTE</li>
 * <li>SHORT</li>
 * <li>LONG</li>
 * <li>BOOLEAN</li>
 * <li>STRING</li>
 * <li>BYTE_ARRAY</li>
 * </ul>
 *
 * @see AssetConfiguration
 */
public final class BaseChannelDescriptor implements ChannelDescriptor {

	/** Localization Resource */
	private static final WireMessages s_message = LocalizationAdapter.adapt(WireMessages.class);

	/** The default elements. */
	private final List<Tad> m_defaultElements;

	/**
	 * Instantiates a new base asset channel descriptor.
	 */
	public BaseChannelDescriptor() {
		this.m_defaultElements = CollectionUtil.newArrayList();

		final Tad name = new Tad();
		name.setId(NAME.value());
		name.setName(NAME.value());
		name.setType(Tscalar.STRING);
		name.setDefault(s_message.string());
		name.setDescription(s_message.pointName());
		name.setCardinality(0);
		name.setRequired(true);

		this.m_defaultElements.add(name);

		final Tad type = new Tad();
		type.setName(TYPE.value());
		type.setId(TYPE.value());
		type.setDescription(s_message.type());
		type.setType(Tscalar.STRING);
		type.setRequired(true);
		type.setDefault(s_message.string());

		final Toption oRead = new Toption();
		oRead.setValue(s_message.read());
		oRead.setLabel(s_message.read());
		type.getOption().add(oRead);

		final Toption oWrite = new Toption();
		oWrite.setValue(s_message.write());
		oWrite.setLabel(s_message.write());
		type.getOption().add(oWrite);

		final Toption oReadWrite = new Toption();
		oReadWrite.setValue(s_message.readWrite());
		oReadWrite.setLabel(s_message.readWrite());
		type.getOption().add(oReadWrite);

		this.m_defaultElements.add(type);

		final Tad valueType = new Tad();
		valueType.setName(VALUE_TYPE.value());
		valueType.setId(VALUE_TYPE.value());
		valueType.setDescription(s_message.typePoint());
		valueType.setType(Tscalar.STRING);
		valueType.setRequired(true);
		valueType.setDefault(s_message.string());

		final Toption oBoolean = new Toption();
		oBoolean.setValue(s_message.booleanString());
		oBoolean.setLabel(s_message.booleanString());
		valueType.getOption().add(oBoolean);

		final Toption oByte = new Toption();
		oByte.setValue(s_message.byteStr());
		oByte.setLabel(s_message.byteStr());
		valueType.getOption().add(oByte);

		final Toption oDouble = new Toption();
		oDouble.setValue(s_message.doubleStr());
		oDouble.setLabel(s_message.doubleStr());
		valueType.getOption().add(oDouble);

		final Toption oInteger = new Toption();
		oInteger.setValue(s_message.integerStr());
		oInteger.setLabel(s_message.integerStr());
		valueType.getOption().add(oInteger);

		final Toption oLong = new Toption();
		oLong.setValue(s_message.longStr());
		oLong.setLabel(s_message.longStr());
		valueType.getOption().add(oLong);

		final Toption oByteArray = new Toption();
		oByteArray.setValue(s_message.byteArray());
		oByteArray.setLabel(s_message.byteArray());
		valueType.getOption().add(oByteArray);

		final Toption oShort = new Toption();
		oShort.setValue(s_message.shortStr());
		oShort.setLabel(s_message.shortStr());
		valueType.getOption().add(oShort);

		final Toption oString = new Toption();
		oString.setValue(s_message.string());
		oString.setLabel(s_message.string());
		valueType.getOption().add(oString);

		this.m_defaultElements.add(valueType);
	}

	/** {@inheritDoc} */
	@Override
	public Object getDescriptor() {
		return this.m_defaultElements;
	}

}

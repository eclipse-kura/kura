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
package org.eclipse.kura.internal.driver.s7plc;

import java.util.List;

import org.eclipse.kura.core.configuration.metatype.Tad;
import org.eclipse.kura.core.configuration.metatype.Tscalar;
import org.eclipse.kura.driver.ChannelDescriptor;
import org.eclipse.kura.localization.LocalizationAdapter;
import org.eclipse.kura.localization.resources.S7PlcMessages;
import org.eclipse.kura.util.collection.CollectionUtil;

/**
 * S7 PLC specific channel descriptor. The descriptor contains the following
 * attribute definition identifiers.
 *
 * <ul>
 * <li>area.no</li> denotes the Area Number
 * <li>offset</li> the offset
 * </ul>
 */
public final class S7PlcChannelDescriptor implements ChannelDescriptor {

	/** Localization Resource. */
	private static final S7PlcMessages s_message = LocalizationAdapter.adapt(S7PlcMessages.class);

	/** The descriptor elements. */
	private List<Tad> m_elements;

	/** {@inheritDoc} */
	@Override
	public Object getDescriptor() {
		this.m_elements = CollectionUtil.newArrayList();

		final Tad areaNo = new Tad();
		areaNo.setName(s_message.areaNo());
		areaNo.setId("area.no");
		areaNo.setDescription(s_message.areaNo());
		areaNo.setType(Tscalar.INTEGER);
		areaNo.setRequired(true);
		areaNo.setDefault("0");

		this.m_elements.add(areaNo);

		final Tad offset = new Tad();
		offset.setName(s_message.offset());
		offset.setId("offset");
		offset.setDescription(s_message.offset());
		offset.setType(Tscalar.INTEGER);
		offset.setRequired(true);
		offset.setDefault("0");

		this.m_elements.add(offset);

		return this.m_elements;
	}

}

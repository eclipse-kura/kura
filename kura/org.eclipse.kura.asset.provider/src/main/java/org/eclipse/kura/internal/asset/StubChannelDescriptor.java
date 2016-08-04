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
package org.eclipse.kura.internal.asset;

import java.util.List;

import org.eclipse.kura.core.configuration.metatype.Tad;
import org.eclipse.kura.core.configuration.metatype.Tscalar;
import org.eclipse.kura.driver.ChannelDescriptor;
import org.eclipse.kura.util.collection.CollectionUtil;

public class StubChannelDescriptor implements ChannelDescriptor {

	private List<Tad> m_elements;

	@Override
	public Object getDescriptor() {
		this.m_elements = CollectionUtil.newArrayList();
		final Tad unitId = new Tad();
		unitId.setId("unit.id");
		unitId.setName("unit.id");
		unitId.setType(Tscalar.INTEGER);
		unitId.setDefault("");
		unitId.setDescription("unit.desc");
		unitId.setCardinality(0);
		unitId.setRequired(true);

		this.m_elements.add(unitId);
		return this.m_elements;
	}

}

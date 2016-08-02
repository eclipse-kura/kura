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
import org.eclipse.kura.core.configuration.metatype.Toption;
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
 * <li>dave.area</li> denotes the Dave Area
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

		final Tad daveArea = new Tad();
		daveArea.setName(s_message.daveArea());
		daveArea.setId("dave.area");
		daveArea.setDescription(s_message.daveArea());
		daveArea.setType(Tscalar.STRING);
		daveArea.setRequired(true);
		daveArea.setDefault("");

		final Toption analogInputs = new Toption();
		analogInputs.setValue(s_message.analogInput());
		analogInputs.setLabel(s_message.analogInput());
		daveArea.getOption().add(analogInputs);

		final Toption analogOutputs = new Toption();
		analogOutputs.setValue(s_message.analogOutput());
		analogOutputs.setLabel(s_message.analogOutput());
		daveArea.getOption().add(analogOutputs);

		final Toption counter = new Toption();
		counter.setValue(s_message.counter());
		counter.setLabel(s_message.counter());
		daveArea.getOption().add(counter);

		final Toption counter200 = new Toption();
		counter200.setValue(s_message.counter200());
		counter200.setLabel(s_message.counter200());
		daveArea.getOption().add(counter200);

		final Toption db = new Toption();
		db.setValue(s_message.db());
		db.setLabel(s_message.db());
		daveArea.getOption().add(db);

		final Toption di = new Toption();
		di.setValue(s_message.di());
		di.setLabel(s_message.di());
		daveArea.getOption().add(di);

		final Toption flags = new Toption();
		flags.setValue(s_message.flags());
		flags.setLabel(s_message.flags());
		daveArea.getOption().add(flags);

		final Toption inputs = new Toption();
		inputs.setValue(s_message.inputs());
		inputs.setLabel(s_message.inputs());
		daveArea.getOption().add(inputs);

		final Toption local = new Toption();
		local.setValue(s_message.local());
		local.setLabel(s_message.local());
		daveArea.getOption().add(local);

		final Toption outputs = new Toption();
		outputs.setValue(s_message.outputs());
		outputs.setLabel(s_message.outputs());
		daveArea.getOption().add(outputs);

		final Toption p = new Toption();
		p.setValue(s_message.p());
		p.setLabel(s_message.p());
		daveArea.getOption().add(p);

		final Toption sysinfo = new Toption();
		sysinfo.setValue(s_message.sysInfo());
		sysinfo.setLabel(s_message.sysInfo());
		daveArea.getOption().add(sysinfo);

		final Toption sysflags = new Toption();
		sysflags.setValue(s_message.sysFlags());
		sysflags.setLabel(s_message.sysFlags());
		daveArea.getOption().add(sysflags);

		final Toption timer = new Toption();
		timer.setValue(s_message.timer());
		timer.setLabel(s_message.timer());
		daveArea.getOption().add(timer);

		final Toption timer200 = new Toption();
		timer200.setValue(s_message.timer200());
		timer200.setLabel(s_message.timer200());
		daveArea.getOption().add(timer200);

		final Toption v = new Toption();
		v.setValue(s_message.v());
		v.setLabel(s_message.v());
		daveArea.getOption().add(v);

		this.m_elements.add(daveArea);

		final Tad areaNo = new Tad();
		areaNo.setName(s_message.areaNo());
		areaNo.setId("register.count");
		areaNo.setDescription(s_message.areaNo());
		areaNo.setType(Tscalar.INTEGER);
		areaNo.setRequired(true);
		areaNo.setDefault("0");

		this.m_elements.add(areaNo);

		final Tad offset = new Tad();
		offset.setName(s_message.offset());
		offset.setId("register.count");
		offset.setDescription(s_message.offset());
		offset.setType(Tscalar.INTEGER);
		offset.setRequired(true);
		offset.setDefault("0");

		this.m_elements.add(offset);

		return this.m_elements;
	}

}

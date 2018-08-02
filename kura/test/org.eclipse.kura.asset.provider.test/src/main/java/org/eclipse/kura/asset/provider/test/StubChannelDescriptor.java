/*******************************************************************************
 * Copyright (c) 2016, 2018 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *******************************************************************************/
package org.eclipse.kura.asset.provider.test;

import java.util.List;

import org.eclipse.kura.core.configuration.metatype.Tad;
import org.eclipse.kura.core.configuration.metatype.Tscalar;
import org.eclipse.kura.driver.ChannelDescriptor;
import org.eclipse.kura.util.collection.CollectionUtil;

/**
 * The Stub Channel Descriptor
 */
public class StubChannelDescriptor implements ChannelDescriptor {

    /** {@inheritDoc} */
    @Override
    public Object getDescriptor() {
        final List<Tad> elements = CollectionUtil.newArrayList();
        final Tad unitId = new Tad();
        unitId.setId("unit.id");
        unitId.setName("unit.id");
        unitId.setType(Tscalar.INTEGER);
        unitId.setDefault("5");
        unitId.setDescription("unit.desc");
        unitId.setCardinality(0);
        unitId.setRequired(true);

        elements.add(unitId);
        return elements;
    }

}

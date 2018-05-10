/**
 * Copyright (c) 2018 Eurotech and/or its affiliates and others
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Eurotech
 */
package org.eclipse.kura.internal.driver.eddystone;

import java.util.List;
import java.util.Map;

import org.eclipse.kura.configuration.metatype.Option;
import org.eclipse.kura.core.configuration.metatype.Tad;
import org.eclipse.kura.core.configuration.metatype.Toption;
import org.eclipse.kura.core.configuration.metatype.Tscalar;
import org.eclipse.kura.driver.ChannelDescriptor;
import org.eclipse.kura.util.collection.CollectionUtil;

/**
 * Eddystone specific channel descriptor. The descriptor contains the following
 * attribute definition identifier.
 *
 * <ul>
 * <li>eddystone.type</li> denotes the type of Eddystones the channel is interested on. Possible values are:
 * <li>UID</li>
 * <li>URL</li>
 * </ul>
 */
public final class EddystoneChannelDescriptor implements ChannelDescriptor {

    private static final String EDDYSTONE_TYPE = "eddystone.type";

    private static void addOptions(Tad target, Enum<?>[] values) {
        final List<Option> options = target.getOption();
        for (Enum<?> value : values) {
            Toption option = new Toption();
            option.setLabel(value.name());
            option.setValue(value.name());
            options.add(option);
        }
    }

    @Override
    public Object getDescriptor() {
        final List<Tad> elements = CollectionUtil.newArrayList();

        final Tad eddystoneType = new Tad();
        eddystoneType.setName(EDDYSTONE_TYPE);
        eddystoneType.setId(EDDYSTONE_TYPE);
        eddystoneType.setDescription(EDDYSTONE_TYPE);
        eddystoneType.setType(Tscalar.STRING);
        eddystoneType.setRequired(true);
        eddystoneType.setDefault(EddystoneFrameType.UID.toString());
        addOptions(eddystoneType, EddystoneFrameType.values());

        elements.add(eddystoneType);
        return elements;
    }

    static EddystoneFrameType getEddystoneType(Map<String, Object> properties) {
        return EddystoneFrameType.valueOf((String) properties.get(EDDYSTONE_TYPE));
    }

}

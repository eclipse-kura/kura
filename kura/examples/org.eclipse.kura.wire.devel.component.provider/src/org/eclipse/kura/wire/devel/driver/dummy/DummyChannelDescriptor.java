/*******************************************************************************
 * Copyright (c) 2018, 2020 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 *******************************************************************************/
package org.eclipse.kura.wire.devel.driver.dummy;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.kura.core.configuration.metatype.Tad;
import org.eclipse.kura.core.configuration.metatype.Tscalar;
import org.eclipse.kura.driver.ChannelDescriptor;
import org.eclipse.kura.wire.devel.Property;

public final class DummyChannelDescriptor implements ChannelDescriptor {

    private static final Property<String> VALUE = new Property<>("value", "0");
    private static final DummyChannelDescriptor INSTANCE = new DummyChannelDescriptor();

    private final List<Tad> ads;

    private DummyChannelDescriptor() {
        this.ads = initAttributes();
    }

    private static List<Tad> initAttributes() {
        final Tad value = new Tad();
        value.setName(VALUE.getKey());
        value.setId(VALUE.getKey());
        value.setDescription("The value to be emitted for this channel");
        value.setType(Tscalar.STRING);
        value.setRequired(true);
        value.setDefault(VALUE.getDefaultValue());

        return Collections.singletonList(value);
    }

    public static DummyChannelDescriptor instance() {
        return INSTANCE;
    }

    public static String getValue(final Map<String, Object> channelConfig) {
        return VALUE.get(channelConfig);
    }

    @Override
    public Object getDescriptor() {
        return ads;
    }
}

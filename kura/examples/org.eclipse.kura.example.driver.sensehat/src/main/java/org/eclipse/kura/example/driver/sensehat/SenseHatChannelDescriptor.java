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

package org.eclipse.kura.example.driver.sensehat;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.kura.configuration.metatype.Option;
import org.eclipse.kura.core.configuration.metatype.Tad;
import org.eclipse.kura.core.configuration.metatype.Toption;
import org.eclipse.kura.core.configuration.metatype.Tscalar;
import org.eclipse.kura.driver.ChannelDescriptor;

public class SenseHatChannelDescriptor implements ChannelDescriptor {

    public static final String SENSEHAT_RESOURCE_PROP_NAME = "resource";
    private static final SenseHatChannelDescriptor INSTANCE = new SenseHatChannelDescriptor();

    private final List<Tad> properties = new ArrayList<>(1);

    private static void addOptions(Tad target, Enum<?>[] values) {
        final List<Option> options = target.getOption();
        for (Enum<?> value : values) {
            Toption option = new Toption();
            option.setLabel(value.name());
            option.setValue(value.name());
            options.add(option);
        }
    }

    public SenseHatChannelDescriptor() {

        final Tad value = new Tad();
        value.setName(SENSEHAT_RESOURCE_PROP_NAME);
        value.setId(SENSEHAT_RESOURCE_PROP_NAME);
        value.setDescription(SENSEHAT_RESOURCE_PROP_NAME);
        value.setType(Tscalar.STRING);
        value.setRequired(true);
        value.setDefault(Resource.TEMPERATURE_FROM_HUMIDITY.name());

        addOptions(value, Resource.values());

        properties.add(value);
    }

    @Override
    public Object getDescriptor() {
        return properties;
    }

    public static SenseHatChannelDescriptor instance() {
        return INSTANCE;
    }

}

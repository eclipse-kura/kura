/*******************************************************************************
 * Copyright (c) 2016, 2018 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *     Amit Kumar Mondal
 *     
 *******************************************************************************/
package org.eclipse.kura.asset.provider;

import static org.eclipse.kura.asset.provider.AssetConstants.NAME;
import static org.eclipse.kura.asset.provider.AssetConstants.TYPE;
import static org.eclipse.kura.asset.provider.AssetConstants.VALUE_TYPE;

import java.util.List;

import org.eclipse.kura.asset.AssetConfiguration;
import org.eclipse.kura.channel.ChannelType;
import org.eclipse.kura.configuration.metatype.Option;
import org.eclipse.kura.core.configuration.metatype.Tad;
import org.eclipse.kura.core.configuration.metatype.Toption;
import org.eclipse.kura.core.configuration.metatype.Tscalar;
import org.eclipse.kura.driver.ChannelDescriptor;
import org.eclipse.kura.type.DataType;
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
 * <li>FLOAT</li>
 * <li>LONG</li>
 * <li>BOOLEAN</li>
 * <li>STRING</li>
 * <li>BYTE_ARRAY</li>
 * </ul>
 *
 * @see AssetConfiguration
 */
public class BaseChannelDescriptor implements ChannelDescriptor {

    private static final BaseChannelDescriptor instance = new BaseChannelDescriptor();

    protected final List<Tad> defaultElements;

    protected static void addOptions(Tad target, Enum<?>[] values) {
        final List<Option> options = target.getOption();
        for (Enum<?> value : values) {
            final String name = value.name();
            final Toption option = new Toption();
            option.setLabel(name);
            option.setValue(name);
            options.add(option);
        }
    }

    /**
     * Instantiates a new base asset channel descriptor.
     */
    protected BaseChannelDescriptor() {
        this.defaultElements = CollectionUtil.newArrayList();

        final Tad enabled = new Tad();
        enabled.setId(AssetConstants.ENABLED.value());
        enabled.setName(AssetConstants.ENABLED.value().substring(1));
        enabled.setType(Tscalar.BOOLEAN);
        enabled.setDefault("true");
        enabled.setDescription("Determines if the channel is enabled or not");
        enabled.setCardinality(0);
        enabled.setRequired(true);

        this.defaultElements.add(enabled);

        final Tad name = new Tad();
        name.setId(NAME.value());
        name.setName(NAME.value().substring(1));
        name.setType(Tscalar.STRING);
        name.setDefault("Channel-1");
        name.setDescription("Name of the Channel");
        name.setCardinality(0);
        name.setRequired(true);

        this.defaultElements.add(name);

        final Tad type = new Tad();
        type.setName(TYPE.value().substring(1));
        type.setId(TYPE.value());
        type.setDescription("Type of the channel");
        type.setType(Tscalar.STRING);
        type.setRequired(true);
        type.setDefault(ChannelType.READ.name());

        addOptions(type, ChannelType.values());

        this.defaultElements.add(type);

        final Tad valueType = new Tad();
        valueType.setName(VALUE_TYPE.value().substring(1));
        valueType.setId(VALUE_TYPE.value());
        valueType.setDescription("Value type of the channel");
        valueType.setType(Tscalar.STRING);
        valueType.setRequired(true);
        valueType.setDefault(DataType.INTEGER.name());

        addOptions(valueType, DataType.values());

        this.defaultElements.add(valueType);
    }

    /** {@inheritDoc} */
    @Override
    public Object getDescriptor() {
        return this.defaultElements;
    }

    public static BaseChannelDescriptor get() {
        return instance;
    }
}

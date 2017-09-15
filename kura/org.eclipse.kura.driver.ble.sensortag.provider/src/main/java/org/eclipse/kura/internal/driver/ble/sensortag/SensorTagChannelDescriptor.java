/**
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Eurotech
 */
package org.eclipse.kura.internal.driver.ble.sensortag;

import java.util.List;
import java.util.Map;

import org.eclipse.kura.configuration.metatype.Option;
import org.eclipse.kura.core.configuration.metatype.Tad;
import org.eclipse.kura.core.configuration.metatype.Toption;
import org.eclipse.kura.core.configuration.metatype.Tscalar;
import org.eclipse.kura.driver.ChannelDescriptor;
import org.eclipse.kura.util.collection.CollectionUtil;

/**
 * BLE SensorTag specific channel descriptor. The descriptor contains the following
 * attribute definition identifier.
 *
 * <ul>
 * <li>sensor.name</li> denotes the BLE SensorTag sensor name
 * <li>sensortag.address</li> denotes the BLE SensorTag BD address
 * index.
 * </ul>
 */
public final class SensorTagChannelDescriptor implements ChannelDescriptor {

    private static final String SENSOR_NAME = "sensor.name";
    private static final String SENSORTAG_ADDRESS = "sensortag.address";

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

        final Tad sensorName = new Tad();
        sensorName.setName(SENSOR_NAME);
        sensorName.setId(SENSOR_NAME);
        sensorName.setDescription(SENSOR_NAME);
        sensorName.setType(Tscalar.STRING);
        sensorName.setRequired(true);
        sensorName.setDefault("TEMP_AMBIENT");

        addOptions(sensorName, SensorName.values());

        final Tad sensorTagAddress = new Tad();
        sensorTagAddress.setName(SENSORTAG_ADDRESS);
        sensorTagAddress.setId(SENSORTAG_ADDRESS);
        sensorTagAddress.setDescription(SENSORTAG_ADDRESS);
        sensorTagAddress.setType(Tscalar.STRING);
        sensorTagAddress.setRequired(true);
        sensorTagAddress.setDefault("AA:BB:CC:DD:EE:FF");
        elements.add(sensorTagAddress);

        elements.add(sensorName);
        return elements;
    }

    static String getsensorTagAddress(Map<String, Object> properties) {
        return (String) properties.get(SENSORTAG_ADDRESS);
    }

    static SensorName getSensorName(Map<String, Object> properties) {
        return SensorName.valueOf((String) properties.get(SENSOR_NAME));
    }

}

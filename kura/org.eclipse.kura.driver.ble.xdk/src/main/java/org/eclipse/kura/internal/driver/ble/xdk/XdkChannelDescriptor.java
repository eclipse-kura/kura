/**
 * Copyright (c) 2019 Eurotech and/or its affiliates and others
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Eurotech
 */
package org.eclipse.kura.internal.driver.ble.xdk;

import java.util.List;
import java.util.Map;

import org.eclipse.kura.configuration.metatype.Option;
import org.eclipse.kura.core.configuration.metatype.Tad;
import org.eclipse.kura.core.configuration.metatype.Toption;
import org.eclipse.kura.core.configuration.metatype.Tscalar;
import org.eclipse.kura.driver.ChannelDescriptor;
import org.eclipse.kura.util.collection.CollectionUtil;

public final class XdkChannelDescriptor implements ChannelDescriptor {

    private static final String SENSOR_NAME = "sensor.name";
    private static final String XDK_ADDRESS = "xdk.address";
    private static final String NOTIFICATION_PERIOD = "notification.period";

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
        sensorName.setDefault("TEMPERATURE");
        addOptions(sensorName, SensorName.values());

        final Tad xdkAddress = new Tad();
        xdkAddress.setName(XDK_ADDRESS);
        xdkAddress.setId(XDK_ADDRESS);
        xdkAddress.setDescription(XDK_ADDRESS);
        xdkAddress.setType(Tscalar.STRING);
        xdkAddress.setRequired(true);
        xdkAddress.setDefault("AA:BB:CC:DD:EE:FF");

        final Tad notificationPeriod = new Tad();
        notificationPeriod.setName(NOTIFICATION_PERIOD);
        notificationPeriod.setId(NOTIFICATION_PERIOD);
        notificationPeriod.setDescription(NOTIFICATION_PERIOD);
        notificationPeriod.setType(Tscalar.INTEGER);
        notificationPeriod.setRequired(true);
        notificationPeriod.setDefault("100");

        elements.add(xdkAddress);
        elements.add(sensorName);
        elements.add(notificationPeriod);
        return elements;
    }

    static String getXdkAddress(Map<String, Object> properties) {
        return (String) properties.get(XDK_ADDRESS);
    }

    static SensorName getSensorName(Map<String, Object> properties) {
        return SensorName.valueOf((String) properties.get(SENSOR_NAME));
    }

    static int getNotificationPeriod(Map<String, Object> properties) {
        return Integer.parseInt((String) properties.get(NOTIFICATION_PERIOD));
    }

}

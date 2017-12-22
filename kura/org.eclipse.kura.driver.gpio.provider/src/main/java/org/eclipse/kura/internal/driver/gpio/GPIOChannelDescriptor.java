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
package org.eclipse.kura.internal.driver.gpio;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.kura.configuration.metatype.Option;
import org.eclipse.kura.core.configuration.metatype.Tad;
import org.eclipse.kura.core.configuration.metatype.Toption;
import org.eclipse.kura.core.configuration.metatype.Tscalar;
import org.eclipse.kura.driver.ChannelDescriptor;
import org.eclipse.kura.gpio.GPIOService;
import org.eclipse.kura.gpio.KuraGPIODirection;
import org.eclipse.kura.gpio.KuraGPIOTrigger;

/**
 * GPIO specific channel descriptor. The descriptor contains the following
 * attribute definition identifier.
 *
 * <ul>
 * <li>resource.name</li> denotes the GPIO number/identifier
 * <li>resource.direction</li> denotes the GPIO direction
 * <li>resource.mode</li> denotes the GPIO output/input mode
 * <li>resource.trigger</li> denotes the GPIO event that triggers a listener
 * </ul>
 */
public final class GPIOChannelDescriptor implements ChannelDescriptor {

    protected static final String DEFAULT_RESOURCE_NAME = "#select resource";
    protected static final String DEFAULT_RESOURCE_DIRECTION = "#select direction";

    private static final String RESOURCE_ENABLE = "resource.enable";
    private static final String RESOURCE_NAME = "resource.name";
    private static final String RESOURCE_DIRECTION = "resource.direction";
    private static final String RESOURCE_TRIGGER = "resource.trigger";

    private List<GPIOService> gpioServices;

    public GPIOChannelDescriptor(List<GPIOService> gpioServices) {
        this.gpioServices = gpioServices;
    }

    private static void addResourceNames(Tad target, Map<Integer, String> values, String defaultValue) {
        final List<Option> options = target.getOption();
        for (Entry<Integer, String> value : values.entrySet()) {
            Toption option = new Toption();
            option.setLabel(value.getValue());
            option.setValue(value.getValue());
            options.add(option);
        }
        if (defaultValue != null && !defaultValue.isEmpty()) {
            Toption option = new Toption();
            option.setLabel(defaultValue);
            option.setValue(defaultValue);
            options.add(option);
        }
    }

    private static void addOptions(Tad target, Enum<?>[] values, String defaultValue) {
        final List<Option> options = target.getOption();
        for (Enum<?> value : values) {
            Toption option = new Toption();
            option.setLabel(value.name());
            option.setValue(value.name());
            options.add(option);
        }
        if (defaultValue != null && !defaultValue.isEmpty()) {
            Toption option = new Toption();
            option.setLabel(defaultValue);
            option.setValue(defaultValue);
            options.add(option);
        }
    }

    @Override
    public Object getDescriptor() {
        final List<Tad> elements = new ArrayList<>();

        final Tad resourceEnable = new Tad();
        resourceEnable.setName(RESOURCE_ENABLE);
        resourceEnable.setId(RESOURCE_ENABLE);
        resourceEnable.setDescription(RESOURCE_ENABLE);
        resourceEnable.setType(Tscalar.BOOLEAN);
        resourceEnable.setRequired(true);
        resourceEnable.setDefault("false");
        elements.add(resourceEnable);

        Map<Integer, String> availablePins = new HashMap<>();
        for (GPIOService service : this.gpioServices) {
            availablePins.putAll(service.getAvailablePins());
        }

        final Tad resourceName = new Tad();
        resourceName.setName(RESOURCE_NAME);
        resourceName.setId(RESOURCE_NAME);
        resourceName.setDescription(RESOURCE_NAME);
        resourceName.setType(Tscalar.STRING);
        resourceName.setRequired(true);
        resourceName.setDefault(DEFAULT_RESOURCE_NAME);
        addResourceNames(resourceName, availablePins, DEFAULT_RESOURCE_NAME);
        elements.add(resourceName);

        final Tad resourceDirection = new Tad();
        resourceDirection.setName(RESOURCE_DIRECTION);
        resourceDirection.setId(RESOURCE_DIRECTION);
        resourceDirection.setDescription(RESOURCE_DIRECTION);
        resourceDirection.setType(Tscalar.STRING);
        resourceDirection.setRequired(true);
        resourceDirection.setDefault(DEFAULT_RESOURCE_DIRECTION);
        addOptions(resourceDirection, KuraGPIODirection.values(), DEFAULT_RESOURCE_DIRECTION);
        elements.add(resourceDirection);

        final Tad resourceTriggers = new Tad();
        resourceTriggers.setName(RESOURCE_TRIGGER);
        resourceTriggers.setId(RESOURCE_TRIGGER);
        resourceTriggers.setDescription(RESOURCE_TRIGGER);
        resourceTriggers.setType(Tscalar.STRING);
        resourceTriggers.setRequired(true);
        resourceTriggers.setDefault(KuraGPIOTrigger.NONE.name());
        addOptions(resourceTriggers, KuraGPIOTrigger.values(), null);
        elements.add(resourceTriggers);

        return elements;
    }

    static Boolean isResourceEnabled(Map<String, Object> properties) {
        return Boolean.valueOf((String) properties.get(RESOURCE_ENABLE));
    }

    static String getResourceName(Map<String, Object> properties) {
        return (String) properties.get(RESOURCE_NAME);
    }

    static KuraGPIODirection getResourceDirection(Map<String, Object> properties) {
        String direction = (String) properties.get(RESOURCE_DIRECTION);
        if (DEFAULT_RESOURCE_DIRECTION.equals(direction)) {
            return null;
        } else {
            return KuraGPIODirection.valueOf(direction);
        }
    }

    static KuraGPIOTrigger getResourceTrigger(Map<String, Object> properties) {
        return KuraGPIOTrigger.valueOf((String) properties.get(RESOURCE_TRIGGER));
    }

}

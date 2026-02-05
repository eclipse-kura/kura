/*******************************************************************************
 * Copyright (c) 2011, 2026 Eurotech and/or its affiliates and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Eurotech
 ******************************************************************************/
package org.eclipse.kura.emulator.gpio;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.kura.gpio.GPIOService;
import org.eclipse.kura.gpio.KuraGPIODescription;
import org.eclipse.kura.gpio.KuraGPIODirection;
import org.eclipse.kura.gpio.KuraGPIOMode;
import org.eclipse.kura.gpio.KuraGPIOPin;
import org.eclipse.kura.gpio.KuraGPIOTrigger;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GpioServiceImpl implements GPIOService {

    private static final Logger logger = LoggerFactory.getLogger(GpioServiceImpl.class);

    private final HashMap<Integer, String> pins = new HashMap<>();
    private final List<KuraGPIODescription> pinDescriptions = new ArrayList<>();

    protected void activate(ComponentContext componentContext) {
        logger.debug("activating emulated GPIOService");
    }

    protected void deactivate(ComponentContext componentContext) {
        logger.debug("deactivating emulated GPIOService");
    }

    @Override
    public KuraGPIOPin getPinByName(String pinName) {
        return new EmulatedPin(pinName);
    }

    @Override
    public KuraGPIOPin getPinByName(String pinName, KuraGPIODirection direction, KuraGPIOMode mode,
            KuraGPIOTrigger trigger) {
        return new EmulatedPin(pinName, direction, mode, trigger);
    }

    @Override
    public KuraGPIOPin getPinByTerminal(int terminal) {
        return new EmulatedPin(terminal);
    }

    @Override
    public KuraGPIOPin getPinByTerminal(int terminal, KuraGPIODirection direction, KuraGPIOMode mode,
            KuraGPIOTrigger trigger) {
        return new EmulatedPin(terminal, direction, mode, trigger);
    }

    @Override
    public Map<Integer, String> getAvailablePins() {
        this.pins.clear();

        for (int i = 1; i < 11; i++) {
            this.pins.put(i, "Pin#" + i);
        }

        return this.pins;
    }

    @Override
    public List<KuraGPIOPin> getPins(Map<String, String> description) {
        if (description == null) {
            throw new IllegalArgumentException("description cannot be null");
        }
        return Arrays.asList(new EmulatedPin(description.get("name")));
    }

    @Override
    public List<KuraGPIOPin> getPins(Map<String, String> description, KuraGPIODirection direction, KuraGPIOMode mode,
            KuraGPIOTrigger trigger) {
        if (description == null) {
            throw new IllegalArgumentException("description cannot be null");
        }
        return Arrays.asList(new EmulatedPin(description.get("name"), direction, mode, trigger));
    }

    @Override
    public List<KuraGPIODescription> getAvailablePinDescriptions() {
        this.pinDescriptions.clear();
        for (int chip = 0; chip < 2; chip++) {
            for (int line = 0; line < 5; line++) {
                String name = "GPIOchip" + chip * 1000 + line;
                Map<String, String> properties = new HashMap<>();
                properties.put("controller", String.valueOf(chip));
                properties.put("line", String.valueOf(line));
                properties.put(KuraGPIODescription.DISPLAY_NAME_PROPERTY, name + ":" + chip + ":" + line);
                KuraGPIODescription desc = new KuraGPIODescription(properties);
                this.pinDescriptions.add(desc);
            }
        }
        return this.pinDescriptions;
    }

}

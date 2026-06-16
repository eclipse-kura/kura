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
import java.util.Collections;
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

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
@Component(
    name = "org.eclipse.kura.gpio.GPIOService",
    immediate = true,
    service = { org.eclipse.kura.gpio.GPIOService.class })
public class GpioServiceImpl implements GPIOService {

    private static final Logger logger = LoggerFactory.getLogger(GpioServiceImpl.class);

    private final HashMap<Integer, String> pins = new HashMap<>();
    private final List<KuraGPIODescription> pinDescriptions = new ArrayList<>();

    @Activate
    protected void activate(ComponentContext componentContext) {
        logger.debug("activating emulated GPIOService");
        for (int chip = 0; chip < 2; chip++) {
            for (int line = 0; line < 5; line++) {
                String name = "GPIOchip" + chip + "line" + line;
                Map<String, String> properties = new HashMap<>();
                properties.put("controller", String.valueOf(chip));
                properties.put("line", String.valueOf(line));
                properties.put(KuraGPIODescription.DISPLAY_NAME_PROPERTY, name + ":" + chip + ":" + line);
                KuraGPIODescription desc = new KuraGPIODescription(properties);
                this.pinDescriptions.add(desc);
                
                this.pins.put(chip * 1000 + line, name);
            }
        }
    }

    @Deactivate
    protected void deactivate(ComponentContext componentContext) {
        logger.debug("deactivating emulated GPIOService");
    }

    @Override
    public KuraGPIOPin getPinByName(String pinName) {
        if (pinName == null || pinName.isEmpty()) {
            throw new IllegalArgumentException("pinName cannot be null");
        }
        return new EmulatedPin(pinName);
    }

    @Override
    public KuraGPIOPin getPinByName(String pinName, KuraGPIODirection direction, KuraGPIOMode mode,
            KuraGPIOTrigger trigger) {
        if (pinName == null || pinName.isEmpty()) {
            throw new IllegalArgumentException("pinName cannot be null");
        }
        return new EmulatedPin(pinName, direction, mode, trigger);
    }

    @Override
    public KuraGPIOPin getPinByTerminal(int terminal) {
        if (terminal < 0) {
            throw new IllegalArgumentException("terminal cannot be negative");
        }
        return new EmulatedPin(terminal);
    }

    @Override
    public KuraGPIOPin getPinByTerminal(int terminal, KuraGPIODirection direction, KuraGPIOMode mode,
            KuraGPIOTrigger trigger) {
        if (terminal < 0) {
            throw new IllegalArgumentException("terminal cannot be negative");
        }
        return new EmulatedPin(terminal, direction, mode, trigger);
    }

    @Override
    public Map<Integer, String> getAvailablePins() {
        return Collections.unmodifiableMap(this.pins);
    }

    @Override
    public List<KuraGPIOPin> getPins(Map<String, String> description) {
        if (description == null || description.isEmpty() || !description.containsKey("name")) {
            throw new IllegalArgumentException("description cannot be null or empty and must contain 'name' key");
        }
        return Arrays.asList(new EmulatedPin(description.get("name")));
    }

    @Override
    public List<KuraGPIOPin> getPins(Map<String, String> description, KuraGPIODirection direction, KuraGPIOMode mode,
            KuraGPIOTrigger trigger) {
        if (description == null || description.isEmpty() || !description.containsKey("name")) {
            throw new IllegalArgumentException("description cannot be null or empty and must contain 'name' key");
        }
        return Arrays.asList(new EmulatedPin(description.get("name"), direction, mode, trigger));
    }

    @Override
    public List<KuraGPIODescription> getAvailablePinDescriptions() {
        return Collections.unmodifiableList(this.pinDescriptions);
    }

}

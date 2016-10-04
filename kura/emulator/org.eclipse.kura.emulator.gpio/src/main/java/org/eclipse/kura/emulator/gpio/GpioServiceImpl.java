/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.emulator.gpio;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.kura.gpio.GPIOService;
import org.eclipse.kura.gpio.KuraGPIODirection;
import org.eclipse.kura.gpio.KuraGPIOMode;
import org.eclipse.kura.gpio.KuraGPIOPin;
import org.eclipse.kura.gpio.KuraGPIOTrigger;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GpioServiceImpl implements GPIOService {

    private static final Logger s_logger = LoggerFactory.getLogger(GpioServiceImpl.class);

    private static final HashMap<Integer, String> pins = new HashMap<Integer, String>();

    protected void activate(ComponentContext componentContext) {
        s_logger.debug("activating emulated GPIOService");
    }

    protected void deactivate(ComponentContext componentContext) {
        s_logger.debug("deactivating emulated GPIOService");
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
        pins.clear();

        for (int i = 1; i < 11; i++) {
            pins.put(i, "Pin#" + String.valueOf(i));
        }

        return pins;
    }

}

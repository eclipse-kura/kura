/*******************************************************************************
 * Copyright (c) 2025 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.linux.gpio.libgpiod;

import java.util.Map;
import java.util.Optional;

import org.eclipse.kura.gpio.GPIOService;
import org.eclipse.kura.gpio.KuraGPIODirection;
import org.eclipse.kura.gpio.KuraGPIOMode;
import org.eclipse.kura.gpio.KuraGPIOPin;
import org.eclipse.kura.gpio.KuraGPIOTrigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GPIOServiceImpl implements GPIOService {

    private static final Logger logger = LoggerFactory.getLogger(GPIOServiceImpl.class);

    private Optional<GPIOService> gpioService = Optional.empty();

    protected void activate() {
        logger.info("Activating libgpiod GPIOService...");
        this.gpioService = LibGpiodGPIOServiceFactory.getInstance();
    }

    protected void deactivate() {
        logger.info("Deactivating libgpiod GPIOService...");
    }

    @Override
    public KuraGPIOPin getPinByName(String pinName) {
        if (!this.gpioService.isPresent()) {
            throw new IllegalStateException("No libgpiod GPIOService implementation available");
        }
        return this.gpioService.get().getPinByName(pinName);
    }

    @Override
    public KuraGPIOPin getPinByName(String pinName, KuraGPIODirection direction, KuraGPIOMode mode,
            KuraGPIOTrigger trigger) {
        if (!this.gpioService.isPresent()) {
            throw new IllegalStateException("No libgpiod GPIOService implementation available");
        }
        return this.gpioService.get().getPinByName(pinName, direction, mode, trigger);
    }

    @Override
    public KuraGPIOPin getPinByTerminal(int terminal) {
        if (!this.gpioService.isPresent()) {
            throw new IllegalStateException("No libgpiod GPIOService implementation available");
        }
        return this.gpioService.get().getPinByTerminal(terminal);
    }

    @Override
    public KuraGPIOPin getPinByTerminal(int terminal, KuraGPIODirection direction, KuraGPIOMode mode,
            KuraGPIOTrigger trigger) {
        if (!this.gpioService.isPresent()) {
            throw new IllegalStateException("No libgpiod GPIOService implementation available");
        }
        return this.gpioService.get().getPinByTerminal(terminal, direction, mode, trigger);
    }

    @Override
    public Map<Integer, String> getAvailablePins() {
        if (!this.gpioService.isPresent()) {
            throw new IllegalStateException("No libgpiod GPIOService implementation available");
        }
        return this.gpioService.get().getAvailablePins();
    }

}

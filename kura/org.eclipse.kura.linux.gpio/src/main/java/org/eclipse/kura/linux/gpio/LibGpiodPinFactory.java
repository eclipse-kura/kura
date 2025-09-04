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

// Add note on generated code
package org.eclipse.kura.linux.gpio;

import org.eclipse.kura.gpio.KuraGPIODirection;
import org.eclipse.kura.gpio.KuraGPIOMode;
import org.eclipse.kura.gpio.KuraGPIOPin;
import org.eclipse.kura.gpio.KuraGPIOTrigger;

/**
 * Factory class for creating LibGpiod-based GPIO pins
 */
public class LibGpiodPinFactory {

    private static final String DEFAULT_CHIP_PATH = "/dev/gpiochip0";

    private LibGpiodPinFactory() {
        // Empty private constructor
    }

    /**
     * Create a GPIO pin using the default chip path
     */
    public static KuraGPIOPin createPin(int offset, KuraGPIODirection direction, KuraGPIOMode mode,
            KuraGPIOTrigger trigger) {
        return createPin(DEFAULT_CHIP_PATH, offset, direction, mode, trigger, null);
    }

    /**
     * Create a GPIO pin with a specific name
     */
    public static KuraGPIOPin createPin(int offset, KuraGPIODirection direction, KuraGPIOMode mode,
            KuraGPIOTrigger trigger, String name) {
        return createPin(DEFAULT_CHIP_PATH, offset, direction, mode, trigger, name);
    }

    /**
     * Create a GPIO pin with a specific chip path
     */
    public static KuraGPIOPin createPin(String chipPath, int offset, KuraGPIODirection direction, KuraGPIOMode mode,
            KuraGPIOTrigger trigger) {
        return createPin(chipPath, offset, direction, mode, trigger, null);
    }

    /**
     * Create a GPIO pin with all parameters
     */
    public static KuraGPIOPin createPin(String chipPath, int offset, KuraGPIODirection direction, KuraGPIOMode mode,
            KuraGPIOTrigger trigger, String name) {
        return new LibGpiodPin(chipPath, offset, direction, mode, trigger, name);
    }

    /**
     * Create a simple input pin with pull-up
     */
    public static KuraGPIOPin createInputPin(int offset) {
        return createPin(offset, KuraGPIODirection.INPUT, KuraGPIOMode.INPUT_PULL_UP, KuraGPIOTrigger.NONE);
    }

    /**
     * Create a simple output pin
     */
    public static KuraGPIOPin createOutputPin(int offset) {
        return createPin(offset, KuraGPIODirection.OUTPUT, KuraGPIOMode.OUTPUT_PUSH_PULL, KuraGPIOTrigger.NONE);
    }

    /**
     * Create an input pin with edge detection
     */
    public static KuraGPIOPin createInputPinWithEdgeDetection(int offset, KuraGPIOTrigger trigger) {
        return createPin(offset, KuraGPIODirection.INPUT, KuraGPIOMode.INPUT_PULL_UP, trigger);
    }
}
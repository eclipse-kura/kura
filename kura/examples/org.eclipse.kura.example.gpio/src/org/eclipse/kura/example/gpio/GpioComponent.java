/*******************************************************************************
 * Copyright (c) 2011, 2017 Eurotech and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *     Red Hat Inc - Fix build warnings
 *******************************************************************************/
package org.eclipse.kura.example.gpio;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.gpio.GPIOService;
import org.eclipse.kura.gpio.KuraClosedDeviceException;
import org.eclipse.kura.gpio.KuraGPIODirection;
import org.eclipse.kura.gpio.KuraGPIOMode;
import org.eclipse.kura.gpio.KuraGPIOPin;
import org.eclipse.kura.gpio.KuraGPIOTrigger;
import org.eclipse.kura.gpio.KuraUnavailableDeviceException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GpioComponent implements ConfigurableComponent {

    private static final Logger logger = LoggerFactory.getLogger(GpioComponent.class);

    // Cloud Application identifier
    private static final String APP_ID = "GPIO_COMPONENT";

    // Property Names
    private static final String PROP_NAME_INPUT_READ_MODE = "gpio.input.read.mode";

    private static final String PROP_NAME_GPIO_PINS = "gpio.pins";
    private static final String PROP_NAME_GPIO_DIRECTIONS = "gpio.directions";
    private static final String PROP_NAME_GPIO_MODES = "gpio.modes";
    private static final String PROP_NAME_GPIO_TRIGGERS = "gpio.triggers";

    private static final String INPUT_READ_MODE_PIN_STATUS_LISTENER = "PIN_STATUS_LISTENER";
    private static final String INPUT_READ_MODE_POLLING = "POLLING";

    private GPIOService gpioService;

    private Map<String, Object> properties = new HashMap<String, Object>();

    private List<KuraGPIOPin> acquiredOutputPins = new ArrayList<KuraGPIOPin>();
    private List<KuraGPIOPin> acquiredInputPins = new ArrayList<KuraGPIOPin>();

    private ScheduledFuture<?> blinkTask = null;
    private ScheduledFuture<?> pollTask = null;

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    // ----------------------------------------------------------------
    //
    // Activation APIs
    //
    // ----------------------------------------------------------------

    public void setGPIOService(GPIOService gpioService) {
        this.gpioService = gpioService;
    }

    public void unsetGPIOService(GPIOService gpioService) {
        this.gpioService = null;
    }

    protected void activate(ComponentContext componentContext, Map<String, Object> properties) {
        logger.debug("Activating {}", APP_ID);

        doUpdate(properties);

        logger.info("Activating {}... Done.", APP_ID);

    }

    protected void deactivate(ComponentContext componentContext) {
        logger.debug("Deactivating {}", APP_ID);

        stopTasks();
        releasePins();

        this.executor.shutdownNow();
    }

    public void updated(Map<String, Object> properties) {
        logger.info("updated...");

        doUpdate(properties);
    }

    // ----------------------------------------------------------------
    //
    // Private Methods
    //
    // ----------------------------------------------------------------

    /**
     * Called after a new set of properties has been configured on the service
     */
    private void doUpdate(Map<String, Object> properties) {
        // for (String s : properties.keySet()) {
        // s_logger.info("Update - " + s + ": " + properties.get(s));
        // }

        this.properties.clear();
        this.properties.putAll(properties);

        stopTasks();
        releasePins();

        acquirePins();

        if (!acquiredOutputPins.isEmpty()) {
            submitBlinkTask(2000, acquiredOutputPins);
        }

        if (!acquiredInputPins.isEmpty()) {
            String inputReadMode = (String) properties.get(PROP_NAME_INPUT_READ_MODE);

            if (INPUT_READ_MODE_PIN_STATUS_LISTENER.equals(inputReadMode)) {
                attachPinListeners(acquiredInputPins);
            } else if (INPUT_READ_MODE_POLLING.equals(inputReadMode)) {
                submitPollTask(500, acquiredInputPins);
            }
        }

    }

    private void acquirePins() {
        Integer[] pins = (Integer[]) this.properties.get(PROP_NAME_GPIO_PINS);
        Integer[] directions = (Integer[]) this.properties.get(PROP_NAME_GPIO_DIRECTIONS);
        Integer[] modes = (Integer[]) this.properties.get(PROP_NAME_GPIO_MODES);
        Integer[] triggers = (Integer[]) this.properties.get(PROP_NAME_GPIO_TRIGGERS);
        logger.info("______________________________");
        logger.info("Available GPIOs on the system:");
        Map<Integer, String> gpios = this.gpioService.getAvailablePins();
        for (Entry<Integer, String> e : gpios.entrySet()) {
            logger.info("#{} - [{}]", e.getKey(), e.getValue());
        }
        logger.info("______________________________");
        for (int i = 0; i < pins.length; i++) {
            try {
                logger.info("Acquiring GPIO pin {} with params:", pins[i]);
                logger.info("   Direction....: {}", directions[i]);
                logger.info("   Mode.........: {}", modes[i]);
                logger.info("   Trigger......: {}", triggers[i]);
                KuraGPIOPin p = this.gpioService.getPinByTerminal(pins[i], getPinDirection(directions[i]),
                        getPinMode(modes[i]), getPinTrigger(triggers[i]));
                p.open();
                logger.info("GPIO pin {} acquired", pins[i]);
                if (p.getDirection() == KuraGPIODirection.OUTPUT) {
                    acquiredOutputPins.add(p);
                } else {
                    acquiredInputPins.add(p);
                }
            } catch (IOException e) {
                logger.error("I/O Error occurred!");
                e.printStackTrace();
            } catch (Exception e) {
                logger.error("got errror", e);
            }
        }
    }

    private void submitBlinkTask(long delayMs, final List<KuraGPIOPin> outputPins) {
        this.blinkTask = this.executor.scheduleWithFixedDelay(() -> {
            for (KuraGPIOPin outputPin : outputPins) {
                try {
                    boolean value = !outputPin.getValue();
                    logger.info("Setting GPIO pin {} to {}", outputPin, value);
                    outputPin.setValue(value);
                } catch (KuraUnavailableDeviceException | KuraClosedDeviceException | IOException e) {
                    logException(outputPin, e);
                }
            }
        }, 0, delayMs, TimeUnit.MILLISECONDS);
    }

    private void submitPollTask(long delayMs, final List<KuraGPIOPin> inputPins) {
        this.pollTask = this.executor.scheduleWithFixedDelay(() -> {
            for (KuraGPIOPin inputPin : inputPins) {
                try {
                    logger.info("input pin {} value {}", inputPin, inputPin.getValue());
                } catch (KuraUnavailableDeviceException | KuraClosedDeviceException | IOException e) {
                    logException(inputPin, e);
                }
            }
        }, 0, delayMs, TimeUnit.MILLISECONDS);
    }

    private void attachPinListeners(final List<KuraGPIOPin> inputPins) {
        for (final KuraGPIOPin pin : inputPins) {
            logger.info("Attaching Pin Listener to GPIO pin {}", pin);
            try {
                pin.addPinStatusListener(
                        value -> logger.info("Pin status for GPIO pin {} changed to {}", pin, value));
            } catch (Exception e) {
                logException(pin, e);
            }
        }
    }

    private void logException(KuraGPIOPin pin, Exception e) {
        if (e instanceof KuraUnavailableDeviceException) {
            logger.warn("GPIO pin {} is not available for export.", pin);
        } else if (e instanceof KuraClosedDeviceException) {
            logger.warn("GPIO pin {} has been closed.", pin);
        } else {
            logger.error("I/O Error occurred!", e);
        }
    }

    private void stopTasks() {
        if (this.blinkTask != null) {
            this.blinkTask.cancel(true);
        }
        if (this.pollTask != null) {
            this.pollTask.cancel(true);
        }
    }

    private void releasePins() {
        Stream.concat(acquiredInputPins.stream(), acquiredOutputPins.stream()).forEach(pin -> {
            try {
                logger.warn("Closing GPIO pin {}", pin);
                pin.close();
            } catch (IOException e) {
                logger.warn("Cannot close pin!");
            }
        });
        acquiredInputPins.clear();
        acquiredOutputPins.clear();
    }

    private KuraGPIODirection getPinDirection(int direction) {
        switch (direction) {
        case 0:
        case 2:
            return KuraGPIODirection.INPUT;
        case 1:
        case 3:
            return KuraGPIODirection.OUTPUT;
        }
        return KuraGPIODirection.OUTPUT;
    }

    private KuraGPIOMode getPinMode(int mode) {
        switch (mode) {
        case 2:
            return KuraGPIOMode.INPUT_PULL_DOWN;
        case 1:
            return KuraGPIOMode.INPUT_PULL_UP;
        case 8:
            return KuraGPIOMode.OUTPUT_OPEN_DRAIN;
        case 4:
            return KuraGPIOMode.OUTPUT_PUSH_PULL;
        }
        return KuraGPIOMode.OUTPUT_OPEN_DRAIN;
    }

    private KuraGPIOTrigger getPinTrigger(int trigger) {
        switch (trigger) {
        case 0:
            return KuraGPIOTrigger.NONE;
        case 2:
            return KuraGPIOTrigger.RAISING_EDGE;
        case 3:
            return KuraGPIOTrigger.BOTH_EDGES;
        case 1:
            return KuraGPIOTrigger.FALLING_EDGE;
        default:
            return KuraGPIOTrigger.NONE;
        }
    }
}

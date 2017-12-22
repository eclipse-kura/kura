/*******************************************************************************
 * Copyright (c) 2011, 2018 Eurotech and others
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

import static java.util.Objects.nonNull;

import java.io.IOException;
import java.util.ArrayList;
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
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GpioComponent implements ConfigurableComponent {

    /**
     * Inner class defined to track the CloudServices as they get added, modified or removed.
     * Specific methods can refresh the cloudService definition and setup again the Cloud Client.
     *
     */
    private final class GPIOServiceTrackerCustomizer implements ServiceTrackerCustomizer<GPIOService, GPIOService> {

        @Override
        public GPIOService addingService(final ServiceReference<GPIOService> reference) {
            GpioComponent.this.gpioService = GpioComponent.this.bundleContext.getService(reference);
            return GpioComponent.this.gpioService;
        }

        @Override
        public void modifiedService(final ServiceReference<GPIOService> reference, final GPIOService service) {
            GpioComponent.this.gpioService = GpioComponent.this.bundleContext.getService(reference);
        }

        @Override
        public void removedService(final ServiceReference<GPIOService> reference, final GPIOService service) {
            GpioComponent.this.gpioService = null;
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(GpioComponent.class);

    // Cloud Application identifier
    private static final String APP_ID = "GPIO_COMPONENT";

    private GpioComponentOptions gpioComponentOptions;
    private ServiceTrackerCustomizer<GPIOService, GPIOService> gpioServiceTrackerCustomizer;
    private ServiceTracker<GPIOService, GPIOService> gpioServiceTracker;

    private BundleContext bundleContext;
    private GPIOService gpioService;

    private List<KuraGPIOPin> acquiredOutputPins = new ArrayList<>();
    private List<KuraGPIOPin> acquiredInputPins = new ArrayList<>();

    private ScheduledFuture<?> blinkTask = null;
    private ScheduledFuture<?> pollTask = null;

    private boolean value;

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    // ----------------------------------------------------------------
    //
    // Activation APIs
    //
    // ----------------------------------------------------------------

    protected void activate(ComponentContext componentContext, Map<String, Object> properties) {
        logger.debug("Activating {}", APP_ID);

        this.bundleContext = componentContext.getBundleContext();

        this.gpioComponentOptions = new GpioComponentOptions(properties);

        this.gpioServiceTrackerCustomizer = new GPIOServiceTrackerCustomizer();
        initGPIOServiceTracking();

        doUpdate(properties);

        logger.info("Activating {}... Done.", APP_ID);
    }

    protected void deactivate(ComponentContext componentContext) {
        logger.debug("Deactivating {}", APP_ID);

        stopTasks();
        releasePins();

        if (nonNull(this.gpioServiceTracker)) {
            this.gpioServiceTracker.close();
        }

        this.executor.shutdownNow();
    }

    public void updated(Map<String, Object> properties) {
        logger.info("updated...");

        this.gpioComponentOptions = new GpioComponentOptions(properties);

        if (nonNull(this.gpioServiceTracker)) {
            this.gpioServiceTracker.close();
        }
        initGPIOServiceTracking();

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
        stopTasks();
        releasePins();

        this.value = false;
        acquirePins();

        if (!acquiredOutputPins.isEmpty()) {
            submitBlinkTask(2000, acquiredOutputPins);
        }

        if (!acquiredInputPins.isEmpty()) {
            String inputReadMode = this.gpioComponentOptions.getInputReadMode();

            if (GpioComponentOptions.INPUT_READ_MODE_PIN_STATUS_LISTENER.equals(inputReadMode)) {
                attachPinListeners(acquiredInputPins);
            } else if (GpioComponentOptions.INPUT_READ_MODE_POLLING.equals(inputReadMode)) {
                submitPollTask(500, acquiredInputPins);
            }
        }

    }

    private void acquirePins() {
        if (this.gpioService != null) {
            logger.info("______________________________");
            logger.info("Available GPIOs on the system:");
            Map<Integer, String> gpios = this.gpioService.getAvailablePins();
            for (Entry<Integer, String> e : gpios.entrySet()) {
                logger.info("#{} - [{}]", e.getKey(), e.getValue());
            }
            logger.info("______________________________");
            getPins();
        }
    }

    private void getPins() {
        String[] pins = this.gpioComponentOptions.getPins();
        Integer[] directions = this.gpioComponentOptions.getDirections();
        Integer[] modes = this.gpioComponentOptions.getModes();
        Integer[] triggers = this.gpioComponentOptions.getTriggers();
        for (int i = 0; i < pins.length; i++) {
            try {
                logger.info("Acquiring GPIO pin {} with params:", pins[i]);
                logger.info("   Direction....: {}", directions[i]);
                logger.info("   Mode.........: {}", modes[i]);
                logger.info("   Trigger......: {}", triggers[i]);
                KuraGPIOPin p = getPin(pins[i], getPinDirection(directions[i]), getPinMode(modes[i]),
                        getPinTrigger(triggers[i]));
                if (p != null) {
                    p.open();
                    logger.info("GPIO pin {} acquired", pins[i]);
                    if (p.getDirection() == KuraGPIODirection.OUTPUT) {
                        acquiredOutputPins.add(p);
                    } else {
                        acquiredInputPins.add(p);
                    }
                } else {
                    logger.info("GPIO pin {} not found", pins[i]);
                }
            } catch (IOException e) {
                logger.error("I/O Error occurred!", e);
            } catch (Exception e) {
                logger.error("got errror", e);
            }
        }
    }

    private KuraGPIOPin getPin(String resource, KuraGPIODirection pinDirection, KuraGPIOMode pinMode,
            KuraGPIOTrigger pinTrigger) {
        KuraGPIOPin pin = null;
        try {
            int terminal = Integer.parseInt(resource);
            if (terminal > 0 && terminal < 1255) {
                pin = this.gpioService.getPinByTerminal(Integer.parseInt(resource), pinDirection, pinMode, pinTrigger);
            }
        } catch (NumberFormatException e) {
            pin = this.gpioService.getPinByName(resource, pinDirection, pinMode, pinTrigger);
        }
        return pin;
    }

    private void submitBlinkTask(long delayMs, final List<KuraGPIOPin> outputPins) {
        this.blinkTask = this.executor.scheduleWithFixedDelay(() -> {
            for (KuraGPIOPin outputPin : outputPins) {
                try {
                    logger.info("Setting GPIO pin {} to {}", outputPin, this.value);
                    outputPin.setValue(this.value);
                } catch (KuraUnavailableDeviceException | KuraClosedDeviceException | IOException e) {
                    logException(outputPin, e);
                }
            }
            this.value = !this.value;
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
                pin.addPinStatusListener(value -> logger.info("Pin status for GPIO pin {} changed to {}", pin, value));
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
        default:
            return KuraGPIODirection.OUTPUT;
        }
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
        default:
            return KuraGPIOMode.OUTPUT_OPEN_DRAIN;
        }
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

    private void initGPIOServiceTracking() {
        String selectedGPIOServicePid = this.gpioComponentOptions.getGpioServicePid();
        String filterString = String.format("(&(%s=%s)(kura.service.pid=%s))", Constants.OBJECTCLASS,
                GPIOService.class.getName(), selectedGPIOServicePid);
        Filter filter = null;
        try {
            filter = this.bundleContext.createFilter(filterString);
        } catch (InvalidSyntaxException e) {
            logger.error("Filter setup exception ", e);
        }
        this.gpioServiceTracker = new ServiceTracker<>(this.bundleContext, filter, this.gpioServiceTrackerCustomizer);
        this.gpioServiceTracker.open();
    }
}

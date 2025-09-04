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

import java.io.IOException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.kura.gpio.KuraClosedDeviceException;
import org.eclipse.kura.gpio.KuraGPIODeviceException;
import org.eclipse.kura.gpio.KuraGPIODirection;
import org.eclipse.kura.gpio.KuraGPIOMode;
import org.eclipse.kura.gpio.KuraGPIOPin;
import org.eclipse.kura.gpio.KuraGPIOTrigger;
import org.eclipse.kura.gpio.KuraUnavailableDeviceException;
import org.eclipse.kura.gpio.PinStatusListener;

import com.sun.jna.Pointer;

/**
 * Implementation of KuraGPIOPin using libgpiod v2.2.x via JNA
 */
public class LibGpiodPin implements KuraGPIOPin {

    private final String chipPath;
    private final int offset;
    private final KuraGPIODirection direction;
    private final KuraGPIOMode mode;
    private final KuraGPIOTrigger trigger;
    private final String name;

    private Pointer chip;
    private Pointer lineRequest;
    private Pointer lineConfig;
    private Pointer lineSettings;
    private Pointer requestConfig;
    private Pointer edgeEventBuffer;

    private final AtomicBoolean isOpen = new AtomicBoolean(false);
    private final CopyOnWriteArrayList<PinStatusListener> listeners = new CopyOnWriteArrayList<>();
    private ExecutorService eventExecutor;
    private volatile boolean monitoring = false;

    public LibGpiodPin(String chipPath, int offset, KuraGPIODirection direction, KuraGPIOMode mode,
            KuraGPIOTrigger trigger, String name) {
        this.chipPath = chipPath;
        this.offset = offset;
        this.direction = direction;
        this.mode = mode;
        this.trigger = trigger;
        this.name = name != null ? name : "GPIO_" + offset;
    }

    @Override
    public void open() throws KuraGPIODeviceException, KuraUnavailableDeviceException, IOException {
        if (this.isOpen.get()) {
            return;
        }

        try {
            // Open the GPIO chip
            this.chip = LibGpiodNative.INSTANCE.gpiod_chip_open(this.chipPath);
            if (this.chip == null) {
                throw new KuraUnavailableDeviceException("Failed to open GPIO chip: " + this.chipPath);
            }

            // Create line settings
            this.lineSettings = LibGpiodNative.INSTANCE.gpiod_line_settings_new();
            if (this.lineSettings == null) {
                throw new KuraGPIODeviceException("Failed to create line settings");
            }

            // Configure direction
            int gpiodDirection = convertDirection(this.direction);
            if (LibGpiodNative.INSTANCE.gpiod_line_settings_set_direction(this.lineSettings, gpiodDirection) < 0) {
                throw new KuraGPIODeviceException("Failed to set line direction");
            }

            // Configure bias/mode
            int gpiodBias = convertModeToBias(this.mode);
            if (gpiodBias != -1) {
                if (LibGpiodNative.INSTANCE.gpiod_line_settings_set_bias(this.lineSettings, gpiodBias) < 0) {
                    throw new KuraGPIODeviceException("Failed to set line bias");
                }
            }

            // Configure drive for output modes
            if (this.direction == KuraGPIODirection.OUTPUT) {
                int gpiodDrive = convertModeToDrive(this.mode);
                if (gpiodDrive != -1) {
                    if (LibGpiodNative.INSTANCE.gpiod_line_settings_set_drive(this.lineSettings, gpiodDrive) < 0) {
                        throw new KuraGPIODeviceException("Failed to set line drive");
                    }
                }
            }

            // Configure edge detection for input with trigger
            if (this.direction == KuraGPIODirection.INPUT && this.trigger != KuraGPIOTrigger.NONE) {
                int gpiodEdge = convertTriggerToEdge(this.trigger);
                if (gpiodEdge != -1) {
                    if (LibGpiodNative.INSTANCE.gpiod_line_settings_set_edge_detection(this.lineSettings,
                            gpiodEdge) < 0) {
                        throw new KuraGPIODeviceException("Failed to set edge detection");
                    }
                }
            }

            // Create line config
            this.lineConfig = LibGpiodNative.INSTANCE.gpiod_line_config_new();
            if (this.lineConfig == null) {
                throw new KuraGPIODeviceException("Failed to create line config");
            }

            // Add line settings to config
            int[] offsets = { this.offset };
            if (LibGpiodNative.INSTANCE.gpiod_line_config_add_line_settings(this.lineConfig, offsets, 1,
                    this.lineSettings) < 0) {
                throw new KuraGPIODeviceException("Failed to add line settings to config");
            }

            // Create request config
            this.requestConfig = LibGpiodNative.INSTANCE.gpiod_request_config_new();
            if (this.requestConfig == null) {
                throw new KuraGPIODeviceException("Failed to create request config");
            }

            LibGpiodNative.INSTANCE.gpiod_request_config_set_consumer(this.requestConfig, "KuraGPIOPin");

            // Request the line
            this.lineRequest = LibGpiodNative.INSTANCE.gpiod_chip_request_lines(this.chip, this.requestConfig,
                    this.lineConfig);
            if (this.lineRequest == null) {
                throw new KuraGPIODeviceException("Failed to request GPIO line " + this.offset);
            }

            // Create edge event buffer if we need event monitoring
            if (this.direction == KuraGPIODirection.INPUT && this.trigger != KuraGPIOTrigger.NONE) {
                this.edgeEventBuffer = LibGpiodNative.INSTANCE.gpiod_edge_event_buffer_new(64);
                if (this.edgeEventBuffer == null) {
                    throw new KuraGPIODeviceException("Failed to create edge event buffer");
                }
            }

            this.isOpen.set(true);

        } catch (Exception e) {
            cleanup();
            if (e instanceof KuraGPIODeviceException || e instanceof KuraUnavailableDeviceException) {
                throw e;
            }
            throw new KuraGPIODeviceException(e, "Failed to open GPIO pin: " + e.getMessage());
        }
    }

    @Override
    public void close() throws IOException {
        if (!this.isOpen.get()) {
            return;
        }

        stopEventMonitoring();
        cleanup();
        this.isOpen.set(false);
    }

    private void cleanup() {
        if (this.edgeEventBuffer != null) {
            LibGpiodNative.INSTANCE.gpiod_edge_event_buffer_free(this.edgeEventBuffer);
            this.edgeEventBuffer = null;
        }

        if (this.lineRequest != null) {
            LibGpiodNative.INSTANCE.gpiod_line_request_release(this.lineRequest);
            this.lineRequest = null;
        }

        if (this.requestConfig != null) {
            LibGpiodNative.INSTANCE.gpiod_request_config_free(this.requestConfig);
            this.requestConfig = null;
        }

        if (this.lineConfig != null) {
            LibGpiodNative.INSTANCE.gpiod_line_config_free(this.lineConfig);
            this.lineConfig = null;
        }

        if (this.lineSettings != null) {
            LibGpiodNative.INSTANCE.gpiod_line_settings_free(this.lineSettings);
            this.lineSettings = null;
        }

        if (this.chip != null) {
            LibGpiodNative.INSTANCE.gpiod_chip_close(this.chip);
            this.chip = null;
        }
    }

    @Override
    public void setValue(boolean active) throws KuraUnavailableDeviceException, KuraClosedDeviceException, IOException {
        if (!this.isOpen.get()) {
            throw new KuraClosedDeviceException("GPIO pin is not open");
        }

        if (this.direction != KuraGPIODirection.OUTPUT) {
            throw new IOException("Cannot set value on input pin");
        }

        int value = active ? LibGpiodNative.GPIOD_LINE_VALUE_ACTIVE : LibGpiodNative.GPIOD_LINE_VALUE_INACTIVE;

        if (LibGpiodNative.INSTANCE.gpiod_line_request_set_value(this.lineRequest, this.offset, value) < 0) {
            throw new IOException("Failed to set GPIO pin value");
        }
    }

    @Override
    public boolean getValue() throws KuraUnavailableDeviceException, KuraClosedDeviceException, IOException {
        if (!this.isOpen.get()) {
            throw new KuraClosedDeviceException("GPIO pin is not open");
        }

        int value = LibGpiodNative.INSTANCE.gpiod_line_request_get_value(this.lineRequest, this.offset);

        if (value == LibGpiodNative.GPIOD_LINE_VALUE_ERROR) {
            throw new IOException("Failed to read GPIO pin value");
        }

        return value == LibGpiodNative.GPIOD_LINE_VALUE_ACTIVE;
    }

    @Override
    public void addPinStatusListener(PinStatusListener listener) throws KuraClosedDeviceException, IOException {
        if (!this.isOpen.get()) {
            throw new KuraClosedDeviceException("GPIO pin is not open");
        }

        if (this.direction != KuraGPIODirection.INPUT) {
            // Silently ignore as per API specification
            return;
        }

        this.listeners.add(listener);

        // Start event monitoring if not already started
        if (!this.monitoring && this.trigger != KuraGPIOTrigger.NONE) {
            startEventMonitoring();
        }
    }

    @Override
    public void removePinStatusListener(PinStatusListener listener) throws KuraClosedDeviceException, IOException {
        if (!this.isOpen.get()) {
            throw new KuraClosedDeviceException("GPIO pin is not open");
        }

        this.listeners.remove(listener);

        // Stop event monitoring if no more listeners
        if (this.listeners.isEmpty() && this.monitoring) {
            stopEventMonitoring();
        }
    }

    private void startEventMonitoring() {
        if (this.edgeEventBuffer == null || this.monitoring) {
            return;
        }

        this.monitoring = true;
        this.eventExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "GPIO-Event-Monitor-" + this.offset);
            t.setDaemon(true);
            return t;
        });

        this.eventExecutor.submit(() -> {
            while (this.monitoring && !Thread.currentThread().isInterrupted()) {
                try {
                    // Wait for edge events with 1 second timeout
                    int result = LibGpiodNative.INSTANCE.gpiod_line_request_wait_edge_events(this.lineRequest,
                            1_000_000_000L);

                    if (result > 0) {
                        // Read events
                        int numEvents = LibGpiodNative.INSTANCE.gpiod_line_request_read_edge_events(this.lineRequest,
                                this.edgeEventBuffer, 64);

                        if (numEvents > 0) {
                            // Process events
                            for (int i = 0; i < numEvents; i++) {
                                Pointer event = LibGpiodNative.INSTANCE
                                        .gpiod_edge_event_buffer_get_event(this.edgeEventBuffer, i);
                                if (event != null) {
                                    int eventType = LibGpiodNative.INSTANCE.gpiod_edge_event_get_event_type(event);
                                    boolean newValue = eventType == 1; // GPIOD_EDGE_EVENT_RISING_EDGE

                                    // Notify all listeners
                                    for (PinStatusListener listener : this.listeners) {
                                        try {
                                            listener.pinStatusChange(newValue);
                                        } catch (Exception e) {
                                            // Ignore listener exceptions
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    // Ignore and continue monitoring
                }
            }
        });
    }

    private void stopEventMonitoring() {
        this.monitoring = false;
        if (this.eventExecutor != null) {
            this.eventExecutor.shutdown();
            this.eventExecutor = null;
        }
    }

    @Override
    public KuraGPIODirection getDirection() {
        return this.direction;
    }

    @Override
    public KuraGPIOMode getMode() {
        return this.mode;
    }

    @Override
    public KuraGPIOTrigger getTrigger() {
        return this.trigger;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public int getIndex() {
        return this.offset;
    }

    @Override
    public boolean isOpen() {
        return this.isOpen.get();
    }

    // Helper methods to convert Kura enums to libgpiod constants

    private int convertDirection(KuraGPIODirection direction) {
        switch (direction) {
        case INPUT:
            return LibGpiodNative.GPIOD_LINE_DIRECTION_INPUT;
        case OUTPUT:
            return LibGpiodNative.GPIOD_LINE_DIRECTION_OUTPUT;
        default:
            return LibGpiodNative.GPIOD_LINE_DIRECTION_AS_IS;
        }
    }

    private int convertModeToBias(KuraGPIOMode mode) {
        switch (mode) {
        case INPUT_PULL_UP:
            return LibGpiodNative.GPIOD_LINE_BIAS_PULL_UP;
        case INPUT_PULL_DOWN:
            return LibGpiodNative.GPIOD_LINE_BIAS_PULL_DOWN;
        default:
            return -1; // No bias setting
        }
    }

    private int convertModeToDrive(KuraGPIOMode mode) {
        switch (mode) {
        case OUTPUT_OPEN_DRAIN:
            return LibGpiodNative.GPIOD_LINE_DRIVE_OPEN_DRAIN;
        case OUTPUT_PUSH_PULL:
            return LibGpiodNative.GPIOD_LINE_DRIVE_PUSH_PULL;
        default:
            return LibGpiodNative.GPIOD_LINE_DRIVE_PUSH_PULL; // Default
        }
    }

    private int convertTriggerToEdge(KuraGPIOTrigger trigger) {
        switch (trigger) {
        case RAISING_EDGE:
            return LibGpiodNative.GPIOD_LINE_EDGE_RISING;
        case FALLING_EDGE:
            return LibGpiodNative.GPIOD_LINE_EDGE_FALLING;
        case BOTH_EDGES:
            return LibGpiodNative.GPIOD_LINE_EDGE_BOTH;
        default:
            return -1; // No edge detection
        }
    }
}
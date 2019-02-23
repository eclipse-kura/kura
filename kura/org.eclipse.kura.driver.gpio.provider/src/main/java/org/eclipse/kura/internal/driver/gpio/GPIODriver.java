/**
 * Copyright (c) 2018 Eurotech and/or its affiliates and others
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

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;
import static org.eclipse.kura.channel.ChannelFlag.FAILURE;
import static org.eclipse.kura.channel.ChannelFlag.SUCCESS;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.eclipse.kura.channel.ChannelFlag;
import org.eclipse.kura.channel.ChannelRecord;
import org.eclipse.kura.channel.ChannelStatus;
import org.eclipse.kura.channel.listener.ChannelEvent;
import org.eclipse.kura.channel.listener.ChannelListener;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.driver.ChannelDescriptor;
import org.eclipse.kura.driver.Driver;
import org.eclipse.kura.driver.PreparedRead;
import org.eclipse.kura.gpio.GPIOService;
import org.eclipse.kura.gpio.KuraClosedDeviceException;
import org.eclipse.kura.gpio.KuraGPIODeviceException;
import org.eclipse.kura.gpio.KuraGPIODirection;
import org.eclipse.kura.gpio.KuraGPIOMode;
import org.eclipse.kura.gpio.KuraGPIOPin;
import org.eclipse.kura.gpio.KuraGPIOTrigger;
import org.eclipse.kura.gpio.KuraUnavailableDeviceException;
import org.eclipse.kura.gpio.PinStatusListener;
import org.eclipse.kura.type.DataType;
import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.type.TypedValues;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Class {@link GPIODriver} is a GPIO Driver implementation for
 * Kura Asset-Driver Topology.
 * <br/>
 * <br/>
 * This GPIO Driver can be used in cooperation with Kura Asset Model and in
 * isolation as well. In case of isolation, the properties needs to be provided
 * externally.
 * <br/>
 * <br/>
 * The required properties are enlisted in {@link GPIOChannelDescriptor}.
 *
 * @see Driver
 * @see GPIOChannelDescriptor
 *
 */
public final class GPIODriver implements Driver, ConfigurableComponent {

    private static final Logger logger = LoggerFactory.getLogger(GPIODriver.class);
    private static final String WRITE_FAILED_MESSAGE = "GPIO write operation failed";
    private static final String READ_FAILED_MESSAGE = "GPIO read operation failed";

    private Set<String> gpioNames;
    private Set<GPIOListener> gpioListeners;
    private final List<GPIOService> gpioServices = new ArrayList<>();

    protected synchronized void bindGPIOService(final GPIOService gpioService) {
        if (!this.gpioServices.contains(gpioService)) {
            this.gpioServices.add(gpioService);
        }
    }

    protected synchronized void unbindGPIOService(final GPIOService gpioService) {
        this.gpioServices.remove(gpioService);
    }

    protected synchronized void activate(final Map<String, Object> properties) {
        logger.debug("Activating GPIO Driver...");
        this.gpioNames = new HashSet<>();
        this.gpioListeners = new HashSet<>();
        logger.debug("Activating GPIO Driver... Done");
    }

    protected synchronized void deactivate() {
        logger.debug("Deactivating GPIO Driver...");
        doDeactivate();
        logger.debug("Deactivating GPIO Driver... Done");
    }

    protected synchronized void update(final Map<String, Object> properties) {
        logger.debug("Updating GPIO Driver...");
        logger.debug("Updating GPIO Driver... Done");
    }

    private void doDeactivate() {
        for (GPIOListener gpioListener : this.gpioListeners) {
            KuraGPIOPin pin = gpioListener.getPin();
            try {
                pin.removePinStatusListener(gpioListener);
            } catch (KuraClosedDeviceException | IOException e) {
                logger.error("Unable to unset listener for pin {}", pin.getName(), e);
            }
        }
        this.gpioListeners.clear();

        for (String name : this.gpioNames) {
            for (GPIOService service : this.gpioServices) {
                KuraGPIOPin pin = service.getPinByName(name);
                if (pin != null && pin.isOpen()) {
                    try {
                        pin.close();
                    } catch (IOException e) {
                        logger.error("Unable to close GPIO resource {}", pin.getName(), e);
                    }
                }
            }
        }
        this.gpioNames.clear();
    }

    @Override
    public void connect() throws ConnectionException {
        // Not implemented
    }

    @Override
    public synchronized void disconnect() throws ConnectionException {
        doDeactivate();
    }

    @Override
    public ChannelDescriptor getChannelDescriptor() {
        return new GPIOChannelDescriptor(this.gpioServices);
    }

    @Override
    public synchronized void read(final List<ChannelRecord> records) throws ConnectionException {
        for (final ChannelRecord record : records) {
            Optional<GPIORequestInfo> requestInfo = GPIORequestInfo.extract(record);
            if (requestInfo.isPresent()) {
                this.gpioNames.add(requestInfo.get().resourceName);
                runReadRequest(requestInfo.get());
            }
        }
    }

    @Override
    public synchronized void write(final List<ChannelRecord> records) throws ConnectionException {
        for (final ChannelRecord record : records) {
            GPIORequestInfo.extract(record).ifPresent(this::runWriteRequest);
        }
    }

    @Override
    public synchronized PreparedRead prepareRead(List<ChannelRecord> channelRecords) {
        requireNonNull(channelRecords, "Channel Record list cannot be null");

        try (GPIOPreparedRead preparedRead = new GPIOPreparedRead()) {
            preparedRead.channelRecords = channelRecords;

            for (ChannelRecord record : channelRecords) {
                Optional<GPIORequestInfo> requestInfo = GPIORequestInfo.extract(record);
                if (requestInfo.isPresent()) {
                    preparedRead.requestInfos.add(requestInfo.get());
                    this.gpioNames.add(requestInfo.get().resourceName);
                }
            }
            return preparedRead;
        }
    }

    @Override
    public synchronized void registerChannelListener(final Map<String, Object> channelConfig,
            final ChannelListener listener) throws ConnectionException {
        String name = GPIOChannelDescriptor.getResourceName(channelConfig);
        KuraGPIODirection direction = GPIOChannelDescriptor.getResourceDirection(channelConfig);
        if (!GPIOChannelDescriptor.DEFAULT_RESOURCE_NAME.equals(name) && direction != null) {
            this.gpioNames.add(name);
            KuraGPIOPin pin;
            if (KuraGPIODirection.INPUT.equals(direction)) {
                pin = getPin(name, direction, KuraGPIOMode.INPUT_PULL_UP,
                        GPIOChannelDescriptor.getResourceTrigger(channelConfig));
            } else {
                pin = getPin(name, direction, KuraGPIOMode.OUTPUT_OPEN_DRAIN,
                        GPIOChannelDescriptor.getResourceTrigger(channelConfig));
            }
            if (pin != null) {
                GPIOListener gpioListener = new GPIOListener(pin, (String) channelConfig.get("+name"),
                        DataType.getDataType((String) channelConfig.get("+value.type")), listener);
                this.gpioListeners.add(gpioListener);
                try {
                    pin.addPinStatusListener(gpioListener);
                } catch (KuraClosedDeviceException | IOException e) {
                    logger.error("Unable to set listener for pin {}", name, e);
                }
            }
        }
    }

    @Override
    public synchronized void unregisterChannelListener(final ChannelListener listener) throws ConnectionException {
        Iterator<GPIOListener> iterator = this.gpioListeners.iterator();
        while (iterator.hasNext()) {
            GPIOListener gpioListener = iterator.next();
            if (listener == gpioListener.getListener()) {
                try {
                    gpioListener.getPin().removePinStatusListener(gpioListener);
                    iterator.remove();
                } catch (KuraClosedDeviceException | IOException e) {
                    logger.error("Unable to unset listener for pin {}", gpioListener.getPin().getName(), e);
                }
            }
        }
    }

    private synchronized void runWriteRequest(GPIORequestInfo requestInfo) {
        ChannelRecord record = requestInfo.channelRecord;
        if (!GPIOChannelDescriptor.DEFAULT_RESOURCE_NAME.equals(requestInfo.resourceName)
                && requestInfo.resourceDirection != null) {
            try {
                final TypedValue<Boolean> value = getBooleanValue(record.getValue());
                this.gpioNames.add(requestInfo.resourceName);
                KuraGPIOPin pin = getPin(requestInfo.resourceName, requestInfo.resourceDirection,
                        requestInfo.resourceMode, requestInfo.resourceTrigger);
                if (pin != null) {
                    pin.setValue((boolean) value.getValue());
                    record.setChannelStatus(new ChannelStatus(SUCCESS));
                    record.setTimestamp(System.currentTimeMillis());
                }
            } catch (IOException | KuraUnavailableDeviceException | KuraClosedDeviceException e) {
                setFailureRecord(record, WRITE_FAILED_MESSAGE);
            }
        } else {
            setFailureRecord(record, WRITE_FAILED_MESSAGE);
        }
    }

    private void setFailureRecord(ChannelRecord record, String errorMessage) {
        record.setChannelStatus(new ChannelStatus(ChannelFlag.FAILURE, errorMessage, null));
        record.setTimestamp(System.currentTimeMillis());
        logger.warn(errorMessage);
    }

    private KuraGPIOPin getPin(String resourceName, KuraGPIODirection resourceDirection, KuraGPIOMode resourceMode,
            KuraGPIOTrigger resourceTrigger) {
        KuraGPIOPin pin = null;
        for (GPIOService service : this.gpioServices) {
            pin = service.getPinByName(resourceName, resourceDirection, resourceMode, resourceTrigger);
            if (pin != null) {
                if (!pin.isOpen()) {
                    try {
                        pin.open();
                    } catch (KuraGPIODeviceException | KuraUnavailableDeviceException | IOException e) {
                        logger.error("Unable to open GPIO resource {}", pin.getName(), e);
                    }
                }
                break;
            }
        }
        return pin;
    }

    private Optional<TypedValue<?>> getTypedValue(final DataType expectedValueType, final Boolean containedValue) {
        try {
            switch (expectedValueType) {
            case LONG:
                return Optional.of(TypedValues.newLongValue(containedValue ? (long) 1 : (long) 0));
            case FLOAT:
                return Optional.of(TypedValues.newFloatValue(containedValue ? (float) 1 : (float) 0));
            case DOUBLE:
                return Optional.of(TypedValues.newDoubleValue(containedValue ? (double) 1 : (double) 0));
            case INTEGER:
                return Optional.of(TypedValues.newIntegerValue(containedValue ? (int) 1 : (int) 0));
            case BOOLEAN:
                return Optional.of(TypedValues.newBooleanValue(containedValue));
            case STRING:
                return Optional.of(TypedValues.newStringValue(containedValue ? "true" : "false"));
            case BYTE_ARRAY:
                byte[] falseArray = { 0x00 };
                byte[] trueArray = { 0x01 };
                return Optional.of(TypedValues.newByteArrayValue(containedValue ? trueArray : falseArray));
            default:
                return Optional.empty();
            }
        } catch (final Exception ex) {
            logger.error("Error while converting the retrieved value to the defined typed", ex);
            return Optional.empty();
        }
    }

    private TypedValue<Boolean> getBooleanValue(TypedValue<?> value) {
        try {
            switch (value.getType()) {
            case LONG:
                return TypedValues.newBooleanValue(((long) value.getValue()) > 0);
            case FLOAT:
                return TypedValues.newBooleanValue(((float) value.getValue()) > 0);
            case DOUBLE:
                return TypedValues.newBooleanValue(((double) value.getValue()) > 0);
            case INTEGER:
                return TypedValues.newBooleanValue(((int) value.getValue()) > 0);
            case BOOLEAN:
                return TypedValues.newBooleanValue((Boolean) value.getValue());
            case STRING:
                String valueString = (String) value.getValue();
                if (valueString != null && !valueString.isEmpty()) {
                    return TypedValues
                            .newBooleanValue(((String) value.getValue()).equalsIgnoreCase("true") ? true : false);
                } else {
                    return TypedValues.newBooleanValue(false);
                }
            case BYTE_ARRAY:
                byte[] valueBytes = (byte[]) value.getValue();
                byte[] zeros = new byte[valueBytes.length];
                Arrays.fill(zeros, (byte) 0x0);
                return TypedValues.newBooleanValue(!Arrays.equals(valueBytes, zeros));
            default:
                return TypedValues.newBooleanValue(false);
            }
        } catch (final Exception ex) {
            logger.error("Error while converting the retrieved value to the defined typed", ex);
            return TypedValues.newBooleanValue(false);
        }
    }

    private synchronized void runReadRequest(GPIORequestInfo requestInfo) {
        ChannelRecord record = requestInfo.channelRecord;
        if (!GPIOChannelDescriptor.DEFAULT_RESOURCE_NAME.equals(requestInfo.resourceName)
                && requestInfo.resourceDirection != null) {
            try {
                KuraGPIOPin pin = getPin(requestInfo.resourceName, requestInfo.resourceDirection,
                        requestInfo.resourceMode, requestInfo.resourceTrigger);
                if (pin != null) {
                    Boolean value = pin.getValue();
                    final Optional<TypedValue<?>> typedValue = getTypedValue(requestInfo.dataType, value);
                    if (!typedValue.isPresent()) {
                        record.setChannelStatus(new ChannelStatus(FAILURE,
                                "Error while converting the retrieved value to the defined typed", null));
                        record.setTimestamp(System.currentTimeMillis());
                        return;
                    }

                    record.setValue(typedValue.get());
                    record.setChannelStatus(new ChannelStatus(SUCCESS));
                    record.setTimestamp(System.currentTimeMillis());
                }
            } catch (IOException | KuraUnavailableDeviceException | KuraClosedDeviceException e) {
                setFailureRecord(record, READ_FAILED_MESSAGE);
            }
        } else {
            setFailureRecord(record, READ_FAILED_MESSAGE);
        }
    }

    private static class GPIORequestInfo {

        private final DataType dataType;
        private String resourceName;
        private KuraGPIODirection resourceDirection;
        private KuraGPIOMode resourceMode;
        private KuraGPIOTrigger resourceTrigger;
        private final ChannelRecord channelRecord;

        public GPIORequestInfo(ChannelRecord channelRecord, DataType dataType) {
            this.dataType = dataType;
            this.channelRecord = channelRecord;
        }

        private static void fail(final ChannelRecord record, final String message) {
            record.setChannelStatus(new ChannelStatus(FAILURE, message, null));
            record.setTimestamp(System.currentTimeMillis());
        }

        public static Optional<GPIORequestInfo> extract(final ChannelRecord record) {
            final Map<String, Object> channelConfig = record.getChannelConfig();
            final DataType dataType = record.getValueType();

            if (isNull(dataType)) {
                fail(record, "Error while retrieving value type");
                return Optional.empty();
            }

            GPIORequestInfo request = new GPIORequestInfo(record, dataType);
            request.resourceName = GPIOChannelDescriptor.getResourceName(channelConfig);
            request.resourceDirection = GPIOChannelDescriptor.getResourceDirection(channelConfig);
            request.resourceTrigger = GPIOChannelDescriptor.getResourceTrigger(channelConfig);
            if (KuraGPIODirection.INPUT.equals(request.resourceDirection)) {
                request.resourceMode = KuraGPIOMode.INPUT_PULL_UP;
            } else {
                request.resourceMode = KuraGPIOMode.OUTPUT_OPEN_DRAIN;
            }

            return Optional.of(request);
        }
    }

    private class GPIOPreparedRead implements PreparedRead {

        private final List<GPIORequestInfo> requestInfos = new ArrayList<>();
        private volatile List<ChannelRecord> channelRecords;

        @Override
        public synchronized List<ChannelRecord> execute() throws ConnectionException {
            for (GPIORequestInfo requestInfo : this.requestInfos) {
                runReadRequest(requestInfo);
            }

            return Collections.unmodifiableList(this.channelRecords);
        }

        @Override
        public List<ChannelRecord> getChannelRecords() {
            return Collections.unmodifiableList(this.channelRecords);
        }

        @Override
        public void close() {
            // Method not supported
        }
    }

    private class GPIOListener implements PinStatusListener {

        private final ChannelListener listener;
        private final String channelName;
        private final KuraGPIOPin pin;
        private final DataType dataType;

        public GPIOListener(KuraGPIOPin pin, String channelName, DataType dataType, ChannelListener listener) {
            this.pin = pin;
            this.channelName = channelName;
            this.dataType = dataType;
            this.listener = listener;
        }

        public KuraGPIOPin getPin() {
            return this.pin;
        }

        public ChannelListener getListener() {
            return this.listener;
        }

        @Override
        public void pinStatusChange(boolean value) {
            ChannelRecord record = ChannelRecord.createReadRecord(this.channelName, this.dataType);
            final Optional<TypedValue<?>> typedValue = getTypedValue(this.dataType, value);
            if (!typedValue.isPresent()) {
                record.setChannelStatus(new ChannelStatus(FAILURE,
                        "Error while converting the retrieved value to the defined typed", null));
                record.setTimestamp(System.currentTimeMillis());
                return;
            }

            record.setValue(typedValue.get());
            record.setChannelStatus(new ChannelStatus(SUCCESS));
            record.setTimestamp(System.currentTimeMillis());
            this.listener.onChannelEvent(new ChannelEvent(record));
        }

    }
}

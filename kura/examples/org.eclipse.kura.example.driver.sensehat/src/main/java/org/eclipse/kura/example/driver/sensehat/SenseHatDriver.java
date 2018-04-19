/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/

package org.eclipse.kura.example.driver.sensehat;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.kura.channel.ChannelFlag;
import org.eclipse.kura.channel.ChannelRecord;
import org.eclipse.kura.channel.ChannelStatus;
import org.eclipse.kura.channel.listener.ChannelEvent;
import org.eclipse.kura.channel.listener.ChannelListener;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.driver.ChannelDescriptor;
import org.eclipse.kura.driver.Driver;
import org.eclipse.kura.driver.PreparedRead;
import org.eclipse.kura.example.driver.sensehat.SenseHatInterface.JoystickEventListener;
import org.eclipse.kura.example.driver.sensehat.SenseHatInterface.SenseHatReadRequest;
import org.eclipse.kura.raspberrypi.sensehat.SenseHat;
import org.eclipse.kura.type.DataType;
import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.type.TypedValues;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SenseHatDriver implements Driver, ConfigurableComponent, JoystickEventListener {

    private static final ChannelStatus CHANNEL_STATUS_OK = new ChannelStatus(ChannelFlag.SUCCESS);
    private static final Logger logger = LoggerFactory.getLogger(SenseHatDriver.class);

    private SenseHat senseHat;

    private static SenseHatInterface senseHatInterface;
    private static AtomicInteger senseHatInterfaceRefCnt = new AtomicInteger(0);

    private Map<Resource, Set<ChannelListenerRegistration>> channelListeners = new HashMap<>();

    public void bindSenseHat(final SenseHat senseHat) {
        this.senseHat = senseHat;
    }

    public void unbindSenseHat(final SenseHat senseHat) {
        if (this.senseHat != null) {
            this.senseHat = null;
        }
    }

    public static synchronized void getSensehatInterface(final SenseHat senseHat) {
        if (senseHatInterfaceRefCnt.getAndIncrement() == 0) {
            logger.info("Opening Sense Hat...");
            senseHatInterface = new SenseHatInterface(senseHat);
            logger.info("Opening Sense Hat...done");
        }
    }

    public static synchronized void ungetSensehatInteface() throws IOException {
        if (senseHatInterfaceRefCnt.decrementAndGet() == 0) {
            logger.info("Closing Sense Hat...");
            senseHatInterface.close();
            logger.info("Closing Sense Hat...done");
            senseHatInterface = null;
        }
    }

    public void activate(final Map<String, Object> properties) {
        logger.info("Activating SenseHat Driver...");
        getSensehatInterface(senseHat);
        senseHatInterface.addJoystickEventListener(this);
        logger.info("Activating SenseHat Driver... Done");
    }

    public void updated(final Map<String, Object> properties) {
        // no need
    }

    public void deactivate() {
        logger.info("Deactivating SenseHat Driver...");
        try {
            senseHatInterface.removeJoystickEventListener(this);
            ungetSensehatInteface();
        } catch (Exception e) {
            logger.warn("Failed to close Sense Hat", e);
        }
        logger.info("Deactivating SenseHat Driver... Done");
    }

    @Override
    public void connect() throws ConnectionException {
        // no need
    }

    @Override
    public void disconnect() throws ConnectionException {
        // no need
    }

    @Override
    public void read(final List<ChannelRecord> records) throws ConnectionException {
        senseHatInterface.runReadRequest(toReadRequest(records));
    }

    @Override
    public void registerChannelListener(final Map<String, Object> channelConfig, final ChannelListener listener)
            throws ConnectionException {

        final Resource resource = Resource.from(channelConfig);

        if (!resource.isJoystickEvent()) {
            throw new UnsupportedOperationException("Cannot attach listeners on resource: " + resource);
        }

        final String channelName = getChannelName(channelConfig);
        final DataType dataType = getChannelValueType(channelConfig);

        if (dataType != DataType.LONG) {
            throw new IllegalArgumentException("Value type for joystick event channels must be: " + DataType.LONG);
        }

        final Set<ChannelListenerRegistration> listeners = getListenersForResource(resource);
        listeners.add(new ChannelListenerRegistration(listener, channelName));
    }

    @Override
    public void unregisterChannelListener(final ChannelListener listener) throws ConnectionException {
        for (Entry<Resource, Set<ChannelListenerRegistration>> e : this.channelListeners.entrySet()) {
            e.getValue().removeIf(registration -> listener == registration.listener);
        }
    }

    @Override
    public void write(final List<ChannelRecord> records) throws ConnectionException {
        final long timestamp = System.currentTimeMillis();
        try {
            senseHatInterface.runFramebufferRequest(toFramebufferRequest(records));
            records.stream().filter(record -> record.getChannelStatus() == null).forEach(record -> {
                record.setChannelStatus(new ChannelStatus(ChannelFlag.SUCCESS));
                record.setTimestamp(timestamp);
            });
        } catch (Exception e) {
            records.stream().filter(record -> record.getChannelStatus() == null).forEach(record -> {
                record.setChannelStatus(new ChannelStatus(ChannelFlag.FAILURE, e.getMessage(), e));
                record.setTimestamp(timestamp);
            });
        }
    }

    @Override
    public PreparedRead prepareRead(List<ChannelRecord> channelRecords) {
        requireNonNull(channelRecords);

        return new SenseHatPreparedRead(toReadRequest(channelRecords));
    }

    private class SenseHatPreparedRead implements PreparedRead {

        private final SenseHatReadRequest readRequest;

        private SenseHatPreparedRead(final SenseHatReadRequest readRequest) {
            this.readRequest = readRequest;
        }

        @Override
        public List<ChannelRecord> execute() throws ConnectionException {

            senseHatInterface.runReadRequest(readRequest);
            return Collections.unmodifiableList(readRequest.getRecords());
        }

        @Override
        public List<ChannelRecord> getChannelRecords() {
            return Collections.unmodifiableList(readRequest.getRecords());
        }

        @Override
        public void close() {
            // TODO Auto-generated method stub
        }
    }

    @Override
    public ChannelDescriptor getChannelDescriptor() {
        return SenseHatChannelDescriptor.instance();
    }

    private static void assertChannelType(final ChannelRecord record, final DataType expectedType) {
        if (record.getValueType() != expectedType) {
            throw new IllegalArgumentException("Channel value type must be: " + expectedType);
        }
    }

    private static float toFloat(final ChannelRecord record) {
        return ((Number) record.getValue().getValue()).floatValue();
    }

    private static int toInt(final ChannelRecord record) {
        return ((Number) record.getValue().getValue()).intValue();
    }

    private static void updateRequest(final ChannelRecord record, final FramebufferRequest request) {
        try {
            final Resource resource = Resource.from(record.getChannelConfig());
            if (resource == Resource.LED_MATRIX_FB_RGB565) {
                assertChannelType(record, DataType.BYTE_ARRAY);
                request.writeRGB565Pixels((byte[]) record.getValue().getValue());
            } else if (resource == Resource.LED_MATRIX_FB_MONOCHROME) {
                assertChannelType(record, DataType.BYTE_ARRAY);
                request.writeMonochromePixels((byte[]) record.getValue().getValue());
            } else if (resource == Resource.LED_MATRIX_CHARS) {
                assertChannelType(record, DataType.STRING);
                request.writeMessage((String) record.getValue().getValue());
            } else if (resource == Resource.LED_MATRIX_CLEAR) {
                request.clear();
            } else if (resource == Resource.LED_MATRIX_ROTATION) {
                request.transform(toInt(record));
            } else if (resource == Resource.LED_MATRIX_FRONT_COLOR_R) {
                request.setFrontColorRed(toFloat(record));
            } else if (resource == Resource.LED_MATRIX_FRONT_COLOR_G) {
                request.setFrontColorGreen(toFloat(record));
            } else if (resource == Resource.LED_MATRIX_FRONT_COLOR_B) {
                request.setFrontColorBlue(toFloat(record));
            } else if (resource == Resource.LED_MATRIX_BACK_COLOR_R) {
                request.setBackColorRed(toFloat(record));
            } else if (resource == Resource.LED_MATRIX_BACK_COLOR_G) {
                request.setBackColorGreen(toFloat(record));
            } else if (resource == Resource.LED_MATRIX_BACK_COLOR_B) {
                request.setBackColorBlue(toFloat(record));
            } else {
                throw new IllegalArgumentException("Resource is not framebuffer related" + resource);
            }
        } catch (Exception e) {
            record.setChannelStatus(new ChannelStatus(ChannelFlag.FAILURE, e.getMessage(), e));
            record.setTimestamp(System.currentTimeMillis());
        }
    }

    public FramebufferRequest toFramebufferRequest(final List<ChannelRecord> records) {
        final FramebufferRequest request = new FramebufferRequest();
        records.forEach(record -> updateRequest(record, request));
        return request;
    }

    private void updateRequest(final ChannelRecord record, final SenseHatReadRequest request) {
        try {
            final Resource resource = Resource.from(record.getChannelConfig());
            if (resource == Resource.ACCELERATION_X) {
                request.addTask(new SensorReadTask(record, SenseHatInterface::getAccelerometerX));
            } else if (resource == Resource.ACCELERATION_Y) {
                request.addTask(new SensorReadTask(record, SenseHatInterface::getAccelerometerY));
            } else if (resource == Resource.ACCELERATION_Z) {
                request.addTask(new SensorReadTask(record, SenseHatInterface::getAccelerometerZ));
            } else if (resource == Resource.GYROSCOPE_X) {
                request.addTask(new SensorReadTask(record, SenseHatInterface::getGyroscopeX));
            } else if (resource == Resource.GYROSCOPE_Y) {
                request.addTask(new SensorReadTask(record, SenseHatInterface::getGyroscopeY));
            } else if (resource == Resource.GYROSCOPE_Z) {
                request.addTask(new SensorReadTask(record, SenseHatInterface::getGyroscopeZ));
            } else if (resource == Resource.MAGNETOMETER_X) {
                request.addTask(new SensorReadTask(record, SenseHatInterface::getMagnetometerX));
            } else if (resource == Resource.MAGNETOMETER_Y) {
                request.addTask(new SensorReadTask(record, SenseHatInterface::getMagnetometerY));
            } else if (resource == Resource.MAGNETOMETER_Z) {
                request.addTask(new SensorReadTask(record, SenseHatInterface::getMagnetometerZ));
            } else if (resource == Resource.HUMIDITY) {
                request.addTask(new SensorReadTask(record, SenseHatInterface::getHumidity));
            } else if (resource == Resource.PRESSURE) {
                request.addTask(new SensorReadTask(record, SenseHatInterface::getPressure));
            } else if (resource == Resource.TEMPERATURE_FROM_HUMIDITY) {
                request.addTask(new SensorReadTask(record, SenseHatInterface::getTemperatureFromHumidity));
            } else if (resource == Resource.TEMPERATURE_FROM_PRESSURE) {
                request.addTask(new SensorReadTask(record, SenseHatInterface::getTemperatureFromPressure));
            } else if (resource.isJoystickEvent()) {
                assertChannelType(record, DataType.LONG);
                request.addTask(new ReadTask(record, sensehat -> {
                    final Long timestamp = senseHatInterface.getLastJoystickEventTimestamp(resource);
                    return TypedValues.newLongValue(timestamp == null ? 0 : timestamp);
                }));
            } else {
                throw new IllegalArgumentException("Resource is not readable" + resource);
            }
            resource.getAssociatedSensor().ifPresent(request::addInvolvedSensor);
        } catch (Exception e) {
            record.setChannelStatus(new ChannelStatus(ChannelFlag.FAILURE, e.getMessage(), e));
            record.setTimestamp(System.currentTimeMillis());
        }
    }

    public SenseHatReadRequest toReadRequest(final List<ChannelRecord> records) {
        final SenseHatReadRequest result = new SenseHatReadRequest(records);

        for (final ChannelRecord record : records) {
            updateRequest(record, result);
        }

        return result;
    }

    private Set<ChannelListenerRegistration> getListenersForResource(Resource resource) {
        return this.channelListeners.computeIfAbsent(resource, res -> new CopyOnWriteArraySet<>());
    }

    private String getChannelName(Map<String, Object> properties) {
        return (String) properties.get("+name");
    }

    private DataType getChannelValueType(Map<String, Object> properties) {
        return DataType.valueOf((String) properties.get("+value.type"));
    }

    @Override
    public void onJoystickEvent(Resource event, long timestamp) {
        final TypedValue<?> value = TypedValues.newLongValue(timestamp);
        final Set<ChannelListenerRegistration> listeners = channelListeners.get(event);
        if (listeners == null) {
            return;
        }
        listeners.forEach(reg -> {
            final ChannelRecord record = ChannelRecord.createReadRecord(reg.channelName, DataType.LONG);
            record.setValue(value);
            record.setChannelStatus(CHANNEL_STATUS_OK);
            record.setTimestamp(timestamp);
            reg.listener.onChannelEvent(new ChannelEvent(record));
        });
    }

    private static class ChannelListenerRegistration {

        private final ChannelListener listener;
        private final String channelName;

        public ChannelListenerRegistration(final ChannelListener listener, final String channelName) {
            this.listener = listener;
            this.channelName = channelName;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((channelName == null) ? 0 : channelName.hashCode());
            result = prime * result + ((listener == null) ? 0 : listener.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            ChannelListenerRegistration other = (ChannelListenerRegistration) obj;
            if (channelName == null) {
                if (other.channelName != null)
                    return false;
            } else if (!channelName.equals(other.channelName))
                return false;
            return listener != other.listener;
        }
    }

}

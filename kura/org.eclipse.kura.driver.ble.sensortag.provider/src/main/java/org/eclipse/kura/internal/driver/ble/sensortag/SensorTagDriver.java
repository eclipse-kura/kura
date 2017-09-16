/**
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Eurotech
 */
package org.eclipse.kura.internal.driver.ble.sensortag;

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;
import static org.eclipse.kura.KuraErrorCode.OPERATION_NOT_SUPPORTED;
import static org.eclipse.kura.channel.ChannelFlag.FAILURE;
import static org.eclipse.kura.channel.ChannelFlag.SUCCESS;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.KuraRuntimeException;
import org.eclipse.kura.bluetooth.le.BluetoothLeAdapter;
import org.eclipse.kura.bluetooth.le.BluetoothLeDevice;
import org.eclipse.kura.bluetooth.le.BluetoothLeService;
import org.eclipse.kura.channel.ChannelFlag;
import org.eclipse.kura.channel.ChannelRecord;
import org.eclipse.kura.channel.ChannelStatus;
import org.eclipse.kura.channel.listener.ChannelListener;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.driver.ChannelDescriptor;
import org.eclipse.kura.driver.Driver;
import org.eclipse.kura.driver.PreparedRead;
import org.eclipse.kura.driver.ble.sensortag.localization.SensorTagMessages;
import org.eclipse.kura.localization.LocalizationAdapter;
import org.eclipse.kura.type.DataType;
import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.type.TypedValues;
import org.eclipse.kura.util.base.TypeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Class {@link SensorTagDriver} is a BLE SensorTag Driver implementation for
 * Kura Asset-Driver Topology.
 * <br/>
 * <br/>
 * This BLE SensorTag Driver can be used in cooperation with Kura Asset Model and in
 * isolation as well. In case of isolation, the properties needs to be provided
 * externally.
 * <br/>
 * <br/>
 * The required properties are enlisted in {@link SensorTagChannelDescriptor} and
 * the driver connection specific properties are enlisted in
 * {@link SensorTagOptions}
 *
 * @see Driver
 * @see SensorTagOptions
 * @see SensorTagChannelDescriptor
 *
 */
public final class SensorTagDriver implements Driver, ConfigurableComponent {

    private static final Logger logger = LoggerFactory.getLogger(SensorTagDriver.class);
    private static final SensorTagMessages message = LocalizationAdapter.adapt(SensorTagMessages.class);

    private static final int TIMEOUT = 5;

    private SensorTagOptions options;
    private BluetoothLeService bluetoothLeService;
    private BluetoothLeAdapter bluetoothLeAdapter;
    private Map<String, TiSensorTag> tiSensorTagMap;

    protected synchronized void bindBluetoothLeService(final BluetoothLeService bluetoothLeService) {
        if (isNull(this.bluetoothLeService)) {
            this.bluetoothLeService = bluetoothLeService;
        }
    }

    protected synchronized void unbindBluetoothLeService(final BluetoothLeService bluetoothLeService) {
        if (this.bluetoothLeService == bluetoothLeService) {
            this.bluetoothLeService = null;
        }
    }

    protected synchronized void activate(final Map<String, Object> properties) {
        logger.debug("Activating BLE SensorTag Driver...");
        this.tiSensorTagMap = new HashMap<>();
        doUpdate(properties);
        logger.debug("Activating BLE SensorTag Driver... Done");
    }

    protected synchronized void deactivate() {
        logger.debug("Deactivating BLE SensorTag Driver...");
        doDeactivate();
        logger.debug("Deactivating BLE SensorTag Driver... Done");
    }

    public synchronized void updated(final Map<String, Object> properties) {
        logger.debug("Updating BLE SensorTag Driver...");
        doDeactivate();
        doUpdate(properties);
        logger.debug("Updating BLE SensorTag Driver... Done");
    }

    private void doDeactivate() {
        if (this.bluetoothLeAdapter != null && this.bluetoothLeAdapter.isDiscovering()) {
            try {
                this.bluetoothLeAdapter.stopDiscovery();
            } catch (KuraException e) {
                logger.error(message.errorStopDiscovery(), e);
            }
        }

        try {
            disconnect();
        } catch (ConnectionException e) {
            logger.error("Disconnecrtion failed", e);
        }

        // cancel bluetoothAdapter
        this.bluetoothLeAdapter = null;
    }

    private void doUpdate(Map<String, Object> properties) {

        this.extractProperties(properties);
        // Get Bluetooth adapter and ensure it is enabled
        this.bluetoothLeAdapter = this.bluetoothLeService.getAdapter(this.options.getBluetoothInterfaceName());
        if (this.bluetoothLeAdapter != null) {
            logger.info("Bluetooth adapter interface => {}", this.options.getBluetoothInterfaceName());
            if (!this.bluetoothLeAdapter.isPowered()) {
                logger.info("Enabling bluetooth adapter...");
                this.bluetoothLeAdapter.setPowered(true);
            }
            logger.info("Bluetooth adapter address => {}", this.bluetoothLeAdapter.getAddress());
        } else {
            logger.info("Bluetooth adapter {} not found.", this.options.getBluetoothInterfaceName());
        }
    }

    @Override
    public void connect() throws ConnectionException {
        // connect to all TiSensorTags in the map
        for (Entry<String, TiSensorTag> entry : this.tiSensorTagMap.entrySet()) {
            if (!entry.getValue().isConnected()) {
                connect(entry.getValue());
            }
        }
    }

    private void connect(TiSensorTag sensorTag) {
        sensorTag.connect();
        if (sensorTag.isConnected()) {
            sensorTag.enableTermometer();

            sensorTag.setAccelerometerPeriod(50);
            if (sensorTag.isCC2650()) {
                byte[] config = { 0x38, 0x02 };
                sensorTag.enableAccelerometer(config);
            } else {
                byte[] config = { 0x01 };
                sensorTag.enableAccelerometer(config);
            }

            sensorTag.enableHygrometer();

            sensorTag.setMagnetometerPeriod(50);
            if (sensorTag.isCC2650()) {
                byte[] config = { 0x40, 0x00 };
                sensorTag.enableMagnetometer(config);
            } else {
                byte[] config = { 0x01 };
                sensorTag.enableMagnetometer(config);
            }

            sensorTag.calibrateBarometer();

            sensorTag.enableBarometer();

            if (sensorTag.isCC2650()) {
                sensorTag.setGyroscopePeriod(50);
                byte[] config = { 0x07, 0x00 };
                sensorTag.enableGyroscope(config);
            } else {
                byte[] config = { 0x07 };
                sensorTag.enableGyroscope(config);
            }

            sensorTag.enableLuxometer();

            sensorTag.enableIOService();
            sensorTag.switchOffBuzzer();
            sensorTag.switchOffGreenLed();
            sensorTag.switchOffRedLed();
        }
    }

    @Override
    public void disconnect() throws ConnectionException {
        // disconnect SensorTags
        for (Entry<String, TiSensorTag> entry : this.tiSensorTagMap.entrySet()) {
            if (entry.getValue().isConnected()) {
                entry.getValue().disconnect();
            }
        }
        this.tiSensorTagMap.clear();
    }

    private void extractProperties(final Map<String, Object> properties) {
        requireNonNull(properties, message.propertiesNonNull());
        this.options = new SensorTagOptions(properties);
    }

    @Override
    public ChannelDescriptor getChannelDescriptor() {
        return new SensorTagChannelDescriptor();
    }

    private Optional<TypedValue<?>> getTypedValue(final DataType expectedValueType, final Object containedValue) {
        try {
            switch (expectedValueType) {
            case LONG:
                return Optional.of(TypedValues.newLongValue((long) Double.parseDouble(containedValue.toString())));
            case FLOAT:
                return Optional.of(TypedValues.newFloatValue(Float.parseFloat(containedValue.toString())));
            case DOUBLE:
                return Optional.of(TypedValues.newDoubleValue(Double.parseDouble(containedValue.toString())));
            case INTEGER:
                return Optional.of(TypedValues.newIntegerValue((int) Double.parseDouble(containedValue.toString())));
            case BOOLEAN:
                return Optional.of(TypedValues.newBooleanValue(Boolean.parseBoolean(containedValue.toString())));
            case STRING:
                return Optional.of(TypedValues.newStringValue(containedValue.toString()));
            case BYTE_ARRAY:
                return Optional.of(TypedValues.newByteArrayValue(TypeUtil.objectToByteArray(containedValue)));
            default:
                return Optional.empty();
            }
        } catch (final Exception ex) {
            logger.error(message.errorValueTypeConversion(), ex);
            return Optional.empty();
        }
    }

    private void runReadRequest(SensorTagRequestInfo requestInfo) {
        TiSensorTag sensorTag = getSensorTag(requestInfo);

        ChannelRecord record = requestInfo.channelRecord;
        Object readResult = null;
        switch (requestInfo.sensorName) {
        case TEMP_AMBIENT:
            readResult = sensorTag.readTemperature()[0];
            break;
        case TEMP_TARGET:
            readResult = sensorTag.readTemperature()[1];
            break;
        case HUMIDITY:
            readResult = sensorTag.readHumidity();
            break;
        case ACCELERATION_X:
            readResult = sensorTag.readAcceleration()[0];
            break;
        case ACCELERATION_Y:
            readResult = sensorTag.readAcceleration()[1];
            break;
        case ACCELERATION_Z:
            readResult = sensorTag.readAcceleration()[2];
            break;
        case MAGNETIC_X:
            readResult = sensorTag.readMagneticField()[0];
            break;
        case MAGNETIC_Y:
            readResult = sensorTag.readMagneticField()[1];
            break;
        case MAGNETIC_Z:
            readResult = sensorTag.readMagneticField()[2];
            break;
        case GYROSCOPE_X:
            readResult = sensorTag.readGyroscope()[0];
            break;
        case GYROSCOPE_Y:
            readResult = sensorTag.readGyroscope()[1];
            break;
        case GYROSCOPE_Z:
            readResult = sensorTag.readGyroscope()[2];
            break;
        case LIGHT:
            readResult = sensorTag.readLight();
            break;
        case PRESSURE:
            readResult = sensorTag.readPressure();
            break;
        default:
        }
        if (readResult == null) {
            record.setChannelStatus(new ChannelStatus(ChannelFlag.FAILURE, message.readFailed(), null));
            record.setTimestamp(System.currentTimeMillis());
            logger.warn(message.readFailed());
            return;
        }

        final Optional<TypedValue<?>> typedValue = this.getTypedValue(requestInfo.dataType, readResult);
        if (!typedValue.isPresent()) {
            record.setChannelStatus(new ChannelStatus(FAILURE, message.errorValueTypeConversion(), null));
            record.setTimestamp(System.currentTimeMillis());
            return;
        }
        record.setValue(typedValue.get());
        record.setChannelStatus(new ChannelStatus(SUCCESS));
        record.setTimestamp(System.currentTimeMillis());
    }

    private TiSensorTag getSensorTag(SensorTagRequestInfo requestInfo) {
        if (!tiSensorTagMap.containsKey(requestInfo.sensorTagAddress)) {
            Future<BluetoothLeDevice> future = this.bluetoothLeAdapter.findDeviceByAddress(TIMEOUT,
                    requestInfo.sensorTagAddress);
            BluetoothLeDevice device = null;
            try {
                device = future.get();
            } catch (InterruptedException | ExecutionException e) {
                logger.error("Get SensorTag {} failed", requestInfo.sensorTagAddress, e);
            }
            if (device != null) {
                tiSensorTagMap.put(requestInfo.sensorTagAddress, new TiSensorTag(device));
            }
        }
        TiSensorTag sensorTag = tiSensorTagMap.get(requestInfo.sensorTagAddress);
        if (!sensorTag.isConnected()) {
            connect(sensorTag);
        }
        return sensorTag;
    }

    @Override
    public void read(final List<ChannelRecord> records) throws ConnectionException {
        for (final ChannelRecord record : records) {
            SensorTagRequestInfo.extract(record).ifPresent(this::runReadRequest);
        }
    }

    @Override
    public void registerChannelListener(final Map<String, Object> channelConfig, final ChannelListener listener)
            throws ConnectionException {
        throw new KuraRuntimeException(OPERATION_NOT_SUPPORTED);
    }

    @Override
    public void unregisterChannelListener(final ChannelListener listener) throws ConnectionException {
        throw new KuraRuntimeException(OPERATION_NOT_SUPPORTED);
    }

    private void runWriteRequest(SensorTagRequestInfo requestInfo) {
        TiSensorTag sensorTag = getSensorTag(requestInfo);

        ChannelRecord record = requestInfo.channelRecord;
        record.setTimestamp(System.currentTimeMillis());
        final TypedValue<?> value = record.getValue();
        if (!value.getType().equals(DataType.BOOLEAN)) {
            record.setChannelStatus(new ChannelStatus(FAILURE, message.writeFailed(), null));
            logger.error("Only boolean types are allowed for this asset");
            return;
        }

        switch (requestInfo.sensorName) {
        case GREEN_LED:
            if ((boolean) value.getValue()) {
                sensorTag.switchOnGreenLed();
            } else {
                sensorTag.switchOffGreenLed();
            }
            break;
        case RED_LED:
            if ((boolean) value.getValue()) {
                sensorTag.switchOnRedLed();
            } else {
                sensorTag.switchOffRedLed();
            }
            break;
        case BUZZER:
            if ((boolean) value.getValue()) {
                sensorTag.switchOnBuzzer();
            } else {
                sensorTag.switchOffBuzzer();
            }
            break;
        default:
        }

        record.setChannelStatus(new ChannelStatus(SUCCESS));
    }

    @Override
    public void write(final List<ChannelRecord> records) throws ConnectionException {
        for (final ChannelRecord record : records) {
            SensorTagRequestInfo.extract(record).ifPresent(this::runWriteRequest);
        }
    }

    private static class SensorTagRequestInfo {

        private final DataType dataType;
        private final String sensorTagAddress;
        private final SensorName sensorName;
        private final ChannelRecord channelRecord;

        public SensorTagRequestInfo(final ChannelRecord channelRecord, final DataType dataType,
                final String sensorTagAddress, final SensorName sensorName) {
            this.dataType = dataType;
            this.sensorTagAddress = sensorTagAddress;
            this.sensorName = sensorName;
            this.channelRecord = channelRecord;
        }

        private static void fail(final ChannelRecord record, final String message) {
            record.setChannelStatus(new ChannelStatus(FAILURE, message, null));
            record.setTimestamp(System.currentTimeMillis());
        }

        public static Optional<SensorTagRequestInfo> extract(final ChannelRecord record) {
            final Map<String, Object> channelConfig = record.getChannelConfig();
            final String sensorTagAddress;
            final SensorName sensorName;

            try {
                sensorTagAddress = SensorTagChannelDescriptor.getsensorTagAddress(channelConfig);
            } catch (final Exception e) {
                fail(record, message.errorRetrievingAddress());
                logger.error("Error retrieving SensorTag Address", e);
                return Optional.empty();
            }

            try {
                sensorName = SensorTagChannelDescriptor.getSensorName(channelConfig);
            } catch (final Exception e) {
                fail(record, message.errorRetrievingSensorName());
                logger.error("Error retrieving Sensor name", e);
                return Optional.empty();
            }

            final DataType dataType = record.getValueType();

            if (isNull(dataType)) {
                fail(record, message.errorRetrievingValueType());
                return Optional.empty();
            }

            return Optional.of(new SensorTagRequestInfo(record, dataType, sensorTagAddress, sensorName));
        }
    }

    @Override
    public PreparedRead prepareRead(List<ChannelRecord> channelRecords) {
        requireNonNull(channelRecords, message.recordListNonNull());

        SensorTagPreparedRead preparedRead = new SensorTagPreparedRead();
        preparedRead.channelRecords = channelRecords;

        for (ChannelRecord record : channelRecords) {
            SensorTagRequestInfo.extract(record).ifPresent(preparedRead.requestInfos::add);
        }
        return preparedRead;
    }

    private class SensorTagPreparedRead implements PreparedRead {

        private List<SensorTagRequestInfo> requestInfos = new ArrayList<>();
        private volatile List<ChannelRecord> channelRecords;

        @Override
        public synchronized List<ChannelRecord> execute() throws ConnectionException {
            for (SensorTagRequestInfo requestInfo : requestInfos) {
                SensorTagDriver.this.runReadRequest(requestInfo);
            }

            return Collections.unmodifiableList(channelRecords);
        }

        @Override
        public List<ChannelRecord> getChannelRecords() {
            return Collections.unmodifiableList(channelRecords);
        }

        @Override
        public void close() {
            // Method not supported
        }
    }
}

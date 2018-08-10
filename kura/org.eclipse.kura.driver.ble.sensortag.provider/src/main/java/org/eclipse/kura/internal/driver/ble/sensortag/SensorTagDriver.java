/**
 * Copyright (c) 2017, 2018 Eurotech and/or its affiliates and others
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
import static org.eclipse.kura.channel.ChannelFlag.FAILURE;
import static org.eclipse.kura.channel.ChannelFlag.SUCCESS;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.eclipse.kura.KuraBluetoothIOException;
import org.eclipse.kura.KuraException;
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

    private static final int TIMEOUT = 5;
    private static final String INTERRUPTED_EX = "Interrupted Exception";

    private SensorTagOptions options;
    private BluetoothLeService bluetoothLeService;
    private BluetoothLeAdapter bluetoothLeAdapter;
    private Map<String, TiSensorTag> tiSensorTagMap;
    private Set<SensorListener> sensorListeners;

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
        this.sensorListeners = new HashSet<>();
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
                logger.error("Failed to stop discovery", e);
            }
        }

        try {
            disconnect();
        } catch (ConnectionException e) {
            logger.error("Disconnection failed", e);
        }

        // cancel bluetoothAdapter
        this.bluetoothLeAdapter = null;
    }

    private void doUpdate(Map<String, Object> properties) {

        extractProperties(properties);
        // Get Bluetooth adapter and ensure it is enabled
        this.bluetoothLeAdapter = this.bluetoothLeService.getAdapter(this.options.getBluetoothInterfaceName());
        if (this.bluetoothLeAdapter != null) {
            logger.info("Bluetooth adapter interface => {}", this.options.getBluetoothInterfaceName());
            if (!this.bluetoothLeAdapter.isPowered()) {
                logger.info("Enabling bluetooth adapter...");
                this.bluetoothLeAdapter.setPowered(true);
                waitFor(1000);
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

    private void connect(TiSensorTag sensorTag) throws ConnectionException {
        sensorTag.connect();
        if (sensorTag.isConnected()) {
            sensorTag.init();
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

            if (sensorTag.enableIOService()) {
                sensorTag.switchOffBuzzer();
                sensorTag.switchOffGreenLed();
                sensorTag.switchOffRedLed();
            }
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
        requireNonNull(properties, "Properties cannot be null");
        this.options = new SensorTagOptions(properties);
    }

    @Override
    public ChannelDescriptor getChannelDescriptor() {
        return new SensorTagChannelDescriptor();
    }

    public static Optional<TypedValue<?>> getTypedValue(final DataType expectedValueType, final Object containedValue) {
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
            logger.error("Error while converting the retrieved value to the defined typed", ex);
            return Optional.empty();
        }
    }

    private void runReadRequest(SensorTagRequestInfo requestInfo) {

        ChannelRecord record = requestInfo.channelRecord;
        try {
            TiSensorTag sensorTag = getSensorTag(requestInfo.sensorTagAddress);
            if (sensorTag.isConnected()) {
                Object readResult = getReadResult(requestInfo.sensorName, sensorTag);
                final Optional<TypedValue<?>> typedValue = getTypedValue(requestInfo.dataType, readResult);
                if (!typedValue.isPresent()) {
                    record.setChannelStatus(new ChannelStatus(FAILURE,
                            "Error while converting the retrieved value to the defined typed", null));
                    record.setTimestamp(System.currentTimeMillis());
                    return;
                }
                record.setValue(typedValue.get());
                record.setChannelStatus(new ChannelStatus(SUCCESS));
                record.setTimestamp(System.currentTimeMillis());
            } else {
                record.setChannelStatus(new ChannelStatus(FAILURE, "Unable to Connect...", null));
                record.setTimestamp(System.currentTimeMillis());
                return;
            }
        } catch (KuraBluetoothIOException | ConnectionException e) {
            record.setChannelStatus(new ChannelStatus(ChannelFlag.FAILURE, "SensortTag Read Operation Failed", null));
            record.setTimestamp(System.currentTimeMillis());
            logger.warn(e.getMessage());
            return;
        }
    }

    private Object getReadResult(SensorName sensorName, TiSensorTag sensorTag) throws KuraBluetoothIOException {
        switch (sensorName) {
        case TEMP_AMBIENT:
            return sensorTag.readTemperature()[0];
        case TEMP_TARGET:
            return sensorTag.readTemperature()[1];
        case HUMIDITY:
            return sensorTag.readHumidity();
        case ACCELERATION_X:
            return sensorTag.readAcceleration()[0];
        case ACCELERATION_Y:
            return sensorTag.readAcceleration()[1];
        case ACCELERATION_Z:
            return sensorTag.readAcceleration()[2];
        case MAGNETIC_X:
            return sensorTag.readMagneticField()[0];
        case MAGNETIC_Y:
            return sensorTag.readMagneticField()[1];
        case MAGNETIC_Z:
            return sensorTag.readMagneticField()[2];
        case GYROSCOPE_X:
            return sensorTag.readGyroscope()[0];
        case GYROSCOPE_Y:
            return sensorTag.readGyroscope()[1];
        case GYROSCOPE_Z:
            return sensorTag.readGyroscope()[2];
        case LIGHT:
            return sensorTag.readLight();
        case PRESSURE:
            return sensorTag.readPressure();
        default:
            throw new KuraBluetoothIOException("Read is unsupported for sensor " + sensorName.toString());
        }
    }

    private TiSensorTag getSensorTag(String sensorTagAddress) throws KuraBluetoothIOException, ConnectionException {
        requireNonNull(sensorTagAddress);
        if (!this.tiSensorTagMap.containsKey(sensorTagAddress)) {
            Future<BluetoothLeDevice> future = this.bluetoothLeAdapter.findDeviceByAddress(TIMEOUT, sensorTagAddress);
            BluetoothLeDevice device = null;
            try {
                device = future.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("Get SensorTag {} failed", sensorTagAddress, e);
            } catch (ExecutionException e) {
                logger.error("Get SensorTag {} failed", sensorTagAddress, e);
            }
            if (device != null) {
                this.tiSensorTagMap.put(sensorTagAddress, new TiSensorTag(device));
            } else {
                throw new KuraBluetoothIOException("Resource unavailable");
            }
        }
        TiSensorTag sensorTag = this.tiSensorTagMap.get(sensorTagAddress);
        if (!sensorTag.isConnected()) {
            connect(sensorTag);
        }
        sensorTag.init();
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

        try {
            TiSensorTag sensorTag = getSensorTag(SensorTagChannelDescriptor.getSensorTagAddress(channelConfig));
            if (sensorTag.isConnected()) {
                SensorListener sensorListener = getSensorListener(sensorTag,
                        SensorTagChannelDescriptor.getSensorName(channelConfig).toString().split("_")[0],
                        SensorTagChannelDescriptor.getNotificationPeriod(channelConfig));
                sensorListener.addChannelName((String) channelConfig.get("+name"));
                sensorListener.addDataType(DataType.getDataType((String) channelConfig.get("+value.type")));
                sensorListener.addListener(listener);
                sensorListener.addSensorName(SensorTagChannelDescriptor.getSensorName(channelConfig));
                registerNotification(sensorListener);
            } else {
                logger.warn("Listener registration failed: TiSensorTag not connected");
            }
        } catch (KuraBluetoothIOException | ConnectionException e) {
            logger.error("Listener registration failed", e);
        }
    }

    @Override
    public void unregisterChannelListener(final ChannelListener listener) throws ConnectionException {
        Iterator<SensorListener> iterator = this.sensorListeners.iterator();
        while (iterator.hasNext()) {
            SensorListener sensorListener = iterator.next();
            if (sensorListener.getListeners().contains(listener)) {
                if (sensorListener.getListeners().size() == 1) {
                    unregisterNotification(sensorListener);
                    iterator.remove();
                } else {
                    int index = sensorListener.getListeners().indexOf(listener);
                    sensorListener.removeAll(index);
                }
            }
        }
    }

    private void runWriteRequest(SensorTagRequestInfo requestInfo) {
        ChannelRecord record = requestInfo.channelRecord;
        record.setTimestamp(System.currentTimeMillis());

        TiSensorTag sensorTag = null;
        try {
            sensorTag = getSensorTag(requestInfo.sensorTagAddress);
        } catch (KuraBluetoothIOException | ConnectionException e) {
            record.setChannelStatus(new ChannelStatus(FAILURE, "SensortTag Write Operation Failed", null));
            logger.error("SensorTag {} not found", requestInfo.sensorTagAddress, e);
            return;
        }

        final TypedValue<?> value = record.getValue();
        if (!value.getType().equals(DataType.BOOLEAN)) {
            record.setChannelStatus(new ChannelStatus(FAILURE, "SensortTag Write Operation Failed", null));
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
                sensorTagAddress = SensorTagChannelDescriptor.getSensorTagAddress(channelConfig);
            } catch (final Exception e) {
                fail(record, "Error while retrieving SensortTag address");
                logger.error("Error retrieving SensorTag Address", e);
                return Optional.empty();
            }

            try {
                sensorName = SensorTagChannelDescriptor.getSensorName(channelConfig);
            } catch (final Exception e) {
                fail(record, "Error while retrieving sensor name");
                logger.error("Error retrieving Sensor name", e);
                return Optional.empty();
            }

            final DataType dataType = record.getValueType();

            if (isNull(dataType)) {
                fail(record, "Error while retrieving value type");
                return Optional.empty();
            }

            return Optional.of(new SensorTagRequestInfo(record, dataType, sensorTagAddress, sensorName));
        }
    }

    @Override
    public PreparedRead prepareRead(List<ChannelRecord> channelRecords) {
        requireNonNull(channelRecords, "Channel Record list cannot be null");

        try (SensorTagPreparedRead preparedRead = new SensorTagPreparedRead()) {
            preparedRead.channelRecords = channelRecords;

            for (ChannelRecord record : channelRecords) {
                SensorTagRequestInfo.extract(record).ifPresent(preparedRead.requestInfos::add);
            }
            return preparedRead;
        }
    }

    private void registerNotification(SensorListener sensorListener) {
        switch (sensorListener.getSensorType()) {
        case "TEMP":
            sensorListener.getSensorTag().setTermometerPeriod((int) (sensorListener.getPeriod() / 10));
            unregisterTemperatureNotification(sensorListener);
            sensorListener.getSensorTag()
                    .enableTemperatureNotifications(SensorListener.getSensorConsumer(sensorListener));
            break;
        case "ACCELERATION":
            sensorListener.getSensorTag().setAccelerometerPeriod((int) (sensorListener.getPeriod() / 10));
            unregisterAccelerationNotification(sensorListener);
            sensorListener.getSensorTag()
                    .enableAccelerationNotifications(SensorListener.getSensorConsumer(sensorListener));
            break;
        case "GYROSCOPE":
            sensorListener.getSensorTag().setGyroscopePeriod((int) (sensorListener.getPeriod() / 10));
            unregisterGyroscopeNotification(sensorListener);
            sensorListener.getSensorTag()
                    .enableGyroscopeNotifications(SensorListener.getSensorConsumer(sensorListener));
            break;
        case "MAGNETIC":
            sensorListener.getSensorTag().setMagnetometerPeriod((int) (sensorListener.getPeriod() / 10));
            unregisterMagneticNotification(sensorListener);
            sensorListener.getSensorTag()
                    .enableMagneticFieldNotifications(SensorListener.getSensorConsumer(sensorListener));
            break;
        case "HUMIDITY":
            sensorListener.getSensorTag().setHygrometerPeriod((int) (sensorListener.getPeriod() / 10));
            unregisterHumidityNotification(sensorListener);
            sensorListener.getSensorTag().enableHumidityNotifications(SensorListener.getSensorConsumer(sensorListener));
            break;
        case "LIGHT":
            sensorListener.getSensorTag().setLuxometerPeriod((int) (sensorListener.getPeriod() / 10));
            unregisterLightNotification(sensorListener);
            sensorListener.getSensorTag().enableLightNotifications(SensorListener.getSensorConsumer(sensorListener));
            break;
        case "PRESSURE":
            sensorListener.getSensorTag().setBarometerPeriod((int) (sensorListener.getPeriod() / 10));
            unregisterPressureNotification(sensorListener);
            sensorListener.getSensorTag().enablePressureNotifications(SensorListener.getSensorConsumer(sensorListener));
            break;
        case "KEYS":
            // Register and unregister listeners for buttons too fast can cause problem on native library
            unregisterKeysNotification(sensorListener);
            waitFor(1000);
            sensorListener.getSensorTag().enableKeysNotification(SensorListener.getSensorConsumer(sensorListener));
            break;
        case "BUZZER":
        case "GREEN_LED":
        case "RED_LED":
            logger.info("Notifications not supported for buzzer and leds");
            break;
        default:

        }
    }

    private void unregisterKeysNotification(SensorListener sensorListener) {
        // Register and unregister listeners for buttons too fast can cause problem on native library
        waitFor(1000);
        if (sensorListener.getSensorTag().isKeysNotifying()) {
            sensorListener.getSensorTag().disableKeysNotifications();
        }
    }

    private void unregisterPressureNotification(SensorListener sensorListener) {
        if (sensorListener.getSensorTag().isBarometerNotifying()) {
            sensorListener.getSensorTag().disablePressureNotifications();
        }
    }

    private void unregisterLightNotification(SensorListener sensorListener) {
        if (sensorListener.getSensorTag().isLuxometerNotifying()) {
            sensorListener.getSensorTag().disableLightNotifications();
        }
    }

    private void unregisterHumidityNotification(SensorListener sensorListener) {
        if (sensorListener.getSensorTag().isHygrometerNotifying()) {
            sensorListener.getSensorTag().disableHumidityNotifications();
        }
    }

    private void unregisterMagneticNotification(SensorListener sensorListener) {
        if (sensorListener.getSensorTag().isMagnetometerNotifying()) {
            sensorListener.getSensorTag().disableMagneticFieldNotifications();
        }
    }

    private void unregisterGyroscopeNotification(SensorListener sensorListener) {
        if (sensorListener.getSensorTag().isGyroscopeNotifying()) {
            sensorListener.getSensorTag().disableGyroscopeNotifications();
        }
    }

    private void unregisterAccelerationNotification(SensorListener sensorListener) {
        if (sensorListener.getSensorTag().isAccelerometerNotifying()) {
            sensorListener.getSensorTag().disableAccelerationNotifications();
        }
    }

    private void unregisterTemperatureNotification(SensorListener sensorListener) {
        if (sensorListener.getSensorTag().isTermometerNotifying()) {
            sensorListener.getSensorTag().disableTemperatureNotifications();
        }
    }

    private void unregisterNotification(SensorListener sensorListener) {
        if (sensorListener.getSensorTag().isConnected()) {
            switch (sensorListener.getSensorType()) {
            case "TEMP":
                unregisterTemperatureNotification(sensorListener);
                break;
            case "ACCELERATION":
                unregisterAccelerationNotification(sensorListener);
                break;
            case "GYROSCOPE":
                unregisterGyroscopeNotification(sensorListener);
                break;
            case "MAGNETIC":
                unregisterMagneticNotification(sensorListener);
                break;
            case "HUMIDITY":
                unregisterHumidityNotification(sensorListener);
                break;
            case "LIGHT":
                unregisterLightNotification(sensorListener);
                break;
            case "PRESSURE":
                unregisterPressureNotification(sensorListener);
                break;
            case "KEYS":
                unregisterKeysNotification(sensorListener);
                break;
            case "BUZZER":
            case "GREEN_LED":
            case "RED_LED":
                logger.info("Notifications not supported for buzzer and leds");
                break;
            default:

            }
        } else {
            logger.info("Listener unregistation failed: TiSensorTag not connected");
        }
    }

    private class SensorTagPreparedRead implements PreparedRead {

        private final List<SensorTagRequestInfo> requestInfos = new ArrayList<>();
        private volatile List<ChannelRecord> channelRecords;

        @Override
        public synchronized List<ChannelRecord> execute() throws ConnectionException {
            for (SensorTagRequestInfo requestInfo : this.requestInfos) {
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

    private SensorListener getSensorListener(TiSensorTag sensorTag, String sensorType, int period) {
        for (SensorListener listener : this.sensorListeners) {
            if (sensorTag == listener.getSensorTag() && sensorType.equals(listener.getSensorType())) {
                return listener;
            }
        }
        SensorListener sensorListener = new SensorListener(sensorTag, sensorType, period);
        this.sensorListeners.add(sensorListener);
        return sensorListener;
    }

    protected static void waitFor(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error(INTERRUPTED_EX, e);
        }
    }
}

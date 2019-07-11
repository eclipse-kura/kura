/**
 * Copyright (c) 2019 Eurotech and/or its affiliates and others
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Eurotech
 */
package org.eclipse.kura.internal.driver.ble.xdk;

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

public class XdkDriver implements Driver, ConfigurableComponent {
	
	private static final Logger logger = LoggerFactory.getLogger(XdkDriver.class);

    private static final int TIMEOUT = 5;
    private static final String INTERRUPTED_EX = "Interrupted Exception";
    
    private static final byte m1 = 0x01;
	private static final byte m2 = 0x02;

    private XdkOptions options;
    private BluetoothLeService bluetoothLeService;
    private BluetoothLeAdapter bluetoothLeAdapter;
    private Map<String, Xdk> xdkMap;
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
        logger.debug("Activating BLE Xdk Driver...");
        this.xdkMap = new HashMap<>();
        this.sensorListeners = new HashSet<>();
        doUpdate(properties);
        logger.debug("Activating BLE Xdk Driver... Done");
    }

    protected synchronized void deactivate() {
        logger.debug("Deactivating BLE Xdk Driver...");
        doDeactivate();
        logger.debug("Deactivating BLE Xdk Driver... Done");
    }

    public synchronized void updated(final Map<String, Object> properties) {
        logger.debug("Updating BLE Xdk Driver...");
        doDeactivate();
        doUpdate(properties);
        logger.debug("Updating BLE Xdk Driver... Done");
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
		// connect to all Xdk in the map
        for (Entry<String, Xdk> entry : this.xdkMap.entrySet()) {
            if (!entry.getValue().isConnected()) {
                connect(entry.getValue());
            }
        }	
	}
	
	private void connect(Xdk xdk) throws ConnectionException {
        xdk.connect();
        if (xdk.isConnected()) {
        	
            xdk.init();
   
            xdk.startSensor();
            
        }
    }
	
	private Xdk reconnect(final String xdkAddress) throws  ConnectionException {
        Xdk xdk = this.xdkMap.get(xdkAddress);
        if (!xdk.isConnected()) {
            connect(xdk);
        }
        xdk.init();
        return xdk;
    }
	
	@Override
	public void disconnect() throws ConnectionException {
		// disconnect Xdk
        for (Entry<String, Xdk> entry : this.xdkMap.entrySet()) {
            if (entry.getValue().isConnected()) {
                entry.getValue().disconnect();
            }
        }
        this.xdkMap.clear();
	}
    
    private void extractProperties(final Map<String, Object> properties) {
        requireNonNull(properties, "Properties cannot be null");
        this.options = new XdkOptions(properties);
    }
    
	@Override
	public ChannelDescriptor getChannelDescriptor() {
		return new XdkChannelDescriptor();
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
	
	private void runReadRequest(XdkRequestInfo requestInfo) {

        ChannelRecord record = requestInfo.channelRecord;
        try {
            Xdk xdk = getXdk(requestInfo.xdkAddress);
            if (xdk.isConnected()) {    /*Read the data*/
                Object readResult = getReadResult(requestInfo.sensorName, xdk); 
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
            record.setChannelStatus(new ChannelStatus(ChannelFlag.FAILURE, "Xdk Read Operation Failed", null));
            record.setTimestamp(System.currentTimeMillis());
            logger.warn(e.getMessage());
            return;
        }
    }
	
	private Object getReadResult(SensorName sensorName, Xdk xdk) throws KuraBluetoothIOException {
		
		
        switch (sensorName) {
        //High Priority Data
        case ACCELERATION_X:
            return xdk.readHighData()[0];
        case ACCELERATION_Y:
            return xdk.readHighData()[1];
        case ACCELERATION_Z:
            return xdk.readHighData()[2];   
        case GYROSCOPE_X:
            return xdk.readHighData()[3];
        case GYROSCOPE_Y:
            return xdk.readHighData()[4];
        case GYROSCOPE_Z:
            return xdk.readHighData()[5];
        //Low Priority Data - Message 1    
        case LIGHT:
            return xdk.readLowData(m1)[0];
        case NOISE:
        	return xdk.readLowData(m1)[1];
        case PRESSURE:
            return xdk.readLowData(m1)[2];
        case TEMPERATURE:
            return xdk.readLowData(m1)[3]; 
        case HUMIDITY: 
        	return xdk.readLowData(m1)[4];
        case SD_CARD_DETECT_STATUS:
        	return xdk.readLowData(m1)[5];
        case BUTTON_STATUS:
        	return xdk.readLowData(m1)[6];
        	//Low Priority Data - Message 2	
        case MAGNETIC_X:
            return xdk.readLowData(m2)[0];
        case MAGNETIC_Y:
            return xdk.readLowData(m2)[1];
        case MAGNETIC_Z:
            return xdk.readLowData(m2)[2];
        case MAGNETOMETER_RESISTANCE:
        	return xdk.readLowData(m2)[3];
        case LED_STATUS:
        	return xdk.readLowData(m2)[4];
        case VOLTAGE_LEM:
        	return xdk.readLowData(m2)[5];
        default:
            throw new KuraBluetoothIOException("Read is unsupported for sensor " + sensorName.toString());
        }
    }

	private Xdk getXdk(String xdkAddress) throws KuraBluetoothIOException, ConnectionException {
        requireNonNull(xdkAddress);
        if (!this.xdkMap.containsKey(xdkAddress)) {
            Future<BluetoothLeDevice> future = this.bluetoothLeAdapter.findDeviceByAddress(TIMEOUT, xdkAddress);
            BluetoothLeDevice device = null;
            try {
                device = future.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("Get Xdk {} failed", xdkAddress, e);
            } catch (ExecutionException e) {
                logger.error("Get Xdk {} failed", xdkAddress, e);
            }
            if (device != null) {
                this.xdkMap.put(xdkAddress, new Xdk(device));
            } else {
                throw new KuraBluetoothIOException("Resource unavailable");
            }
        }
        Xdk xdk = this.xdkMap.get(xdkAddress);
        if (!xdk.isConnected()) {
            connect(xdk);
        }
        xdk.init();
        return xdk;
    }

	@Override
	public void read(final List<ChannelRecord> records) throws ConnectionException {
        for (final ChannelRecord record : records) {
        	XdkRequestInfo.extract(record).ifPresent(this::runReadRequest);
        }
	}

	@Override
    public void registerChannelListener(final Map<String, Object> channelConfig, final ChannelListener listener)
            throws ConnectionException {

        try {
            Xdk xdk = getXdk(XdkChannelDescriptor.getXdkAddress(channelConfig));
            if (xdk.isConnected()) {
                SensorListener sensorListener = getSensorListener(xdk,
                        XdkChannelDescriptor.getSensorName(channelConfig).toString(),
                        XdkChannelDescriptor.getNotificationPeriod(channelConfig));
                sensorListener.addChannelName((String) channelConfig.get("+name"));
                sensorListener.addDataType(DataType.getDataType((String) channelConfig.get("+value.type")));
                sensorListener.addListener(listener);
                sensorListener.addSensorName(XdkChannelDescriptor.getSensorName(channelConfig));
                registerNotification(sensorListener);
            } else {
                logger.warn("Listener registration failed: TiSensorTag not connected");
            }
        } catch (KuraBluetoothIOException | ConnectionException e) {
            logger.error("Listener registration failed", e);
        }
    }
	
	@Override
	public void unregisterChannelListener(ChannelListener listener) throws ConnectionException {
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

	@Override
	public void write(List<ChannelRecord> records) throws ConnectionException {
		
	}
	

	private static class XdkRequestInfo {

        private final DataType dataType;
        private final String xdkAddress;
        private final SensorName sensorName;
        private final ChannelRecord channelRecord;

        public XdkRequestInfo(final ChannelRecord channelRecord, final DataType dataType,
                final String xdkAddress, final SensorName sensorName) {
            this.dataType = dataType;
            this.xdkAddress = xdkAddress;
            this.sensorName = sensorName;
            this.channelRecord = channelRecord;
        }

        private static void fail(final ChannelRecord record, final String message) {
            record.setChannelStatus(new ChannelStatus(FAILURE, message, null));
            record.setTimestamp(System.currentTimeMillis());
        }

        public static Optional<XdkRequestInfo> extract(final ChannelRecord record) {
            final Map<String, Object> channelConfig = record.getChannelConfig();
            final String xdkAddress;
            final SensorName sensorName;

            try {
                xdkAddress = XdkChannelDescriptor.getXdkAddress(channelConfig);
            } catch (final Exception e) {
                fail(record, "Error while retrieving Xdk address");
                logger.error("Error retrieving Xdk Address", e);
                return Optional.empty();
            }

            try {
                sensorName = XdkChannelDescriptor.getSensorName(channelConfig);
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

            return Optional.of(new XdkRequestInfo(record, dataType, xdkAddress, sensorName));
        }
    }

	@Override
	public PreparedRead prepareRead(List<ChannelRecord> channelRecords) {
		requireNonNull(channelRecords, "Channel Record list cannot be null");

        try (XdkPreparedRead preparedRead = new XdkPreparedRead()) {
            preparedRead.channelRecords = channelRecords;

            for (ChannelRecord record : channelRecords) {
                XdkRequestInfo.extract(record).ifPresent(preparedRead.requestInfos::add);
            }
            return preparedRead;
        }
	}
	 
	 private void registerNotification(SensorListener sensorListener) throws ConnectionException {
		 	if(!sensorListener.getXdk().isConnected()) {
		    connect(sensorListener.getXdk());
		 	}
	        switch (sensorListener.getSensorType()) {
	        case "ACCELERATION_X":
	        	sensorListener.getXdk().setSamplingRate((int) (sensorListener.getPeriod() / 10));
	        	sensorListener.getXdk().disableHighNotifications();
	        	sensorListener.getXdk().enableHighNotifications(SensorListener.getSensorConsumer(sensorListener));

	            break;
	        case "ACCELERATION_Y":
	        	sensorListener.getXdk().setSamplingRate((int) (sensorListener.getPeriod() / 10));
	        	sensorListener.getXdk().disableHighNotifications();
	            sensorListener.getXdk()
	                    .enableHighNotifications(SensorListener.getSensorConsumer(sensorListener));
	            break;
	        case "ACCELERATION_Z":
	        	sensorListener.getXdk().setSamplingRate((int) (sensorListener.getPeriod() / 10));
	        	sensorListener.getXdk().disableHighNotifications();
	            sensorListener.getXdk()
	                    .enableHighNotifications(SensorListener.getSensorConsumer(sensorListener));
	            break;
	        case "GYROSCOPE_X":
	        	sensorListener.getXdk().setSamplingRate((int) (sensorListener.getPeriod() / 10));
	        	sensorListener.getXdk().disableHighNotifications();
	            sensorListener.getXdk()
	                    .enableHighNotifications(SensorListener.getSensorConsumer(sensorListener));
	            break;
	        case "GYROSCOPE_Y":
	        	sensorListener.getXdk().setSamplingRate((int) (sensorListener.getPeriod() / 10));
	        	sensorListener.getXdk().disableHighNotifications();
	            sensorListener.getXdk()
	                    .enableHighNotifications(SensorListener.getSensorConsumer(sensorListener));
	            break;
	        case "GYROSCOPE_Z":
	        	sensorListener.getXdk().setSamplingRate((int) (sensorListener.getPeriod() / 10));
	        	sensorListener.getXdk().disableHighNotifications();
	            sensorListener.getXdk()
	                    .enableHighNotifications(SensorListener.getSensorConsumer(sensorListener));
	            break;
	        case "LIGHT":
	        	sensorListener.getXdk().setSamplingRate((int) (sensorListener.getPeriod() / 10));
	        	sensorListener.getXdk().disableLowNotifications();
	            sensorListener.getXdk()
	                    .enableLowNotifications(SensorListener.getSensorConsumer(sensorListener), m1);
	            break;
	        case "NOISE":
	        	sensorListener.getXdk().setSamplingRate((int) (sensorListener.getPeriod() / 10));
	        	sensorListener.getXdk().disableLowNotifications();
	            sensorListener.getXdk()
	                    .enableLowNotifications(SensorListener.getSensorConsumer(sensorListener), m1);
	            break;
	        case "PRESURE":
	        	sensorListener.getXdk().setSamplingRate((int) (sensorListener.getPeriod() / 10));
	        	sensorListener.getXdk().disableLowNotifications();
	            sensorListener.getXdk()
	                    .enableLowNotifications(SensorListener.getSensorConsumer(sensorListener), m1);
	            break;
	        case "TEMPERATURE":
	        	sensorListener.getXdk().setSamplingRate((int) (sensorListener.getPeriod() / 10));
	        	sensorListener.getXdk().disableLowNotifications();
	            sensorListener.getXdk()
	                    .enableLowNotifications(SensorListener.getSensorConsumer(sensorListener), m1);
	            break;
            case "HUMIDITY":
            	sensorListener.getXdk().setSamplingRate((int) (sensorListener.getPeriod() / 10));
            	sensorListener.getXdk().disableLowNotifications();
	            sensorListener.getXdk()
	                    .enableLowNotifications(SensorListener.getSensorConsumer(sensorListener), m1);
	            break;
	        case "SD_CARD_DETECT_STATUS":
	        	sensorListener.getXdk().setSamplingRate((int) (sensorListener.getPeriod() / 10));
	        	sensorListener.getXdk().disableLowNotifications();
	            sensorListener.getXdk()
	                    .enableLowNotifications(SensorListener.getSensorConsumer(sensorListener), m1);
	            break;
	        case "BUTTON_STATUS":
	        	sensorListener.getXdk().setSamplingRate((int) (sensorListener.getPeriod() / 10));
	        	sensorListener.getXdk().disableLowNotifications();
	        	waitFor(1000);
	            sensorListener.getXdk()
	                    .enableLowNotifications(SensorListener.getSensorConsumer(sensorListener), m1);
	            break;
	        case "MAGNETIC_X":
	        	sensorListener.getXdk().setSamplingRate((int) (sensorListener.getPeriod() / 10));
	        	sensorListener.getXdk().disableLowNotifications();
	            sensorListener.getXdk()
	                    .enableLowNotifications(SensorListener.getSensorConsumer(sensorListener), m2);
	            break;
	        case "MAGNETIC_Y":
	        	sensorListener.getXdk().setSamplingRate((int) (sensorListener.getPeriod() / 10));
	        	sensorListener.getXdk().disableLowNotifications();
	            sensorListener.getXdk()
	                    .enableLowNotifications(SensorListener.getSensorConsumer(sensorListener), m2);
	            break;
	        case "MAGNETIC_Z":
	        	sensorListener.getXdk().setSamplingRate((int) (sensorListener.getPeriod() / 10));
	        	sensorListener.getXdk().disableLowNotifications();
	            sensorListener.getXdk()
	                    .enableLowNotifications(SensorListener.getSensorConsumer(sensorListener), m2);
	            break;
	        case "MAGNETOMETER_RESISTENCE":
	        	sensorListener.getXdk().setSamplingRate((int) (sensorListener.getPeriod() / 10));
	        	sensorListener.getXdk().disableLowNotifications();
	            sensorListener.getXdk()
	                    .enableLowNotifications(SensorListener.getSensorConsumer(sensorListener), m2);
	            break;
	        case "LED_STATUS":
	        	sensorListener.getXdk().setSamplingRate((int) (sensorListener.getPeriod() / 10));
	        	sensorListener.getXdk().disableLowNotifications();
	            sensorListener.getXdk()
	                    .enableLowNotifications(SensorListener.getSensorConsumer(sensorListener), m2);
	            break;
	        case "VOLTAGE_LEM":
	        	sensorListener.getXdk().setSamplingRate((int) (sensorListener.getPeriod() / 10));
	        	sensorListener.getXdk().disableLowNotifications();
	            sensorListener.getXdk()
	                    .enableLowNotifications(SensorListener.getSensorConsumer(sensorListener), m2);
	            break;
	        default:

	        }
	    }

	 
	 
	 private void unregisterSensorNotification(SensorListener sensorListener) {
		    if (sensorListener.getXdk().isHighNotifying()) {
	            sensorListener.getXdk().disableHighNotifications();
		    }else if (sensorListener.getXdk().isLowNotifying()) {
	            sensorListener.getXdk().disableLowNotifications(); 
	        }
	    }
	 
	 
	 private void unregisterNotification(SensorListener sensorListener) {
	        if (sensorListener.getXdk().isConnected()) {
	            switch (sensorListener.getSensorType()) {
	            case "ACCELERATION_X":
	            	unregisterSensorNotification(sensorListener);
		             break;
	            case "ACCELERATION_Y":
	            	unregisterSensorNotification(sensorListener);
		             break;
	            case "ACCELERATION_Z":
	            	unregisterSensorNotification(sensorListener);
		             break;
	            case "GYROSCOPE_X":
	            	unregisterSensorNotification(sensorListener);
		             break;
	            case "GYROSCOPE_Y":
	            	unregisterSensorNotification(sensorListener);
		             break;
	            case "GYROSCOPE_Z":
	            	unregisterSensorNotification(sensorListener);
		             break;
	            case "LIGHT":
	            	unregisterSensorNotification(sensorListener);
		             break;
	            case "PRESSURE":
	            	unregisterSensorNotification(sensorListener);
		             break;
	            case "TEMPERATURE":
	            	unregisterSensorNotification(sensorListener);
		             break;
	            case "HUMIDITY":
	            	unregisterSensorNotification(sensorListener);
		             break;
	            case "SD_CARD_DETECT_STATUS":
	            	unregisterSensorNotification(sensorListener);
		             break;
	            case "BUTTON_STATUS":
	            	unregisterSensorNotification(sensorListener);
		             break;
	            case "MAGNETIC_X":
	            	unregisterSensorNotification(sensorListener);
		             break;
	            case "MAGNETIC_Y":
	            	unregisterSensorNotification(sensorListener);
		             break;
	            case "MAGNETIC_Z":
	            	unregisterSensorNotification(sensorListener);
		             break;
	            case "MAGNETOMETER_RESISTENCE":
	            	unregisterSensorNotification(sensorListener);
		             break;
	            case "LED_STATUS":
	            	unregisterSensorNotification(sensorListener);
		             break;
	            case "VOLTAGE_LEM":
	            	 unregisterSensorNotification(sensorListener);
		             break;
	            default:

	            }
	        } else {
	            logger.info("Listener unregistation failed: TiSensorTag not connected");
	        }
	    }
	 
	
	 
	     private class XdkPreparedRead implements PreparedRead {

	        private final List<XdkRequestInfo> requestInfos = new ArrayList<>();
	        private volatile List<ChannelRecord> channelRecords;

	        @Override
	        public synchronized List<ChannelRecord> execute() throws ConnectionException {
	            for (XdkRequestInfo requestInfo : this.requestInfos) {
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
	    
		private SensorListener getSensorListener(Xdk xdk, String sensorType, int period) {
	        for (SensorListener listener : this.sensorListeners) {
	            if (xdk == listener.getXdk() && sensorType.equals(listener.getSensorType())) {
	                return listener;
	            }
	        }
	        SensorListener sensorListener = new SensorListener(xdk, sensorType, period);
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

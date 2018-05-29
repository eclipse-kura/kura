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
package org.eclipse.kura.internal.driver.ibeacon;

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;
import static org.eclipse.kura.channel.ChannelFlag.FAILURE;
import static org.eclipse.kura.channel.ChannelFlag.SUCCESS;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.KuraRuntimeException;
import org.eclipse.kura.ble.ibeacon.BluetoothLeIBeacon;
import org.eclipse.kura.ble.ibeacon.BluetoothLeIBeaconService;
import org.eclipse.kura.bluetooth.le.BluetoothLeAdapter;
import org.eclipse.kura.bluetooth.le.BluetoothLeService;
import org.eclipse.kura.bluetooth.le.beacon.BluetoothLeBeaconScanner;
import org.eclipse.kura.bluetooth.le.beacon.listener.BluetoothLeBeaconListener;
import org.eclipse.kura.channel.ChannelRecord;
import org.eclipse.kura.channel.ChannelStatus;
import org.eclipse.kura.channel.listener.ChannelEvent;
import org.eclipse.kura.channel.listener.ChannelListener;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.driver.ChannelDescriptor;
import org.eclipse.kura.driver.Driver;
import org.eclipse.kura.driver.PreparedRead;
import org.eclipse.kura.type.DataType;
import org.eclipse.kura.type.TypedValues;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Class {@link IBeaconDriver} is an iBeacon Driver implementation for
 * Kura Asset-Driver Topology.
 * <br/>
 * <br/>
 * This iBeacon Driver can be used in cooperation with Kura Asset Model and in
 * isolation as well. In case of isolation, the properties needs to be provided
 * externally.
 * <br/>
 * <br/>
 * The required properties are enlisted in {@link IBeaconChannelDescriptor} and
 * the driver connection specific properties are enlisted in
 * {@link IBeaconOptions}
 *
 * @see Driver
 * @see IBeaconOptions
 * @see IBeaconChannelDescriptor
 *
 */
public final class IBeaconDriver
        implements Driver, ConfigurableComponent, BluetoothLeBeaconListener<BluetoothLeIBeacon> {

    private static final Logger logger = LoggerFactory.getLogger(IBeaconDriver.class);

    private static final int SCAN_TIMEOUT = 600;
    private static final int MONITOR_TIMEOUT = 30;
    private static final String READ_ERROR_MESSAGE = "Read operation not supported";

    private ScheduledExecutorService worker;
    private ScheduledFuture<?> handle;

    private IBeaconOptions options;
    private BluetoothLeService bluetoothLeService;
    private BluetoothLeAdapter bluetoothLeAdapter;
    private BluetoothLeIBeaconService bluetoothLeIBeaconService;
    private BluetoothLeBeaconScanner<BluetoothLeIBeacon> bluetoothLeIBeaconScanner;

    private Set<IBeaconListener> iBeaconListeners;

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

    public synchronized void bindBluetoothLeIBeaconService(BluetoothLeIBeaconService bluetoothLeIBeaconService) {
        if (isNull(this.bluetoothLeIBeaconService)) {
            this.bluetoothLeIBeaconService = bluetoothLeIBeaconService;
        }
    }

    public synchronized void unbindBluetoothLeIBeaconService(BluetoothLeIBeaconService bluetoothLeIBeaconService) {
        if (this.bluetoothLeIBeaconService == bluetoothLeIBeaconService) {
            this.bluetoothLeIBeaconService = null;
        }
    }

    protected synchronized void activate(final Map<String, Object> properties) {
        logger.debug("Activating iBeacon Driver...");
        this.iBeaconListeners = new HashSet<>();
        doUpdate(properties);
        logger.debug("Activating iBeacon Driver... Done");
    }

    protected synchronized void deactivate() {
        logger.debug("Deactivating iBeacon Driver...");
        doDeactivate();
        logger.debug("Deactivating iBeacon Driver... Done");
    }

    public synchronized void updated(final Map<String, Object> properties) {
        logger.debug("Updating iBeacon Driver...");
        doDeactivate();
        doUpdate(properties);
        logger.debug("Updating iBeacon Driver... Done");
    }

    private void doDeactivate() {
        this.iBeaconListeners.clear();
        releaseResources();

        if (this.handle != null) {
            this.handle.cancel(true);
        }

        if (this.worker != null) {
            this.worker.shutdown();
        }

        // cancel bluetoothAdapter
        this.bluetoothLeAdapter = null;
    }

    private void doUpdate(Map<String, Object> properties) {
        extractProperties(properties);

        this.bluetoothLeAdapter = this.bluetoothLeService.getAdapter(this.options.getBluetoothInterfaceName());
        if (bluetoothLeAdapter != null) {
            if (!bluetoothLeAdapter.isPowered()) {
                bluetoothLeAdapter.setPowered(true);
            }
            this.bluetoothLeIBeaconScanner = this.bluetoothLeIBeaconService.newBeaconScanner(bluetoothLeAdapter);
            this.bluetoothLeIBeaconScanner.addBeaconListener(this);
            this.worker = Executors.newSingleThreadScheduledExecutor();
            // Setup a task that monitor the scan every 30 seconds
            this.handle = this.worker.scheduleAtFixedRate(this::monitor, 0, MONITOR_TIMEOUT, TimeUnit.SECONDS);
        } else {
            logger.warn("No Bluetooth adapter found ...");
        }
    }

    private void monitor() {
        if (!this.bluetoothLeIBeaconScanner.isScanning()) {
            try {
                // Perform a scan for 10 minutes
                this.bluetoothLeIBeaconScanner.startBeaconScan(SCAN_TIMEOUT);
            } catch (KuraException e) {
                logger.error("iBeacon scanning failed", e);
            }
        }
    }

    @Override
    public void connect() throws ConnectionException {
        // Not needed
    }

    @Override
    public void disconnect() throws ConnectionException {
        // Not needed
    }

    private void extractProperties(final Map<String, Object> properties) {
        requireNonNull(properties, "Properties cannot be null");
        this.options = new IBeaconOptions(properties);
    }

    @Override
    public ChannelDescriptor getChannelDescriptor() {
        return new IBeaconChannelDescriptor();
    }

    @Override
    public void read(final List<ChannelRecord> records) throws ConnectionException {
        logger.warn(READ_ERROR_MESSAGE);
        for (final ChannelRecord record : records) {
            record.setChannelStatus(new ChannelStatus(FAILURE, READ_ERROR_MESSAGE, null));
            record.setTimestamp(System.currentTimeMillis());
        }
    }

    @Override
    public void registerChannelListener(final Map<String, Object> channelConfig, final ChannelListener listener)
            throws ConnectionException {

        String channelName = (String) channelConfig.get("+name");
        IBeaconListener iBeaconListener = new IBeaconListener(channelName, listener);

        this.iBeaconListeners.add(iBeaconListener);
    }

    @Override
    public void unregisterChannelListener(final ChannelListener listener) throws ConnectionException {
        Iterator<IBeaconListener> iterator = this.iBeaconListeners.iterator();
        while (iterator.hasNext()) {
            IBeaconListener iBeaconListener = iterator.next();
            if (iBeaconListener.getListener().equals(listener)) {
                iterator.remove();
            }
        }
    }

    @Override
    public void write(final List<ChannelRecord> records) throws ConnectionException {
        logger.warn("Write operation not supported");
        throw new KuraRuntimeException(KuraErrorCode.OPERATION_NOT_SUPPORTED);
    }

    private void releaseResources() {
        if (this.bluetoothLeIBeaconScanner != null) {
            if (this.bluetoothLeIBeaconScanner.isScanning()) {
                this.bluetoothLeIBeaconScanner.stopBeaconScan();
            }
            this.bluetoothLeIBeaconScanner.removeBeaconListener(this);
            this.bluetoothLeIBeaconService.deleteBeaconScanner(this.bluetoothLeIBeaconScanner);
        }
    }

    @Override
    public PreparedRead prepareRead(List<ChannelRecord> channelRecords) {
        logger.warn(READ_ERROR_MESSAGE);
        return null;
    }

    @Override
    public void onBeaconsReceived(BluetoothLeIBeacon ibeacon) {

        for (IBeaconListener iBeaconListener : this.iBeaconListeners) {
            ChannelRecord record = ChannelRecord.createReadRecord(iBeaconListener.getChannelName(), DataType.STRING);

            record.setValue(TypedValues.newStringValue(iBeaconToString(ibeacon)));
            record.setChannelStatus(new ChannelStatus(SUCCESS));
            record.setTimestamp(System.currentTimeMillis());
            iBeaconListener.getListener().onChannelEvent(new ChannelEvent(record));
        }

    }

    private String iBeaconToString(BluetoothLeIBeacon iBeacon) {
        StringBuilder sb = new StringBuilder();
        sb.append(iBeacon.getUuid().toString());
        sb.append(";");
        sb.append((int) iBeacon.getTxPower());
        sb.append(";");
        sb.append(iBeacon.getRssi());
        sb.append(";");
        sb.append((int) iBeacon.getMajor());
        sb.append(";");
        sb.append((int) iBeacon.getMinor());
        sb.append(";");
        sb.append(calculateDistance(iBeacon.getRssi(), iBeacon.getTxPower()));

        return sb.toString();
    }

    private double calculateDistance(int rssi, int txpower) {

        int ratioDB = txpower - rssi;
        double ratioLinear = Math.pow(10, (double) ratioDB / 10);
        return Math.sqrt(ratioLinear);
    }

}
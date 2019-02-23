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
package org.eclipse.kura.internal.driver.eddystone;

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
import org.eclipse.kura.ble.eddystone.BluetoothLeEddystone;
import org.eclipse.kura.ble.eddystone.BluetoothLeEddystoneService;
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
 * The Class {@link EddystoneDriver} is an Eddystone Driver implementation for
 * Kura Asset-Driver Topology.
 * <br/>
 * <br/>
 * This Eddystone Driver can be used in cooperation with Kura Asset Model and in
 * isolation as well. In case of isolation, the properties needs to be provided
 * externally.
 * <br/>
 * <br/>
 * The required properties are enlisted in {@link EddystoneChannelDescriptor} and
 * the driver connection specific properties are enlisted in
 * {@link EddystoneOptions}
 *
 * @see Driver
 * @see EddystoneOptions
 * @see EddystoneChannelDescriptor
 *
 */
public final class EddystoneDriver
        implements Driver, ConfigurableComponent, BluetoothLeBeaconListener<BluetoothLeEddystone> {

    private static final Logger logger = LoggerFactory.getLogger(EddystoneDriver.class);

    private static final int SCAN_TIMEOUT = 600;
    private static final int MONITOR_TIMEOUT = 30;
    private static final String READ_ERROR_MESSAGE = "Read operation not supported";

    private ScheduledExecutorService worker;
    private ScheduledFuture<?> handle;

    private EddystoneOptions options;
    private BluetoothLeService bluetoothLeService;
    private BluetoothLeAdapter bluetoothLeAdapter;
    private BluetoothLeEddystoneService bluetoothLeEddystoneService;
    private BluetoothLeBeaconScanner<BluetoothLeEddystone> bluetoothLeEddystoneScanner;

    private Set<EddystoneListener> eddystoneListeners;

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

    public synchronized void bindBluetoothLeEddystoneService(BluetoothLeEddystoneService bluetoothLeEddystoneService) {
        if (isNull(this.bluetoothLeEddystoneService)) {
            this.bluetoothLeEddystoneService = bluetoothLeEddystoneService;
        }
    }

    public synchronized void unbindBluetoothLeEddystoneService(
            BluetoothLeEddystoneService bluetoothLeEddystoneService) {
        if (this.bluetoothLeEddystoneService == bluetoothLeEddystoneService) {
            this.bluetoothLeEddystoneService = null;
        }
    }

    protected synchronized void activate(final Map<String, Object> properties) {
        logger.debug("Activating Eddystone Driver...");
        this.eddystoneListeners = new HashSet<>();
        doUpdate(properties);
        logger.debug("Activating Eddystone Driver... Done");
    }

    protected synchronized void deactivate() {
        logger.debug("Deactivating Eddystoneg Driver...");
        doDeactivate();
        logger.debug("Deactivating Eddystone Driver... Done");
    }

    public synchronized void updated(final Map<String, Object> properties) {
        logger.debug("Updating Eddystone Driver...");
        doDeactivate();
        doUpdate(properties);
        logger.debug("Updating Eddystone Driver... Done");
    }

    private void doDeactivate() {
        this.eddystoneListeners.clear();
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
            this.bluetoothLeEddystoneScanner = this.bluetoothLeEddystoneService.newBeaconScanner(bluetoothLeAdapter);
            this.bluetoothLeEddystoneScanner.addBeaconListener(this);
            this.worker = Executors.newSingleThreadScheduledExecutor();
            // Setup a task that monitor the scan every 30 seconds
            this.handle = this.worker.scheduleAtFixedRate(this::monitor, 0, MONITOR_TIMEOUT, TimeUnit.SECONDS);
        } else {
            logger.warn("No Bluetooth adapter found ...");
        }
    }

    private void monitor() {
        if (!this.bluetoothLeEddystoneScanner.isScanning()) {
            try {
                // Perform a scan for 10 minutes
                this.bluetoothLeEddystoneScanner.startBeaconScan(SCAN_TIMEOUT);
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
        this.options = new EddystoneOptions(properties);
    }

    @Override
    public ChannelDescriptor getChannelDescriptor() {
        return new EddystoneChannelDescriptor();
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
        EddystoneFrameType frameType = EddystoneChannelDescriptor.getEddystoneType(channelConfig);
        EddystoneListener eddystoneListener = new EddystoneListener(channelName, listener, frameType);

        this.eddystoneListeners.add(eddystoneListener);
    }

    @Override
    public void unregisterChannelListener(final ChannelListener listener) throws ConnectionException {
        Iterator<EddystoneListener> iterator = this.eddystoneListeners.iterator();
        while (iterator.hasNext()) {
            EddystoneListener sensorListener = iterator.next();
            if (sensorListener.getListener().equals(listener)) {
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
        if (this.bluetoothLeEddystoneScanner != null) {
            if (this.bluetoothLeEddystoneScanner.isScanning()) {
                this.bluetoothLeEddystoneScanner.stopBeaconScan();
            }
            this.bluetoothLeEddystoneScanner.removeBeaconListener(this);
            this.bluetoothLeEddystoneService.deleteBeaconScanner(this.bluetoothLeEddystoneScanner);
        }
    }

    @Override
    public PreparedRead prepareRead(List<ChannelRecord> channelRecords) {
        logger.warn(READ_ERROR_MESSAGE);
        return null;
    }

    @Override
    public void onBeaconsReceived(BluetoothLeEddystone eddystone) {

        for (EddystoneListener eddystoneListener : this.eddystoneListeners) {
            if (eddystoneListener.getEddystoneFrameType()
                    .equals(EddystoneFrameType.valueOf(eddystone.getFrameType()))) {
                ChannelRecord record = ChannelRecord.createReadRecord(eddystoneListener.getChannelName(),
                        DataType.STRING);

                record.setValue(TypedValues.newStringValue(eddystoneToString(eddystone)));
                record.setChannelStatus(new ChannelStatus(SUCCESS));
                record.setTimestamp(System.currentTimeMillis());
                eddystoneListener.getListener().onChannelEvent(new ChannelEvent(record));
            }
        }

    }

    private String eddystoneToString(BluetoothLeEddystone eddystone) {
        StringBuilder sb = new StringBuilder();
        sb.append(eddystone.getFrameType());
        sb.append(";");
        if ("UID".equals(eddystone.getFrameType())) {
            sb.append(bytesArrayToHexString(eddystone.getNamespace()));
            sb.append(";");
            sb.append(bytesArrayToHexString(eddystone.getInstance()));
            sb.append(";");
        } else if ("URL".equals(eddystone.getFrameType())) {
            sb.append(eddystone.getUrl());
            sb.append(";");
        }
        sb.append((int) eddystone.getTxPower());
        sb.append(";");
        sb.append(eddystone.getRssi());
        sb.append(";");
        sb.append(calculateDistance(eddystone.getRssi(), eddystone.getTxPower()));

        return sb.toString();
    }

    private double calculateDistance(int rssi, int txpower) {

        int ratioDB = txpower - rssi;
        double ratioLinear = Math.pow(10, (double) ratioDB / 10);
        return Math.sqrt(ratioLinear);
    }

    private static String bytesArrayToHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

}

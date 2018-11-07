/*******************************************************************************
 * Copyright (c) 2017, 2018 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.internal.ble;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.kura.KuraBluetoothDiscoveryException;
import org.eclipse.kura.KuraBluetoothRemoveException;
import org.eclipse.kura.bluetooth.le.BluetoothLeAdapter;
import org.eclipse.kura.bluetooth.le.BluetoothLeDevice;
import org.eclipse.kura.bluetooth.le.BluetoothTransportType;

import tinyb.BluetoothDevice;
import tinyb.BluetoothException;
import tinyb.TransportType;

public class BluetoothLeAdapterImpl implements BluetoothLeAdapter {

    private static final Logger logger = LogManager.getLogger(BluetoothLeAdapterImpl.class);
    private static final long TIMEOUT = 30;

    private final tinyb.BluetoothAdapter adapter;

    public BluetoothLeAdapterImpl(tinyb.BluetoothAdapter adapter) {
        this.adapter = adapter;
    }

    @Override
    public String getAddress() {
        return this.adapter.getAddress();
    }

    @Override
    public String getName() {
        return this.adapter.getName();
    }

    @Override
    public String getInterfaceName() {
        return this.adapter.getInterfaceName();
    }

    @Override
    public String getModalias() {
        return this.adapter.getModalias();
    }

    @Override
    public String getAlias() {
        return this.adapter.getAlias();
    }

    @Override
    public void setAlias(String value) {
        this.adapter.setAlias(value);
    }

    @Override
    public long getBluetoothClass() {
        return this.adapter.getBluetoothClass();
    }

    @Override
    public boolean isPowered() {
        return this.adapter.getPowered();
    }

    @Override
    public void setPowered(boolean value) {
        this.adapter.setPowered(value);
    }

    @Override
    public boolean isDiscoverable() {
        return this.adapter.getDiscoverable();
    }

    @Override
    public void setDiscoverable(boolean value) {
        this.adapter.setDiscoverable(value);
    }

    @Override
    public long getDiscoverableTimeout() {
        return this.adapter.getDiscoverableTimeout();
    }

    @Override
    public void setDiscoverableTimout(long value) {
        setDiscoverableTimout(value);
    }

    @Override
    public boolean isPairable() {
        return this.adapter.getPairable();
    }

    @Override
    public void setPairable(boolean value) {
        this.adapter.setPairable(value);
    }

    @Override
    public long getPairableTimeout() {
        return this.adapter.getPairableTimeout();
    }

    @Override
    public void setPairableTimeout(long value) {
        this.adapter.setPairableTimeout(value);
    }

    @Override
    public boolean isDiscovering() {
        return this.adapter.getDiscovering();
    }

    @Override
    public UUID[] getUUIDs() {
        List<UUID> uuidList = new ArrayList<>();
        for (String uuid : this.adapter.getUUIDs()) {
            uuidList.add(UUID.fromString(uuid));
        }
        UUID[] uuids = new UUID[uuidList.size()];
        return uuidList.toArray(uuids);
    }

    @Override
    public void stopDiscovery() throws KuraBluetoothDiscoveryException {
        try {
            this.adapter.stopDiscovery();
        } catch (BluetoothException e) {
            throw new KuraBluetoothDiscoveryException(e, "Stop discovery failed");
        }
    }

    @Override
    public Future<BluetoothLeDevice> findDeviceByAddress(long timeout, String address) {
        return new BluetoothFuture<>(timeout, null, address);
    }

    @Override
    public Future<BluetoothLeDevice> findDeviceByName(long timeout, String name) {
        return new BluetoothFuture<>(timeout, name, null);
    }

    @Override
    public void findDeviceByAddress(long timeout, String address, Consumer<BluetoothLeDevice> consumer) {
        BluetoothFuture<BluetoothLeDevice> future = new BluetoothFuture<>(timeout, null, address);
        future.setConsumer(consumer);
    }

    @Override
    public void findDeviceByName(long timeout, String name, Consumer<BluetoothLeDevice> consumer) {
        BluetoothFuture<BluetoothLeDevice> future = new BluetoothFuture<>(timeout, name, null);
        future.setConsumer(consumer);
    }

    @Override
    public Future<List<BluetoothLeDevice>> findDevices(long timeout) {
        return new BluetoothFuture<>(timeout);
    }

    @Override
    public void findDevices(long timeout, Consumer<List<BluetoothLeDevice>> consumer) {
        BluetoothFuture<List<BluetoothLeDevice>> future = new BluetoothFuture<>(timeout, null, null);
        future.setConsumer(consumer);
    }

    public class BluetoothFuture<T> extends CompletableFuture<T> implements Runnable {

        private final long internalTimeout;
        private String name;
        private String address;
        private Consumer<T> consumer;

        public BluetoothFuture(long timeout) {
            super();
            this.internalTimeout = timeout * 1000;
            new Thread(this).start();
        }

        public BluetoothFuture(long timeout, String name, String address) {
            super();
            this.internalTimeout = timeout * 1000;
            this.name = name;
            this.address = address;
            new Thread(this).start();
        }

        public void setConsumer(Consumer<T> consumer) {
            this.consumer = consumer;
        }

        @Override
        public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            logger.info("The timeout value provided by get(...) method will be ignored");
            return this.get();
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            try {
                BluetoothLeAdapterImpl.this.adapter.stopDiscovery();
            } catch (BluetoothException e) {
                logger.error("Stop discovery failed", e);
                return false;
            }
            return super.cancel(mayInterruptIfRunning);
        }

        @SuppressWarnings("unchecked")
        @Override
        public void run() {
            if (BluetoothLeAdapterImpl.this.adapter.getDiscovering()) {
                completeExceptionally(
                        new KuraBluetoothDiscoveryException("The BLE adapter has already been discovering..."));
            }
            try {
                BluetoothLeAdapterImpl.this.adapter.startDiscovery();
            } catch (BluetoothException e) {
                logger.error("Start discovery failed", e);
            }
            waitForStop();
            try {
                if (this.name != null || this.address != null) {
                    BluetoothDevice leDevice = BluetoothLeAdapterImpl.this.adapter.find(this.name, this.address,
                            Duration.ofSeconds(TIMEOUT));
                    if (leDevice == null || leDevice.getRSSI() == 0) {
                        complete((T) null);
                    } else {
                        complete((T) new BluetoothLeDeviceImpl(leDevice));
                    }
                    if (leDevice != null && this.consumer != null) {
                        this.consumer.accept((T) new BluetoothLeDeviceImpl(leDevice));
                    }
                } else {
                    List<BluetoothLeDevice> devices = BluetoothLeAdapterImpl.this.adapter.getDevices().stream()
                            .filter(device -> device.getRSSI() != 0).map(BluetoothLeDeviceImpl::new)
                            .collect(Collectors.toList());
                    complete((T) devices);
                    if (this.consumer != null) {
                        this.consumer.accept((T) devices);
                    }
                }

                BluetoothLeAdapterImpl.this.adapter.stopDiscovery();
            } catch (BluetoothException e) {
                logger.error("Stop discovery failed", e);
            }
        }

        private void waitForStop() {
            long start = System.currentTimeMillis();
            while (System.currentTimeMillis() - start <= this.internalTimeout) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    completeExceptionally(e);
                }
            }
        }
    }

    @Override
    public int removeDevices() throws KuraBluetoothRemoveException {
        int removedDevices = 0;
        try {
            removedDevices = this.adapter.removeDevices();
        } catch (BluetoothException e) {
            throw new KuraBluetoothRemoveException(e, "Failed to remove devices");
        }
        return removedDevices;
    }

    @Override
    public void setDiscoveryFilter(List<UUID> uuids, int rssi, int pathloss, BluetoothTransportType transportType) {
        this.adapter.setDiscoveryFilter(uuids, rssi, pathloss, toTransportType(transportType));
    }

    @Override
    public void setRssiDiscoveryFilter(int rssi) {
        this.adapter.setRssiDiscoveryFilter(rssi);
    }

    private TransportType toTransportType(BluetoothTransportType type) {
        switch (type) {
        case AUTO:
            return TransportType.AUTO;
        case BREDR:
            return TransportType.BREDR;
        case LE:
            return TransportType.LE;
        default:
            return TransportType.AUTO;
        }
    }
}

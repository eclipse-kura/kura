/*******************************************************************************
 * Copyright (c) 2017, 2021 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.internal.ble;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.kura.KuraBluetoothDiscoveryException;
import org.eclipse.kura.KuraBluetoothRemoveException;
import org.eclipse.kura.bluetooth.le.BluetoothLeAdapter;
import org.eclipse.kura.bluetooth.le.BluetoothLeDevice;
import org.eclipse.kura.bluetooth.le.BluetoothTransportType;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.DBusExecutionException;
import org.freedesktop.dbus.types.UInt16;
import org.freedesktop.dbus.types.Variant;

import com.github.hypfvieh.bluetooth.DeviceManager;
import com.github.hypfvieh.bluetooth.DiscoveryTransport;
import com.github.hypfvieh.bluetooth.wrapper.BluetoothAdapter;
import com.github.hypfvieh.bluetooth.wrapper.BluetoothDevice;

public class BluetoothLeAdapterImpl implements BluetoothLeAdapter {

    private static final Logger logger = LogManager.getLogger(BluetoothLeAdapterImpl.class);
    private static final String STOP_DISCOVERY_FAILED = "Stop discovery failed";
    private static final String START_DISCOVERY_FAILED = "Start discovery failed";

    private final BluetoothAdapter adapter;

    public BluetoothLeAdapterImpl(BluetoothAdapter adapter) {
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
        return this.adapter.getDeviceName();
    }

    @Override
    public String getModalias() {
        return this.adapter.getModAlias();
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
        Integer deviceClass = this.adapter.getDeviceClass();
        return deviceClass == null ? -1 : deviceClass;
    }

    @Override
    public boolean isPowered() {
        return this.adapter.isPowered();
    }

    @Override
    public void setPowered(boolean value) {
        this.adapter.setPowered(value);
    }

    @Override
    public boolean isDiscoverable() {
        Boolean discoverable = this.adapter.isDiscoverable();
        if (discoverable != null) {
            return discoverable;
        }
        return false;
    }

    @Override
    public void setDiscoverable(boolean value) {
        this.adapter.setDiscoverable(value);
    }

    @Override
    public long getDiscoverableTimeout() {
        Integer timeout = this.adapter.getDiscoverableTimeout();
        return timeout == null ? -1 : timeout;
    }

    @Override
    public void setDiscoverableTimout(long value) {
        this.setDiscoverableTimeout(value);
    }

    @Override
    public void setDiscoverableTimeout(long value) {
        this.adapter.setDiscoverableTimeout((int) value);
    }

    @Override
    public boolean isPairable() {
        Boolean pairable = this.adapter.isPairable();
        if (pairable != null) {
            return pairable;
        }
        return false;
    }

    @Override
    public void setPairable(boolean value) {
        this.adapter.setPairable(value);
    }

    @Override
    public long getPairableTimeout() {
        Integer timeout = this.adapter.getPairableTimeout();
        return timeout == null ? -1 : timeout;
    }

    @Override
    public void setPairableTimeout(long value) {
        this.adapter.setPairableTimeout((int) value);
    }

    @Override
    public boolean isDiscovering() {
        return this.adapter.isDiscovering();
    }

    @Override
    public UUID[] getUUIDs() {
        List<UUID> uuidList = new ArrayList<>();
        String[] strUuids = this.adapter.getUuids();
        if (strUuids != null) {
            for (String uuid : strUuids) {
                uuidList.add(UUID.fromString(uuid));
            }
        }
        UUID[] uuids = new UUID[uuidList.size()];
        return uuidList.toArray(uuids);
    }

    @Override
    public void startDiscovery() throws KuraBluetoothDiscoveryException {
        try {
            if (!this.adapter.startDiscovery()) {
                throw new KuraBluetoothDiscoveryException(START_DISCOVERY_FAILED);
            }
        } catch (DBusExecutionException ex) {
            throw new KuraBluetoothDiscoveryException(START_DISCOVERY_FAILED);
        }
    }

    @Override
    public void stopDiscovery() throws KuraBluetoothDiscoveryException {
        try {
            if (!this.adapter.stopDiscovery()) {
                throw new KuraBluetoothDiscoveryException(STOP_DISCOVERY_FAILED);
            }
        } catch (DBusExecutionException ex) {
            throw new KuraBluetoothDiscoveryException(STOP_DISCOVERY_FAILED);
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
                if (!BluetoothLeAdapterImpl.this.adapter.stopDiscovery()) {
                    logger.error(STOP_DISCOVERY_FAILED);
                    return false;
                }
            } catch (DBusExecutionException ex) {
                logger.error(STOP_DISCOVERY_FAILED, ex);
                return false;
            }

            return super.cancel(mayInterruptIfRunning);
        }

        @SuppressWarnings("unchecked")
        @Override
        public void run() {
            if (BluetoothLeAdapterImpl.this.isDiscovering()) {
                completeExceptionally(new KuraBluetoothDiscoveryException("The BLE adapter is already discovering..."));
            }
            try {
                if (!BluetoothLeAdapterImpl.this.adapter.startDiscovery()) {
                    logger.error(START_DISCOVERY_FAILED);
                }
            } catch (DBusExecutionException ex) {
                logger.error(START_DISCOVERY_FAILED, ex);
            }
            waitForStop();
            List<BluetoothDevice> devices = DeviceManager.getInstance()
                    .getDevices(BluetoothLeAdapterImpl.this.adapter.getAddress(), true);
            if (devices.isEmpty()) {
                complete((T) null);
            }
            if (this.name != null || this.address != null) {
                getDevice(devices);
            } else {
                List<BluetoothLeDevice> leDevices = new ArrayList<>();
                for (BluetoothDevice device : devices) {
                    leDevices.add(new BluetoothLeDeviceImpl(device));
                }
                complete((T) leDevices);
                if (this.consumer != null) {
                    this.consumer.accept((T) leDevices);
                }
            }
            try {
                if (!BluetoothLeAdapterImpl.this.adapter.stopDiscovery()) {
                    logger.error(STOP_DISCOVERY_FAILED);
                }
            } catch (DBusExecutionException ex) {
                logger.error(STOP_DISCOVERY_FAILED, ex);
            }
        }

        @SuppressWarnings("unchecked")
        private void getDevice(List<BluetoothDevice> devices) {
            BluetoothDevice leDevice = null;
            for (BluetoothDevice device : devices) {
                if (this.address != null && device.getAddress().equals(this.address)
                        || this.name != null && device.getName().equals(this.name)) {
                    leDevice = device;
                    break;
                }
            }
            if (leDevice == null) {
                complete((T) null);
            } else {
                complete((T) new BluetoothLeDeviceImpl(leDevice));
            }
            if (leDevice != null && this.consumer != null) {
                this.consumer.accept((T) new BluetoothLeDeviceImpl(leDevice));
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
            List<BluetoothDevice> devices = DeviceManager.getInstance().getDevices(this.adapter.getAddress(), true);
            for (BluetoothDevice device : devices) {
                this.adapter.removeDevice(device.getRawDevice());
                removedDevices++;
            }
        } catch (DBusException e) {
            throw new KuraBluetoothRemoveException(e, "Failed to remove devices");
        }
        return removedDevices;
    }

    @Override
    public void setDiscoveryFilter(List<UUID> uuids, int rssi, int pathloss, BluetoothTransportType transportType) {
        this.setDiscoveryFilter(uuids, rssi, pathloss, transportType, false);
    }

    @Override
    public void setDiscoveryFilter(List<UUID> uuids, int rssi, int pathloss, BluetoothTransportType transportType,
            boolean duplicateData) {
        Map<String, Variant<?>> filter = new LinkedHashMap<>();

        // UUIDs
        if (uuids != null && !uuids.isEmpty()) {
            String[] strUuids = new String[uuids.size()];
            for (int i = 0; i < uuids.size(); i++) {
                strUuids[i] = uuids.get(i).toString();
            }
            filter.put("UUIDs", new Variant<>(strUuids));
        }

        // RSSI & Pathloss
        if (rssi != 0) {
            filter.put("RSSI", new Variant<>((short) rssi));
        } else if (pathloss != 0) {
            filter.put("Pathloss", new Variant<>(new UInt16(pathloss)));
        }

        // Transport Type
        filter.put("Transport", new Variant<>(toDiscoveryTransport(transportType).toString()));

        // Duplicate Data
        filter.put("DuplicateData", new Variant<>(Boolean.valueOf(duplicateData)));

        try {
            this.adapter.setDiscoveryFilter(filter);
        } catch (DBusException | DBusExecutionException e) {
            logger.error("Failed to set discovery filter", e);
        }
    }

    @Override
    public void setRssiDiscoveryFilter(int rssi) {
        setDiscoveryFilter(null, rssi, 0, BluetoothTransportType.AUTO, false);
    }

    private DiscoveryTransport toDiscoveryTransport(BluetoothTransportType type) {
        switch (type) {
        case AUTO:
            return DiscoveryTransport.AUTO;
        case BREDR:
            return DiscoveryTransport.BREDR;
        case LE:
            return DiscoveryTransport.LE;
        default:
            return DiscoveryTransport.AUTO;
        }
    }
}

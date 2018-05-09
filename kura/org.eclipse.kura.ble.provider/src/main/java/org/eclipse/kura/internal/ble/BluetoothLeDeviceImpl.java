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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.eclipse.kura.KuraBluetoothConnectionException;
import org.eclipse.kura.KuraBluetoothPairException;
import org.eclipse.kura.KuraBluetoothRemoveException;
import org.eclipse.kura.KuraBluetoothResourceNotFoundException;
import org.eclipse.kura.bluetooth.le.BluetoothLeAdapter;
import org.eclipse.kura.bluetooth.le.BluetoothLeDevice;
import org.eclipse.kura.bluetooth.le.BluetoothLeGattService;

import tinyb.BluetoothDevice;
import tinyb.BluetoothException;
import tinyb.BluetoothGattService;

public class BluetoothLeDeviceImpl implements BluetoothLeDevice {

    private static final long TIMEOUT = 30;
    private final BluetoothDevice device;

    public BluetoothLeDeviceImpl(tinyb.BluetoothDevice device) {
        this.device = device;
    }

    @Override
    public BluetoothLeGattService findService(UUID uuid) throws KuraBluetoothResourceNotFoundException {
        return findService(uuid, BluetoothLeDeviceImpl.TIMEOUT);
    }

    @Override
    public BluetoothLeGattService findService(UUID uuid, long timeout) throws KuraBluetoothResourceNotFoundException {
        BluetoothGattService service = this.device.find(uuid.toString(), Duration.ofSeconds(timeout));
        if (service != null) {
            return new BluetoothLeGattServiceImpl(service);
        } else {
            throw new KuraBluetoothResourceNotFoundException("Gatt service not found.");
        }
    }

    @Override
    public List<BluetoothLeGattService> findServices() throws KuraBluetoothResourceNotFoundException {
        List<BluetoothGattService> tinybServices = this.device.getServices();
        List<BluetoothLeGattService> services = new ArrayList<>();
        if (tinybServices != null) {
            for (BluetoothGattService service : tinybServices) {
                services.add(new BluetoothLeGattServiceImpl(service));
            }
        } else {
            throw new KuraBluetoothResourceNotFoundException("Gatt services not found.");
        }
        return services;
    }

    @Override
    public void disconnect() throws KuraBluetoothConnectionException {
        try {
            this.device.disconnect();
        } catch (BluetoothException e) {
            throw new KuraBluetoothConnectionException(e, "Disconnection from device failed");
        }
    }

    @Override
    public void connect() throws KuraBluetoothConnectionException {
        try {
            this.device.connect();
        } catch (BluetoothException e) {
            throw new KuraBluetoothConnectionException(e, "Connection to device failed");
        }
    }

    @Override
    public void connectProfile(UUID uuid) throws KuraBluetoothConnectionException {
        try {
            this.device.connectProfile(uuid.toString());
        } catch (BluetoothException e) {
            throw new KuraBluetoothConnectionException(e, "Connection to profile failed");
        }
    }

    @Override
    public void disconnectProfile(UUID uuid) throws KuraBluetoothConnectionException {
        try {
            this.device.disconnectProfile(uuid.toString());
        } catch (BluetoothException e) {
            throw new KuraBluetoothConnectionException(e, "Disconnection from profile failed");
        }
    }

    @Override
    public void pair() throws KuraBluetoothPairException {
        try {
            this.device.pair();
        } catch (BluetoothException e) {
            throw new KuraBluetoothPairException(e, "Pairing failed");
        }
    }

    @Override
    public void cancelPairing() throws KuraBluetoothPairException {
        try {
            this.device.cancelPairing();
        } catch (BluetoothException e) {
            throw new KuraBluetoothPairException(e, "Cancel pairing failed");
        }
    }

    @Override
    public String getAddress() {
        return this.device.getAddress();
    }

    @Override
    public String getName() {
        return this.device.getName();
    }

    @Override
    public String getAlias() {
        return this.device.getAlias();
    }

    @Override
    public void setAlias(String value) {
        this.device.setAlias(value);
    }

    @Override
    public int getBluetoothClass() {
        return this.device.getBluetoothClass();
    }

    @Override
    public short getAppearance() {
        return this.device.getAppearance();
    }

    @Override
    public String getIcon() {
        return this.device.getIcon();
    }

    @Override
    public boolean isPaired() {
        return this.device.getPaired();
    }

    @Override
    public boolean isTrusted() {
        return this.device.getTrusted();
    }

    @Override
    public void setTrusted(boolean value) {
        this.device.setTrusted(value);
    }

    @Override
    public boolean isBlocked() {
        return this.device.getBlocked();
    }

    @Override
    public void setBlocked(boolean value) {
        this.device.setBlocked(value);
    }

    @Override
    public boolean isLegacyPairing() {
        return this.device.getLegacyPairing();
    }

    @Override
    public short getRSSI() {
        return this.device.getRSSI();
    }

    @Override
    public boolean isConnected() {
        return this.device.getConnected();
    }

    @Override
    public UUID[] getUUIDs() {
        List<UUID> uuidList = new ArrayList<>();
        for (String uuid : this.device.getUUIDs()) {
            uuidList.add(UUID.fromString(uuid));
        }
        UUID[] uuids = new UUID[uuidList.size()];
        return uuidList.toArray(uuids);
    }

    @Override
    public String getModalias() {
        return this.device.getModalias();
    }

    @Override
    public BluetoothLeAdapter getAdapter() {
        return new BluetoothLeAdapterImpl(this.device.getAdapter());
    }

    @Override
    public Map<Short, byte[]> getManufacturerData() {
        return this.device.getManufacturerData();
    }

    @Override
    public Map<UUID, byte[]> getServiceData() {
        Map<UUID, byte[]> serviceData = new HashMap<>();
        for (Entry<String, byte[]> entry : this.device.getServiceData().entrySet()) {
            serviceData.put(UUID.fromString(entry.getKey()), entry.getValue());
        }
        return serviceData;
    }

    @Override
    public short getTxPower() {
        return this.device.getTxPower();
    }

    @Override
    public boolean isServicesResolved() {
        return this.device.getServicesResolved();
    }

    @Override
    public boolean remove() throws KuraBluetoothRemoveException {
        boolean result = false;
        try {
            result = this.device.remove();
        } catch (BluetoothException e) {
            throw new KuraBluetoothRemoveException(e, "Failed to remove the device");
        }
        return result;
    }

}

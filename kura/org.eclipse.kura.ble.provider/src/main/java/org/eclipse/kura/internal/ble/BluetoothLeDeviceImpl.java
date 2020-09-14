/*******************************************************************************
 * Copyright (c) 2017, 2020 Eurotech and/or its affiliates and others
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
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.types.UInt16;

import com.github.hypfvieh.bluetooth.wrapper.BluetoothDevice;
import com.github.hypfvieh.bluetooth.wrapper.BluetoothGattService;

public class BluetoothLeDeviceImpl implements BluetoothLeDevice {

    private final BluetoothDevice device;

    public BluetoothLeDeviceImpl(BluetoothDevice device) {
        this.device = device;
    }

    @Override
    public BluetoothLeGattService findService(UUID uuid) throws KuraBluetoothResourceNotFoundException {
        BluetoothGattService service = this.device.getGattServiceByUuid(uuid.toString());
        if (service != null) {
            return new BluetoothLeGattServiceImpl(service);
        } else {
            throw new KuraBluetoothResourceNotFoundException("Gatt service not found.");
        }
    }

    @Override
    public BluetoothLeGattService findService(UUID uuid, long timeout) throws KuraBluetoothResourceNotFoundException {
        return this.findService(uuid);
    }

    @Override
    public List<BluetoothLeGattService> findServices() throws KuraBluetoothResourceNotFoundException {
        List<BluetoothGattService> serviceList = this.device.getGattServices();
        List<BluetoothLeGattService> services = new ArrayList<>();
        if (serviceList != null) {
            for (BluetoothGattService service : serviceList) {
                services.add(new BluetoothLeGattServiceImpl(service));
            }
        } else {
            throw new KuraBluetoothResourceNotFoundException("Gatt services not found.");
        }
        return services;
    }

    @Override
    public void disconnect() throws KuraBluetoothConnectionException {
        if (!this.device.disconnect()) {
            throw new KuraBluetoothConnectionException("Disconnection from device failed");
        }
    }

    @Override
    public void connect() throws KuraBluetoothConnectionException {
        if (!this.device.connect()) {
            throw new KuraBluetoothConnectionException("Connection to device failed");
        }
    }

    @Override
    public void connectProfile(UUID uuid) throws KuraBluetoothConnectionException {
        if (!this.device.connectProfile(uuid.toString())) {
            throw new KuraBluetoothConnectionException("Connection to profile failed");
        }
    }

    @Override
    public void disconnectProfile(UUID uuid) throws KuraBluetoothConnectionException {
        if (!this.device.disconnectProfile(uuid.toString())) {
            throw new KuraBluetoothConnectionException("Disconnection from profile failed");
        }
    }

    @Override
    public void pair() throws KuraBluetoothPairException {
        if (!this.device.pair()) {
            throw new KuraBluetoothPairException("Pairing failed");
        }
    }

    @Override
    public void cancelPairing() throws KuraBluetoothPairException {
        if (!this.device.cancelPairing()) {
            throw new KuraBluetoothPairException("Cancel pairing failed");
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
        Integer btClass = this.device.getBluetoothClass();
        return btClass == null ? -1 : btClass;
    }

    @Override
    public short getAppearance() {
        Integer appearance = this.device.getAppearance();
        return appearance == null ? -1 : appearance.shortValue();
    }

    @Override
    public String getIcon() {
        return this.device.getIcon();
    }

    @Override
    public boolean isPaired() {
        Boolean paired = this.device.isPaired();
        return paired == null ? false : paired;
    }

    @Override
    public boolean isTrusted() {
        Boolean trusted = this.device.isTrusted();
        return trusted == null ? false : trusted;
    }

    @Override
    public void setTrusted(boolean value) {
        this.device.setTrusted(value);
    }

    @Override
    public boolean isBlocked() {
        Boolean blocked = this.device.isBlocked();
        return blocked == null ? false : blocked;
    }

    @Override
    public void setBlocked(boolean value) {
        this.device.setBlocked(value);
    }

    @Override
    public boolean isLegacyPairing() {
        Boolean legacyPairing = this.device.isLegacyPairing();
        return legacyPairing == null ? false : legacyPairing;
    }

    @Override
    public short getRSSI() {
        Short rssi = this.device.getRssi();
        return rssi == null ? 0 : rssi;
    }

    @Override
    public boolean isConnected() {
        Boolean connected = this.device.isConnected();
        return connected == null ? false : connected;
    }

    @Override
    public UUID[] getUUIDs() {
        List<UUID> uuidList = new ArrayList<>();
        String[] strUuids = this.device.getUuids();
        if (strUuids != null) {
            for (String uuid : strUuids) {
                uuidList.add(UUID.fromString(uuid));
            }
        }
        UUID[] uuids = new UUID[uuidList.size()];
        return uuidList.toArray(uuids);
    }

    @Override
    public String getModalias() {
        return this.device.getModAlias();
    }

    @Override
    public BluetoothLeAdapter getAdapter() {
        return new BluetoothLeAdapterImpl(this.device.getAdapter());
    }

    @Override
    public Map<Short, byte[]> getManufacturerData() {
        Map<Short, byte[]> manufacturerData = new HashMap<>();
        Map<UInt16, byte[]> originalData = this.device.getManufacturerData();
        if (originalData != null) {
            for (Entry<UInt16, byte[]> entry : originalData.entrySet()) {
                manufacturerData.put(entry.getKey().shortValue(), entry.getValue());
            }
        }
        return manufacturerData;
    }

    @Override
    public Map<UUID, byte[]> getServiceData() {
        Map<UUID, byte[]> serviceData = new HashMap<>();
        Map<String, byte[]> originalData = this.device.getServiceData();
        if (originalData != null) {
            for (Entry<String, byte[]> entry : originalData.entrySet()) {
                serviceData.put(UUID.fromString(entry.getKey()), entry.getValue());
            }
        }
        return serviceData;
    }

    @Override
    public short getTxPower() {
        Short txPower = this.device.getTxPower();
        return txPower == null ? 0 : this.device.getTxPower();
    }

    @Override
    public boolean isServicesResolved() {
        Boolean servicesResolved = this.device.isServicesResolved();
        return servicesResolved == null ? false : servicesResolved;
    }

    @Override
    public void remove() throws KuraBluetoothRemoveException {
        try {
            this.device.getAdapter().removeDevice(this.device.getRawDevice());
        } catch (DBusException e) {
            throw new KuraBluetoothRemoveException(e, "Failed to remove the device");
        }
    }

}

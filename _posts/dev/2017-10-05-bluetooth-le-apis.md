---
layout: page
title:  "Kura Bluetooth LE APIs"
categories: [dev]
---

[Overview](#overview)

[Bluez-Dbus - BLE GATT API](#bluez-dbus---ble-gatt-api)

[APIs description](#apis-description)

[How to use the Kura BLE API](#how-to-use-the-kura-ble-api)

[Configure Bluez on the Raspberry Pi](#configure-bluez-on-the-raspberry-pi)

## Overview

Starting from version 3.1.0, Eclipse Kura implements a new set of APIs for managing Bluetooth Low Energy and Beacon devices. The new APIs replace the existing Bluetooth APIs, but the old ones are still available and can be used. So, the applications developed before Kura 3.1.0 continue to work.

The purpose of the new BLE APIs is to simplify the development of applications that interact with Bluetooth LE devices, offering clear and easy-to-use methods, and add new features to correctly manage the connection with remote devices.
Moreover, the APIs organize the methods in a logical way to access all levels of a GATT client, from GATT services to GATT characteristics and descriptors, using UUIDs to identify the correct resource.

## Bluez-Dbus - BLE GATT API

The implementation of the new Kura BLE APIs is based on the <a href="https://github.com/hypfvieh/bluez-dbus" about="_blank">Bluez-Dbus</a> library that provides an easy to use Bluetooth LE API based on BlueZ over DBus. The library eases the access to GATT services and the management of BLE connections and discovery, without using any wrapper library as it is based on a newer version of dbus-java which uses jnr-unixsocket.

## APIs description

The new BLE APIs are exported in the **org.eclipse.kura.bluetooth.le** package. The interfaces are briefly described in the following.

* **BluetoothLeService** is the entry point of the OSGI service. It allows to get all the Bluetooth interfaces installed on the gateway or a specific one using the name of the adapter.
* **BluetoothLeAdapter** represents the physical Bluetooth adapter on the gateway. It allows to start/stop a discovery, search a specific BLE device based on the BD address, power up/down the adapter and get information about the adapter.
* **BluetoothLeDevice** represents a Bluetooth LE device. The interface provides methods for connections and disconnections, list the GATT services or search a specific one based on the UUID and get generic information about the device.
* **BluetoothLeGattService** represents a GATT service and allows listing the GATT characteristics provided by the device.
* **BluetoothLeGattCharacteristic** represents a GATT characteristic. It provides methods to read from and write to the characteristic, enable or disable notifications and get the properties.
* **BluetoothLeGattDescriptor** represents a GATT descriptor associated with the characteristic.

More information about the APIs can be found in [API Reference](../ref/api-ref.html).

## How to use the Kura BLE API

This section briefly presents how to use the Kura BLE APIs, providing several code snippets to explain how to perform common bluetooth operations. For a complete example, please refer to the new <a href="https://github.com/eclipse/kura/tree/develop/kura/examples/org.eclipse.kura.example.ble.tisensortag.dbus" about="_blank">SensorTag application</a>.

An application that wants to use the Kura BLE APIs should bind the **BluetoothLeService** OSGI service, as shown in the following Java snippet:

```
public void setBluetoothLeService(BluetoothLeService bluetoothLeService) {
    this.bluetoothLeService = bluetoothLeService;
}

public void unsetBluetoothLeService(BluetoothLeService bluetoothLeService) {
    this.bluetoothLeService = null;
}
```

and in the component definition:

```
<reference bind="setBluetoothLeService" 
            cardinality="1..1" 
            interface="org.eclipse.kura.bluetooth.le.BluetoothLeService" 
            name="BluetoothLeService" 
            policy="static" 
            unbind="unsetBluetoothLeService"/>
```

### Get the Bluetooth adapter

Once bound to the **BluetoothLeService**, an application can get the Bluetooth adapter and power on it, if needed:

````
this.bluetoothLeAdapter = this.bluetoothLeService.getAdapter(adapterName);
if (this.bluetoothLeAdapter != null) {
    if (!this.bluetoothLeAdapter.isPowered()) {
        this.bluetoothLeAdapter.setPowered(true);
    }
} 
````

where **adapterName** is the name of the adapter, i.e. hci0.

### Search for BLE devices

The **BluetoothLeAdapter** provides several methods to search for a device, a.k.a. perform a BLE discovery:

* **Future\<BluetoothLeDevice\> findDeviceByAddress(long timeout, String address)** search for a BLE device with the specified address. The method will perform a BLE discovery for at most timeout seconds or until the device is found. It will return a Future instance and the discovered device can be retrieved using the get() method. 
* **Future\<BluetoothLeDevice\> findDeviceByName(long timeout, String name)** search for a BLE device with the specified system name and return a Future.
* **void findDeviceByAddress(long timeout, String address, Consumer\<BluetoothLeDevice\> consumer)** search for a BLE device with the specified address. The method will perform a BLE discovery for at most timeout seconds or until the device is found. When the device is found or the timeout is reached the consumer is used to get the device.
* **void findDeviceByAddress(long timeout, String address, Consumer\<BluetoothLeDevice\> consumer)** search for a BLE device with the specified name and use the provided consumer to return the device.
* **Future\<List\<BluetoothLeDevice\>\> findDevices(long timeout)** and **void findDevices(long timeout, Consumer\<List\<BluetoothLeDevice\>\> consumer)** are similar to the methods above, but they get a list of Bluetooth devices.

The following snippet shows how to perform a discovery of 10 seconds using **findDevices** method:

```
if (this.bluetoothLeAdapter.isDiscovering()) {
    try {
        this.bluetoothLeAdapter.stopDiscovery();
    } catch (KuraException e) {
        logger.error("Failed to stop discovery", e);
    }
}
Future<List<BluetoothLeDevice>> future = this.bluetoothLeAdapter.findDevices(10);
try {
    List<BluetoothLeDevice> devices = future.get();
} catch (InterruptedException | ExecutionException e) {
    logger.error("Scan for devices failed", e);
}
```

### Get the GATT services and characteristics

To get the GATT services using the **BluetoothLeDevice**, use the following snippet:

```
try {
    List<BluetoothLeGattService> services = device.findServices();
} catch (KuraBluetoothResourceNotFoundException e) {
    logger.error("Unable to find GATT services", e);
}
```

A specific GATT service can be retrieved using its UUID:

```
try {
    BluetoothLeGattService service = device.findService(uuid);
} catch (KuraBluetoothResourceNotFoundException e) {
    logger.error("Unable to find GATT service", e);
}
```

Using the GATT service, it is possible to get a specific GATT characteristic (or the complete list) and the GATT descriptor from it:

```
try {
    BluetoothLeGattCharacteristic characteristic = service.findCharacteristic(characteristicUuid);
    BluetoothLeGattDescriptor descriptor = characteristic.findDescriptor(descriptorUuid);
} catch (KuraBluetoothResourceNotFoundException e) {
    logger.error("Unable to find GATT resources", e);
}
```

### IO operations on GATT characteristics and descriptors

The Kura BLE APIs provides methods to manage the IO operations on GATT characteristics and descriptors. The following snippet provides an example on how to read and write data to a characteristic.

```
try {
    byte[] valueRead = characteristic.readValue();
    byte[] valueWrite = { 0x01};
    characteristic.writeValue(valueWrite);
} catch (KuraBluetoothIOException e) {
    logger.error("IO operation failed", e);
}
```

In the following example, instead, a notification listener is configured to periodically receive the data from a GATT characteristic and print the first value of the given array. The period is internally set by the BLE device.

```
try {
    Consumer<byte[]> callback = valueBytes -> System.out.println((int) valueBytes[0]);
    characteristic.enableValueNotifications(callback);
} catch (KuraBluetoothNotificationException e) {
    logger.error();
}
```

## Configure Bluez on the Raspberry Pi

The minimum version of Bluez supported by Kura Bluetooth LE APIs is 5.42. The Raspbian Stretch OS comes with Bluez 5.43, but older OS couldn't have an updated Bluez version. In this case, it is possible to compile and install Bluez from sources using a Raspberry Pi. The Bluez sources can be found <a href="http://www.bluez.org/download/" about="_blank">here</a>. Proceed as follows:

* Install the packages needed for compile Bluez: 

```
sudo apt-get install libusb-dev libdbus-1-dev libglib2.0-dev libudev-dev libical-dev libreadline-dev
```
* Download bluez-5.43.tar.xz (or newer version) from <a href="http://www.bluez.org/download/" about="_blank">here</a>
* Decompress the compressed archive:

```
tar -xf bluez-5.43.tar.x
```

* Compile the sources:

```
cd bluez-5.43
./configure --prefix=/usr --sysconfdir=/etc --localstatedir=/var --enable-library -disable-systemd --enable-experimental --enable-maintainer-mod
make
make install
```



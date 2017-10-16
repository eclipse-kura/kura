---
layout: page
title:  "Kura Beacon APIs"
categories: [dev]
---

[Overview](#overview)

[How to use Kura iBeacon&trade; APIs](#how-to-use-kura-ibeacontrade-apis)

[How to use Kura Eddystone&trade; APIs](#how-to-use-kura-eddystonetrade-apis)

[Add new Beacon APIs implementation](#add-new-beacon-apis-implementation)

## Overview

Starting from version 3.1.0, Eclipse Kura implements a new set of APIs for managing Bluetooth Low Energy and Beacon devices. The new APIs replace the existing Bluetooth APIs, but the old ones are still available and can be used. So, the applications developed before Kura 3.1.0 continue to work.

The purpose of the new BLE Beacon APIs is to simplify the development of applications that interact with Bluetooth LE Beacon devices, offering clear and easy-to-use methods for advertising and scanning. Eclipse Kura offers out-of-the-box the implementation of the Beacon APIs for iBeacon&trade; and Eddystone&trade; technologies. Moreover, the new APIs allow to easily integrate new beacon implementations with Eclipse Kura.

## How to use Kura iBeacon&trade; APIs

This section briefly presents how to use the iBeacon&trade; implementation of the Kura Beacon APIs, providing several code snippets to explain how to perform common operations on iBeacons. For a complete example on iBeacon advertising and scanning, please refer to the new <a href="https://github.com/eclipse/kura/tree/develop/kura/examples/org.eclipse.kura.example.ibeacon.advertiser" about="_blank">iBeacon&trade; advertiser</a> and <a href="https://github.com/eclipse/kura/tree/develop/kura/examples/org.eclipse.kura.example.ibeacon.scanner" about="_blank">iBeacon&trade; scanner</a> examples. For more information about iBeacon&trade; please refer to <a href="https://developer.apple.com/ibeacon/" about="_blank">official page</a>.

An application that wants to use the iBeacon&trade; implementation of Kura Beacon APIs should bind the **BluetoothLeService** and **BluetoothLeIBeaconService** OSGI services, as shown in the following Java snippet:

```
public void setBluetoothLeService(BluetoothLeService bluetoothLeService) {
    this.bluetoothLeService = bluetoothLeService;
}

public void unsetBluetoothLeService(BluetoothLeService bluetoothLeService) {
    this.bluetoothLeService = null;
}

public void setBluetoothLeIBeaconService(BluetoothLeIBeaconService bluetoothLeIBeaconService) {
    this.bluetoothLeIBeaconService = bluetoothLeIBeaconService;
}

public void unsetBluetoothLeIBeaconService(BluetoothLeIBeaconService bluetoothLeIBeaconService) {
    this.bluetoothLeIBeaconService = null;
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

<reference bind="setBluetoothLeIBeaconService" 
            cardinality="1..1" 
            interface="org.eclipse.kura.ble.ibeacon.BluetoothLeIBeaconService" 
            name="BluetoothLeIBeaconService" 
            policy="static" 
            unbind="unsetBluetoothLeIBeaconService"/>
```

The **BluetoothLeService** is used to get the **BluetoothLeAdapter** to be used with the **BluetoothLeIBeaconScanner** and **BluetoothLeIBeaconAdvertiser**. As explained [here](bluetooth-le-apis.html#get-the-bluetooth-adapter), the adapter can be retrieved and powered on as follows:

````
this.bluetoothLeAdapter = this.bluetoothLeService.getAdapter(adapterName);
if (this.bluetoothLeAdapter != null) {
    if (!this.bluetoothLeAdapter.isPowered()) {
        this.bluetoothLeAdapter.setPowered(true);
    }
} 
````

where **adapterName** is the name of the adapter, e.g. hci0.

### Create an iBeacon&trade; advertiser

In order to properly configure an iBeacon&trade; advertiser, a **BluetoothLeIBeaconService** is needed to create a new **BluetoothLeIBeaconAdvertiser** instance bound to a specific Bluetooth adapter:

```
try {
    BluetoothLeBeaconAdvertiser<BluetoothLeIBeacon> advertiser = this.bluetoothLeIBeaconService.newBeaconAdvertiser(this.bluetoothLeAdapter);
} catch (KuraBluetoothBeaconAdvertiserNotAvailable e) {
    logger.error("Beacon Advertiser not available on {}", this.bluetoothLeAdapter.getInterfaceName(),e);
}
```

Then a **BluetoothLeIBeacon** object should be created, containing all the information to be broadcasted. In the following snippet, the **BluetoothLeIBeacon** object is instantiated and added to the advertiser. Then the broadcast time interval is set and the beacon advertising is started.

```
try {
    BluetoothLeIBeacon iBeacon = new BluetoothLeIBeacon(uuid, major, minor, txPower);
    advertiser.updateBeaconAdvertisingData(iBeacon);
    advertiser.updateBeaconAdvertisingInterval(minInterval, maxInterval;

    advertiser.startBeaconAdvertising();
} catch (KuraBluetoothCommandException e) {
    logger.error("IBeacon configuration failed", e);
}
```

The **BluetoothLeIBeacon** represents the beacon packet that will be broadcasted by the advertiser and it should be configured will the following parameters:

* **uuid** a unique number that identifies the beacon.
* **major** a number that identifies a subset of beacons within a large group.
* **minor** a number that identifies a specific beacon.
* **txPower** the transmitter power level indicating the signal strength one meter from the device.

{% include alerts.html message='Only one advertising packet can be broadcasted at a time on a specific Bluetooth adapter.' %}

Finally, in the following snippet the advertiser is stopped and removed from the **BluetoothLeIBeaconService**:

```
try {
    advertiser.stopBeaconAdvertising();
    this.bluetoothLeIBeaconService.deleteBeaconAdvertiser(advertiser);
} catch (KuraBluetoothCommandException e) {
    logger.error("Stop iBeacon advertising failed", e);
}
```

### Create an iBeacon&trade; scanner

As done for the [advertiser](#create-an-ibeacontrade-advertiser), a **BluetoothLeIBeaconService** is needed to create a new **BluetoothLeIBeaconScanner** instance bound to a specific Bluetooth adapter:

```
bluetoothLeBeaconScanner<BluetoothLeIBeacon> scanner = this.bluetoothLeIBeaconService.newBeaconScanner(this.bluetoothLeAdapter);
```

A **BluetoothLeIBeaconScanner** needs a listener to collect the iBeacon packets that the Bluetooth adapter detects. In the following snippet, a simple listener that prints the iBeacon packet configuration is added to the scanner object:

```
private class iBeaconListener implements BluetoothLeBeaconListener<BluetoothLeIBeacon> {

    @Override
    public void onBeaconsReceived(BluetoothLeIBeacon beacon) {
        logger.info("iBeacon received from {}", beacon.getAddress());
        logger.info("UUID : {}", beacon.getUuid());
        logger.info("Major : {}", beacon.getMajor());
        logger.info("Minor : {}", beacon.getMinor());
        logger.info("TxPower : {}", beacon.getTxPower());
        logger.info("RSSI : {}", beacon.getRssi());   
    }
    
}
```
```
scanner.addBeaconListener(listener);
```

The scanner is started for a specific time interval (in this case 10 seconds):

```
scanner.startBeaconScan(10);
```

Finally the scanner should be stopped, if needed, and the resources are released:

```
if (scanner.isScanning()) {
    scanner.stopBeaconScan();
}
scanner.removeBeaconListener(listener);
this.bluetoothLeIBeaconService.deleteBeaconScanner(scanner);
```

## How to use Kura Eddystone&trade; APIs

Eddystone&trade; is a protocol specification that defines a BLE message format for proximity beacon messages. It describes several different frame types that may be used individually or in combinations to create beacons that can be used for a variety of applications. For more information please see <a href="https://developers.google.com/beacons/" about="_blank">here</a> and <a href="https://github.com/google/eddystone" about="_blank">here</a>.

In this section the Eddystone&trade; implementation of the Kura Beacon APIs is presented, providing several code snippets to explain how to perform common operations on them. For a complete example on Eddystone&trade; advertising and scanning, please refer to the new <a href="https://github.com/eclipse/kura/tree/develop/kura/examples/org.eclipse.kura.example.eddystone.advertiser" about="_blank">Eddystone&trade; advertiser</a> and <a href="https://github.com/eclipse/kura/tree/develop/kura/examples/org.eclipse.kura.example.eddystone.scanner" about="_blank">Eddystone&trade; scanner</a> examples.

{% include alerts.html message='Only Eddystone UID and URL frame types are currently supported.' %}

As done with the [iBeacon&trade;](#how-to-use-kura-ibeacontrade-apis) implementation, an application has to bind the **BluetoothLeService** and **BluetoothLeEddystoneService** OSGI services, as shown in the following Java snippet:

```
public void setBluetoothLeService(BluetoothLeService bluetoothLeService) {
    this.bluetoothLeService = bluetoothLeService;
}

public void unsetBluetoothLeService(BluetoothLeService bluetoothLeService) {
    this.bluetoothLeService = null;
}

public void setBluetoothLeEddystoneService(BluetoothLeEddystoneService bluetoothLeEddystoneService) {
    this.bluetoothLeEddystoneService = bluetoothLeEddystoneService;
}

public void unsetBluetoothLeEddystoneService(BluetoothLeEddystoneService bluetoothLeEddystoneService) {
    this.bluetoothLeEddystoneService = null;
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

<reference bind="setBluetoothLeEddystoneService" 
            cardinality="1..1" 
            interface="org.eclipse.kura.ble.eddystone.BluetoothLeEddystoneService" 
            name="BluetoothLeEddystoneService" 
            policy="static" 
            unbind="unsetBluetoothLeEddystoneService"/>
```

The **BluetoothLeService** is used to get the **BluetoothLeAdapter** to be used with the **BluetoothLeEddystoneScanner** and **BluetoothLeEddystoneAdvertiser**. As explained [here](bluetooth-le-apis.html#get-the-bluetooth-adapter), the adapter can be retrieved and powered on as follows:

````
this.bluetoothLeAdapter = this.bluetoothLeService.getAdapter(adapterName);
if (this.bluetoothLeAdapter != null) {
    if (!this.bluetoothLeAdapter.isPowered()) {
        this.bluetoothLeAdapter.setPowered(true);
    }
} 
````

where **adapterName** is the name of the adapter, e.g. hci0.

### Create an Eddystone&trade; advertiser

In order to properly configure an Eddystone&trade; advertiser, a **BluetoothLeEddystoneService** is needed to create a new **BluetoothLeEddystoneAdvertiser** instance bound to a specific Bluetooth adapter:

```
try {
    BluetoothLeBeaconAdvertiser<BluetoothLeEddystone> advertiser = this.advertising = this.bluetoothLeEddystoneService.newBeaconAdvertiser(this.bluetoothLeAdapter);
} catch (KuraBluetoothBeaconAdvertiserNotAvailable e) {
    logger.error("Beacon Advertiser not available on {}", this.bluetoothLeAdapter.getInterfaceName(),e);
}
```

The advertiser has to be configured with a **BluetoothLeEddystone** object that contains all the information to be broadcasted. Currently, **UID** and **URL** frame types are supported. A UID frame can be created as follows:

```
BluetoothLeEddystone eddystone = new BluetoothLeEddystone();
eddystone.configureEddystoneUIDFrame(namespace, instance, txPower);
```

where **namespace** and **instance** are respectively 10-byte and 6-byte long sequences that compose a unique 16-byte Beacon ID. The **txPower** is the calibrated transmission power at 0 m.

A URL frame is created as follows:

```
BluetoothLeEddystone eddystone = new BluetoothLeEddystone();
eddystone.configureEddystoneURLFrame(url, txPower);
```

where **url** is the URL to be broadcasted and the **txPower** is the calibrated transmission power at 0 m.

After the **BluetoothLeEddystone** creation, the packet is added to the advertiser and the broadcast time interval is set. Then the advertiser is started:

```
try {
    advertiser.updateBeaconAdvertisingData(eddystone);
    advertiser.updateBeaconAdvertisingInterval(this.options.getMinInterval(), this.options.getMaxInterval());
    advertiserstartBeaconAdvertising();
} catch (KuraBluetoothCommandException e) {
    logger.error("Eddystone configuration failed", e);
}
```

Finally, in the following snippet the advertiser is stopped and removed from the **BluetoothLeEddystoneService**:

```
try {
    advertiser.stopBeaconAdvertising();
    this.bluetoothLeEddystoneService.deleteBeaconAdvertiser(advertiser);
} catch (KuraBluetoothCommandException e) {
    logger.error("Stop Advertiser advertising failed", e);
}
```

### Create an Eddystone&trade; scanner

As done for the [advertiser](#create-an-eddystonetrade-advertiser), a **BluetoothLeEddystoneService** is needed to create a new **BluetoothLeEddystoneScanner** instance bound to a specific Bluetooth adapter:

```
bluetoothLeBeaconScanner<BluetoothLeEddystone> scanner = this.bluetoothLeEddystoneService.newBeaconScanner(this.bluetoothLeAdapter);
```

A **BluetoothLeEddystoneScanner** needs a listener to collect the Eddystone packets that the Bluetooth adapter detects. In the following snippet, a simple listener that detects the frame type and prints the packet content is added to the scanner object:

```
private class EddystoneListener implements BluetoothLeBeaconListener<BluetoothLeEddystone> {

    @Override
    public void onBeaconsReceived(BluetoothLeEddystone beacon) {
        logger.info("Eddystone {} received from {}", eddystone.getFrameType(), eddystone.getAddress());
        if ("UID".equals(eddystone.getFrameType())) {
            logger.info("Namespace : {}", bytesArrayToHexString(eddystone.getNamespace()));
            logger.info("Instance : {}", bytesArrayToHexString(eddystone.getInstance()));
        } else if ("URL".equals(eddystone.getFrameType())) {
            logger.info("URL : {}", eddystone.getUrlScheme() + eddystone.getUrl());
        }
        logger.info("TxPower : {}", eddystone.getTxPower());
        logger.info("RSSI : {}", eddystone.getRssi());  
    }
    
}
```
```
scanner.addBeaconListener(listener);
```

The scanner is started for a specific time interval (in this case 10 seconds):

```
scanner.startBeaconScan(10);
```

Finally the scanner should be stopped, if needed, and the resources are released:

```
if (scanner.isScanning()) {
    scanner.stopBeaconScan();
}
scanner.removeBeaconListener(listener);
this.bluetoothLeEddystoneService.deleteBeaconScanner(scanner);
```

## Add new Beacon APIs implementation

Eclipse Kura offers the implementation for iBeacon&trade; and Eddystone&trade; protocols, but it is possible to add  implementations of different kinds of beacon protocols.

The **org.eclipse.kura.bluetooth.le.beacon** package contains the interfaces used by the beacon implementations:

* **BluetoothLeBeaconService** is the entry point for applications that want to use the Beacon APIs.
* **BluetoothLeBeaconManager** is used by the BluetoothLeBeaconService and provides methods to create and delete Beacon advertisers and scanners.
* **BluetoothLeBeaconAdvertiser** allows configuring advertisement packets and managing advertising.
* **BluetoothLeBeaconScanner** is used to search for specific Beacon packets.
* **BluetoothLeBeaconEncoder** implements methods for encoding a Beacon object to a stream of bytes.
* **BluetoothLeBeaconDecoder** implements methods for decoding a stream of bytes in to a Beacon object.
* **BluetoothLeBeacon** represents a generic Beacon packet.

The **BluetoothLeBeaconManager**, **BluetoothLeBeaconScanner** and **BluetoothLeBeaconAdvertiser** interfaces handles generic **BluetoothLeBeacon** objects and their implementations are provided by the **org.eclipse.kura.ble.provider**. The others interfaces, instead, are Beacon specific and their implementations depend on the specific protocol that is used. As a consequence, who wants to support a new Beacon protocol, should provide the implementation of the **BluetoothLeBeaconService**, **BluetoothLeBeaconEncoder** and **BluetoothLeBeaconDecoder** interfaces and extend the **BluetoothLeBeacon** class.

As an example, the <a href="https://github.com/eclipse/kura/tree/develop/kura/org.eclipse.kura.ble.ibeacon.provider" about="_blank">**org.eclipse.kura.ble.ibeacon.provider**</a> provides the implementation of the above APIs for the iBeacon&trade; protocol. In this case, the **org.eclipse.kura.ble.ibeacon** package contains the following:

* **BluetoothLeIBeacon** implements the **BluetoothLeBeacon** interface for the iBeacon&trade; packet.
* **BluetoothLeIBeaconEncoder** is a marker interface that extends **BluetoothLeBeaconEncoder**.
* **BluetoothLeIBeaconDecoder** is a marker interface that extends **BluetoothLeBeaconDecoder**.
* **BluetoothLeIBeaconService** is a marker interface that extends **BluetoothLeBeaconService** and is the entry point for applications that wants to use an iBeacon&trade;.

The **org.eclipse.kura.internal.ble.ibeacon** provides the implementations for the above interfaces:

* **BluetoothLeIBeaconEncoderImpl** implements **BluetoothLeIBeaconEncoder** offering a method to encode the **BluetoothLeIBeacon** into a byte stream.
* **BluetoothLeIBeaconDecoderImpl** implements **BluetoothLeIBeaconDecoder** offering a method to decode a stream of bytes into a **BluetoothLeIBeacon** object.
* **BluetoothLeIBeaconServiceImpl** is the implementation of **BluetoothLeIBeaconService** and uses the generic **BluetoothLeBeaconManager** service to create scanners and advertisers.

The following image shows the UML diagram.

![sensortag_asset]({{ site.baseurl }}/assets/images/beacon/beaconUML.png)

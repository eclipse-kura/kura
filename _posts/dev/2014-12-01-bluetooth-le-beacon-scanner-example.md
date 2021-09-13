---
layout: page
title:  "Legacy BLE Beacon Scanner Example"
categories: [dev]
---

[Overview](#overview)

[Prerequisites](#prerequisites)

[Beacon Scanning with Kura](#beacon-scanning-with-kura)

*  [Develop the Beacon Scanner Bundle](#develop-the-beacon-scanner-bundle)

*  [Deploy and Validate the Bundle](#deploy-and-validate-the-bundle)

{% include alerts.html message="This guide uses the deprecated Kura Bluetooth APIs. Please consider to use the new [ones](beacon-apis)." %}

## Overview

The Bluetooth Beacon Scanner example is a bundle for Eclipse Kura that uses the Bluetooth LE service to search for near Beacon devices. A Beacon device is a Bluetooth Low Energy device that broadcasts its identity to nearby devices. It uses a specific BLE packet, called beacon or advertising packet, that contains the following information:

* **Proximity UUID**  - a 128-bit value that uniquely identifies one or more beacons as a certain type or from a certain organization.

* **Major value** - an optional 16-bit unsigned integer that can group related beacons with the same proximity UUID.

* **Minor value** - an optional 16-bit unsigned integer that differentiates beacons with the same proximity UUID and major value.

* **Tx Power** - a value programmed into the beacon that enables distance from the beacon to be determined based on signal strength.

The Beacon Scanner bundle is configured with a **Company Code** in order to filter the near beacons. So, only beacons with a specific Company Code are discovered by the bundle and their information are reported. Moreover the bundle is able to roughly estimate the distance from the beacon.

For further information about the Beacons, please refer to the [BLE Beacon Example](bluetooth-le-example.html).

## Prerequisites

* [Development Environment Setup](kura-setup.html)

* Hardware

  * Embedded device running Kura with Bluetooth 4.0 (LE) capabilities.

  * bluez_ packet must be installed on the embedded device. Follow the installation instructions in [How to Use Bluetooth LE](bluetooth-le-example.html).

For this tutorial a Raspberry Pi Type B with a LMTechnologies LM506 Bluetooth 4.0 <http://lm-technologies.com/wireless-adapters/lm506-class-1-bluetooth-4-0-usb-adapter/> dongle is used.

## Beacon Scanning with Kura

The Beacon Scanner bundle is a Kura example that allows you to configure the Company Code for the Beacon filtering and to start/stop the scanner procedure.

## <span id="develop_the_beacon_bundle" class="anchor"><span id="develop_the_beacon_bundle_1" class="anchor"></span></span>Develop the Beacon Scanner Bundle

The Beacon Scanner bundle code development follows the guidelines presented in the [Hello World Application](hello-example.html) :

* Create a Plug-in Project named **org.eclipse.kura.example.beacon.scanner**.

* Create the class **BeaconScannerExample**.

* Include the following bundles in the MANIFEST.MF:
  * org.eclipse.kura.bluetooth
  * org.eclipse.kura.configuration
  * org.osgi.service.component
  * org.slf4j

The following files need to be implemented in order to write the source code:


* [**META-INF/MANIFEST.MF**](<https://github.com/eclipse/kura/blob/develop/kura/examples/org.eclipse.kura.example.beacon.scanner/META-INF/MANIFEST.MF>) - OSGI manifest that describes the bundle and its dependencies.

* [**OSGI-INF/beaconExample.xml**](<https://github.com/eclipse/kura/blob/develop/kura/examples/org.eclipse.kura.example.beacon.scanner/OSGI-INF/beaconExample.xml>) - declarative services definition that describes the services exposed and consumed by this bundle.

* [**OSGI-INF/metatype/org.eclipse.kura.example.beacon.scanner.BeaconScannerExample.xml**](<https://github.com/eclipse/kura/blob/develop/kura/examples/org.eclipse.kura.example.beacon.scanner/OSGI-INF/metatype/org.eclipse.kura.example.beacon.scanner.BeaconScannerExample.xml>) - configuration description of the bundle and its parameters, types, and defaults.

* [**org.eclipse.kura.example.beacon.scanner.BeaconScannerExample.java**](<https://github.com/eclipse/kura/blob/develop/kura/examples/org.eclipse.kura.example.beacon.scanner/src/main/java/org/eclipse/kura/example/beacon/scanner/BeaconScannerExample.java>) - main implementation class.

### OSGI-INF/metatype/org.eclipse.kura.example.beacon.scanner.BeaconScannerExample.xml File

The OSGI-INF/metatype/org.eclipse.kura.example.beacon.scanner.BeaconScannerExample.xml file describes the parameters for this bundle including the following:

* **enableScanning** - enables Beacon scanning.

* **companyCode** - defines a 16-bit company code as hex string.

* **iname** - provides the name of bluetooth adapter.

### org.eclipse.kura.example.beacon.scanner.BeaconScannerExample.java File

The com.eurotech.example.beacon.scanner.BeaconScannerExample.java file contains the activate, deactivate and updated methods for this bundle. The activate and updated methods gets the properties from the configurable component and, if the scanning is enabled, call the _setup_ private method. This method gets the _BluetoothAdapter_, enables the interface if needed, and starts the scan calling the _bluetootAdapter.startBeaconScan(companyCode, listener)_ method. The arguments of the method are the _companyCode_ and a _listener_ that is notified when a device is detected. In this case the _BeaconScannerExample_ class implements _BluetoothBeaconScanListener_.
The following code sample shows the _setup_ method:

```java
private void setup() {

    this.publishTimes = new HashMap<String, Long>();

    this.bluetoothAdapter = this.bluetoothService.getBluetoothAdapter(this.adapterName);
    if (this.bluetoothAdapter != null) {
        this.bluetoothAdapter.startBeaconScan(this.companyCode, this);
    }

}
```

Since _BeaconScannerExample_ implements _BluetoothBeaconScanListener_, the _onBeaconDataReceived_ method must be overridden. When a device is detected, the listener is notified and the _onBeaconDataReceived_ method is called with a _BluetoothBeaconData_ object. The _BluetoothBeaconData_ class contains the following fields:

* **uuid** - a 128-bit value that uniquely identifies one or more beacons as a certain type or from a certain organization.

* **address** - the source of the beacon

* **major** - an optional 16-bit unsigned integer that can group related beacons with the same proximity UUID.

* **minor** - an optional 16-bit unsigned integer that differentiates beacons with the same proximity UUID and major value.

* **rssi** - the Received Signal Strength Indication

* **txpower** - a value programmed into the beacon that enables distance from the beacon to be determined based on signal strength.

The following code is an implementation of _onBeaconDataReceived_ that logs the _BluetoothBeaconData_ fields:

```java
public void onBeaconDataReceived(BluetoothBeaconData beaconData) {

    logger.debug("Beacon from {} detected.", beaconData.address);
    long now = System.nanoTime();

    Long lastPublishTime = this.publishTimes.get(beaconData.address);

    // If this beacon is new, or it last published more than 'rateLimit' ms ago
    if (lastPublishTime == null || (now - lastPublishTime) / 1000000L > this.rateLimit) {

        // Store the publish time against the address
        this.publishTimes.put(beaconData.address, now);
        if (this.cloudPublisher == null) {
            logger.info("No cloud publisher selected. Cannot publish!");
            return;
        }

        // Publish the beacon data to the beacon's topic
        KuraPayload kp = new KuraPayload();
        kp.setTimestamp(new Date());
        kp.addMetric("uuid", beaconData.uuid);
        kp.addMetric("txpower", beaconData.txpower);
        kp.addMetric("rssi", beaconData.rssi);
        kp.addMetric("major", beaconData.major);
        kp.addMetric("minor", beaconData.minor);
        kp.addMetric("distance", calculateDistance(beaconData.rssi, beaconData.txpower));

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("address", beaconData.address);
        KuraMessage message = new KuraMessage(kp, properties);
        try {
            this.cloudPublisher.publish(message);
        } catch (KuraException e) {
            logger.error("Unable to publish", e);
        }
    }
}
```

Finally, the Beacon Scanner is able to roughly estimate the distance of the detected beacon using the _calculateDistance_ method:

```java
private double calculateDistance(int rssi, int txpower) {

    double distance;

    int ratioDB = txpower - rssi;
    double ratioLinear = Math.pow(10, (double) ratioDB / 10);
    distance = Math.sqrt(ratioLinear);

    return distance;
}
```

## <span id="deploy_and_validate_the_bundle" class="anchor"><span id="deploy-and-validate-the-bundle" class="anchor"></span></span>Deploy and Validate the Bundle

In order to proceed, you need to know the IP address of your embedded gateway that is on the remote target unit. With this information, follow the mToolkit instructions for installing a single bundle to the remote target device [located here](deploying-bundles.html#_Install_Single_Bundle).  When the installation is complete, the bundle starts automatically.

In the Kura Gateway Administration Console, the BeaconScannerExample tab appears on the left and enables the device to be configured for scanning.

You should see a message similar to the one below from **/var/log/kura.log** indicating that the bundle was successfully installed and configured.

```
2016-08-08 14:39:48,351 [Component Resolve Thread (Bundle 6)] INFO  o.e.k.e.b.s.BeaconScannerExample - Activating Bluetooth Beacon Scanner example...
2016-08-08 14:39:48,353 [Component Resolve Thread (Bundle 6)] INFO  o.e.k.e.b.s.BeaconScannerExample - Activating Bluetooth Beacon Scanner example...Done
2016-08-08 14:39:48,373 [Component Resolve Thread (Bundle 6)] INFO  o.e.k.c.c.ConfigurableComponentTracker - Adding ConfigurableComponent with pid org.eclipse.kura.example.beacon.scanner.BeaconScannerExample, service pid org.eclipse.kura.example.beacon.scanner.BeaconScannerExample and factory pid null
2016-08-08 14:39:48,373 [Component Resolve Thread (Bundle 6)] INFO  o.e.k.c.c.ConfigurationServiceImpl - Registration of ConfigurableComponent org.eclipse.kura.example.beacon.scanner.BeaconScannerExample by org.eclipse.kura.core.configuration.ConfigurationServiceImpl@1197c95...
2016-08-08 14:40:40,186 [qtp23115489-40] WARN  o.e.k.w.s.s.SkinServlet - Resource File /opt/eclipse/kura/console/skin/skin.js does not exist
2016-08-08 14:40:56,996 [qtp23115489-42] INFO  o.e.k.c.c.ConfigurationServiceImpl - Loading init configurations from: 1470667042563...
2016-08-08 14:40:57,679 [qtp23115489-42] INFO  o.e.k.c.c.ConfigurationServiceImpl - Merging configuration for pid: org.eclipse.kura.example.beacon.scanner.BeaconScannerExample
2016-08-08 14:40:57,687 [qtp23115489-42] INFO  o.e.k.c.c.ConfigurationServiceImpl - Updating Configuration of ConfigurableComponent org.eclipse.kura.example.beacon.scanner.BeaconScannerExample ... Done.
2016-08-08 14:40:57,689 [qtp23115489-42] INFO  o.e.k.c.c.ConfigurationServiceImpl - Writing snapshot - Saving /opt/eclipse/kura/data/snapshots/snapshot_1470667257688.xml...
2016-08-08 14:40:57,914 [qtp23115489-42] INFO  o.e.k.c.c.ConfigurationServiceImpl - Writing snapshot - Saving /opt/eclipse/kura/data/snapshots/snapshot_1470667257688.xml... Done.
2016-08-08 14:40:57,916 [qtp23115489-42] INFO  o.e.k.c.c.ConfigurationServiceImpl - Snapshots Garbage Collector. Deleting /opt/eclipse/kura/data/snapshots/snapshot_1470651681077.xml
2016-08-08 14:40:58,013 [Component Resolve Thread (Bundle 6)] INFO  o.e.k.l.b.l.BluetoothLeScanner - Starting bluetooth le beacon scan...
```

Using a device equipped with Kura acting as a Beacon (see [BLE Beacon Example](bluetooth-le-example.html)), the following lines appear on the log file when the device is detected:

```
2016-08-08 14:49:03,487 [BluetoothProcess BTSnoop Gobbler] INFO  o.e.k.e.b.s.BeaconScannerExample - UUID : AAAAAAAABBBBCCCCDDDDEEEEEEEEEEEE
2016-08-08 14:49:03,488 [BluetoothProcess BTSnoop Gobbler] INFO  o.e.k.e.b.s.BeaconScannerExample - TxPower : -58
2016-08-08 14:49:03,488 [BluetoothProcess BTSnoop Gobbler] INFO  o.e.k.e.b.s.BeaconScannerExample - RSSI : -55
2016-08-08 14:49:03,489 [BluetoothProcess BTSnoop Gobbler] INFO  o.e.k.e.b.s.BeaconScannerExample - Major : 0
2016-08-08 14:49:03,489 [BluetoothProcess BTSnoop Gobbler] INFO  o.e.k.e.b.s.BeaconScannerExample - Minor : 0
2016-08-08 14:49:03,490 [BluetoothProcess BTSnoop Gobbler] INFO  o.e.k.e.b.s.BeaconScannerExample - Address : 5C:F3:70:60:63:8F
2016-08-08 14:49:03,490 [BluetoothProcess BTSnoop Gobbler] INFO  o.e.k.e.b.s.BeaconScannerExample - Distance : 0.7079457843841379
```

---
layout: page
title:  "BLE Beacon Example"
date:   2014-08-10 11:28:21
categories: [doc]
---

[Overview](#overview_1)

[Prerequisites](#prerequisites)

[Beacon Advertising with *hcitool*](#beacon_advertising_with_hcitool_1)

[Beacon Advertising with ESF](#beacon_advertising_with_esf_1)

*  [Develop the Beacon Bundle](#develop_the_beacon_bundle_1)

    *  [OSGI-INF/metatype/org.eclipse.kura.example.beacon.BeaconExample.xml File](#OSGI-INF/metatype_1)

    *  [org.eclipse.kura.example.beacon.BeaconExample.java File](#BluetoothExample_1)

*  [Deploy and Validate the Bundle](#deploy_and_validate_the_bundle_1)

## Overview

The Bluetooth Beacon example is a simple bundle for Eclipse Kura that allows you to configure a device as a Beacon. A Beacon device is a Bluetooth Low Energy device that broadcasts its identity to nearby devices. It uses a specific BLE packet, called beacon or advertising packet, that contains the following information:

* **Proximity UUID**  - a 128-bit value that uniquely identifies one or more beacons as a certain type or from a certain organization.

* **Major value** - an optional 16-bit unsigned integer that can group related beacons with the same proximity UUID.

* **Minor value** - an optional 16-bit unsigned integer that differentiates beacons with the same proximity UUID and major value.

* **Tx Power** - a value programmed into the beacon that enables distance from the beacon to be determined based on signal strength.

The advertising packet has a fixed format and is broadcasted periodically. The information contained in the advertising packet can be used by a receiver, typically a smartphone, to identify the beacon and to roughly estimate its distance.

## Prerequisites

* [Development Environment Setup](kura-setup.html)

* Hardware

  * Embedded device running ESF with Bluetooth 4.0 (LE) capabilities.

  * bluez_ packet must be installed on the embedded device. Follow the installation instructions in [How to Use Bluetooth LE](bluetooth-le-example.html).

For this tutorial a Raspberry Pi Type B with a LMTechnologies LM506 Bluetooth 4.0 <http://lm-technologies.com/wireless-adapters/lm506-class-1-bluetooth-4-0-usb-adapter/> dongle is used.

## Beacon Advertising with *hcitool*

After the embedded device is properly configured, the advertising may be started using the _hcitool_ command contained in the _bluez_ packet.

Plug in the Bluetooth dongle if needed and verify that the interface is up with the following command:

```
sudo hciconfig hci0
```

If the interface is down, enable it with the following command:

```
sudo hciconfig hci0 up
```

To configure the advertising packet, enter the following command:

```
sudo hcitool -i hci0 cmd 0x08 0x0008 1e 02 01 1a 1a ff 4c 00 02 15 aa aa aa aa bb bb cc cc dd dd ee ee ee ee ee ee 01 00 01 00 c5
```

In this example, the packet will contain the uuid **aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee**, major **1**, minor **1** and Tx Power **-59** dBm.

For further information about BLE commands and packet formats, refer to the [Bluetooth 4.0 Core specifications](https://www.bluetooth.org/docman/handlers/downloaddoc.ashx?doc_id=229737)

To set the advertising interval to 1 second, enter the following command:

```
sudo hcitool -i hci0 cmd 0x08 0x0006 a0 00 a0 00 03 00 00 00 00 00 00 00 00 07 00
```

Finally, to start the advertising, enter the following command:
```
sudo hcitool -i hci0 cmd 0x08 0x000a 01
```
To verify that the embedded device is broadcasting its beacon, use a smartphone with a iBeacon scanner app (e.g., iBeacon Finder, iBeacon Scanner, or iBeaconDetector on Android).

To stop the advertising, write 0 to the register 0x000a as shown in the following command:

```
sudo hcitool -i hci0 cmd 0x08 0x000a 00
```

## Beacon Advertising with ESF

The Beacon bundle is a simple example that allows you to configure the advertising packet, the time interval, and to start/stop the advertising.

## Develop the Beacon Bundle

The Beacon bundle code development follows the guidelines presented in the [Hello World Application](hello-example.html) :

* Create a Plug-in Project named **org.eclipse.kura.example.beacon**.

* Create the class **BeaconExample**.

* Include the following bundles in the MANIFEST.MF:
  * org.eclipse.kura.bluetooth
  * org.eclipse.kura.configuration
  * org.osgi.service.component
  * org.slf4j

The following files need to be implemented in order to write the source code:


* [**META-INF/MANIFEST.MF**](<https://github.com/eclipse/kura/blob/develop/kura/examples/org.eclipse.kura.example.beacon/META-INF/MANIFEST.MF>) - OSGI manifest that describes the bundle and its dependencies.

* [**OSGI-INF/beaconExample.xml**](<https://github.com/eclipse/kura/blob/develop/kura/examples/org.eclipse.kura.example.beacon/OSGI-INF/beaconExample.xml>) - declarative services definition that describes the services exposed and consumed by this bundle.

* [**OSGI-INF/metatype/org.eclipse.kura.example.beacon.BeaconExample.xml**](<https://github.com/eclipse/kura/blob/develop/kura/examples/org.eclipse.kura.example.beacon/OSGI-INF/metatype/org.eclipse.kura.example.beacon.BeaconExample.xml>) - configuration description of the bundle and its parameters, types, and defaults.

* [**org.eclipse.kura.example.beacon.BeaconExample.java**](<https://github.com/eclipse/kura/blob/develop/kura/examples/org.eclipse.kura.example.beacon/src/main/java/org/eclipse/kura/example/beacon/BeaconExample.java>) - main implementation class.

### OSGI-INF/metatype/org.eclipse.kura.example.beacon.BeaconExample.xml File

The OSGI-INF/metatype/org.eclipse.kura.example.beacon.beaconExample.xml file describes the parameters for this bundle including the following:

* **enableAdvertising** - enables Beacon advertising.

* **minBeaconInterval** - sets the minimum time interval between beacons (milliseconds).

* **maxBeaconInterval** - sets the maximum time interval between beacons (milliseconds).

* **uuid** - defines a 128-bit uuid for beacon advertising expressed as hex string.

* **major** - sets the major value.

* **minor** - sets the minor value.

* **companyCode** - defines a 16-bit company code as hex string.

* **txPower** - indicates the transmission power measured at 1m away from the beacon expressed in dBm.

* **LELimited** - defines the LE Discoverable Mode. Set false to advertise for 30.72s and then stops. Set true to advertise indefinitely.

* **BR_EDRSupported** - indicates whether BR/EDR is supported.

* **LE_BRController** - indicates whether LE and BR/EDR Controller operates simultaneously.

* **LE_BRHost** - indicates whether LE and BR/EDR Host operates simultaneously.

* **iname** - provides the name of bluetooth adapter.

### org.eclipse.kura.example.beacon.BeaconExample.java File

The com.eurotech.example.beacon.BeaconExample.java file contains the activate and deactivate methods for this bundle. The activate method gets the _BluetoothAdapter_, enables the interface if needed, and executes the _configureBeacon_ method that configures the device according to the properties. The following code sample shows part of the activate method:

```java
try {
	// Get Bluetooth adapter with Beacon capabilities and ensure it is enabled
	m_bluetoothAdapter = m_bluetoothService.getBluetoothAdapter(m_iname, this);
	if (m_bluetoothAdapter != null) {
		s_logger.info("Bluetooth adapter interface => " + m_iname);
		s_logger.info("Bluetooth adapter address => " + m_bluetoothAdapter.getAddress());
		s_logger.info("Bluetooth adapter le enabled => " + m_bluetoothAdapter.isLeReady());

		if (!m_bluetoothAdapter.isEnabled()) {
			s_logger.info("Enabling bluetooth adapter...");
			m_bluetoothAdapter.enable();
			s_logger.info("Bluetooth adapter address => " + m_bluetoothAdapter.getAddress());
		}

		configureBeacon();

	}
	else
		s_logger.warn("No Bluetooth adapter found ...");
}
```

The _configureBeacon_ (shown below) is a private method:

```java
private void configureBeacon() {

	if (m_enable) {

		if (m_minInterval != null && m_maxInterval != null) {
			m_bluetoothAdapter.setBeaconAdvertisingInterval(m_minInterval, m_maxInterval);
		}

		if (m_uuid != null && m_major != null && m_minor != null && m_companyCode != null && m_txPower != null) {
			m_bluetoothAdapter.setBeaconAdvertisingData(m_uuid, m_major, m_minor, m_companyCode, m_txPower, m_LELimited, (m_LELimited) ? false : true,
					m_BRSupported, m_BRController, m_BRHost);
		}

		m_bluetoothAdapter.startBeaconAdvertising();
	}
	else
		m_bluetoothAdapter.stopBeaconAdvertising();
}
```

## Deploy and Validate the Bundle

In order to proceed, you need to know the IP address of your embedded gateway that is on the remote target unit. With this information, follow the mToolkit instructions for installing a single bundle to the remote target device [located here](deploying-bundles.html#_Install_Single_Bundle).  When the installation is complete, the bundle starts automatically.

In the ESF Gateway Administration Console, the BeaconExample tab appears on the left and enables the beacon to be configured for advertising.

You should see a message similar to the one below from **/var/log/kura.log** indicating that the bundle was successfully installed and configured.

```
2015-07-09 10:46:06,522 [Component Resolve Thread (Bundle 6)] INFO  o.e.k.e.b.BeaconExample - Activating Bluetooth Beacon example...
2015-07-09 10:46:06,639 [Component Resolve Thread (Bundle 6)] INFO  o.e.k.e.b.BeaconExample - Bluetooth adapter interface => hci0
2015-07-09 10:46:06,643 [Component Resolve Thread (Bundle 6)] INFO  o.e.k.e.b.BeaconExample - Bluetooth adapter address => null
2015-07-09 10:46:06,645 [Component Resolve Thread (Bundle 6)] INFO  o.e.k.e.b.BeaconExample - Bluetooth adapter le enabled => false
2015-07-09 10:46:06,664 [Component Resolve Thread (Bundle 6)] INFO  o.e.k.e.b.BeaconExample - Enabling bluetooth adapter...
2015-07-09 10:46:06,745 [Component Resolve Thread (Bundle 6)] INFO  o.e.k.e.b.BeaconExample - Bluetooth adapter address => 5C:F3:70:60:63:9E
2015-07-09 10:46:06,770 [Component Resolve Thread (Bundle 6)] INFO  o.e.k.l.b.BluetoothAdapterImpl - Set Advertising Parameters on interface hci0
2015-07-09 10:46:06,842 [BluetoothProcess Input Stream Gobbler] INFO  o.e.k.l.b.l.b.BluetoothBeaconListener - Command ogf 0x08, ocf 0x0006 Succeeded.
2015-07-09 10:46:06,852 [Component Resolve Thread (Bundle 6)] INFO  o.e.k.l.b.BluetoothAdapterImpl - Set Advertising Data on interface hci0
2015-07-09 10:46:06,859 [BluetoothProcess Input Stream Gobbler] INFO  o.e.k.e.b.BeaconExample - Command results :   01 06 20 00
2015-07-09 10:46:06,872 [Component Resolve Thread (Bundle 6)] INFO  o.e.k.l.b.BluetoothAdapterImpl - Start Advertising on interface hci0
2015-07-09 10:46:06,906 [BluetoothProcess Input Stream Gobbler] INFO  o.e.k.l.b.l.b.BluetoothBeaconListener - Command ogf 0x08, ocf 0x0008 Succeeded.
2015-07-09 10:46:06,908 [BluetoothProcess Input Stream Gobbler] INFO  o.e.k.e.b.BeaconExample - Command results :   01 08 20 00
2015-07-09 10:46:06,921 [BluetoothProcess Input Stream Gobbler] INFO  o.e.k.l.b.l.b.BluetoothBeaconListener - Command ogf 0x08, ocf 0x000a Succeeded.
2015-07-09 10:46:06,923 [BluetoothProcess Input Stream Gobbler] INFO  o.e.k.e.b.BeaconExample - Command results :   01 0A 20 00
2015-07-09 10:46:06,947 [Component Resolve Thread (Bundle 6)] INFO  o.e.k.c.c.ConfigurableComponentTracker - Adding ConfigurableComponent org.eclipse.kura.example.beacon.BeaconExample
2015-07-09 10:46:06,950 [Component Resolve Thread (Bundle 6)] INFO  o.e.k.c.c.ConfigurationServiceImpl - Registration of ConfigurableComponent org.eclipse.kura.example.beacon.BeaconExample by org.eclipse.kura.core.configuration.ConfigurationServiceImpl@11120b6...
2015-07-09 10:46:06,996 [Component Resolve Thread (Bundle 6)] INFO  o.e.k.c.c.ConfigurationServiceImpl - Registering org.eclipse.kura.example.beacon.BeaconExample with ocd: org.eclipse.kura.core.configuration.metatype.Tocd@8af8b3 ...
2015-07-09 10:46:06,999 [Component Resolve Thread (Bundle 6)] INFO  o.e.k.c.c.ConfigurationServiceImpl - Registration Completed for Component org.eclipse.kura.example.beacon.BeaconExample.
```

Note that the bundle writes the string returned by the configuration commands to the log:

```
2015-07-09 10:46:06,859 [BluetoothProcess Input Stream Gobbler] INFO  o.e.k.e.b.BeaconExample - Command results :   01 06 20 00
```

The last number of the string is the error code. A value of "00" indicates a successful command. Refer to the [Bluetooth 4.0 Core specifications](https://www.bluetooth.org/docman/handlers/downloaddoc.ashx?doc_id=229737) for a complete list of the error codes.

Once the bundle is deployed, you can use a iBeacon scanner app to detect the bundle. Also, you can modify the bundle properties and verify the results in the scanner.

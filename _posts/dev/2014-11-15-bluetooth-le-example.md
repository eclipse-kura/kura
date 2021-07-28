---
layout: page
title:  "Legacy Bluetooth LE Example"
categories: [dev]
---

[Overview](#overview)

*  [Prerequisites](#prerequisites)

[Prepare the Embedded Device](#prepare-the-embedded-device)

[SensorTag Communication via Command Line](#sensortag-communication-via-command-line)

[BLE Bundle for TI SensorTag](#ble-bundle-for-ti-sensortag)

*  [Develop the BLE Bundle](#develop-the-ble-bundle)

*  [Deploy and Validate the Bundle](#deploy-and-validate-the-bundle)

## Overview

This section provides an example of how to develop a simple bundle that discovers and connects to a Smart device (BLE), retrieves data from it, and publishes the results to the cloud. This example uses the TI SensorTag based on CC2541 or CC2650. For more information about this device, refer to <http://www.ti.com/tool/cc2541dk-sensor> and <https://www.ti.com/tool/TIDC-CC2650STK-SENSORTAG>.

You will learn how to perform the following functions:

* Prepare the embedded device to communicate with a Smart device

* Develop a bundle retrieves data from the device

* Optionally publish the data in the cloud

### Prerequisites

* [Setting up Kura Development Environment](kura-setup.html)

* Hardware

  * Use an embedded device running Kura with Bluetooth 4.0 (LE) capabilities

  * Use at least one TI SensorTag

This tutorial uses a Raspberry Pi Type B with a LMTechnologies LM506 Bluetooth 4.0 <http://lm-technologies.com/wireless-adapters/lm506-class-1-bluetooth-4-0-usb-adapter/> dongle.

## Prepare the Embedded Device

In order to communicate with Smart devices, the _bluez_ package must be installed on the embedded device. To do so, make sure you have the necessary libraries on the Raspberry Pi and proceed as follows:

```
sudo apt-get install libusb-dev libdbus-1-dev libglib2.0-dev libudev-dev libical-dev libreadline-dev
```

Next, download and uncompress the package:

```
sudo wget https://www.kernel.org/pub/linux/bluetooth/bluez-4.101.tar.xz
sudo tar xvf bluez-4.101.tar
```

Change to the blues folder, and then configure and install the package:

```
cd bluez-4.101
sudo ./configure --disable-systemd --enable-tools
sudo make
sudo make install
```

Finally, change the location of the hciconfig and gatttool commands:

```
sudo mv /usr/local/sbin/hciconfig /usr/sbin
sudo mv /usr/local/bin/gatttool /usr/sbin
```

{% include alerts.html message="Both _bluez_ 4.101 and 5.XX are supported." %}

## SensorTag Communication via Command Line

Once configured, you can scan and connect with a Smart device. A TI SensorTag is used in the example that follows.

Plug in the Bluetooth dongle if needed and verify that the interface is up:

```
sudo hciconfig hci0
```

If the interface is down, enable it with the following command:

```
sudo hciconfig hci0 up
```

Perform a BLE scan with hcitool (this process may be interrupted with **ctrl-c**):

```
sudo hcitool lescan
LE Scan ...
BC:6A:29:AE:CC:96 (unknown)
BC:6A:29:AE:CC:96 SensorTag
```

If the SensorTag is not listed, press the button on the left side of the device to make it discoverable. Interactive communication with the device is possible using the gatttool:

```
sudo gatttool -b BC:6A:29:AE:CC:96 -I
[   ][BC:6A:29:AE:CC:96][LE]> connect
[CON][BC:6A:29:AE:CC:96][LE]>
```

If the output of the connect command is
```
connect: Connection refused (111)
```
then you have to enable LE capabilities on your BT interface:

```
cd bluez-4.101/mgmt
sudo ./btmgmt le on
```

In order to read the sensor values from the SensorTag, you need to write some registers on the device. The example that follows shows the procedure for retrieving the temperature value from the SensorTag based on the CC2541.

Once connected with gatttool, the IR temperature sensor is enabled to write the value 01 to the handle 0x0029:

```
[CON][BC:6A:29:AE:CC:96][LE]> char-write-cmd 0x0029 01
```

Next, the temperature value is read from the 0x0025 handle:

```
[CON][BC:6A:29:AE:CC:96][LE]> char-read-hnd 0x0025
[CON][BC:6A:29:AE:CC:96][LE]>
Characteristic value/descriptor: a7 fe 2c 0d",
```

In accordance with the documentation, the retrieved raw values have to be refined in order to obtain the ambient and object temperature.

Enable notifications writing the value 0001 to the 0x0026 register:

```
[CON][BC:6A:29:AE:CC:96][LE]> char-write-cmd 0x0026 0100
[CON][BC:6A:29:AE:CC:96][LE]>
Notification handle = 0x0025 value: a5 fe 3c 0d
[CON][BC:6A:29:AE:CC:96][LE]>
Notification handle = 0x0025 value: 9f fe 3c 0d
[CON][BC:6A:29:AE:CC:96][LE]>
Notification handle = 0x0025 value: 9a fe 3c 0d
```

Stop the notifications by writing 0000 to the same register:

```
[CON][BC:6A:29:AE:CC:96][LE]>
Notification handle = 0x0025 value: 9e fe 3c 0d
[CON][BC:6A:29:AE:CC:96][LE]> char-write-cmd 0x0026 0000
Notification handle = 0x0025 value: a3 fe 3c 0d
[CON][BC:6A:29:AE:CC:96][LE]>
```

{% include alerts.html message="_bluez_ 5.XX comes with the _bluetoothctl_ tool that can be used in place of _hcitool_ and _gatttool_. Please refer to the man page and help for more details." %}

## BLE Bundle for TI SensorTag

The BLE bundle performs the following operations:

* Starts a scan for smart devices (lescan)

* Selects all the TI SensorTag in range

* Connects to the discovered SensorTags and discovers their capabilities

* Reads data from all the sensors onboard and writes the values in the log file

{% include alerts.html message="The Legacy Bluetooth LE Example supports TI SensorTag CC2541 (all firmware versions) and CC2650 (firmware version above 1.20)" %}

## Develop the BLE Bundle

Once the required packages are installed and communication with the SensorTag via command line is established, you may start to develop the BLE bundle.
For more detailed information about bundle development (i.e., the plug-in project, classes, and MANIFEST file configuration), please refer to the [Hello World Application](hello-example.html).

* Create a Plug-in Project named **org.eclipse.kura.example.ble.tisensortag**.

* Create the following classes: **BluetoothLe**, **TiSensorTag**, and **TiSensorTagGatt**.

* Include the following bundles in the MANIFEST.MF:
  * org.eclipse.kura.bluetooth
  * org.eclipse.kura.configuration
  * org.eclipse.kura.message
  * org.osgi.service.component
  * org.slf4j

The following files need to be implemented in order to write the source code:

* [**META-INF/MANIFEST.MF**](<https://github.com/eclipse/kura/blob/develop/kura/examples/org.eclipse.kura.example.ble.tisensortag/META-INF/MANIFEST.MF>) - OSGI manifest that describes the bundle and its dependencies.

* [**OSGI-INF/bleExample.xml**](<https://github.com/eclipse/kura/blob/develop/kura/examples/org.eclipse.kura.example.ble.tisensortag/OSGI-INF/bleExample.xml>) - declarative services definition that describes the services exposed and consumed by this bundle.

* [**OSGI-INF/metatype/org.eclipse.kura.example.ble.tisensortag.BluetoothLe.xml**](<https://github.com/eclipse/kura/blob/develop/kura/examples/org.eclipse.kura.example.ble.tisensortag/OSGI-INF/metatype/org.eclipse.kura.example.ble.tisensortag.BluetoothLe.xml>) - configuration description of the bundle and its parameters, types, and defaults.

* [**org.eclipse.kura.example.ble.tisensortag.BluetoothLe.java**](<https://github.com/eclipse/kura/blob/develop/kura/examples/org.eclipse.kura.example.ble.tisensortag/src/main/java/org/eclipse/kura/example/ble/tisensortag/BluetoothLe.java>) - main implementation class.

* [**org.eclipse.kura.example.ble.tisensortag.TiSensorTag.java**](<https://github.com/eclipse/kura/blob/develop/kura/examples/org.eclipse.kura.example.ble.tisensortag/src/main/java/org/eclipse/kura/example/ble/tisensortag/TiSensorTag.java>) - class used to connect with a TI SensorTag.

* [**org.eclipse.kura.example.ble.tisensortag.TiSensorTagGatt.java**](<https://github.com/eclipse/kura/blob/develop/kura/examples/org.eclipse.kura.example.ble.tisensortag/src/main/java/org/eclipse/kura/example/ble/tisensortag/TiSensorTagGatt.java>) - class that describes all the handles and UUIDs to access to the SensorTag sensors.

### OSGI-INF/metatype/org.eclipse.kura.example.ble.tisensortag.BluetoothLe.xml File

The OSGI-INF/metatype/org.eclipse.kura.example.ble.tisensortag.BluetoothLe.xml file describes the parameters for this bundle including the following:

* **scan_time** - specifies the length of time to scan for devices in seconds.

* **period** - specifies the time interval in seconds between two publishes.

* **enableTermometer** - Enable temperature sensor.

* **enableAccelerometer** - Enable accelerometer sensor.

* ...

* **publishTopic** - supplies the topic to publish data to the cloud.

* **iname** - Name of bluetooth adapter.

### org.eclipse.kura.example.ble.tisensortag.BluetoothLe.java File

The org.eclipse.kura.example.ble.tisensortag.BluetoothLe.java file contains the activate, deactivate and updated methods for this bundle. The activate and update methods gets the _BluetoothAdapter_ and schedules the execution of the _performScan_ method every second and _readTiSensorTags_ every user defined period. The following code sample shows part of the code:

```java
this.options = new BluetoothLeOptions(properties);
this.startTime = 0;
if (this.options.isEnableScan()) {
	// re-create the worker
	this.worker = Executors.newScheduledThreadPool(2);

	// Get Bluetooth adapter and ensure it is enabled
	this.bluetoothAdapter = this.bluetoothService.getBluetoothAdapter(this.options.getIname());
	if (this.bluetoothAdapter != null) {
		logger.info("Bluetooth adapter interface => {}", this.options.getIname());
		if (!this.bluetoothAdapter.isEnabled()) {
			logger.info("Enabling bluetooth adapter...");
			this.bluetoothAdapter.enable();
		}
		logger.info("Bluetooth adapter address => {}", this.bluetoothAdapter.getAddress());

		this.scanHandle = this.worker.scheduleAtFixedRate(this::performScan, 0, 1, TimeUnit.SECONDS);
		this.readHandle = this.worker.scheduleAtFixedRate(this::readTiSensorTags, 0, this.options.getPeriod(),
				TimeUnit.SECONDS);
	} else {
		logger.info("Bluetooth adapter {} not found.", this.options.getIname());
	}
}
```

The _performScan_ method manages the start and stop of the scanning procedure as shown below.

```java
void performScan() {

	// Scan for devices
	if (this.bluetoothAdapter.isScanning()) {
		logger.info("bluetoothAdapter.isScanning");
		if (System.currentTimeMillis() - this.startTime >= this.options.getScantime() * 1000) {
			this.bluetoothAdapter.killLeScan();
		}
	} else {
		if (System.currentTimeMillis() - this.startTime >= this.options.getPeriod() * 1000) {
			logger.info("startLeScan");
			this.bluetoothAdapter.startLeScan(this);
			this.startTime = System.currentTimeMillis();
		}
	}

}
```

The _BluetoothLe_ class implements the _org.eclipse.kura.bluetooth.BluetoothLeScanListener_ interface and the _onScanResults_ method is called when the scan procedure ends.
The method filters the scan results and stores the SensorTag devices in a list. Part of the _onScanResults_ method is shown below.

```java
    @Override
    public void onScanResults(List<BluetoothDevice> scanResults) {

        // Scan for TI SensorTag
        for (BluetoothDevice bluetoothDevice : scanResults) {
            logger.info("Address {} Name {}", bluetoothDevice.getAdress(), bluetoothDevice.getName());

            if (bluetoothDevice.getName().contains("SensorTag") && !isSensorTagInList(bluetoothDevice.getAdress())) {
                this.tiSensorTagList.add(new TiSensorTag(bluetoothDevice));
            }
        }

    }
```

The _readTiSensorTags_ is responsible to read the sensors for all the SensorTags contained in the list and publish the resulting data.

```java
private void readTiSensorTags() {
	// connect to TiSensorTags
	this.tiSensorTagList.forEach(myTiSensorTag -> {
		connect(myTiSensorTag);

		if (myTiSensorTag.isConnected()) {
			...

				KuraPayload payload = new KuraPayload();
				payload.setTimestamp(new Date());
				payload.addMetric("Firmware", myTiSensorTag.getFirmwareRevision());
				if (myTiSensorTag.isCC2650()) {
					payload.addMetric("Type", "CC2650");
				} else {
					payload.addMetric("Type", "CC2541");
				}
				readServicesAndCharacteristics(myTiSensorTag);
				readSensors(myTiSensorTag, payload);
				myTiSensorTag.enableIOService();

				publishData(myTiSensorTag, payload);

			...
		} else {
			logger.warn("Cannot connect to TI SensorTag {}.", myTiSensorTag.getBluetoothDevice().getAdress());
		}
	});

}
```

Since it is not possible to poll the status of the buttons on the SensorTag, the BLE example enables the notifications for them.

### org.eclipse.kura.example.ble.sensortag.TiSensorTag.java File

The _org.eclipse.kura.example.ble.sensortag.TiSensorTag.java_ file is used to connect and disconnect to the SensorTag. It also contains the methods to configure and read data from the sensor. The connection method uses the BluetoothGatt Service as shown below:

```java
public boolean connect(String adapterName) {
	this.bluetoothGatt = this.device.getBluetoothGatt();
	boolean connected = false;
	try {
		connected = this.bluetoothGatt.connect(adapterName);
	} catch (KuraException e) {
		logger.error(e.toString());
	}
	if (connected) {
		this.bluetoothGatt.setBluetoothLeNotificationListener(this);
		setFirmwareRevision();
		this.isConnected = true;
		return true;
	} else {
		// If connect command is not executed, close gatttool
		this.bluetoothGatt.disconnect();
		this.isConnected = false;
		return false;
	}
}
```

A set of methods for _reading from_ and _writing to_ the internal register of the device are included in the file. The following code sample presents the methods to manage the temperature sensor.

```java
/*
* Enable temperature sensor
*/
public void enableThermometer() {
	// Write "01" to enable thermometer sensor
	if (this.cc2650) {
		this.bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_TEMP_SENSOR_ENABLE_2650, "01");
	} else {
		this.bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_TEMP_SENSOR_ENABLE_2541, "01");
	}
}

/*
* Disable temperature sensor
*/
public void disableThermometer() {
	// Write "00" disable thermometer sensor
	if (this.cc2650) {
		this.bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_TEMP_SENSOR_ENABLE_2650, "00");
	} else {
		this.bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_TEMP_SENSOR_ENABLE_2541, "00");
	}
}

/*
* Read temperature sensor
*/
public double[] readTemperature() {
	double[] temperatures = new double[2];
	// Read value
	try {
		if (this.cc2650) {
			temperatures = calculateTemperature(
					this.bluetoothGatt.readCharacteristicValue(TiSensorTagGatt.HANDLE_TEMP_SENSOR_VALUE_2650));
		} else {
			temperatures = calculateTemperature(
					this.bluetoothGatt.readCharacteristicValue(TiSensorTagGatt.HANDLE_TEMP_SENSOR_VALUE_2541));
		}
	} catch (KuraException e) {
		logger.error(e.toString());
	}
	return temperatures;
}

/*
* Read temperature sensor by UUID
*/
public double[] readTemperatureByUuid() {
	double[] temperatures = new double[2];
	try {
		temperatures = calculateTemperature(
				this.bluetoothGatt.readCharacteristicValueByUuid(TiSensorTagGatt.UUID_TEMP_SENSOR_VALUE));
	} catch (KuraException e) {
		logger.error(e.toString());
	}
	return temperatures;
}

/*
* Enable temperature notifications
*/
public void enableTemperatureNotifications(TiSensorTagNotificationListener listener) {
	setListener(listener);
	// Write "01:00 to enable notifications
	if (this.cc2650) {
		this.bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_TEMP_SENSOR_NOTIFICATION_2650,
				ENABLE_NOTIFICATIONS);
	} else {
		this.bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_TEMP_SENSOR_NOTIFICATION_2541,
				ENABLE_NOTIFICATIONS);
	}
}

/*
* Disable temperature notifications
*/
public void disableTemperatureNotifications() {
	unsetListener();
	// Write "00:00 to enable notifications
	if (this.cc2650) {
		this.bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_TEMP_SENSOR_NOTIFICATION_2650,
				DISABLE_NOTIFICATIONS);
	} else {
		this.bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_TEMP_SENSOR_NOTIFICATION_2541,
				DISABLE_NOTIFICATIONS);
	}
}

/*
	* Set sampling period (only for CC2650)
	*/
public void setThermometerPeriod(String period) {
	this.bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_TEMP_SENSOR_PERIOD_2650, period);
}

/*
* Calculate temperature
*/
private double[] calculateTemperature(String value) {

	logger.info("Received temperature value: {}", value);

	double[] temperatures = new double[2];

	byte[] valueByte = hexStringToByteArray(value.replace(" ", ""));

	if (this.cc2650) {
		int ambT = shortUnsignedAtOffset(valueByte, 2);
		int objT = shortUnsignedAtOffset(valueByte, 0);
		temperatures[0] = (ambT >> 2) * 0.03125;
		temperatures[1] = (objT >> 2) * 0.03125;
	} else {

		int ambT = shortUnsignedAtOffset(valueByte, 2);
		int objT = shortSignedAtOffset(valueByte, 0);
		temperatures[0] = ambT / 128.0;

		double vobj2 = objT;
		vobj2 *= 0.00000015625;

		double tdie = ambT / 128.0 + 273.15;

		double s0 = 5.593E-14; // Calibration factor
		double a1 = 1.75E-3;
		double a2 = -1.678E-5;
		double b0 = -2.94E-5;
		double b1 = -5.7E-7;
		double b2 = 4.63E-9;
		double c2 = 13.4;
		double tref = 298.15;
		double s = s0 * (1 + a1 * (tdie - tref) + a2 * Math.pow(tdie - tref, 2));
		double vos = b0 + b1 * (tdie - tref) + b2 * Math.pow(tdie - tref, 2);
		double fObj = vobj2 - vos + c2 * Math.pow(vobj2 - vos, 2);
		double tObj = Math.pow(Math.pow(tdie, 4) + fObj / s, .25);

		temperatures[1] = tObj - 273.15;
	}

	return temperatures;
}
```

The _TiSensorTag_ class implements the _org.eclipse.kura.bluetooth.BluetoothLeNotificationListener_ interface and the method _onDataReceived_ is called when a BLE notification is received. In this example the notifications are used only for the buttons. The method is shown below.

```java
public void onDataReceived(String handle, String value) {

	if (this.notificationListener != null) {
		Map<String, Object> values = new HashMap<>();
		if (handle.equals(TiSensorTagGatt.HANDLE_KEYS_STATUS_2541)
				|| handle.equals(TiSensorTagGatt.HANDLE_KEYS_STATUS_2650)) {
			logger.info("Received keys value: {}", value);
			if (!value.equals("00")) {
				values.put("Keys", Integer.parseInt(value));
				this.notificationListener.notify(this.device.getAdress(), values);
			}
		} else if (handle.equals(TiSensorTagGatt.HANDLE_TEMP_SENSOR_VALUE_2541)
				|| handle.equals(TiSensorTagGatt.HANDLE_TEMP_SENSOR_VALUE_2650)) {
			double[] temperatures = calculateTemperature(value);
			values.put("Ambient", temperatures[0]);
			values.put("Target", temperatures[1]);
			this.notificationListener.notify(this.device.getAdress(), values);
		}
	}

}
```

## Deploy and Validate the Bundle

In order to proceed, you need to know the IP address of your embedded gateway that is on the remote target unit. Once you do, follow the mToolkit instructions for installing a single bundle to the remote target device [located here](deploying-bundles.html#remote-target-device). When the installation completes, the bundle starts automatically. You should see a message similar to the one below from **/var/log/kura.log** indicating that the bundle was successfully installed and configured, and started to search for TI SensorTags.

```
2015-11-11 13:38:19,208 [Component Resolve Thread (Bundle 6)] INFO  o.e.k.e.b.t.BluetoothLe - Activating BluetoothLe example...
2015-11-11 13:38:19,382 [Component Resolve Thread (Bundle 6)] INFO  o.e.k.e.b.t.BluetoothLe - Bluetooth adapter interface => hci0
2015-11-11 13:38:19,383 [Component Resolve Thread (Bundle 6)] INFO  o.e.k.e.b.t.BluetoothLe - Bluetooth adapter address => 5C:F3:70:60:63:8F
2015-11-11 13:38:19,383 [Component Resolve Thread (Bundle 6)] INFO  o.e.k.e.b.t.BluetoothLe - Bluetooth adapter le enabled => true
2015-11-11 13:38:19,395 [pool-26-thread-1] INFO  o.e.k.e.b.t.BluetoothLe - startLeScan
2015-11-11 13:38:19,396 [pool-26-thread-1] INFO  o.e.k.l.b.l.BluetoothLeScanner - Starting bluetooth le scan...
2015-11-11 13:38:19,406 [Component Resolve Thread (Bundle 6)] INFO  o.e.k.c.c.ConfigurableComponentTracker - Adding ConfigurableComponent org.eclipse.kura.example.ble.tisensortag.BluetoothLe
2015-11-11 13:38:19,408 [Component Resolve Thread (Bundle 6)] INFO  o.e.k.c.c.ConfigurationServiceImpl - Registration of ConfigurableComponent org.eclipse.kura.example.ble.tisensortag.BluetoothLe by org.eclipse.kura.core.configuration.ConfigurationServiceImpl@19cb019...
2015-11-11 13:38:19,424 [Component Resolve Thread (Bundle 6)] INFO  o.e.k.c.c.ConfigurationServiceImpl - Registering org.eclipse.kura.example.ble.tisensortag.BluetoothLe with ocd: org.eclipse.kura.core.configuration.metatype.Tocd@106b1d9 ...
2015-11-11 13:38:19,426 [Component Resolve Thread (Bundle 6)] INFO  o.e.k.c.c.ConfigurationServiceImpl - Registration Completed for Component org.eclipse.kura.example.ble.tisensortag.BluetoothLe.
2015-11-11 13:38:20,394 [pool-26-thread-1] INFO  o.e.k.e.b.t.BluetoothLe - m_bluetoothAdapter.isScanning
2015-11-11 13:38:21,394 [pool-26-thread-1] INFO  o.e.k.e.b.t.BluetoothLe - m_bluetoothAdapter.isScanning
2015-11-11 13:38:22,394 [pool-26-thread-1] INFO  o.e.k.e.b.t.BluetoothLe - m_bluetoothAdapter.isScanning
2015-11-11 13:38:23,394 [pool-26-thread-1] INFO  o.e.k.e.b.t.BluetoothLe - m_bluetoothAdapter.isScanning
2015-11-11 13:38:24,394 [pool-26-thread-1] INFO  o.e.k.e.b.t.BluetoothLe - m_bluetoothAdapter.isScanning
2015-11-11 13:38:25,394 [pool-26-thread-1] INFO  o.e.k.e.b.t.BluetoothLe - m_bluetoothAdapter.isScanning
2015-11-11 13:38:25,396 [pool-26-thread-1] INFO  o.e.k.l.b.l.BluetoothLeScanner - Killing hcitool...
2015-11-11 13:38:25,449 [BluetoothProcess Input Stream Gobbler] INFO  o.e.k.l.b.l.BluetoothLeScanner - LE Scan ...
2015-11-11 13:38:25,450 [BluetoothProcess Input Stream Gobbler] INFO  o.e.k.l.b.l.BluetoothLeScanner - 7F:D8:F8:45:6B:C2 (unknown)
2015-11-11 13:38:25,452 [BluetoothProcess Input Stream Gobbler] INFO  o.e.k.l.b.l.BluetoothLeScanner - 7F:D8:F8:45:6B:C2 (unknown)
2015-11-11 13:38:25,453 [BluetoothProcess Input Stream Gobbler] INFO  o.e.k.l.b.l.BluetoothLeScanner - BC:6A:29:AE:CC:96 (unknown)
2015-11-11 13:38:25,454 [BluetoothProcess Input Stream Gobbler] INFO  o.e.k.l.b.l.BluetoothLeScanner - BC:6A:29:AE:CC:96 SensorTag
2015-11-11 13:38:25,455 [BluetoothProcess Input Stream Gobbler] INFO  o.e.k.l.b.l.BluetoothLeScanner - 68:64:4B:3F:04:9B (unknown)
2015-11-11 13:38:25,456 [BluetoothProcess Input Stream Gobbler] INFO  o.e.k.l.b.l.BluetoothLeScanner - 68:64:4B:3F:04:9B (unknown)
2015-11-11 13:38:25,457 [BluetoothProcess Input Stream Gobbler] INFO  o.e.k.l.b.l.BluetoothLeScanner - 18:EE:69:15:21:B0 (unknown)
2015-11-11 13:38:25,458 [BluetoothProcess Input Stream Gobbler] INFO  o.e.k.l.b.l.BluetoothLeScanner - 18:EE:69:15:21:B0 (unknown)
2015-11-11 13:38:25,459 [BluetoothProcess Input Stream Gobbler] INFO  o.e.k.l.b.l.BluetoothLeScanner - m_scanResult.add 68:64:4B:3F:04:9B - (unknown)
2015-11-11 13:38:25,464 [BluetoothProcess Input Stream Gobbler] INFO  o.e.k.l.b.l.BluetoothLeScanner - m_scanResult.add 18:EE:69:15:21:B0 - (unknown)
2015-11-11 13:38:25,465 [BluetoothProcess Input Stream Gobbler] INFO  o.e.k.l.b.l.BluetoothLeScanner - m_scanResult.add BC:6A:29:AE:CC:96 - SensorTag
2015-11-11 13:38:25,466 [BluetoothProcess Input Stream Gobbler] INFO  o.e.k.l.b.l.BluetoothLeScanner - m_scanResult.add 7F:D8:F8:45:6B:C2 - (unknown)
2015-11-11 13:38:25,467 [BluetoothProcess Input Stream Gobbler] INFO  o.e.k.e.b.t.BluetoothLe - Address 68:64:4B:3F:04:9B Name (unknown)
2015-11-11 13:38:25,467 [BluetoothProcess Input Stream Gobbler] INFO  o.e.k.e.b.t.BluetoothLe - Found device = 68:64:4B:3F:04:9B
2015-11-11 13:38:25,468 [BluetoothProcess Input Stream Gobbler] INFO  o.e.k.e.b.t.BluetoothLe - Address 18:EE:69:15:21:B0 Name (unknown)
2015-11-11 13:38:25,468 [BluetoothProcess Input Stream Gobbler] INFO  o.e.k.e.b.t.BluetoothLe - Found device = 18:EE:69:15:21:B0
2015-11-11 13:38:25,469 [BluetoothProcess Input Stream Gobbler] INFO  o.e.k.e.b.t.BluetoothLe - Address BC:6A:29:AE:CC:96 Name SensorTag
2015-11-11 13:38:25,469 [BluetoothProcess Input Stream Gobbler] INFO  o.e.k.e.b.t.BluetoothLe - TI SensorTag BC:6A:29:AE:CC:96 found.
2015-11-11 13:38:25,470 [BluetoothProcess Input Stream Gobbler] INFO  o.e.k.e.b.t.BluetoothLe - Address 7F:D8:F8:45:6B:C2 Name (unknown)
2015-11-11 13:38:25,470 [BluetoothProcess Input Stream Gobbler] INFO  o.e.k.e.b.t.BluetoothLe - Found device = 7F:D8:F8:45:6B:C2
2015-11-11 13:38:25,470 [BluetoothProcess Input Stream Gobbler] INFO  o.e.k.e.b.t.BluetoothLe - Connecting to TiSensorTag...
2015-11-11 13:38:25,475 [BluetoothProcess Input Stream Gobbler] INFO  o.e.k.l.b.l.BluetoothGattImpl - Sending connect message...
2015-11-11 13:38:25,859 [DnsMonitorServiceImpl] WARN  o.e.k.n.a.m.DnsMonitorServiceImpl - Not Setting DNS servers to empty
2015-11-11 13:38:26,883 [BluetoothProcess Input Stream Gobbler] INFO  o.e.k.e.b.t.TiSensorTag - Received temperature value: 7f fe fc 0c
2015-11-11 13:38:26,885 [BluetoothProcess Input Stream Gobbler] INFO  o.e.k.e.b.t.BluetoothLe - Ambient: 25.96875 Target: 20.801530505264225
2015-11-11 13:38:28,028 [BluetoothProcess Input Stream Gobbler] INFO  o.e.k.e.b.t.TiSensorTag - Received accelerometer value: ff 06 42
2015-11-11 13:38:28,029 [BluetoothProcess Input Stream Gobbler] INFO  o.e.k.e.b.t.BluetoothLe - Acc X: -0.015625 Acc Y: 0.09375 Acc Z: -1.03125
2015-11-11 13:38:29,182 [BluetoothProcess Input Stream Gobbler] INFO  o.e.k.e.b.t.TiSensorTag - Received barometer value: e8 6a 22 56
2015-11-11 13:38:29,183 [BluetoothProcess Input Stream Gobbler] INFO  o.e.k.e.b.t.BluetoothLe - Humidity: 36.053864
2015-11-11 13:38:30,327 [BluetoothProcess Input Stream Gobbler] INFO  o.e.k.e.b.t.TiSensorTag - Received magnetometer value: e5 f6 d6 fc 62 04
2015-11-11 13:38:30,328 [BluetoothProcess Input Stream Gobbler] INFO  o.e.k.e.b.t.BluetoothLe - Mag X: 203.43018 Mag Y: 320.43457 Mag Z: 765.7471
2015-11-11 13:38:32,623 [BluetoothProcess Input Stream Gobbler] INFO  o.e.k.e.b.t.TiSensorTag - Received pressure value: 1d fd dd 99
2015-11-11 13:38:32,625 [BluetoothProcess Input Stream Gobbler] INFO  o.e.k.e.b.t.BluetoothLe - Pre : 99334.60900594086
2015-11-11 13:38:33,767 [BluetoothProcess Input Stream Gobbler] INFO  o.e.k.e.b.t.TiSensorTag - Received gyro value: cc 01 1d ff d2 ff
2015-11-11 13:38:33,769 [BluetoothProcess Input Stream Gobbler] INFO  o.e.k.e.b.t.BluetoothLe - Gyro X: -101.55487 Gyro Y: -58.58612 Gyro Z: -87.898254
2015-11-11 13:38:33,769 [BluetoothProcess Input Stream Gobbler] INFO  o.e.k.e.b.t.TiSensorTag - Not optical sensor on CC2541.
2015-11-11 13:38:34,770 [BluetoothProcess Input Stream Gobbler] INFO  o.e.k.e.b.t.TiSensorTag - Not optical sensor on CC2541.
2015-11-11 13:38:34,771 [BluetoothProcess Input Stream Gobbler] INFO  o.e.k.e.b.t.BluetoothLe - Light: 0.0
```

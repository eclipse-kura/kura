---
layout: page
title:  "Bluetooth LE example"
date:   2014-08-10 11:28:21
categories: [doc]
---

[Overview](#overview_1)

-   [Prerequisites](#prerequisites)

[Prepare the Embedded Device](#prepare_the_embedded_device_1)

[SensorTag Communication via Command Line](#sensortag_command_line_1)

[BLE Bundle for TI SensorTag](#bundle_sensortag_1)

-   [Develop the BLE Bundle](#develop_the_ble_bundle_1)

    -   [OSGI-INF/metatype/org.eclipse.kura.example.ble.tisensortag.BluetoothLe.xml File](#OSGI-INF/metatype_1)

    -   [org.eclipse.kura.example.ble.tisensortag.BluetoothLe.java File](#BluetoothLe_1)

    -   [org.eclipse.kura.example.ble.sensortag.TiSensorTag.java File](#TiSensorTag_1)

-   [Deploy and Validate the Bundle](#deploy_and_validate_the_bundle_1)

<span id="overview" class="anchor"><span id="overview_1" class="anchor"></span></span></span>Overview
==========================================================================================================================================

This section provides an example of how to develop a simple bundle that discovers and connects to a Smart device (BLE), retrieves data from it, and publishes the results to the cloud. This example uses the TI SensorTag based on CC2541 or CC2650. For more information about this device, refer to <http://www.ti.com/tool/cc2541dk-sensor> and <http://www.ti.com/ww/en/wireless_connectivity/sensortag2015/index.html>.

You will learn how to perform the following functions:

* Prepare the embedded device to communicate with a Smart device

* Develop a bundle retrieves data from the device

* Optionally publish the data in the cloud

## <span id="prerequisites" class="anchor"><span id="prerequisites_1" class="anchor"></span></span>Prerequisites

* [Setting up Kura Development Environment](kura-setup.html)

* Hardware

  * Use an embedded device running Kura with Bluetooth 4.0 (LE) capabilities

  * Use at least one TI SensorTag

This tutorial uses a Raspberry Pi Type B with a LMTechnologies LM506 Bluetooth 4.0 <http://lm-technologies.com/wireless-adapters/lm506-class-1-bluetooth-4-0-usb-adapter/> dongle.

<span id="prepare_the_embedded_device" class="anchor"><span id="prepare_the_embedded_device_1" class="anchor"></span></span>Prepare the Embedded Device
==============================================================================================================================================

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
sudo ./configure --disable-systemd
sudo make
sudo make install
```

Finally, change the location of the hciconfig and gatttool commands:

```
sudo mv /usr/local/sbin/hciconfig /usr/sbin
sudo mv /usr/local/bin/gatttool /usr/sbin
```

<span id="sensortag_command_line" class="anchor"><span id="sensortag_command_line_1" class="anchor"></span></span>SensorTag Communication via Command Line
==========================================================================================================================================================

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

In order to read the sensor values from the SensorTag, you need to write some registers on the device. For details, please refer to the CC2541 user guide:  <http://processors.wiki.ti.com/index.php/SensorTag_User_Guide>. Note that the reported BLE handles are not up-to-date on this page.

Also refer to this updated attribute table: <http://processors.wiki.ti.com/images/archive/a/a8/20130111154127!BLE_SensorTag_GATT_Server.pdf>.
For the CC2650 please refer to <http://www.ti.com/ww/en/wireless_connectivity/sensortag2015/tearDown.html#main>.

The example that follows shows the procedure for retrieving the temperature value from the SensorTag based on the CC2541.

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
<span id="bundle_sensortag" class="anchor"><span id="bundle_sensortag_1" class="anchor"></span></span>BLE Bundle for TI SensorTag
=======================================================================================================================================

The BLE bundle performs the following operations:

* Starts a scan for smart devices (lescan)

* Selects all the TI SensorTag in range

* Connects to the discovered SensorTags and discovers their capabilities

* Reads data from all the sensors onboard and writes the values in the log file

## <span id="develop_the_ble_bundle" class="anchor"><span id="develop_the_ble_bundle_1" class="anchor"></span></span>Develop the BLE Bundle

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

### <span id="OSGI-INF/metatype" class="anchor"><span id="OSGI-INF/metatype_1" class="anchor"></span></span>OSGI-INF/metatype/org.eclipse.kura.example.ble.tisensortag.BluetoothLe.xml File

The OSGI-INF/metatype/org.eclipse.kura.example.ble.tisensortag.BluetoothLe.xml file describes the parameters for this bundle including the following:

* **scan_time** - specifies the length of time to scan for devices in seconds.

* **period** - specifies the time interval in seconds between two publishes.

* **enableTermometer** - Enable temperature sensor.

* **enableAccelerometer** - Enable accelerometer sensor.

* ...

* **publishTopic** - supplies the topic to publish data to the cloud.

* **iname** - Name of bluetooth adapter.

### <span id="BluetoothLe" class="anchor"><span id="BluetoothLe_1" class="anchor"></span></span>org.eclipse.kura.example.ble.tisensortag.BluetoothLe.java File

The org.eclipse.kura.example.ble.tisensortag.BluetoothLe.java file contains the activate and deactivate methods for this bundle. The activate method gets the _BluetoothAdapter_ and defines a _ScheduledExecutorService_, which schedules the execution of the _checkScan_ method every second. The following code sample shows part of the activate method:

```
m_tiSensorTagList = new ArrayList<TiSensorTag>();
m_worker = Executors.newSingleThreadScheduledExecutor();

try {
	m_cloudClient = m_cloudService.newCloudClient(APP_ID);
	m_cloudClient.addCloudClientListener(this);

	// Get Bluetooth adapter and ensure it is enabled
	m_bluetoothAdapter = m_bluetoothService.getBluetoothAdapter(iname);
	if (m_bluetoothAdapter != null) {
		s_logger.info("Bluetooth adapter interface => " + iname);
		s_logger.info("Bluetooth adapter address => " + m_bluetoothAdapter.getAddress());
		s_logger.info("Bluetooth adapter le enabled => " + m_bluetoothAdapter.isLeReady());

		if (!m_bluetoothAdapter.isEnabled()) {
			s_logger.info("Enabling bluetooth adapter...");
			m_bluetoothAdapter.enable();
			s_logger.info("Bluetooth adapter address => " + m_bluetoothAdapter.getAddress());
		}
		m_startTime = 0;
		m_connected = false;
		m_handle = m_worker.scheduleAtFixedRate(new Runnable() {		
			@Override
			public void run() {
				checkScan();
			}
		}, 0, 1, TimeUnit.SECONDS);
	}
	else s_logger.warn("No Bluetooth adapter found ...");
} catch (Exception e) {
	s_logger.error("Error starting component", e);
	throw new ComponentException(e);
}
```

The _checkScan_ method manages the start and stop of the scanning procedure as shown below.

```
void checkScan() {

	// Scan for devices
	if(m_bluetoothAdapter.isScanning()) {
		s_logger.info("m_bluetoothAdapter.isScanning");
		if((System.currentTimeMillis() - m_startTime) >= (m_scantime * 1000)) {
			m_bluetoothAdapter.killLeScan();
		}
	}
	else {
		if((System.currentTimeMillis() - m_startTime) >= (m_period * 1000)) {
			s_logger.info("startLeScan");
			m_bluetoothAdapter.startLeScan(this);
			m_startTime = System.currentTimeMillis();
		}
	}

}
```

The _BluetoothLe_ class implements the _org.eclipse.kura.bluetooth.BluetoothLeScanListener_ interface and the _onScanResults_ method is called when the scan procedure ends. The method filters the scan results and stores the SensorTag devices in a list. For each device in the list a connection is opened and the selected sensors are read. Part of the _onScanResults_ method is shown below.

```
public void onScanResults(List<BluetoothDevice> scanResults) {

	// Scan for TI SensorTag
	for (BluetoothDevice bluetoothDevice : scanResults) {
		s_logger.info("Address " + bluetoothDevice.getAdress() + " Name " + bluetoothDevice.getName());

		if (bluetoothDevice.getName().contains("SensorTag")) {
			s_logger.info("TI SensorTag " + bluetoothDevice.getAdress() + " found.");
			if (!searchSensorTagList(bluetoothDevice.getAdress())){
				TiSensorTag tiSensorTag = new TiSensorTag(bluetoothDevice);
				m_tiSensorTagList.add(tiSensorTag);
			}
		}
		else {
			s_logger.info("Found device = " + bluetoothDevice.getAdress());
		}
	}

	// connect to TiSensorTags
	for (TiSensorTag myTiSensorTag : m_tiSensorTagList) {

		if (!myTiSensorTag.isConnected()) {
			s_logger.info("Connecting to TiSensorTag...");
			m_connected = myTiSensorTag.connect();
		}
		else {
			s_logger.info("TiSensorTag already connected!");
			m_connected = true;
		}

		if (m_connected) {

			myTiSensorTag.setFirmwareRevision(myTiSensorTag.firmwareRevision());

			if (enableTemp) {
				myTiSensorTag.enableTermometer();
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				double[] temperatures = myTiSensorTag.readTemperature();

				s_logger.info("Ambient: " + temperatures[0] + " Target: " + temperatures[1]);
			}

      ...

  }
  else {
    s_logger.info("Cannot connect to TI SensorTag " + myTiSensorTag.getBluetoothDevice().getAdress() + ".");
  }

}
```

Since it is not possible to poll the status of the buttons on the SensorTag, the BLE example enables the notifications for them.

### <span id="TiSensorTag" class="anchor"><span id="TiSensorTag_1" class="anchor"></span></span>org.eclipse.kura.example.ble.sensortag.TiSensorTag.java File

The org.eclipse.kura.example.ble.sensortag.TiSensorTag.java file is used to connect and disconnect to the SensorTag. It also contains the methods to configure and read data from the sensor. The connection method uses the BluetoothGatt Service as shown below:

```
public boolean connect() {
    m_bluetoothGatt = m_device.getBluetoothGatt();
    boolean connected = m_bluetoothGatt.connect();
    if(connected) {
        m_bluetoothGatt.setBluetoothLeNotificationListener(this);
        m_connected = true;
        return true;
    }
    else {
    	// If connect command is not executed, close gatttool
    	m_bluetoothGatt.disconnect();
    	m_connected = false;
        return false;
    }
}
```

A set of methods for _reading from_ and _writing to_ the internal register of the device are included in the file. The following code sample presents the methods to manage the temperature sensor.

```
/*
 * Enable temperature sensor
 */
public void enableTermometer() {
	// Write "01" to enable temperature sensor
	if (CC2650)
		m_bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_TEMP_SENSOR_ENABLE_2650, "01");
	else
		m_bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_TEMP_SENSOR_ENABLE_2541, "01");
}

/*
 * Disable temperature sensor
 */
public void disableTermometer() {
	// Write "00" disable temperature sensor
	if (CC2650)
		m_bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_TEMP_SENSOR_ENABLE_2650, "00");
	else
		m_bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_TEMP_SENSOR_ENABLE_2541, "00");
}

/*
 * Read temperature sensor
 */
public double[] readTemperature() {
	// Read value
	if (CC2650)
		return calculateTemperature(m_bluetoothGatt.readCharacteristicValue(TiSensorTagGatt.HANDLE_TEMP_SENSOR_VALUE_2650));
	else
		return calculateTemperature(m_bluetoothGatt.readCharacteristicValue(TiSensorTagGatt.HANDLE_TEMP_SENSOR_VALUE_2541));
}

/*
 * Read temperature sensor by UUID
 */
public double[] readTemperatureByUuid() {
	return calculateTemperature(m_bluetoothGatt.readCharacteristicValueByUuid(TiSensorTagGatt.UUID_TEMP_SENSOR_VALUE));
}

/*
 * Enable temperature notifications
 */
public void enableTemperatureNotifications() {
	// Write "01:00 to enable notifications
	if (CC2650)
		m_bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_TEMP_SENSOR_NOTIFICATION_2650, "01:00");
	else
		m_bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_TEMP_SENSOR_NOTIFICATION_2541, "01:00");
}

/*
 * Disable temperature notifications
 */
public void disableTemperatureNotifications() {
	// Write "00:00 to enable notifications
	if (CC2650)
		m_bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_TEMP_SENSOR_NOTIFICATION_2650, "00:00");
	else
		m_bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_TEMP_SENSOR_NOTIFICATION_2541, "00:00");
}

/*
 * Set sampling period (only for CC2650)
 */
public void setTermometerPeriod(String period) {
	m_bluetoothGatt.writeCharacteristicValue(TiSensorTagGatt.HANDLE_TEMP_SENSOR_PERIOD_2650, period);
}

/*
 * Calculate temperature
 */
private double[] calculateTemperature(String value) {

	s_logger.info("Received temperature value: " + value);

	double[] temperatures = new double[2];

	String[] tmp = value.split("\\s");
	int lsbObj = Integer.parseInt(tmp[0], 16);
	int msbObj = Integer.parseInt(tmp[1], 16);
	int lsbAmb = Integer.parseInt(tmp[2], 16);
	int msbAmb = Integer.parseInt(tmp[3], 16);

	int objT = (unsignedToSigned(msbObj) << 8) + lsbObj;
	int ambT = (msbAmb << 8) + lsbAmb;

	temperatures[0] = ambT / 128.0;

	if (CC2650) {
		temperatures[1] = objT / 128.0;
	} else {
		double Vobj2 = objT;
		Vobj2 *= 0.00000015625;

		double Tdie = (ambT / 128.0) + 273.15;

		double S0 = 5.593E-14;	// Calibration factor
		double a1 = 1.75E-3;
		double a2 = -1.678E-5;
		double b0 = -2.94E-5;
		double b1 = -5.7E-7;
		double b2 = 4.63E-9;
		double c2 = 13.4;
		double Tref = 298.15;
		double S = S0*(1+a1*(Tdie - Tref)+a2*Math.pow((Tdie - Tref),2));
		double Vos = b0 + b1*(Tdie - Tref) + b2*Math.pow((Tdie - Tref),2);
		double fObj = (Vobj2 - Vos) + c2*Math.pow((Vobj2 - Vos),2);
		double tObj = Math.pow(Math.pow(Tdie,4) + (fObj/S),.25);

		temperatures[1] = tObj - 273.15;
	}

    return temperatures;
}
```

The _TiSensorTag_ class implements the org.eclipse.kura.bluetooth.BluetoothLeNotificationListener interface and the method _onDataReceived_ is called when a BLE notification is received. In this example the notifications are used only for the buttons. The method is shown below.

```
public void onDataReceived(String handle, String value) {

	if (handle.equals(TiSensorTagGatt.HANDLE_KEYS_STATUS_2541) || handle.equals(TiSensorTagGatt.HANDLE_KEYS_STATUS_2650)) {
		s_logger.info("Received keys value: " + value);
		if (!value.equals("00"))
			BluetoothLe.doPublishKeys(m_device.getAdress(), Integer.parseInt(value) );
	}

}
```

## <span id="deploy_and_validate_the_bundle" class="anchor"><span id="deploy_and_validate_the_bundle_1" class="anchor"></span></span>Deploy and Validate the Bundle

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

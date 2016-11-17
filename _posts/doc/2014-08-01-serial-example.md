---
layout: page
title:  "Serial Example"
categories: [doc]
---

[Overview](#_Overview_1)

*  [Prerequisites](#prerequisites)

[Serial Communication with Kura](#_Serial_Communication_with)

*  [Hardware Setup](#hardware-setup)

*  [Determine Serial Device Nodes](#determine-serial-device-nodes)

*  [Implement the Bundle](#implement-the-bundle)

    *  [META-INF/MANIFEST.MF File](#_META-INF/MANIFEST.MF_File)

    *  [OSGI-INF/component.xml File](#osgi-infcomponent.xml-file)

    *  [OSGI-INF/metatype/org.eclipse.kura.example.serial.SerialExample.xml
        File](#_OSGI-INF/metatype/org.eclipse.kura.exam)

    *  [org.eclipse.kura.example.serial.SerialExample.java
        File](#_org.eclipse.kura.example.serial.SerialE)

*  [Export the Bundle](#_Export_the_Bundle)

*  [Deploy the Bundle](#deploy-the-bundle)

*  [Validate the Bundle](#validate-the-bundle)

## Overview

This section provides an example of how to create a Kura bundle that
will communicate with a serial device. In this example, you will
communicate with a simple terminal emulator to demonstrate both
transmitting and receiving data. You will learn how to perform the
following functions:

*  Create a plugin that communicates to serial devices

*  Export the bundle

*  Install the bundle on the remote device

*  Test the communication with minicom where, minicom is acting as an
    attached serial device such as an NFC reader, GPS device, or some
    other ASCII-based communication device

### Prerequisites

*  Setting up Kura Development Environment (refer to section *2.01
    Setting up the Kura Development Environment*)

*  Hello World Using the Kura Logger (refer to section *2.02 Hello
    World using the Kura Logger*)

*  Hardware

    *  Use an embedded device running Kura with two available serial
        ports.
        (If the device does not have a serial port, USB to serial
        adapters can be used.)

    *  Ensure minicom is installed on the embedded device.

## Serial Communication with Kura

This section of the tutorial covers setting up the hardware, determining
serial port device nodes, implementing the basic serial communication
bundle, deploying the bundle, and validating its functionality. After
completing this section, you should be able to communicate with any
ASCII-based serial device attached to a Kura-enabled embedded gateway.
In this example, we are using ASCII for clarity, but these same
techniques can be used to communicate with serial devices that
communicate using binary protocols.

### Hardware Setup

Your setup requirements will depend on your hardware platform. At a
minimum, you will need two serial ports with a null modem serial,
crossover cable connecting them.

*  If your platform has integrated serial ports, you only need to
    connect them using a null modem serial cable.

*  If you do not have integrated serial ports on your platform, you
    will need to purchase USB-to-Serial adapters. It is recommended to
    use a USB-to-Serial adapter with either the PL2303 or FTDI chipset,
    but others may work depending on your hardware platform and
    underlying Linux support. Once you have attached these adapters to
    your device, you can attach the null modem serial cable between the
    two ports.

### Determine Serial Device Nodes

This step is hardware specific. If your hardware device has integrated
serial ports, contact your hardware device manufacturer or review the
documentation to find out how the ports are named in the operating
system. The device identifiers should be similar to the following:

```
/dev/ttyS*xx*
/dev/ttyUSB*xx*
/dev/ttyACM*xx*
```

If you are using USB-to-Serial adapters, Linux usually allocates the
associated device nodes dynamically at the time of insertion. In order
to determine what they are, run the following command at a terminal on
the embedded gateway:

```
tail -f /var/log/syslog
```

{% include alerts.html message="Depending on your specific Linux implementation, other possible
log files may be: /var/log/kern.log, /var/log/kernel, or /var/log/dmesg." %}

With the above command running, insert your USB-to-Serial adapter. You
should see output similar to the following:

```
root@localhost:/root> tail -f /var/log/syslog
Aug 15 18:43:47 localhost kernel: usb 3-2: new full speed USB device using uhci_hcd and address 3
Aug 15 18:43:47 localhost kernel: pl2303 3-2:1.0: pl2303 converter detected
Aug 15 18:43:47 localhost kernel: usb 3-2: pl2303 converter now attached to ttyUSB10
```

In this example, our device is a PL2303-compatible device and is
allocated a device node of “/dev/ttyUSB10”. While your results may
differ, the key is to identify the “tty” device that was allocated. For
the rest of this tutorial, this device will be referred to as
[device_node_1], which in this example is /dev/ttyUSB10. During
development, it is also important to keep in mind that these values are
dynamic; therefore, from one boot to the next and one insertion to the
next, these values may change. To stop ‘tail’ from running in your
console, escape with ‘<CTRL> c’.

If you are using two USB-to-Serial adapters, repeat the above procedure
for the second serial port. The resulting device node will be referred
to as [device_node_2].

### Implement the Bundle

Now that you have two serial ports connected to each other, you are
ready to implement the code. You will use the same general method that
is described in section *2.02 Hello World Using the Kura Logger* to
implement the serial example with the following exceptions: 1) the
process to export the OSGi bundle (described in the [Export the
Bundle](#_Export_the_Bundle_1) section) will have an additional step,
and 2) the actual code in this example will have the following
differences:

*  The new Plug-in Project is named “org.eclipse.kura.example.serial”

*  A class named “SerialExample” is created in the
    org.eclipse.kura.example.serial project

*  The following bundles are included in the Automated Management of
    Dependencies section in the MANIFEST.MF:

    *  javax.comm

    *  javax.microedition.io

    *  org.eclipse.kura.cloud

    *  org.eclipse.kura.comm

    *  org.eclipse.kura.configuration

    *  org.osgi.service.component

    *  org.osgi.service.io

    *  org.slf4j

The following files need to be implemented:

*  META-INF/MANIFEST.MF – OSGI manifest that describes the bundle
    and its dependencies

*  OSGI-INF/component.xml – declarative services definition that
    describe what services are exposed and consumed by this bundle

*  OSGI-INF/metatype/org.eclipse.kura.example.serial.SerialExample.xml
    – configuration description of the bundle and its parameters, types,
    and defaults

*  org.eclipse.kura.example.serial.SerialExample.java – main
    implementation class

#### META-INF/MANIFEST.MF File

The META-INF/MANIFEST.MF file should appear as shown below when
complete:

NOTE: Whitespace is significant in this file. Make sure yours matches
this file exactly with the exception that
RequiredExecutionEnvironment may be JavaSE-1.6 or JavaSE-1.7,
depending on the Java installation of your device.

```
Manifest-Version: 1.0
Bundle-ManifestVersion: 2
Bundle-Name: Serial
Bundle-SymbolicName: org.eclipse.kura.example.serial
Bundle-Version: 1.0.0.qualifier
Bundle-RequiredExecutionEnvironment: JavaSE-1.7
Service-Component: OSGI-INF/component.xml
Bundle-ActivationPolicy: lazy
Import-Package: javax.comm;version="1.2.0",
  javax.microedition.io;resolution:=optional,
  org.eclipse.kura.cloud;version="0.2.0",
  org.eclipse.kura.comm;version="0.2.0",
  org.eclipse.kura.configuration;version="0.2.0",
  org.osgi.service.component;version="1.2.0",
  org.osgi.service.io;version="1.0.0",
  org.slf4j;version="1.6.4"
Bundle-ClassPath: .

```

In addition, the build.propertiesfile should have org.eclipse.equinox.io listed as an additional bundle similar to below:

```
additional.bundles = org.eclipse.equinox.io
```

#### OSGI-INF/component.xml File

The OSGI-INF/component.xml should appear as shown below when complete:

```
<?xml version="1.0" encoding="UTF-8"?>
<scr:component xmlns:scr="http://www.osgi.org/xmlns/scr/v1.1.0"
  name="org.eclipse.kura.example.serial.SerialExample" activate="activate"
  deactivate="deactivate" modified="updated" enabled="true" immediate="true"
  configuration-policy="require">

  <implementation class="org.eclipse.kura.example.serial.SerialExample"/>
  <property name="service.pid" type="String" value="org.eclipse.kura.example.serial.SerialExample"/>

  <service>
    <provide interface="org.eclipse.kura.example.serial.SerialExample"/>
  </service>
  <reference bind="setConnectionFactory" cardinality="1..1"
    interface="org.osgi.service.io.ConnectionFactory" name="ConnectionFactory"
    policy="static" unbind="unsetConnectionFactory" />
</scr:component>
```

#### OSGI-INF/metatype/org.eclipse.kura.example.serial.SerialExample.xml File

The OSGI-INF/metatype/org.eclipse.kura.example.serial.SerialExample.xml
file should appear as shown below when complete:

```
<?xml version="1.0" encoding="UTF-8"?>
<MetaData xmlns="http://www.osgi.org/xmlns/metatype/v1.2.0" localization="en_us">
  <OCD id="org.eclipse.kura.example.serial.SerialExample"
    name="SerialExample"
    description="Example of a Configuring KURA Application echoing data read from the serial port.">

    <Icon resource="http://sphotos-a.xx.fbcdn.net/hphotos-ash4/p480x480/408247_10151040905591065_1989684710_n.jpg" size="32"/>

    <AD id="serial.device"
        name="serial.device"
        type="String"
        cardinality="0"
        required="false"
        description="Name of the serial device (e.g. /dev/ttyS0, /dev/ttyACM0, /dev/ttyUSB0)."/>

    <AD id="serial.baudrate"
        name="serial.baudrate"
        type="String"
        cardinality="0"
        required="true"
        default="9600"
        description="Baudrate.">
        <Option label="9600" value="9600"/>
        <Option label="19200" value="19200"/>
        <Option label="38400" value="38400"/>
        <Option label="57600" value="57600"/>
        <Option label="115200" value="115200"/>
    </AD>

    <AD id="serial.data-bits"
        name="serial.data-bits"
        type="String"
        cardinality="0"
        required="true"
        default="8"
        description="Data bits.">
        <Option label="7" value="7"/>
        <Option label="8" value="8"/>
    </AD>

    <AD id="serial.parity"
        name="serial.parity"
        type="String"
        cardinality="0"
        required="true"
        default="none"
        description="Parity.">
        <Option label="none" value="none"/>
        <Option label="even" value="even"/>
        <Option label="odd" value="odd"/>
    </AD>

    <AD id="serial.stop-bits"
        name="serial.stop-bits"
        type="String"
        cardinality="0"
        required="true"
        default="1"
        description="Stop bits.">
        <Option label="1" value="1"/>
        <Option label="2" value="2"/>
    </AD>

  </OCD>
  <Designate pid="org.eclipse.kura.example.serial.SerialExample">
    <Object ocdref="org.eclipse.kura.example.serial.SerialExample"/>
  </Designate>
</MetaData>
```

#### org.eclipse.kura.example.serial.SerialExample.java File

The org.eclipse.kura.example.serial.SerialExample.java file should
appear as shown below when complete:

```java
package org.eclipse.kura.example.serial;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.io.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SerialExample implements ConfigurableComponent {

  private static final Logger s_logger = LoggerFactory.getLogger(SerialExample.class);

  private static final String SERIAL_DEVICE_PROP_NAME= "serial.device";
  private static final String SERIAL_BAUDRATE_PROP_NAME= "serial.baudrate";
  private static final String SERIAL_DATA_BITS_PROP_NAME= "serial.data-bits";
  private static final String SERIAL_PARITY_PROP_NAME= "serial.parity";
  private static final String SERIAL_STOP_BITS_PROP_NAME= "serial.stop-bits";

  private ConnectionFactory m_connectionFactory;
  private CommConnection m_commConnection;
  private InputStream m_commIs;
  private OutputStream m_commOs;
  private ScheduledThreadPoolExecutor m_worker;
  private Future<?> m_handle;
  private Map<String, Object> m_properties;

  // ----------------------------------------------------------------
  //
  // Dependencies
  //
  // ----------------------------------------------------------------
  public void setConnectionFactory(ConnectionFactory connectionFactory) {
    this.m_connectionFactory = connectionFactory;
  }

  public void unsetConnectionFactory(ConnectionFactory connectionFactory) {
    this.m_connectionFactory = null;
  }

  // ----------------------------------------------------------------
  //
  // Activation APIs
  //
  // ----------------------------------------------------------------

  protected void activate(ComponentContext componentContext, Map<String,Object> properties) {
    s_logger.info("Activating SerialExample...");

    m_worker = new ScheduledThreadPoolExecutor(1);
    m_properties = new HashMap<String, Object>();
    doUpdate(properties);
    s_logger.info("Activating SerialExample... Done.");
  }

  protected void deactivate(ComponentContext componentContext) {
    s_logger.info("Deactivating SerialExample...");

    // shutting down the worker and cleaning up the properties
    m_handle.cancel(true);
    m_worker.shutdownNow();
    //close the serial port
    closePort();
    s_logger.info("Deactivating SerialExample... Done.");
  }

  public void updated(Map<String,Object> properties) {
    s_logger.info("Updated SerialExample...");

    doUpdate(properties);
    s_logger.info("Updated SerialExample... Done.");
  }

  // ----------------------------------------------------------------
  //
  // Private Methods
  //
  // ----------------------------------------------------------------

  /**
   * Called after a new set of properties has been configured on the service
   */
  private void doUpdate(Map<String, Object> properties) {
    try {
      for (String s : properties.keySet()) {
        s_logger.info("Update - "+s+": "+properties.get(s));
      }

      // cancel a current worker handle if one if active
      if (m_handle != null) {
        m_handle.cancel(true);
      }

      //close the serial port so it can be reconfigured
      closePort();

      //store the properties
      m_properties.clear();
      m_properties.putAll(properties);

      //reopen the port with the new configuration
      openPort();

      //start the worker thread
      m_handle = m_worker.submit(new Runnable() {
        @Override
        public void run() {
          doSerial();
        }
      });

    } catch (Throwable t) {
        s_logger.error("Unexpected Throwable", t);
      }
  }

  private void openPort() {
    String port = (String) m_properties.get(SERIAL_DEVICE_PROP_NAME);

    if (port == null) {
      s_logger.info("Port name not configured");
      return;
    }

    int baudRate = Integer.valueOf((String) m_properties.get(SERIAL_BAUDRATE_PROP_NAME));
    int dataBits = Integer.valueOf((String) m_properties.get(SERIAL_DATA_BITS_PROP_NAME));
    int stopBits = Integer.valueOf((String) m_properties.get(SERIAL_STOP_BITS_PROP_NAME));
    String sParity = (String) m_properties.get(SERIAL_PARITY_PROP_NAME);
    int parity = CommURI.PARITY_NONE;

    if (sParity.equals("none")) {
      parity = CommURI.PARITY_NONE;
    } else if (sParity.equals("odd")) {
        parity = CommURI.PARITY_ODD;
    } else if (sParity.equals("even")) {
        parity = CommURI.PARITY_EVEN;
    }

    String uri = new CommURI.Builder(port)
    .withBaudRate(baudRate)
    .withDataBits(dataBits)
    .withStopBits(stopBits)
    .withParity(parity)
    .withTimeout(1000)
    .build().toString();

    try {
      m_commConnection = (CommConnection) m_connectionFactory.createConnection(uri, 1, false);
      m_commIs = m_commConnection.openInputStream();
      m_commOs = m_commConnection.openOutputStream();
      s_logger.info(port+" open");
    } catch (IOException e) {
      s_logger.error("Failed to open port " + port, e);
      cleanupPort();
    }
  }

  private void cleanupPort() {

    if (m_commIs != null) {
      try {
        s_logger.info("Closing port input stream...");
        m_commIs.close();
        s_logger.info("Closed port input stream");
      } catch (IOException e) {
          s_logger.error("Cannot close port input stream", e);
      }
      m_commIs = null;
    }

    if (m_commOs != null) {
      try {
        s_logger.info("Closing port output stream...");
        m_commOs.close();
        s_logger.info("Closed port output stream");
      } catch (IOException e) {
          s_logger.error("Cannot close port output stream", e);
      }
      m_commOs = null;
    }

    if (m_commConnection != null) {
      try {
        s_logger.info("Closing port...");
        m_commConnection.close();
        s_logger.info("Closed port");
      } catch (IOException e) {
          s_logger.error("Cannot close port", e);
      }
      m_commConnection = null;
    }
  }

  private void closePort() {
    cleanupPort();
  }

  private void doSerial() {
    if (m_commIs != null) {
      try {
        int c = -1;
        StringBuilder sb = new StringBuilder();
        while (m_commIs != null) {
          if (m_commIs.available() != 0) {
            c = m_commIs.read();
          } else {
            try {
              Thread.sleep(100);
              continue;
            } catch (InterruptedException e) {
                return;
            }
          }

        // on reception of CR, publish the received sentence
        if (c==13) {
          s_logger.debug("Received serial input, echoing to output: " + sb.toString());
          sb.append("\r\n");
          String dataRead = sb.toString();
          //echo the data to the output stream
          m_commOs.write(dataRead.getBytes());
          //reset the buffer
          sb = new StringBuilder();
        } else if (c!=10) {
          sb.append((char) c);
        }
      }

      } catch (IOException e) {
          s_logger.error("Cannot read port", e);
      } finally {
          try {
            m_commIs.close();
          } catch (IOException e) {
            s_logger.error("Cannot close buffered reader", e);
          }
      }
    }
  }
}
```
At this point, the bundle implementation is complete. *Make sure to save
all files before proceeding.*

### Export the Bundle

To build the Serial Example bundle as a stand-alone OSGi plugin,
right-click the project and select Export.

From the wizard, select Plug-in Development | Deployable plug-ins and
fragments and click Next.

![]({{ site.baseurl }}/assets/images/serial_example//media/image1.png)

The Export window appears. Under Available Plug-ins and
Fragments, verify that the newly created plug-in is selected.

Under Destination, select the Directory option button and use
the Browse button to select an appropriate place to save the JAR
file on the local file system.

{% include alerts.html message=" You will need to know the location where this JAR file is saved
for the deployment process." %}

![]({{ site.baseurl }}/assets/images/serial_example//media/image2.png)

Under Options, select the checkbox Use class files compiled in the
workspace in addition to the checkboxes already enabled, and click
Finish.

![]({{ site.baseurl }}/assets/images/serial_example//media/image3.png)

Doing so will create a JAR file in the selected directory (e.g.,
/home/joe/myPlugins/plugins/org.eclipse.kura.example.serial_1.0.0.201410311510.jar).

### Deploy the Bundle

In order to proceed, you need to know the IP address of your embedded
gateway that is running Kura. Once you have this IP address, follow the
mToolkit instructions for installing a single bundle to a remote target
device (refer to section *2.03 Testing and Deploying Bundles*).

Once the installation successfully completes, you should see a message
from the /var/log/kura.log file indicating that the bundle was
successfully installed and configured. You can also run this example
with the emulator in a Linux or OS X environment as shown sample output
below. Make sure that your user account has owner permission for the
serial device in /dev.

![]({{ site.baseurl }}/assets/images/serial_example//media/image4.png)

### Validate the Bundle

Next, you need to test that your bundle does indeed echo characters back
by opening minicom and configuring it to use [device_node_2] that was
previously determined.

Open minicom using the following command at a Linux terminal on the
remote gateway device:

```
minicom -s
```

This command opens a view similar to the following screen capture:

![]({{ site.baseurl }}/assets/images/serial_example//media/image5.png)

Scroll down to Serial port setup and press <ENTER>. A new dialog
window opens as shown below:

![]({{ site.baseurl }}/assets/images/serial_example//media/image6.png)

<span id="_Cloud_Enabled_Serial" class="anchor"></span>

Use the minicom menu options on the left (i.e., A, B, C, etc.) to change
desired fields. Set the fields to the same values as shown in the
previous screen capture except the Serial Device should match the
[device_node_2] on your target device. Once this is set, press
\<ENTER\> to exit from this menu.

In the main configuration menu, select Exit (*do not* select the
option Exit from Minicom). At this point, you have successfully started
minicom on the second serial port attached to your null modem cable
allowing minicom to act as a serial device that can send and receive
commands to your Kura bundle. You can verify this operation by typing
characters and pressing <ENTER>. The <ENTER> function (specifically
a ‘\\n’ character) signals to the Kura application to echo the buffered
characters back to the serial device (minicom in this case).

Upon startup, minicom sends an initialization string to the serial
device. These characters are sent to the minicom terminal because they
were echoed back by Kura listening on the port at the other end of the
null modem cable.

When you are done, exit minicom by pressing ‘<CTRL> a’, then ‘q’, and
finally ‘<ENTER>’. Doing so brings you back to the Linux command
prompt.

This tutorial instructed you how to write and deploy a Kura bundle on
your target device that listens for serial data (coming from the minicom
terminal and being received on [device_node_1]). This tutorial also
demonstrated that the application echoes data back to the same serial
port that is received in minicom, which acts a serial device that sends
and receives data. If supported by the device, Kura may send and receive
binary data instead of ASCII.

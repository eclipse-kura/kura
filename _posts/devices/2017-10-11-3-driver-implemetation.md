---
layout: page
title:  "Driver implementation"
categories: [devices]
---

A **Driver** encapsulates the communication protocol and its configuration parameters.

The Driver API abstracts the specificities of the end Fieldbus protocols providing a clean and easy to use set of calls that can be used to develop end-applications.

Using the Driver APIs, an application can simply use the connect and disconnect methods to open or close the connection with the Field device. Furthermore, the read and write methods allow exchanging​ data with the Field device.  

A Driver instance can be associated with an Asset to abstract even more the low-level specificities and allow an easy and portable development of the Java applications that need to interact with sensors, actuators, and PLCs.

The Asset will use the Driver's protocol-specific channel descriptor to compose the Asset Channel description.

## Driver Configuration 
Generally, a Driver instance is a configurable component which parameters can be updated in the **Drivers and Assets** section of the Kura Administrative User Interface.

![driver_config]({{ site.baseurl }}/assets/images/drivers_and_assets/Driver_config.png)

## Supported Field Protocols and Availability

Drivers will be provided as add-ons available in the [Eclipse IoT Marketplace](https://marketplace.eclipse.org/category/categories/eclipse-kura).

Currently, supported drivers are:
- [Modbus](https://marketplace.eclipse.org/content/esf-modbus-driver-community-edition);
- [OPC-UA](https://marketplace.eclipse.org/content/opc-ua-driver-kura);
- [S7](https://marketplace.eclipse.org/content/s7-driver-kura-developer-preview);
- [TiSensorTag](https://marketplace.eclipse.org/content/ti-sensortag-driver-eclipse-kura);
- [GPIO](https://marketplace.eclipse.org/content/???);

## Driver-Specific Optimizations

The Driver API provides a simple method to read a list of [Channel Records](http://download.eclipse.org/kura/docs/api/3.1.0/apidocs/org/eclipse/kura/channel/ChannelRecord.html):

```
public void read(List<ChannelRecord> records) throws ConnectionException;
```

Typically, since the records to read do not change until the Asset configuration is changed by the user, a Driver can perform some optimisations to efficiently read the requested records at once. For example, a Modbus driver can read a range of holding registers using a single request.

Since these operations are costly, the ESF API adds methods to ask the driver to prepare reading a given list of records and execute the prepared read:

```
public PreparedRead prepareRead(List<ChannelRecord> records);
```

Invocation of the **preparedRead** method will result in a [PreparedRead](http://download.eclipse.org/kura/docs/api/3.1.0/apidocs/org/eclipse/kura/driver/PreparedRead.html) instance returned.

On a PreparedRead, the execute method will perform the optimized read request.
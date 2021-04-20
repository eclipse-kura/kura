---
layout: page
title:  "Gateway Status"
categories: [config]
---

The status of a gateway may be viewed from the Status window, which is accessed by selecting the **Status** option located in the **System** area. The Status window provides a summary of the key information regarding the status of the gateway including its IoT Cloud connection and network configuration.

## Cloud and Data Services

This section provides a summary of the IoT Cloud connections status including the following details:

- **Service Status** - provides the status of the DataService and DataTransport connection. Valid values are CONNECTED or DISCONNECTED.

- **Auto-connect** - specifies whether the DataService automatically connects to the remote IoT Cloud Service on startup and disconnect.

- **Broker URL** - defines the URL of the MQTT broker.

- **Account** - defines the name of the account used by the MqttDataTransport service when an MQTT connection is opened.

- **Username** - supplies the name of the user used by the MqttDataTransport service when an MQTT connection is opened.

- **Client ID** - specifies the client identifier used by the MqttDataTransport service  when an MQTT connection is opened.

## Ethernet and Wireless Settings

These sections provide a summary of the network interfaces configurations as eth0, eth1 and wlan0.

## Position Status

This section provides the GPS status and latest known position (if applicable) including the following details:

- **Longitude** - longitude as reported by the _PositionService_ in degrees.

- **Latitude** - latitude as reported by the _PositionService_ in degrees.

- **Altitude** - altitude as reported by the _PositionService_ in meters.

![]({{ site.baseurl }}/assets/images/config/GatewayStatus.png)

# Gateway Status

The status of the gateway may be viewed from the Status window, which is accessed by selecting the **Status** option located in the **System** area. The Status window provides a summary of the key information regarding the status of the gateway including its IoT Cloud connection and network configuration.

The values reported in the page can be reloaded using the Refresh button. This will read the current values from the system and update the page. Since the update procedure can take time, the update can be performed at most every 30 seconds.

## Cloud Services

This section provides a summary of the IoT Cloud connection status including the following details:

- **Account** - defines the name of the account used by the MqttDataTransport service when an MQTT connection is opened.
- **Broker URL** - defines the URL of the MQTT broker.
- **Client ID** - specifies the client identifier used by the MqttDataTransport service when an MQTT connection is opened.
- **Service Status** - provides the status of the DataService and DataTransport connection. Valid values are CONNECTED or DISCONNECTED.
- **Username** - supplies the name of the user used by the MqttDataTransport service when an MQTT connection is opened.


## Ethernet, Wireless, and Cellular Settings

This section provides information about the currently configured network interfaces.

## Position Status

This section provides the GPS status and latest known position (if applicable) including the following details:

- **Longitude** - longitude as reported by the _PositionService_ in radians.
- **Latitude** - latitude as reported by the _PositionService_ in radians.
- **Altitude** - altitude as reported by the _PositionService_ in meters.

!!! warning
    The status reported in the page may not be synchronized with the real state of the system. In this case, use the Refresh button to updated the values in the page.

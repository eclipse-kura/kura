---
layout: page
title:  "Data Service Configuration"
categories: [config]
---

The DataService provides the ability to connect to a remote broker, publish messages, subscribe to topics, receive messages on the subscribed topics, and disconnect from the remote message broker. The DataService delegates to the MqttDataTransport service the implementation of the transport protocol that is used to interact with the remote server.

The DataService also adds the capability of storing published messages in a persistent store function and sending them over the wire at a later time. The purpose of this feature is to relieve service users from implementing their own persistent store. Service users may publish messages independently on the DataService connection status.

In order to overcome the potential latencies introduced by buffering messages, the DataService allows a priority level to be assigned to​ each published message. Depending on the store configuration, there are certain guarantees that stored messages are not lost due to sudden crashes or power outages.

To use this service, select the **DataService** option located in the **Cloud Connections** area as shown in the screen capture below.

![kura_cloud_stack]({{ site.baseurl }}/assets/images/config/Kura_data_service.png)

The DataService offers methods and configuration options to manage the connection to the remote server including the following (all required) parameters described below.

- **connect.auto-on-startup** - when set to true, the service tries to auto-connect to the remote server on start-up and restore the connection every time the device is disconnected. These attempts are made at the frequency defined in the _connect.retry-interval_ parameter until the connection is established.

- **connect.retry-interval** - specifies the connection retry frequency after a disconnection.

- **enable.recovery.on.connection.failure** - when enabled, activates the recovery feature on connection failure: if the device is not able to connect to a remote cloud platform, the service will wait for a specified amount of connection retries. If the recovery fails, the device will be rebooted. Being based on the Watchdog service, it needs to be activated as well.

- **connection.recovery.max.failures** - related to the previous parameter. It specifies the number of failures before a reboot is requested.

- **disconnect.quiesce-timeout** - allows the delivery of in-flight messages to be completed before disconnecting from the broker when a disconnection from the broker is being forced.

- **store.db.service.pid** - The Kura Service PID of the database instance to be used. The PID of the default instance is org.eclipse.kura.db.H2DbService.

- **store.housekeeper-interval** - defines the interval in seconds used to run the Data Store housekeeper task.

- **store.purge-age** - defines the age in seconds of completed messages (either published with QoS = 0 or confirmed with QoS > 0) after which they are deleted (minimum 5).

- **store.capacity** - defines the maximum number of messages persisted in the Data Store.

- **in-flight-messages.republish-on-new-session** - it specifies whether to republish in-flight messages on a new MQTT session.

- **in-flight-messages.max-number** - it specifies the maximum number of in-flight messages.

- **in-flight-messages.congestion-timeout** - timeouts the in-flight messages congestion condition. The service will force a disconnect attempting to reconnect.

- **enable.rate.limit** - Enables the token bucket message rate limiting.

- **rate.limit.average** - The average message publishing rate. It is intended as the number of messages per unit of time.
  
{% include alerts.html message="The maximum allowed message rate is **1 message per millisecond**, so the following limitations are applied:

- 86400000 per DAY
- 3600000 per HOUR
- 60000 messages per MINUTE
- 1000 messages per SECOND" %}

- **rate.limit.time.unit** - The time unit for the rate.limit.average.

- **rate.limit.burst.size** - The token bucket burst size.
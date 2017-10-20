---
layout: page
title:  "ActiveMQ Artemis Broker Service"
categories: [builtin]
---

Apart from using the simple ActiveMQ Artemis MQTT instance available in the [Simple Artemis MQTT Broker Service](simple-artemis-mqtt-broker-service.html) this service allows to configure, in a more detailed way, the characteristics of the [ActiveMQ Artemis broker](https://activemq.apache.org/artemis/index.html) instance running in Eclipse Kura.

![kura_artemis_broker]({{ site.baseurl }}/assets/images/builtin/Kura_artemis_broker.png)

This service exposes the following configuration parameters:

  - **Enabled** - (Required) - Enables the broker instance
  - **Broker XML** - Broker XML configuration. An empty broker configuration will disable the broker.
  - **Required protocols** - A comma seperated list of all required protocol factories (e.g. AMQP or MQTT)
  - **User configuration** - (Required) - User configuration in the format: ```user=password|role1,role2,...```
  - **Default user name** - The name of the default user

Please refer to the [official documentation](https://activemq.apache.org/artemis/docs.html) for more details on how to configure the ActiveMQ broker service.

# Service Usage Example
Setting the **Broker XML** field as follows:

```xml
<?xml version="1.0" encoding="UTF-8"?>

<configuration xmlns="urn:activemq"
xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
xsi:schemaLocation="
urn:activemq https://raw.githubusercontent.com/apache/activemq-artemis/master/artemis-server/src/main/resources/schema/artemis-server.xsd
urn:activemq:core https://raw.githubusercontent.com/apache/activemq-artemis/master/artemis-server/src/main/resources/schema/artemis-configuration.xsd
urn:activemq:jms https://raw.githubusercontent.com/apache/activemq-artemis/master/artemis-jms-server/src/main/resources/schema/artemis-jms.xsd
">

	<core xmlns="urn:activemq:core">
		<persistence-enabled>false</persistence-enabled>

		<acceptors>
			<acceptor name="netty-acceptor">tcp://localhost:61616</acceptor>
			<acceptor name="amqp-acceptor">tcp://localhost:5672?protocols=AMQP</acceptor>
			<acceptor name="mqtt-acceptor">tcp://localhost:1883?protocols=MQTT</acceptor>
		</acceptors>

		<resolve-protocols>false</resolve-protocols>

		<security-settings>
		<!-- WARNING: this is only for testing and completely insecure -->
			<security-setting match="#">
				<permission type="createDurableQueue" roles="guest"/>
				<permission type="deleteDurableQueue" roles="guest"/>
				<permission type="createNonDurableQueue" roles="guest"/>
				<permission type="deleteNonDurableQueue" roles="guest"/>
				<permission type="consume" roles="guest"/>
				<permission type="send" roles="guest"/>
			</security-setting>
		</security-settings>

	</core>

	<jms xmlns="urn:activemq:jms">
    <topic name="TEST.T.1" />
	</jms>

</configuration>
```

the **User configuration** to

```
guest=test12|guest
```

while setting the **Default user name** to

```
guest
```

will determine that the TCP ports 1883, 5672 and 61616 are now open (you can verify that via **netstat -antup**).

Configuring the **MqttDataTransport** in System -> Cloud Services -> MqttDataTransport to use:

- **broker-url** - mqtt://localhost:1883
- **username** - guest
- **password** - test12

Clicking on the **Connect** button will result in a successful connection of Kura cloud service to the Apache ActiveMQ Artemis MQTT broker.

> Note: The XML configuration above only allows connections originating from the gateway itself. In order to allow external connection the bind URLs specified using the `acceptor` tag must be modified by specifying an external accessible address instead of `localhost`. If the bind address is set to `0.0.0.0`, the broker will listen on all available addresses.

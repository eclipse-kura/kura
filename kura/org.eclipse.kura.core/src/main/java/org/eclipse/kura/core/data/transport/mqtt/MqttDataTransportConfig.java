package org.eclipse.kura.core.data.transport.mqtt;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.Icon;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.Option;

@ObjectClassDefinition(
        id = "org.eclipse.kura.core.data.transport.mqtt.MqttDataTransport",
        name = "MqttDataTransport",
        description = "The MqttDataTransport provides an MQTT connection. Its configuration parameters are used to determine"
                + " the MQTT broker and the credentials to connect to the broker.",
        localization = "en_us",
        icon = @Icon(resource = "MqttDataTransport", size = 32))
@interface MqttDataTransportConfig {

    @AttributeDefinition(
            name = "Broker-url",
            cardinality = 0,
            required = true,
            description = "URL of the MQTT broker to connect to, specifying protocol, hostname and port "
                    + "(for example mqtt://your.broker.url:1883/ ). Supported protocols are mqtt, mqtts, ws and wss.")
    String broker$_$url() default "mqtt://broker-url:1883/";

    @AttributeDefinition(
            name = "Topic Context Account-Name",
            cardinality = 0,
            required = false,
            description = "The value of this attribute will replace the '#account-name' token found in publishing topics."
                    + " For connections to remote management servers, this is generally the name of the server side account.")
    String topic_context_account$_$name() default "account-name";

    @AttributeDefinition(
            name = "Username",
            cardinality = 0,
            required = false,
            description = "Username to be used when connecting to the MQTT broker.")
    String username() default "username";

    // TODO: deprecate this als use a id .password according spec
    @AttributeDefinition(
            name = "Password",
            type = AttributeType.PASSWORD,
            cardinality = 0,
            required = false,
            description = "Password to be used when connecting to the MQTT broker.")
    String password() default "password";

    @AttributeDefinition(
            name = "Client-id",
            cardinality = 0,
            required = false,
            description = "Client identifier to be used when connecting to the MQTT broker. The identifier has to be unique within your account."
                    + " Characters '/', '+', '#' and '.' are invalid and they will be replaced by '-'. If left empty,"
                    + " this is automatically determined by the client software as the MAC address of the main network interface"
                    + " (in general uppercase and without ':').")
    String client$_$id() default "";

    @AttributeDefinition(
            name = "LWT Topic",
            cardinality = 0,
            required = false,
            description = "MQTT Last Will and Testament topic. The tokens '#account-name' and '#client-id' will be replaced by"
                    + " the values of the properties topic.context.account-name and client-id")
    String lwt_topic() default "$EDC/#account-name/#client-id/MQTT/LWT";

    @AttributeDefinition(
            name = "LWT Payload",
            cardinality = 0,
            required = false,
            description = "MQTT Last Will and Testament payload as a string.")
    String lwt_payload() default "";

    @AttributeDefinition(
            name = "LWT Qos",
            cardinality = 0,
            required = false,
            description = "MQTT Last Will and Testament QoS (0..2).",
            options = { @Option(label = "0", value = "0"), @Option(label = "1", value = "1"),
                    @Option(label = "2", value = "2") })
    int lwt_qos() default 0;

    @AttributeDefinition(
            name = "LWT Retain",
            cardinality = 0,
            required = false,
            description = "MQTT Last Will and Testament Retain flag.")
    boolean lwt_retain() default false;

    @AttributeDefinition(
            name = "In-flight Persistence",
            cardinality = 0,
            required = true,
            description = "Storage type where in-flight messages are persisted across reconnections.",
            options = { @Option(label = "file", value = "file"), @Option(label = "memory", value = "memory") })
    String in$_$flight_persistence() default "memory";

    @AttributeDefinition(
            name = "Protocol-version",
            cardinality = 0,
            required = false,
            description = "MQTT Protocol Version.",
            options = { @Option(label = "3.1", value = "3"), @Option(label = "3.1.1", value = "4") })
    int protocol_version() default 4;

    @AttributeDefinition(
            name = "Keep-alive",
            cardinality = 0,
            required = true,
            description = "Frequency in seconds for the periodic MQTT PING message.")
    int keep$_$alive() default 30;

    @AttributeDefinition(
            name = "Timeout",
            cardinality = 0,
            required = true,
            description = "Timeout used for all interactions with the MQTT broker.")
    int timeout() default 10;

    @AttributeDefinition(
            name = "SslManagerService Target Filter",
            cardinality = 0,
            required = true,
            description = "Specifies, as an OSGi target filter, the pid of the SslManagerService used to create SSL connections.")
    String SslManagerService_target() default "(kura.service.pid=org.eclipse.kura.ssl.SslManagerService)";

}

package org.eclipse.kura.core.data;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Icon;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(
        id = "org.eclipse.kura.data.DataService",
        name = "DataService",
        description = "DataService provides auto-connect, reconnect on connection drops and storing of outgoing messages.",
        localization = "en_us",
        icon = @Icon(resource = "DataService", size = 32))
public @interface DataServiceConfig {

    enum RateLimitTimeUnit {
        SECONDS,
        MINUTES,
        HOURS,
        DAYS
    }

    @AttributeDefinition(
            name = "Connect Auto-on-startup",
            cardinality = 0,
            description = "Enable automatic connect of the Data Publishers on startup and after a disconnection.")
    boolean connect_auto$_$on$_$startup() default false;

    @AttributeDefinition(
            name = "Rate Limit Time Unit",
            cardinality = 0,
            required = true,
            description = "The time unit for the rate.limit.average.")
    RateLimitTimeUnit rate_limit_time_unit() default RateLimitTimeUnit.SECONDS;

    @AttributeDefinition(
            name = "Rate Limit Burst Size",
            cardinality = 0,
            required = true,
            min = "1",
            description = "The token bucket burst size.")
    int rate_limit_burst_size() default 1;

    @AttributeDefinition(
            name = "Connect Retry-interval",
            min = "1",
            cardinality = 0,
            required = true,
            description = "Frequency in seconds to retry a connection of the Data Publishers after a disconnect (Minimum value 1).")
    int connect_retry$_$interval() default 60;

    @AttributeDefinition(
            name = "Enable Recovery On Connection Failure",
            cardinality = 0,
            required = true,
            description = "Enables the recovery feature on connection failure. If the device is not able to connect to a remote cloud platform,"
                    + " the service will wait for a specified amount of connection retries. If the recovery fails, the device will be rebooted."
                    + " Being based on the Watchdog service, it needs to be activated as well.")
    boolean enable_recovery_on_connection_failure() default false;

    @AttributeDefinition(
            name = "Connection Recovery Max Failures",
            cardinality = 0,
            min = "1",
            required = true,
            description = "Number of failures in Data Publishers connection before forcing a reboot.")
    int connection_recovery_max_failures() default 10;

    @AttributeDefinition(
            name = "Disconnect Quiesce-timeout",
            min = "0",
            cardinality = 0,
            required = true,
            description = "Timeout used to try to complete the delivery of stored messages before forcing a disconnect of the Data Publisher.")
    int disconnect_quiesce$_$timeout() default 10;

    @AttributeDefinition(
            name = "Store DB Service PID",
            cardinality = 0,
            required = true,
            description = "The Kura service pid of the H2 database instance to be used. The pid of the default instance is"
                    + " org.eclipse.kura.db.H2DbService.")
    String store_db_service_pid() default "org.eclipse.kura.db.H2DbService";

    @AttributeDefinition(
            name = "Store Housekeeper-interval",
            min = "5",
            cardinality = 0,
            required = true,
            description = "Interval in seconds used to run the Data Store housekeeper task (min 5).")
    int store_housekeeper$_$interval() default 900;

    @AttributeDefinition(
            name = "Store Purge-age",
            min = "5",
            cardinality = 0,
            required = true,
            description = "Age in seconds of completed messages (either published with QoS = 0 or confirmed with QoS > 0) after which they are"
                    + " deleted (min 5).")
    int store_purge$_$age() default 60;

    @AttributeDefinition(
            name = "Store Capacity",
            min = "1",
            cardinality = 0,
            required = true,
            description = "Maximum number of messages persisted in the Data Store. The limit does not apply to messages with the priority less "
                    + "than 2. These priority levels are reserved to the framework which uses it for life-cycle messages - birth and death "
                    + "certificates - and replies to request/response flows.")
    int store_capacity() default 10000;

    @AttributeDefinition(
            name = "In-flight-messages Republish-on-new-session",
            cardinality = 0,
            required = true,
            description = "Whether to republish in-flight messages on a new MQTT session.")
    boolean in$_$flight$_$messages_republish$_$on$_$new$_$session() default true;

    @AttributeDefinition(
            name = "In-flight-messages Max-number",
            min = "1",
            max = "10",
            cardinality = 0,
            required = true,
            description = "The maximum number of in-flight messages.")
    int in$_$flight$_$messages_max$_$number() default 9;

    @AttributeDefinition(
            name = "In-flight-messages Congestion-timeout",
            cardinality = 0,
            required = true,
            description = "nables the token bucket message rate limiting.")
    int in$_$flight$_$messages_congestion$_$timeout() default 0;

    @AttributeDefinition(name = "Enable Rate Limit", cardinality = 0, required = true, description = "")
    boolean enable_rate_limit() default true;

    @AttributeDefinition(
            name = "Rate Limit Average",
            min = "1",
            cardinality = 0,
            required = true,
            description = "The average message publish rate in number of messages per unit of time (e.g. 10 messages per MINUTE). This parameter has"
                    + " some limitations described in http://eclipse.github.io/kura/config/data-service-configuration.html")
    int rate_limit_average() default 1;

}

/*******************************************************************************
 * Copyright (c) 2011, 2026 Eurotech and/or its affiliates and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Eurotech
 ******************************************************************************/
package org.eclipse.kura.core.data;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.Icon;
import org.osgi.service.metatype.annotations.Option;

@ObjectClassDefinition(id = "org.eclipse.kura.data.DataService", name = "DataService", description = "DataService provides auto-connect, reconnect on connection drops and storing of outgoing messages.", icon = @Icon(resource = "DataService", size = 32))
public @interface DataServiceMetatype {

    @AttributeDefinition(name = "Connect Auto-on-startup", description = "Enable automatic connect of the Data Publishers on startup and after a disconnection.")
    boolean connect_auto$_$on$_$startup() default false;

    @AttributeDefinition(name = "Connect Retry-interval", min = "1", description = "Frequency in seconds to retry a connection of the Data Publishers after a disconnect (Minimum value 1).")
    int connect_retry$_$interval() default 60;

    @AttributeDefinition(name = "Enable Recovery On Connection Failure", description = "Enables the recovery feature on connection failure. If the device is not able to connect to a remote cloud platform, the service will wait for a specified amount of connection retries. If the recovery fails, the device will be rebooted. Being based on the Watchdog service, it needs to be activated as well.")
    boolean enable_recovery_on_connection_failure() default false;

    @AttributeDefinition(name = "Connection Recovery Max Failures", min = "1", description = "Number of failures in Data Publishers connection before forcing a reboot.")
    int connection_recovery_max_failures() default 10;

    @AttributeDefinition(name = "Disconnect Quiesce-timeout", min = "0", description = "Timeout used to try to complete the delivery of stored messages before forcing a disconnect of the Data Publisher.")
    int disconnect_quiesce$_$timeout() default 10;

    @AttributeDefinition(name = "Message Store Provider Service PID", description = "The Kura service pid of the Message Store instance to be used. The pid of the default instance is org.eclipse.kura.db.H2DbService.")
    String store_db_service_pid() default "org.eclipse.kura.db.H2DbService";

    @AttributeDefinition(name = "Store Housekeeper-interval", min = "5", description = "Interval in seconds used to run the Data Store housekeeper task (min 5).")
    int store_housekeeper$_$interval() default 900;

    @AttributeDefinition(name = "Store Purge-age", min = "5", description = "Age in seconds of completed messages (either published with QoS = 0 or confirmed with QoS > 0) after which they are deleted (min 5).")
    int store_purge$_$age() default 60;

    @AttributeDefinition(name = "Store Capacity", min = "1", description = "Maximum number of messages persisted in the Data Store. The limit does not apply to messages with the priority less than 2. These priority levels are reserved to the framework which uses it for life-cycle messages - birth and death certificates - and replies to request/response flows.")
    int store_capacity() default 10000;

    @AttributeDefinition(name = "In-flight-messages Republish-on-new-session", description = "Whether to republish in-flight messages on a new MQTT session.")
    boolean in$_$flight$_$messages_republish$_$on$_$new$_$session() default true;

    @AttributeDefinition(name = "In-flight-messages Max-number", min = "1", max = "10", description = "The maximum number of in-flight messages.")
    int in$_$flight$_$messages_max$_$number() default 9;

    @AttributeDefinition(name = "In-flight-messages Congestion-timeout", min = "0", description = "Timeouts the in-flight messages congestion condition. The service will force a disconnect attempting to reconnect (0 to disable).")
    int in$_$flight$_$messages_congestion$_$timeout() default 0;

    @AttributeDefinition(name = "Enable Rate Limit", description = "Enables the token bucket message rate limiting.")
    boolean enable_rate_limit() default true;

    @AttributeDefinition(name = "Rate Limit Average", min = "1", description = "The average message publish rate in number of messages per unit of time (e.g. 10 messages per MINUTE). This parameter has some limitations described on the data service configuration page in the official product documentation.")
    int rate_limit_average() default 1;

    @AttributeDefinition(name = "Rate Limit Time Unit", options = { @Option(label = "SECONDS", value = "SECONDS"), @Option(label = "MINUTES", value = "MINUTES"), @Option(label = "HOURS", value = "HOURS"), @Option(label = "DAYS", value = "DAYS") }, description = "The time unit for the rate.limit.average.")
    String rate_limit_time_unit() default "SECONDS";

    @AttributeDefinition(name = "Rate Limit Burst Size", min = "1", description = "The token bucket burst size.")
    int rate_limit_burst_size() default 1;

    @AttributeDefinition(name = "Enable Connection Schedule", description = "Enables or disables the connection scheduling feature.")
    boolean connection_schedule_enabled() default false;

    @AttributeDefinition(name = "Connection Schedule CRON Expression", description = "A CRON expression that specifies the instants when the gateway should perform a connection attempt. This parameter is only used if Enable Connection Schedule is set to true. The default expression schedules a connection every day at midnight.")
    String connection_schedule_expression() default "0 0 0 ? * * *";

    @AttributeDefinition(name = "Allow priority message to overide connection schedule", description = "Allows messages beyond a specified priority to force a connection and be sent regardless of connection schedule.")
    boolean connection_schedule_priority_override_enable() default false;

    @AttributeDefinition(name = "Message schedule priority override threshold", description = "A message with a priority equal to or less than this threshold will cause the framework to automatically re-connect and send regardless of the connection schedule.")
    int connection_schedule_priority_override_threshold() default 1;

    @AttributeDefinition(name = "Connection Schedule Disconnect Inactivity Interval Seconds", min = "1", description = "Specifies an inactivity timeout in seconds. If the timeout expires, the cloud connection will be automatically closed. This timeout is delayed for the specified amount of seconds every time a new message is published. This parameter is only used if Enable Connection Schedule is set to true.")
    long connection_schedule_inactivity_interval_seconds() default 60L;

    @AttributeDefinition(name = "Maximum Payload Size", min = "0", description = "The maximum allowed size in bytes for the message payload.")
    long maximum_payload_size() default 16777216L;

}



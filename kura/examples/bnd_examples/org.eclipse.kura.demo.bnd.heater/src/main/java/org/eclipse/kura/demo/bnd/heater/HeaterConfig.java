/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates and others
 *
 *  This program and the accompanying materials are made
 *  available under the terms of the Eclipse Public License 2.0
 *  which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *   Eurotech
 *******************************************************************************/

package org.eclipse.kura.demo.bnd.heater;

import org.osgi.service.metatype.annotations.*;

/**
 * Meta type information for {@link Heater}
 * <p>
 * <strong>Note: </strong> The id must be the full qualified name of the assigned component.
 * </p>
 */
@ObjectClassDefinition(
        id = "org.eclipse.kura.demo.bnd.heater.Heater",
        name = "Heater Config",
        icon = @Icon(resource = "heater.png", size = 32),
        description = "This is the configuration of the Heater demo."
)
@interface HeaterConfig {

    @AttributeDefinition(
            name = "Mode",
            type = AttributeType.STRING,
            defaultValue = "Program",
            description = "Operating mode for the heater. If operatng mode is Vacation, set point is automatiaclly set to 6.0C.",
            options = {
                    @Option(label = "Program", value = "Program"),
                    @Option(label = "Manual", value = "Manual"),
                    @Option(label = "Vacation", value = "Vacation"),
            }
    )
    String mode();

    @AttributeDefinition(
            name = "Program start time",
            type = AttributeType.STRING,
            required = false,
            defaultValue = "06:00",
            description = "Start time for the heating cycle with the operating mode is Program."
    )
    String program_startTime();

    @AttributeDefinition(
            name = "Program stop time",
            type = AttributeType.STRING,
            required = false,
            defaultValue = "22:00",
            description = "Stop time for the heating cycle with the operating mode is Program."
    )
    String program_stopTime();

    @AttributeDefinition(
            name = "Program set point",
            type = AttributeType.FLOAT,
            required = false,
            defaultValue = "20.5",
            min = "5.0",
            max = "40.0",
            description = "Temperature Set Point in Celsius for the heating cycle with the operating mode is Program."
    )
    String program_setPoint();

    @AttributeDefinition(
            name = "Manual set point",
            type = AttributeType.FLOAT,
            required = false,
            defaultValue = "15.0",
            min = "5.0",
            max = "40.0",
            description = "Temperature Set Point in Celsius for the heating cycle with the operating mode is Manual."
    )
    String manual_setPoint();

    @AttributeDefinition(
            name = "Initial temperature",
            type = AttributeType.FLOAT,
            required = false,
            defaultValue = "10",
            description = "Initial value for the temperature metric."
    )
    String temperature_initial();

    @AttributeDefinition(
            name = "Temperature increment",
            type = AttributeType.FLOAT,
            required = false,
            defaultValue = "0.25",
            description = "Increment value for the temperature metric."
    )
    String temperature_increment();

    @AttributeDefinition(
            name = "Publish rate",
            type = AttributeType.INTEGER,
            defaultValue = "2",
            min = "1",
            description = "Default message publishing rate in seconds (min 1)."
    )
    String publish_rate();

    @AttributeDefinition(
            name = "Publish semantic topic",
            type = AttributeType.STRING,
            defaultValue = "data",
            description = "Default semantic topic to publish the message to."
    )
    String publish_semanticTopic();

    @AttributeDefinition(
            name = "Publish QoS",
            type = AttributeType.INTEGER,
            defaultValue = "0",
            description = "Default QoS to publish the message with.",
            options = {
                    @Option(label = "Fire and forget", value = "0"),
                    @Option(label = "At least once", value = "1"),
                    @Option(label = "At most once", value = "2")
            }
    )
    String publish_qos();

    @AttributeDefinition(
            name = "Publish retain",
            type = AttributeType.BOOLEAN,
            defaultValue = "false",
            description = "Default retaining flag for the published message."
    )
    String publish_retain();
}

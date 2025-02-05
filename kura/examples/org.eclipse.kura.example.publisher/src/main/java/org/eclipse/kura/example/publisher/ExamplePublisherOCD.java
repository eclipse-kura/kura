/*******************************************************************************
 * Copyright (c) 2025 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 *  Red Hat Inc
 *******************************************************************************/
package org.eclipse.kura.example.publisher;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.Icon;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.Option;

// allow using _ in method names, it is needed for having ids containing '.'
@SuppressWarnings("checkstyle:MethodName")
@ObjectClassDefinition(id = "org.eclipse.kura.example.publisher.ExamplePublisher", //
        name = "ExamplePublisher", //
        description = "Example of a Configuring Kura Application.", //
        icon = { //
                @Icon(resource = "http://s3.amazonaws.com/kura-resources/application/icon/applications-other.png", //
                        size = 32) //
        })
public interface ExamplePublisherOCD {

    @AttributeDefinition(name = "CloudPublisher Target Filter", //
            defaultValue = "(kura.service.pid=changeme)", //
            description = "Specifies, as an OSGi target filter, " //
                    + "the pid of the Cloud Publisher used to publish messages to the cloud platform." //
    )
    public String CloudPublisher_target();

    @AttributeDefinition(name = "CloudSubscriber Target Filter", //
            defaultValue = "(kura.service.pid=changeme)", //
            description = "Specifies, as an OSGi target filter," //
                    + " the pid of the Cloud Subscriber used to receive messages from the cloud platform." //
    )
    public String CloudSubscriber_target();

    @AttributeDefinition(name = "Publish Rate", //
            defaultValue = "1000", //
            min = "1", //
            description = "Default message publishing rate in milliseconds (min 1)." //
    )
    public Integer publish_rate();

    @AttributeDefinition(name = "Initial Temperature", //
            defaultValue = "10", //
            description = "Initial value for the temperature metric." //
    )
    public Float metric_temperature_initial();

    @AttributeDefinition(name = "Temperature Increment", //
            defaultValue = "0.1", //
            description = "Increment value for the temperature metric." //
    )
    public Float metric_temperature_increment();

    @AttributeDefinition(name = "Metric String", //
            defaultValue = "string.value", //
            description = "Test metric of String type." //
    )
    public String metric_string();

    @AttributeDefinition(name = "Metric String Oneof", //
            defaultValue = "string.value.option.1", //
            description = "Test metric of String type whose value is one of a list.", //
            options = { //
                    @Option(label = "string.value.option.1", value = "string.value.option.1"), //
                    @Option(label = "string.value.option.2", value = "string.value.option.2"), //
                    @Option(label = "string.value.option.3", value = "string.value.option.3") //
            })
    public String metric_string_oneof();

    @AttributeDefinition(name = "Metric Long", //
            defaultValue = "9223372036854774999", //
            min = "-9223372036854775000", //
            max = "9223372036854775000", //
            description = "Long metric. Metric range min -9223372036854775000" //
                    + " max 9223372036854775000." //
                    + " Long min -9223372036854775808" //
                    + " max 9223372036854775807." //
    )
    public Long metric_long();

    @AttributeDefinition(name = "Metric Integer", //
            defaultValue = "2147483599", //
            min = "-2147483600", //
            max = "2147483600", //
            description = "Integer metric. " + "Metric range min -2147483600 " + "max 2147483600. "
                    + "Integer min -2147483648" + " max 2147483647.")
    public Integer metric_integer();

    @AttributeDefinition(name = "Metric Integer Fixed", //
            defaultValue = "101", //
            min = "101", //
            max = "101", //
            description = "Integer metric. " //
                    + "The only allowed value is 101." //
    )
    public Integer metric_integer_fixed();

    @AttributeDefinition(name = "Metric Short", //
            defaultValue = "32759", //
            min = "-32760", //
            max = "32760", //
            description = "Short metric. " //
                    + "Metric range min -32760" //
                    + " max 32760. " //
                    + "Short min -32768" //
                    + " max 32767." //
    )
    public Short metric_short();

    @AttributeDefinition(name = "Metric Double", //
            defaultValue = "4294967295.99998", //
            min = "-4294967295.99999", //
            max = "4294967295.99999", //
            description = "Double metric." //
                    + " Metric range min -4294967295.99999" //
                    + " max 4294967295.99999." //
    )
    public Double metric_double();

    @AttributeDefinition(name = "Metric Float", //
            defaultValue = "32766.98", //
            min = "-32766.99", //
            max = "32766.99", //
            description = "Float metric. " //
                    + "Metric range min -32766.99 " //
                    + "max 32766.99." //
    )
    public Float metric_float();

    @AttributeDefinition(name = "Metric Char", //
            defaultValue = "a", //
            min = "a", //
            max = "z", //
            description = "Char metric. Min a. Max z." //
    )
    public Character metric_char();

    @AttributeDefinition(name = "Metric Byte", //
            defaultValue = "119", //
            min = "-120", //
            max = "120", //
            description = "Byte metric. " //
                    + "Metric range min -120 " //
                    + "max 120. " //
                    + "Byte min -128 " //
                    + "max 127." //
    )
    public Byte metric_byte();

    @AttributeDefinition(name = "Metric Boolean", //
            defaultValue = "false", //
            description = "Boolean metric." //
    )
    public Boolean metric_boolean();

    @AttributeDefinition(name = "Metric Password", //
            type = AttributeType.PASSWORD, //
            defaultValue = "password.value", //
            description = "Password metric." //
    )
    public String metric_password();

    @AttributeDefinition(name = "Metric String Array", //
            cardinality = 5, //
            required = false, //
            defaultValue = { "string.value1", "string.value2", "string.value3" }, //
            description = "Test metric of Array of String type." //
    )
    public String[] metric_string_array();

    @AttributeDefinition(name = "Metric Short Array", //
            cardinality = 5, //
            required = false, //
            defaultValue = { "10", "20", "30" }, //
            min = "-32760", //
            max = "32760", //
            description = "Short Array metric." //
                    + " Metric range min -32760" //
                    + " max 32760." //
                    + " Short min -32768" //
                    + " max 32767." //
    )
    public Short[] metric_short_array();

    @AttributeDefinition(name = "Metric String Optional", //
            required = false, //
            description = "Test optional strng metric. Leave EMPTY.")
    public String metric_string_optional();
}

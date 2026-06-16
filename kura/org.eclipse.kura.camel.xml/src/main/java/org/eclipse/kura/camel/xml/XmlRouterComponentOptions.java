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
package org.eclipse.kura.camel.xml;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.Icon;

@ObjectClassDefinition(id = "org.eclipse.kura.camel.xml.XmlRouterComponent", name = "Camel XML router", description = "Configurable Camel-based XML router", icon = @Icon(resource = "OSGI-INF/logo.png", size = 32))
public @interface XmlRouterComponentOptions {

    @AttributeDefinition(name = "Router XML", cardinality = 1, required = false, max = "2147483647", description = "Camel XML route definitions|TextArea")
    String xml_data() default "<routes xmlns=\"http://camel.apache.org/schema/spring\">\n  <route id=\"route1\">\n         <from uri=\"kura-cloud:myapp/xmltopic\"/>\n         <to uri=\"log:MESSAGE_FROM_CLOUD\"/>\n     </route>\n </routes>";

    @AttributeDefinition(name = "Required Camel Components", cardinality = 1, required = false, max = "2147483647", description = "A comma separated list of Camel components which are required in order to start up this setup (e.g. amqp, stream)")
    String component_prereqs();

    @AttributeDefinition(name = "Required Camel Languages", cardinality = 1, required = false, max = "2147483647", description = "A comma separated list of Camel languages which are required in order to start up this setup (e.g. javaScript)")
    String language_prereqs();

    @AttributeDefinition(name = "Cloud Service Mappings", cardinality = 1, required = false, max = "2147483647", description = "A comma separated list of entries in the format name=filter or name=kura-pid, mapping cloud service instances to component names. (e.g. cloud=org.eclipse.kura.cloud.CloudService)")
    String cloudService_prereqs();

    @AttributeDefinition(name = "JavaScript init code (Java 8 only)", cardinality = 1, required = false, description = "JavaScript code which is called when the router is initialized first. The camel context is available in the variable 'camelContext'. Warning: this feature only works on JRE with Nashorn (Java < 15).|TextArea")
    String initCode();

    @AttributeDefinition(name = "Disable JMX", cardinality = 1, description = "Disable the JMX integration for this Camel context")
    boolean disableJmx() default false;

}



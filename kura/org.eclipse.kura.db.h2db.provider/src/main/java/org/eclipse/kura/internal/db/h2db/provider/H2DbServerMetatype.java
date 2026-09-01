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
package org.eclipse.kura.internal.db.h2db.provider;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.Option;

@ObjectClassDefinition(id = "org.eclipse.kura.core.db.H2DbServer", name = "H2DbServer", description = "H2 based database server.")
public @interface H2DbServerMetatype {

    @AttributeDefinition(name = "db.server.enabled", description = "Specifies whether the DB server is enabled or not.")
    boolean db_server_enabled() default false;

    @AttributeDefinition(name = "db.server.type", options = { @Option(label = "WEB", value = "WEB"), @Option(label = "TCP", value = "TCP"), @Option(label = "PG", value = "PG") }, description = "Specifies the server type, see http://www.h2database.com/javadoc/org/h2/tools/Server.html for more details.")
    String db_server_type() default "TCP";

    @AttributeDefinition(name = "db.server.commandline", description = "Specifies the parameters for the server, see http://www.h2database.com/javadoc/org/h2/tools/Server.html for more details. The listening port must be manually openend in the Firewall configuration section in order to allow external connections.")
    String db_server_commandline() default "-tcpPort 9123 -tcpAllowOthers -ifExists";

}



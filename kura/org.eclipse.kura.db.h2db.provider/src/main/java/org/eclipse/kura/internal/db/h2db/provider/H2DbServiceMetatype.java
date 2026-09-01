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
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(id = "org.eclipse.kura.core.db.H2DbService", name = "DbService", description = "H2 based database service.")
public @interface H2DbServiceMetatype {

    @AttributeDefinition(name = "Connector URL", description = "JDBC connector URL of the database instance. See http://www.h2database.com/html/features.html for more information.              Passing the USER and PASSWORD parameters in the connector URL is not supported, these paramters will be ignored if present.              Please use the db.user and db.password fields to provide the credentials.      In case of persisted databases, the database file path is subject to limitations.              Please make sure to read official H2DbService documentation before creating a new database.")
    String db_connector_url() default "jdbc:h2:mem:kuradb";

    @AttributeDefinition(name = "User", description = "Specifies the user for the database connection.")
    String db_user() default "SA";

    @AttributeDefinition(name = "Password", type = AttributeType.PASSWORD, required = false, description = "Specifies the password. The default password is the empty string.")
    String db_password() default "";

    @AttributeDefinition(name = "Checkpoint interval (seconds)", min = "5", description = "H2DbService instances support running periodic checkpoints to ensure data consistency. This parameter specifies the interval in seconds between two successive checkpoints. This setting has no effect for in-memory database instances.")
    int db_checkpoint_interval_seconds() default 900;

    @AttributeDefinition(name = "Defrag interval (minutes)", min = "0", description = "H2DbService instances support running periodic defragmentation. This parameter specifies the interval in minutes beetween two successive checkpoints, set to zero to disable. This setting has no effect for in-memory database instances. Existing database connections will be closed during the defragmentation process and need to be reopened by the applications.")
    int db_defrag_interval_minutes() default 15;

    @AttributeDefinition(name = "Connection pool max size", min = "1", description = "The H2DbService manages connections using a connection pool. This parameter defines the maximum number of connections for the pool")
    int db_connection_pool_max_size() default 10;

}



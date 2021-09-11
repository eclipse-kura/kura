package org.eclipse.kura.core.db;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(id = "org.eclipse.kura.core.db.H2DbService", name = "DbService", description = "H2 based database service.", localization = "en_us")
public @interface H2DbServiceConfig {

    @AttributeDefinition(name = "Connector URL", cardinality = 0, required = true, description = "JDBC connector URL of the database instance. See http://www.h2database.com/html/features.html for more information. Passing the USER and PASSWORD parameters in the connector URL is not supported, these paramters will be ignored if present. Please use the db.user and db.password fields to provide the credentials.")
    String db_connector_url() default "jdbc:h2:mem:kuradb";

    @AttributeDefinition(name = "User", cardinality = 0, required = true, description = "Specifies the user for the database connection.")
    String db_user() default "SA";

    @AttributeDefinition(name = "Password", type = AttributeType.PASSWORD, cardinality = 0, required = false, description = "Specifies the password. The default password is the empty string.")
    String db_password() default "";

    @AttributeDefinition(name = "Checkpoint interval (seconds)", cardinality = 0, required = true, min = "5", description = "H2DbService instances support running periodic checkpoints to ensure data consistency. This parameter specifies the interval in seconds between two successive checkpoints. This setting has no effect for in-memory database instances.")
    int db_checkpoint_interval_seconds() default 900;

    @AttributeDefinition(name = "Defrag interval (minutes)", cardinality = 0, required = true, min = "0", description = "H2DbService instances support running periodic defragmentation. This parameter specifies the interval in minutes beetween two successive checkpoints, set to zero to disable. This setting has no effect for in-memory database instances. Existing database connections will be closed during the defragmentation process and need to be reopened by the applications.")
    int db_defrag_interval_minutes() default 15;

    @AttributeDefinition(name = "Connection pool max size", cardinality = 0, required = true, min = "1", description = "The H2DbService manages connections using a connection pool. This parameter defines the maximum number of connections for the pool")
    int db_connection_pool_max_size() default 10;

}

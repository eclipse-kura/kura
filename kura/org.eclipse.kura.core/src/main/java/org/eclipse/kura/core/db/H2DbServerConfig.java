package org.eclipse.kura.core.db;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(
        id = "org.eclipse.kura.core.db.H2DbServer",
        name = "H2DbServer",
        description = "H2 based database server.",
        localization = "en_us")
public @interface H2DbServerConfig {

    enum DbServerType {
        WEB,
        TCP,
        PG
    }

    @AttributeDefinition(
            name = "db.server.enabled",
            cardinality = 0,
            required = true,
            description = "Specifies whether the DB server is enabled or not.")
    boolean db_server_enabled() default false;

    @AttributeDefinition(
            name = "db.server.type",
            cardinality = 0,
            required = true,
            description = "Specifies the server type, see http://www.h2database.com/javadoc/org/h2/tools/Server.html for more details.")
    DbServerType db_server_type() default DbServerType.TCP;

    @AttributeDefinition(
            name = "db.server.commandline",
            cardinality = 0,
            required = true,
            description = "Specifies the parameters for the server, see http://www.h2database.com/javadoc/org/h2/tools/Server.html"
                    + " for more details. The listening port must be manually openend in the Firewall configuration section"
                    + " in order to allow external connections.")
    String db_server_commandline() default "-tcpPort 9123 -tcpAllowOthers -ifExists";

}

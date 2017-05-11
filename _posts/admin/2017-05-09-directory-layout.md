---
layout: page
title:  "Directory Layout"
categories: [admin]
---

This section describes the default Kura directory layout that is created upon installation. The default installation directory is as follows:

```
/opt/eclipse
```

The **kura** sub-directory is a logical link on the actual Kura release directory:

```
kura -> /opt/eclipse/kura_3.0.0_raspberry-pi-2
kura_3.0.0_raspberry-pi-2
```

## Kura File Structure
All of the Kura files are located within this parent directory using the structure shown in the following table:

Directory             | Description
-----------------------|-----------------------------
bin                   | Scripts that start Kura
console               | Web console CSS file
data                  | Data of the Kura application
data/db               | HSQL embedded database
data/paho-persistence | Message storage for paho (MQTT client) persistence
data/snapshots        | XML snapshot files; up to 10 successive configurations including the original (see [Snapshot Management](snapshot-management.html))
kura                  | Configuration files for the Kura framework and logging
kura/plugins          | All of the Kura specific jar files
kura/packages         | Deployment package files that are not part of the standard ESF framework, but are installed at a later time (see [Application Management](application-management.html) )
log                   | Log file from the latest Kura installation
plugins               | All of the libraries needed for Kura execution as jar files

## Log Files
ESF regularly updates two log files in the **/var/log** directory:

- **/var/log/kura-console.log** - stores the standard console output of the application. This log file contains the eventual errors that are thrown upon ESF startup.

- **/var/log/kura.log** - stores all of the logging information from ESF bundles. The logger levels are defined in the **log4j.properties** configuration file as shown below:

```
/opt/eclpse/kura/kura/log4j.properties
```

The default logger level in this file is set to INFO. This level may be modified for the whole application or for a specific package as shown in the following example:

```
log4j.logger.org.eclipse.kura=INFO
log4j.logger.org.eclipse.kura.net.admin=DEBUG
```

In this example, the logger level is set to DEBUG only for the **net.admin** bundle. Additional, more specific, properties may be defined as required for your particular logging needs. The logger levels are hierarchical, so that those in a deeper level of the hierarchy will apply; otherwise, the more general logger level will override them.

Once the logger levels are modified as needed and the **log4j.properties** configuration file is saved, Kura must be restarted in order for the changes to take affect.

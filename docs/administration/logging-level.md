# Increase the log level

Increasing the log level can expand the detail of the information gathered by the framework, which is useful for troubleshooting problems and monitoring performance.

To increase the level of detail, set the `org.eclipse.kura` logger to one of the following levels:

* **OFF**: Disables all logging information (discouraged).
* **INFO**: The default level; only relevant information and errors are displayed.
* **DEBUG**: Unlock the debug level, which is useful for debugging applications and displaying more information.
* **TRACE**: Displays a large amount of information tracked by the framework, but is very verbose and can impact the overall performance.

Other levels are available, as described [here](https://www.slf4j.org/api/org/apache/log4j/Level.html).

For example, the configuration below enables the DEBUG level for the loggers `org.eclipse` and `com.eurotech`. This means that all messages produced by the Java classes with these prefixes in their fully qualified class names are included.

As an example of a more fine-graded informational event, with this configuration the **Internet connection status change** event is printed in the log.

``` xml title="log4j.xml"
<?xml version="1.0" encoding="UTF-8"?>
<!--
    Copyright (c) 2020, 2025 Eurotech and/or its affiliates. All rights reserved.
-->
<Configuration status="warn" strict="true" name="KuraConfig"
    monitorInterval="30">

    <Filter type="ThresholdFilter" level="trace" />

    <Appenders>
        <SystemdJournal name="journal" logStacktrace="true" logSource="false" syslogIdentifier="ESF" syslogFacility="20" logLoggerAppName="ESF" />
    </Appenders>

    <Loggers>
        <Logger name="org.eclipse" level="debug" additivity="false">
            <AppenderRef ref="journal" />
        </Logger>
        <Logger name="org.glassfish.jersey.internal" level="error" additivity="false">
            <AppenderRef ref="journal" />
        </Logger>
        <Logger name="com.eurotech" level="debug" additivity="false">
            <AppenderRef ref="journal" />
        </Logger>
        <Root level="info">
            <AppenderRef ref="journal" />
        </Root>
    </Loggers>

</Configuration>
```

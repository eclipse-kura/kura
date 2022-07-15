/*******************************************************************************
 * Copyright (c) 2021, 2022 Eurotech and/or its affiliates and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Eurotech
 *******************************************************************************/
package org.eclipse.kura.log.filesystem.provider;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.kura.log.LogEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class KuraLogLineParser {

    private static final Logger logger = LoggerFactory.getLogger(KuraLogLineParser.class);

    public static final long DEFAULT_TIMESTAMP = new Date(0).getTime();
    public static final String DEFAULT_PID = "undefined";
    public static final String DEFAULT_PRIORITY = "INFO";
    public static final String DEFAULT_SYSLOG_IDENTIFIER = "Kura";
    public static final String DEFAULT_STACKTRACE = "";

    private long timestamp;
    private String pid;
    private String priority;
    private String message;
    private final String transport;
    private String syslogIdentifier;
    private String stacktrace;

    private static KuraLogLineParser instance;

    private KuraLogLineParser(String message, String filepath, String stacktrace) {
        this.timestamp = DEFAULT_TIMESTAMP;
        this.pid = DEFAULT_PID;
        this.priority = DEFAULT_PRIORITY;
        this.message = message;
        this.transport = filepath;
        this.syslogIdentifier = DEFAULT_SYSLOG_IDENTIFIER;
        this.stacktrace = stacktrace;
    }

    public static LogEntry createLogEntry(String message, String filepath, String stacktrace) {
        instance = new KuraLogLineParser(message, filepath, stacktrace);

        if (filepath.contains("kura.log")) {
            instance.parseKuraLog();
        }

        if (filepath.contains("kura-audit.log")) {
            instance.parseKuraAuditLog();
        }

        return instance.generateLogEntry();
    }

    /*
     * kura.log message format:
     *
     * _SOURCE_REALTIME_TIMESTAMP [PID] PRIORITY MESSAGE_WITH_POSSIBLE_SPACES
     */
    private void parseKuraLog() {
        String[] splits = this.message.split(" ");
        if (splits.length >= 3) {
            instance.timestamp = parseStringToEpoch("yyyy-MM-dd'T'hh:mm:ss,S", splits[0]);

            instance.pid = splits[1];
            instance.pid = instance.pid.replace("[", "");
            instance.pid = instance.pid.replace("]", "");
            instance.priority = splits[2];
            StringBuilder sb = new StringBuilder();
            for (int i = 3; i < splits.length; i++) {
                sb.append(splits[i]);
                sb.append(" ");
            }
            instance.message = sb.toString().trim();
        }
    }

    private long parseStringToEpoch(String format, String date) {
        try {
            Date parsedDate = new SimpleDateFormat(format).parse(date);
            return parsedDate.toInstant().getEpochSecond();
        } catch (ParseException e) {
            logger.error("Error parsing Kura log timestamp.", e);
        }
        return 0;
    }

    /*
     * kura-audit.log message format:
     *
     * <ID>NUMBER TIMESTAMP DEVICE SYSLOG_IDENTIFIER - - [RequestContext@28392 category="AuditLogger"
     * exception="STACKTRACE" priority="PRIORITY" thread="PID"] MESSAGE_WITH_POSSIBLE_SPACES
     */
    private void parseKuraAuditLog() {
        String[] splits = this.message.split(" ");
        if (splits.length >= 11) {
            instance.timestamp = parseStringToEpoch("yyyy-MM-dd'T'hh:mm:ss.SSSXXX", splits[1]);

            instance.syslogIdentifier = splits[3];
            instance.stacktrace += splits[8].replace("exception=", "").replace("\"", "");
            instance.priority = splits[9].replace("priority=", "").replace("\"", "");
            instance.pid = splits[10].replace("thread=", "").replace("\"", "").replace("]", "");
            StringBuilder sb = new StringBuilder();
            for (int i = 11; i < splits.length; i++) {
                sb.append(splits[i]);
                sb.append(" ");
            }
            instance.message = sb.toString().trim();
        }
    }

    private LogEntry generateLogEntry() {
        Map<String, Object> entryProperties = new HashMap<>();
        entryProperties.put("_PID", instance.pid);
        entryProperties.put("MESSAGE", instance.message);
        entryProperties.put("PRIORITY", instance.priority);
        entryProperties.put("SYSLOG_IDENTIFIER", instance.syslogIdentifier);
        entryProperties.put("_TRANSPORT", instance.transport);
        entryProperties.put("STACKTRACE", instance.stacktrace);

        return new LogEntry(entryProperties, instance.timestamp);
    }
}

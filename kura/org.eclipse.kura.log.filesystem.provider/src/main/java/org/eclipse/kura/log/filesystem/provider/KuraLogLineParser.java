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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    private static final Pattern PID_PATTERN = Pattern.compile("\\[[A-Za-z0-9 ]*\\]");

    private long timestamp;
    private String pid;
    private String priority;
    private String message;
    private final String filepath;
    private String syslogIdentifier;
    private String stacktrace;
    private boolean pidWhitespaceReplaced;

    public KuraLogLineParser(String message, String filepath, String stacktrace) {
        this.timestamp = DEFAULT_TIMESTAMP;
        this.pid = DEFAULT_PID;
        this.priority = DEFAULT_PRIORITY;
        this.message = message;
        this.filepath = filepath;
        this.syslogIdentifier = DEFAULT_SYSLOG_IDENTIFIER;
        this.stacktrace = stacktrace;
    }

    public LogEntry createLogEntry() {
        if (this.filepath.contains("kura.log")) {
            parseKuraLog();
        }

        if (this.filepath.contains("kura-audit.log")) {
            parseKuraAuditLog();
        }

        return generateLogEntry();
    }

    /*
     * kura.log message format:
     *
     * _SOURCE_REALTIME_TIMESTAMP [PID] PRIORITY MESSAGE_WITH_POSSIBLE_SPACES
     */
    private void parseKuraLog() {
        String[] splits = innerTrimPid(this.message).split(" ");
        if (splits.length >= 3) {
            this.timestamp = parseStringToEpoch("yyyy-MM-dd'T'hh:mm:ss,S", splits[0]);

            this.pid = splits[1];
            this.pid = this.pid.replace("[", "");
            this.pid = this.pid.replace("]", "");

            this.pid = this.pidWhitespaceReplaced ? this.pid.replace("-", " ") : this.pid;

            this.priority = splits[2];
            StringBuilder sb = new StringBuilder();
            for (int i = 3; i < splits.length; i++) {
                sb.append(splits[i]);
                sb.append(" ");
            }
            this.message = sb.toString().trim();
        }
    }

    private String innerTrimPid(String message) {

        String trimmedMessage = message;

        Matcher pidMatcher = PID_PATTERN.matcher(message);

        if (pidMatcher.find()) {
            String foundPid = pidMatcher.group();
            trimmedMessage = trimmedMessage.replace(foundPid, foundPid.replace(" ", "-"));
            this.pidWhitespaceReplaced = true;
        }

        return trimmedMessage;
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
            this.timestamp = parseStringToEpoch("yyyy-MM-dd'T'hh:mm:ss.SSSXXX", splits[1]);

            this.syslogIdentifier = splits[3];
            this.stacktrace += splits[8].replace("exception=", "").replace("\"", "");
            this.priority = splits[9].replace("priority=", "").replace("\"", "");
            this.pid = splits[10].replace("thread=", "").replace("\"", "").replace("]", "");
            StringBuilder sb = new StringBuilder();
            for (int i = 11; i < splits.length; i++) {
                sb.append(splits[i]);
                sb.append(" ");
            }
            this.message = sb.toString().trim();
        }
    }

    private LogEntry generateLogEntry() {
        Map<String, Object> entryProperties = new HashMap<>();
        entryProperties.put("_PID", this.pid);
        entryProperties.put("MESSAGE", this.message);
        entryProperties.put("PRIORITY", this.priority);
        entryProperties.put("SYSLOG_IDENTIFIER", this.syslogIdentifier);
        entryProperties.put("_TRANSPORT", this.filepath);
        entryProperties.put("STACKTRACE", this.stacktrace);

        return new LogEntry(entryProperties, this.timestamp);
    }
}

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
 ******************************************************************************/
package org.eclipse.kura.log.filesystem.provider;

import static org.junit.Assert.assertEquals;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import org.eclipse.kura.log.LogEntry;
import org.junit.Test;

public class KuraLogLineParserTest {

    private static final String EXAMPLE_KURA_LOG_LINE = "2021-11-12T17:13:47,184 [qtp30640932-8244] WARN  o.e.k.w.s.s.SkinServlet - Failed to load skin resource, Resource File /skin.js does not exist";
    private static final String EXAMPLE_KURA_AUDIT_LOG_LINE = "<132>1 2021-11-12T23:32:15.594Z raspberrypi EclipseKura - - [RequestContext@28392 category=\"AuditLogger\" exception=\"\" priority=\"WARN\" thread=\"qtp30640932-60362\"] {entrypoint=Internal} UI Session - Failure - Session expired";
    private static final String EXAMPLE_GENERIC_LOG_LINE = "20211112 - this is a generic log line";
    private static final String EXAMPLE_STACKTRACE = "java.lang.UnsupportedOperationException: null\n    at java.util.Collections$UnmodifiableMap.put(Collections.java:1459) ~[?:1.8.0_282]";
    private static final String EXAMPLE_KURA_LOG_LINE_WITH_EXCEPTION = "14:01:17.971 [Thread-2] ERROR org.eclipse.kura.log.filesystem.provider.FilesystemLogProvider - Unexpected exception in FilesystemLogProvider.";

    private static final String EXAMPLE_KURA_LOG_LINE_WITH_PID_WHITESPACE = "2022-08-10T11:03:30,545 [ConfigurationListener Event Queue] INFO  o.e.k.e.p.ExamplePublisher - Activating ExamplePublisher... Done.";

    private String logLine;
    private String filePath;
    private String stacktrace;
    private LogEntry parsedLogEntry;

    /*
     * Scenarios
     */

    @Test
    public void shouldParseCorrectKuraEntry() throws ParseException {
        givenKuraLogLine(EXAMPLE_KURA_LOG_LINE);

        whenParseLogLine();

        thenLogEntryIs(
                new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss,S").parse("2021-11-12T17:13:47,184").toInstant()
                        .getEpochSecond(), // timestamp
                "qtp30640932-8244", // pid
                "o.e.k.w.s.s.SkinServlet - Failed to load skin resource, Resource File /skin.js does not exist", // message
                "WARN", // priority
                "Kura", // syslogid
                this.filePath, // transport
                ""); // stacktrace
    }

    @Test
    public void shouldParseCorrectAuditEntry() throws ParseException {
        givenKuraAuditLogLine(EXAMPLE_KURA_AUDIT_LOG_LINE);

        whenParseLogLine();

        thenLogEntryIs(
                new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss.SSSXXX").parse("2021-11-12T23:32:15.594Z").toInstant()
                        .getEpochSecond(), // timestamp
                "qtp30640932-60362", // pid
                "{entrypoint=Internal} UI Session - Failure - Session expired", // message
                "WARN", // priority
                "EclipseKura", // syslogid
                this.filePath, // transport
                ""); // stacktrace
    }

    @Test
    public void shouldParseGenericEntry() {
        givenGenericLogLine(EXAMPLE_GENERIC_LOG_LINE);

        whenParseLogLine();

        thenLogEntryIs(KuraLogLineParser.DEFAULT_TIMESTAMP, // timestamp
                KuraLogLineParser.DEFAULT_PID, // pid
                this.logLine, // message
                KuraLogLineParser.DEFAULT_PRIORITY, // priority
                KuraLogLineParser.DEFAULT_SYSLOG_IDENTIFIER, // syslogid
                this.filePath, // transport
                KuraLogLineParser.DEFAULT_STACKTRACE); // stacktrace
    }

    @Test
    public void shouldParseExceptionsCorrectly() {
        givenKuraLogLineWithStacktrace(EXAMPLE_KURA_LOG_LINE_WITH_EXCEPTION, EXAMPLE_STACKTRACE);

        whenParseLogLine();

        thenLogEntryIs(0, // timestamp
                "Thread-2", // pid
                "org.eclipse.kura.log.filesystem.provider.FilesystemLogProvider - Unexpected exception in FilesystemLogProvider.", // message
                "ERROR", // priority
                "Kura", // syslogid
                this.filePath, // transport
                EXAMPLE_STACKTRACE); // stacktrace
    }

    @Test
    public void shouldParseCorrectKuraEntryWithPidWithWhitespace() throws ParseException {
        givenKuraLogLine(EXAMPLE_KURA_LOG_LINE_WITH_PID_WHITESPACE);

        whenParseLogLine();

        thenLogEntryIs(
                new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss,S").parse("2022-08-10T11:03:30,545").toInstant()
                        .getEpochSecond(), // timestamp
                "ConfigurationListener Event Queue", // pid
                "o.e.k.e.p.ExamplePublisher - Activating ExamplePublisher... Done.", // message
                "INFO", // priority
                "Kura", // syslogid
                this.filePath, // transport
                ""); // stacktrace
    }

    /*
     * Steps
     */

    /*
     * Given
     */

    private void givenKuraLogLine(String line) {
        this.logLine = line;
        this.filePath = "/var/log/kura.log";
        this.stacktrace = "";
    }

    private void givenKuraAuditLogLine(String line) {
        this.logLine = line;
        this.filePath = "/var/log/kura-audit.log";
        this.stacktrace = "";
    }

    private void givenGenericLogLine(String line) {
        this.logLine = line;
        this.filePath = "example/file/path/test.log";
        this.stacktrace = "";
    }

    private void givenKuraLogLineWithStacktrace(String line, String stacktrace) {
        this.logLine = line;
        this.filePath = "/var/log/kura.log";
        this.stacktrace = stacktrace;
    }

    /*
     * When
     */

    private void whenParseLogLine() {
        this.parsedLogEntry = null;
        this.parsedLogEntry = new KuraLogLineParser(this.logLine, this.filePath, this.stacktrace).createLogEntry();
    }

    /*
     * Then
     */

    private void thenLogEntryIs(long expectedTimestamp, String expectedPid, String expectedMessage,
            String expectedPriority, String expectedSyslogId, String expectedTransport, String expectedStacktrace) {

        assertEquals(this.parsedLogEntry.getTimestamp(), expectedTimestamp);
        assertEquals(this.parsedLogEntry.getProperties().get("_PID"), expectedPid);
        assertEquals(this.parsedLogEntry.getProperties().get("MESSAGE"), expectedMessage);
        assertEquals(this.parsedLogEntry.getProperties().get("PRIORITY"), expectedPriority);
        assertEquals(this.parsedLogEntry.getProperties().get("SYSLOG_IDENTIFIER"), expectedSyslogId);
        assertEquals(this.parsedLogEntry.getProperties().get("_TRANSPORT"), expectedTransport);
        assertEquals(this.parsedLogEntry.getProperties().get("STACKTRACE"), expectedStacktrace);
    }

}

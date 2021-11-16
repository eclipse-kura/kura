/*******************************************************************************
 * Copyright (c) 2021 Eurotech and/or its affiliates and others
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

import org.eclipse.kura.log.LogEntry;

import org.junit.Test;

public class KuraLogLineParserTest {

    private static final String EXAMPLE_KURA_LOG_LINE = "2021-11-12T17:13:47,184 [qtp30640932-8244] WARN  o.e.k.w.s.s.SkinServlet - Failed to load skin resource, Resource File /skin.js does not exist";
    private static final String EXAMPLE_KURA_AUDIT_LOG_LINE = "<132>1 2021-11-12T23:32:15.594Z raspberrypi EclipseKura - - [RequestContext@28392 category=\"AuditLogger\" exception=\"\" priority=\"WARN\" thread=\"qtp30640932-60362\"] {entrypoint=Internal} UI Session - Failure - Session expired";
    private static final String EXAMPLE_GENERIC_LOG_LINE = "20211112 - this is a generic log line";

    private String logLine;
    private String filePath;
    private LogEntry parsedLogEntry;

    /*
     * Scenarios
     */

    @Test
    public void shouldParseCorrectKuraEntry() {
        givenKuraLogLine(EXAMPLE_KURA_LOG_LINE);

        whenParseLogLine();

        thenLogEntryIs("2021-11-12T17:13:47,184", // timestamp
                "qtp30640932-8244", // pid
                "o.e.k.w.s.s.SkinServlet - Failed to load skin resource, Resource File /skin.js does not exist", // message
                "WARN", // priority
                "Kura", // syslogid
                this.filePath, // transport
                ""); // stacktrace
    }

    @Test
    public void shouldParseCorrectAuditEntry() {
        givenKuraAuditLogLine(EXAMPLE_KURA_AUDIT_LOG_LINE);

        whenParseLogLine();

        thenLogEntryIs("2021-11-12T23:32:15.594Z", // timestamp
                "qtp30640932-60362", // pid
                "{entrypoint=Internal} UI Session - Failure - Session expired", // message
                "WARN", // priority
                "EclipseKura", // syslogid
                "raspberrypi", // transport
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

    /*
     * Steps
     */

    /*
     * Given
     */

    private void givenKuraLogLine(String line) {
        this.logLine = line;
        this.filePath = "/var/log/kura.log";
    }

    private void givenKuraAuditLogLine(String line) {
        this.logLine = line;
        this.filePath = "/var/log/kura-audit.log";
    }

    private void givenGenericLogLine(String line) {
        this.logLine = line;
        this.filePath = "example/file/path/test.log";
    }

    /*
     * When
     */

    private void whenParseLogLine() {
        this.parsedLogEntry = KuraLogLineParser.stringToLogEntry(this.logLine, this.filePath);
    }

    /*
     * Then
     */

    private void thenLogEntryIs(String expectedTimestamp, String expectedPid, String expectedMessage,
            String expectedPriority, String expectedSyslogId, String expectedTransport, String expectedStacktrace) {

        assertEquals(this.parsedLogEntry.getProperties().get("_SOURCE_REALTIME_TIMESTAMP"), expectedTimestamp);
        assertEquals(this.parsedLogEntry.getProperties().get("_PID"), expectedPid);
        assertEquals(this.parsedLogEntry.getProperties().get("MESSAGE"), expectedMessage);
        assertEquals(this.parsedLogEntry.getProperties().get("PRIORITY"), expectedPriority);
        assertEquals(this.parsedLogEntry.getProperties().get("SYSLOG_IDENTIFIER"), expectedSyslogId);
        assertEquals(this.parsedLogEntry.getProperties().get("_TRANSPORT"), expectedTransport);
        assertEquals(this.parsedLogEntry.getProperties().get("STACKTRACE"), expectedStacktrace);
    }

}

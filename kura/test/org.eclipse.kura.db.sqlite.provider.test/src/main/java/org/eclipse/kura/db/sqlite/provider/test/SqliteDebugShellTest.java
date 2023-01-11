/*******************************************************************************
 * Copyright (c) 2022 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.db.sqlite.provider.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.eclipse.kura.core.testutil.service.ServiceUtil;
import org.junit.Test;

public class SqliteDebugShellTest extends SqliteDbServiceTestBase {

    @Test
    public void shouldNotAllowAccessToDatabaseIfNotEnabled() throws Exception {
        givenSqliteDbService(Collections.emptyMap());

        whenCommandIsExecuted("executeQuery " + dbServicePid() + " 'CREATE TABLE FOO (BAR INTEGER);'");

        thenExceptionIsThrown(IllegalArgumentException.class, "is not available");
    }

    @Test
    public void shouldAllowAccessToDatabaseIfEnabled() throws Exception {
        givenSqliteDbService(map("debug.shell.access.enabled", true));

        whenCommandIsExecuted("executeQuery " + dbServicePid() + " 'CREATE TABLE FOO (BAR INTEGER);'");

        thenNoExceptionIsThrown();
        thenCommandOutputContains("0 rows changed");
    }

    @Test
    public void shouldDisableDebugShellOnUpdate() throws Exception {
        givenSqliteDbService(map("debug.shell.access.enabled", true));

        whenSqliteDbServiceIsUpdated(map("debug.shell.access.enabled", false));
        whenCommandIsExecuted("executeQuery " + dbServicePid() + " 'CREATE TABLE FOO (BAR INTEGER);'");

        thenExceptionIsThrown(IllegalArgumentException.class, "is not available");
    }

    @Test
    public void shouldEnableDebugShellOnUpdate() throws Exception {
        givenSqliteDbService(map("debug.shell.access.enabled", false));

        whenSqliteDbServiceIsUpdated(map("debug.shell.access.enabled", true));
        whenCommandIsExecuted("executeQuery " + dbServicePid() + " 'CREATE TABLE FOO (BAR INTEGER);'");

        thenNoExceptionIsThrown();
        thenCommandOutputContains("0 rows changed");
    }

    @Test
    public void shouldPrintResultSet() throws Exception {
        givenSqliteDbService(map("debug.shell.access.enabled", true));
        givenExecutedQuery("CREATE TABLE FOO (T TEXT, N NUMERIC, I INTEGER, R REAL, B BLOB);");
        givenExecutedQuery(
                "INSERT INTO FOO (T, N, I, R, B) VALUES (\"abc\", 123, 12, 12.33, x'0123456789abcdef0123456789abcdef')");

        whenCommandIsExecuted("executeQuery " + dbServicePid() + " 'SELECT T, N, I, R, quote(B) AS B FROM FOO;'");

        thenNoExceptionIsThrown();
        thenCommandOutputEquals("| T\t| N\t| I\t| R\t| B\t|\n"
                + "| abc\t| 123\t| 12\t| 12.33\t| X'0123456789ABCDEF0123456789ABCDEF'\t|\n" + "");
    }

    @Test
    public void shouldReportFailure() throws Exception {
        givenSqliteDbService(map("debug.shell.access.enabled", true));
        givenExecutedQuery("CREATE TABLE FOO (BAR INTEGER);");
        givenExecutedQuery("INSERT INTO FOO VALUES (1)");
        givenExecutedQuery("INSERT INTO FOO VALUES (2)");

        whenCommandIsExecuted("executeQuery " + dbServicePid() + " 'SELECT * FROM NONEXISTING;'");

        thenExceptionIsThrown(SQLException.class, "");
    }

    private final CommandProcessor commandProcessor;

    private final ByteArrayOutputStream out = new ByteArrayOutputStream();
    private final ByteArrayOutputStream err = new ByteArrayOutputStream();

    public SqliteDebugShellTest() throws InterruptedException, ExecutionException, TimeoutException {
        super();
        this.commandProcessor = ServiceUtil.trackService(CommandProcessor.class, Optional.empty()).get(30,
                TimeUnit.SECONDS);
    }

    private void whenCommandIsExecuted(String command) {
        final CommandSession session = commandProcessor.createSession(new InputStream() {

            @Override
            public int read() throws IOException {
                return -1;
            }
        }, out, err);

        try {
            session.execute(command);
        } catch (final Exception e) {
            this.exception = Optional.of(e);
        }
    }

    private void thenCommandOutputContains(String string) {
        final String errString = err.toString();
        final String outString = out.toString();
        assertTrue(string + " not found in out, out : \"" + outString + "\" err : \"" + errString + "\"",
                outString.contains(string));
    }

    private void thenCommandOutputEquals(String string) {
        assertEquals(string, out.toString());
    }
}

/*******************************************************************************
 * Copyright (c) 2026 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.http.server.manager;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.jetty.server.ServerConnector;
import org.junit.Test;

public class ConnectorOpenerTest {

    private static final int ATTEMPTS = 3;
    private static final long NO_DELAY = 0;

    private final ServerConnector connector = mock(ServerConnector.class);
    private final IOException portInUse = new IOException("Failed to bind to 0.0.0.0/0.0.0.0:8080");
    private IOException failure;

    @Test
    public void shouldOpenTheConnectorAtTheFirstAttempt() throws IOException {
        givenAConnectorWhosePortIsFree();

        whenTheConnectorIsOpened();

        thenNoFailureIsReported();
        thenTheConnectorWasOpened(1);
    }

    @Test
    public void shouldRetryWhileThePortIsStillInUse() throws IOException {
        givenAConnectorWhosePortIsReleasedAfterFailures(2);

        whenTheConnectorIsOpened();

        thenNoFailureIsReported();
        thenTheConnectorWasOpened(3);
    }

    @Test
    public void shouldGiveUpAfterTheLastAttempt() throws IOException {
        givenAConnectorWhosePortStaysInUse();

        whenTheConnectorIsOpened();

        thenTheLastFailureIsReported();
        thenTheConnectorWasOpened(ATTEMPTS);
    }

    private void givenAConnectorWhosePortIsFree() throws IOException {
        doNothing().when(this.connector).open();
    }

    private void givenAConnectorWhosePortIsReleasedAfterFailures(final int failures) throws IOException {
        final AtomicInteger calls = new AtomicInteger();
        doAnswer(invocation -> {
            if (calls.incrementAndGet() <= failures) {
                throw this.portInUse;
            }
            return null;
        }).when(this.connector).open();
    }

    private void givenAConnectorWhosePortStaysInUse() throws IOException {
        doThrow(this.portInUse).when(this.connector).open();
    }

    private void whenTheConnectorIsOpened() {
        try {
            ConnectorOpener.open(this.connector, ATTEMPTS, NO_DELAY);
        } catch (final IOException e) {
            this.failure = e;
        }
    }

    private void thenNoFailureIsReported() {
        assertNull(this.failure);
    }

    private void thenTheLastFailureIsReported() {
        assertNotNull(this.failure);
        assertSame(this.portInUse, this.failure);
    }

    private void thenTheConnectorWasOpened(final int times) throws IOException {
        verify(this.connector, times(times)).open();
    }
}

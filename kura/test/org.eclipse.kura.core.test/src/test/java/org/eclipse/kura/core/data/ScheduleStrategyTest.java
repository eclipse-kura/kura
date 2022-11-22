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
 *******************************************************************************/
package org.eclipse.kura.core.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.core.data.AutoConnectStrategy.ConnectionManager;
import org.junit.Test;
import org.mockito.Mockito;
import org.quartz.CronExpression;

public class ScheduleStrategyTest {

    @Test
    public void shouldScheduleFirstConnectionAttempt() {
        givenTime("1/1/2000");
        givenCronExpression("0/2 * * * * ?");

        whenScheduleStrategyIsCreated();

        thenTimeoutIsRequestedAfterMs(2000);
    }

    @Test
    public void shouldRequestConnection() {
        givenTime("1/1/2000");
        givenCronExpression("0/2 * * * * ?");
        givenScheduleStrategy();

        whenTimeoutOccurs();

        thenConnectionTaskIsStarted();
    }

    @Test
    public void shouldScheduleDisconnectTimeout() {
        givenTime("1/1/2000");
        givenCronExpression("0/2 * * * * ?");
        givenScheduleStrategy();
        givenTimeout();

        whenConnectionIsEstablished();

        thenTimeoutIsRequestedAfterMs(60000);
    }

    @Test
    public void shouldRequestDisconnect() {
        givenTime("1/1/2000");
        givenCronExpression("0/2 * * * * ?");
        givenScheduleStrategy();
        givenTimeout();
        givenConnectionEstablished();

        whenTimeoutOccurs();

        thenDisconnectIsRequested();
        thenConnectionTaskIsStopped();
    }

    private final ExecutorState executorState = new ExecutorState();
    private final ConnectionManagerState connectionManagerState = new ConnectionManagerState();
    private Date now;
    private CronExpression expression;
    private ScheduleStrategy strategy;
    private long disconnectTimeoutMs = 60000;

    private void givenCronExpression(String expressionString) {
        try {
            this.expression = new CronExpression(expressionString);
        } catch (ParseException e) {
            throw new IllegalStateException("failed to parse cron expression", e);
        }

    }

    private void givenTime(final String time) {
        try {
            now = new SimpleDateFormat("dd/MM/yyyy").parse(time);
        } catch (ParseException e) {
            throw new IllegalStateException("failed to parse date", e);
        }
    }

    private void givenScheduleStrategy() {
        whenScheduleStrategyIsCreated();
    }

    private void givenTimeout() {
        whenTimeoutOccurs();
    }

    private void givenConnectionEstablished() {
        whenConnectionIsEstablished();
    }

    private void whenTimeoutOccurs() {
        executorState.triggerTimeout();
    }

    private void whenScheduleStrategyIsCreated() {
        this.strategy = new ScheduleStrategy(expression, disconnectTimeoutMs,
                this.connectionManagerState.connectionManager,
                this.executorState.executor, () -> this.now);
    }

    private void whenConnectionIsEstablished() {
        this.strategy.onConnectionEstablished();
    }

    private void thenTimeoutIsRequestedAfterMs(long expectedDelay) {
        assertEquals(expectedDelay, this.executorState.getDelay());
    }

    private void thenConnectionTaskIsStarted() {
        try {
            connectionManagerState.startConnectionTask.get(30, TimeUnit.SECONDS);
        } catch (Exception e) {
            fail("connection task not started");
        }
    }

    private void thenConnectionTaskIsStopped() {
        try {
            connectionManagerState.stopConnectionTask.get(30, TimeUnit.SECONDS);
        } catch (Exception e) {
            fail("connection task not stopped");
        }
    }

    private void thenDisconnectIsRequested() {
        try {
            connectionManagerState.disconnect.get(30, TimeUnit.SECONDS);
        } catch (Exception e) {
            fail("disconnect not requested");
        }
    }

    private class ConnectionManagerState {
        private ConnectionManager connectionManager = mock(ConnectionManager.class);
        private CompletableFuture<?> startConnectionTask = new CompletableFuture<>();
        private CompletableFuture<?> stopConnectionTask = new CompletableFuture<>();
        private CompletableFuture<?> disconnect = new CompletableFuture<>();

        ConnectionManagerState() {
            doAnswer(i -> {
                startConnectionTask.complete(null);
                return null;
            }).when(connectionManager).startConnectionTask();

            doAnswer(i -> {
                stopConnectionTask.complete(null);
                return null;
            }).when(connectionManager).stopConnectionTask();

            doAnswer(i -> {
                disconnect.complete(null);
                return null;
            }).when(connectionManager).disconnect();

            when(connectionManager.isConnected()).thenReturn(false);
        }
    }

    private class ExecutorState {
        private ScheduledExecutorService executor = mock(ScheduledExecutorService.class);
        private CompletableFuture<Runnable> task = new CompletableFuture<>();
        private CompletableFuture<Long> lastDelay = new CompletableFuture<>();

        public ExecutorState() {
            Mockito.when(executor.schedule((Runnable) Mockito.any(), Mockito.anyLong(), Mockito.any()))
                    .thenAnswer(i -> {

                        this.task.complete(i.getArgument(0, Runnable.class));
                        this.lastDelay.complete(i.getArgument(1, Long.class));

                        return Mockito.mock(ScheduledFuture.class);
                    });

            Mockito.doAnswer(i -> {
                i.getArgument(0, Runnable.class).run();

                return Mockito.mock(ScheduledFuture.class);
            }).when(executor).execute(Mockito.any());
        }

        private void triggerTimeout() {
            try {
                this.task.get(30, TimeUnit.SECONDS).run();
                this.task = new CompletableFuture<>();
                this.lastDelay = new CompletableFuture<>();
            } catch (Exception e) {
                throw new IllegalStateException("timeout not set");
            }
        }

        private long getDelay() {
            try {
                return lastDelay.get(30, TimeUnit.SECONDS);
            } catch (Exception e) {
                throw new IllegalStateException("timeout not set");
            }
        }
    }
}

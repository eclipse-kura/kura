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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.core.data.AutoConnectStrategy.ConnectionManager;
import org.junit.Test;
import org.mockito.Mockito;
import org.quartz.CronExpression;

public class ScheduleStrategyTest {

    private DataServiceOptions dataServiceOptions;

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

    @Test
    public void shouldForceReconnectOutsideOfSchedule() {
        givenTime("1/1/2000");
        givenCronExpression("0/2 * * * * ?");
        givenScheduleStrategy();
        givenTimeout();

        whenScheduleStrategyIsCreatedWithAlternativeConstructor();

        whenMessageIsSent();

        thenConnectionTaskIsNotStarted();

        whenPriorityMessageIsSent();

        thenConnectionTaskIsStarted();
    }
    
    @Test
    public void shouldReconnectIfMessageIsSentDuringDisconnect() {
        givenTime("1/1/2000");
        givenCronExpression("0/2 * * * * ?");
        givenScheduleStrategy();
        givenTimeout();
        
        whenScheduleStrategyIsCreatedWithAlternativeConstructor();
        
        whenMessageIsSent();
        
        thenConnectionTaskIsNotStarted();
        
        whenPriorityMessageIsSent();
        
        thenConnectionTaskIsStarted();
        
        //Create Minor Delay
        whenMessageIsSent();
        whenMessageIsSent();
        whenMessageIsSent();
        
        whenPriorityMessageIsSent();
        
        thenConnectionTaskIsStarted();
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

        // Create DataServiceOptions
        Map<String, Object> properties = new HashMap<>();
        properties.put("connection.schedule.priority.override.enable", true);
        properties.put("connection.schedule.priority.override.threshold", 3);
        properties.put("connect.auto-on-startup", true);
        properties.put("connection.schedule.inactivity.interval.seconds", disconnectTimeoutMs);
        properties.put("connection.schedule.enabled", true);
        properties.put("connection.schedule.expression", expression.toString());

        this.dataServiceOptions = new DataServiceOptions(properties);

        this.strategy = new ScheduleStrategy(expression, disconnectTimeoutMs,
                this.connectionManagerState.connectionManager,
                this.executorState.executor, () -> this.now, this.dataServiceOptions);
    }

    private void whenScheduleStrategyIsCreatedWithAlternativeConstructor() {

        // Create DataServiceOptions
        Map<String, Object> properties = new HashMap<>();
        properties.put("connection.schedule.priority.override.enable", true);
        properties.put("connection.schedule.priority.override.threshold", 3);
        properties.put("connect.auto-on-startup", true);
        properties.put("connection.schedule.inactivity.interval.seconds", disconnectTimeoutMs);
        properties.put("connection.schedule.enabled", true);
        properties.put("connection.schedule.expression", expression.toString());
        properties.put("connection.schedule.inactivity.interval.seconds", (long) 1);

        this.dataServiceOptions = new DataServiceOptions(properties);

        this.strategy = new ScheduleStrategy(this.expression, this.dataServiceOptions,
                this.connectionManagerState.connectionManager);
    }

    private void whenConnectionIsEstablished() {
        this.strategy.onConnectionEstablished();
    }

    private void whenMessageIsSent() {
        this.strategy.onPublishRequested("test/topic", null, 0, false, 7);
    }

    private void whenPriorityMessageIsSent() {
        this.strategy.onPublishRequested("test/topic", null, 0, false, 0);
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

    private void thenConnectionTaskIsNotStarted() {
        assertFalse(connectionManagerState.connectionManager.isConnected());
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

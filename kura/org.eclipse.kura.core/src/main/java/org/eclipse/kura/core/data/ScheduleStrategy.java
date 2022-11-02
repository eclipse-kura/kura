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
package org.eclipse.kura.core.data;

import java.util.Date;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import org.quartz.CronExpression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScheduleStrategy implements AutoConnectStrategy {

    private static final Logger logger = LoggerFactory.getLogger(ScheduleStrategy.class);

    private final ScheduledExecutorService executor;

    private final CronExpression expression;
    private final long disconnectTimeoutMs;
    private final ConnectionManager connectionManager;
    private final Supplier<Date> currentTimeProvider;
    private State state;

    private Optional<ScheduledFuture<?>> timeout = Optional.empty();

    public ScheduleStrategy(final CronExpression expression, final long disconnectTimeoutMs,
            final ConnectionManager connectionManager) {
        this(expression, disconnectTimeoutMs, connectionManager, Executors.newSingleThreadScheduledExecutor(),
                Date::new);
    }

    public ScheduleStrategy(final CronExpression expression, final long disconnectTimeoutMs,
            final ConnectionManager connectionManager, final ScheduledExecutorService executor,
            final Supplier<Date> currentTimeProvider) {
        this.expression = expression;
        this.disconnectTimeoutMs = disconnectTimeoutMs;
        this.connectionManager = connectionManager;
        this.state = new AwaitConnectTime();
        this.executor = executor;
        this.currentTimeProvider = currentTimeProvider;

        updateState(State::onEnterState);
        executor.scheduleWithFixedDelay(new TimeShiftDetector(60000), 0, 1, TimeUnit.MINUTES);
    }

    private interface State {
        public default State onEnterState() {
            return this;
        }

        public default State onConnectionEstablished() {
            return this;
        }

        public default State onMessageEvent() {
            return this;
        }

        public default State onConnectionLost() {
            return this;
        }

        public default State onTimeout() {
            return this;
        }
    }

    private class AwaitConnectTime implements State {

        @Override
        public State onEnterState() {
            connectionManager.stopConnectionTask();
            connectionManager.disconnect();

            final Date now = currentTimeProvider.get();

            final Date nextTick = expression.getNextValidTimeAfter(now);

            final long delay = Math.max(1, nextTick.getTime() - now.getTime());

            logger.info("Connection scheduled at {} in {} ms", nextTick, delay);

            rescheduleTimeout(delay);

            return this;
        }

        @Override
        public State onTimeout() {

            return new AwaitConnect();
        }

    }

    private class AwaitConnect implements State {

        @Override
        public State onEnterState() {
            if (connectionManager.isConnected()) {
                return new AwaitDisconnectTime();
            } else {
                connectionManager.startConnectionTask();
                return this;
            }
        }

        @Override
        public State onConnectionLost() {
            connectionManager.startConnectionTask();
            return this;
        }

        @Override
        public State onConnectionEstablished() {
            return new AwaitDisconnectTime();
        }

    }

    private class AwaitDisconnectTime implements State {

        @Override
        public State onEnterState() {
            return onMessageEvent();
        }

        @Override
        public State onConnectionLost() {
            return new AwaitConnect();
        }

        @Override
        public State onMessageEvent() {

            rescheduleTimeout(disconnectTimeoutMs);

            return this;
        }

        @Override
        public State onTimeout() {
            if (connectionManager.hasInFlightMessages()) {
                return this;
            } else {
                return new AwaitConnectTime();
            }
        }
    }

    private void rescheduleTimeout(final long timeoutMs) {
        final Optional<ScheduledFuture<?>> currentFuture = this.timeout;

        if (currentFuture.isPresent()) {
            currentFuture.get().cancel(false);
        }

        this.timeout = Optional.of(executor.schedule(() -> updateState(State::onTimeout), timeoutMs,
                TimeUnit.MILLISECONDS));
    }

    private void updateState(final UnaryOperator<State> transition) {
        executor.execute(() -> updateStateInternal(transition));
    }

    private void updateStateInternal(final UnaryOperator<State> transitionFunction) {
        final Optional<ScheduledFuture<?>> currentFuture = this.timeout;

        final State nextState = transitionFunction.apply(this.state);

        if (nextState != this.state) {
            logger.info("State change: {} -> {}", state.getClass().getSimpleName(),
                    nextState.getClass().getSimpleName());
            currentFuture.ifPresent(c -> c.cancel(false));

            this.state = nextState;
            updateStateInternal(State::onEnterState);
        }
    }

    private class TimeShiftDetector implements Runnable {
        private OptionalLong previousTimestamp = OptionalLong.empty();
        private final long expectedDelay;

        public TimeShiftDetector(final long expectedTickRate) {
            this.expectedDelay = expectedTickRate;
        }

        public void run() {
            final long now = System.currentTimeMillis();

            final OptionalLong previous = this.previousTimestamp;

            if (!previous.isPresent()) {
                this.previousTimestamp = OptionalLong.of(now);
                return;
            }

            if (now < previous.getAsLong() || Math.abs((now - previous.getAsLong()) - expectedDelay) > 60000) {
                logger.warn("Time shift detected, reinitializing connection schedule");
                updateState(c -> new AwaitConnectTime());
            }

            this.previousTimestamp = OptionalLong.of(now);

        }

    }

    @Override
    public void shutdown() {
        executor.shutdown();
        try {
            executor.awaitTermination(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Interrupted while waiting for executor shutdown");
        }
    }

    @Override
    public void onConnectionEstablished() {
        updateState(State::onConnectionEstablished);
    }

    @Override
    public void onDisconnecting() {
        // no need
    }

    @Override
    public void onDisconnected() {
        updateState(State::onConnectionLost);
    }

    @Override
    public void onConnectionLost(Throwable cause) {
        updateState(State::onConnectionLost);
    }

    @Override
    public void onMessageArrived(String topic, byte[] payload, int qos, boolean retained) {
        // no need
    }

    @Override
    public void onMessagePublished(int messageId, String topic) {
        updateState(State::onMessageEvent);
    }

    @Override
    public void onMessageConfirmed(int messageId, String topic) {
        updateState(State::onMessageEvent);
    }

}

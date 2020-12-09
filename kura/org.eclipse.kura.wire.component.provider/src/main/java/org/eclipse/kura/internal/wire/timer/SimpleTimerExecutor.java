/*******************************************************************************
 * Copyright (c) 2020 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.internal.wire.timer;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.wire.WireSupport;

public class SimpleTimerExecutor implements TimerExecutor {

    private final ScheduledExecutorService executor;

    public SimpleTimerExecutor(final TimerOptions options, final WireSupport wireSupport) {
        this.executor = Executors.newSingleThreadScheduledExecutor(getThreadFactory(options.getOwnPid()));

        long firstTickInterval = options.getSimpleInterval() * options.getSimpleTimeUnitMultiplier();
        if (!options.isDefaultFirstTickBehavior()) {
            firstTickInterval = options.firstTickInterval() * options.getSimpleTimeUnitMultiplier();
        }

        this.executor.scheduleAtFixedRate(() -> Timer.emit(wireSupport), firstTickInterval,
                options.getSimpleInterval() * options.getSimpleTimeUnitMultiplier(), TimeUnit.MILLISECONDS);
    }

    private static ThreadFactory getThreadFactory(final String pid) {
        final ThreadFactory defaultFactory = Executors.defaultThreadFactory();

        return runnable -> {
            final Thread result = defaultFactory.newThread(runnable);
            result.setName("WiresTimer_" + pid + "_" + System.identityHashCode(result));
            return result;
        };
    }

    @Override
    public void shutdown() {
        this.executor.shutdownNow();
    }
}

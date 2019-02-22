/**
 * Copyright (c) 2018 Eurotech and/or its affiliates and others
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.eclipse.kura.internal.driver.opcua;

import java.util.ArrayDeque;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class AsyncTaskQueue {

    private final ArrayDeque<Supplier<CompletableFuture<Void>>> pending = new ArrayDeque<Supplier<CompletableFuture<Void>>>();
    private final AtomicBoolean enabled = new AtomicBoolean(true);

    private CompletableFuture<Void> inProgress = null;
    private Consumer<Throwable> failureHandler = ex -> {
    };

    private synchronized void runNext() {
        this.inProgress = null;
        if (!pending.isEmpty()) {
            this.inProgress = pending.pop().get();
        }
    }

    private CompletableFuture<Void> addCompletionHandler(CompletableFuture<Void> task) {
        return task.handle((ok, err) -> {
            if (err != null && enabled.get()) {
                this.failureHandler.accept(err);
            }
            runNext();
            return null;
        });
    }

    public synchronized void push(final Supplier<CompletableFuture<Void>> next) {
        if (!enabled.get()) {
            return;
        }
        pending.push(() -> addCompletionHandler(next.get()));
        if (inProgress == null || inProgress.isDone()) {
            runNext();
        }
    }

    public void onFailure(Consumer<Throwable> failureHandler) {
        this.failureHandler = failureHandler;
    }

    public synchronized void close(final Supplier<CompletableFuture<Void>> last) {
        this.failureHandler = ex -> {
        };
        this.pending.clear();
        push(() -> {
            this.enabled.set(false);
            return last.get();
        });
    }
}

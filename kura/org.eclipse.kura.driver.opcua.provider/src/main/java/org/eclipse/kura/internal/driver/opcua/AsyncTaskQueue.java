/**
 * Copyright (c) 2018, 2020 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 */

package org.eclipse.kura.internal.driver.opcua;

import java.util.ArrayDeque;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class AsyncTaskQueue {

    private final ArrayDeque<Supplier<CompletableFuture<Void>>> pending = new ArrayDeque<>();
    private final AtomicBoolean enabled = new AtomicBoolean(true);

    private CompletableFuture<Void> inProgress = null;
    private Consumer<Throwable> failureHandler = ex -> {
    };

    private synchronized void runNext() {
        this.inProgress = null;
        if (!this.pending.isEmpty()) {
            this.inProgress = this.pending.pop().get();
        }
    }

    private CompletableFuture<Void> addCompletionHandler(CompletableFuture<Void> task) {
        return task.handle((ok, err) -> {
            if (err != null && this.enabled.get()) {
                this.failureHandler.accept(err);
            }
            runNext();
            return null;
        });
    }

    public synchronized void push(final Supplier<CompletableFuture<Void>> next) {
        if (!this.enabled.get()) {
            return;
        }
        this.pending.push(() -> addCompletionHandler(next.get()));
        if (this.inProgress == null || this.inProgress.isDone()) {
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

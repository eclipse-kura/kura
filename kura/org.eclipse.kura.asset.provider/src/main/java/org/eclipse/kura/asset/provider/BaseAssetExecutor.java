package org.eclipse.kura.asset.provider;

/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *     
 *******************************************************************************/
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BaseAssetExecutor {

    private static final Logger logger = LoggerFactory.getLogger(BaseAssetExecutor.class);

    private final ExecutorService ioExecutor;
    private final boolean isIoExecutorShared;

    private final ExecutorService configExecutor;
    private final boolean isConfigExecutorShared;

    private AtomicReference<CompletableFuture<Void>> queue = new AtomicReference<>(
            CompletableFuture.completedFuture(null));

    public BaseAssetExecutor(final ExecutorService ioExecutor, final ExecutorService configExecutor) {
        this(ioExecutor, false, configExecutor, false);
    }

    public BaseAssetExecutor(final ExecutorService ioExecutor, final boolean isIoExecutorShared,
            final ExecutorService configExecutor, final boolean isConfigExecutorShared) {
        this.ioExecutor = ioExecutor;
        this.isIoExecutorShared = isIoExecutorShared;
        this.configExecutor = configExecutor;
        this.isConfigExecutorShared = isConfigExecutorShared;
    }

    public <T> CompletableFuture<T> runIO(final Callable<T> task) {
        final CompletableFuture<T> result = new CompletableFuture<>();

        ioExecutor.execute(() -> {
            try {
                result.complete(task.call());
            } catch (Exception e) {
                result.completeExceptionally(e);
            }
        });

        return result;
    }

    public CompletableFuture<Void> runConfig(final Runnable task) {

        final CompletableFuture<Void> next = new CompletableFuture<>();
        final CompletableFuture<Void> previous = queue.getAndSet(next);

        previous.whenComplete((ok, err) -> configExecutor.execute(() -> {
            try {
                task.run();
                next.complete(null);
            } catch (Exception e) {
                logger.warn("Asset task failed", e);
                next.completeExceptionally(e);
            }
        }));

        return next;
    }

    public void shutdown() {
        if (!isIoExecutorShared) {
            ioExecutor.shutdown();
        }
        if (!isConfigExecutorShared) {
            configExecutor.shutdown();
        }
    }

}

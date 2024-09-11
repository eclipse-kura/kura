/*******************************************************************************
 * Copyright (c) 2023 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.core.data.store;

import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.KuraStoreException;
import org.eclipse.kura.core.data.DataServiceOptions;
import org.eclipse.kura.message.store.provider.MessageStore;
import org.eclipse.kura.message.store.provider.MessageStoreProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessageStoreState {

    private static final Logger logger = LoggerFactory.getLogger(MessageStoreState.class);

    private final MessageStoreProvider messageStoreProvider;

    private DataServiceOptions options;
    private Optional<MessageStore> messageStore = Optional.empty();
    private Optional<ScheduledExecutorService> houseKeeperExecutor = Optional.empty();

    public MessageStoreState(final MessageStoreProvider messageStoreProvider, final DataServiceOptions options) {

        this.messageStoreProvider = messageStoreProvider;

        update(options);
    }

    public synchronized void update(final DataServiceOptions dataServiceOptions) {
        this.options = dataServiceOptions;

        shutdown();

        if (!this.houseKeeperExecutor.isPresent()) {
            this.houseKeeperExecutor = Optional.of(Executors.newSingleThreadScheduledExecutor());
            this.houseKeeperExecutor.get().scheduleWithFixedDelay(
                    new HouseKeeperTask(this, dataServiceOptions.getStorePurgeAge()), 1, // start in one second
                    dataServiceOptions.getStoreHousekeeperInterval(), // repeat every retryInterval until we stopped.
                    TimeUnit.SECONDS);
        }
    }

    public MessageStoreProvider getMessageStoreProvider() {
        return messageStoreProvider;
    }

    public synchronized MessageStore getOrOpenMessageStore() throws KuraStoreException {
        if (this.messageStore.isPresent()) {
            return this.messageStore.get();
        }

        return this.openMessageStore();
    }

    public synchronized MessageStore openMessageStore() throws KuraStoreException {

        final MessageStore result = this.messageStoreProvider.openMessageStore(this.options.getKuraServicePid());

        this.messageStore = Optional.of(result);

        return result;
    }

    public synchronized void shutdown() {
        if (this.houseKeeperExecutor.isPresent()) {
            this.houseKeeperExecutor.get().shutdown();
            try {
                this.houseKeeperExecutor.get().awaitTermination(30, TimeUnit.SECONDS);
            } catch (final InterruptedException e) {
                logger.warn("Interrupted while waiting for housekeeper task shutdown", e);
                Thread.currentThread().interrupt();
            }
            this.houseKeeperExecutor = Optional.empty();
        }

        if (this.messageStore.isPresent()) {
            this.messageStore.get().close();
            this.messageStore = Optional.empty();
        }
    }
}

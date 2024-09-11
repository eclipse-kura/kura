/*******************************************************************************
 * Copyright (c) 2011, 2023 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.core.data.store;

import org.eclipse.kura.KuraStoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Housekeeper Task which periodically purges confirmed messages from the local
 * database.
 * It also contains the total number of messages in the system to a given cap.
 */
public class HouseKeeperTask implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(HouseKeeperTask.class);

    private final int purgeAge;
    private final MessageStoreState store;

    public HouseKeeperTask(MessageStoreState store, int purgeAge) {
        this.purgeAge = purgeAge;
        this.store = store;
    }

    @Override
    public void run() {
        try {
            Thread.currentThread().setName(getClass().getSimpleName());
            logger.info("HouseKeeperTask started.");

            //
            // delete all confirmed messages
            logger.info("HouseKeeperTask: Delete confirmed messages...");
            this.store.getOrOpenMessageStore().deleteStaleMessages(this.purgeAge);

            logger.info("HouseKeeperTask ended.");
        } catch (KuraStoreException me) { // do not throw the exception as that will stop future executions
            logger.warn("HouseCleaningTask exception", me);
        } catch (Throwable t) { // do not throw the exception as that will stop future executions
            if (t instanceof InterruptedException) {
                logger.info("HouseCleaningTask stopped");
            } else {
                logger.warn("HouseCleaningTask exception", t);
            }
        }
    }
}

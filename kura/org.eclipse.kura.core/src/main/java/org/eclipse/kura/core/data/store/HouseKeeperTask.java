/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.core.data.store;

import org.eclipse.kura.KuraStoreException;
import org.eclipse.kura.core.data.DataStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Housekeeper Task which periodically purges confirmed messages from the local database.
 * It also contains the total number of messages in the system to a given cap.
 */
public class HouseKeeperTask implements Runnable {

    private static final Logger s_logger = LoggerFactory.getLogger(HouseKeeperTask.class);

    private final int m_purgeAge;
    private final boolean doRepair;
    private final DataStore m_store;

    public HouseKeeperTask(DataStore store, int purgeAge, boolean doRepair) {
        this.m_purgeAge = purgeAge;
        this.m_store = store;
        this.doRepair = doRepair;
    }

    @Override
    public void run() {
        try {
            Thread.currentThread().setName(getClass().getSimpleName());
            s_logger.info("HouseKeeperTask started.");

            //
            // check and attempt to repair the store
            if (this.doRepair) {
                s_logger.info("HouseKeeperTask: Check store...");
                this.m_store.repair();
            }

            //
            // delete all confirmed messages
            s_logger.info("HouseKeeperTask: Delete confirmed messages...");
            this.m_store.deleteStaleMessages(this.m_purgeAge);

            // delete overflowing messages
            // s_logger.info("HouseKeeperTask: Delete overflow messages...");
            // String maxNumMsgsStr = m_config.getProperty("data.service.store.max_number_of_messages");
            // int maxNumMsgs = Integer.parseInt(maxNumMsgsStr);
            // m_store.deleteOverflowMessages(maxNumMsgs);

            s_logger.info("HouseKeeperTask ended.");
        } catch (KuraStoreException me) { // do not throw the exception as that will stop future executions
            s_logger.warn("HouseCleaningTask exception", me);
        } catch (Throwable t) { // do not throw the exception as that will stop future executions
            if (t instanceof InterruptedException) {
                s_logger.info("HouseCleaningTask stopped");
            } else {
                s_logger.warn("HouseCleaningTask exception", t);
            }
        }
    }
}

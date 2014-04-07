/**
 * Copyright (c) 2011, 2014 Eurotech and/or its affiliates
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Eurotech
 */
package org.eclipse.kura.core.data.store;

import org.eclipse.kura.KuraStoreException;
import org.eclipse.kura.core.data.DataStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Housekeeper Task which periodically purges confirmed messages from the local database.
 * It also contains the total number of messages in the system to a given cap. 
 */
public class HouseKeeperTask implements Runnable 
{
	private static final Logger s_logger = LoggerFactory.getLogger(HouseKeeperTask.class);
	
    private int               m_purgeAge;
    private boolean			  m_doCheckpoint;
	private DataStore         m_store;
	
	public HouseKeeperTask(DataStore store, int purgeAge, boolean doCheckpoint)
	{
		m_purgeAge = purgeAge;
		m_doCheckpoint = doCheckpoint;
		m_store  = store;
	}
	
	
	@Override
	public void run()
	{	
		//String originalName = Thread.currentThread().getName(); 
		try {
			Thread.currentThread().setName(getClass().getSimpleName());
			s_logger.info("HouseKeeperTask started.");

			//
			// delete all confirmed messages
			s_logger.info("HouseKeeperTask: Delete confirmed messages...");	
			m_store.deleteStaleMessages(m_purgeAge);

			// delete overflowing messages
			//			s_logger.info("HouseKeeperTask: Delete overflow messages...");
			//			String maxNumMsgsStr = m_config.getProperty("data.service.store.max_number_of_messages");			
			//			int maxNumMsgs = Integer.parseInt(maxNumMsgsStr);			
			//			m_store.deleteOverflowMessages(maxNumMsgs);

			if (m_doCheckpoint) {
				s_logger.info("HouseKeeperTask: Performing store checkpoint with defrag...");
				m_store.defrag();
			}
			
			s_logger.info("HouseKeeperTask ended.");
		}
		//
		// do not throw the exception as that will stop future executions
		catch (KuraStoreException me) {
			s_logger.warn("HouseCleaningTask exception", me);
		}
		catch (Throwable t) {
			if (t instanceof InterruptedException) {
				s_logger.info("HouseCleaningTask stopped");
			}
			else {
				s_logger.warn("HouseCleaningTask exception", t);
			}
		}
		finally {
			Thread.currentThread().setName(getClass().getSimpleName());			
		}
	}    	
}

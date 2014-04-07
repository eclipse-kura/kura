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
package org.eclipse.kura.core.util;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


/**
 * ExecutorUtil should be used for any asynchronous threads to be kicked off by a bundle/service.
 * ExecutorUtil centralizes the number of threads running in the whole JVM instance by
 * controlling the Thread pool and queuing tasks if resources are not available.
 * 
 * TODO: use configuration service/file to parametrize this as sizing
 * may depend on the hardware configuration
 */
public class ExecutorUtil extends ScheduledThreadPoolExecutor 
{		
	private static ExecutorUtil s_instance = new ExecutorUtil(); 
	
	/**
	 * Private constructor for the singleton.
	 */
	private ExecutorUtil()
	{
		super(50, new NamedThreadFactory());     // corePoolSize, 
		setMaximumPoolSize(100);	             // maximumPoolSize, 
		setKeepAliveTime(1, TimeUnit.SECONDS);   // keepAliveTime,
//	      new ArrayBlockingQueue<Runnable>(10)); // workQueue
	}
	
	
	/**
	 * Returns the ExecutorUtil instance that should be used
	 * by all service/bundles when they need to kick off 
	 * background or asynchronous tasks.
	 *  
	 * @return ExecutorUtil 
	 */
	public static ExecutorUtil getInstance() {
		return s_instance;
	}
	
}

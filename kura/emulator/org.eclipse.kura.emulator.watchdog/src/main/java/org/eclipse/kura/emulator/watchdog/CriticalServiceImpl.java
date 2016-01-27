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
/*
* Copyright (c) 2011 Eurotech Inc. All rights reserved.
*/

package org.eclipse.kura.emulator.watchdog;

public class CriticalServiceImpl {

	private String name;
	private long timeout;
	private long updated;
	
	/**
	 * 
	 * @param name
	 * @param timeout		timeout for reporting interval in seconds
	 */
	public CriticalServiceImpl(String name, long timeout) {
		this.name = name;
		this.timeout = timeout;
		this.updated = System.currentTimeMillis();
	}
	
	public String getName() {
		return name;
	}
	
	public long getTimeout() {
		return timeout;
	}
	
	public boolean isTimedOut() {
		long current = System.currentTimeMillis();
		return timeout < (current - updated);
	}
	
	public void update() {
		updated = System.currentTimeMillis();
	}
	
	public String toString() {
		return "Service Name:  " + name + ", Timeout(ms):  " + timeout;
	}
}

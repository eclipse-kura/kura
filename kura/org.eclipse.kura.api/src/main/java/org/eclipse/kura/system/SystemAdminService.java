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
package org.eclipse.kura.system;

/**
 * Service to perform basic system tasks.
 * 
 * Copyright (c) 2012 Eurotech Inc. All rights reserved.
 */
public interface SystemAdminService 
{
	/**
	 * Gets the amount of time this device has been up in milliseconds.
	 * 
	 * @return				How long this device has been up in milliseconds.
	 */
	public String getUptime();
	
	/**
	 * Reboots the device.
	 */
	public void reboot();
	
	/**
	 * Synchronizes data on flash with memory.
	 */
	public void sync();
}

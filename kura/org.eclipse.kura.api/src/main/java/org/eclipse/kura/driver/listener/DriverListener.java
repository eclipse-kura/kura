/**
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Eurotech
 *   Amit Kumar Mondal (admin@amitinside.com)
 */
package org.eclipse.kura.driver.listener;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.KuraRuntimeException;
import org.eclipse.kura.driver.DriverEvent;

/**
 * The listener interface DriverListener is mainly for receiving driver events.
 * The class that is interested in processing a driver event implements this
 * interface, and the object created with that class is registered with a driver
 * component using the driver component's
 * {@code Driver#registerDriverListener(java.util.Map, DriverListener)} method.
 * When the driver event occurs, that object's appropriate method is invoked.
 *
 * @see DriverEvent
 */
public interface DriverListener {

	/**
	 * Triggers on driver event
	 *
	 * @param event
	 *            the fired driver event
	 * @throws KuraException
	 *             if the driver encounters any operation specific exception
	 * @throws KuraRuntimeException
	 *             if event is null
	 */
	public void onDriverEvent(DriverEvent event) throws KuraException;

}

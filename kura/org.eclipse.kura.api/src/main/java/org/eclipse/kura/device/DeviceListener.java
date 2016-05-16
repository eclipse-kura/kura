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
package org.eclipse.kura.device;

import org.eclipse.kura.KuraRuntimeException;

/**
 * The listener interface is mainly for receiving device events. The class that
 * is interested in processing a device event implements this interface, and the
 * object created with that class is registered with a device component using
 * the device component's
 * {@code Device#registerDeviceListener(String, DeviceListener)} method. When
 * the device event occurs, that object's appropriate method is invoked.
 *
 * @see DeviceEvent
 */
public interface DeviceListener {

	/**
	 * Triggers on device event
	 *
	 * @param event
	 *            the fired device event
	 * @throws KuraRuntimeException
	 *             if event is null
	 */
	public void onDeviceEvent(DeviceEvent event);

}

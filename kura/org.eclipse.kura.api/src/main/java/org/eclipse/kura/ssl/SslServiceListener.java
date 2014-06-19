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

package org.eclipse.kura.ssl;

/**
 * Listener interface to be implemented by applications that needs to be notified of events in the {@link SslManagerService}.
 * All registered listeners are called synchronously by the {@link SslManagerService} at the occurrence of the event.
 * It expected that implementers of this interface do NOT perform long running tasks in the implementation of this interface.
 */

public interface SslServiceListener {

	/**
	 * Notifies the {@link SslManagerService} has received a configuration update and it has applied the new configuration
	 */
    public void onConfigurationUpdated();
}

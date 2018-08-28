/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.cloudconnection.listener;

import org.osgi.annotation.versioning.ConsumerType;

/**
 * Implementors of this interface will be able to handle cloud stack related
 * events that deal with message delivery.
 * 
 * All the registered listeners are called synchronously at the occurrence of the event.
 * It is expected that implementors of this interface do NOT perform long running tasks in the implementation of this
 * interface.
 *
 * @since 2.0
 */
@ConsumerType
public interface CloudDeliveryListener {

    /**
     * Confirms message delivery to the cloud platform.
     *
     * @param messageId
     */
    public void onMessageConfirmed(String messageId);

}

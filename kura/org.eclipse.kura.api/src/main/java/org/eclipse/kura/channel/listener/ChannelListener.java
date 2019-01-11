
/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.channel.listener;

import org.osgi.annotation.versioning.ConsumerType;

/**
 * The listener interface ChannelListener is mainly for receiving channel events.
 * The class that is interested in processing a channel event implements this
 * interface.
 * 
 * @see ChannelEvent
 * @since 1.2
 */
@FunctionalInterface
@ConsumerType
public interface ChannelListener {

    /**
     * Triggers on channel event
     *
     * @param event
     *            the fired channel event
     * @throws NullPointerException
     *             if event is null
     */
    public void onChannelEvent(ChannelEvent event);

}

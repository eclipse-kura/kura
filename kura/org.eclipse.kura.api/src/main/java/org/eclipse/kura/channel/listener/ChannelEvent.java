/*******************************************************************************
 * Copyright (c) 2017, 2020 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 *  Amit Kumar Mondal
 ******************************************************************************/
package org.eclipse.kura.channel.listener;

import static java.util.Objects.requireNonNull;

import org.eclipse.kura.annotation.NotThreadSafe;
import org.eclipse.kura.channel.ChannelRecord;
import org.osgi.annotation.versioning.ProviderType;

/**
 * This class represents an event occurred while monitoring specific channel
 * configuration
 *
 * @noextend This class is not intended to be extended by clients.
 * @since 1.2
 */
@NotThreadSafe
@ProviderType
public class ChannelEvent {

    /**
     * Represents the channel record as triggered due to the asset specific
     * monitor operation
     */
    private final ChannelRecord channelRecord;

    /**
     * Instantiates a new channel event.
     *
     * @param channelRecord
     *            the channel record
     * @throws NullPointerException
     *             if the argument is null
     */
    public ChannelEvent(final ChannelRecord channelRecord) {
        requireNonNull(channelRecord, "Channel record cannot be null");
        this.channelRecord = channelRecord;
    }

    /**
     * Returns the associated channel record.
     *
     * @return the channel record
     */
    public ChannelRecord getChannelRecord() {
        return this.channelRecord;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "ChannelEvent [channeRecord=" + this.channelRecord + "]";
    }

}

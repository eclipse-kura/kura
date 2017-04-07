/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Eurotech
 *  Amit Kumar Mondal
 *
 *******************************************************************************/
package org.eclipse.kura.channel;

/**
 * This provides the necessary constants to denote the type of the channel
 * (whether the channel is for reading or writing or both)
 * @since 1.2
 */
public enum ChannelType {

    /**
     * The channel will be used for performing reading operation
     */
    READ,

    /**
     * The channel will be used for performing both reading and writing
     * operations
     */
    READ_WRITE,

    /**
     * The channel will be used for performing writing operation
     */
    WRITE;

    /**
     * Converts {@code channelTypeString}, if possible, to the related {@link ChannelType}.
     * 
     * @param channelTypeString
     *            String that we want to use to get the respective {@link ChannelType}.
     * @return a ChannelType that corresponds to the String passed as argument.
     * @throws IllegalArgumentException
     *             if the passed string does not correspond to an existing {@link ChannelType}.
     */
    public static ChannelType getChannelType(String channelTypeString) {
        if (READ.name().equalsIgnoreCase(channelTypeString)) {
            return READ;
        }
        if (WRITE.name().equalsIgnoreCase(channelTypeString)) {
            return WRITE;
        }
        if (READ_WRITE.name().equalsIgnoreCase(channelTypeString)) {
            return READ_WRITE;
        }

        throw new IllegalArgumentException("Cannot convert to ChannelType");
    }
}

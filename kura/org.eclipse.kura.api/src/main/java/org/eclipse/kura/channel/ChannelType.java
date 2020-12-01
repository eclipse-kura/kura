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
package org.eclipse.kura.channel;

/**
 * This provides the necessary constants to denote the type of the channel
 * (whether the channel is for reading or writing or both)
 *
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

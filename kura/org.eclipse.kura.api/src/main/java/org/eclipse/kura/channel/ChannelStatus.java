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

import static java.util.Objects.requireNonNull;

import org.eclipse.kura.annotation.Immutable;
import org.eclipse.kura.annotation.Nullable;
import org.eclipse.kura.annotation.ThreadSafe;
import org.osgi.annotation.versioning.ProviderType;

/**
 * The Class ChannelStatus is responsible for representing the status of any
 * channel specific operation
 *
 * @noextend This class is not intended to be extended by clients.
 * @since 1.2
 */
@Immutable
@ThreadSafe
@ProviderType
public class ChannelStatus {

    /** The channel flag. */
    private final ChannelFlag channelFlag;

    /** The exception instance if needed. */
    @Nullable
    private final Exception exception;

    /** The exception Message. */
    @Nullable
    private final String exceptionMessage;

    /**
     * Instantiates a new status.
     *
     * @param channelFlag
     *            the channel flag
     * @throws NullPointerException
     *             if the channel flag is null
     */
    public ChannelStatus(final ChannelFlag channelFlag) {
        requireNonNull(channelFlag, "Channel Flag cannot be null");
        this.channelFlag = channelFlag;
        this.exceptionMessage = null;
        this.exception = null;
    }

    /**
     * Instantiates a new status.
     *
     * @param channelFlag
     *            the channel flag
     * @param exceptionMessage
     *            the exception message
     * @param exception
     *            the exception
     * @throws NullPointerException
     *             if the channel flag is null
     */
    public ChannelStatus(final ChannelFlag channelFlag, @Nullable final String exceptionMessage,
            @Nullable final Exception exception) {
        requireNonNull(channelFlag, "Driver Flag cannot be null");
        this.channelFlag = channelFlag;
        this.exceptionMessage = exceptionMessage;
        this.exception = exception;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (this.getClass() != obj.getClass()) {
            return false;
        }
        final ChannelStatus other = (ChannelStatus) obj;
        if (this.channelFlag != other.channelFlag) {
            return false;
        }
        if (this.exception == null) {
            if (other.exception != null) {
                return false;
            }
        } else if (!this.exception.equals(other.exception)) {
            return false;
        }
        if (this.exceptionMessage == null) {
            if (other.exceptionMessage != null) {
                return false;
            }
        } else if (!this.exceptionMessage.equals(other.exceptionMessage)) {
            return false;
        }
        return true;
    }

    /**
     * Gets the channel flag.
     *
     * @return the channel flag
     */
    public ChannelFlag getChannelFlag() {
        return this.channelFlag;
    }

    /**
     * Gets the exception.
     *
     * @return the exception
     */
    public Exception getException() {
        return this.exception;
    }

    /**
     * Gets the exception message.
     *
     * @return the exception message
     */
    public String getExceptionMessage() {
        return this.exceptionMessage;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (this.channelFlag == null ? 0 : this.channelFlag.hashCode());
        result = prime * result + (this.exception == null ? 0 : this.exception.hashCode());
        result = prime * result + (this.exceptionMessage == null ? 0 : this.exceptionMessage.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return "ChannelStatus [channelFlag=" + this.channelFlag + ", exception=" + this.exception
                + ", exceptionMessage=" + this.exceptionMessage + "]";
    }
}

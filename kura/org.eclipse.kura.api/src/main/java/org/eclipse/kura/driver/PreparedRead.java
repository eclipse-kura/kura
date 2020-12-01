/**
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
 */
package org.eclipse.kura.driver;

import java.util.List;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.channel.ChannelRecord;
import org.eclipse.kura.driver.Driver.ConnectionException;
import org.osgi.annotation.versioning.ProviderType;

/**
 * This interface represents an optimized request that can be performed by a driver.
 * Implementations of this interface are returned by a driver as a result of
 * a call to the {@link Driver#prepareRead(java.util.List)} method.
 *
 * @see Driver#prepareRead(java.util.List)
 *
 * @noimplement This interface is not intended to be implemented by clients.
 * @since 1.2
 */
@ProviderType
public interface PreparedRead extends AutoCloseable {

    /**
     * Performs the optimized read request described by this {@link PreparedRead} instance.
     * In order to improve efficiency this method can return the same {@link ChannelRecord} instances that were supplied
     * as arguments to the {@link Driver#prepareRead(List)} call that created this {@link PreparedRead}.
     * The returned records should not be modified while a valid (not closed) {@link PreparedRead} holds a
     * reference to them, otherwise unpredictable behavior can occur.
     *
     * @return the result of the request as a list of {@link ChannelRecord} instances.
     * @throws KuraException
     *             if the provided {@link PreparedRead} is not valid (for example if it has been closed)
     * @throws ConnectionException
     *             if the connection to the field device is interrupted
     */
    public List<ChannelRecord> execute() throws ConnectionException, KuraException;

    /**
     * Returns the list of channel records associated with this prepared read.
     * In order to improve efficiency this method can return the same {@link ChannelRecord} instances that were supplied
     * as arguments to the {@link Driver#prepareRead(List)} call that created this {@link PreparedRead}.
     * The returned records should not be modified while a valid (not closed) {@link PreparedRead} holds a
     * reference to them, otherwise unpredictable behavior can occur.
     *
     * @return The list of channel records associated with this prepared read.
     */
    public List<ChannelRecord> getChannelRecords();
}

/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 ******************************************************************************/
package org.eclipse.kura.cloud;

import org.eclipse.kura.KuraConnectException;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.KuraStoreException;
import org.eclipse.kura.KuraTimeoutException;
import org.eclipse.kura.message.KuraPayload;
import org.eclipse.kura.message.KuraResponsePayload;
import org.osgi.annotation.versioning.ProviderType;

/**
 * The CloudCallService provides helper methods to make a request/response conversation with the remote server.
 * The call methods deal with the logic required to build request messages and track the corresponding responses.
 * All call methods are synchronous; after a request is issued, the implementation will wait for the response
 * to arrive or a timeout occurs. The timeout interval used by the service is configurable as a property
 * of the {@link org.eclipse.kura.data.DataTransportService}.
 *
 * @noimplement This interface is not intended to be implemented by clients.
 * @deprecated
 */
@ProviderType
@Deprecated
public interface CloudCallService {

    /**
     * Sends a local (to this device) request to a Cloudlet application
     * with the given application ID waiting for the response.
     *
     * @param appId
     * @param appTopic
     * @param appPayload
     *            the application specific payload of an KuraRequestPayload.
     * @param timeout
     * @return
     * @throws KuraConnectException
     * @throws KuraTimeoutException
     * @throws KuraStoreException
     * @throws KuraException
     */
    public KuraResponsePayload call(String appId, String appTopic, KuraPayload appPayload, int timeout)
            throws KuraConnectException, KuraTimeoutException, KuraStoreException, KuraException;

    /**
     * Sends a request to a remote server or device identified by the specified deviceId
     * and targeting the given application ID waiting for the response.
     *
     * @param deviceId
     * @param appId
     * @param appTopic
     * @param appPayload
     * @param timeout
     * @return
     * @throws KuraConnectException
     * @throws KuraTimeoutException
     * @throws KuraStoreException
     * @throws KuraException
     */
    public KuraResponsePayload call(String deviceId, String appId, String appTopic, KuraPayload appPayload, int timeout)
            throws KuraConnectException, KuraTimeoutException, KuraStoreException, KuraException;

    /**
     * Returns true if the underlying {@link org.eclipse.kura.data.DataService} is currently connected to the remote server.
     *
     * @return
     */
    public boolean isConnected();
}

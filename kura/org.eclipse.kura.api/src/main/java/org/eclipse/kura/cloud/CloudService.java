/*******************************************************************************
 * Copyright (c) 2011, 2018 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.cloud;

import org.eclipse.kura.KuraException;
import org.osgi.annotation.versioning.ProviderType;

/**
 * The CloudService provides an easy to use API layer for M2M application to communicate with a remote server.
 * It operates as a decorator for the {@link org.eclipse.kura.data.DataService} providing add-on
 * features over the management of the transport layer.
 * In addition to simple publish/subscribe, the Cloud Service API simplifies the implementation of more complex
 * interaction flows like request/response or remote resource management. Cloud Service abstracts the
 * developers from the complexity of the transport protocol and payload format used in the communication.<br>
 * CloudService allows for a single connection to a remote server to be shared across more than one application
 * in the gateway providing the necessary topic partitioning.<br>
 * Its responsibilities can be summarized as:
 * <ul>
 * <li>Adds application topic prefixes to allow for a single remote server connection to be shared across applications
 * <li>Define a payload data model and provide default encoding/decoding serializers
 * <li>Publish life-cycle messages when device and applications start and stop
 * </ul>
 * The CloudService can be used through the {@link CloudClient} API or by extending the {@link Cloudlet} class.
 * {@link Cloudlet} simplifies the interactions with remote servers providing a servlet-like API
 * to implement request and response flows and remote resource management.
 *
 * @noimplement This interface is not intended to be implemented by clients.
 *
 * @deprecated Please consider using {@link CloudConnectionManager}
 */
@ProviderType
@Deprecated
public interface CloudService {

    /**
     * Returns a new instance of the CloudClient for the given application Id.
     * The CloudClient is designed to be used by single application bundles.
     * CloudClient instances are acquired from the CloudService and they are released
     * when the work is completed. Generally, a CloudClient is acquired during the
     * activation phase of a bundle and it is released through the
     * {@link CloudClient#release} method during the bundle deactivation phase.
     * <br>
     * CloudClient will clean-up the subscriptions and the callback registrations
     * when the {@link CloudClient#release} method is called.
     * <br>
     * If the bundle using the CloudClient relies on subscriptions,
     * it is responsibility of the application to implement the
     * {@link CloudClientListener#onConnectionEstablished()} callback method
     * in the CloudCallbackHandler to restore the subscriptions it needs.
     *
     * @param appId
     *            A String object specifying a unique application ID.
     * @return CloudClient instance
     * @throws KuraException
     */
    public CloudClient newCloudClient(String appId) throws KuraException;

    /**
     * Returns the application identifiers for which a CloudClient instance was created.
     *
     * @return An array of application identifiers
     */
    public String[] getCloudApplicationIdentifiers();

    /**
     * Returns true if the underlying {@link org.eclipse.kura.data.DataService} is currently connected to the remote
     * server.
     *
     * @return
     */
    public boolean isConnected();
}

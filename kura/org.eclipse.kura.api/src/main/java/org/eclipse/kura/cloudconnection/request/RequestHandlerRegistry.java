/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.cloudconnection.request;

import org.eclipse.kura.KuraException;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Interface used to register or unregister {@link RequestHandler}s identified by a specific id
 *
 * @noextend This class is not intended to be subclassed by clients.
 * @since 2.0
 */
@ProviderType
public interface RequestHandlerRegistry {

    /**
     * Registers a {@link RequestHandler} identified by the specified {@code id}. Once registered, the
     * {@link RequestHandler} instance can be notified for request messages targeting the registered {@code id}
     *
     * @param id
     *            a String identifying a specific {@link RequestHandler}
     * @param requestHandler
     *            a {@link RequestHandler} instance identified by the specified {@code id}
     * @throws KuraException
     */
    public void registerRequestHandler(String id, RequestHandler requestHandler) throws KuraException;

    /**
     * Unregisters the {@link RequestHandler} identified by the specified {@code id}. From that moment on, no
     * notifications will be sent to the unregistered {@RequestHandler}
     *
     * @param id
     *            a String identifying a specific {@link RequestHandler}
     * @throws KuraException
     */
    public void unregister(String id) throws KuraException;

}

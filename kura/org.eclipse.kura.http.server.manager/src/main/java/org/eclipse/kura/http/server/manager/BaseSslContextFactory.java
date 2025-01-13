/*******************************************************************************
 * Copyright (c) 2025 Eurotech and/or its affiliates and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Eurotech
 *******************************************************************************/

package org.eclipse.kura.http.server.manager;

import java.security.KeyStore;

import javax.net.ssl.KeyManager;

import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.kura.security.keystore.KeystoreService;

public class BaseSslContextFactory extends SslContextFactory.Server {

    protected final KeystoreService keystoreService;

    public BaseSslContextFactory(final KeystoreService keystoreService) {
        this.keystoreService = keystoreService;
    }

    @Override
    protected KeyManager[] getKeyManagers(KeyStore keyStore) throws Exception {
        return this.keystoreService.getKeyManagers(getKeyManagerFactoryAlgorithm()).toArray(new KeyManager[0]);
    }

}

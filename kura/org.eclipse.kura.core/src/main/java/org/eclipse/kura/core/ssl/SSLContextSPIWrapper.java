/*******************************************************************************
 * Copyright (c) 2019 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.core.ssl;

import java.security.KeyManagementException;
import java.security.SecureRandom;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLContextSpi;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSessionContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

public class SSLContextSPIWrapper extends SSLContextSpi {

    private final SSLContext wrapped;
    private final SSLSocketFactory factory;

    public SSLContextSPIWrapper(final SSLContext wrapped, final SSLSocketFactory factory) {
        this.wrapped = wrapped;
        this.factory = factory;
    }

    @Override
    protected SSLEngine engineCreateSSLEngine() {
        return wrapped.createSSLEngine();
    }

    @Override
    protected SSLEngine engineCreateSSLEngine(final String arg0, final int arg1) {
        return wrapped.createSSLEngine(arg0, arg1);
    }

    @Override
    protected SSLSessionContext engineGetClientSessionContext() {
        return wrapped.getClientSessionContext();
    }

    @Override
    protected SSLSessionContext engineGetServerSessionContext() {
        return wrapped.getServerSessionContext();
    }

    @Override
    protected SSLServerSocketFactory engineGetServerSocketFactory() {
        return wrapped.getServerSocketFactory();
    }

    @Override
    protected SSLSocketFactory engineGetSocketFactory() {
        return factory;
    }

    @Override
    protected void engineInit(KeyManager[] arg0, TrustManager[] arg1, SecureRandom arg2) throws KeyManagementException {
        wrapped.init(arg0, arg1, arg2);
    }

}

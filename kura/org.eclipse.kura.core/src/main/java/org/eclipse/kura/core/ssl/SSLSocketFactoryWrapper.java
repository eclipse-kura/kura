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
package org.eclipse.kura.core.ssl;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;

import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wrapper over the SSLSocketFactory which enforces HTTPS style hostname validation on the returned sockets.
 * http://stackoverflow.com/questions/18139448/how-should-i-do-hostname-validation-when-using-jsse
 */
public class SSLSocketFactoryWrapper extends SSLSocketFactory {

    private static final Logger logger = LoggerFactory.getLogger(SSLSocketFactoryWrapper.class);

    private final String ciphers;
    private final Boolean hostnameVerification;
    private final SSLSocketFactory sslsf;

    public SSLSocketFactoryWrapper(SSLSocketFactory sslsf, String ciphers, Boolean hnVerify) {
        this.sslsf = sslsf;
        this.ciphers = ciphers;
        this.hostnameVerification = hnVerify;
    }

    @Override
    public String[] getDefaultCipherSuites() {
        return this.sslsf.getDefaultCipherSuites();
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return this.sslsf.getSupportedCipherSuites();
    }

    @Override
    public Socket createSocket() throws IOException {
        Socket socket = this.sslsf.createSocket();
        updateSSLParameters(socket);
        return socket;
    }

    @Override
    public Socket createSocket(InetAddress host, int port) throws IOException {
        Socket socket = this.sslsf.createSocket(host, port);
        updateSSLParameters(socket);
        return socket;
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localHost, int localPort)
            throws IOException {
        Socket socket = this.sslsf.createSocket(host, port, localHost, localPort);
        updateSSLParameters(socket);
        return socket;
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException {
        Socket socket = this.sslsf.createSocket(host, port);
        updateSSLParameters(socket);
        return socket;
    }

    @Override
    public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort)
            throws IOException {
        Socket socket = this.sslsf.createSocket(address, port, address, localPort);
        updateSSLParameters(socket);
        return socket;
    }

    @Override
    public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
        Socket socket = this.sslsf.createSocket(s, host, port, autoClose);
        updateSSLParameters(socket);
        return socket;
    }

    private void updateSSLParameters(Socket socket) throws SocketException {
        if (socket instanceof SSLSocket) {
            SSLSocket sslSocket = (SSLSocket) socket;

            SSLParameters sslParams = sslSocket.getSSLParameters();

            // Do not send an SSL-2.0-compatible Client Hello.
            ArrayList<String> protocols = new ArrayList<>(Arrays.asList(sslParams.getProtocols()));
            protocols.remove("SSLv2Hello");
            sslParams.setProtocols(protocols.toArray(new String[protocols.size()]));

            // enable server verification
            if (this.hostnameVerification) {
                sslParams.setEndpointIdentificationAlgorithm("HTTPS");
                logger.info("SSL Endpoint Identification enabled.");
            }

            // Adjust the supported ciphers.
            if (this.ciphers != null && !this.ciphers.isEmpty()) {
                String[] arrCiphers = this.ciphers.split(",");
                ArrayList<String> lsCiphers = new ArrayList<>();
                for (String cipher : arrCiphers) {
                    lsCiphers.add(cipher.trim());
                }
                sslParams.setCipherSuites(lsCiphers.toArray(new String[lsCiphers.size()]));
            }

            // update the socket parameters
            sslSocket.setSSLParameters(sslParams);

            // Disable the Nagle algorithm.
            socket.setTcpNoDelay(true);
        }
    }
}

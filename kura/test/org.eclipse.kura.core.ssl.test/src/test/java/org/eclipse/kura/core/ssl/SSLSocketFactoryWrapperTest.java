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
 *******************************************************************************/
package org.eclipse.kura.core.ssl;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import org.eclipse.kura.core.testutil.TestUtil;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class SSLSocketFactoryWrapperTest {

    @Test
    public void testSSLSocketFactoryWrapper() throws NoSuchFieldException {
        SSLSocketFactory factory = mock(SSLSocketFactory.class);
        SSLSocketFactoryWrapper wrapper = new SSLSocketFactoryWrapper(factory, "ciphers", false);

        assertEquals(factory, TestUtil.getFieldValue(wrapper, "sslsf"));
        assertEquals("ciphers", TestUtil.getFieldValue(wrapper, "ciphers"));
        assertEquals(false, TestUtil.getFieldValue(wrapper, "hostnameVerification"));
    }

    @Test
    public void testGetDefaultCipherSuites() {
        SSLSocketFactory factory = mock(SSLSocketFactory.class);
        SSLSocketFactoryWrapper wrapper = new SSLSocketFactoryWrapper(factory, "ciphers", false);

        String[] defaultCipherSuites = { "cipher1", "cipher2" };
        when(factory.getDefaultCipherSuites()).thenReturn(defaultCipherSuites);

        assertArrayEquals(defaultCipherSuites, wrapper.getDefaultCipherSuites());
    }

    @Test
    public void testGetSupportedCipherSuites() {
        SSLSocketFactory factory = mock(SSLSocketFactory.class);
        SSLSocketFactoryWrapper wrapper = new SSLSocketFactoryWrapper(factory, "ciphers", false);

        String[] sSupportedCipherSuites = { "cipher1", "cipher2" };
        when(factory.getSupportedCipherSuites()).thenReturn(sSupportedCipherSuites);

        assertArrayEquals(sSupportedCipherSuites, wrapper.getSupportedCipherSuites());
    }

    @Test
    public void testCreateSocket() throws IOException {
        SSLSocketFactory factory = mock(SSLSocketFactory.class);
        SSLSocketFactoryWrapper wrapper = new SSLSocketFactoryWrapper(factory,
                "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256, TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256", true);

        SSLSocket socket = (SSLSocket) SSLSocketFactory.getDefault().createSocket();
        socket.setEnabledProtocols(new String[] { "TLSv1.1", "SSLv2Hello", "TLSv1.2" });
        when(factory.createSocket()).thenReturn(socket);

        Socket resultSocket = wrapper.createSocket();
        assertTrue(resultSocket instanceof SSLSocket);

        SSLParameters resultParameters = ((SSLSocket) resultSocket).getSSLParameters();
        String[] expectedProtocols = { "TLSv1.1", "TLSv1.2" };
        String[] expectedCiphers = { "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256",
                "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256" };
        assertArrayEquals(expectedProtocols, resultParameters.getProtocols());
        assertEquals("HTTPS", resultParameters.getEndpointIdentificationAlgorithm());
        assertArrayEquals(expectedCiphers, resultParameters.getCipherSuites());

        assertTrue(resultSocket.getTcpNoDelay());
    }

    @Test
    public void testCreateSocketByHostAndPort() throws IOException, NoSuchAlgorithmException {
        SSLContext sslCtx = SSLContext.getDefault();
        SSLSocketFactory factory = sslCtx.getSocketFactory();
        SSLSocketFactoryWrapper wrapper = new SSLSocketFactoryWrapper(factory,
                "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256, TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256", true);

        InetAddress address = InetAddress.getByName("google.com");
        Socket resultSocket = wrapper.createSocket(address, 443);
        assertTrue(resultSocket instanceof SSLSocket);

        SSLParameters resultParameters = ((SSLSocket) resultSocket).getSSLParameters();

        String[] expectedCiphers = { "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256",
                "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256" };
        List<String> expectedProtocols = Arrays.asList(new String[] { "TLSv1.3", "TLSv1.2", "TLSv1.1", "TLSv1" });
        List<String> resultProtocols = Arrays.asList(resultParameters.getProtocols());
        assertTrue(expectedProtocols.size() >= resultProtocols.size() && expectedProtocols.containsAll(resultProtocols));
        assertEquals("HTTPS", resultParameters.getEndpointIdentificationAlgorithm());
        assertArrayEquals(expectedCiphers, resultParameters.getCipherSuites());

        assertTrue(resultSocket.getTcpNoDelay());

        resultSocket.close();
    }

    @Test
    public void testCreateSocketNoHostnameVerification() throws IOException, NoSuchAlgorithmException {
        SSLContext sslCtx = SSLContext.getDefault();
        SSLSocketFactory factory = sslCtx.getSocketFactory();
        SSLSocketFactoryWrapper wrapper = new SSLSocketFactoryWrapper(factory,
                "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256, TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256", false);

        Socket resultSocket = wrapper.createSocket();
        assertTrue(resultSocket instanceof SSLSocket);

        SSLParameters resultParameters = ((SSLSocket) resultSocket).getSSLParameters();

        String[] expectedCiphers = { "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256",
                "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256" };

        List<String> expectedProtocols = Arrays.asList(new String[] { "TLSv1.3", "TLSv1.2", "TLSv1.1", "TLSv1" });
        List<String> resultProtocols = Arrays.asList(resultParameters.getProtocols());
        assertTrue(expectedProtocols.size() >= resultProtocols.size() && expectedProtocols.containsAll(resultProtocols));
        assertNotEquals("HTTPS", resultParameters.getEndpointIdentificationAlgorithm());
        assertArrayEquals(expectedCiphers, resultParameters.getCipherSuites());

        assertTrue(resultSocket.getTcpNoDelay());
    }

    @Test
    public void testCreateSocketNullCyphers() throws IOException, NoSuchAlgorithmException {
        SSLContext sslCtx = SSLContext.getDefault();
        SSLSocketFactory factory = sslCtx.getSocketFactory();
        SSLSocketFactoryWrapper wrapper = new SSLSocketFactoryWrapper(factory, null, true);

        Socket resultSocket = wrapper.createSocket();
        assertTrue(resultSocket instanceof SSLSocket);

        SSLParameters resultParameters = ((SSLSocket) resultSocket).getSSLParameters();
        List<String> expectedProtocols = Arrays.asList(new String[] { "TLSv1.3", "TLSv1.2", "TLSv1.1", "TLSv1" });
        List<String> resultProtocols = Arrays.asList(resultParameters.getProtocols());
        assertTrue(expectedProtocols.size() >= resultProtocols.size() && expectedProtocols.containsAll(resultProtocols));
        assertEquals("HTTPS", resultParameters.getEndpointIdentificationAlgorithm());
        assertNotEquals(0, resultParameters.getCipherSuites().length);

        assertTrue(resultSocket.getTcpNoDelay());
    }

    @Test
    public void testCreateSocketEmptyCyphers() throws IOException, NoSuchAlgorithmException {
        SSLContext sslCtx = SSLContext.getDefault();
        SSLSocketFactory factory = sslCtx.getSocketFactory();
        SSLSocketFactoryWrapper wrapper = new SSLSocketFactoryWrapper(factory, "", true);

        Socket resultSocket = wrapper.createSocket();
        assertTrue(resultSocket instanceof SSLSocket);

        SSLParameters resultParameters = ((SSLSocket) resultSocket).getSSLParameters();
        List<String> expectedProtocols = Arrays.asList(new String[] { "TLSv1.3", "TLSv1.2", "TLSv1.1", "TLSv1" });
        List<String> resultProtocols = Arrays.asList(resultParameters.getProtocols());
        assertTrue(expectedProtocols.size() >= resultProtocols.size() && expectedProtocols.containsAll(resultProtocols));
        assertEquals("HTTPS", resultParameters.getEndpointIdentificationAlgorithm());
        assertNotEquals(0, resultParameters.getCipherSuites().length);

        assertTrue(resultSocket.getTcpNoDelay());
    }
}

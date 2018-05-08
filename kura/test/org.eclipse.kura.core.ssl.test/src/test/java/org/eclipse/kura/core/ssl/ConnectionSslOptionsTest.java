/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.kura.core.ssl;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ConnectionSslOptionsTest {

    @Test
    public void testConnectionSslOptions() {
        SslManagerServiceOptions serviceOptions = mock(SslManagerServiceOptions.class);
        ConnectionSslOptions sslOptions = new ConnectionSslOptions(serviceOptions);

        assertEquals(serviceOptions, sslOptions.getSslManagerOpts());
    }

    @Test
    public void testSetProtocolNull() {
        SslManagerServiceOptions serviceOptions = mock(SslManagerServiceOptions.class);
        ConnectionSslOptions sslOptions = new ConnectionSslOptions(serviceOptions);

        when(serviceOptions.getSslProtocol()).thenReturn("p1");
        sslOptions.setProtocol(null);
        assertEquals("p1", sslOptions.getProtocol());
    }

    @Test
    public void testSetProtocolEmpty() {
        SslManagerServiceOptions serviceOptions = mock(SslManagerServiceOptions.class);
        ConnectionSslOptions sslOptions = new ConnectionSslOptions(serviceOptions);

        when(serviceOptions.getSslProtocol()).thenReturn("p2");
        sslOptions.setProtocol("");
        assertEquals("p2", sslOptions.getProtocol());
    }

    @Test
    public void testSetProtocolWhitespace() {
        SslManagerServiceOptions serviceOptions = mock(SslManagerServiceOptions.class);
        ConnectionSslOptions sslOptions = new ConnectionSslOptions(serviceOptions);

        when(serviceOptions.getSslProtocol()).thenReturn("p3");
        sslOptions.setProtocol(" \t\r\b\f");
        assertEquals("p3", sslOptions.getProtocol());
    }

    @Test
    public void testSetProtocolNonEmpty() {
        SslManagerServiceOptions serviceOptions = mock(SslManagerServiceOptions.class);
        ConnectionSslOptions sslOptions = new ConnectionSslOptions(serviceOptions);

        sslOptions.setProtocol("p4");
        assertEquals("p4", sslOptions.getProtocol());
    }

    @Test
    public void testSetCiphersNull() {
        SslManagerServiceOptions serviceOptions = mock(SslManagerServiceOptions.class);
        ConnectionSslOptions sslOptions = new ConnectionSslOptions(serviceOptions);

        when(serviceOptions.getSslCiphers()).thenReturn("c1");
        sslOptions.setCiphers(null);
        assertEquals("c1", sslOptions.getCiphers());
    }

    @Test
    public void testSetCiphersEmpty() {
        SslManagerServiceOptions serviceOptions = mock(SslManagerServiceOptions.class);
        ConnectionSslOptions sslOptions = new ConnectionSslOptions(serviceOptions);

        when(serviceOptions.getSslCiphers()).thenReturn("c2");
        sslOptions.setCiphers("");
        assertEquals("c2", sslOptions.getCiphers());
    }

    @Test
    public void testSetCiphersWhitespace() {
        SslManagerServiceOptions serviceOptions = mock(SslManagerServiceOptions.class);
        ConnectionSslOptions sslOptions = new ConnectionSslOptions(serviceOptions);

        when(serviceOptions.getSslCiphers()).thenReturn("c3");
        sslOptions.setCiphers(" \t\r\b\f");
        assertEquals("c3", sslOptions.getCiphers());
    }

    @Test
    public void testSetCiphersNonEmpty() {
        SslManagerServiceOptions serviceOptions = mock(SslManagerServiceOptions.class);
        ConnectionSslOptions sslOptions = new ConnectionSslOptions(serviceOptions);

        sslOptions.setCiphers("c4");

        assertEquals("c4", sslOptions.getCiphers());
    }

    @Test
    public void testSetTrustStoreNull() {
        SslManagerServiceOptions serviceOptions = mock(SslManagerServiceOptions.class);
        ConnectionSslOptions sslOptions = new ConnectionSslOptions(serviceOptions);

        when(serviceOptions.getSslKeyStore()).thenReturn("ts1");
        sslOptions.setTrustStore(null);
        assertEquals("ts1", sslOptions.getTrustStore());
    }

    @Test
    public void testSetTrustStoreEmpty() {
        SslManagerServiceOptions serviceOptions = mock(SslManagerServiceOptions.class);
        ConnectionSslOptions sslOptions = new ConnectionSslOptions(serviceOptions);

        when(serviceOptions.getSslKeyStore()).thenReturn("ts2");
        sslOptions.setTrustStore("");
        assertEquals("ts2", sslOptions.getTrustStore());
    }

    @Test
    public void testSetTrustStoreWhitespace() {
        SslManagerServiceOptions serviceOptions = mock(SslManagerServiceOptions.class);
        ConnectionSslOptions sslOptions = new ConnectionSslOptions(serviceOptions);

        when(serviceOptions.getSslKeyStore()).thenReturn("ts3");
        sslOptions.setTrustStore(" \t\r\b\f");

        assertEquals("ts3", sslOptions.getTrustStore());
    }

    @Test
    public void testSetTrustStoreNonEmpty() {
        SslManagerServiceOptions serviceOptions = mock(SslManagerServiceOptions.class);
        ConnectionSslOptions sslOptions = new ConnectionSslOptions(serviceOptions);

        sslOptions.setTrustStore("ts4");
        assertEquals("ts4", sslOptions.getTrustStore());
    }

    @Test
    public void testSetKeyStoreNull() {
        SslManagerServiceOptions serviceOptions = mock(SslManagerServiceOptions.class);
        ConnectionSslOptions sslOptions = new ConnectionSslOptions(serviceOptions);

        when(serviceOptions.getSslKeyStore()).thenReturn("ks1");
        sslOptions.setKeyStore(null);
        assertEquals("ks1", sslOptions.getKeyStore());
    }

    @Test
    public void testSetKeyStoreEmpty() {
        SslManagerServiceOptions serviceOptions = mock(SslManagerServiceOptions.class);
        ConnectionSslOptions sslOptions = new ConnectionSslOptions(serviceOptions);

        when(serviceOptions.getSslKeyStore()).thenReturn("ks2");
        sslOptions.setKeyStore("");
        assertEquals("ks2", sslOptions.getKeyStore());
    }

    @Test
    public void testSetKeyStoreWhitespace() {
        SslManagerServiceOptions serviceOptions = mock(SslManagerServiceOptions.class);
        ConnectionSslOptions sslOptions = new ConnectionSslOptions(serviceOptions);

        when(serviceOptions.getSslKeyStore()).thenReturn("ks3");
        sslOptions.setKeyStore(" \t\r\b\f");
        assertEquals("ks3", sslOptions.getKeyStore());
    }

    @Test
    public void testSetKeyStoreNonEmpty() {
        SslManagerServiceOptions serviceOptions = mock(SslManagerServiceOptions.class);
        ConnectionSslOptions sslOptions = new ConnectionSslOptions(serviceOptions);

        sslOptions.setKeyStore("ks4");
        assertEquals("ks4", sslOptions.getKeyStore());
    }

    @Test
    public void testSetKeyStorePassword() {
        SslManagerServiceOptions serviceOptions = mock(SslManagerServiceOptions.class);
        ConnectionSslOptions sslOptions = new ConnectionSslOptions(serviceOptions);

        char[] password = "keyStorePassword".toCharArray();
        sslOptions.setKeyStorePassword(password);
        assertArrayEquals(password, sslOptions.getKeyStorePassword());
    }

    @Test
    public void testSetAlias() {
        SslManagerServiceOptions serviceOptions = mock(SslManagerServiceOptions.class);
        ConnectionSslOptions sslOptions = new ConnectionSslOptions(serviceOptions);

        sslOptions.setAlias("alias");
        assertEquals("alias", sslOptions.getAlias());
    }

    @Test
    public void testSetHostnameVerification() {
        SslManagerServiceOptions serviceOptions = mock(SslManagerServiceOptions.class);
        ConnectionSslOptions sslOptions = new ConnectionSslOptions(serviceOptions);

        sslOptions.setHostnameVerification(true);
        assertEquals(true, sslOptions.getHostnameVerification());

        sslOptions.setHostnameVerification(false);
        assertEquals(false, sslOptions.getHostnameVerification());
    }
}

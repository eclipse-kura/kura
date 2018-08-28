/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.kura.core.certificates;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.testutil.TestUtil;
import org.eclipse.kura.crypto.CryptoService;
import org.junit.Test;

public class CertificatesManagerTest {
	@Test
	public void testReturnCertificate() throws NoSuchFieldException, KuraException {
		// Create an extended manager instance with test support
		String keyStorePassword = "password";
		String certificateAlias = "alias";
		String invalidCertificateAlias = "invalid";
		Certificate mockCertificate = mock(Certificate.class);
		CertificatesManager manager = new ExtendedCertificatesManager(keyStorePassword, certificateAlias,
				mockCertificate, null);

		String keyStorePath = getDefaultKeyStore(manager);
		CryptoService mockCryptoService = createMockCryptoService(keyStorePath, keyStorePassword);

		manager.setCryptoService(mockCryptoService);

		// Try to get a certificate with a valid alias
		Certificate certificate = manager.returnCertificate(certificateAlias);
		assertEquals(mockCertificate, certificate);

		// Try to get a certificate with an invalid alias
		certificate = manager.returnCertificate(invalidCertificateAlias);
		assertNull(certificate);
	}

	@Test(expected = KuraException.class)
	public void testStoreCertificate() throws KuraException {
		// Not implemented, always throws an exception
		CertificatesManager manager = new CertificatesManager();

		Certificate mockCertificate = mock(Certificate.class);
		manager.storeCertificate(mockCertificate, "alias");
	}

	@Test
	public void testListBundleCertificatesAliases() throws NoSuchFieldException {
		// Create an extended manager instance with test support
		String keyStorePassword = "password";
		ArrayList<String> aliases = new ArrayList<>();
		aliases.add("alias1");
		aliases.add("alias2");
		CertificatesManager manager = new ExtendedCertificatesManager(keyStorePassword, null, null,
				Collections.enumeration(aliases));

		String keyStorePath = getDefaultKeyStore(manager);
		CryptoService mockCryptoService = createMockCryptoService(keyStorePath, keyStorePassword);

		manager.setCryptoService(mockCryptoService);

		// Read aliases
		Enumeration<String> readAliases = manager.listBundleCertificatesAliases();
		ArrayList<String> readAliasesList = Collections.list(readAliases);

		assertEquals(aliases.size(), readAliasesList.size());
		assertTrue(readAliasesList.containsAll(aliases));
	}

	@Test
	public void testListDMCertificatesAliases() throws NoSuchFieldException {
		// Create an extended manager instance with test support
		String keyStorePassword = "password";
		ArrayList<String> aliases = new ArrayList<>();
		aliases.add("alias1");
		aliases.add("alias2");
		CertificatesManager manager = new ExtendedCertificatesManager(keyStorePassword, null, null,
				Collections.enumeration(aliases));

		String keyStorePath = getDefaultKeyStore(manager);
		CryptoService mockCryptoService = createMockCryptoService(keyStorePath, keyStorePassword);

		manager.setCryptoService(mockCryptoService);

		// Read aliases
		Enumeration<String> readAliases = manager.listDMCertificatesAliases();
		ArrayList<String> readAliasesList = Collections.list(readAliases);

		assertEquals(aliases.size(), readAliasesList.size());
		assertTrue(readAliasesList.containsAll(aliases));
	}

	@Test
	public void testListSSLCertificatesAliases() throws NoSuchFieldException {
		// Create an extended manager instance with test support
		String keyStorePassword = "password";
		ArrayList<String> aliases = new ArrayList<>();
		aliases.add("alias1");
		aliases.add("alias2");
		CertificatesManager manager = new ExtendedCertificatesManager(keyStorePassword, null, null,
				Collections.enumeration(aliases));

		String keyStorePath = getDefaultKeyStore(manager);
		CryptoService mockCryptoService = createMockCryptoService(keyStorePath, keyStorePassword);

		manager.setCryptoService(mockCryptoService);

		// Read aliases
		Enumeration<String> readAliases = manager.listSSLCertificatesAliases();
		ArrayList<String> readAliasesList = Collections.list(readAliases);

		assertEquals(aliases.size(), readAliasesList.size());
		assertTrue(readAliasesList.containsAll(aliases));
	}

	@Test
	public void testListCACertificatesAliases() throws NoSuchFieldException {
		// Create an extended manager instance with test support
		String keyStorePassword = "password";
		ArrayList<String> aliases = new ArrayList<>();
		aliases.add("alias1");
		aliases.add("alias2");
		CertificatesManager manager = new ExtendedCertificatesManager(keyStorePassword, null, null,
				Collections.enumeration(aliases));

		String keyStorePath = getDefaultKeyStore(manager);
		CryptoService mockCryptoService = createMockCryptoService(keyStorePath, keyStorePassword);

		manager.setCryptoService(mockCryptoService);

		// Read aliases
		Enumeration<String> readAliases = manager.listCACertificatesAliases();
		ArrayList<String> readAliasesList = Collections.list(readAliases);

		assertEquals(aliases.size(), readAliasesList.size());
		assertTrue(readAliasesList.containsAll(aliases));
	}

	@Test(expected = KuraException.class)
	public void testRemoveCertificate() throws KuraException {
		// Not implemented, always throws an exception
		CertificatesManager manager = new CertificatesManager();
		manager.removeCertificate("alias");
	}

	@Test
	public void testVerifySignature() {
		// Not implemented, always returns true
		CertificatesManager manager = new CertificatesManager();
		assertTrue(manager.verifySignature(null, null));
	}

	String getDefaultKeyStore(CertificatesManager manager) throws NoSuchFieldException {
		return (String) TestUtil.getFieldValue(manager, "DEFAULT_KEYSTORE");
	}

	CryptoService createMockCryptoService(String keyStorePath, String keyStorePassword) throws NoSuchFieldException {
		CryptoService mockService = mock(CryptoService.class);
		when(mockService.getKeyStorePassword(keyStorePath)).thenReturn(keyStorePassword.toCharArray());

		return mockService;
	}
}

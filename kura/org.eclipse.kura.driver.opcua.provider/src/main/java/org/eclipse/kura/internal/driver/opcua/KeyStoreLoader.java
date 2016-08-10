/**
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Eurotech
 *   Amit Kumar Mondal (admin@amitinside.com)
 */
package org.eclipse.kura.internal.driver.opcua;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;

/**
 * The Class KeyStoreLoader loads the provided keystore
 */
public final class KeyStoreLoader {

	/** Keystore Certificate. */
	private final String m_certificate;

	/** Keystore Client Alias. */
	private final String m_clientAlias;

	/** Keystore Client Certificate. */
	private X509Certificate m_clientCertificate;

	/** Keystore Key Pair. */
	private KeyPair m_clientKeyPair;

	/** Keystore Type. */
	private final String m_keystoreType;

	/** Keystore Password. */
	private final char[] m_password;

	/** Keystore Server Alias. */
	@SuppressWarnings("unused")
	private final String m_serverAlias;

	/**
	 * Instantiates a new key store loader.
	 *
	 * @param keystoreType
	 *            the keystore type
	 * @param clientAlias
	 *            the client alias
	 * @param serverAlias
	 *            the server alias
	 * @param password
	 *            the password
	 * @param certificate
	 *            the certificate
	 */
	public KeyStoreLoader(final String keystoreType, final String clientAlias, final String serverAlias,
			final String password, final String certificate) {
		this.m_keystoreType = keystoreType;
		this.m_clientAlias = clientAlias;
		this.m_serverAlias = serverAlias;
		this.m_password = password.toCharArray();
		this.m_certificate = certificate;
	}

	/**
	 * Gets the client certificate.
	 *
	 * @return the client certificate
	 */
	public X509Certificate getClientCertificate() {
		return this.m_clientCertificate;
	}

	/**
	 * Gets the client key pair.
	 *
	 * @return the client key pair
	 */
	public KeyPair getClientKeyPair() {
		return this.m_clientKeyPair;
	}

	/**
	 * Loads the certificate.
	 *
	 * @return the keystore instance
	 * @throws Exception
	 *             if the load is unsuccessful
	 */
	public KeyStoreLoader load() throws Exception {
		final KeyStore keyStore = KeyStore.getInstance(this.m_keystoreType);
		keyStore.load(Files.newInputStream(Paths.get(this.m_certificate)), this.m_password);
		final Key clientPrivateKey = keyStore.getKey(this.m_clientAlias, this.m_password);

		if (clientPrivateKey instanceof PrivateKey) {
			this.m_clientCertificate = (X509Certificate) keyStore.getCertificate(this.m_clientAlias);
			final PublicKey clientPublicKey = this.m_clientCertificate.getPublicKey();
			this.m_clientKeyPair = new KeyPair(clientPublicKey, (PrivateKey) clientPrivateKey);
		}

		return this;
	}
}
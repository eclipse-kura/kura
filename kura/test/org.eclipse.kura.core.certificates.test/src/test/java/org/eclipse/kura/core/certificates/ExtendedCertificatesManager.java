/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.kura.core.certificates;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.Enumeration;

public class ExtendedCertificatesManager extends CertificatesManager {

	private String keyStorePassword;
	private String fakeCertificateAlias;
	private Certificate fakeCertificate;
	private Enumeration<String> fakeAliases;

	public ExtendedCertificatesManager(String keyStorePassword, String fakeCertificateAlias,
			Certificate fakeCertificate, Enumeration<String> fakeAliases) {
		this.keyStorePassword = keyStorePassword;
		this.fakeCertificateAlias = fakeCertificateAlias;
		this.fakeCertificate = fakeCertificate;
		this.fakeAliases = fakeAliases;
	}

	@Override
	protected Certificate getCertificateFromKeyStore(char[] keyStorePassword, String alias)
			throws NoSuchAlgorithmException, CertificateException, KeyStoreException, IOException {

		if (!Arrays.equals(keyStorePassword, this.keyStorePassword.toCharArray())) {
			throw new IOException(new UnrecoverableKeyException("Invalid password"));
		}

		if (alias != fakeCertificateAlias) {
			return null;
		}

		return this.fakeCertificate;
	}

	@Override
	protected Enumeration<String> getAliasesFromKeyStore(char[] keyStorePassword)
			throws NoSuchAlgorithmException, CertificateException, KeyStoreException, IOException {

		if (!Arrays.equals(keyStorePassword, this.keyStorePassword.toCharArray())) {
			throw new IOException(new UnrecoverableKeyException("Invalid password"));
		}

		return this.fakeAliases;
	}
}

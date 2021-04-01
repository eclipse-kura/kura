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
package org.eclipse.kura.core.certificates;

import java.security.cert.Certificate;
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

    // @Override
    // protected Certificate getCertificateFromKeyStore(char[] keyStorePassword, String alias)
    // throws NoSuchAlgorithmException, CertificateException, KeyStoreException, IOException {
    //
    // if (!Arrays.equals(keyStorePassword, this.keyStorePassword.toCharArray())) {
    // throw new IOException(new UnrecoverableKeyException("Invalid password"));
    // }
    //
    // if (alias != fakeCertificateAlias) {
    // return null;
    // }
    //
    // return this.fakeCertificate;
    // }
    //
    // @Override
    // protected Enumeration<String> getAliasesFromKeyStore(char[] keyStorePassword)
    // throws NoSuchAlgorithmException, CertificateException, KeyStoreException, IOException {
    //
    // if (!Arrays.equals(keyStorePassword, this.keyStorePassword.toCharArray())) {
    // throw new IOException(new UnrecoverableKeyException("Invalid password"));
    // }
    //
    // return this.fakeAliases;
    // }
    //
    // @Override
    // protected Enumeration<String> getAliasesFromKeyStore(String path, char[] keyStorePassword)
    // throws NoSuchAlgorithmException, CertificateException, KeyStoreException, IOException {
    // if (!Arrays.equals(keyStorePassword, this.keyStorePassword.toCharArray())) {
    // throw new IOException(new UnrecoverableKeyException("Invalid password"));
    // }
    //
    // return this.fakeAliases;
    // }
    //
    // @Override
    // protected String getSslKeystorePath() throws KuraException {
    // try {
    // return (String) TestUtil.getFieldValue(this, "DEFAULT_KEYSTORE");
    // } catch (NoSuchFieldException e) {
    // throw new KuraException(KuraErrorCode.SECURITY_EXCEPTION, e);
    // }
    // }
}

/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates and others
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

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

final class KeyStoreManagement {

    private static final String ENV_JAVA_KEYSTORE = System.getenv("JAVA_HOME") + "/jre/lib/security/cacerts";

    private KeyStoreManagement() {

    }

    static KeyStore loadKeyStore(byte[] password)
            throws NoSuchAlgorithmException, CertificateException, KeyStoreException, IOException {
        return loadKeyStore(new String(password).toCharArray());
    }

    static void saveKeyStore(KeyStore keystore, byte[] password)
            throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
        saveKeyStore(keystore, new String(password).toCharArray());
    }

    static KeyStore loadKeyStore(char[] password)
            throws NoSuchAlgorithmException, CertificateException, KeyStoreException, IOException {
        return loadKeyStore(ENV_JAVA_KEYSTORE, new String(password).toCharArray());
    }

    static void saveKeyStore(KeyStore keystore, char[] password)
            throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
        saveKeyStore(keystore, ENV_JAVA_KEYSTORE, new String(password).toCharArray());
    }

    static void saveKeyStore(String keystorePath, KeyStore keystore, char[] password)
            throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
        saveKeyStore(keystore, keystorePath, new String(password).toCharArray());
    }

    static KeyStore loadKeyStore(String location, char[] password)
            throws IOException, NoSuchAlgorithmException, CertificateException, KeyStoreException {
        FileInputStream is = null;
        try {
            is = new FileInputStream(location);
            KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
            keystore.load(is, password);
            is.close();
            return keystore;
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }

    static void saveKeyStore(KeyStore keystore, String location, char[] password)
            throws IOException, KeyStoreException, NoSuchAlgorithmException, CertificateException {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(location);
            keystore.store(fos, password);
            fos.flush();
            fos.close();
        } finally {
            if (fos != null) {
                fos.close();
            }
        }
    }

}

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
 ******************************************************************************/
package org.eclipse.kura.crypto;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.eclipse.kura.KuraException;
import org.osgi.annotation.versioning.ProviderType;

/**
 * The CryptoService is used to provide AES encrypt and decrypt functionality, Base64 encoding and
 * decoding, and hash generation.
 *
 * @noimplement This interface is not intended to be implemented by clients.
 */
@ProviderType
public interface CryptoService {

    /**
     * Returns an AES encrypted char array based on the provided value.
     *
     * @param value
     *            A char array that will be encrypted.
     * @return The char array representing the encrypted value.
     * @throws KuraException
     */
    public char[] encryptAes(char[] value) throws KuraException;

    /**
     * Returns a char array based on the provided encrypted value.
     *
     * @param encryptedValue
     *            A char array representing the value to be decrypted.
     * @return char[] that has been decrypted.
     * @throws KuraException
     */
    public char[] decryptAes(char[] encryptedValue) throws KuraException;

    /**
     * Returns an AES encrypted string based on the provided value.
     *
     * @param value
     *            A string that will be encrypted.
     * @return String that has been encrypted.
     * @throws NoSuchAlgorithmException
     * @throws NoSuchPaddingException
     * @throws InvalidKeyException
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     * @deprecated Use {@link #encryptAes(char[]) instead
     */
    @Deprecated
    public String encryptAes(String value) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException,
            IllegalBlockSizeException, BadPaddingException;

    /**
     * Returns a plain text string based on the provided encrypted value.
     *
     * @param encryptedValue
     *            A string representing the value to be decrypted.
     * @return String that has been decrypted.
     * @throws NoSuchAlgorithmException
     * @throws NoSuchPaddingException
     * @throws InvalidKeyException
     * @throws IOException
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     * @deprecated Use {@link #decryptAes(char[])} instead
     */
    @Deprecated
    public String decryptAes(String encryptedValue) throws NoSuchAlgorithmException, NoSuchPaddingException,
            InvalidKeyException, IOException, IllegalBlockSizeException, BadPaddingException;

    /**
     * Returns a SHA1 hashed value of the provided string s.
     *
     * @param s
     *            A string on which to run the SHA1 hashing algorithm.
     * @return String that has been hashed.
     * @throws NoSuchAlgorithmException
     * @throws UnsupportedEncodingException
     */
    default String sha1Hash(String s) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        return hash(s, "SHA-1");
    }

    /**
     * Returns a SHA-256 hashed value of the provided string s.
     *
     * @param s
     *            A string on which to run the SHA-256 hashing algorithm.
     * @return String that has been hashed.
     * @throws NoSuchAlgorithmException
     * @throws UnsupportedEncodingException
     * @since 3.0
     */
    default String sha256Hash(String s) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        return hash(s, "SHA-256");
    }

    /**
     * Returns an hashed value of the provided string s.
     *
     * @param s
     *            A string on which to run the hashing algorithm.
     * @param algorithm
     *            A String representing the hashing algorithm to be applied
     * @return String that has been hashed.
     * @throws NoSuchAlgorithmException
     * @throws UnsupportedEncodingException
     * @since 3.0
     */
    public String hash(String s, String algorithm) throws NoSuchAlgorithmException, UnsupportedEncodingException;

    /**
     * Returns an encoded string based on the provided stringValue.
     *
     * @param stringValue
     *            A string to be encoded.
     * @return String that has been encoded.
     * @throws NoSuchAlgorithmException
     * @throws UnsupportedEncodingException
     */
    public String encodeBase64(String stringValue) throws NoSuchAlgorithmException, UnsupportedEncodingException;

    /**
     * Returns a decoded string based on the provided encodedValue.
     *
     * @param encodedValue
     *            A string to be decoded.
     * @return String that has been decoded.
     * @throws NoSuchAlgorithmException
     * @throws UnsupportedEncodingException
     */
    public String decodeBase64(String encodedValue) throws NoSuchAlgorithmException, UnsupportedEncodingException;

    /**
     * Takes a keystore path and returns the corresponding password that can be
     * used to access to the data saved in the specified keystore.
     *
     * @param keyStorePath
     *            A String that represents a unique identifier of the specified keystore.
     * @return A char array that represents the password of the specified keystore.
     */
    public char[] getKeyStorePassword(String keyStorePath);

    /**
     * Takes a keystore path as a String and a char array representing a password
     * that has to be stored for the specified keystore.
     *
     * @param keyStorePath
     *            A String that represents a unique identifier of the specified keystore.
     * @param password
     *            A char array that represents the password of the specified keystore.
     * @throws KuraException
     */
    public void setKeyStorePassword(String keyStorePath, char[] password) throws KuraException;

    /**
     * Takes a keystore path as a String and a char array representing a password
     * that has to be stored for the specified keystore.
     *
     * @param keyStorePath
     *            A String that represents a unique identifier of the specified keystore.
     * @param password
     *            A String that represents the password of the specified keystore.
     * @throws IOException
     * @deprecated Use {@link #setKeyStorePassword(String, char[])} instead
     */
    @Deprecated
    public void setKeyStorePassword(String keyStorePath, String password) throws IOException;

    /**
     * Answers if the Kura framework is running in security mode.
     *
     * @return true if the framework is running in security mode.
     */
    public boolean isFrameworkSecure();
}

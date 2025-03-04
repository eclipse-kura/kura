/*******************************************************************************
 * Copyright (c) 2017, 2025 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.marshalling;

import java.io.InputStream;

import org.eclipse.kura.KuraException;
import org.osgi.annotation.versioning.ProviderType;

/**
 * This interface exposes methods for unmarshalling content
 *
 * @noimplement This interface is not intended to be implemented by clients.
 * @since 1.4
 */
@ProviderType
public interface Unmarshaller {

    /**
     * This method takes a String representation and a class that will be used as reference to construct the result.
     *
     * @param string
     *            the input string
     * @param clazz
     *            the class representing the type of object expected for the result
     * @return an object that is constructed from the passed string
     * @throws KuraException
     *             when the unmarshaling operation fails.
     */
    public <T> T unmarshal(String string, Class<T> clazz) throws KuraException;

    /**
     * Deserialises an object of the specified type from the provided {@link InputStream}
     * 
     * @param in
     *            the input stream
     * @param clazz
     *            the class representing the type of object expected for the result
     * @return an object that is constructed from the passed string
     * @throws KuraException
     *             when the unmarshaling operation fails.
     * @since 3.0
     */
    public <T> T unmarshal(InputStream in, Class<T> clazz) throws KuraException;
}

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

import java.io.OutputStream;

import org.eclipse.kura.KuraException;
import org.osgi.annotation.versioning.ProviderType;

/**
 * This interface exposes methods for marshalling content
 *
 * @noimplement This interface is not intended to be implemented by clients.
 * @since 1.4
 */
@ProviderType
public interface Marshaller {

    /**
     * Returns a {@link String} that represents the {@link Object} passed as input.
     *
     * @param object
     *            the object that will be marshalled.
     * @return a {@link String} representing the string representation of the object passed as input
     * @throws KuraException
     *             when the marshalling operation fails.
     */
    public String marshal(Object object) throws KuraException;

    /**
     * Writes on the provided {@link OutputStream} the {@link Object} passed as input
     * 
     * @param out
     *            the {@link OutputStream} on which the data will be written
     * @param object
     *            the {@link Object} that will be marshalled.
     * @throws KuraException
     * @since 8.0
     */
    public void marshal(final OutputStream out, Object object) throws KuraException;
}

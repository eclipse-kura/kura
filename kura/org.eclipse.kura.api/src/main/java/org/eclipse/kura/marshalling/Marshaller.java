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
 ******************************************************************************/
package org.eclipse.kura.marshalling;

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
}

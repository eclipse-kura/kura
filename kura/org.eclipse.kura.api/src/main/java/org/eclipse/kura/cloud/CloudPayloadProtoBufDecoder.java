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
package org.eclipse.kura.cloud;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.message.KuraPayload;
import org.osgi.annotation.versioning.ProviderType;

/**
 * @noimplement This interface is not intended to be implemented by clients.
 */
@ProviderType
public interface CloudPayloadProtoBufDecoder {

    /**
     * Decodes a Google Protocol Buffers encoded, optionally gzipped, binary payload to a
     * {@link org.eclipse.kura.message.KuraPayload}.
     *
     * @param payload
     * @return
     * @throws KuraException
     */
    public KuraPayload buildFromByteArray(byte[] payload) throws KuraException;
}

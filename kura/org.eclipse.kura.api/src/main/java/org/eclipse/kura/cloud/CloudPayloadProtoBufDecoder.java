/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.cloud;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.message.KuraPayload;

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

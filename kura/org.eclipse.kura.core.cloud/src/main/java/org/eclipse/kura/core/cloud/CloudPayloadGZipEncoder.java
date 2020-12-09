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
package org.eclipse.kura.core.cloud;

import java.io.IOException;

import org.eclipse.kura.core.util.GZipUtil;

public class CloudPayloadGZipEncoder implements CloudPayloadEncoder {

    private final CloudPayloadEncoder decorated;

    public CloudPayloadGZipEncoder(CloudPayloadEncoder decorated) {
        this.decorated = decorated;
    }

    @Override
    public byte[] getBytes() throws IOException {
        byte[] source = this.decorated.getBytes();
        byte[] compressed = GZipUtil.compress(source);

        // Return gzip compressed data only if shorter than uncompressed one
        return compressed.length < source.length ? compressed : source;
    }
}

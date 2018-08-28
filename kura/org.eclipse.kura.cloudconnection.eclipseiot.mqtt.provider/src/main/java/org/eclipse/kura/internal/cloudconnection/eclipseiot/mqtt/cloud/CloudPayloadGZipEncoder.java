/*******************************************************************************
 * Copyright (c) 2011, 2018 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.internal.cloudconnection.eclipseiot.mqtt.cloud;

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

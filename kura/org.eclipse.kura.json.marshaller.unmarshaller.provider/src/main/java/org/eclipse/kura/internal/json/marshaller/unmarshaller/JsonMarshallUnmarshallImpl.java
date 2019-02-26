/*******************************************************************************
 * Copyright (c) 2017, 2019 Eurotech and/or its affiliates and others
 *
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.kura.internal.json.marshaller.unmarshaller;

import java.io.InputStream;
import java.io.OutputStream;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.internal.json.marshaller.unmarshaller.message.CloudPayloadJsonDecoder;
import org.eclipse.kura.internal.json.marshaller.unmarshaller.message.CloudPayloadJsonEncoder;
import org.eclipse.kura.internal.json.marshaller.unmarshaller.wiregraph.WireGraphJsonMarshallUnmarshallImpl;
import org.eclipse.kura.marshalling.Marshaller;
import org.eclipse.kura.marshalling.Unmarshaller;
import org.eclipse.kura.message.KuraPayload;
import org.eclipse.kura.wire.graph.WireGraphConfiguration;

import com.eclipsesource.json.JsonObject;

public class JsonMarshallUnmarshallImpl implements Marshaller, Unmarshaller {

    @Override
    public String marshal(Object object) throws KuraException {
        if (object instanceof WireGraphConfiguration) {
            JsonObject result = WireGraphJsonMarshallUnmarshallImpl
                    .marshalWireGraphConfiguration((WireGraphConfiguration) object);
            return result.toString();
        } else if (object instanceof KuraPayload) {
            return CloudPayloadJsonEncoder.marshal((KuraPayload) object);
        }
        throw new KuraException(KuraErrorCode.INVALID_PARAMETER);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T unmarshal(String s, Class<T> clazz) throws KuraException {
        if (clazz.equals(WireGraphConfiguration.class)) {
            return (T) WireGraphJsonMarshallUnmarshallImpl.unmarshalToWireGraphConfiguration(s);
        } else if (clazz.equals(KuraPayload.class)) {
            return (T) CloudPayloadJsonDecoder.buildFromString(s);
        }
        throw new IllegalArgumentException("Invalid parameter!");
    }

    @Override
    public void marshal(Object object, OutputStream w) throws Exception {

    }

    @Override
    public <T> T unmarshal(InputStream inputStream, Class<T> clazz) throws KuraException {
        return null;
    }

}

/*******************************************************************************
 * Copyright (c) 2023 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.core.keystore.request.handler;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloudconnection.message.KuraMessage;
import org.eclipse.kura.cloudconnection.request.RequestHandlerContext;
import org.eclipse.kura.core.keystore.request.PrivateKeyWriteRequest;
import org.eclipse.kura.core.keystore.util.PrivateKeyInfo;
import org.eclipse.kura.message.KuraPayload;
import org.eclipse.kura.message.KuraResponsePayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KeystoreServiceRequestHandlerV2 extends KeystoreServiceRequestHandler {

    private static final Logger logger = LoggerFactory.getLogger(KeystoreServiceRequestHandlerV2.class);

    protected static final String PRIVATEKEY = "privatekey";

    private static final List<String> PRIVATE_KEY_RESOURCE = Arrays.asList(KEYSTORES, ENTRIES, PRIVATEKEY);

    public KeystoreServiceRequestHandlerV2() {
        super("KEYS-V2");
    }

    @Override
    public KuraMessage doPost(RequestHandlerContext context, KuraMessage reqMessage) throws KuraException {
        final List<String> resourcePath = extractResourcePath(reqMessage);

        if (!PRIVATE_KEY_RESOURCE.equals(resourcePath)) {
            return super.doPost(context, reqMessage);
        }

        final KuraPayload reqPayload = reqMessage.getPayload();

        final String body = new String(reqPayload.getBody(), StandardCharsets.UTF_8);
        final PrivateKeyInfo privateKeyInfo = unmarshal(body, PrivateKeyInfo.class);
        validateAs(privateKeyInfo, PrivateKeyWriteRequest::new);
        try {
            storePrivateKeyEntryInternal(privateKeyInfo);
        } catch (final Exception e) {
            logger.warn("Failed to store private key entry", e);
            return new KuraMessage(new KuraResponsePayload(500));
        }
        return new KuraMessage(new KuraResponsePayload(200));

    }

}

/*******************************************************************************
 * Copyright (c) 2024 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.internal.rest.security.provider;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.request.handler.jaxrs.DefaultExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

@Path("security/v2")
public class SecurityRestServiceV2 extends AbstractRestSecurityService {

    private static final Logger logger = LoggerFactory.getLogger(SecurityRestServiceV2.class);
    private static final String MQTT_APP_ID = "SEC-V2";

    @Override
    public String getMqttAppId() {
        return MQTT_APP_ID;
    }

    /**
     * POST method <br /> This method replaces the security policy with the default production one. Then a fingerprint
     * reload is performed.
     */
    @POST
    @RolesAllowed(REST_ROLE_NAME)
    @Path("/security-policy/apply-default-production")
    public Response applyDefaultProductionSecurityPolicy() {
        try {
            logger.debug(DEBUG_MESSAGE, "applyDefaultProductionSecurityPolicy");

            this.security.applyDefaultProductionSecurityPolicy();
            this.security.reloadSecurityPolicyFingerprint();
            this.security.reloadCommandLineFingerprint();
        } catch (Exception e) {
            throw DefaultExceptionHandler.toWebApplicationException(e);
        }

        return Response.ok().build();
    }

    /**
     * POST method <br /> This method replaces the security policy with the provided one. Then a fingerprint reload is
     * performed.
     */
    @POST
    @RolesAllowed(REST_ROLE_NAME)
    @Path("/security-policy/apply")
    public Response applySecurityPolicy(InputStream securityPolicyInputStream) {
        try {
            logger.debug(DEBUG_MESSAGE, "applySecurityPolicy");

            this.security.applySecurityPolicy(readSecurityPolicyString(securityPolicyInputStream));
            this.security.reloadSecurityPolicyFingerprint();
            this.security.reloadCommandLineFingerprint();
        } catch (KuraException e) {
            if (KuraErrorCode.INVALID_PARAMETER.equals(e.getCode())) {
                throw DefaultExceptionHandler.buildWebApplicationException(Status.BAD_REQUEST, e.getMessage());
            }
            throw DefaultExceptionHandler.toWebApplicationException(e);
        }

        return Response.ok().build();
    }

    private String readSecurityPolicyString(InputStream securityPolicyInputStream) {
        if (securityPolicyInputStream == null) {
            throw DefaultExceptionHandler.buildWebApplicationException(Status.BAD_REQUEST,
                    "Security Policy cannot be null or empty");
        }
        int bytesRead;
        int totalBytesRead = 0;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[1024];
        try {
            while ((bytesRead = securityPolicyInputStream.read(data, 0, data.length)) != -1) {
                totalBytesRead += bytesRead;
                if (totalBytesRead > 1024 * 1024) {
                    throw DefaultExceptionHandler.buildWebApplicationException(Status.BAD_REQUEST,
                            "Security policy too large");
                }
                buffer.write(data, 0, bytesRead);
            }
            buffer.flush();
        } catch (IOException e) {
            throw DefaultExceptionHandler.toWebApplicationException(e);
        }
        if (buffer.size() == 0) {
            throw DefaultExceptionHandler.buildWebApplicationException(Status.BAD_REQUEST,
                    "Security Policy cannot be null or empty");
        }
        CharBuffer charBuffer = getCharBuffer(buffer);
        return charBuffer.toString();
    }

    private static CharBuffer getCharBuffer(ByteArrayOutputStream buffer) {
        final CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT);
        CharBuffer charBuffer;
        try {
            charBuffer = decoder.decode(ByteBuffer.wrap(buffer.toByteArray()));
        } catch (CharacterCodingException e) {
            throw DefaultExceptionHandler.buildWebApplicationException(Status.BAD_REQUEST,
                    "Security Policy must be UTF-8 encoded");
        }
        return charBuffer;
    }

}

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

import org.eclipse.kura.request.handler.jaxrs.DefaultExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
        } catch (Exception e) {
            throw DefaultExceptionHandler.toWebApplicationException(e);
        }

        return Response.ok().build();
    }

    private String readSecurityPolicyString(InputStream securityPolicyInputStream) throws IOException {
        if (securityPolicyInputStream == null) {
            throw new IllegalArgumentException("Security Policy cannot be null or empty");
        }
        int bytesRead;
        int chunksRead = 0;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[1024];
        while ((bytesRead = securityPolicyInputStream.read(data, 0, data.length)) != -1) {
            if (chunksRead++ > 1024) {
                throw new IllegalArgumentException("Security policy too large");
            }
            buffer.write(data, 0, bytesRead);
        }
        buffer.flush();
        if (buffer.size() == 0) {
            throw new IllegalArgumentException("Security Policy cannot be null or empty");
        }
        return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
    }

}

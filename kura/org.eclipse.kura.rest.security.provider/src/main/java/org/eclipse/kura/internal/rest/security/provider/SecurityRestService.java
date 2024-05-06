/*******************************************************************************
 * Copyright (c) 2023, 2024 Eurotech and/or its affiliates and others
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

import org.eclipse.kura.cloudconnection.request.RequestHandler;
import org.eclipse.kura.cloudconnection.request.RequestHandlerRegistry;
import org.eclipse.kura.internal.rest.security.provider.dto.DebugEnabledDTO;
import org.eclipse.kura.internal.rest.security.provider.dto.SecurityPolicyDTO;
import org.eclipse.kura.request.handler.jaxrs.DefaultExceptionHandler;
import org.eclipse.kura.request.handler.jaxrs.JaxRsRequestHandlerProxy;
import org.eclipse.kura.security.SecurityService;
import org.eclipse.kura.system.SystemService;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.UserAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

@Path("security/v1")
public class SecurityRestService {

    private static final Logger logger = LoggerFactory.getLogger(SecurityRestService.class);
    private static final String DEBUG_MESSAGE = "Processing request for method '{}'";

    private static final String MQTT_APP_ID = "SEC-V1";
    private static final String REST_ROLE_NAME = "security";
    private static final String KURA_PERMISSION_REST_ROLE = "kura.permission.rest." + REST_ROLE_NAME;

    private SecurityService security;
    private SystemService system;
    private final RequestHandler requestHandler = new JaxRsRequestHandlerProxy(this);

    public void bindSecurityService(SecurityService securityService) {
        this.security = securityService;
    }

    public void bindUserAdmin(UserAdmin userAdmin) {
        userAdmin.createRole(KURA_PERMISSION_REST_ROLE, Role.GROUP);
    }

    public void bindRequestHandlerRegistry(RequestHandlerRegistry registry) {
        try {
            registry.registerRequestHandler(MQTT_APP_ID, this.requestHandler);
        } catch (final Exception e) {
            logger.warn("Failed to register {} request handler", MQTT_APP_ID, e);
        }
    }

    public void unbindRequestHandlerRegistry(RequestHandlerRegistry registry) {
        try {
            registry.unregister(MQTT_APP_ID);
        } catch (final Exception e) {
            logger.warn("Failed to unregister {} request handler", MQTT_APP_ID, e);
        }
    }

    public void bindSystemService(SystemService systemService) {
        this.system = systemService;
    }

    /**
     * POST method <br /> This method allows the reload of the security policy's fingerprint
     */
    @POST
    @RolesAllowed(REST_ROLE_NAME)
    @Path("/security-policy-fingerprint/reload")
    @Produces(MediaType.APPLICATION_JSON)
    public Response reloadSecurityPolicyFingerprint() {
        try {
            logger.debug(DEBUG_MESSAGE, "reloadSecurityPolicyFingerprint");
            this.security.reloadSecurityPolicyFingerprint();
        } catch (Exception e) {
            throw DefaultExceptionHandler.toWebApplicationException(e);
        }

        return Response.ok().build();
    }

    /**
     * POST method <br /> This method allows the reload of the command line fingerprint
     */
    @POST
    @RolesAllowed(REST_ROLE_NAME)
    @Path("/command-line-fingerprint/reload")
    @Produces(MediaType.APPLICATION_JSON)
    public Response reloadCommandLineFingerprint() {
        try {
            logger.debug(DEBUG_MESSAGE, "reloadCommandLineFingerprint");
            this.security.reloadCommandLineFingerprint();
        } catch (Exception e) {
            throw DefaultExceptionHandler.toWebApplicationException(e);
        }

        return Response.ok().build();
    }

    /**
     * POST method <br /> This method replaces the security policy with the default production one. Then a fingerprint
     * reload is performed.
     */
    @POST
    @RolesAllowed(REST_ROLE_NAME)
    @Path("/security-policy/default-load")
    @Produces(MediaType.APPLICATION_JSON)
    public Response loadDefaultSecurityPolicy() {
        try {
            logger.debug(DEBUG_MESSAGE, "loadDefaultSecurityPolicy");
            copyDefaultSecurityPolicy();
            this.security.reloadSecurityPolicyFingerprint();
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
    @Path("/security-policy/upload")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response uploadSecurityPolicy(SecurityPolicyDTO securityPolicy) {
        try {
            logger.debug(DEBUG_MESSAGE, "uploadSecurityPolicy");

            String securityPolicyContent = securityPolicy.getSecurityPolicy();
            if (SecurityPolicyDTO.isEmptyOrNull(securityPolicyContent)) {
                throw DefaultExceptionHandler.buildWebApplicationException(Status.BAD_REQUEST,
                        "Security Policy not specified");
            }

            if (!isXmlValid(securityPolicyContent)) {
                throw DefaultExceptionHandler.buildWebApplicationException(Status.BAD_REQUEST,
                        "Security Policy not valid");
            }

            saveSecurityPolicy(securityPolicyContent);
            this.security.reloadSecurityPolicyFingerprint();
        } catch (Exception e) {
            throw DefaultExceptionHandler.toWebApplicationException(e);
        }

        return Response.ok().build();
    }

    /**
     * GET method
     *
     * @return true if the debug is permitted. False otherwise.
     */
    @GET
    @Path("/debug-enabled")
    @Produces(MediaType.APPLICATION_JSON)
    public DebugEnabledDTO isDebugEnabled(final @Context ContainerRequestContext context) {
        try {

            if (context != null && !Optional.ofNullable(context.getSecurityContext())
                    .filter(c -> c.getUserPrincipal() != null).isPresent()) {
                throw new WebApplicationException(Status.UNAUTHORIZED);
            }

            logger.debug(DEBUG_MESSAGE, "isDebugEnabled");
            return new DebugEnabledDTO(this.security.isDebugEnabled());
        } catch (Exception e) {
            throw DefaultExceptionHandler.toWebApplicationException(e);
        }

    }

    private void copyDefaultSecurityPolicy() throws IOException {
        String kuraHomeFolder = this.system.getKuraHome();
        java.nio.file.Path defaultSecurityPolicyPath = Paths.get(
                kuraHomeFolder + "/.data/security_policy_backup/security-production.policy");
        String kuraUserFolder = this.system.getKuraUserConfigDirectory();
        java.nio.file.Path securityPolicyPath = Paths.get(kuraUserFolder + "/security/security.policy");
        Files.copy(defaultSecurityPolicyPath, securityPolicyPath, StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.COPY_ATTRIBUTES);
    }

    private void saveSecurityPolicy(String securityPolicyContent) throws IOException {
        String kuraUserFolder = this.system.getKuraUserConfigDirectory();
        java.nio.file.Path securityPolicyPath = Paths.get(kuraUserFolder + "/security/security.policy");
        Files.write(securityPolicyPath, securityPolicyContent.getBytes());
    }

    // add temporary files....

    private boolean isXmlValid(String xml) throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        factory.setValidating(false);
        factory.setNamespaceAware(true);
        DocumentBuilder parser = factory.newDocumentBuilder();

        try {
            parser.parse(new InputSource(xml));
        } catch (SAXException | IOException e) {
            return false;
        }

        return true;
    }
}

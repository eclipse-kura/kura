/*******************************************************************************
 * Copyright (c) 2017, 2019 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/

package org.eclipse.kura.internal.rest.provider;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.Arrays;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.ext.Provider;

import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.crypto.CryptoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eclipsesource.jaxrs.provider.security.AuthenticationHandler;
import com.eclipsesource.jaxrs.provider.security.AuthorizationHandler;

@Provider
public class RestService
        implements ConfigurableComponent, AuthenticationHandler, AuthorizationHandler, ContainerResponseFilter {

    private static final Logger logger = LoggerFactory.getLogger(RestService.class);
    private static final Logger auditLogger = LoggerFactory.getLogger("AuditLogger");

    private static final Decoder BASE64_DECODER = Base64.getDecoder();
    private static final Response UNAUTHORIZED_RESPONSE = Response.status(Response.Status.UNAUTHORIZED)
            .header("WWW-Authenticate", "Basic realm=\"kura-rest-api\"").build();

    private Map<String, User> users;

    private CryptoService cryptoService;

    public void setCryptoService(CryptoService cryptoService) {
        this.cryptoService = cryptoService;
    }

    public void activate(Map<String, Object> properties) {
        logger.info("activating...");
        updated(properties);
        logger.info("activating...done");
    }

    public void deactivate() {
        logger.info("deactivating...");
        logger.info("deactivating...done");
    }

    public void updated(Map<String, Object> properties) {
        logger.info("updating...");
        this.users = User.fromOptions(new RestServiceOptions(properties));
        logger.info("updating...done");
    }

    @Override
    public boolean isUserInRole(Principal requestUser, String role) {
        return ((User) requestUser).getRoles().contains(role);
    }

    @Override
    public Principal authenticate(ContainerRequestContext request) {

        String path = getRequestPath(request);

        String authHeader = request.getHeaderString("Authorization");
        if (authHeader == null) {
            auditLogger.warn("UI Rest - Failure - Received unauthorized REST request. Method: {}, path: {}",
                    request.getMethod(), path);
            request.abortWith(UNAUTHORIZED_RESPONSE);
            return null;
        }

        StringTokenizer tokens = new StringTokenizer(authHeader);
        String authScheme = tokens.nextToken();
        if (!"Basic".equals(authScheme)) {
            auditLogger.warn("UI Rest - Failure - Received unauthorized REST request. Method: {}, path: {}",
                    request.getMethod(), path);
            request.abortWith(UNAUTHORIZED_RESPONSE);
            return null;
        }

        final String credentials = new String(BASE64_DECODER.decode(tokens.nextToken()), StandardCharsets.UTF_8);

        int colon = credentials.indexOf(':');
        String userName = credentials.substring(0, colon);
        String requestPassword = credentials.substring(colon + 1);

        final User user = this.users.get(userName);

        MultivaluedMap<String, String> headers = request.getHeaders();
        for (Entry<String, List<String>> header : headers.entrySet()) {
            logger.info("Header key: {}, value: {}", header.getKey(), header.getValue());
        }

        Collection<String> propNames = request.getPropertyNames();
        for (String propName : propNames) {
            logger.info("Property key: {}, value: {}", propName, request.getProperty(propName));
        }

        try {
            final char[] userPassword = user.getPassword().getPassword();
            if (userPassword.length == 0 && requestPassword.isEmpty()
                    || Arrays.equals(userPassword, this.cryptoService.encryptAes(requestPassword.toCharArray()))) {
                auditLogger.info("UI Rest - Success - Received REST request for user: {}, method: {}, path: {}",
                        userName, request.getMethod(), path);
                return user;
            }
        } catch (Exception e) {
        }
        return null;
    }

    private String getRequestPath(ContainerRequestContext request) {
        List<PathSegment> pathSegments = request.getUriInfo().getPathSegments();
        Iterator<PathSegment> iterator = pathSegments.iterator();
        StringBuilder pathBuilder = new StringBuilder();

        while (iterator.hasNext()) {
            pathBuilder.append(iterator.next().getPath());
            if (iterator.hasNext()) {
                pathBuilder.append("/");
            }
        }

        return pathBuilder.toString();
    }

    @Override
    public String getAuthenticationScheme() {
        return SecurityContext.BASIC_AUTH;
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
            throws IOException {
        String path = getRequestPath(requestContext);

        int responseStatus = responseContext.getStatus();
        if (responseStatus == Response.Status.OK.getStatusCode()) {
            auditLogger.info("UI Rest - Success - Request succeeded for method: {}, path: {}",
                    requestContext.getMethod(), path);
        } else {
            auditLogger.warn(
                    "UI Rest - Failure - Request failed for method: {}, path: {}, response code: {}, message: {}",
                    requestContext.getMethod(), path, responseStatus, responseContext.getEntity());
        }

    }
}

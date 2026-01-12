/*******************************************************************************
 * Copyright (c) 2025 Eurotech and/or its affiliates and others
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

package org.eclipse.kura.internal.rest.provider;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.kura.audit.AuditConstants;
import org.eclipse.kura.audit.AuditContext;
import org.eclipse.kura.audit.AuditContext.Scope;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.PathSegment;

public final class RestServiceUtils {

    private RestServiceUtils() {
    }

    public static AuditContext initAuditContext(final ContainerRequestContext requestContext,
            final HttpServletRequest request) {

        final Object rawContext = requestContext.getProperty("org.eclipse.kura.rest.audit.context");

        if (rawContext != null) {
            return (AuditContext) rawContext;
        }

        final Map<String, String> properties = new HashMap<>();

        String requestIp = requestContext.getHeaderString("X-FORWARDED-FOR");
        if (requestIp == null) {
            requestIp = request.getRemoteAddr();
        }

        properties.put(AuditConstants.KEY_ENTRY_POINT.getValue(), "RestService");
        properties.put(AuditConstants.KEY_IP.getValue(), requestIp);
        properties.put("rest.method", requestContext.getMethod());
        properties.put("rest.path", getRequestPath(requestContext));

        final AuditContext result = new AuditContext(properties);

        final Scope scope = AuditContext.openScope(result);

        requestContext.setProperty("org.eclipse.kura.rest.audit.context", result);
        requestContext.setProperty("org.eclipse.kura.rest.audit.scope", scope);

        return result;
    }

    private static String getRequestPath(final ContainerRequestContext request) {
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

    public static void closeAuditContext(ContainerRequestContext request) {
        final Object rawScope = request.getProperty("org.eclipse.kura.rest.audit.scope");

        if (rawScope instanceof Scope) {
            ((Scope) rawScope).close();
        }
    }

    public static Dictionary<String, Object> singletonDictionary(final String key, final Object value) {
        final Dictionary<String, Object> result = new Hashtable<>();
        result.put(key, value);
        return result;
    }

    public static Dictionary<String, Object> resourceProperties() {
        return singletonDictionary("osgi.jakartars.resource", true);
    }

    public static Dictionary<String, Object> extensionProperties() {
        return singletonDictionary("osgi.jakartars.extension", true);
    }
}

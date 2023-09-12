/*******************************************************************************
 * Copyright (c) 2017, 2023 Eurotech and/or its affiliates and others
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

import static org.eclipse.kura.internal.rest.auth.SessionRestServiceConstants.BASE_PATH;
import static org.eclipse.kura.internal.rest.auth.SessionRestServiceConstants.CHANGE_PASSWORD_PATH;
import static org.eclipse.kura.internal.rest.auth.SessionRestServiceConstants.XSRF_TOKEN_PATH;

import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import javax.annotation.Priority;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import org.eclipse.kura.audit.AuditConstants;
import org.eclipse.kura.audit.AuditContext;
import org.eclipse.kura.audit.AuditContext.Scope;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.crypto.CryptoService;
import org.eclipse.kura.internal.rest.auth.BasicAuthenticationProvider;
import org.eclipse.kura.internal.rest.auth.CertificateAuthenticationProvider;
import org.eclipse.kura.internal.rest.auth.RestSessionHelper;
import org.eclipse.kura.internal.rest.auth.SessionAuthProvider;
import org.eclipse.kura.internal.rest.auth.SessionRestService;
import org.eclipse.kura.rest.auth.AuthenticationProvider;
import org.eclipse.kura.util.useradmin.UserAdminHelper;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.useradmin.UserAdmin;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eclipsesource.jaxrs.provider.security.AuthenticationHandler;
import com.eclipsesource.jaxrs.provider.security.AuthorizationHandler;

@SuppressWarnings("restriction")
@Provider
public class RestService
        implements AuthenticationHandler, AuthorizationHandler, ConfigurableComponent, ContainerResponseFilter {

    private static final Logger logger = LoggerFactory.getLogger(RestService.class);
    private static final Logger auditLogger = LoggerFactory.getLogger("AuditLogger");

    private static final Response NOT_FOUND_RESPONSE = Response.status(Response.Status.NOT_FOUND).build();

    private CryptoService cryptoService;
    private UserAdmin userAdmin;

    RestServiceOptions options;

    private final List<ServiceRegistration<?>> registeredServices = new ArrayList<>();
    private final Set<AuthenticationProviderHolder> authenticationProviders = new TreeSet<>();

    private AuthenticationProvider basicAuthProvider;
    private AuthenticationProvider certificateAuthProvider;
    private SessionAuthProvider sessionAuthenticationProvider;
    private SessionRestService authRestService;
    private UserAdminHelper userAdminHelper;

    private ServiceTracker<Object, Thread> tracker;

    @Context
    private HttpServletRequest request;
    @Context
    private HttpServletResponse response;

    public void setUserAdmin(final UserAdmin userAdmin) {
        this.userAdmin = userAdmin;
    }

    public void setCryptoService(CryptoService cryptoService) {
        this.cryptoService = cryptoService;
    }

    public void bindAuthenticationProvider(final AuthenticationProvider provider) {
        synchronized (this.authenticationProviders) {
            final AuthenticationProviderHolder holder = new AuthenticationProviderHolder(provider);
            this.authenticationProviders.add(holder);
            holder.onEnabled();
        }
    }

    public void unbindAuthenticationProvider(final AuthenticationProvider provider) {
        synchronized (this.authenticationProviders) {
            final AuthenticationProviderHolder holder = new AuthenticationProviderHolder(provider);
            if (this.authenticationProviders.remove(holder)) {
                holder.onDisabled();
            }
        }
    }

    public void activate(final Map<String, Object> properties) {
        logger.info("activating...");

        final BundleContext bundleContext = FrameworkUtil.getBundle(RestService.class).getBundleContext();

        try {
            this.tracker = new ServiceTracker<>(bundleContext, FrameworkUtil.createFilter(
                    "(osgi.http.whiteboard.servlet.name=com.eclipsesource.jaxrs.publisher.internal.ServletContainerBridge)"),
                    new ServletContainerBridgeFix(bundleContext));
            this.tracker.open();
        } catch (InvalidSyntaxException e) {
            // no need
        }

        this.userAdminHelper = new UserAdminHelper(this.userAdmin, this.cryptoService);
        final RestSessionHelper restSessionHelper = new RestSessionHelper(this.userAdminHelper);

        registeredServices
                .add(bundleContext.registerService(ContainerRequestFilter.class, new IncomingPortCheckFilter(), null));

        this.basicAuthProvider = new BasicAuthenticationProvider(bundleContext, this.userAdminHelper);
        this.certificateAuthProvider = new CertificateAuthenticationProvider(this.userAdminHelper);
        this.sessionAuthenticationProvider = new SessionAuthProvider(restSessionHelper,
                new HashSet<>(Arrays.asList(BASE_PATH + CHANGE_PASSWORD_PATH, BASE_PATH + XSRF_TOKEN_PATH)),
                Collections.singleton(BASE_PATH + XSRF_TOKEN_PATH));
        this.authRestService = new SessionRestService(this.userAdminHelper, restSessionHelper);

        this.registeredServices
                .add(bundleContext.registerService(SessionRestService.class, this.authRestService, null));

        update(properties);

        logger.info("activating...done");
    }

    public void update(final Map<String, Object> properties) {
        logger.info("updating...");

        final RestServiceOptions newOptions = new RestServiceOptions(properties);

        if (!Objects.equals(this.options, newOptions)) {
            this.options = newOptions;
            updateBuiltinAuthenticationProviders(newOptions);
            this.authRestService.setOptions(newOptions);
            this.sessionAuthenticationProvider.setOptions(newOptions);
        }

        logger.info("updating...done");
    }

    public void deactivate() {
        logger.info("deactivating...");

        this.tracker.close();

        for (final ServiceRegistration<?> reg : registeredServices) {
            reg.unregister();
        }

        synchronized (this.authenticationProviders) {
            final Iterator<AuthenticationProviderHolder> iter = this.authenticationProviders.iterator();
            while (iter.hasNext()) {
                iter.next().onDisabled();
                iter.remove();
            }
        }

        logger.info("deactivating...done");
    }

    @Override
    public boolean isUserInRole(Principal requestUser, String role) {

        try {
            this.userAdminHelper.requirePermissions(requestUser.getName(), "rest." + role);
            return true;

        } catch (final Exception e) {
            return false;
        }
    }

    @Override
    public Principal authenticate(ContainerRequestContext requestContext) {

        initAuditContext(requestContext);

        synchronized (this.authenticationProviders) {
            for (final AuthenticationProviderHolder provider : this.authenticationProviders) {
                final Optional<Principal> principal = provider.authenticate(request, requestContext);

                if (principal.isPresent()) {
                    return principal.get();
                }
            }
        }

        return null;

    }

    @Override
    public String getAuthenticationScheme() {
        return null;
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
            throws IOException {
        int responseStatus = responseContext.getStatus();

        final AuditContext auditContext = initAuditContext(requestContext);

        try {
            if (responseContext.getStatus() == 404) {
                auditLogger.warn("{} Rest - Failure - Service not found", auditContext);
                return;
            }

            if (responseContext.getStatus() == 403) {
                if (requestContext.getSecurityContext() == null
                        || requestContext.getSecurityContext().getUserPrincipal() == null) {
                    responseContext.setStatus(401);
                } else {
                    auditLogger.warn("{} Rest - Failure - User not authorized to perform the requested operation",
                            auditContext);
                    return;
                }

            }

            if (responseContext.getStatus() == 401) {
                auditLogger.warn("{} Rest - Failure - User not authenticated", auditContext);
                return;
            }

            if (responseStatus >= 200 && responseStatus < 400) {
                auditLogger.info("{} Rest - Success - Rest request succeeded", auditContext);
            } else {
                auditLogger.warn("{} Rest - Failure - Request failed", auditContext);
            }
        } finally {
            closeAuditContext(requestContext);
        }

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

    private AuditContext initAuditContext(ContainerRequestContext request) {

        final Object rawContext = request.getProperty("org.eclipse.kura.rest.audit.context");

        if (rawContext != null) {
            return (AuditContext) rawContext;
        }

        final Map<String, String> properties = new HashMap<>();

        String requestIp = request.getHeaderString("X-FORWARDED-FOR");
        if (requestIp == null) {
            requestIp = this.request.getRemoteAddr();
        }

        properties.put(AuditConstants.KEY_ENTRY_POINT.getValue(), "RestService");
        properties.put(AuditConstants.KEY_IP.getValue(), requestIp);
        properties.put("rest.method", request.getMethod());
        properties.put("rest.path", getRequestPath(request));

        final AuditContext result = new AuditContext(properties);

        final Scope scope = AuditContext.openScope(result);

        request.setProperty("org.eclipse.kura.rest.audit.context", result);
        request.setProperty("org.eclipse.kura.rest.audit.scope", scope);

        return result;
    }

    private void closeAuditContext(ContainerRequestContext request) {
        final Object rawScope = request.getProperty("org.eclipse.kura.rest.audit.scope");

        if (rawScope instanceof Scope) {
            ((Scope) rawScope).close();
        }
    }

    @Provider
    @Priority(Priorities.AUTHENTICATION - 100)
    private class IncomingPortCheckFilter implements ContainerRequestFilter {

        @Context
        private HttpServletRequest sr;

        @Override
        public void filter(final ContainerRequestContext request) throws IOException {

            initAuditContext(request);

            final Set<Integer> allowedPorts = options.getAllowedPorts();

            if (allowedPorts.isEmpty()) {
                return;
            }

            final int port = sr.getLocalPort();

            if (!allowedPorts.contains(port)) {
                request.abortWith(NOT_FOUND_RESPONSE);
            }
        }

    }

    private void updateBuiltinAuthenticationProviders(final RestServiceOptions options) {
        if (options.isPasswordAuthEnabled() && options.isBasicAuthEnabled()) {
            bindAuthenticationProvider(this.basicAuthProvider);
        } else {
            unbindAuthenticationProvider(this.basicAuthProvider);
        }

        if (options.isCertificateAuthEnabled() && options.isStatelessCertificateAuthEnabled()) {
            bindAuthenticationProvider(this.certificateAuthProvider);
        } else {
            unbindAuthenticationProvider(this.certificateAuthProvider);
        }

        if (options.isSessionManagementEnabled()) {
            bindAuthenticationProvider(this.sessionAuthenticationProvider);

        }

    }

}

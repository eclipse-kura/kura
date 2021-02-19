/*******************************************************************************
 * Copyright (c) 2011, 2021 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.web.server;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.eclipse.kura.audit.AuditContext;
import org.eclipse.kura.audit.AuditContext.Scope;
import org.eclipse.kura.web.Console;
import org.eclipse.kura.web.UserManager;
import org.eclipse.kura.web.session.Attributes;
import org.eclipse.kura.web.shared.model.GwtUserConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.server.rpc.RPCRequest;
import com.google.gwt.user.server.rpc.SerializationPolicy;
import com.google.gwt.user.server.rpc.SerializationPolicyLoader;

public class OsgiRemoteServiceServlet extends KuraRemoteServiceServlet {

    private final Logger auditLogger = LoggerFactory.getLogger("AuditLogger");

    private final Set<String> servicePermissionRequirements = new HashSet<>();
    private final Map<Method, Set<String>> methodPermissionRequirements = new HashMap<>();
    private final Map<Method, Audit> methodAuditSettings = new HashMap<>();

    public OsgiRemoteServiceServlet() {
        for (final Class<?> intf : getClass().getInterfaces()) {
            final RequiredPermissions permissions = intf.getAnnotation(RequiredPermissions.class);

            if (permissions != null) {
                servicePermissionRequirements.addAll(Arrays.asList(permissions.value()));
            }

            for (final Method method : intf.getMethods()) {
                final RequiredPermissions methodPermissions = method.getAnnotation(RequiredPermissions.class);

                if (methodPermissions != null) {
                    methodPermissionRequirements.put(method, new HashSet<>(Arrays.asList(methodPermissions.value())));
                }

                final Audit methodAudit = method.getAnnotation(Audit.class);

                if (methodAudit != null) {
                    methodAuditSettings.put(method, methodAudit);
                }
            }
        }
    }

    private static final long serialVersionUID = -8826193840033103296L;

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // Cache the current thread
        Thread currentThread = Thread.currentThread();
        // We are going to swap the class loader
        ClassLoader oldContextClassLoader = currentThread.getContextClassLoader();
        currentThread.setContextClassLoader(this.getClass().getClassLoader());

        final AuditContext auditContext = Console.initAuditContext(req);

        try (final Scope scope = AuditContext.openScope(auditContext)) {
            super.service(req, resp);
        } finally {
            currentThread.setContextClassLoader(oldContextClassLoader);
        }
    }

    /**
     * Gets the {@link SerializationPolicy} for given module base URL and strong
     * name if there is one.
     *
     * Override this method to provide a {@link SerializationPolicy} using an
     * alternative approach.
     *
     * @param request
     *            the HTTP request being serviced
     * @param moduleBaseURL
     *            as specified in the incoming payload
     * @param strongName
     *            a strong name that uniquely identifies a serialization policy
     *            file
     * @return a {@link SerializationPolicy} for the given module base URL and
     *         strong name, or <code>null</code> if there is none
     */
    @Override
    protected SerializationPolicy doGetSerializationPolicy(HttpServletRequest request, String moduleBaseURL,
            String strongName) {
        // The request can tell you the path of the web app relative to the
        // container root.
        String contextPath = request.getContextPath();
        String modulePath = null;

        if (moduleBaseURL != null) {
            try {
                modulePath = new URL(moduleBaseURL).getPath();
            } catch (MalformedURLException ex) {
                // log the information, we will default
                log("Malformed moduleBaseURL: " + moduleBaseURL, ex);
            }
        }

        SerializationPolicy serializationPolicy = null;

        /*
         * Check that the module path must be in the same web app as the servlet
         * itself. If you need to implement a scheme different than this,
         * override this method.
         */
        if (modulePath == null || !modulePath.startsWith(contextPath)) {
            String message = "ERROR: The module path requested, " + modulePath
                    + ", is not in the same web application as this servlet, " + contextPath
                    + ".  Your module may not be properly configured or your client and server code maybe out of date.";
            log(message, null);
        } else {
            // Strip off the context path from the module base URL. It should be
            // a
            // strict prefix.
            String contextRelativePath = modulePath.substring(contextPath.length());

            // adding a comment
            // adding a comment2

            String serializationPolicyFilePath = SerializationPolicyLoader
                    .getSerializationPolicyFileName(contextRelativePath + strongName);

            // Open the RPC resource file read its contents.
            InputStream is = getServletContext().getResourceAsStream(serializationPolicyFilePath);
            if (is == null) {
                // try: /www/denali/202D6ADA06C975A44587AEAB102E2B68.gwt.rpc
                String file = "/www" + serializationPolicyFilePath.replace("/admin", "");
                log("Trying " + file);
                is = Thread.currentThread().getContextClassLoader().getResourceAsStream(file);
            }

            try {
                if (is != null) {
                    try {
                        serializationPolicy = SerializationPolicyLoader.loadFromStream(is, null);
                    } catch (ParseException e) {
                        log("ERROR: Failed to parse the policy file '" + serializationPolicyFilePath + "'", e);
                    } catch (IOException e) {
                        log("ERROR: Could not read the policy file '" + serializationPolicyFilePath + "'", e);
                    }
                } else {
                    String message = "ERROR: The serialization policy file '" + serializationPolicyFilePath
                            + "' was not found; did you forget to include it in this deployment?";
                    log(message, null);
                }
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e) {
                        // Ignore this error
                    }
                }
            }
        }
        return serializationPolicy;
    }

    @Override
    public String processCall(final RPCRequest rpcRequest) throws SerializationException {

        final Method method = rpcRequest.getMethod();

        AuditContext.currentOrInternal().getProperties().put("rpc.method", getClass().getSimpleName() + "." + method.getName());

        checkPermissions(rpcRequest);

        final Optional<Audit> methodAudit = Optional.ofNullable(methodAuditSettings.get(method));

        try {
            final String result = super.processCall(rpcRequest);

            if (methodAudit.isPresent()) {
                if (result == null || result.startsWith("//EX")) {
                    auditLogger.warn("{} {} - Failure - {}", AuditContext.currentOrInternal(), methodAudit.get().componentName(),
                            methodAudit.get().description());
                } else {
                    auditLogger.info("{} {} - Success - {}", AuditContext.currentOrInternal(), methodAudit.get().componentName(),
                            methodAudit.get().description());
                }
            }

            return result;

        } catch (final Exception e) {
            if (methodAudit.isPresent()) {
                auditLogger.warn("{} {} - Failure - {}", AuditContext.currentOrInternal(), methodAudit.get().componentName(),
                        methodAudit.get().description());
            }

            throw e;
        }
    }

    private void checkPermissions(final RPCRequest request) {
        final Method method = request.getMethod();

        final Set<String> requiredPermissions;

        if (methodPermissionRequirements.containsKey(method)) {
            requiredPermissions = methodPermissionRequirements.get(method);
        } else {
            requiredPermissions = servicePermissionRequirements;
        }

        if (requiredPermissions.isEmpty()) {
            return;
        }

        final HttpSession session = getThreadLocalRequest().getSession(false);

        final UserManager userManager = Console.instance().getUserManager();

        final Object rawUserName = session.getAttribute(Attributes.AUTORIZED_USER.getValue());

        if (!(rawUserName instanceof String)) {
            throw new KuraPermissionException();
        }

        final String userName = (String) rawUserName;

        final Optional<GwtUserConfig> config = userManager.getUserConfig(userName);

        if (!config.isPresent()) {
            throw new KuraPermissionException();
        }

        if (config.get().isAdmin()) {
            return;
        }

        if (!config.get().getPermissions().containsAll(requiredPermissions)) {
            throw new KuraPermissionException();
        }
    }

    @Override
    protected void doUnexpectedFailure(Throwable e) {
        if (e instanceof KuraPermissionException) {
            try {
                getThreadLocalResponse().sendError(403);
                return;
            } catch (IOException e1) {
                // ignore
            }
        }
        super.doUnexpectedFailure(e);
    }

    private class KuraPermissionException extends RuntimeException {

        private static final long serialVersionUID = 7782509676228955785L;

    }
}

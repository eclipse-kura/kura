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
package org.eclipse.kura.web;

import static java.util.Objects.isNull;
import static org.eclipse.kura.web.session.SecurityHandler.chain;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Stream;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.audit.AuditConstants;
import org.eclipse.kura.audit.AuditContext;
import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.KuraConfigReadyEvent;
import org.eclipse.kura.configuration.SelfConfiguringComponent;
import org.eclipse.kura.crypto.CryptoService;
import org.eclipse.kura.system.SystemService;
import org.eclipse.kura.web.api.ClientExtensionBundle;
import org.eclipse.kura.web.server.GwtCertificatesServiceImpl;
import org.eclipse.kura.web.server.GwtCloudConnectionServiceImpl;
import org.eclipse.kura.web.server.GwtComponentServiceImpl;
import org.eclipse.kura.web.server.GwtDeviceServiceImpl;
import org.eclipse.kura.web.server.GwtDriverAndAssetServiceImpl;
import org.eclipse.kura.web.server.GwtEventServiceImpl;
import org.eclipse.kura.web.server.GwtExtensionServiceImpl;
import org.eclipse.kura.web.server.GwtLoginInfoServiceImpl;
import org.eclipse.kura.web.server.GwtNetworkServiceImpl;
import org.eclipse.kura.web.server.GwtPackageServiceImpl;
import org.eclipse.kura.web.server.GwtPasswordAuthenticationServiceImpl;
import org.eclipse.kura.web.server.GwtSecurityServiceImpl;
import org.eclipse.kura.web.server.GwtSecurityTokenServiceImpl;
import org.eclipse.kura.web.server.GwtSessionServiceImpl;
import org.eclipse.kura.web.server.GwtSnapshotServiceImpl;
import org.eclipse.kura.web.server.GwtStatusServiceImpl;
import org.eclipse.kura.web.server.GwtUserServiceImpl;
import org.eclipse.kura.web.server.GwtWireGraphServiceImpl;
import org.eclipse.kura.web.server.KuraRemoteServiceServlet;
import org.eclipse.kura.web.server.servlet.ChannelServlet;
import org.eclipse.kura.web.server.servlet.DeviceSnapshotsServlet;
import org.eclipse.kura.web.server.servlet.FileServlet;
import org.eclipse.kura.web.server.servlet.LogServlet;
import org.eclipse.kura.web.server.servlet.RedirectServlet;
import org.eclipse.kura.web.server.servlet.SendStatusServlet;
import org.eclipse.kura.web.server.servlet.SkinServlet;
import org.eclipse.kura.web.server.servlet.SslAuthenticationServlet;
import org.eclipse.kura.web.server.servlet.WiresBlinkServlet;
import org.eclipse.kura.web.server.servlet.WiresSnapshotServlet;
import org.eclipse.kura.web.session.Attributes;
import org.eclipse.kura.web.session.BaseSecurityHandler;
import org.eclipse.kura.web.session.CreateSessionSecurityHandler;
import org.eclipse.kura.web.session.HttpContextImpl;
import org.eclipse.kura.web.session.RoutingSecurityHandler;
import org.eclipse.kura.web.session.SecurityHandler;
import org.eclipse.kura.web.session.SessionAutorizationSecurityHandler;
import org.eclipse.kura.web.session.SessionExpirationSecurityHandler;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventProperties;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.osgi.service.useradmin.UserAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Console implements SelfConfiguringComponent, org.eclipse.kura.web.api.Console {

    private static final String EVENT_PATH = "/event";

    public static final String ADMIN_ROOT = "/admin";

    private static final String LOGIN_MODULE_PATH = ADMIN_ROOT + "/login";
    private static final String DENALI_MODULE_PATH = ADMIN_ROOT + "/denali";

    private static final String AUTH_RESOURCE_PATH = ADMIN_ROOT + "/auth.html";
    private static final String CONSOLE_RESOURCE_PATH = ADMIN_ROOT + "/denali.html";

    private static final String AUTH_PATH = ADMIN_ROOT + "/auth";
    private static final String CONSOLE_PATH = ADMIN_ROOT + "/console";

    private static final String PASSWORD_AUTH_PATH = LOGIN_MODULE_PATH + "/password";
    private static final String CERT_AUTH_PATH = LOGIN_MODULE_PATH + "/cert";

    private static final Logger logger = LoggerFactory.getLogger(Console.class);

    private String appRoot;
    private int sessionMaxInactiveInterval;
    private ComponentContext componentContext;

    private HttpService httpService;

    private SystemService systemService;
    private CryptoService cryptoService;

    private UserAdmin userAdmin;

    private EventAdmin eventAdmin;
    private UserManager userManager;
    private GwtEventServiceImpl eventService;
    private WiresBlinkServlet wiresBlinkService;

    private HttpContext sessionContext;

    private final Set<ServletRegistration> securedServlets = new CopyOnWriteArraySet<>();
    private final Set<ServletRegistration> loginServlets = new CopyOnWriteArraySet<>();
    private final Set<ClientExtensionBundle> consoleExtensions = new CopyOnWriteArraySet<>();
    private final Set<ClientExtensionBundle> loginExtensions = new CopyOnWriteArraySet<>();

    private static Console instance;

    private static ConsoleOptions consoleOptions;

    // ----------------------------------------------------------------
    //
    // Dependencies
    //
    // ----------------------------------------------------------------

    public void setHttpService(HttpService httpService) {
        this.httpService = httpService;
    }

    public void setSystemService(SystemService systemService) {
        this.systemService = systemService;
    }

    public void setCryptoService(CryptoService cryptoService) {
        this.cryptoService = cryptoService;
    }

    public void setEventAdminService(EventAdmin eventAdmin) {
        this.eventAdmin = eventAdmin;
    }

    public void setUserAdmin(final UserAdmin userAdmin) {
        this.userAdmin = userAdmin;
    }

    // ----------------------------------------------------------------
    //
    // Activation APIs
    //
    // ----------------------------------------------------------------

    protected void activate(ComponentContext context, Map<String, Object> properties) {

        setInstance(this);
        try {
            setConsoleOptions(properties == null ? ConsoleOptions.defaultConfiguration()
                    : ConsoleOptions.fromProperties(properties));
        } catch (final Exception e) {
            logger.warn("failed to build console options", e);
            return;
        }

        // Check if web interface is enabled.
        boolean webEnabled = Boolean.parseBoolean(this.systemService.getKuraWebEnabled());

        if (!webEnabled) {
            logger.info("Web interface disabled in Kura properties file.");
            return;
        }

        logger.info("activate...");

        setComponentContext(context);
        this.userManager = new UserManager(this.userAdmin, this.cryptoService);

        doUpdate(properties);

        Map<String, Object> props = new HashMap<>();
        props.put("kura.version", this.systemService.getKuraVersion());
        EventProperties eventProps = new EventProperties(props);

        try {
            logger.info("initializing useradmin...");
            this.userManager.update(consoleOptions);
            logger.info("initializing useradmin...done");
        } catch (final Exception e) {
            logger.warn("failed to update UserAdmin", e);
        }

        logger.info("postInstalledEvent() :: posting KuraConfigReadyEvent");

        this.eventAdmin.postEvent(new Event(KuraConfigReadyEvent.KURA_CONFIG_EVENT_READY_TOPIC, eventProps));
    }

    private void setAppRoot(String propertiesAppRoot) {
        this.appRoot = propertiesAppRoot;
    }

    private void setSessionMaxInactiveInterval(int sessionMaxInactiveInterval) {
        this.sessionMaxInactiveInterval = sessionMaxInactiveInterval;
    }

    private void setComponentContext(ComponentContext context) {
        this.componentContext = context;
    }

    protected void updated(Map<String, Object> properties) {
        boolean webEnabled = Boolean.parseBoolean(this.systemService.getKuraWebEnabled());
        if (!webEnabled) {
            return;
        }

        unregisterServlet();
        doUpdate(properties);
    }

    private void doUpdate(Map<String, Object> properties) {
        ConsoleOptions options;
        try {
            options = properties == null ? ConsoleOptions.defaultConfiguration()
                    : ConsoleOptions.fromProperties(properties);
        } catch (final Exception e) {
            logger.warn("failed to build console options", e);
            return;
        }

        Console.setConsoleOptions(options);

        try {
            this.userManager.update(consoleOptions);
        } catch (Exception e) {
            logger.warn("Error Updating Web properties", e);
        }

        setAppRoot(options.getAppRoot());
        setSessionMaxInactiveInterval(options.getSessionMaxInactivityInterval());

        try {
            initHTTPService();
        } catch (NamespaceException | ServletException e) {
            logger.warn("Error Registering Web Resources", e);
        }
    }

    protected void deactivate(BundleContext context) {
        logger.info("deactivate...");

        unregisterServlet();
    }

    // ----------------------------------------------------------------
    //
    // Private methods
    //
    // ----------------------------------------------------------------

    private synchronized void unregisterServlet() {
        this.httpService.unregister("/");
        this.httpService.unregister(ADMIN_ROOT);
        this.httpService.unregister(CONSOLE_PATH);
        this.httpService.unregister(AUTH_PATH);

        this.httpService.unregister(AUTH_RESOURCE_PATH);
        this.httpService.unregister(CONSOLE_RESOURCE_PATH);
        this.httpService.unregister(PASSWORD_AUTH_PATH);
        this.httpService.unregister(CERT_AUTH_PATH);
        this.httpService.unregister(LOGIN_MODULE_PATH + "/loginInfo");
        this.httpService.unregister(DENALI_MODULE_PATH + "/session");
        this.httpService.unregister(DENALI_MODULE_PATH + "/xsrf");
        this.httpService.unregister(DENALI_MODULE_PATH + "/status");
        this.httpService.unregister(DENALI_MODULE_PATH + "/device");
        this.httpService.unregister(DENALI_MODULE_PATH + "/network");
        this.httpService.unregister(DENALI_MODULE_PATH + "/component");
        this.httpService.unregister(DENALI_MODULE_PATH + "/package");
        this.httpService.unregister(DENALI_MODULE_PATH + "/snapshot");
        this.httpService.unregister(DENALI_MODULE_PATH + "/certificate");
        this.httpService.unregister(DENALI_MODULE_PATH + "/security");
        this.httpService.unregister(DENALI_MODULE_PATH + "/users");
        this.httpService.unregister(DENALI_MODULE_PATH + "/file");
        this.httpService.unregister(DENALI_MODULE_PATH + "/device_snapshots");
        this.httpService.unregister(DENALI_MODULE_PATH + "/assetsUpDownload");
        this.httpService.unregister(DENALI_MODULE_PATH + "/log");
        this.httpService.unregister(DENALI_MODULE_PATH + "/skin");
        this.httpService.unregister(DENALI_MODULE_PATH + "/cloudservices");
        this.httpService.unregister(DENALI_MODULE_PATH + "/wires");
        this.httpService.unregister(DENALI_MODULE_PATH + "/wiresSnapshot");
        this.httpService.unregister(DENALI_MODULE_PATH + "/assetservices");
        this.httpService.unregister(DENALI_MODULE_PATH + "/extension");
        this.httpService.unregister(LOGIN_MODULE_PATH + "/extension");
        this.wiresBlinkService.stop();
        this.httpService.unregister(ADMIN_ROOT + "/sse");
        this.eventService.stop();
        this.httpService.unregister(DENALI_MODULE_PATH + EVENT_PATH);

        for (final ServletRegistration reg : this.securedServlets) {
            this.httpService.unregister(reg.path);
        }

        for (final ServletRegistration reg : this.loginServlets) {
            this.httpService.unregister(reg.path);
        }
    }

    public static Console instance() {
        return instance;
    }

    private static void setInstance(final Console instance) {
        Console.instance = instance;
    }

    public static ConsoleOptions getConsoleOptions() {
        return consoleOptions;
    }

    private static void setConsoleOptions(final ConsoleOptions options) {
        Console.consoleOptions = options;
    }

    public BundleContext getBundleContext() {
        return this.componentContext.getBundleContext();
    }

    public String getApplicationRoot() {
        return this.appRoot;
    }

    public HttpSession createSession(final HttpServletRequest request) {
        final HttpSession session = request.getSession();

        session.setMaxInactiveInterval(this.sessionMaxInactiveInterval * 60);
        session.setAttribute(Attributes.LAST_ACTIVITY.getValue(), System.currentTimeMillis());

        return session;
    }

    final Set<String> authenticationPaths = new HashSet<>(Arrays.asList(AUTH_PATH, PASSWORD_AUTH_PATH, CERT_AUTH_PATH));

    private HttpContext initSessionContext(final HttpContext defaultContext) {

        final Set<String> eventPaths = new HashSet<>(Arrays.asList(DENALI_MODULE_PATH + EVENT_PATH, "/sse"));

        final SecurityHandler baseHandler = chain(new BaseSecurityHandler());
        final SecurityHandler sessionAuthHandler = new SessionAutorizationSecurityHandler();
        final SecurityHandler sessionExpirationHandler = new SessionExpirationSecurityHandler();

        // default session handler requires an authenticated session and handles session expiration
        final SecurityHandler defaultHandler = chain(baseHandler, sessionAuthHandler, sessionExpirationHandler);

        final RoutingSecurityHandler routingHandler = new RoutingSecurityHandler(
                defaultHandler.sendErrorOnFailure(401));

        // exception on authentication paths, allow access without authenticaton but create a session
        routingHandler.addRouteHandler(
                p -> this.authenticationPaths.contains(p)
                        || this.loginServlets.stream().anyMatch(r -> r.path.contentEquals(p)),
                chain(baseHandler, new CreateSessionSecurityHandler()));

        // exception on event paths, activity on these paths does not count towards session expiration
        routingHandler.addRouteHandler(eventPaths::contains,
                chain(baseHandler, sessionAuthHandler).sendErrorOnFailure(401));

        // exception on admin console path, redirect to login page on failure instead of sending 401 status
        routingHandler.addRouteHandler(CONSOLE_PATH::equals, defaultHandler.redirectOnFailure(AUTH_PATH));

        return new HttpContextImpl(routingHandler, defaultContext);
    }

    private HttpContext initResourceContext(final HttpContext defaultContext) {
        return new HttpContextImpl(new BaseSecurityHandler(), defaultContext);
    }

    private synchronized void initHTTPService() throws NamespaceException, ServletException {

        this.eventService = new GwtEventServiceImpl();
        this.wiresBlinkService = new WiresBlinkServlet();

        final HttpContext defaultContext = this.httpService.createDefaultHttpContext();
        final HttpContext resourceContext = initResourceContext(defaultContext);
        this.sessionContext = initSessionContext(defaultContext);

        this.httpService.registerResources(ADMIN_ROOT, "www", resourceContext);
        this.httpService.registerResources(AUTH_PATH, "www/auth.html", this.sessionContext);
        this.httpService.registerResources(CONSOLE_PATH, "www/denali.html", this.sessionContext);
        this.httpService.registerServlet(LOGIN_MODULE_PATH + "/loginInfo", new GwtLoginInfoServiceImpl(), null,
                resourceContext);

        this.httpService.registerServlet("/", new RedirectServlet("/"::equals, this.appRoot), null, resourceContext);
        this.httpService.registerServlet(AUTH_RESOURCE_PATH, new SendStatusServlet(404), null, resourceContext);
        this.httpService.registerServlet(CONSOLE_RESOURCE_PATH, new SendStatusServlet(404), null, resourceContext);

        this.httpService.registerServlet(PASSWORD_AUTH_PATH,
                new GwtPasswordAuthenticationServiceImpl(this.userManager, CONSOLE_PATH), null, this.sessionContext);
        this.httpService.registerServlet(CERT_AUTH_PATH, new SslAuthenticationServlet(CONSOLE_PATH, this.userManager),
                null, this.sessionContext);
        this.httpService.registerServlet(DENALI_MODULE_PATH + "/extension", new GwtExtensionServiceImpl(), null,
                resourceContext);
        this.httpService.registerServlet(LOGIN_MODULE_PATH + "/extension", new GwtExtensionServiceImpl(), null,
                resourceContext);
        this.httpService.registerServlet(DENALI_MODULE_PATH + "/session", new GwtSessionServiceImpl(this.userManager),
                null, this.sessionContext);
        this.httpService.registerServlet(DENALI_MODULE_PATH + "/xsrf", new GwtSecurityTokenServiceImpl(), null,
                this.sessionContext);
        this.httpService.registerServlet(DENALI_MODULE_PATH + "/status", new GwtStatusServiceImpl(), null,
                this.sessionContext);
        this.httpService.registerServlet(DENALI_MODULE_PATH + "/device", new GwtDeviceServiceImpl(), null,
                this.sessionContext);
        this.httpService.registerServlet(DENALI_MODULE_PATH + "/network", new GwtNetworkServiceImpl(), null,
                this.sessionContext);
        this.httpService.registerServlet(DENALI_MODULE_PATH + "/component", new GwtComponentServiceImpl(), null,
                this.sessionContext);
        this.httpService.registerServlet(DENALI_MODULE_PATH + "/package", new GwtPackageServiceImpl(), null,
                this.sessionContext);
        this.httpService.registerServlet(DENALI_MODULE_PATH + "/snapshot", new GwtSnapshotServiceImpl(), null,
                this.sessionContext);
        this.httpService.registerServlet(DENALI_MODULE_PATH + "/certificate", new GwtCertificatesServiceImpl(), null,
                this.sessionContext);
        this.httpService.registerServlet(DENALI_MODULE_PATH + "/security", new GwtSecurityServiceImpl(), null,
                this.sessionContext);
        this.httpService.registerServlet(DENALI_MODULE_PATH + "/users", new GwtUserServiceImpl(this.userManager), null,
                this.sessionContext);
        this.httpService.registerServlet(DENALI_MODULE_PATH + "/file", new FileServlet(), null, this.sessionContext);
        this.httpService.registerServlet(DENALI_MODULE_PATH + "/device_snapshots", new DeviceSnapshotsServlet(), null,
                this.sessionContext);
        this.httpService.registerServlet(DENALI_MODULE_PATH + "/assetsUpDownload", new ChannelServlet(), null,
                this.sessionContext);
        this.httpService.registerServlet(DENALI_MODULE_PATH + "/log", new LogServlet(), null, resourceContext);
        this.httpService.registerServlet(DENALI_MODULE_PATH + "/skin", new SkinServlet(), null, resourceContext);
        this.httpService.registerServlet(DENALI_MODULE_PATH + "/cloudservices", new GwtCloudConnectionServiceImpl(),
                null, this.sessionContext);
        this.httpService.registerServlet(DENALI_MODULE_PATH + "/wires", new GwtWireGraphServiceImpl(), null,
                this.sessionContext);
        this.httpService.registerServlet(DENALI_MODULE_PATH + "/wiresSnapshot", new WiresSnapshotServlet(), null,
                this.sessionContext);
        this.httpService.registerServlet(DENALI_MODULE_PATH + "/assetservices", new GwtDriverAndAssetServiceImpl(),
                null, this.sessionContext);
        this.httpService.registerServlet(ADMIN_ROOT + "/sse", this.wiresBlinkService, null, this.sessionContext);
        this.httpService.registerServlet(DENALI_MODULE_PATH + EVENT_PATH, this.eventService, null, this.sessionContext);

        for (final ServletRegistration reg : this.securedServlets) {
            this.httpService.registerServlet(reg.path, reg.servlet, null, this.sessionContext);
        }

        for (final ServletRegistration reg : this.loginServlets) {
            this.httpService.registerServlet(reg.path, reg.servlet, null, this.sessionContext);
        }

        this.eventService.start();
    }

    public Set<ClientExtensionBundle> getConsoleExtensions() {
        return this.consoleExtensions;
    }

    public Set<ClientExtensionBundle> getLoginExtensions() {
        return this.loginExtensions;
    }

    public Set<String> getBuiltinAuthenticationMethods() {
        return new HashSet<>(Arrays.asList("Certificate", "Password"));
    }

    public Set<String> getAuthenticationMethods() {
        final Set<String> result = new LinkedHashSet<>();

        result.add("Password");
        result.add("Certificate");

        Stream.concat(this.loginExtensions.stream(), this.consoleExtensions.stream()).forEach(b -> {
            for (final ClientExtensionBundle bundle : this.loginExtensions) {
                final Set<String> providedMethods = bundle.getProvidedAuthenticationMethods();

                if (providedMethods == null) {
                    continue;
                }

                result.addAll(providedMethods);
            }
        });

        return result;
    }

    private static final class ServletRegistration {

        private final String path;
        private final Servlet servlet;

        public ServletRegistration(String path, Servlet servlet) {
            this.path = path;
            this.servlet = servlet;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (this.path == null ? 0 : this.path.hashCode());
            result = prime * result + (this.servlet == null ? 0 : this.servlet.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            ServletRegistration other = (ServletRegistration) obj;
            if (this.path == null) {
                if (other.path != null) {
                    return false;
                }
            } else if (!this.path.equals(other.path)) {
                return false;
            }
            return this.servlet == other.servlet;
        }

    }

    private void refreshOptions() {
        try {
            setConsoleOptions(
                    ConsoleOptions.fromProperties(getConsoleOptions().getConfiguration().getConfigurationProperties()));
        } catch (final Exception e) {
            logger.warn("Failed to update options", e);
        }
    }

    public UserManager getUserManager() {
        return this.userManager;
    }

    @Override
    public void registerConsoleExtensionBundle(ClientExtensionBundle extension) {
        this.consoleExtensions.add(extension);
        refreshOptions();
    }

    @Override
    public void unregisterConsoleExtensionBundle(ClientExtensionBundle extension) {
        this.consoleExtensions.remove(extension);
        refreshOptions();
    }

    @Override
    public void registerLoginExtensionBundle(ClientExtensionBundle extension) {
        this.loginExtensions.add(extension);
        refreshOptions();
    }

    @Override
    public void unregisterLoginExtensionBundle(ClientExtensionBundle extension) {
        this.loginExtensions.remove(extension);
        refreshOptions();
    }

    @Override
    public synchronized void registerSecuredServlet(final String path, final Servlet servlet)
            throws NamespaceException, ServletException {
        this.securedServlets.add(new ServletRegistration(path, servlet));
        this.httpService.registerServlet(path, servlet, null, this.sessionContext);
    }

    @Override
    public synchronized void registerLoginServlet(final String path, final Servlet servlet)
            throws NamespaceException, ServletException {
        this.loginServlets.add(new ServletRegistration(path, servlet));
        this.httpService.registerServlet(path, servlet, null, this.sessionContext);
        this.authenticationPaths.add(path);
    }

    @Override
    public synchronized void unregisterServlet(final String path) throws NamespaceException, ServletException {
        this.securedServlets.removeIf(r -> r.path.contentEquals(path));
        this.loginServlets.removeIf(r -> r.path.contentEquals(path));
        this.httpService.unregister(path);
        this.authenticationPaths.remove(path);
    }

    @Override
    public String setAuthenticated(final HttpSession session, final String user, final AuditContext context) {
        session.setAttribute(Attributes.AUTORIZED_USER.getValue(), user);

        context.getProperties().put(AuditConstants.KEY_IDENTITY.getValue(), user);
        session.setAttribute(Attributes.AUDIT_CONTEXT.getValue(), context);
        session.setAttribute(Attributes.CREDENTIALS_HASH.getValue(), this.userManager.getCredentialsHash(user));

        return CONSOLE_PATH;
    }

    @Override
    public AuditContext initAuditContext(final HttpServletRequest req) {
        final HttpSession session = req.getSession(false);

        String requestIp = req.getHeader("X-FORWARDED-FOR");
        if (isNull(requestIp)) {
            requestIp = req.getRemoteAddr();
        }

        final Object rawAuditContext = session != null ? session.getAttribute(Attributes.AUDIT_CONTEXT.getValue())
                : null;

        final AuditContext auditContext;

        if (rawAuditContext instanceof AuditContext) {
            auditContext = ((AuditContext) rawAuditContext).copy();
            auditContext.getProperties().remove("rpc.method");
            auditContext.getProperties().put(AuditConstants.KEY_IP.getValue(), requestIp);
        } else {
            final Map<String, String> properties = new HashMap<>();
            properties.put(AuditConstants.KEY_IP.getValue(), requestIp);
            properties.put(AuditConstants.KEY_ENTRY_POINT.getValue(), "WebConsole");
            auditContext = new AuditContext(properties);
        }

        auditContext.getProperties().put("web.path", req.getRequestURI());

        return auditContext;
    }

    @Override
    public void checkXSRFToken(final HttpServletRequest req, final String token) throws KuraException {
        if (!KuraRemoteServiceServlet.isValidXSRFToken(req, token)) {
            throw new KuraException(KuraErrorCode.SECURITY_EXCEPTION);
        }
    }

    @Override
    public Optional<String> getUsername(HttpSession session) {
        return Optional.ofNullable(session.getAttribute(Attributes.AUTORIZED_USER.getValue())).map(String.class::cast);
    }

    @Override
    public ComponentConfiguration getConfiguration() throws KuraException {
        return consoleOptions.getConfiguration();
    }
}

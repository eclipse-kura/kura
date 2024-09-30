/*******************************************************************************
 * Copyright (c) 2011, 2024 Eurotech and/or its affiliates and others
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
import java.util.Hashtable;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.audit.AuditConstants;
import org.eclipse.kura.audit.AuditContext;
import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.KuraConfigReadyEvent;
import org.eclipse.kura.configuration.SelfConfiguringComponent;
import org.eclipse.kura.identity.IdentityService;
import org.eclipse.kura.ssl.SslManagerService;
import org.eclipse.kura.system.SystemService;
import org.eclipse.kura.web.server.GwtCertificatesServiceImpl;
import org.eclipse.kura.web.server.GwtCloudConnectionServiceImpl;
import org.eclipse.kura.web.server.GwtComponentServiceImpl;
import org.eclipse.kura.web.server.GwtDeviceServiceImpl;
import org.eclipse.kura.web.server.GwtDriverAndAssetServiceImpl;
import org.eclipse.kura.web.server.GwtEventServiceImpl;
import org.eclipse.kura.web.server.GwtKeystoreServiceImpl;
import org.eclipse.kura.web.server.GwtLogServiceImpl;
import org.eclipse.kura.web.server.GwtLoginInfoServiceImpl;
import org.eclipse.kura.web.server.GwtNetworkServiceImpl;
import org.eclipse.kura.web.server.GwtPackageServiceImpl;
import org.eclipse.kura.web.server.GwtPasswordAuthenticationServiceImpl;
import org.eclipse.kura.web.server.GwtSecurityServiceImpl;
import org.eclipse.kura.web.server.GwtSecurityTokenServiceImpl;
import org.eclipse.kura.web.server.GwtSessionServiceImpl;
import org.eclipse.kura.web.server.GwtSnapshotServiceImpl;
import org.eclipse.kura.web.server.GwtSslManagerServiceImpl;
import org.eclipse.kura.web.server.GwtStatusServiceImpl;
import org.eclipse.kura.web.server.GwtUserServiceImpl;
import org.eclipse.kura.web.server.GwtWireGraphServiceImpl;
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
import org.eclipse.kura.web.server.util.GwtServerUtil;
import org.eclipse.kura.web.session.Attributes;
import org.eclipse.kura.web.session.BaseSecurityHandler;
import org.eclipse.kura.web.session.CreateSessionSecurityHandler;
import org.eclipse.kura.web.session.HttpServletContextHelper;
import org.eclipse.kura.web.session.RoutingSecurityHandler;
import org.eclipse.kura.web.session.SecurityHandler;
import org.eclipse.kura.web.session.SessionAutorizationSecurityHandler;
import org.eclipse.kura.web.session.SessionExpirationSecurityHandler;
import org.eclipse.kura.web.session.SessionLockedSecurityHandler;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventProperties;
import org.osgi.service.http.NamespaceException;
import org.osgi.service.servlet.context.ServletContextHelper;
import org.osgi.service.servlet.whiteboard.HttpWhiteboardConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

public class Console implements SelfConfiguringComponent {

    private static final String SESSION_CONTEX_NAME = "sessionContex";

    private static final String RESOURCE_CONTEX_NAME = "resourceContex";

    private static final String SESSION = "/session";

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

    private SystemService systemService;
    private final AtomicReference<Optional<SslManagerService>> sslManagerService = new AtomicReference<>(
            Optional.empty());

    private IdentityService identityService;

    private EventAdmin eventAdmin;
    private UserManager userManager;
    private GwtEventServiceImpl eventService;
    private WiresBlinkServlet wiresBlinkService;

    private final Set<ServiceRegistration<ServletContextHelper>> contexts = new CopyOnWriteArraySet<>();
    private final Set<ServiceRegistration<ResourcesService>> resources = new CopyOnWriteArraySet<>();
    private final Set<ServiceRegistration<HttpServlet>> servlets = new CopyOnWriteArraySet<>();

    private BundleContext bundleContext;

    private static Console instance;

    private static ConsoleOptions consoleOptions;

    // ----------------------------------------------------------------
    //
    // Dependencies
    //
    // ----------------------------------------------------------------

    public void setSslManagerService(SslManagerService sslManagerService) {
        this.sslManagerService.set(Optional.of(sslManagerService));
    }

    public void unsetSslManagerService(SslManagerService sslManagerService) {

        this.sslManagerService.updateAndGet(s -> {
            final Optional<SslManagerService> service = Optional.ofNullable(sslManagerService);

            if (Objects.equals(s, service)) {
                return Optional.empty();
            } else {
                return s;
            }
        });
    }

    public void setSystemService(SystemService systemService) {
        this.systemService = systemService;
    }

    public void setEventAdminService(EventAdmin eventAdmin) {
        this.eventAdmin = eventAdmin;
    }

    public void setIdentityService(final IdentityService identityService) {
        this.identityService = identityService;
    }

    // ----------------------------------------------------------------
    //
    // Activation APIs
    //
    // ----------------------------------------------------------------

    protected void activate(ComponentContext context, Map<String, Object> properties) {

        this.bundleContext = context.getBundleContext();

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
        this.userManager = new UserManager(this.identityService);

        doUpdate(getConsoleOptions());

        Map<String, Object> props = new HashMap<>();
        props.put("kura.version", this.systemService.getKuraVersion());
        EventProperties eventProps = new EventProperties(props);

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

        ConsoleOptions newOptions;
        try {
            newOptions = properties == null ? ConsoleOptions.defaultConfiguration()
                    : ConsoleOptions.fromProperties(properties);
        } catch (final Exception e) {
            logger.warn("failed to build console options", e);
            return;
        }

        if (!newOptions.equals(Console.getConsoleOptions())) {
            logger.info("Console options changed, reconfiguring...");
            Console.setConsoleOptions(newOptions);
            unregisterAll();
            doUpdate(newOptions);
        }
    }

    private void doUpdate(ConsoleOptions options) {

        try {
            this.userManager.update();
        } catch (Exception e) {
            logger.warn("Error Updating Web properties", e);
        }

        setAppRoot(options.getAppRoot());
        setSessionMaxInactiveInterval(options.getSessionMaxInactivityInterval());

        try {
            initHTTPService();
        } catch (NamespaceException e) {
            logger.warn("Error Registering Web Resources", e);
        }
    }

    protected void deactivate(BundleContext context) {
        logger.info("deactivate...");

        unregisterAll();
    }

    // ----------------------------------------------------------------
    //
    // Private methods
    //
    // ----------------------------------------------------------------

    private synchronized void unregisterAll() {

        this.contexts.forEach(ServiceRegistration::unregister);
        this.resources.forEach(ServiceRegistration::unregister);
        this.servlets.forEach(ServiceRegistration::unregister);

        this.wiresBlinkService.stop();

        this.eventService.stop();

    }

    public String setAuthenticated(final HttpSession session, final String user, final AuditContext context) {
        session.setAttribute(Attributes.AUTORIZED_USER.getValue(), user);

        context.getProperties().put(AuditConstants.KEY_IDENTITY.getValue(), user);
        session.setAttribute(Attributes.AUDIT_CONTEXT.getValue(), context);
        try {
            session.setAttribute(Attributes.CREDENTIALS_HASH.getValue(), this.userManager.getCredentialsHash(user));
        } catch (Exception e) {
            logger.warn("failed to compute credentials hash", e);
        }

        return CONSOLE_PATH;
    }

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

    public EventAdmin getEventAdmin() {
        return this.eventAdmin;
    }

    public HttpSession createNewSession(final HttpServletRequest request) {
        final HttpSession existingSession = request.getSession(false);

        if (existingSession != null) {
            existingSession.invalidate();
        }

        final HttpSession newSession = createSession(request);
        request.changeSessionId();

        updateAuditContext(newSession);

        return newSession;
    }

    public HttpSession createSession(final HttpServletRequest request) {
        final HttpSession session = request.getSession();

        session.setMaxInactiveInterval(this.sessionMaxInactiveInterval * 60);
        session.setAttribute(Attributes.LAST_ACTIVITY.getValue(), System.currentTimeMillis());

        updateAuditContext(session);

        return session;
    }

    private void updateAuditContext(final HttpSession session) {
        final String id = GwtServerUtil.getSessionIdHash(session);

        AuditContext.currentOrInternal().getProperties().put("session.id", id);

        final Object sessionAuditContext = session.getAttribute(Attributes.AUDIT_CONTEXT.getValue());

        if (sessionAuditContext instanceof AuditContext) {
            ((AuditContext) sessionAuditContext).getProperties().put("session.id", id);
        }

    }

    final Set<String> authenticationPaths = new HashSet<>(Arrays.asList(AUTH_PATH, PASSWORD_AUTH_PATH, CERT_AUTH_PATH));

    private SecurityHandler createSessionHandlerChain() {

        final Set<String> eventPaths = new HashSet<>(Arrays.asList(DENALI_MODULE_PATH + EVENT_PATH, "/sse"));

        final SecurityHandler baseHandler = chain(new BaseSecurityHandler());
        final SecurityHandler sessionAuthHandler = new SessionAutorizationSecurityHandler();
        final SecurityHandler sessionExpirationHandler = new SessionExpirationSecurityHandler();
        final SecurityHandler sessionLockedSecurityHandler = new SessionLockedSecurityHandler();

        // default session handler requires an authenticated session and handles session
        // expiration, handles session
        // lock
        final SecurityHandler defaultHandler = chain(baseHandler, sessionAuthHandler, sessionLockedSecurityHandler,
                sessionExpirationHandler);

        final RoutingSecurityHandler routingHandler = new RoutingSecurityHandler(
                defaultHandler.sendErrorOnFailure(401));

        // exception on authentication paths, allow access without authenticaton but
        // create a session
        routingHandler.addRouteHandler(this.authenticationPaths::contains,
                chain(baseHandler, new CreateSessionSecurityHandler()));

        // exception on event paths, activity on these paths does not count towards
        // session expiration
        routingHandler.addRouteHandler(eventPaths::contains,
                chain(baseHandler, sessionAuthHandler).sendErrorOnFailure(401));

        // exception on admin console path, redirect to login page on failure instead of
        // sending 401 status
        routingHandler.addRouteHandler(CONSOLE_PATH::equals, defaultHandler.redirectOnFailure(AUTH_PATH));

        // exception on login session and xsrf path, like default but without locked
        // session checking
        routingHandler.addRouteHandler(
                Arrays.asList(LOGIN_MODULE_PATH + SESSION, LOGIN_MODULE_PATH + "/xsrf")::contains,
                chain(baseHandler, sessionAuthHandler, sessionExpirationHandler));

        return routingHandler;
    }

    private synchronized void initHTTPService() throws NamespaceException {

        this.eventService = new GwtEventServiceImpl();
        this.wiresBlinkService = new WiresBlinkServlet();

        ServletContextHelper resourceContextHelper = new HttpServletContextHelper(new BaseSecurityHandler());
        ServletContextHelper sessionContextHelper = new HttpServletContextHelper(createSessionHandlerChain());

        registerContextHelper(RESOURCE_CONTEX_NAME, "/", resourceContextHelper);
        registerContextHelper(SESSION_CONTEX_NAME, "/", sessionContextHelper);

        registerResources(ADMIN_ROOT, "www", new AdminResources(), RESOURCE_CONTEX_NAME);
        registerResources(AUTH_PATH, "www/auth.html", new AuthorizationResources(), SESSION_CONTEX_NAME);
        registerResources(CONSOLE_PATH, "www/denali.html", new ConsoleResources(), SESSION_CONTEX_NAME);

        registerServlet("gwtLoginInfoService", LOGIN_MODULE_PATH + "/loginInfo", new GwtLoginInfoServiceImpl(),
                RESOURCE_CONTEX_NAME);
        registerServlet("redirectServlet", "/", new RedirectServlet("/"::equals, this.appRoot), RESOURCE_CONTEX_NAME);

        registerServlet("notFoundAuthResourceServlet", AUTH_RESOURCE_PATH, new SendStatusServlet(404),
                RESOURCE_CONTEX_NAME);
        registerServlet("notFoundConsoleResourceServlet", CONSOLE_RESOURCE_PATH, new SendStatusServlet(404),
                RESOURCE_CONTEX_NAME);

        registerServlet("gwtPasswordAuthenticationService", PASSWORD_AUTH_PATH,
                new GwtPasswordAuthenticationServiceImpl(this.userManager, CONSOLE_PATH), SESSION_CONTEX_NAME);
        registerServlet("sslAuthenticationServlet", CERT_AUTH_PATH,
                new SslAuthenticationServlet(CONSOLE_PATH, this.userManager), SESSION_CONTEX_NAME);

        registerServlet("gwtKeystoreServiceImpl", DENALI_MODULE_PATH + "/keystore", new GwtKeystoreServiceImpl(),
                SESSION_CONTEX_NAME);
        registerServlet("gwtSslManagerServiceImpl", DENALI_MODULE_PATH + "/ssl", new GwtSslManagerServiceImpl(),
                SESSION_CONTEX_NAME);

        registerServlet("denaliSessionService", DENALI_MODULE_PATH + SESSION,
                new GwtSessionServiceImpl(this.userManager), SESSION_CONTEX_NAME);

        registerServlet("loginSessionService", LOGIN_MODULE_PATH + SESSION, new GwtSessionServiceImpl(this.userManager),
                SESSION_CONTEX_NAME);
        registerServlet("xsrfLoginServlet", LOGIN_MODULE_PATH + "/xsrf", new GwtSecurityTokenServiceImpl(),
                SESSION_CONTEX_NAME);
        registerServlet("xsrfDenaliServlet", DENALI_MODULE_PATH + "/xsrf", new GwtSecurityTokenServiceImpl(),
                SESSION_CONTEX_NAME);
        registerServlet("statusService", DENALI_MODULE_PATH + "/status", new GwtStatusServiceImpl(),
                SESSION_CONTEX_NAME);
        registerServlet("deviceService", DENALI_MODULE_PATH + "/device", new GwtDeviceServiceImpl(),
                SESSION_CONTEX_NAME);
        registerServlet("logService", DENALI_MODULE_PATH + "/logservice", new GwtLogServiceImpl(), SESSION_CONTEX_NAME);
        registerServlet("networkService", DENALI_MODULE_PATH + "/network", new GwtNetworkServiceImpl(),
                SESSION_CONTEX_NAME);
        registerServlet("componentService", DENALI_MODULE_PATH + "/component", new GwtComponentServiceImpl(),
                SESSION_CONTEX_NAME);
        registerServlet("packageService", DENALI_MODULE_PATH + "/package",
                new GwtPackageServiceImpl(this.sslManagerService::get), SESSION_CONTEX_NAME);
        registerServlet("snapshotServiceImpl", DENALI_MODULE_PATH + "/snapshot", new GwtSnapshotServiceImpl(),
                SESSION_CONTEX_NAME);
        registerServlet("certificateService", DENALI_MODULE_PATH + "/certificate", new GwtCertificatesServiceImpl(),
                SESSION_CONTEX_NAME);
        registerServlet("securityService", DENALI_MODULE_PATH + "/security", new GwtSecurityServiceImpl(),
                SESSION_CONTEX_NAME);
        registerServlet("usersService", DENALI_MODULE_PATH + "/users", new GwtUserServiceImpl(this.userManager),
                SESSION_CONTEX_NAME);
        registerServlet("fileServlet", DENALI_MODULE_PATH + "/file", new FileServlet(), SESSION_CONTEX_NAME);
        registerServlet("deviceSnapshotsServlet", DENALI_MODULE_PATH + "/device_snapshots",
                new DeviceSnapshotsServlet(), SESSION_CONTEX_NAME);
        registerServlet("channelServlet", DENALI_MODULE_PATH + "/assetsUpDownload", new ChannelServlet(),
                SESSION_CONTEX_NAME);
        registerServlet("logServlet", DENALI_MODULE_PATH + "/log", new LogServlet(), SESSION_CONTEX_NAME);
        registerServlet("skinServlet", DENALI_MODULE_PATH + "/skin", new SkinServlet(), RESOURCE_CONTEX_NAME);
        registerServlet("cloudServices", DENALI_MODULE_PATH + "/cloudservices", new GwtCloudConnectionServiceImpl(),
                SESSION_CONTEX_NAME);
        registerServlet("wireGraphService", DENALI_MODULE_PATH + "/wires", new GwtWireGraphServiceImpl(),
                SESSION_CONTEX_NAME);
        registerServlet("wiresSnapshotServlet", DENALI_MODULE_PATH + "/wiresSnapshot", new WiresSnapshotServlet(),
                SESSION_CONTEX_NAME);
        registerServlet("driverAndAssetService", DENALI_MODULE_PATH + "/assetservices",
                new GwtDriverAndAssetServiceImpl(), SESSION_CONTEX_NAME);
        registerServlet("wiresBlinkService", ADMIN_ROOT + "/sse", this.wiresBlinkService, SESSION_CONTEX_NAME);
        registerServlet("eventService", DENALI_MODULE_PATH + EVENT_PATH, this.eventService, SESSION_CONTEX_NAME);

        this.eventService.start();
    }

    private void registerContextHelper(String contextName, String contextPath, ServletContextHelper contextHelper) {
        Map<String, String> props = new HashMap<>();

        props.put(Constants.SERVICE_ID, contextName);

        props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, contextName);
        props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, contextPath);

        ServiceRegistration<ServletContextHelper> contextService = this.bundleContext
                .registerService(ServletContextHelper.class, contextHelper, new Hashtable<>(props));

        this.contexts.add(contextService);
    }

    private void registerResources(String pattern, String prefix, ResourcesService resourcesService,
            String contextHelperName) {
        Map<String, String> props = new HashMap<>();

        props.put(Constants.SERVICE_ID, resourcesService.getClass().getSimpleName());

        props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_RESOURCE_PATTERN, pattern);
        props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_RESOURCE_PREFIX, prefix);
        props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT, contextHelperName);

        ServiceRegistration<ResourcesService> resourcesS = this.bundleContext.registerService(ResourcesService.class,
                resourcesService, new Hashtable<>(props));

        this.resources.add(resourcesS);
    }

    private void registerServlet(String servletName, String servletPattern, HttpServlet servlet,
            String contextHelperName) {

        Map<String, String> props = new HashMap<>();

        props.put(Constants.SERVICE_SCOPE, Constants.SCOPE_PROTOTYPE);
        props.put(Constants.SERVICE_ID, servletName);

        props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, servletName);
        props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, servletPattern);
        props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT, contextHelperName);

        ServiceRegistration<HttpServlet> servletService = this.bundleContext.registerService(HttpServlet.class, servlet,
                new Hashtable<>(props));

        this.servlets.add(servletService);
    }

    public interface ResourcesService {
    }

    public class AdminResources implements ResourcesService {
    }

    public class AuthorizationResources implements ResourcesService {
    }

    public class ConsoleResources implements ResourcesService {
    }

    public Set<String> getBuiltinAuthenticationMethods() {
        return new HashSet<>(Arrays.asList("Certificate", "Password"));
    }

    public Set<String> getAuthenticationMethods() {
        return new LinkedHashSet<>(Arrays.asList("Password", "Certificate"));
    }

    public UserManager getUserManager() {
        return this.userManager;
    }

    @Override
    public ComponentConfiguration getConfiguration() throws KuraException {
        return consoleOptions.getConfiguration();
    }
}

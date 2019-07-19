/*******************************************************************************
 * Copyright (c) 2011, 2019 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.web;

import static org.eclipse.kura.web.session.SecurityHandler.chain;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.configuration.KuraConfigReadyEvent;
import org.eclipse.kura.crypto.CryptoService;
import org.eclipse.kura.system.SystemService;
import org.eclipse.kura.web.server.GwtAssetServiceImpl;
import org.eclipse.kura.web.server.GwtBannerServiceImpl;
import org.eclipse.kura.web.server.GwtCertificatesServiceImpl;
import org.eclipse.kura.web.server.GwtCloudConnectionServiceImpl;
import org.eclipse.kura.web.server.GwtComponentServiceImpl;
import org.eclipse.kura.web.server.GwtDeviceServiceImpl;
import org.eclipse.kura.web.server.GwtEventServiceImpl;
import org.eclipse.kura.web.server.GwtNetworkServiceImpl;
import org.eclipse.kura.web.server.GwtPackageServiceImpl;
import org.eclipse.kura.web.server.GwtPasswordAuthenticationServiceImpl;
import org.eclipse.kura.web.server.GwtSecurityServiceImpl;
import org.eclipse.kura.web.server.GwtSecurityTokenServiceImpl;
import org.eclipse.kura.web.server.GwtSessionServiceImpl;
import org.eclipse.kura.web.server.GwtSnapshotServiceImpl;
import org.eclipse.kura.web.server.GwtSslServiceImpl;
import org.eclipse.kura.web.server.GwtStatusServiceImpl;
import org.eclipse.kura.web.server.GwtWireGraphServiceImpl;
import org.eclipse.kura.web.server.servlet.ChannelServlet;
import org.eclipse.kura.web.server.servlet.DeviceSnapshotsServlet;
import org.eclipse.kura.web.server.servlet.FileServlet;
import org.eclipse.kura.web.server.servlet.RedirectServlet;
import org.eclipse.kura.web.server.servlet.SendStatusServlet;
import org.eclipse.kura.web.server.servlet.SkinServlet;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Console implements ConfigurableComponent {

    private static final String EVENT_PATH = "/event";

    public static final String ADMIN_ROOT = "/admin";

    private static final String LOGIN_MODULE_PATH = ADMIN_ROOT + "/login";
    private static final String DENALI_MODULE_PATH = ADMIN_ROOT + "/denali";

    private static final String AUTH_RESOURCE_PATH = ADMIN_ROOT + "/auth.html";
    private static final String CONSOLE_RESOURCE_PATH = ADMIN_ROOT + "/denali.html";

    private static final String AUTH_PATH = ADMIN_ROOT + "/auth";
    private static final String CONSOLE_PATH = ADMIN_ROOT + "/console";

    private static final String PASSWORD_AUTH_PATH = LOGIN_MODULE_PATH + "/password";

    private static final Logger logger = LoggerFactory.getLogger(Console.class);

    private HttpContext resourceContext;

    private String appRoot;
    private int sessionMaxInactiveInterval;
    private ComponentContext componentContext;

    private HttpService httpService;

    private SystemService systemService;
    private CryptoService cryptoService;

    private EventAdmin eventAdmin;
    private AuthenticationManager authMgr;
    private GwtEventServiceImpl eventService;

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

    public void unsetHttpService(HttpService httpService) {
        this.httpService = null;
    }

    public void setSystemService(SystemService systemService) {
        this.systemService = systemService;
    }

    public void unsetSystemService(SystemService systemService) {
        this.systemService = null;
    }

    public void setCryptoService(CryptoService cryptoService) {
        this.cryptoService = cryptoService;
    }

    public void unsetCryptoService(CryptoService cryptoService) {
        this.cryptoService = null;
    }

    public void setEventAdminService(EventAdmin eventAdmin) {
        this.eventAdmin = eventAdmin;
    }

    public void unsetEventAdminService(EventAdmin eventAdmin) {
        this.eventAdmin = null;
    }

    // ----------------------------------------------------------------
    //
    // Activation APIs
    //
    // ----------------------------------------------------------------

    protected void activate(ComponentContext context, Map<String, Object> properties) {
        setInstance(this);
        // Check if web interface is enabled.
        boolean webEnabled = Boolean.parseBoolean(this.systemService.getKuraWebEnabled());

        if (!webEnabled) {
            logger.info("Web interface disabled in Kura properties file.");
            return;
        }

        logger.info("activate...");

        setComponentContext(context);
        this.authMgr = AuthenticationManager.getInstance();
        this.eventService = new GwtEventServiceImpl();

        doUpdate(properties);

        Map<String, Object> props = new HashMap<>();
        props.put("kura.version", this.systemService.getKuraVersion());
        EventProperties eventProps = new EventProperties(props);
        logger.info("postInstalledEvent() :: posting KuraConfigReadyEvent");
        this.eventAdmin.postEvent(new Event(KuraConfigReadyEvent.KURA_CONFIG_EVENT_READY_TOPIC, eventProps));
    }

    private void updateAuthenticationManager(String username, String password)
            throws KuraException, NoSuchAlgorithmException, UnsupportedEncodingException {

        char[] decryptedPassword = this.cryptoService.decryptAes(password.toCharArray());
        char[] propertyPassword = this.cryptoService.sha1Hash(new String(decryptedPassword)).toCharArray();

        this.authMgr.setUsername(username);
        this.authMgr.setPassword(propertyPassword);
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

    protected void updated(Map<String, Object> properties) throws ServletException, NamespaceException {
        boolean webEnabled = Boolean.parseBoolean(this.systemService.getKuraWebEnabled());
        if (!webEnabled) {
            return;
        }

        ConsoleOptions options = new ConsoleOptions(properties);

        Console.setConsoleOptions(options);

        try {
            updateAuthenticationManager(options.getUsername(), options.getUserPassword());
        } catch (Exception e) {
            logger.warn("Error Updating Web properties", e);
        }

        setAppRoot(options.getAppRoot());
        setSessionMaxInactiveInterval(options.getSessionMaxInactivityInterval());
        this.httpService.unregister("/");
        this.httpService.registerServlet("/", new RedirectServlet("/"::equals, this.appRoot), null,
                this.resourceContext);

    }

    private void doUpdate(Map<String, Object> properties) {
        ConsoleOptions options = new ConsoleOptions(properties);

        Console.setConsoleOptions(options);

        try {
            updateAuthenticationManager(options.getUsername(), options.getUserPassword());
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

    private void unregisterServlet() {
        this.httpService.unregister("/");
        this.httpService.unregister(ADMIN_ROOT);
        this.httpService.unregister(CONSOLE_PATH);
        this.httpService.unregister(AUTH_PATH);

        this.httpService.unregister(AUTH_RESOURCE_PATH);
        this.httpService.unregister(CONSOLE_RESOURCE_PATH);
        this.httpService.unregister(PASSWORD_AUTH_PATH);
        this.httpService.unregister(LOGIN_MODULE_PATH + "/banner");
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
        this.httpService.unregister(DENALI_MODULE_PATH + "/file");
        this.httpService.unregister(DENALI_MODULE_PATH + "/device_snapshots");
        this.httpService.unregister(DENALI_MODULE_PATH + "/assetsUpDownload");
        this.httpService.unregister(DENALI_MODULE_PATH + "/skin");
        this.httpService.unregister(DENALI_MODULE_PATH + "/ssl");
        this.httpService.unregister(DENALI_MODULE_PATH + "/cloudservices");
        this.httpService.unregister(DENALI_MODULE_PATH + "/wires");
        this.httpService.unregister(DENALI_MODULE_PATH + "/wiresSnapshot");
        this.httpService.unregister(DENALI_MODULE_PATH + "/assetservices");
        this.httpService.unregister(ADMIN_ROOT + "/sse");
        this.eventService.stop();
        this.httpService.unregister(DENALI_MODULE_PATH + EVENT_PATH);
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

    public HttpSession createSession(final HttpServletRequest request, final HttpServletResponse response) {
        final HttpSession session = request.getSession();

        session.setMaxInactiveInterval(this.sessionMaxInactiveInterval * 60);
        session.setAttribute(Attributes.LAST_ACTIVITY.getValue(), System.currentTimeMillis());

        return session;
    }

    private HttpContext initSessionContext(final HttpContext defaultContext) {

        final Set<String> authenticationPaths = new HashSet<>(Arrays.asList(AUTH_PATH, PASSWORD_AUTH_PATH));
        final Set<String> eventPaths = new HashSet<>(Arrays.asList(DENALI_MODULE_PATH + EVENT_PATH, "/sse"));

        final SecurityHandler baseHandler = new BaseSecurityHandler();
        final SecurityHandler sessionAuthHandler = new SessionAutorizationSecurityHandler();
        final SecurityHandler sessionExpirationHandler = new SessionExpirationSecurityHandler();

        // default session handler requires an authenticated session and handles session expiration
        final SecurityHandler defaultHandler = chain(baseHandler, sessionAuthHandler, sessionExpirationHandler);

        final RoutingSecurityHandler routingHandler = new RoutingSecurityHandler(
                defaultHandler.sendErrorOnFailure(401));

        // exception on authentication paths, allow access without authenticaton but create a session
        routingHandler.addRouteHandler(authenticationPaths::contains,
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

    private void initHTTPService() throws NamespaceException, ServletException {

        final HttpContext defaultContext = this.httpService.createDefaultHttpContext();

        this.resourceContext = initResourceContext(defaultContext);
        final HttpContext sessionContext = initSessionContext(defaultContext);

        this.httpService.registerResources(ADMIN_ROOT, "www", resourceContext);
        this.httpService.registerResources(AUTH_PATH, "www/auth.html", sessionContext);
        this.httpService.registerResources(CONSOLE_PATH, "www/denali.html", sessionContext);
        this.httpService.registerServlet(LOGIN_MODULE_PATH + "/banner", new GwtBannerServiceImpl(), null,
                resourceContext);

        this.httpService.registerServlet("/", new RedirectServlet("/"::equals, this.appRoot), null, resourceContext);
        this.httpService.registerServlet(AUTH_RESOURCE_PATH, new SendStatusServlet(404), null, resourceContext);
        this.httpService.registerServlet(CONSOLE_RESOURCE_PATH, new SendStatusServlet(404), null, resourceContext);

        this.httpService.registerServlet(PASSWORD_AUTH_PATH,
                new GwtPasswordAuthenticationServiceImpl(this.authMgr, CONSOLE_PATH), null, sessionContext);
        this.httpService.registerServlet(DENALI_MODULE_PATH + "/session", new GwtSessionServiceImpl(), null,
                sessionContext);
        this.httpService.registerServlet(DENALI_MODULE_PATH + "/xsrf", new GwtSecurityTokenServiceImpl(), null,
                sessionContext);
        this.httpService.registerServlet(DENALI_MODULE_PATH + "/status", new GwtStatusServiceImpl(), null,
                sessionContext);
        this.httpService.registerServlet(DENALI_MODULE_PATH + "/device", new GwtDeviceServiceImpl(), null,
                sessionContext);
        this.httpService.registerServlet(DENALI_MODULE_PATH + "/network", new GwtNetworkServiceImpl(), null,
                sessionContext);
        this.httpService.registerServlet(DENALI_MODULE_PATH + "/component", new GwtComponentServiceImpl(), null,
                sessionContext);
        this.httpService.registerServlet(DENALI_MODULE_PATH + "/package", new GwtPackageServiceImpl(), null,
                sessionContext);
        this.httpService.registerServlet(DENALI_MODULE_PATH + "/snapshot", new GwtSnapshotServiceImpl(), null,
                sessionContext);
        this.httpService.registerServlet(DENALI_MODULE_PATH + "/certificate", new GwtCertificatesServiceImpl(), null,
                sessionContext);
        this.httpService.registerServlet(DENALI_MODULE_PATH + "/security", new GwtSecurityServiceImpl(), null,
                sessionContext);
        this.httpService.registerServlet(DENALI_MODULE_PATH + "/file", new FileServlet(), null, sessionContext);
        this.httpService.registerServlet(DENALI_MODULE_PATH + "/device_snapshots", new DeviceSnapshotsServlet(), null,
                sessionContext);
        this.httpService.registerServlet(DENALI_MODULE_PATH + "/assetsUpDownload", new ChannelServlet(), null,
                sessionContext);
        this.httpService.registerServlet(DENALI_MODULE_PATH + "/skin", new SkinServlet(), null, resourceContext);
        this.httpService.registerServlet(DENALI_MODULE_PATH + "/ssl", new GwtSslServiceImpl(), null, sessionContext);
        this.httpService.registerServlet(DENALI_MODULE_PATH + "/cloudservices", new GwtCloudConnectionServiceImpl(),
                null, sessionContext);
        this.httpService.registerServlet(DENALI_MODULE_PATH + "/wires", new GwtWireGraphServiceImpl(), null,
                sessionContext);
        this.httpService.registerServlet(DENALI_MODULE_PATH + "/wiresSnapshot", new WiresSnapshotServlet(), null,
                sessionContext);
        this.httpService.registerServlet(DENALI_MODULE_PATH + "/assetservices", new GwtAssetServiceImpl(), null,
                sessionContext);
        this.httpService.registerServlet(ADMIN_ROOT + "/sse", new WiresBlinkServlet(), null, sessionContext);
        this.httpService.registerServlet(DENALI_MODULE_PATH + EVENT_PATH, this.eventService, null, sessionContext);
        this.eventService.start();
    }

}

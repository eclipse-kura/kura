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

    private static final String PASSWORD_AUTH_PATH = "/login/password";

    private static final String ADMIN_LOGIN_PATH = "/admin/login";

    private static final String ADMIN_PATH = "/admin";

    private static final Logger logger = LoggerFactory.getLogger(Console.class);

    private static final String SERVLET_ALIAS_ROOT = "servlet.alias.root";
    private static final String APP_ROOT = "app.root";
    private static final String SESSION_MAX_INACTIVITY_INTERVAL = "session.max.inactivity.interval";

    private static final String CONSOLE_PASSWORD = "console.password.value";
    private static final String CONSOLE_USERNAME = "console.username.value";

    private String servletRoot;
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

    private void updateAuthenticationManager(Map<String, Object> properties)
            throws KuraException, NoSuchAlgorithmException, UnsupportedEncodingException {
        String registeredUsername = (String) properties.get(CONSOLE_USERNAME);

        Object value = properties.get(CONSOLE_PASSWORD);
        char[] decryptedPassword = this.cryptoService.decryptAes(((String) value).toCharArray());
        char[] propertyPassword = this.cryptoService.sha1Hash(new String(decryptedPassword)).toCharArray();

        this.authMgr.setUsername(registeredUsername);
        this.authMgr.setPassword(propertyPassword);
    }

    private void setAppRoot(String propertiesAppRoot) {
        appRoot = propertiesAppRoot;
    }

    private void setSessionMaxInactiveInterval(int sessionMaxInactiveInterval) {
        this.sessionMaxInactiveInterval = sessionMaxInactiveInterval;
    }

    private void setServletRoot(String propertiesAliasRoot) {
        servletRoot = propertiesAliasRoot;
    }

    private void setComponentContext(ComponentContext context) {
        componentContext = context;
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
        try {
            updateAuthenticationManager(properties);
        } catch (Exception e) {
            logger.warn("Error Updating Web properties", e);
        }

        setServletRoot((String) properties.get(SERVLET_ALIAS_ROOT));
        setAppRoot((String) properties.get(APP_ROOT));
        setSessionMaxInactiveInterval((int) properties.getOrDefault(SESSION_MAX_INACTIVITY_INTERVAL, 15));

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
        this.httpService.unregister(ADMIN_PATH);
        this.httpService.unregister(servletRoot);
        this.httpService.unregister(ADMIN_LOGIN_PATH);
        this.httpService.unregister("/login");

        this.httpService.unregister(PASSWORD_AUTH_PATH);
        this.httpService.unregister(servletRoot + "/session");
        this.httpService.unregister(servletRoot + "/xsrf");
        this.httpService.unregister(servletRoot + "/status");
        this.httpService.unregister(servletRoot + "/device");
        this.httpService.unregister(servletRoot + "/network");
        this.httpService.unregister(servletRoot + "/component");
        this.httpService.unregister(servletRoot + "/package");
        this.httpService.unregister(servletRoot + "/snapshot");
        this.httpService.unregister(servletRoot + "/certificate");
        this.httpService.unregister(servletRoot + "/security");
        this.httpService.unregister(servletRoot + "/file");
        this.httpService.unregister(servletRoot + "/device_snapshots");
        this.httpService.unregister(servletRoot + "/assetsUpDownload");
        this.httpService.unregister(servletRoot + "/skin");
        this.httpService.unregister(servletRoot + "/ssl");
        this.httpService.unregister(servletRoot + "/cloudservices");
        this.httpService.unregister(servletRoot + "/wires");
        this.httpService.unregister(servletRoot + "/wiresSnapshot");
        this.httpService.unregister(servletRoot + "/assetservices");
        this.httpService.unregister("/sse");
        this.eventService.stop();
        this.httpService.unregister(servletRoot + "/event");
    }

    public static Console instance() {
        return instance;
    }

    private static void setInstance(final Console instance) {
        Console.instance = instance;
    }

    public BundleContext getBundleContext() {
        return componentContext.getBundleContext();
    }

    public String getApplicationRoot() {
        return appRoot;
    }

    public String getServletRoot() {
        return servletRoot;
    }

    public HttpSession createSession(final HttpServletRequest request, final HttpServletResponse response) {
        final HttpSession session = request.getSession();

        session.setMaxInactiveInterval(sessionMaxInactiveInterval * 60);

        session.setAttribute(Attributes.LAST_ACTIVITY.getValue(), System.currentTimeMillis());

        return session;
    }

    private HttpContext initSessionContext(final HttpContext defaultContext) {

        final Set<String> authenticationPaths = new HashSet<>(Arrays.asList(ADMIN_LOGIN_PATH, PASSWORD_AUTH_PATH));
        final Set<String> eventPaths = new HashSet<>(Arrays.asList(servletRoot + "/event", "/sse"));

        final SecurityHandler baseHandler = new BaseSecurityHandler(appRoot);
        final SecurityHandler sessionAuthHandler = new SessionAutorizationSecurityHandler();
        final SecurityHandler sessionExpirationHandler = new SessionExpirationSecurityHandler();

        // default session handler requires an authenticated session and handles session expiration
        final SecurityHandler defaultHandler = chain(baseHandler, sessionAuthHandler, sessionExpirationHandler);

        final RoutingSecurityHandler routingHandler = new RoutingSecurityHandler(
                defaultHandler.sendErrorOnFailure(401));

        // exception on authentication paths, allow access without authenticaton but create a session
        routingHandler.addRouteHandler(authenticationPaths::contains,
                chain(baseHandler, new CreateSessionSecurityHandler()));

        // exception on event paths, activity on these paths does not count towards session expriration
        routingHandler.addRouteHandler(eventPaths::contains,
                chain(baseHandler, sessionAuthHandler).sendErrorOnFailure(401));

        // exception on admin console path, redirect to login page on failure instead of sending 401 status
        routingHandler.addRouteHandler(ADMIN_PATH::equals, defaultHandler.redirectOnFailure(ADMIN_LOGIN_PATH));

        return new HttpContextImpl(routingHandler, defaultContext);
    }

    private HttpContext initResourceContext(final HttpContext defaultContext) {
        return new HttpContextImpl(new BaseSecurityHandler(appRoot), defaultContext);
    }

    private void initHTTPService() throws NamespaceException, ServletException {

        final HttpContext defaultContext = this.httpService.createDefaultHttpContext();

        final HttpContext resourceContext = initResourceContext(defaultContext);
        final HttpContext sessionContext = initSessionContext(defaultContext);

        this.httpService.registerResources("/", "www", resourceContext);
        this.httpService.registerResources(ADMIN_PATH, "www/denali.html", sessionContext);
        this.httpService.registerResources(servletRoot, "www" + servletRoot, resourceContext);
        this.httpService.registerResources(ADMIN_LOGIN_PATH, "www/login.html", sessionContext);
        this.httpService.registerResources("/login", "www/login", resourceContext);

        this.httpService.registerServlet(PASSWORD_AUTH_PATH,
                new GwtPasswordAuthenticationServiceImpl(this.authMgr, ADMIN_PATH), null, sessionContext);
        this.httpService.registerServlet(servletRoot + "/session", new GwtSessionServiceImpl(), null, sessionContext);
        this.httpService.registerServlet(servletRoot + "/xsrf", new GwtSecurityTokenServiceImpl(), null,
                sessionContext);
        this.httpService.registerServlet(servletRoot + "/status", new GwtStatusServiceImpl(), null, sessionContext);
        this.httpService.registerServlet(servletRoot + "/device", new GwtDeviceServiceImpl(), null, sessionContext);
        this.httpService.registerServlet(servletRoot + "/network", new GwtNetworkServiceImpl(), null, sessionContext);
        this.httpService.registerServlet(servletRoot + "/component", new GwtComponentServiceImpl(), null,
                sessionContext);
        this.httpService.registerServlet(servletRoot + "/package", new GwtPackageServiceImpl(), null, sessionContext);
        this.httpService.registerServlet(servletRoot + "/snapshot", new GwtSnapshotServiceImpl(), null, sessionContext);
        this.httpService.registerServlet(servletRoot + "/certificate", new GwtCertificatesServiceImpl(), null,
                sessionContext);
        this.httpService.registerServlet(servletRoot + "/security", new GwtSecurityServiceImpl(), null, sessionContext);
        this.httpService.registerServlet(servletRoot + "/file", new FileServlet(), null, sessionContext);
        this.httpService.registerServlet(servletRoot + "/device_snapshots", new DeviceSnapshotsServlet(), null,
                sessionContext);
        this.httpService.registerServlet(servletRoot + "/assetsUpDownload", new ChannelServlet(), null, sessionContext);
        this.httpService.registerServlet(servletRoot + "/skin", new SkinServlet(), null, resourceContext);
        this.httpService.registerServlet(servletRoot + "/ssl", new GwtSslServiceImpl(), null, sessionContext);
        this.httpService.registerServlet(servletRoot + "/cloudservices", new GwtCloudConnectionServiceImpl(), null,
                sessionContext);
        this.httpService.registerServlet(servletRoot + "/wires", new GwtWireGraphServiceImpl(), null, sessionContext);
        this.httpService.registerServlet(servletRoot + "/wiresSnapshot", new WiresSnapshotServlet(), null,
                sessionContext);
        this.httpService.registerServlet(servletRoot + "/assetservices", new GwtAssetServiceImpl(), null,
                sessionContext);
        this.httpService.registerServlet("/sse", new WiresBlinkServlet(), null, sessionContext);
        this.httpService.registerServlet(servletRoot + "/event", this.eventService, null, sessionContext);
        this.eventService.start();
    }

}

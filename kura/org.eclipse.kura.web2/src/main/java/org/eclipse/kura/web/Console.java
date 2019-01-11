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

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;

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
import org.eclipse.kura.web.server.GwtSecurityServiceImpl;
import org.eclipse.kura.web.server.GwtSecurityTokenServiceImpl;
import org.eclipse.kura.web.server.GwtSettingServiceImpl;
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

    private static final Logger logger = LoggerFactory.getLogger(Console.class);

    private static final String SERVLET_ALIAS_ROOT = "servlet.alias.root";
    private static final String APP_ROOT = "app.root";

    private static final String CONSOLE_PASSWORD = "console.password.value";
    private static final String CONSOLE_USERNAME = "console.username.value";

    private static String servletRoot;
    private static String appRoot;
    private static ComponentContext componentContext;

    private HttpService httpService;

    private SystemService systemService;
    private CryptoService cryptoService;

    private EventAdmin eventAdmin;
    private AuthenticationManager authMgr;
    private GwtEventServiceImpl eventService;

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
        // Check if web interface is enabled.
        boolean webEnabled = Boolean.parseBoolean(this.systemService.getKuraWebEnabled());

        if (!webEnabled) {
            logger.info("Web interface disabled in Kura properties file.");
            return;
        }

        try {
            logger.info("activate...");

            setComponentContext(context);
            setServletRoot((String) properties.get(SERVLET_ALIAS_ROOT));
            setAppRoot((String) properties.get(APP_ROOT));

            this.authMgr = AuthenticationManager.getInstance();

            updateAuthenticationManager(properties);

            this.eventService = new GwtEventServiceImpl();

            initHTTPService();

            Map<String, Object> props = new HashMap<>();
            props.put("kura.version", this.systemService.getKuraVersion());
            EventProperties eventProps = new EventProperties(props);
            logger.info("postInstalledEvent() :: posting KuraConfigReadyEvent");
            this.eventAdmin.postEvent(new Event(KuraConfigReadyEvent.KURA_CONFIG_EVENT_READY_TOPIC, eventProps));
        } catch (Exception e) {
            logger.warn("Error Registering Web Resources", e);
        }
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

    private static void setAppRoot(String propertiesAppRoot) {
        appRoot = propertiesAppRoot;
    }

    private static void setServletRoot(String propertiesAliasRoot) {
        servletRoot = propertiesAliasRoot;
    }

    private static void setComponentContext(ComponentContext context) {
        componentContext = context;
    }

    protected void updated(Map<String, Object> properties) {
        boolean webEnabled = Boolean.parseBoolean(this.systemService.getKuraWebEnabled());
        if (!webEnabled) {
            return;
        }

        try {
            updateAuthenticationManager(properties);
        } catch (Exception e) {
            logger.warn("Error Updating Web properties", e);
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
        this.httpService.unregister(appRoot);
        this.httpService.unregister(servletRoot);
        this.httpService.unregister(servletRoot + "/status");
        this.httpService.unregister(servletRoot + "/device");
        this.httpService.unregister(servletRoot + "/network");
        this.httpService.unregister(servletRoot + "/component");
        this.httpService.unregister(servletRoot + "/package");
        this.httpService.unregister(servletRoot + "/snapshot");
        this.httpService.unregister(servletRoot + "/setting");
        this.httpService.unregister(servletRoot + "/file");
        this.httpService.unregister(servletRoot + "/device_snapshots");
        this.httpService.unregister(servletRoot + "/assetsUpDownload");
        this.httpService.unregister(servletRoot + "/skin");
        this.httpService.unregister(servletRoot + "/wires");
        this.httpService.unregister("/sse");
        this.eventService.stop();
        this.httpService.unregister(servletRoot + "/event");
    }

    public static BundleContext getBundleContext() {
        return componentContext.getBundleContext();
    }

    public static String getApplicationRoot() {
        return appRoot;
    }

    public static String getServletRoot() {
        return servletRoot;
    }

    private void initHTTPService() throws NamespaceException, ServletException {
        // Initialize HttpService

        HttpContext httpCtx = new SecureBasicHttpContext(this.httpService.createDefaultHttpContext(), this.authMgr);
        this.httpService.registerResources("/", "www", httpCtx);
        this.httpService.registerResources(appRoot, "www/denali.html", httpCtx);
        this.httpService.registerResources(servletRoot, "www" + servletRoot, httpCtx);

        this.httpService.registerServlet(servletRoot + "/xsrf", new GwtSecurityTokenServiceImpl(), null, httpCtx);
        this.httpService.registerServlet(servletRoot + "/status", new GwtStatusServiceImpl(), null, httpCtx);
        this.httpService.registerServlet(servletRoot + "/device", new GwtDeviceServiceImpl(), null, httpCtx);
        this.httpService.registerServlet(servletRoot + "/network", new GwtNetworkServiceImpl(), null, httpCtx);
        this.httpService.registerServlet(servletRoot + "/component", new GwtComponentServiceImpl(), null, httpCtx);
        this.httpService.registerServlet(servletRoot + "/package", new GwtPackageServiceImpl(), null, httpCtx);
        this.httpService.registerServlet(servletRoot + "/snapshot", new GwtSnapshotServiceImpl(), null, httpCtx);
        this.httpService.registerServlet(servletRoot + "/setting", new GwtSettingServiceImpl(), null, httpCtx);
        this.httpService.registerServlet(servletRoot + "/certificate", new GwtCertificatesServiceImpl(), null, httpCtx);
        this.httpService.registerServlet(servletRoot + "/security", new GwtSecurityServiceImpl(), null, httpCtx);
        this.httpService.registerServlet(servletRoot + "/file", new FileServlet(), null, httpCtx);
        this.httpService.registerServlet(servletRoot + "/device_snapshots", new DeviceSnapshotsServlet(), null,
                httpCtx);
        this.httpService.registerServlet(servletRoot + "/assetsUpDownload", new ChannelServlet(), null, httpCtx);
        this.httpService.registerServlet(servletRoot + "/skin", new SkinServlet(), null, httpCtx);
        this.httpService.registerServlet(servletRoot + "/ssl", new GwtSslServiceImpl(), null, httpCtx);
        this.httpService.registerServlet(servletRoot + "/cloudservices", new GwtCloudConnectionServiceImpl(), null,
                httpCtx);
        this.httpService.registerServlet(servletRoot + "/wires", new GwtWireGraphServiceImpl(), null, httpCtx);
        this.httpService.registerServlet(servletRoot + "/wiresSnapshot", new WiresSnapshotServlet(), null, httpCtx);
        this.httpService.registerServlet(servletRoot + "/assetservices", new GwtAssetServiceImpl(), null, httpCtx);
        this.httpService.registerServlet("/sse", new WiresBlinkServlet(), null, httpCtx);
        this.httpService.registerServlet(servletRoot + "/event", this.eventService, null, httpCtx);
        this.eventService.start();
    }

}

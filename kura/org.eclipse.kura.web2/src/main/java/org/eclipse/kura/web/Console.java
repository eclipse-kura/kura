/*******************************************************************************
 * Copyright (c) 2011, 2018 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.web;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;

import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.configuration.KuraConfigReadyEvent;
import org.eclipse.kura.crypto.CryptoService;
import org.eclipse.kura.system.SystemService;
import org.eclipse.kura.web.server.GwtAssetServiceImpl;
import org.eclipse.kura.web.server.GwtCertificatesServiceImpl;
import org.eclipse.kura.web.server.GwtCloudServiceImpl;
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
import org.eclipse.kura.web.server.GwtWireServiceImpl;
import org.eclipse.kura.web.server.servlet.ChannelServlet;
import org.eclipse.kura.web.server.servlet.DeviceSnapshotsServlet;
import org.eclipse.kura.web.server.servlet.EventHandlerServlet;
import org.eclipse.kura.web.server.servlet.FileServlet;
import org.eclipse.kura.web.server.servlet.SkinServlet;
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

    private static String aliasRoot;
    private static String appRoot;
    private static ComponentContext context;

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
        try {
            // Check if web interface is enabled.
            boolean webEnabled = Boolean.parseBoolean(this.systemService.getKuraWebEnabled());

            if (webEnabled) {
                logger.info("activate...");

                Console.context = context;
                aliasRoot = (String) properties.get(SERVLET_ALIAS_ROOT);
                appRoot = (String) properties.get(APP_ROOT);
                String servletRoot = aliasRoot;

                Object pwdProp = properties.get(CONSOLE_PASSWORD);
                char[] propertyPassword = null;
                if (pwdProp instanceof char[]) {
                    propertyPassword = (char[]) properties.get(CONSOLE_PASSWORD);
                } else {
                    propertyPassword = properties.get(CONSOLE_PASSWORD).toString().toCharArray();
                }

                try {
                    propertyPassword = this.cryptoService.decryptAes(propertyPassword);
                } catch (Exception e) {
                }

                Object value = properties.get(CONSOLE_PASSWORD);
                char[] decryptedPassword = null;
                try {
                    decryptedPassword = this.cryptoService.decryptAes(((String) value).toCharArray());
                } catch (Exception e) {
                    decryptedPassword = value.toString().toCharArray();
                }
                propertyPassword = this.cryptoService.sha1Hash(new String(decryptedPassword)).toCharArray();

                String registeredUsername = (String) properties.get(CONSOLE_USERNAME);
                this.authMgr = new AuthenticationManager(registeredUsername, propertyPassword);

                this.eventService = new GwtEventServiceImpl();

                initHTTPService(this.authMgr, servletRoot);

                Map<String, Object> props = new HashMap<>();
                props.put("kura.version", this.systemService.getKuraVersion());
                EventProperties eventProps = new EventProperties(props);
                logger.info("postInstalledEvent() :: posting KuraConfigReadyEvent");
                this.eventAdmin.postEvent(new Event(KuraConfigReadyEvent.KURA_CONFIG_EVENT_READY_TOPIC, eventProps));

            } else {
                logger.info("Web interface disabled in Kura properties file.");
            }
        } catch (Throwable t) {
            logger.warn("Error Registering Web Resources", t);
        }

    }

    protected void updated(Map<String, Object> properties) {

        boolean webEnabled = Boolean.parseBoolean(this.systemService.getKuraWebEnabled());
        if (!webEnabled) {
            return;
        }

        char[] propertyPassword = null;

        String registeredUsername = (String) properties.get(CONSOLE_USERNAME);
        this.authMgr.updateUsername(registeredUsername);

        try {
            Object value = properties.get(CONSOLE_PASSWORD);
            char[] decryptedPassword = null;
            try {
                decryptedPassword = this.cryptoService.decryptAes(((String) value).toCharArray());
            } catch (Exception e) {
                decryptedPassword = value.toString().toCharArray();
            }

            propertyPassword = this.cryptoService.sha1Hash(new String(decryptedPassword)).toCharArray();

            this.authMgr.updatePassword(propertyPassword);
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
        String servletRoot = aliasRoot;
        this.httpService.unregister("/");
        this.httpService.unregister(appRoot);
        this.httpService.unregister(aliasRoot);
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
        return context.getBundleContext();
    }

    public static String getApplicationRoot() {
        return appRoot;
    }

    public static String getServletRoot() {
        return aliasRoot;
    }

    private void initHTTPService(AuthenticationManager authMgr, String servletRoot)
            throws NamespaceException, ServletException {
        // Initialize HttpService

        HttpContext httpCtx = new SecureBasicHttpContext(this.httpService.createDefaultHttpContext(), authMgr);
        this.httpService.registerResources("/", "www", httpCtx);
        this.httpService.registerResources(appRoot, "www/denali.html", httpCtx);
        this.httpService.registerResources(aliasRoot, "www" + aliasRoot, httpCtx);

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
        this.httpService.registerServlet(servletRoot + "/cloudservices", new GwtCloudServiceImpl(), null, httpCtx);
        this.httpService.registerServlet(servletRoot + "/wires", new GwtWireServiceImpl(), null, httpCtx);
        this.httpService.registerServlet(servletRoot + "/wiresSnapshot", new WiresSnapshotServlet(), null, httpCtx);
        this.httpService.registerServlet(servletRoot + "/assetservices", new GwtAssetServiceImpl(), null, httpCtx);
        this.httpService.registerServlet("/sse", new EventHandlerServlet(), null, httpCtx);
        this.httpService.registerServlet(servletRoot + "/event", this.eventService, null, httpCtx);
        this.eventService.start();

        org.eclipse.jetty.http.HttpGenerator.setJettyVersion("Jetty");
    }

}

/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *     Jens Reimann <jreimann@redhat.com> - Fix possible NPE, cleanup
 *******************************************************************************/
package org.eclipse.kura.web;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.ServletException;

import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.configuration.KuraConfigReadyEvent;
import org.eclipse.kura.crypto.CryptoService;
import org.eclipse.kura.system.SystemService;
import org.eclipse.kura.web.server.GwtCertificatesServiceImpl;
import org.eclipse.kura.web.server.GwtComponentServiceImpl;
import org.eclipse.kura.web.server.GwtDeviceServiceImpl;
import org.eclipse.kura.web.server.GwtNetworkServiceImpl;
import org.eclipse.kura.web.server.GwtPackageServiceImpl;
import org.eclipse.kura.web.server.GwtSecurityServiceImpl;
import org.eclipse.kura.web.server.GwtSecurityTokenServiceImpl;
import org.eclipse.kura.web.server.GwtSettingServiceImpl;
import org.eclipse.kura.web.server.GwtSnapshotServiceImpl;
import org.eclipse.kura.web.server.GwtSslServiceImpl;
import org.eclipse.kura.web.server.GwtStatusServiceImpl;
import org.eclipse.kura.web.server.servlet.DeviceSnapshotsServlet;
import org.eclipse.kura.web.server.servlet.FileServlet;
import org.eclipse.kura.web.server.servlet.SkinServlet;
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
	private static final Logger s_logger = LoggerFactory.getLogger(Console.class);

	private static final String SERVLET_ALIAS_ROOT = "servlet.alias.root";
	private static final String APP_ROOT = "app.root";

	private static final String CONSOLE_PASSWORD = "console.password.value";
	private static final String CONSOLE_USERNAME = "console.username.value";

	private static String s_aliasRoot;
	private static String s_appRoot;
	private static ComponentContext s_context;

	private HttpService m_httpService;

	private SystemService m_systemService;
	private CryptoService m_cryptoService;

	private Map<String, Object> m_properties;

	private EventAdmin m_eventAdmin;
	private AuthenticationManager authMgr;

	// ----------------------------------------------------------------
	//
	// Dependencies
	//
	// ----------------------------------------------------------------

	public void setHttpService(HttpService httpService) {
		this.m_httpService = httpService;
	}

	public void unsetHttpService(HttpService httpService) {
		this.m_httpService = null;
	}

	public void setSystemService(SystemService systemService) {
		this.m_systemService = systemService;
	}

	public void unsetSystemService(SystemService systemService) {
		this.m_systemService = null;
	}

	public void setCryptoService(CryptoService cryptoService) {
		this.m_cryptoService = cryptoService;
	}

	public void unsetCryptoService(CryptoService cryptoService) {
		this.m_cryptoService = null;
	}

	public void setEventAdminService(EventAdmin eventAdmin) {
		m_eventAdmin = eventAdmin;
	}

	public void unsetEventAdminService(EventAdmin eventAdmin) {
		m_eventAdmin = null;
	}

	// ----------------------------------------------------------------
	//
	// Activation APIs
	//
	// ----------------------------------------------------------------

	protected void activate(ComponentContext context, Map<String, Object> properties) {
		try {
			// Check if web interface is enabled.
			boolean webEnabled = Boolean.parseBoolean((m_systemService.getKuraWebEnabled()));

			if (webEnabled) {
				s_logger.info("activate...");

				s_context = context;
				s_aliasRoot = (String) properties.get(SERVLET_ALIAS_ROOT);
				s_appRoot = (String) properties.get(APP_ROOT);
				String servletRoot = s_aliasRoot;

				m_properties = new HashMap<String, Object>();
				Iterator<String> keys = properties.keySet().iterator();
				while (keys.hasNext()) {
					String key = keys.next();
					Object value = properties.get(key);
					m_properties.put(key, value);
				}

				Object pwdProp = properties.get(CONSOLE_PASSWORD);
				char[] propertyPassword = null;
				if (pwdProp instanceof char[]) {
					propertyPassword = (char[]) properties.get(CONSOLE_PASSWORD);
				} else {
					propertyPassword = properties.get(CONSOLE_PASSWORD).toString().toCharArray();
				}

				try {
					propertyPassword = m_cryptoService.decryptAes(propertyPassword);
				} catch (Exception e) {
				}

				Object value = properties.get(CONSOLE_PASSWORD);
				char[] decryptedPassword = null;
				try {
					decryptedPassword = m_cryptoService.decryptAes(((String) value).toCharArray());
				} catch (Exception e) {
					decryptedPassword = value.toString().toCharArray();
				}
				propertyPassword = m_cryptoService.sha1Hash(new String(decryptedPassword)).toCharArray();

				String registeredUsername = (String) properties.get(CONSOLE_USERNAME);
				authMgr = new AuthenticationManager(registeredUsername, propertyPassword);
				initHTTPService(authMgr, servletRoot);

				Map<String, Object> props = new HashMap<String, Object>();
				props.put("kura.version", m_systemService.getKuraVersion());
				EventProperties eventProps = new EventProperties(props);
				s_logger.info("postInstalledEvent() :: posting KuraConfigReadyEvent");
				m_eventAdmin.postEvent(new Event(KuraConfigReadyEvent.KURA_CONFIG_EVENT_READY_TOPIC, eventProps));
			} else {
				s_logger.info("Web interface disabled in Kura properties file.");
			}
		} catch (Throwable t) {
			s_logger.warn("Error Registering Web Resources", t);
		}

	}

	protected void updated(Map<String, Object> properties) {

		boolean webEnabled = Boolean.parseBoolean((m_systemService.getKuraWebEnabled()));
		if (!webEnabled) {
			return;
		}

		char[] propertyPassword = null;

		String registeredUsername = (String) properties.get(CONSOLE_USERNAME);
		authMgr.updateUsername(registeredUsername);

		try {
			Object value = properties.get(CONSOLE_PASSWORD);
			char[] decryptedPassword = null;
			try {
				decryptedPassword = m_cryptoService.decryptAes(((String) value).toCharArray());
			} catch (Exception e) {
				decryptedPassword = value.toString().toCharArray();
			}

			propertyPassword = m_cryptoService.sha1Hash(new String(decryptedPassword)).toCharArray();

			authMgr.updatePassword(propertyPassword);
		} catch (Exception e) {
			s_logger.warn("Error Updating Web properties", e);
		}

	}

	protected void deactivate(BundleContext context) {
		s_logger.info("deactivate...");

		s_context = null;

		unregisterServlet();
	}

	// ----------------------------------------------------------------
	//
	// Private methods
	//
	// ----------------------------------------------------------------

	private void unregisterServlet() {
		String servletRoot = s_aliasRoot;
		m_httpService.unregister("/");
		m_httpService.unregister(s_appRoot);
		m_httpService.unregister(s_aliasRoot);
		m_httpService.unregister(servletRoot + "/status");
		m_httpService.unregister(servletRoot + "/device");
		m_httpService.unregister(servletRoot + "/network");
		m_httpService.unregister(servletRoot + "/component");
		m_httpService.unregister(servletRoot + "/package");
		m_httpService.unregister(servletRoot + "/snapshot");
		m_httpService.unregister(servletRoot + "/setting");
		m_httpService.unregister(servletRoot + "/file");
		m_httpService.unregister(servletRoot + "/device_snapshots");
		m_httpService.unregister(servletRoot + "/skin");

	}

	public static BundleContext getBundleContext() {
		return s_context.getBundleContext();
	}

	public static String getApplicationRoot() {
		return s_appRoot;
	}

	public static String getServletRoot() {
		return s_aliasRoot;
	}

	private void initHTTPService(AuthenticationManager authMgr, String servletRoot)
			throws NamespaceException, ServletException {
		// Initialize HttpService

		HttpContext httpCtx = new SecureBasicHttpContext(m_httpService.createDefaultHttpContext(), authMgr);
		m_httpService.registerResources("/", "www", httpCtx);
		m_httpService.registerResources(s_appRoot, "www/denali.html", httpCtx);
		m_httpService.registerResources(s_aliasRoot, "www" + s_aliasRoot, httpCtx);

		m_httpService.registerServlet(servletRoot + "/xsrf", new GwtSecurityTokenServiceImpl(), null, httpCtx);
		m_httpService.registerServlet(servletRoot + "/status", new GwtStatusServiceImpl(), null, httpCtx);
		m_httpService.registerServlet(servletRoot + "/device", new GwtDeviceServiceImpl(), null, httpCtx);
		m_httpService.registerServlet(servletRoot + "/network", new GwtNetworkServiceImpl(), null, httpCtx);
		m_httpService.registerServlet(servletRoot + "/component", new GwtComponentServiceImpl(), null, httpCtx);
		m_httpService.registerServlet(servletRoot + "/package", new GwtPackageServiceImpl(), null, httpCtx);
		m_httpService.registerServlet(servletRoot + "/snapshot", new GwtSnapshotServiceImpl(), null, httpCtx);
		m_httpService.registerServlet(servletRoot + "/setting", new GwtSettingServiceImpl(), null, httpCtx);
		m_httpService.registerServlet(servletRoot + "/certificate", new GwtCertificatesServiceImpl(), null, httpCtx);
		m_httpService.registerServlet(servletRoot + "/security", new GwtSecurityServiceImpl(), null, httpCtx);
		m_httpService.registerServlet(servletRoot + "/file", new FileServlet(), null, httpCtx);
		m_httpService.registerServlet(servletRoot + "/device_snapshots", new DeviceSnapshotsServlet(), null, httpCtx);
		m_httpService.registerServlet(servletRoot + "/skin", new SkinServlet(), null, httpCtx);
		m_httpService.registerServlet(servletRoot + "/ssl", new GwtSslServiceImpl(), null, httpCtx);
	}
}

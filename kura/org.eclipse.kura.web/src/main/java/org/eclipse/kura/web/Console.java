/**
 * Copyright (c) 2011, 2014 Eurotech and/or its affiliates
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Eurotech
 */
package org.eclipse.kura.web;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.Set;

import javax.servlet.ServletException;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.configuration.KuraConfigReadyEvent;
import org.eclipse.kura.crypto.CryptoService;
import org.eclipse.kura.db.DbService;
import org.eclipse.kura.system.SystemService;
import org.eclipse.kura.web.server.GwtComponentServiceImpl;
import org.eclipse.kura.web.server.GwtDeviceServiceImpl;
import org.eclipse.kura.web.server.GwtNetworkServiceImpl;
import org.eclipse.kura.web.server.GwtPackageServiceImpl;
import org.eclipse.kura.web.server.GwtSettingServiceImpl;
import org.eclipse.kura.web.server.GwtSnapshotServiceImpl;
import org.eclipse.kura.web.server.GwtStatusServiceImpl;
import org.eclipse.kura.web.server.servlet.DeviceSnapshotsServlet;
import org.eclipse.kura.web.server.servlet.FileServlet;
import org.eclipse.kura.web.server.servlet.SkinServlet;
import org.osgi.framework.BundleContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventProperties;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Console implements ConfigurableComponent
{
	private static final Logger s_logger = LoggerFactory.getLogger(Console.class);

	private static final String ESF_DATA_DIR = "kura.data";
	private static final String SERVLET_ALIAS_ROOT = "servlet.alias.root";
	private static final String APP_ROOT		   = "app.root";
	private static final String APP_PID= "service.pid";

	private static final String CONSOLE_PASSWORD = "console.password.value";

	private static String        s_aliasRoot;
	private static String		 s_appRoot;
	private static BundleContext s_context;

	private DbService            m_dbService;
	private HttpService          m_httpService;

	private ExecutorService    m_worker;
	private Future<?>          m_handle;

	@SuppressWarnings("unused")
	private SystemService        m_systemService;
	@SuppressWarnings("unused")
	private ConfigurationService m_configService;
	@SuppressWarnings("unused")
	private CryptoService		m_cryptoService;

	private Map<String,Object> m_properties;

	private EventAdmin m_eventAdmin;
	private AuthenticationManager authMgr;

	// ----------------------------------------------------------------
	//
	//   Dependencies
	//
	// ----------------------------------------------------------------

	public Console() 
	{
		super();
		m_worker = Executors.newSingleThreadExecutor();
	}

	public void setHttpService(HttpService httpService) {
		this.m_httpService = httpService;
	}

	public void unsetHttpService(HttpService httpService) {
		this.m_httpService = null;
	}

	public void setDbService(DbService dbService) {
		this.m_dbService = dbService;
	}

	public void unsetDbService(DbService dbService) {
		this.m_dbService = null;
	}

	public void setSystemService(SystemService systemService) {
		this.m_systemService = systemService;
	}

	public void unsetSystemService(SystemService systemService) {
		this.m_systemService = null;
	}

	public void setConfigurationService(ConfigurationService configService) {
		this.m_configService = configService;
	}

	public void unsetConfigurationService(ConfigurationService configService) {
		this.m_configService = null;
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
	//   Activation APIs
	//
	// ----------------------------------------------------------------

	protected void activate(BundleContext context, Map<String,Object> properties)
	{
		try {
			// Check if web interface is enabled.
			Boolean webEnabled = Boolean.valueOf(m_systemService.getKuraWebEnabled());

			if (webEnabled) {
				s_logger.info("activate...");

				s_context   = context;
				s_aliasRoot = (String) properties.get(SERVLET_ALIAS_ROOT);
				s_appRoot   = (String) properties.get(APP_ROOT);
				String servletRoot = s_aliasRoot;
				m_properties= properties;

				// Initialize AuthenticationManager with DbService
				String dataDir = m_systemService.getProperties().getProperty(ESF_DATA_DIR);

				String passwordFromDB= AuthenticationManager.isDBInitialized(m_dbService, dataDir);
				try{
					passwordFromDB= m_cryptoService.decryptAes(passwordFromDB);
				}catch (Exception e){
				}
				String propertyPassword= (String) properties.get(CONSOLE_PASSWORD);
				try{
					propertyPassword= m_cryptoService.decryptAes(propertyPassword);
				}catch (Exception e){
				}

				if(passwordFromDB != null){ 
					if(!propertyPassword.equals(passwordFromDB)){
						if(propertyPassword.equals("admin")){

							Map<String, Object> updatedProperties= new HashMap<String, Object>();
							Iterator<String> keys = properties.keySet().iterator();
							while (keys.hasNext()) {
								String key = keys.next();
								Object value = properties.get(key);
								if (key.equals(CONSOLE_PASSWORD)) {								
									updatedProperties.put(key, passwordFromDB);
								}else{
									updatedProperties.put(key, value);
								}
							}
							m_properties= updatedProperties;

							doUpdate(false);
						}else{
							Iterator<String> keys = properties.keySet().iterator();
							while (keys.hasNext()) {
								String key = keys.next();
								Object value = properties.get(key);
								if (key.equals(CONSOLE_PASSWORD)) {
									String decryptedPassword= null;
									try{
										decryptedPassword= m_cryptoService.decryptAes((String) value);
									}catch (Exception e){
										decryptedPassword= (String) value;
									}
									propertyPassword= m_cryptoService.sha1Hash(decryptedPassword);
								}
							}
						}
					}
				} else {
					Iterator<String> keys = properties.keySet().iterator();
					while (keys.hasNext()) {
						String key = keys.next();
						Object value = properties.get(key);
						if (key.equals(CONSOLE_PASSWORD)) {
							String decryptedPassword= null;
							try{
								decryptedPassword= m_cryptoService.decryptAes((String) value);
							}catch (Exception e){
								decryptedPassword= (String) value;
							}
							propertyPassword= m_cryptoService.sha1Hash(decryptedPassword);
						}
					}
				}


				authMgr = new AuthenticationManager(propertyPassword);
				initHTTPService(authMgr, servletRoot);

				Map<String,Object> props = new HashMap<String,Object>();
				props.put("kura.version", m_systemService.getKuraVersion());	
				EventProperties eventProps = new EventProperties(props);
				s_logger.info("postInstalledEvent() :: posting KuraConfigReadyEvent");
				m_eventAdmin.postEvent(new Event(KuraConfigReadyEvent.KURA_CONFIG_EVENT_READY_TOPIC, eventProps));
			}
			else {
				s_logger.info("Web interface disabled in Kura properties file.");
			}
		}
		catch (Throwable t) {
			s_logger.warn("Error Registering Web Resources", t);
		}


	}

	protected void updated(Map<String, Object> properties) {
		Iterator<String> keys = properties.keySet().iterator();
		String propertyPassword=null;
		try{
			while (keys.hasNext()) {
				String key = keys.next();
				Object value = properties.get(key);
				if (key.equals(CONSOLE_PASSWORD)) {
					String decryptedPassword= null;
					try{
						decryptedPassword= m_cryptoService.decryptAes((String) value);
					}catch (Exception e){
						decryptedPassword= (String) value;
					}
					propertyPassword= m_cryptoService.sha1Hash(decryptedPassword);
				}
			}
			authMgr.updatePassword(propertyPassword);
		}catch(Exception e){
			s_logger.warn("Error Updating Web properties", e);
		}
	}


	protected void deactivate(BundleContext context) 
	{
		s_logger.info("deactivate...");

		s_context = null;
		m_worker.shutdown();

		unregisterServlet();
	}

	private void unregisterServlet() {
		String servletRoot = s_aliasRoot;
		m_httpService.unregister("/");
		m_httpService.unregister(s_appRoot);
		m_httpService.unregister(s_aliasRoot);
		m_httpService.unregister(servletRoot+"/status");
		m_httpService.unregister(servletRoot+"/device");
		m_httpService.unregister(servletRoot+"/network");
		m_httpService.unregister(servletRoot+"/component");
		m_httpService.unregister(servletRoot+"/package");
		m_httpService.unregister(servletRoot+"/snapshot");
		m_httpService.unregister(servletRoot+"/setting");
		m_httpService.unregister(servletRoot+"/file");
		m_httpService.unregister(servletRoot+"/device_snapshots");
		m_httpService.unregister(servletRoot+"/skin");

	}

	public static BundleContext getBundleContext() {
		return s_context;
	}

	public static String getApplicationRoot() {
		return s_appRoot;
	}

	public static String getServletRoot() {
		return s_aliasRoot;
	}

	private void doUpdate(boolean onUpdate) 
	{
		// cancel a current worker handle if one if active
		if (m_handle != null) {
			m_handle.cancel(true);
		}


		m_worker= Executors.newSingleThreadExecutor();
		m_handle = m_worker.submit(new Runnable() {		
			public void run() {
				String searchedPID= (String) m_properties.get(APP_PID);
				while(true){
					Set<String> pids= m_configService.getConfigurableComponentPids();
					Iterator<String> pidIterator= pids.iterator();
					while(pidIterator.hasNext()){
						String pid= pidIterator.next();
						if(pid.equals(searchedPID)){
							try {
								m_configService.updateConfiguration(searchedPID, m_properties);
								return;
							} catch (KuraException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					}
				}
			}
		});
	}

	private void initHTTPService(AuthenticationManager authMgr, String servletRoot) throws NamespaceException, ServletException{
		// Initialize HttpService
		HttpContext httpCtx = new SecureBasicHttpContext(m_httpService.createDefaultHttpContext(), authMgr);	
		m_httpService.registerResources("/", "www", httpCtx);
		m_httpService.registerResources(s_appRoot, "www/denali.html", httpCtx);
		m_httpService.registerResources(s_aliasRoot, "www"+s_aliasRoot, httpCtx);

		m_httpService.registerServlet(servletRoot+"/status",            new GwtStatusServiceImpl(),    null, httpCtx);
		m_httpService.registerServlet(servletRoot+"/device",    		new GwtDeviceServiceImpl(),    null, httpCtx);
		m_httpService.registerServlet(servletRoot+"/network",   		new GwtNetworkServiceImpl(),   null, httpCtx);
		m_httpService.registerServlet(servletRoot+"/component", 		new GwtComponentServiceImpl(), null, httpCtx);
		m_httpService.registerServlet(servletRoot+"/package",   		new GwtPackageServiceImpl(),   null, httpCtx);
		m_httpService.registerServlet(servletRoot+"/snapshot",  		new GwtSnapshotServiceImpl(),  null, httpCtx);
		m_httpService.registerServlet(servletRoot+"/setting",   		new GwtSettingServiceImpl(),   null, httpCtx);
		m_httpService.registerServlet(servletRoot+"/file",      		new FileServlet(),             null, httpCtx);
		m_httpService.registerServlet(servletRoot+"/device_snapshots", 	new DeviceSnapshotsServlet(),  null, httpCtx);
		m_httpService.registerServlet(servletRoot+"/skin", 				new SkinServlet(),			   null, httpCtx);
	}
}

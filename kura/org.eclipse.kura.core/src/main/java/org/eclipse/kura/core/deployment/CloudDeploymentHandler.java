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
package org.eclipse.kura.core.deployment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.text.ParseException;
import java.util.Date;
import java.util.Dictionary;
import java.util.Hashtable;

import javax.xml.bind.JAXBException;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloud.CloudClient;
import org.eclipse.kura.cloud.CloudClientListener;
import org.eclipse.kura.cloud.CloudService;
import org.eclipse.kura.cloud.CloudletTopic;
import org.eclipse.kura.cloud.CloudletTopic.Method;
import org.eclipse.kura.core.util.ThrowableUtil;
import org.eclipse.kura.core.util.XmlUtil;
import org.eclipse.kura.deployment.agent.DeploymentAgentService;
import org.eclipse.kura.message.KuraPayload;
import org.eclipse.kura.message.KuraRequestPayload;
import org.eclipse.kura.message.KuraResponsePayload;
import org.eclipse.kura.system.SystemService;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.deploymentadmin.BundleInfo;
import org.osgi.service.deploymentadmin.DeploymentAdmin;
import org.osgi.service.deploymentadmin.DeploymentPackage;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CloudDeploymentHandler implements EventHandler, CloudClientListener 
{
	private static Logger s_logger = LoggerFactory.getLogger(CloudDeploymentHandler.class);
	
	public static final String  APP_ID         = "DEPLOY-V1";
	
	// GET
	public static final String  RESOURCE_PACKAGES = "packages";
	public static final String  RESOURCE_BUNDLES  = "bundles";
	
	// EXEC
	public static final String  RESOURCE_INSTALL   = "install";
	public static final String  RESOURCE_UNINSTALL = "uninstall";
	public static final String  RESOURCE_START     = "start";
	public static final String  RESOURCE_STOP      = "stop";
	
	// Metrics of RESOURCE_INSTALL
	public static final String  METRIC_INSTALL_COMMAND_URL      = "deploy.url";
	public static final String  METRIC_INSTALL_COMMAND_FILENAME = "deploy.filename";
	
	// Metrics in the REPLY to RESOURCE_INSTALL	
	public static final String  METRIC_INSTALL_REPLY_PKG_NAME    = "deploy.pkg.name";
	public static final String  METRIC_INSTALL_REPLY_PKG_VERSION = "deploy.pkg.version";
	
	//private final static String [] EVENT_TOPICS = new String[] {"*"};
	//private final static String [] EVENT_TOPICS = new String[] {"org/osgi/service/deployment/*"};
	private final static String [] EVENT_TOPICS = {
		DeploymentAgentService.EVENT_INSTALLED_TOPIC,
		DeploymentAgentService.EVENT_UNINSTALLED_TOPIC
		};
	
	private final static String SYS_PROP_REQ_ID = CloudDeploymentHandler.class.getPackage().getName() + "request.id";
	private final static String SYS_PROP_REQUESTER_CLIENT_ID = CloudDeploymentHandler.class.getPackage().getName() + "requester.client.id";
	private final static String SYS_PROP_PACKAGE_URL = CloudDeploymentHandler.class.getPackage().getName() + "package.ulr";
	
		
	private static final int     DFLT_PUB_QOS  = 0;
	private static final boolean DFLT_RETAIN   = false;
	private static final int     DFLT_PRIORITY = 1;

	private CloudService             m_cloudService;
	private CloudClient   m_cloudClient;
	private DeploymentAdmin          m_deploymentAdmin;
	private DeploymentAgentService   m_deploymentAgentService;
	private ServiceRegistration<?> m_eventServiceRegistration;
	
	private String m_pendingInstPackageUrl;
	private String m_pendingInstRequestId;
	private String m_pendingInstRequesterClientId;
	
	private String m_pendingUninstPackageName;
	private String m_pendingUninstRequestId;
	private String m_pendingUninstRequesterClientId;

	
	private BundleContext m_bundleContext;
	
	
	// ----------------------------------------------------------------
	//
	//   Dependencies
	//
	// ----------------------------------------------------------------
	
	protected void setCloudService(CloudService cloudService) {
		m_cloudService = cloudService;
	}

	protected void unsetCloudService(CloudService cloudService) {
		m_cloudService = null;
	}
	
	protected void setDeploymentAdmin(DeploymentAdmin deploymentAdmin) {
		m_deploymentAdmin = deploymentAdmin;
	}
	
	protected void unsetDeploymentAdmin(DeploymentAdmin deploymentAdmin) {
		m_deploymentAdmin = null;
	}
	
	protected void setDeploymentAgentService(DeploymentAgentService deploymentAgentService) {
		m_deploymentAgentService = deploymentAgentService;
	}

	protected void unsetDeploymentAgentService(DeploymentAgentService deploymentAgentService) {
		m_deploymentAgentService = null;
	}
	
	public void setSystemService(SystemService systemService) {
	}

	public void unsetSystemService(SystemService systemService) {
	}
	
	// ----------------------------------------------------------------
	//
	//   Activation APIs
	//
	// ----------------------------------------------------------------
	
	protected void activate(ComponentContext componentContext) 
	{
		s_logger.info("activate...");
		
		m_bundleContext = componentContext.getBundleContext();
		
		m_pendingInstPackageUrl = System.getProperty(SYS_PROP_PACKAGE_URL);
		if (m_pendingInstPackageUrl != null) {
			s_logger.info("Found pending package URL {}", m_pendingInstPackageUrl);
		}
		
		m_pendingInstRequestId = System.getProperty(SYS_PROP_REQ_ID);
		if (m_pendingInstRequestId != null) {
			s_logger.info("Found pending request ID {}", m_pendingInstRequestId);
		}

		m_pendingInstRequesterClientId = System.getProperty(SYS_PROP_REQUESTER_CLIENT_ID);
		if (m_pendingInstRequesterClientId != null) {
			s_logger.info("Found pending requester client ID {}", m_pendingInstRequesterClientId);
		}
		
		Dictionary<String, String[]> d = new Hashtable<String, String[]>();
		d.put(EventConstants.EVENT_TOPIC, EVENT_TOPICS);
//		d.put(EventConstants.EVENT_FILTER,
//		"(bundle.symbolicName=com.acme.*)" );
		BundleContext bundleContext = componentContext.getBundleContext();
		m_eventServiceRegistration = bundleContext.registerService(EventHandler.class.getName(), this, d);
				
		try {
			
			s_logger.info("Getting CloudApplicationClient for {}...", APP_ID);
			
			m_cloudClient = m_cloudService.newCloudClient(APP_ID);
			m_cloudClient.addCloudClientListener(this);
		} catch (KuraException e) {
			s_logger.error("Error getting CloudApplicationClient", e);
		}
	}
	
	protected void deactivate(ComponentContext componentContext) 
	{
		s_logger.info("deactivate...");
				
		m_cloudClient.release();
		m_eventServiceRegistration.unregister();
		
		System.clearProperty(SYS_PROP_PACKAGE_URL);
		System.clearProperty(SYS_PROP_REQUESTER_CLIENT_ID);
		System.clearProperty(SYS_PROP_REQ_ID);
		
		if (m_pendingInstPackageUrl != null) {
			s_logger.info("Setting system property {} to {}", SYS_PROP_PACKAGE_URL, m_pendingInstPackageUrl);
			System.setProperty(SYS_PROP_PACKAGE_URL, m_pendingInstPackageUrl);
		}
		if (m_pendingInstRequestId != null) {
			s_logger.info("Setting system property {} to {}", SYS_PROP_REQ_ID, m_pendingInstRequestId);
			System.setProperty(SYS_PROP_REQ_ID, String.valueOf(m_pendingInstRequestId));
		}
		if (m_pendingInstRequesterClientId != null) {
			s_logger.info("Setting system property {} to {}", SYS_PROP_REQUESTER_CLIENT_ID, m_pendingInstRequesterClientId);
			System.setProperty(SYS_PROP_REQUESTER_CLIENT_ID, String.valueOf(m_pendingInstRequesterClientId));
		}
		
		m_pendingInstPackageUrl = null;
		m_pendingInstRequestId = null;
		m_pendingInstRequesterClientId = null;
		
		m_bundleContext = null;
	}
	
	@Override
	public void handleEvent(Event event) {
		
		String topic = event.getTopic();
		
		s_logger.info("Received event topic {}", topic);

		String[] propertyNames = event.getPropertyNames();
		
		for (String propertyName : propertyNames) {
			//System.err.println("Event property " + propertyName + ": " + event.getProperty(propertyName));
			s_logger.debug("Event property {}: {}", propertyName, event.getProperty(propertyName));
		}
		
		if (topic.equals(DeploymentAgentService.EVENT_INSTALLED_TOPIC)) {
			
			if (m_pendingInstPackageUrl == null) {
				s_logger.info("Ignore event because no request is pending");
				return;
			}
			
			String packageName = (String) event.getProperty(DeploymentAgentService.EVENT_PACKAGE_NAME);
			String packageVersion = (String) event.getProperty(DeploymentAgentService.EVENT_PACKAGE_VERSION);
			String packageUrl = (String) event.getProperty(DeploymentAgentService.EVENT_PACKAGE_URL);
			Boolean successful = (Boolean) event.getProperty(DeploymentAgentService.EVENT_SUCCESSFUL);
			Exception ex = (Exception) event.getProperty(DeploymentAgentService.EVENT_EXCEPTION);
			
			String successfully = successful ? "Successfully" : "Unsuccessfully";
			
			s_logger.info("{} completed installation of package {}", successfully, packageUrl);
			
			s_logger.info("Responding to command {}...", RESOURCE_INSTALL);
			
			if (m_pendingInstRequestId == null) {
				s_logger.error("Unexpected null request ID associated to package URL {}", m_pendingInstPackageUrl);
				return;
			}

			if (m_pendingInstRequesterClientId == null) {
				s_logger.error("Unexpected null requester client ID associated to package URL {}", m_pendingInstPackageUrl);
				return;
			}
			
			KuraResponsePayload response = null;
			try {
				int responseCode = successful ? KuraResponsePayload.RESPONSE_CODE_OK : KuraResponsePayload.RESPONSE_CODE_ERROR;
				
				response = new KuraResponsePayload(responseCode);
				response.addMetric(METRIC_INSTALL_REPLY_PKG_NAME, packageName);
				response.addMetric(METRIC_INSTALL_REPLY_PKG_VERSION, packageVersion);
				response.setException(ex);
				response.setTimestamp( new Date());
				response.setBody(packageUrl.getBytes("UTF-8"));
			} catch(Exception e) {
				response = new KuraResponsePayload(e); 
				response.setTimestamp(new Date());
				s_logger.error("Error responding to command {} {}", RESOURCE_INSTALL, e);
			}

			try {
				m_cloudClient.controlPublish(m_pendingInstRequesterClientId,
											 "REPLY" + "/" + m_pendingInstRequestId, 
										     response,
										     DFLT_PUB_QOS, 
										     DFLT_RETAIN,
										     DFLT_PRIORITY);
			}
			catch (KuraException e) {
				s_logger.error("Error publishing response for topic {} {}", RESOURCE_INSTALL, e);
			}
			
			m_pendingInstPackageUrl = null;
			m_pendingInstRequestId = null;
			m_pendingInstRequesterClientId = null;
		} else if (topic.equals(DeploymentAgentService.EVENT_UNINSTALLED_TOPIC)) {
			if (m_pendingUninstPackageName == null) {
				s_logger.info("Ignore event because no request is pending");
				return;
			}

			String packageName = (String) event.getProperty(DeploymentAgentService.EVENT_PACKAGE_NAME);
			Boolean successful = (Boolean) event.getProperty(DeploymentAgentService.EVENT_SUCCESSFUL);
			Exception ex = (Exception) event.getProperty(DeploymentAgentService.EVENT_EXCEPTION);

			String successfully = successful ? "Successfully" : "Unsuccessfully";

			s_logger.info("{} completed installation of package {}", successfully, packageName);

			s_logger.info("Responding to command {}...", RESOURCE_UNINSTALL);

			if (m_pendingUninstRequestId == null) {
				s_logger.error("Unexpected null request ID associated to package {}", m_pendingUninstPackageName);
				return;
			}
			
			if (m_pendingUninstRequesterClientId == null) {
				s_logger.error("Unexpected null requester client ID associated to package {}", m_pendingUninstPackageName);
				return;
			}

			KuraResponsePayload response = null;
			try {
				int responseCode = successful ? KuraResponsePayload.RESPONSE_CODE_OK : KuraResponsePayload.RESPONSE_CODE_ERROR;

				response = new KuraResponsePayload(responseCode);
				response.setException(ex);
				response.setTimestamp( new Date());
				response.setBody(packageName.getBytes("UTF-8"));
			} catch(Exception e) {
				response = new KuraResponsePayload(e); 
				response.setTimestamp(new Date());
				s_logger.error("Error responding to command {} {}", RESOURCE_UNINSTALL, e);
			}

			try {
				m_cloudClient.controlPublish(m_pendingUninstRequesterClientId,
											 "REPLY" + "/" + m_pendingUninstRequestId, 
											 response,
											 DFLT_PUB_QOS, 
											 DFLT_RETAIN,
											 DFLT_PRIORITY);
			}
			catch (KuraException e) {
				s_logger.error("Error publishing response for command {} {}", RESOURCE_UNINSTALL, e);
			}

			m_pendingUninstPackageName = null;
			m_pendingUninstRequestId = null;
			m_pendingUninstRequesterClientId = null;
		}
	}

	
	private KuraResponsePayload doExecInstallFromUrl(CloudletTopic requestTopic, KuraRequestPayload request) {
		KuraResponsePayload response = null;
		
		String packageUrl = (String) request.getMetric(METRIC_INSTALL_COMMAND_URL);
		
		if (packageUrl == null) {
			s_logger.error("Package URL parameter missing");
			response = new KuraResponsePayload(KuraResponsePayload.RESPONSE_CODE_BAD_REQUEST);
			response.setTimestamp(new Date());
			try {
				response.setBody("Package URL parameter missing".getBytes("UTF-8"));
			} catch (UnsupportedEncodingException e) {
				// Ignore
			}
			
			return response;
		}
		
		s_logger.info("About to download and install package at URL {}", packageUrl);
		
		try {
			m_pendingInstPackageUrl = packageUrl;
			m_pendingInstRequestId = request.getRequestId();
			m_pendingInstRequesterClientId = request.getRequesterClientId();
						
			s_logger.info("Installing package at URL: " + packageUrl);
			m_deploymentAgentService.installDeploymentPackageAsync(packageUrl);
		} catch (Exception e) {
			
			s_logger.error("Failed to download and install package at URL {}: {}", packageUrl, e);
			
			m_pendingInstPackageUrl = null;
			m_pendingInstRequestId = null;
			m_pendingInstRequesterClientId = null;
			
			response = new KuraResponsePayload(KuraResponsePayload.RESPONSE_CODE_ERROR);
			response.setTimestamp(new Date());
			try {
				response.setBody(e.getMessage().getBytes("UTF-8"));
			} catch (UnsupportedEncodingException uee) {
				// Ignore
			}
		}
		
		return response;
	}
	
	private KuraResponsePayload doExecInstallFromData(CloudletTopic requestTopic, KuraRequestPayload request) {
		KuraResponsePayload response = null;
		
		String packageFilename = (String) request.getMetric(METRIC_INSTALL_COMMAND_FILENAME);
		byte[] packageData = request.getBody();
		
		if (packageFilename == null || packageData == null) {
			s_logger.error("Package filename or data missing");
			response = new KuraResponsePayload(KuraResponsePayload.RESPONSE_CODE_BAD_REQUEST);
			response.setTimestamp(new Date());
			try {
				response.setBody("Package filename or data missing".getBytes("UTF-8"));
			} catch (UnsupportedEncodingException e) {
				// Ignore
			}
			
			return response;
		}
						
		try {
			String filePath = System.getProperty("java.io.tmpdir") + File.separator + packageFilename;
			
			s_logger.info("Writing deployment package data to file {}", filePath);

			String packageUrl = null;
		    FileOutputStream fos = null;
		    try {
		    	fos = new FileOutputStream(filePath);
		    	fos.write(packageData);
		    	
				URL url = new URL("file", null, filePath);
				packageUrl = url.toString();
		    } catch (IOException e) {
		    	throw (e);
		    } finally {
		    	if (fos != null) {
		    		try {
		    			fos.close();
		    		} catch (IOException e) {
		    			// Ignore
		    		}
		    	}
		    }
			
			m_pendingInstPackageUrl = packageUrl;
			m_pendingInstRequestId = request.getRequestId();
			m_pendingInstRequesterClientId = request.getRequesterClientId();
			
			s_logger.info("Installing package...");
			m_deploymentAgentService.installDeploymentPackageAsync(packageUrl);
		} catch (Exception e) {
			
			s_logger.error("Failed to install package {}: {}", packageFilename, e);
			m_pendingInstPackageUrl = null;
			m_pendingInstRequestId = null;
			m_pendingInstRequesterClientId = null;
			
			response = new KuraResponsePayload(KuraResponsePayload.RESPONSE_CODE_ERROR);
			response.setTimestamp(new Date());
			try {
				response.setBody(e.getMessage().getBytes("UTF-8"));
			} catch (UnsupportedEncodingException uee) {
				// Ignore
			}
		}
		
		return response;
	}	
	
	private KuraResponsePayload doExecInstall(CloudletTopic requestTopic, KuraRequestPayload request) {
		KuraResponsePayload response = null;
		
		//
		// We only allow one request at a time
		if (m_pendingInstPackageUrl != null) {
			s_logger.info("Antother request seems still pending: {}. Checking if stale...", m_pendingInstPackageUrl);
			
			boolean isPending = m_deploymentAgentService.isInstallingDeploymentPackage(m_pendingInstPackageUrl);
			if (isPending) {
				s_logger.info("...it isn't");
				response = new KuraResponsePayload(KuraResponsePayload.RESPONSE_CODE_ERROR);
				response.setTimestamp(new Date());
				try {
					response.setBody("Only one request at a time is allowed".getBytes("UTF-8"));
				} catch (UnsupportedEncodingException e) {
					// Ignore
				}
				
				return response;
			}
		}
		
		if (request.getMetric(METRIC_INSTALL_COMMAND_URL) != null) {
			response = doExecInstallFromUrl(requestTopic, request);
		} else {
			response = doExecInstallFromData(requestTopic, request);
		}
		
		return response;
	}
	
	private KuraResponsePayload doExecUninstall(CloudletTopic requestTopic, KuraRequestPayload request) {
		
		KuraResponsePayload response = null;
		
		String packageName = request.getBody() == null ? null : new String(request.getBody());
		
		if (packageName == null) {
			s_logger.error("Package name parameter missing");
			response = new KuraResponsePayload(KuraResponsePayload.RESPONSE_CODE_BAD_REQUEST);
			response.setTimestamp(new Date());
			try {
				response.setBody("Package name parameter missing".getBytes("UTF-8"));
			} catch (UnsupportedEncodingException e) {
				// Ignore
			}
			
			return response;
		}
		
		//
		// We only allow one request at a time
		if (m_pendingUninstPackageName != null) {
			s_logger.info("Antother request seems still pending: {}. Checking if stale...", m_pendingUninstPackageName);
			
			boolean isPending = m_deploymentAgentService.isUninstallingDeploymentPackage(m_pendingUninstPackageName);
			if (isPending) {
				s_logger.info("...it isn't");
				response = new KuraResponsePayload(KuraResponsePayload.RESPONSE_CODE_ERROR);
				response.setTimestamp(new Date());
				try {
					response.setBody("Only one request at a time is allowed".getBytes("UTF-8"));
				} catch (UnsupportedEncodingException e) {
					// Ignore
				}
				
				return response;
			}
		}
		
		s_logger.info("About to uninstall package {}", packageName);
		
		try {
			m_pendingUninstPackageName = packageName;
			m_pendingUninstRequestId = request.getRequestId();
			m_pendingUninstRequesterClientId = request.getRequesterClientId();
						
			s_logger.info("Uninstalling package...");
			m_deploymentAgentService.uninstallDeploymentPackageAsync(packageName);
		} catch (Exception e) {
			
			s_logger.error("Failed to uninstall package {}: {}", packageName, e);
			
			m_pendingUninstPackageName = null;
			m_pendingUninstRequestId = null;
			m_pendingUninstRequesterClientId = null;
			
			response = new KuraResponsePayload(KuraResponsePayload.RESPONSE_CODE_ERROR);
			response.setTimestamp(new Date());
			try {
				response.setBody(e.getMessage().getBytes("UTF-8"));
			} catch (UnsupportedEncodingException uee) {
				// Ignore
			}
		}
		
		return response;
	}
	
	private KuraResponsePayload doGetPackages(KuraPayload request) {
		KuraResponsePayload response = null;
		
		DeploymentPackage[] dps = m_deploymentAdmin.listDeploymentPackages();
		XmlDeploymentPackages xdps = new XmlDeploymentPackages();
		XmlDeploymentPackage[] axdp = new XmlDeploymentPackage[dps.length];
		
		for (int i = 0; i < dps.length; i++) {
			DeploymentPackage dp = dps[i];
			
			XmlDeploymentPackage xdp = new XmlDeploymentPackage();
			xdp.setName(dp.getName());
			xdp.setVersion(dp.getVersion().toString());
			
			BundleInfo[] bis = dp.getBundleInfos();
			XmlBundleInfo[] axbi = new XmlBundleInfo[bis.length];
			
			for (int j = 0; j < bis.length; j++) {
				
				BundleInfo bi = bis[j];
				XmlBundleInfo xbi = new XmlBundleInfo();
				xbi.setName(bi.getSymbolicName());
				xbi.setVersion(bi.getVersion().toString());
				
				axbi[j] = xbi;
			}
			
			xdp.setBundleInfos(axbi);
			
			axdp[i] = xdp;
		}
		
		xdps.setDeploymentPackages(axdp);
		
		try {
			String s = XmlUtil.marshal(xdps);
			
			s_logger.info("Getting resource {}: {}", RESOURCE_PACKAGES, s);
			
			response = new KuraResponsePayload(KuraResponsePayload.RESPONSE_CODE_OK); 
			response.setTimestamp(new Date());
			
			try {
				response.setBody(s.getBytes("UTF-8"));
			} catch (UnsupportedEncodingException e) {
				// Ignore
			}
		} catch (JAXBException e) {
			s_logger.error("Error getting resource {}: {}", RESOURCE_PACKAGES, e);
		}
		
		return response;
	}

	private KuraResponsePayload doGetBundles(KuraPayload request) {
		KuraResponsePayload response = null;

		Bundle[] bundles = m_bundleContext.getBundles();
		XmlBundles xmlBundles = new XmlBundles();
		XmlBundle[] axb = new XmlBundle[bundles.length];
		
		for (int i = 0; i < bundles.length; i++) {
			
			Bundle bundle = bundles[i];
			XmlBundle xmlBundle = new XmlBundle();
			
			xmlBundle.setName(bundle.getSymbolicName());
			xmlBundle.setVersion(bundle.getVersion().toString());
			xmlBundle.setId(bundle.getBundleId());
			
			int state = bundle.getState();
			
			switch(state) {
			case Bundle.UNINSTALLED:
				xmlBundle.setState("UNINSTALLED");
				break;
				
			case Bundle.INSTALLED:
				xmlBundle.setState("INSTALLED");
				break;
			
			case Bundle.RESOLVED:
				xmlBundle.setState("RESOLVED");
				break;
				
			case Bundle.STARTING:
				xmlBundle.setState("STARTING");
				break;
				
			case Bundle.STOPPING:
				xmlBundle.setState("STOPPING");
				break;
				
			case Bundle.ACTIVE:
				xmlBundle.setState("ACTIVE");
				break;
				
			default:
				xmlBundle.setState(String.valueOf(state));
			}
			
			axb[i] = xmlBundle;
		}
		
		xmlBundles.setBundles(axb);

		try {
			String s = XmlUtil.marshal(xmlBundles);
			
			s_logger.info("Getting resource {}: {}", RESOURCE_BUNDLES, s);
			
			response = new KuraResponsePayload(KuraResponsePayload.RESPONSE_CODE_OK); 
			response.setTimestamp(new Date());
			
			try {
				response.setBody(s.getBytes("UTF-8"));
			} catch (UnsupportedEncodingException e) {
				// Ignore
			}
		} catch (JAXBException e) {
			s_logger.error("Error getting resource {}: {}", RESOURCE_BUNDLES, e);
		}

		
		return response;
	}
	
	private KuraResponsePayload doExecStartStopBundle(CloudletTopic requestTopic, KuraPayload request, boolean start) {
		
		KuraResponsePayload response = null;
		
		String bundleId = requestTopic.getResources()[1];
		
		if (bundleId == null) {
			s_logger.info("EXEC start/stop bundle: null bundle ID");
			
			response = new KuraResponsePayload(
					KuraResponsePayload.RESPONSE_CODE_BAD_REQUEST);
			
			response.setTimestamp(new Date());
		} else {
			Long id = null;
			try {
				id = Long.valueOf(bundleId);
			} catch (NumberFormatException e){
				
				s_logger.error("EXEC start/stop bundle: bad bundle ID format: {}", e);
				response = new KuraResponsePayload(KuraResponsePayload.RESPONSE_CODE_BAD_REQUEST);
				response.setTimestamp(new Date());
				response.setExceptionMessage(e.getMessage());

				response.setExceptionStack(ThrowableUtil.stackTraceAsString(e));
			}
			
			if (id != null) {
				
				s_logger.info("Executing command {}", start ? RESOURCE_START : RESOURCE_STOP);
				
				Bundle bundle = m_bundleContext.getBundle(id);
				if (bundle == null) {
					s_logger.error("Bundle ID {} not found", id);
					response = new KuraResponsePayload(KuraResponsePayload.RESPONSE_CODE_NOTFOUND);
					response.setTimestamp(new Date());
				} else {
					try {
						if (start) {
							bundle.start();
						} else {
							bundle.stop();
						}
						s_logger.info("{} bundle ID {} ({})", new Object[] {start ? "Started" : "Stopped", id, bundle.getSymbolicName()});
						response = new KuraResponsePayload(KuraResponsePayload.RESPONSE_CODE_OK);
						response.setTimestamp(new Date());
					} catch (BundleException e) {
						s_logger.error("Failed to {} bundle {}: {}", new Object[] {start ? "start" : "stop", id, e});
						response = new KuraResponsePayload(KuraResponsePayload.RESPONSE_CODE_ERROR);
						response.setTimestamp(new Date());
					}
				}				
			}
		}
	
		return response;
	}
	
	KuraResponsePayload doGetResource(CloudletTopic requestTopic, KuraPayload request) {
		KuraResponsePayload response = null;
		
		String resourceName = requestTopic.getResources()[0];
		
		if (resourceName == null) {

			s_logger.info("GET: null resource name");
			
			response = new KuraResponsePayload(
					KuraResponsePayload.RESPONSE_CODE_BAD_REQUEST);
			
			response.setTimestamp(new Date());

		} else if (resourceName.equals(RESOURCE_PACKAGES)) {
			response = doGetPackages(request);
		} else if (resourceName.equals(RESOURCE_BUNDLES)) {
			response = doGetBundles(request);
		} else {
			s_logger.info("Resource {} not found", resourceName);
			
			response = new KuraResponsePayload(
					KuraResponsePayload.RESPONSE_CODE_NOTFOUND);
			
			response.setTimestamp(new Date());
		}
		
		return response;
	}
	
	KuraResponsePayload doExecCommand(CloudletTopic requestTopic, KuraRequestPayload request) {
		KuraResponsePayload response = null;
		
		String commandName = requestTopic.getResources()[0];

		if (commandName == null) {
			
			s_logger.info("EXEC: null command name");
			
			response = new KuraResponsePayload(
					KuraResponsePayload.RESPONSE_CODE_BAD_REQUEST);
			
			response.setTimestamp(new Date());

		} else if (commandName.equals(RESOURCE_INSTALL)) {
			response = doExecInstall(requestTopic, request);
		} else if (commandName.equals(RESOURCE_UNINSTALL)) {
			response = doExecUninstall(requestTopic, request);
		} else if (commandName.equals(RESOURCE_START)) {
			response = doExecStartStopBundle(requestTopic, request, true);
		} else if (commandName.equals(RESOURCE_STOP)) {
			response = doExecStartStopBundle(requestTopic, request, false);
		} else {
			s_logger.info("Command {} not found", commandName);
			
			response = new KuraResponsePayload(
					KuraResponsePayload.RESPONSE_CODE_NOTFOUND);
			
			response.setTimestamp(new Date());			
		}
		
		return response;
	}
	
	@Override
	public void onControlMessageArrived(String deviceId, 
									    String appTopic,
								        KuraPayload msg, 
								        int qos, 
								        boolean retain) 
	{		
		if (appTopic.startsWith("REPLY")) {
			// Ignore
			return;
		}
		
		KuraRequestPayload reqPayload;
		try {
			reqPayload = KuraRequestPayload.buildFromKuraPayload(msg);
		} catch (ParseException e) {
			s_logger.error("Error building request payload for topic: {}", appTopic);
			return;
		}
		
		KuraResponsePayload response = null;
		try {
			CloudletTopic requestTopic = CloudletTopic.parseAppTopic(appTopic);		
			Method method = requestTopic.getMethod();
			switch (method) {
			case GET:
				response = doGetResource(requestTopic, reqPayload);
				break;
				
			case EXEC:
				response = doExecCommand(requestTopic, reqPayload);
				break;
			
			default:
				s_logger.error("invalid operation {}", method);			
				response = new KuraResponsePayload(KuraResponsePayload.RESPONSE_CODE_BAD_REQUEST);			
				response.setTimestamp(new Date());
				break;
			}
		}
		catch (IllegalArgumentException e) {
			s_logger.error("invalid operation {}", appTopic);			
			response = new KuraResponsePayload(KuraResponsePayload.RESPONSE_CODE_BAD_REQUEST);			
			response.setTimestamp(new Date());
		}
		
		if (response != null) {
			try {
				String requestId = reqPayload.getRequestId();
				String requesterClientId = reqPayload.getRequesterClientId();
				
				m_cloudClient.controlPublish(requesterClientId,
						                     "REPLY" + "/" + requestId, 
										     response,
										     DFLT_PUB_QOS,
										     DFLT_RETAIN,
										     DFLT_PRIORITY);
			}
			catch (KuraException e) {
				s_logger.error("Error publishing response for topic {}: {}", appTopic, e);
			}
		}
	}

	@Override
	public void onMessageArrived(String deviceId, 
								 String appTopic,
								 KuraPayload msg, 
								 int qos, 
								 boolean retain) 
	{
		s_logger.warn("publishArrived on semantic topic {}. Should have never happened.", appTopic);
	}

	@Override
	public void onConnectionLost() {
		s_logger.info("connectionLost");
	}

	@Override
	public void onConnectionEstablished() {
		s_logger.info("connectionRestored");
	}

	@Override
	public void onMessagePublished(int messageId, String topic) {
		// Ignore
	}

	@Override
	public void onMessageConfirmed(int messageId, String topic) {
		// Ignore
	}

}

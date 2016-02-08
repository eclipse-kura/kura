/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.core.configuration;

import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloud.CloudService;
import org.eclipse.kura.cloud.Cloudlet;
import org.eclipse.kura.cloud.CloudletTopic;
import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.core.configuration.util.XmlUtil;
import org.eclipse.kura.message.KuraPayload;
import org.eclipse.kura.message.KuraRequestPayload;
import org.eclipse.kura.message.KuraResponsePayload;
import org.eclipse.kura.system.SystemService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CloudConfigurationHandler extends Cloudlet 
{
	private static Logger s_logger = LoggerFactory.getLogger(CloudConfigurationHandler.class);
	
	public static final String APP_ID = "CONF-V1";
	
	/* GET or PUT */
	public static final String RESOURCE_CONFIGURATIONS = "configurations";
	/* GET */
	public static final String RESOURCE_SNAPSHOTS      = "snapshots";
	/* EXEC */
	public static final String RESOURCE_SNAPSHOT       = "snapshot";
	public static final String RESOURCE_ROLLBACK       = "rollback";

	private SystemService m_systemService;
	
	private ScheduledExecutorService m_executor;
	
	//
	// ServiceTracker to track the CloudService
	private class ServiceTrackerAdapter extends ServiceTracker<CloudService,CloudService> 
	{
		private ServiceTrackerAdapter(BundleContext context) {
			super(context, CloudService.class, null);
		}
		
		public CloudService addingService(ServiceReference<CloudService> ref) {
			CloudService cloudService = (CloudService) context.getService(ref);
			
			// Explicitly call dependency injection
			setCloudService(cloudService);
			activate(null);

			return cloudService;
		}

		public void removedService(ServiceReference<CloudService> ref, CloudService service) {
			// Explicitly call dependency injection
			deactivate(null);
			unsetCloudService(null);
		}
		
		public void close() {
			// Explicitly call dependency injection
			deactivate(null);
			unsetCloudService(null);
			super.close();
		}
	}
	
	private ServiceTrackerAdapter m_serviceTrackerAdapter;
	private ConfigurationServiceImpl m_configService;
	
	public CloudConfigurationHandler(BundleContext context,
			ConfigurationServiceImpl configService,
			SystemService systemService) 
	{
		super(APP_ID);
		m_serviceTrackerAdapter = new ServiceTrackerAdapter(context);
		m_configService = configService;
		m_systemService = systemService;
	}

	public void open() {
		m_executor = Executors.newSingleThreadScheduledExecutor();
		m_serviceTrackerAdapter.open();
	}

	public void close() {
		m_serviceTrackerAdapter.close();
		m_executor.shutdownNow();
	}
	
	@Override
	protected void doGet(CloudletTopic reqTopic, KuraRequestPayload reqPayload,
			KuraResponsePayload respPayload) throws KuraException {
		
		String resources[] = reqTopic.getResources();
		
		if (resources == null || resources.length == 0) {
			s_logger.error("Bad request topic: {}", reqTopic.toString()); 
			s_logger.error("Expected one resource but found {}", resources !=null ? resources.length: "none");
			respPayload.setResponseCode(KuraResponsePayload.RESPONSE_CODE_BAD_REQUEST);
			return;
		}
			
		if (resources[0].equals(RESOURCE_CONFIGURATIONS)) {
			doGetConfigurations(reqTopic, reqPayload, respPayload);
		} else if (resources[0].equals(RESOURCE_SNAPSHOTS)) {
			doGetSnapshots(reqTopic, reqPayload, respPayload);
		} else {
			s_logger.error("Bad request topic: {}", reqTopic.toString());
			s_logger.error("Cannot find resource with name: {}", resources[0]);
			respPayload.setResponseCode(KuraResponsePayload.RESPONSE_CODE_NOTFOUND);
			return;
		}
	}

	@Override
	protected void doPut(CloudletTopic reqTopic, KuraRequestPayload reqPayload,
			KuraResponsePayload respPayload) throws KuraException {
		
		String resources[] = reqTopic.getResources();
		
		if (resources == null || resources.length == 0) {
			s_logger.error("Bad request topic: {}", reqTopic.toString()); 
			s_logger.error("Expected one resource but found {}", resources !=null ? resources.length: "none");
			respPayload.setResponseCode(KuraResponsePayload.RESPONSE_CODE_BAD_REQUEST);
			return;
		}

		if (resources[0].equals(RESOURCE_CONFIGURATIONS)) {
			doPutConfigurations(reqTopic, reqPayload, respPayload);
		} else {
			s_logger.error("Bad request topic: {}", reqTopic.toString());
			s_logger.error("Cannot find resource with name: {}", resources[0]);
			respPayload.setResponseCode(KuraResponsePayload.RESPONSE_CODE_NOTFOUND);
			return;
		}
	}

	@Override
	protected void doExec(CloudletTopic reqTopic, KuraRequestPayload reqPayload,
			KuraResponsePayload respPayload) throws KuraException {
		
		String[] resources = reqTopic.getResources();
		
		if (resources == null || resources.length == 0) {
			s_logger.error("Bad request topic: {}", reqTopic.toString()); 
			s_logger.error("Expected one resource but found {}", resources !=null ? resources.length: "none");
			respPayload.setResponseCode(KuraResponsePayload.RESPONSE_CODE_BAD_REQUEST);
			return;
		}
		
		
		if (resources[0].equals(RESOURCE_SNAPSHOT)) {
			doExecSnapshot(reqTopic, reqPayload, respPayload);
		} else if (resources[0].equals(RESOURCE_ROLLBACK)) {
			doExecRollback(reqTopic, reqPayload, respPayload);
		} else {
			s_logger.error("Bad request topic: {}", reqTopic.toString());
			s_logger.error("Cannot find resource with name: {}", resources[0]);
			respPayload.setResponseCode(KuraResponsePayload.RESPONSE_CODE_NOTFOUND);
			return;
		}
	}	
	
	private void doGetSnapshots(CloudletTopic reqTopic,
			KuraPayload reqPayload, KuraResponsePayload respPayload) throws KuraException {

		String[] resources = reqTopic.getResources();
		
		if (resources.length > 2) {
			s_logger.error("Bad request topic: {}", reqTopic.toString()); 
			s_logger.error("Expected one or two resource(s) but found {}", resources.length);
			respPayload.setResponseCode(KuraResponsePayload.RESPONSE_CODE_BAD_REQUEST);
			return;
		}
		
		String snapshotId = resources.length == 2 ? resources[1] : null;
		
		if (snapshotId != null) {
			long sid = Long.parseLong(snapshotId);
			XmlComponentConfigurations xmlConfigs = m_configService.loadEncryptedSnapshotFileContent(sid);
			//
			// marshall the response	
			
			//List<ComponentConfigurationImpl> decryptedConfigs = new ArrayList<ComponentConfigurationImpl>();
			List<ComponentConfigurationImpl> configs = xmlConfigs.getConfigurations();
			for (ComponentConfigurationImpl config : configs) {
				if (config != null) {
					try {
						Map<String,Object> decryptedProperties= m_configService.decryptPasswords(config);
						config.setProperties(decryptedProperties);
						//decryptedConfigs.add(config);
					}
					catch (Throwable t) {
						s_logger.warn("Error during snapshot password decryption");
					}
				}
			}
			//xmlConfigs.setConfigurations(decryptedConfigs);
			
			
			
			
			byte[] body = toResponseBody(xmlConfigs);
			
			//
			// Build payload
			respPayload.setBody(body);
		} 
		else {		
			// get the list of snapshot IDs and put them into a response object
			Set<Long> sids = null;
			try {
				sids = m_configService.getSnapshots();
			} catch (KuraException e) {
				s_logger.error("Error listing snapshots: {}", e);
				throw new KuraException(KuraErrorCode.CONFIGURATION_SNAPSHOT_LISTING, e);
			}
			List<Long> snapshotIds = new ArrayList<Long>(sids);
			XmlSnapshotIdResult xmlResult = new XmlSnapshotIdResult();
			xmlResult.setSnapshotIds(snapshotIds);

			//
			// marshall the response		
			byte[] body = toResponseBody(xmlResult);

			//
			// Build payload
			respPayload.setBody(body);
		}
	}
	

	private void doGetConfigurations(CloudletTopic reqTopic,
							         KuraPayload reqPayload, 
							         KuraResponsePayload respPayload) 
		throws KuraException 
	{	
		String[] resources = reqTopic.getResources();		
		if (resources.length > 2) {
			s_logger.error("Bad request topic: {}", reqTopic.toString()); 
			s_logger.error("Expected at most two resource(s) but found {}", resources.length);
			respPayload.setResponseCode(KuraResponsePayload.RESPONSE_CODE_BAD_REQUEST);
			return;
		}
		String pid = resources.length == 2 ? resources[1] : null;
		
		//
		// get current configuration with descriptors
		List<ComponentConfigurationImpl> configs = new ArrayList<ComponentConfigurationImpl>();		
		try {
			
			if (pid == null) {
				List<String> pidsToIgnore = m_systemService.getDeviceManagementServiceIgnore();
				
				// the configuration for all components has been requested
				Set<String> componentPids = m_configService.getConfigurableComponentPids();
				for(String componentPid : componentPids) {
					boolean skip = false;
					if(pidsToIgnore != null && !pidsToIgnore.isEmpty()) {
						for(String pidToIgnore : pidsToIgnore) {
							if(componentPid.equals(pidToIgnore)) {
								skip=true;
								break;
							}
						}
					}
					if(skip) {
						continue;
					}
					
					ComponentConfiguration cc = m_configService.getComponentConfiguration(componentPid);
					
					// TODO: define a validate method for ComponentConfiguration
					if (cc == null) {
						s_logger.error("null ComponentConfiguration");
						continue;
					}
					if (cc.getPid() == null || cc.getPid().isEmpty()) {
						s_logger.error("null or empty ComponentConfiguration PID");
						continue;
					}
					if (cc.getDefinition() == null) {
						s_logger.error("null OCD for ComponentConfiguration PID {}", cc.getPid());
						continue;
					}
					if (cc.getDefinition().getId() == null || cc.getDefinition().getId().isEmpty()) {
						
						s_logger.error("null or empty OCD ID for ComponentConfiguration PID {}. OCD ID: {}", cc.getPid(), cc.getDefinition().getId());
						continue;
					}
					configs.add((ComponentConfigurationImpl) cc);
				}
			}
			else {

				// the configuration for a specific component has been requested.
				ComponentConfiguration cc = m_configService.getComponentConfiguration(pid);
				if (cc != null) {
					configs.add((ComponentConfigurationImpl) cc);
				}
			}
		} catch (KuraException e) {
			s_logger.error("Error getting component configurations: {}", e);
			throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR, e, "Error getting component configurations");
		}
		
		XmlComponentConfigurations xmlConfigs = new XmlComponentConfigurations();
		xmlConfigs.setConfigurations(configs);
		
		//
		// marshall
		byte[] body = toResponseBody(xmlConfigs);
		
		//
		// Build response payload
		respPayload.setBody(body);	
	}


	private void doPutConfigurations(CloudletTopic reqTopic,
			KuraPayload reqPayload, KuraResponsePayload respPayload) 
	{
		String[] resources = reqTopic.getResources();
		
		if (resources.length > 2) {
			s_logger.error("Bad request topic: {}", reqTopic.toString()); 
			s_logger.error("Expected at most two resource(s) but found {}", resources.length);
			respPayload.setResponseCode(KuraResponsePayload.RESPONSE_CODE_BAD_REQUEST);
			return;
		}
		
		String pid = resources.length == 2 ? resources[1] : null;
		
		XmlComponentConfigurations xmlConfigs = null;
		try {

			// unmarshall the response
			if (reqPayload.getBody() == null || reqPayload.getBody().length == 0) {
				throw new IllegalArgumentException("body"); 
			}

			String s = new String(reqPayload.getBody(), "UTF-8");
			s_logger.info("Received new Configuration");
			
			StringReader sr = new StringReader(s);
			xmlConfigs = XmlUtil.unmarshal(sr, XmlComponentConfigurations.class);
		}
		catch (Exception e) {
			s_logger.error("Error unmarshalling the request body: {}", e);
			respPayload.setResponseCode(KuraResponsePayload.RESPONSE_CODE_BAD_REQUEST);
			respPayload.setException(e);
			return;
		}

		m_executor.schedule(new UpdateConfigurationsCallable(pid, xmlConfigs, m_configService),
				            1000, TimeUnit.MILLISECONDS);
	}


	
	private void doExecRollback(CloudletTopic reqTopic,
			KuraPayload reqPayload, KuraResponsePayload respPayload) throws KuraException {
		
		String[] resources = reqTopic.getResources();

		if (resources.length > 2) {
			s_logger.error("Bad request topic: {}", reqTopic.toString()); 
			s_logger.error("Expected at most two resource(s) but found {}", resources !=null ? resources.length: "none");
			respPayload.setResponseCode(KuraResponsePayload.RESPONSE_CODE_BAD_REQUEST);
			return;
		}
		
		String snapshotId = resources.length == 2 ? resources[1] : null;
		Long sid;
		try {
			sid = snapshotId != null ? Long.parseLong(snapshotId) : null;
		} catch (NumberFormatException e) {
			s_logger.error("Bad numeric numeric format for snapshot ID: {}", snapshotId);
			respPayload.setResponseCode(KuraResponsePayload.RESPONSE_CODE_BAD_REQUEST);
			return;
		}
		
		m_executor.schedule(new RollbackCallable(sid, m_configService),
				            1000, TimeUnit.MILLISECONDS);
	}

	private void doExecSnapshot(CloudletTopic reqTopic,
			KuraPayload reqPayload, KuraResponsePayload respPayload) throws KuraException {
		
		String[] resources = reqTopic.getResources(); 
		
		if (resources.length > 1) {
			s_logger.error("Bad request topic: {}", reqTopic.toString()); 
			s_logger.error("Expected one resource(s) but found {}", resources !=null ? resources.length: "none");
			respPayload.setResponseCode(KuraResponsePayload.RESPONSE_CODE_BAD_REQUEST);
			return;
		}
		
		// take a new snapshot and get the id
		long snapshotId;
		try {
			snapshotId = m_configService.snapshot();
		} catch (KuraException e) {
			s_logger.error("Error taking snapshot: {}", e);
			throw new KuraException(KuraErrorCode.CONFIGURATION_SNAPSHOT_TAKING, e);
		}
		List<Long> snapshotIds = new ArrayList<Long>();
		snapshotIds.add(snapshotId);
		XmlSnapshotIdResult xmlResult = new XmlSnapshotIdResult();
		xmlResult.setSnapshotIds(snapshotIds);
		
		byte[] body = toResponseBody(xmlResult);
		
		respPayload.setBody(body);
	}

	private static byte[] toResponseBody(Object o) throws KuraException {
		//
		// marshall the response
		StringWriter sw = new StringWriter();
		try {
			XmlUtil.marshal(o, sw);
		} catch (Exception e) {
			s_logger.error("Error marshalling snapshots: {}", e);
			throw new KuraException(KuraErrorCode.CONFIGURATION_SNAPSHOT_LOADING, e);
		}
		
		byte[] body = null;
		try {
			body = sw.toString().getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			s_logger.error("Error encoding response body: {}", e);
			throw new KuraException(KuraErrorCode.CONFIGURATION_SNAPSHOT_LOADING, e);
		}
		
		return body;
	}
}

class UpdateConfigurationsCallable implements Callable<Void> {
	
	private static Logger s_logger = LoggerFactory.getLogger(UpdateConfigurationsCallable.class);
	
	private String m_pid;
	private XmlComponentConfigurations m_xmlConfigurations;
	private ConfigurationServiceImpl m_configurationService;
	
	public UpdateConfigurationsCallable(String pid,
			                            XmlComponentConfigurations xmlConfigurations,
			                            ConfigurationServiceImpl configurationService) {
		m_pid = pid;
		m_xmlConfigurations = xmlConfigurations;
		m_configurationService = configurationService;
	}
	
	@Override
	public Void call() throws Exception {
		
		s_logger.info("Updating configurations");
		Thread.currentThread().setName(getClass().getSimpleName());
		//                                                                                                                      
		// update the configuration
		try {
			List<ComponentConfigurationImpl> configImpls = m_xmlConfigurations != null ? m_xmlConfigurations.getConfigurations() : null;
			if (configImpls == null) {
				return null;
			}

			List<ComponentConfiguration> configs = new ArrayList<ComponentConfiguration>();
			configs.addAll(configImpls);

			if (m_pid == null) {
				// update all the configurations provided
				m_configurationService.updateConfigurations(configs);
			}
			else {
				// update only the configuration with the provided id                                                           
				for (ComponentConfiguration config : configs) {
					if (m_pid.equals(config.getPid())) {
						m_configurationService.updateConfiguration(m_pid, config.getConfigurationProperties());
					}
				}
			}
		} catch (KuraException e) {
			s_logger.error("Error updating configurations: {}", e);
			throw new KuraException(KuraErrorCode.CONFIGURATION_UPDATE, e);
		}
		
		return null;
	}
}

class RollbackCallable implements Callable<Void> {
	
	private static Logger s_logger = LoggerFactory.getLogger(RollbackCallable.class);
	
	private Long m_snapshotId;
	private ConfigurationServiceImpl m_configurationService;
	
	public RollbackCallable(Long snapshotId, ConfigurationServiceImpl configurationService) {
		super();
		m_snapshotId = snapshotId;
		m_configurationService = configurationService;
	}

	@Override
	public Void call() throws Exception {
		Thread.currentThread().setName(getClass().getSimpleName());
		// rollback to the specified snapshot if any
		try {
			if (m_snapshotId == null) {
				m_configurationService.rollback();
			}
			else {
				m_configurationService.rollback(m_snapshotId);
			}
		} catch (KuraException e) {
			s_logger.error("Error rolling back to snapshot: {}", e);
			throw new KuraException(KuraErrorCode.CONFIGURATION_ROLLBACK, e);
		}
		
		return null;
	}
	
}
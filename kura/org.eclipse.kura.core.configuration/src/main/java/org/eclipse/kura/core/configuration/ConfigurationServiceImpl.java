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
package org.eclipse.kura.core.configuration;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.stream.FactoryConfigurationError;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.KuraPartialSuccessException;
import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.configuration.SelfConfiguringComponent;
import org.eclipse.kura.configuration.metatype.AD;
import org.eclipse.kura.configuration.metatype.OCD;
import org.eclipse.kura.configuration.metatype.Scalar;
import org.eclipse.kura.core.configuration.metatype.Tocd;
import org.eclipse.kura.core.configuration.util.CollectionsUtil;
import org.eclipse.kura.core.configuration.util.ComponentUtil;
import org.eclipse.kura.core.configuration.util.StringUtil;
import org.eclipse.kura.core.configuration.util.XmlUtil;
import org.eclipse.kura.crypto.CryptoService;
import org.eclipse.kura.system.SystemService;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.ConfigurationListener;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentException;
import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.MetaTypeService;
import org.osgi.service.metatype.ObjectClassDefinition;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of ConfigurationService. 
 */
public class ConfigurationServiceImpl implements ConfigurationService, ConfigurationListener
{
	private static final Logger s_logger = LoggerFactory.getLogger(ConfigurationServiceImpl.class);
	
	private ComponentContext             m_ctx;
	private CloudConfigurationHandler    m_cloudHandler;
	private ServiceTracker<?,?>          m_serviceTracker;
	private BundleTracker<Bundle>        m_bundleTracker;

	@SuppressWarnings("unused")
	private MetaTypeService     m_metaTypeService;
	private ConfigurationAdmin  m_configurationAdmin;
	private SystemService 		m_systemService;
	@SuppressWarnings("unused")
	private CryptoService		m_cryptoService;
	
	// contains all the PIDs - both of regular and self components 
	private Set<String> m_allPids;
	
	// contains the self configuring components ONLY!
	private Set<String> m_selfConfigComponents;
	
	// contains the current configuration of regular components ONLY
	private Map<String,Tocd> m_ocds;
	
	// contains all pids which have been configured and for which we have not received the corresponding ConfigurationEvent yet
	private Set<String> m_pendingConfigurationPids;

	// ----------------------------------------------------------------
	//
	//   Dependencies
	//
	// ----------------------------------------------------------------

	public void setConfigurationAdmin(ConfigurationAdmin configAdmin) {
		this.m_configurationAdmin = configAdmin;
	}

	public void unsetConfigurationAdmin(ConfigurationAdmin configAdmin) {
		this.m_configurationAdmin = null;
	}

	public void setMetaTypeService(MetaTypeService metaTypeService) {
		this.m_metaTypeService = metaTypeService;
	}

	public void unsetMetaTypeService(MetaTypeService metaTypeService) {
		this.m_metaTypeService = null;
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

	public ConfigurationServiceImpl()
	{
		m_allPids                  = new HashSet<String>();
		m_selfConfigComponents     = new HashSet<String>();
		m_pendingConfigurationPids = new HashSet<String>();
		m_ocds                     = new HashMap<String,Tocd>();
	}
	
	
	// ----------------------------------------------------------------
	//
	//   Activation APIs
	//
	// ----------------------------------------------------------------
	
	protected void activate(ComponentContext componentContext) throws InvalidSyntaxException 
	{
		s_logger.info("activate...");

		// save the bundle context
		m_ctx = componentContext;

		// 1. Register the ConfigurationListener to 
		// monitor Configuration updates
		m_ctx.getBundleContext().registerService(ConfigurationListener.class.getName(), this, null); 

		// 2. Load the latest snapshot and push it to ConfigurationAdmin
		try {
			loadLatestSnapshotInConfigAdmin();
		}
		catch (Exception e) {
			throw new ComponentException("Error loading latest snapshot", e);
		}
		
		//
		// start the trackers
		s_logger.info("Trackers being opened...");
		m_cloudHandler = new CloudConfigurationHandler(m_ctx.getBundleContext(), this, m_systemService);
		m_cloudHandler.open();
				
		m_serviceTracker = new ConfigurableComponentTracker(m_ctx.getBundleContext(), this);
		m_serviceTracker.open(true);

		m_bundleTracker = new ComponentMetaTypeBundleTracker(m_ctx.getBundleContext(),m_configurationAdmin, this);
		m_bundleTracker.open();
	}
	
	
	protected void deactivate(ComponentContext componentContext) 
	{
		s_logger.info("deactivate...");
		
		//
		// stop the trackers
		if (m_cloudHandler != null) {
			m_cloudHandler.close();
		}
		if (m_serviceTracker != null) {
			m_serviceTracker.close();
		}
		if (m_bundleTracker != null) {
			m_bundleTracker.close();
		}
	}

	
	// ----------------------------------------------------------------
	//
	//   ConfigurationListener
	//
	// ----------------------------------------------------------------

	@Override
	public synchronized void configurationEvent(ConfigurationEvent event) 
	{
		// Called every time a new service configuration is invoked
		// we need to take a new snapshot every time this happens
		String pid = event.getPid();		
		if (m_pendingConfigurationPids.contains(pid)) {

			// ignore the ConfigurationEvent for those PIDs whose 
			// configuration update was by the ConfigurationService itself
			m_pendingConfigurationPids.remove(pid);
			return;
		}
		try {
			if (m_allPids.contains(pid)) {			
				// Take a new snapshot
				s_logger.info("ConfigurationEvent for tracked ConfigurableComponent with pid: {}", pid);
				snapshot();
			}
		}
		catch (Exception e) { 
			s_logger.error("Error taking snapshot after ConfigurationEvent", e);
		}
	}
	
	
	// ----------------------------------------------------------------
	//
	//   Service APIs
	//
	// ----------------------------------------------------------------

	public boolean hasConfigurableComponent(String pid) {
		return m_allPids.contains(pid);
	}

	
	@Override
	public Set<String> getConfigurableComponentPids() {
		if (m_allPids.isEmpty()) {
			return Collections.emptySet();
		}
		return Collections.unmodifiableSet(m_allPids);
	}

	
	@Override
	public List<ComponentConfiguration> getComponentConfigurations()
		throws KuraException
	{
		List<ComponentConfiguration> configs = new ArrayList<ComponentConfiguration>();

		// assemble all the configurations we have
		// clone the list to avoid concurrent modifications
		List<String> allPids = new ArrayList<String>(m_allPids);
		ComponentConfiguration cc = null;
 		for (String pid : allPids) { 
			try {
				cc = null;
				if (!m_selfConfigComponents.contains(pid)) {
					cc = getConfigurableComponentConfiguration(pid);
				}
				else {
					cc = getSelfConfiguringComponentConfiguration(pid);
				}
				if (cc != null) {
					configs.add(cc);
				}
			} 
			catch (Exception e) {
				s_logger.error("Error getting configuration for component "+pid, e);
				throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR, e, "Error getting configuration for component "+pid);
			}
		}
		return configs;
	}

	
	@Override
	public ComponentConfiguration getComponentConfiguration(String pid) 
		throws KuraException
	{
		ComponentConfiguration cc = null;
		if (!m_selfConfigComponents.contains(pid)) {
			cc = getConfigurableComponentConfiguration(pid);
		}
		else {
			cc = getSelfConfiguringComponentConfiguration(pid);
		}
		return cc;
	}

	
	@Override
	public synchronized void updateConfiguration(String pidToUpdate, 
			                                     Map<String,Object> propertiesToUpdate)
		throws KuraException
	{		
		// Update the component configuration
		boolean snapshotOnConfirmation = false;
		
		updateConfigurationInternal(pidToUpdate, propertiesToUpdate, snapshotOnConfirmation);

		// Build the current configuration
		ComponentConfiguration cc = null; 
		ComponentConfigurationImpl cci = null; 
		Map<String,Object> props = null;
		List<ComponentConfigurationImpl> configs = new ArrayList<ComponentConfigurationImpl>();

		// clone the list to avoid concurrent modifications
		List<String> allPids = new ArrayList<String>(m_allPids);
		for (String pid : allPids) {
			if(pid.equals(pidToUpdate)) {
				cci   = new ComponentConfigurationImpl(pid, null, propertiesToUpdate);
				configs.add((ComponentConfigurationImpl) cci);
			} else {
				if (!m_selfConfigComponents.contains(pid)) {
					cc = getConfigurableComponentConfiguration(pid);
				}
				else {
					cc = getSelfConfiguringComponentConfiguration(pid);
				}
				if (cc != null && cc.getPid() != null && cc.getPid().equals(pid)) {
					props = cc.getConfigurationProperties();
					cci   = new ComponentConfigurationImpl(pid, null, props);
					configs.add((ComponentConfigurationImpl) cci);
				}
			}
		}

		saveSnapshot(configs);
	}


	@Override
	public long snapshot() 
		throws KuraException
	{
		s_logger.info("Writing snapshot - Getting component configurations...");

		//
		// Build the current configuration
		ComponentConfiguration cc = null; 
		ComponentConfigurationImpl cci = null; 
		Map<String,Object> props = null;
		List<ComponentConfigurationImpl> configs = new ArrayList<ComponentConfigurationImpl>();

		// clone the list to avoid concurrent modifications
		List<String> allPids = new ArrayList<String>(m_allPids);
		for (String pid : allPids) {

			if (!m_selfConfigComponents.contains(pid)) {
				cc = getConfigurableComponentConfiguration(pid);
			}
			else {
				cc = getSelfConfiguringComponentConfiguration(pid);
			}
			if (cc != null && cc.getPid() != null && cc.getPid().equals(pid)) {
				props = cc.getConfigurationProperties();
				cci   = new ComponentConfigurationImpl(pid, null, props);
				configs.add((ComponentConfigurationImpl) cci);
			}
		}

		return saveSnapshot(configs);
	}
	
	
		
	@Override
	public long rollback() 
		throws KuraException
	{	
		// get the second-last most recent snapshot
		// and rollback to that one.
		Set<Long> ids = getSnapshots();
		if (ids.size() < 2) {
			throw new KuraException(KuraErrorCode.CONFIGURATION_SNAPSHOT_NOT_FOUND, null, "No Snapshot Available");
		}
		
		// rollback to the second last snapshot
		Long[] snapshots = ids.toArray( new Long[]{});
		Long   id = snapshots[ids.size()-2];
		
		rollback(id); 
		return id;
	}

	
	@Override
	public synchronized void rollback(long id) 
		throws KuraException
	{
		// load the snapshot we need to rollback to
		XmlComponentConfigurations xmlConfigs = loadSnapshot(id);

		//
		// restore configuration
		s_logger.info("Rolling back to snapshot {}...", id);

		Set<String> snapshotPids = new HashSet<String>();
		boolean snapshotOnConfirmation = false;
		List<Throwable> causes = new ArrayList<Throwable>();
		List<ComponentConfigurationImpl> configs = xmlConfigs.getConfigurations();
		for (ComponentConfigurationImpl config : configs) {
			if (config != null) {
				try {
					updateConfigurationInternal(config.getPid(), 
											    config.getConfigurationProperties(),
											    snapshotOnConfirmation);
				}
				catch (Throwable t) {
					s_logger.warn("Error during rollback for component "+config.getPid(), t);
					causes.add(t);
				}
				// Track the pid of the component
				snapshotPids.add(config.getPid());
			}
		}
		
		// rollback to the default configuration for those configurable components
		// whose configuration is not present in the snapshot
		Set<String> pids = new HashSet<String>(m_allPids);
		pids.removeAll(m_selfConfigComponents);
		pids.removeAll(snapshotPids);
		
		for (String pid : pids) {
			s_logger.info("Rolling back to default configuration for component pid: '{}'", pid);
			Bundle bundle = m_ctx.getBundleContext().getServiceReference(pid).getBundle();
			try {
				OCD ocd = ComponentUtil.readObjectClassDefinition(bundle, pid);
				Map<String, Object> defaults = ComponentUtil.getDefaultProperties(ocd);
				updateConfigurationInternal(pid, defaults, snapshotOnConfirmation);
			} catch (Throwable t) {
				s_logger.warn("Error during rollback for component "+pid, t);
				causes.add(t);
			}
		}
		if (causes.size() > 0) {
			throw new KuraPartialSuccessException("Rollback", causes);
		}

		// Do not call snapshot() here because it gets the configurations of SelfConfiguringComponents
		// using SelfConfiguringComponent.getConfiguration() and the configuration returned
		// might be the old one not the one just loaded from the snapshot and updated through
		// the Configuration Admin. Instead just make a copy of the snapshot.
		saveSnapshot(configs);
	}


	
	@Override
	public Set<Long> getSnapshots() throws KuraException 
	{
		return getSnapshotsInternal();
	}

	
	public List<ComponentConfiguration> getSnapshot(long sid)
		throws KuraException
	{
		List<ComponentConfiguration> configs = new ArrayList<ComponentConfiguration>();		
		XmlComponentConfigurations xmlConfigs = loadSnapshot(sid);
		if (xmlConfigs != null) {
			configs.addAll(xmlConfigs.getConfigurations());
		}
		
		return configs;
	}
	
	
	public synchronized void updateConfigurations(List<ComponentConfiguration> configsToUpdate)
		throws KuraException
	{	
		boolean snapshotOnConfirmation = false;
		List<Throwable> causes = new ArrayList<Throwable>();
		for (ComponentConfiguration config : configsToUpdate) {
			if (config != null) {
				try {
					updateConfigurationInternal(config.getPid(), 
												config.getConfigurationProperties(),
												snapshotOnConfirmation);
				}
				catch (KuraException e) {
					s_logger.warn("Error during updateConfigurations for component "+config.getPid(), e);
					causes.add(e);
				}
			}
		}
		
		// Build the current configuration
		ComponentConfiguration cc = null; 
		ComponentConfigurationImpl cci = null; 
		Map<String,Object> props = null;
		List<ComponentConfiguration> configs = new ArrayList<ComponentConfiguration>();

		// clone the list to avoid concurrent modifications
		List<String> allPids = new ArrayList<String>(m_allPids);
		for (String pid : allPids) {
			boolean isConfigToUpdate = false;
			for(ComponentConfiguration configToUpdate : configsToUpdate) {
				if(configToUpdate.getPid().equals(pid)) {
					//found a match
					isConfigToUpdate = true;
					configs.add(configToUpdate);
					break;
				}
			}
			
			if(!isConfigToUpdate) {
				if (!m_selfConfigComponents.contains(pid)) {
					cc = getConfigurableComponentConfiguration(pid);
				}
				else {
					cc = getSelfConfiguringComponentConfiguration(pid);
				}
				if (cc != null && cc.getPid() != null && cc.getPid().equals(pid)) {
					props = cc.getConfigurationProperties();
					cci   = new ComponentConfigurationImpl(pid, null, props);
					configs.add((ComponentConfigurationImpl) cci);
				}
			}
		}

		saveSnapshot(configs);
		
		if (causes.size() > 0) {
			throw new KuraPartialSuccessException("updateConfigurations", causes);
		}
	}

	
	// ----------------------------------------------------------------
	//
	//   Package APIs
	//
	// ----------------------------------------------------------------

	
	synchronized void registerComponentConfiguration(Bundle bundle, String pid)
		throws KuraException
	{
		if (!m_allPids.contains(pid)) {
			
			s_logger.info("Registration of ConfigurableComponent {} by {}...", pid, this);
			try {
				
				// register it
				m_allPids.add(pid);	

				// Get the ocd
				Tocd ocd = null;		
				try {
					ocd = ComponentUtil.readObjectClassDefinition(bundle, pid);
					if (ocd != null) {						
						s_logger.info("Registering {} with ocd: {} ...", pid, ocd);
						m_ocds.put(pid,  ocd);
					}
				}
				catch (Throwable t) {
					s_logger.error("Error reading ObjectClassDefinition for "+pid, t);
				}
				s_logger.info("Registration Completed for Component {}.", pid);				
			}
			catch (Exception e) {
				s_logger.error("Error initializing Component Configuration", e);
			}
		}
	}


	synchronized void registerSelfConfiguringComponent(String pid)
		throws KuraException
	{
		if (pid == null) {
			throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR);
		}
		
		m_allPids.add(pid);
		m_selfConfigComponents.add(pid);
	}

	
	void unregisterComponentConfiguration(String pid)
	{
		if (pid == null) {
			return;
		}
		
		s_logger.debug("Removing component configuration for " + pid);
		m_allPids.remove(pid);
		m_ocds.remove(pid);
		m_selfConfigComponents.remove(pid);
	}
		
	void updateConfigurationInternal(String pid, 
									 Map<String,Object> properties,
									 boolean snapshotOnConfirmation)
		throws KuraException
	{
		s_logger.debug("Attempting update configuration for {}", pid);
				
		if (!m_allPids.contains(pid)) {
			s_logger.info("UpdatingConfiguration ignored as ConfigurableComponent {} is NOT tracked.", pid);
			return;
		}
		if (properties == null) {
			s_logger.info("UpdatingConfiguration ignored as properties for ConfigurableComponent {} are NULL.", pid);
			return;
		}
		
		Map<String, Object> mergedProperties = new HashMap<String, Object>(properties);
		
		// Try to get the OCD from the registered ConfigurableComponents
		OCD registerdOCD = m_ocds.get(pid);
		// Otherwise try to get it from the registered SelfConfiguringComponents
		// (whose OCD is not tracked in the m_ocds map - why?).
		if (registerdOCD == null) {
			ComponentConfiguration config = getSelfConfiguringComponentConfiguration(pid);
			if (config != null) {
				registerdOCD = config.getDefinition();
			}
		}
		
		if (registerdOCD != null) {
			boolean changed = mergeWithDefaults(registerdOCD, mergedProperties);
			if (changed) {
				s_logger.info("mergeWithDefaults returned "+changed);
			}
		}
		
		try {
			
			if (!m_selfConfigComponents.contains(pid)) {

				// load the ocd to do the validation
				BundleContext ctx = m_ctx.getBundleContext();
				ObjectClassDefinition ocd = null;
				ocd = ComponentUtil.getObjectClassDefinition(ctx, pid);

				// Validate the properties to be applied and set them
				validateProperties(pid, ocd, mergedProperties);
			}
			else {				
				// FIXME: validation of properties for self-configuring components 
			}		

			if (!snapshotOnConfirmation) {
				m_pendingConfigurationPids.add(pid);
			}
			else {
				s_logger.info("Snapshot on EventAdmin configuration will be taken for {}.", pid);
			}

			// Update the new properties
			// use ConfigurationAdmin to do the update
			Configuration config = m_configurationAdmin.getConfiguration(pid);
			config.update(CollectionsUtil.mapToDictionary(mergedProperties));

			s_logger.info("Updating Configuration of ConfigurableComponent {} ... Done.", pid);
		}
		catch (IOException e) {
			s_logger.error("Error updating Configuration of ConfigurableComponent "+pid, e);
			throw new KuraException(KuraErrorCode.CONFIGURATION_UPDATE, e, pid);
		}
	}

	
	
	XmlComponentConfigurations loadSnapshot(long id)
			throws KuraException, FactoryConfigurationError 
	{
		// Get the snapshot file to rollback to
		File fSnapshot = getSnapshotFile(id);
		if (!fSnapshot.exists()) {
			throw new KuraException(KuraErrorCode.CONFIGURATION_SNAPSHOT_NOT_FOUND, fSnapshot.getAbsolutePath());
		}

		// Unmarshall
		XmlComponentConfigurations xmlConfigs = null;
		try {			
			FileReader fr = new FileReader(fSnapshot);
			xmlConfigs = XmlUtil.unmarshal(fr, XmlComponentConfigurations.class);			
		}
		catch (Exception e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		}
		return xmlConfigs;
	}

	
		
	// ----------------------------------------------------------------
	//
	//   Private APIs
	//
	// ----------------------------------------------------------------
	
	private synchronized long saveSnapshot(List<? extends ComponentConfiguration> configs)
		throws KuraException 
	{
		List<ComponentConfigurationImpl> configImpls = new ArrayList<ComponentConfigurationImpl>();
		for (ComponentConfiguration config : configs) {
			if (config instanceof ComponentConfigurationImpl) {
				configImpls.add((ComponentConfigurationImpl) config);
			}
		}

		//
		// Build the XML structure
		XmlComponentConfigurations conf = new XmlComponentConfigurations();
		conf.setConfigurations(configImpls);

		//
		// Write it to disk: marshall
		long sid = (new Date()).getTime();

		//
		// Do not save the snapshot in the past
		Set<Long> snapshotIDs = getSnapshots();
		if (snapshotIDs != null && snapshotIDs.size() > 0) {
			Long[] snapshots = snapshotIDs.toArray( new Long[]{});
			Long   lastestID = snapshots[snapshotIDs.size()-1];

			if (lastestID != null && sid <= lastestID) {
				s_logger.warn("Snapshot ID: {} is in the past. Adjusting ID to: {} + 1", sid, lastestID);
				sid = lastestID + 1;
			}
		}
		
		File fSnapshot = getSnapshotFile(sid);
		s_logger.info("Writing snapshot - Saving {}...", fSnapshot.getAbsolutePath());
		try {
		
			FileOutputStream fos = new FileOutputStream(fSnapshot);
			OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF-8");
			XmlUtil.marshal(conf, osw);
			osw.flush();
			fos.flush();
			fos.getFD().sync();
			osw.close();
			fos.close();
		}
		catch (Throwable t) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, t);
		}
		s_logger.info("Writing snapshot - Saving {}... Done.", fSnapshot.getAbsolutePath());
		
		//
		// Garbage Collector for number of Snapshots Saved
		garbageCollectionOldSnapshots();
		return sid;
	}

	
	private ComponentConfiguration getConfigurableComponentConfiguration(String pid)
	{
		ComponentConfiguration cc = null;
		try {

			Tocd ocd = m_ocds.get(pid);
		
			Configuration cfg = m_configurationAdmin.getConfiguration(pid);
			Map<String,Object> props = CollectionsUtil.dictionaryToMap(cfg.getProperties(), ocd);
			
			cc = new ComponentConfigurationImpl(pid, ocd, props);
		}
		catch (Exception e) {
			s_logger.error("Error getting Configuration for component: "+pid+". Ignoring it.", e);
		}
		return cc;
	}

	
	private ComponentConfiguration getSelfConfiguringComponentConfiguration(String pid)
	{
		ComponentConfiguration cc = null;
		ServiceReference<?> ref = m_ctx.getBundleContext().getServiceReference(pid);
		if (ref != null) {
			Object obj = m_ctx.getBundleContext().getService(ref);
			try {
				if (obj instanceof SelfConfiguringComponent) {
					SelfConfiguringComponent selfConfigComp = null;
					selfConfigComp = (SelfConfiguringComponent) obj;
					try {
						
						cc = selfConfigComp.getConfiguration();
						if (cc.getPid() == null || !cc.getPid().equals(pid)) {
							s_logger.error("Invalid pid for returned Configuration of SelfConfiguringComponent with pid: "+pid+". Ignoring it.");
							cc = null; // do not return the invalid configuration
							return cc;
						}
						
						OCD ocd = cc.getDefinition();
						if (ocd != null) {
							List<AD> ads = ocd.getAD();
							
							if (ads != null) {
								for (AD ad : ads) {
									String adId = ad.getId();
									String adType = ad.getType().value();
									
									if (adId == null) {
										s_logger.error("null required id for AD for returned Configuration of SelfConfiguringComponent with pid: {}", pid);
										cc = null;
										return cc;  // do not return the invalid configuration										
									}
									if (adType == null) {
										s_logger.error("null required type for AD id: {} for returned Configuration of SelfConfiguringComponent with pid: {}", adId, pid);
										cc = null;
										return cc;  // do not return the invalid configuration
									}

									Map<String, Object> props = cc.getConfigurationProperties();
									if (props != null) {
										for (String propName : props.keySet()) {
											if (propName.equals(adId)) {
												Object value = props.get(propName);
												if (value != null) {
													String propType = value.getClass().getSimpleName();
													try {
														s_logger.debug("pid: {}, property name: {}, type: {}, value: {}", new Object[] {pid, propName, propType, value});
														Scalar.fromValue(propType);
														if (!propType.equals(adType)) {
															s_logger.error("Type: {} for property named: {} does not match the AD type: {} for returned Configuration of SelfConfiguringComponent with pid: {}",
																	new Object[] {propType, propName, adType, pid});
															cc = null;
															return cc;  // do not return the invalid configuration															
														}
													} catch (IllegalArgumentException e) {
														s_logger.error("Invalid class: {} for property named: {} for returned Configuration of SelfConfiguringComponent with pid: " + pid, propType, propName);
														cc = null;
														return cc;  // do not return the invalid configuration
													}
												}
											}
										}
									}
								}
							}
						}						
					} 
					catch (KuraException e) {
						s_logger.error("Error getting Configuration for component: "+pid+". Ignoring it.", e);
					}
				}
				else {
					s_logger.error("Component "+obj+" is not a SelfConfiguringComponent. Ignoring it.");			
				}
			}
			finally {
				m_ctx.getBundleContext().ungetService(ref);
			}
		}
		return cc;
	}
	

	private TreeSet<Long> getSnapshotsInternal() 
	{
		// keeps the list of snapshots ordered
		TreeSet<Long> ids = new TreeSet<Long>();		
		String configDir = getSnapshotsDirectory();
		if(configDir != null) {
			File  fConfigDir = new File(configDir);
			File[] files = fConfigDir.listFiles();
			if (files != null) {
				
				Pattern p = Pattern.compile("snapshot_([0-9]+)\\.xml");
				for (File file : files) {
					Matcher m = p.matcher(file.getName());
					if (m.matches()) {
						ids.add(Long.parseLong(m.group(1)));
					}
				}
			}
		}
		return ids;
	}

	
	String getSnapshotsDirectory() 
	{
		String configDirs = m_systemService.getKuraSnapshotsDirectory();
		return configDirs;
	}
	
	
	private File getSnapshotFile(long id) 
	{
		String configDir = getSnapshotsDirectory();
		StringBuilder sbSnapshot = new StringBuilder(configDir);
		sbSnapshot.append(File.separator)
				  .append("snapshot_")
				  .append(id)
				  .append(".xml");

		String snapshot = sbSnapshot.toString();		
		return new File(snapshot);
	}

	
	private void garbageCollectionOldSnapshots()
	{
		// get the current snapshots and compared with the maximum number we need to keep 
		TreeSet<Long> sids = getSnapshotsInternal();

		int currCount = sids.size(); 
		int maxCount  = m_systemService.getKuraSnapshotsCount();
		while (currCount > maxCount) {
			
			// preserve snapshot ID 0 as this will be considered the seeding one.
			long sid = sids.pollFirst();			
			if (sid != 0) {
				File fSnapshot = getSnapshotFile(sid);
				if (fSnapshot.exists()) {
					s_logger.info("Snapshots Garbage Collector. Deleting {}", fSnapshot.getAbsolutePath());
					fSnapshot.delete();
					currCount--;
				}
			}
		}
	}

	
	private void loadLatestSnapshotInConfigAdmin() 
		throws KuraException 
	{
		//
		// Get the latest snapshot file to use as initialization
		Set<Long> snapshotIDs = getSnapshots();
		if (snapshotIDs == null || snapshotIDs.size() == 0) {
			return;
		}
		
		Long[] snapshots = snapshotIDs.toArray( new Long[]{});
		Long   lastestID = snapshots[snapshotIDs.size()-1];
		String configDir = getSnapshotsDirectory();
		StringBuilder sbSnapshot = new StringBuilder(configDir);
		sbSnapshot.append(File.separator)
				  .append("snapshot_")
				  .append(lastestID)
				  .append(".xml");

		String snapshot = sbSnapshot.toString();
		File fSnapshot = new File(snapshot);
		if (!fSnapshot.exists()) {
			throw new KuraException(KuraErrorCode.CONFIGURATION_SNAPSHOT_NOT_FOUND, snapshot);
		}

		//
		// Unmarshall
		s_logger.info("Loading init configurations from: {}...", snapshot);
		XmlComponentConfigurations xmlConfigs = null;
		try {
			
			FileReader  fr = new FileReader(fSnapshot);
			xmlConfigs = XmlUtil.unmarshal(fr, XmlComponentConfigurations.class);			
		}
		catch (Exception e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		}

		//
		// save away initial configuration
		List<ComponentConfigurationImpl> configs = xmlConfigs.getConfigurations();
		for (ComponentConfigurationImpl config : configs) {
			if (config != null) {				
				Configuration cfg;
				try {
					s_logger.debug("Pushing config to config admin: {}", config.getPid());

					// push it to the ConfigAdmin
					cfg = m_configurationAdmin.getConfiguration(config.getPid());
					cfg.update(CollectionsUtil.mapToDictionary(config.getConfigurationProperties()));

					// track it as a pending Configuration
					// for which we are expecting a confirmation
					m_pendingConfigurationPids.add(config.getPid());
				} 
				catch (IOException e) {
					s_logger.warn("Error seeding initial properties to ConfigAdmin for service: "+config.getPid(), e);
				}
			}
		}
	}
	
	
	private void validateProperties(String pid,
									ObjectClassDefinition ocd,
	        					    Map<String, Object> updatedProps) 
	    throws KuraException 
	{
		if (ocd != null) {

			// build a map of all the attribute definitions
			Map<String,AttributeDefinition> attrDefs = new HashMap<String,AttributeDefinition>();				
			AttributeDefinition[] defs = ocd.getAttributeDefinitions(ObjectClassDefinition.ALL);
			for (AttributeDefinition def : defs) {
				attrDefs.put(def.getID(), def);					
			}

			// loop over the proposed property values
			// and validate them against the definition
			Iterator<String> keys = updatedProps.keySet().iterator();
			while (keys.hasNext()) {

				String key = keys.next();
				AttributeDefinition attrDef = attrDefs.get(key);

				// is attribute undefined?
				if (attrDef == null) {
					// we do not have an attribute descriptor to the validation against
					// As OSGI insert attributes at runtime like service.pid, component.name,
					// for the attribute for which we do not have a definition, 
					// just accept them.
					continue;
				}

				// validate the attribute value
				Object objectValue = updatedProps.get(key);
				String stringValue = StringUtil.valueToString(objectValue);
				if (stringValue != null) {
					String result = attrDef.validate(stringValue);
					if (result != null && !result.isEmpty()) {
						throw new KuraException(KuraErrorCode.CONFIGURATION_ATTRIBUTE_INVALID, attrDef.getID()+": "+result);					
					}
				}
			}
			
			// make sure all required properties are set
			OCD ocdFull = m_ocds.get(pid);
			if (ocdFull != null) {
				for (AD attrDef : ocdFull.getAD()) {					
					// to the required attributes make sure a value is defined.
					if (attrDef.isRequired()) {						
						if (updatedProps.get(attrDef.getId()) == null) {	
							// if the default one is not defined, throw exception.							
							throw new KuraException(KuraErrorCode.CONFIGURATION_REQUIRED_ATTRIBUTE_MISSING, attrDef.getId());
						}
					}
				}
			}
		}
	}
	
	boolean mergeWithDefaults(OCD ocd, Map<String,Object> properties) 
		throws KuraException
	{
		boolean changed = false;
		Set<String> keys = properties.keySet();
		
		Map<String, Object> defaults = ComponentUtil.getDefaultProperties(ocd);		
		Set<String> defaultsKeys = defaults.keySet();
		defaultsKeys.removeAll(keys);
		if (!defaultsKeys.isEmpty()) {
			
			changed = true;
			s_logger.info("Merging configuration for pid: {}", ocd.getId());
			for (String key : defaultsKeys) {

				Object value = defaults.get(key);
			    properties.put(key, value);			    
				s_logger.debug("Merged configuration properties with property with name: {} and default value {}", key, value);
			}
		}		
		return changed;
	}
}

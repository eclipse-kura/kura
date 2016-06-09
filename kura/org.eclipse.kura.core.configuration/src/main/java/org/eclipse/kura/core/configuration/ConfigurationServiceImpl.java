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
 *******************************************************************************/
package org.eclipse.kura.core.configuration;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLStreamException;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.KuraPartialSuccessException;
import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.configuration.Password;
import org.eclipse.kura.configuration.SelfConfiguringComponent;
import org.eclipse.kura.configuration.metatype.AD;
import org.eclipse.kura.configuration.metatype.OCD;
import org.eclipse.kura.configuration.metatype.Scalar;
import org.eclipse.kura.core.configuration.metatype.Tocd;
import org.eclipse.kura.core.configuration.upgrade.ConfigurationUpgrade;
import org.eclipse.kura.core.configuration.util.CollectionsUtil;
import org.eclipse.kura.core.configuration.util.ComponentUtil;
import org.eclipse.kura.core.configuration.util.StringUtil;
import org.eclipse.kura.core.configuration.util.XmlUtil;
import org.eclipse.kura.crypto.CryptoService;
import org.eclipse.kura.system.SystemService;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
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
public class ConfigurationServiceImpl implements ConfigurationService, ConfigurationListener {
	private static final Logger s_logger = LoggerFactory.getLogger(ConfigurationServiceImpl.class);
	
	private ComponentContext m_ctx;
	private ServiceTracker<?, ?> m_serviceTracker;
	private BundleTracker<Bundle> m_bundleTracker;

	@SuppressWarnings("unused")
	private MetaTypeService m_metaTypeService;
	private ConfigurationAdmin m_configurationAdmin;
	private SystemService m_systemService;
	private CryptoService m_cryptoService;

	// contains all the PIDs (aka kura.service.pid) - both of configurable and self configuring components
	private Set<String> m_allActivatedPids;

	// contains the self configuring components ONLY!
	private Set<String> m_activatedSelfConfigComponents;

	// maps either service.pid or service.factoryPid to the related OCD
	private Map<String, Tocd> m_ocds;

	// contains the service.factoryPid of all Factory Components
	private Set<String> m_factoryPids;
		
	// maps the kura.service.pid to the associated service.factoryPid
	private Map<String, String> m_factoryPidByPid;
	
	// contains all pids (aka kura.service.pid) which have been configured and for which we have not
	// received the corresponding ConfigurationEvent yet
	private Set<String> m_pendingConfigurationPids;
	
	private Set<String> m_pendingDeletePids;
	
	// maps the kura.service.pid to the associated service.pid
	private Map<String, String> m_servicePidByPid;

	// ----------------------------------------------------------------
	//
	// Dependencies
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

	public ConfigurationServiceImpl() {
		m_allActivatedPids = new HashSet<String>();
		m_activatedSelfConfigComponents = new HashSet<String>();
		m_pendingConfigurationPids = new HashSet<String>();
		m_pendingDeletePids = new HashSet<String>();
		m_ocds = new HashMap<String, Tocd>();
		m_factoryPids = new HashSet<String>();
		m_factoryPidByPid = new HashMap<String, String>();
		m_servicePidByPid = new HashMap<String, String>();
	}

	// ----------------------------------------------------------------
	//
	// Activation APIs
	//
	// ----------------------------------------------------------------

	protected void activate(ComponentContext componentContext) throws InvalidSyntaxException {
		s_logger.info("activate...");

		// save the bundle context
		m_ctx = componentContext;

		// 1. Register the ConfigurationListener to
		// monitor Configuration updates
		m_ctx.getBundleContext().registerService(ConfigurationListener.class.getName(), this, null);

		// 2. Load the latest snapshot and push it to ConfigurationAdmin
		try {
			loadLatestSnapshotInConfigAdmin();
		} catch (Exception e) {
			throw new ComponentException("Error loading latest snapshot", e);
		}

		//
		// start the trackers
		s_logger.info("Trackers being opened...");
		
		m_serviceTracker = new ConfigurableComponentTracker(m_ctx.getBundleContext(), this);
		m_serviceTracker.open(true);

		m_bundleTracker = new ComponentMetaTypeBundleTracker(m_ctx.getBundleContext(), m_configurationAdmin, this);
		m_bundleTracker.open();
	}

	protected void deactivate(ComponentContext componentContext) {
		s_logger.info("deactivate...");

		//
		// stop the trackers
		if (m_serviceTracker != null) {
			m_serviceTracker.close();
		}
		if (m_bundleTracker != null) {
			m_bundleTracker.close();
		}
	}

	// ----------------------------------------------------------------
	//
	// ConfigurationListener
	//
	// ----------------------------------------------------------------

	@Override
	public synchronized void configurationEvent(ConfigurationEvent event) {
		// Called every time a new service configuration is invoked
		// we need to take a new snapshot every time this happens
		
		// note that the pid in the event is the service.pid
		String pid = getPidByServicePid(event.getPid());

		if (m_pendingConfigurationPids.contains(pid)) {
			
			// ignore the ConfigurationEvent for those PIDs whose
			// configuration update was by the ConfigurationService itself
			m_pendingConfigurationPids.remove(pid);
			return;
		}
		try {
			if (m_allActivatedPids.contains(pid)) {
				// Take a new snapshot
				s_logger.info("ConfigurationEvent for tracked ConfigurableComponent with pid: {}", pid);
				snapshot();
			}
		} catch (Exception e) {
			s_logger.error("Error taking snapshot after ConfigurationEvent", e);
		}
	}

	// ----------------------------------------------------------------
	//
	// Service APIs
	//
	// ----------------------------------------------------------------

	@Override
	public Set<String> getConfigurableComponentPids() {
		if (m_allActivatedPids.isEmpty()) {
			return Collections.emptySet();
		}
		return Collections.unmodifiableSet(m_allActivatedPids);
	}

	@Override
	public List<ComponentConfiguration> getComponentConfigurations() throws KuraException {
		List<ComponentConfiguration> configs = new ArrayList<ComponentConfiguration>();

		// assemble all the configurations we have
		// clone the list to avoid concurrent modifications
		List<String> allPids = new ArrayList<String>(m_allActivatedPids);
		for (String pid : allPids) {
			try {
				ComponentConfiguration cc = getComponentConfiguration(pid);
				if (cc != null) {
					configs.add(cc);
				}
			} catch (Exception e) {
				s_logger.error("Error getting configuration for component " + pid, e);
				throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR, e, "Error getting configuration for component " + pid);
			}
		}
		return configs;
	}

	@Override
	public ComponentConfiguration getComponentConfiguration(String pid) throws KuraException {
		ComponentConfiguration cc;
		if (!m_activatedSelfConfigComponents.contains(pid)) {
			cc = getConfigurableComponentConfiguration(pid);
		} else {
			cc = getSelfConfiguringComponentConfiguration(pid);
		}
//		if(cc != null && cc.getConfigurationProperties() != null){
//			decryptPasswords(cc);
//		}
		return cc;
	}
	
	@Override
	public synchronized void updateConfiguration(String pidToUpdate, Map<String, Object> propertiesToUpdate) throws KuraException {
		updateConfiguration(pidToUpdate, propertiesToUpdate, true);
	}

	@Override
	public synchronized void updateConfiguration(String pidToUpdate, Map<String, Object> propertiesToUpdate, boolean takeSnapshot) throws KuraException {
		List<ComponentConfiguration> configs = new ArrayList<ComponentConfiguration>();
		ComponentConfigurationImpl cci = new ComponentConfigurationImpl(pidToUpdate, null, propertiesToUpdate);
		configs.add(cci);
		updateConfigurationsInternal(configs, takeSnapshot);
	}

	// ----------------------------------------------------------------
	//
	// Service APIs: Factory Management
	//
	// ----------------------------------------------------------------
	@Override
	public Set<String> getFactoryComponentPids() {
		return Collections.unmodifiableSet(m_factoryPids);
	}

	@Override
	public ComponentConfiguration getDefaultComponentConfiguration(String pid) throws KuraException {
		Tocd ocd = getOCDForPid(pid); 
		Map<String, Object> props = ComponentUtil.getDefaultProperties(ocd, m_ctx);
		return new ComponentConfigurationImpl(pid, ocd, props);
	}

	@Override
	public synchronized void createFactoryConfiguration(String factoryPid, String pid, Map<String, Object> properties, boolean takeSnapshot) throws KuraException {
		if(pid == null){
			throw new KuraException(KuraErrorCode.INVALID_PARAMETER, "pid cannot be null");
		} else if(m_servicePidByPid.containsKey(pid)){
				throw new KuraException(KuraErrorCode.INVALID_PARAMETER, "pid " + pid + " already exists");
		}
		
		try {
			// Second argument in createFactoryConfiguration is a bundle location. If left null the new bundle location
			// will be bound to the location of the first bundle that registers a Managed Service Factory with a corresponding PID
			s_logger.info("Creating new configuration for factory pid {} and pid {}", factoryPid, pid);
			String servicePid = m_configurationAdmin.createFactoryConfiguration(factoryPid, null).getPid();
						
			s_logger.info("Updating newly created configuration for pid {}", pid);

			Map<String, Object> mergedProperties = new HashMap<String, Object>();
			if (properties != null) {
				mergedProperties.putAll(properties);
			}

			OCD ocd = m_ocds.get(factoryPid);
			mergeWithDefaults(ocd, mergedProperties);

			mergedProperties.put(ConfigurationService.KURA_SERVICE_PID, pid);
			
			Dictionary<String, Object> dict = CollectionsUtil.mapToDictionary(mergedProperties);
			Configuration config = m_configurationAdmin.getConfiguration(servicePid, null);
			config.update(dict);
			
			registerComponentConfiguration(pid, servicePid, factoryPid);
			
			if (!takeSnapshot) {
				m_pendingConfigurationPids.add(pid);
			}
		} catch (IOException e) {
			throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR, e, "Cannot create component instance for factory " + factoryPid);
		}
	}

	@Override
	public synchronized void deleteFactoryConfiguration(String pid, boolean takeSnapshot) throws KuraException {
		if (pid == null){
			throw new KuraException(KuraErrorCode.INVALID_PARAMETER, "pid cannot be null");
		} else if (m_factoryPidByPid.get(pid) == null) {
			throw new KuraException(KuraErrorCode.INVALID_PARAMETER, "pid " + pid + " is not a factory component instance");
		}

		try {
			s_logger.info("Deleting configuration for pid {}", pid);
			Configuration config = m_configurationAdmin.getConfiguration(m_servicePidByPid.get(pid));
			
			if (config != null) {
				config.delete();
			}
			
			unregisterComponentConfiguration(pid);
			
			m_pendingDeletePids.add(pid);
			
			if (takeSnapshot) {
				snapshot();
			}
		} catch (IOException e) {
			throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR, e, "Cannot delete component instance " + pid);
		}
	}

	// ----------------------------------------------------------------
	//
	// Service APIs: Snapshot Management
	//
	// ----------------------------------------------------------------

	@Override
	public long snapshot() throws KuraException {
		s_logger.info("Writing snapshot - Getting component configurations...");

		List<ComponentConfiguration> configs = buildCurrentConfiguration(null);

		return saveSnapshot(configs);
	}

	@Override
	public long rollback() throws KuraException {
		// get the second-last most recent snapshot
		// and rollback to that one.
		Set<Long> ids = getSnapshots();
		if (ids.size() < 2) {
			throw new KuraException(KuraErrorCode.CONFIGURATION_SNAPSHOT_NOT_FOUND, null, "No Snapshot Available");
		}

		// rollback to the second last snapshot
		Long[] snapshots = ids.toArray(new Long[] {});
		Long id = snapshots[ids.size() - 2];

		rollback(id);
		return id;
	}

	@Override
	public synchronized void rollback(long id) throws KuraException {
		// load the snapshot we need to rollback to
		XmlComponentConfigurations xmlConfigs = loadEncryptedSnapshotFileContent(id);

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
					rollbackConfigurationInternal(config.getPid(), config.getConfigurationProperties(), snapshotOnConfirmation);
				} catch (Throwable t) {
					s_logger.warn("Error during rollback for component " + config.getPid(), t);
					causes.add(t);
				}
				// Track the pid of the component
				snapshotPids.add(config.getPid());
			}
		}

		// rollback to the default configuration for those configurable
		// components
		// whose configuration is not present in the snapshot
		Set<String> pids = new HashSet<String>(m_allActivatedPids);
		pids.removeAll(m_activatedSelfConfigComponents);
		pids.removeAll(snapshotPids);

		for (String pid : pids) {
			s_logger.info("Rolling back to default configuration for component pid: '{}'", pid);
			try {
				ServiceReference<?>[] refs = m_ctx.getBundleContext().getServiceReferences((String) null, null);
				if (refs != null) {
					for (ServiceReference<?> ref : refs) {
						String ppid = (String) ref.getProperty(Constants.SERVICE_PID);
						if (pid.equals(ppid)) {
							Bundle bundle = ref.getBundle();
							try {
								OCD ocd = ComponentUtil.readObjectClassDefinition(bundle, pid);
								Map<String, Object> defaults = ComponentUtil.getDefaultProperties(ocd, m_ctx);
								rollbackConfigurationInternal(pid, defaults, snapshotOnConfirmation);
							} catch (Throwable t) {
								s_logger.warn("Error during rollback for component "+pid, t);
								causes.add(t);
							}
						}
					}
				}
			} catch (InvalidSyntaxException e) {
				s_logger.warn("Error during rollback for component "+pid, e);
			}
		}
		if (!causes.isEmpty()) {
			throw new KuraPartialSuccessException("Rollback", causes);
		}

		// Do not call snapshot() here because it gets the configurations of
		// SelfConfiguringComponents
		// using SelfConfiguringComponent.getConfiguration() and the
		// configuration returned
		// might be the old one not the one just loaded from the snapshot and
		// updated through
		// the Configuration Admin. Instead just make a copy of the snapshot.
		saveSnapshot(configs);
	}

	@Override
	public Set<Long> getSnapshots() throws KuraException {
		return getSnapshotsInternal();
	}

	@Override
	public List<ComponentConfiguration> getSnapshot(long sid) throws KuraException {
		XmlComponentConfigurations xmlConfigs = loadEncryptedSnapshotFileContent(sid);

		List<ComponentConfigurationImpl> configs = xmlConfigs.getConfigurations();
		for (ComponentConfigurationImpl config : configs) {
			if (config != null) {
				try {
					Map<String, Object> decryptedProperties = decryptPasswords(config);
					config.setProperties(decryptedProperties);
				} catch (Throwable t) {
					s_logger.warn("Error during snapshot password decryption");
				}
			}
		}

		List<ComponentConfiguration> returnConfigs = new ArrayList<ComponentConfiguration>();
		if (xmlConfigs != null) {
			returnConfigs.addAll(xmlConfigs.getConfigurations());
		}

		return returnConfigs;
	}

	@Override
	public synchronized void updateConfigurations(List<ComponentConfiguration> configsToUpdate) throws KuraException {
		updateConfigurationsInternal(configsToUpdate, true);
	}

	public synchronized void updateConfigurationsInternal(List<ComponentConfiguration> configsToUpdate, boolean takeSnapshot) throws KuraException {
		boolean snapshotOnConfirmation = false;
		List<Throwable> causes = new ArrayList<Throwable>();
		for (ComponentConfiguration config : configsToUpdate) {
			if (config != null) {
				encryptPasswords(config);
			}
		}

		for (ComponentConfiguration config : configsToUpdate) {
			if (config != null) {
				try {
					updateConfigurationInternal(config.getPid(), config.getConfigurationProperties(), snapshotOnConfirmation);
				} catch (KuraException e) {
					s_logger.warn("Error during updateConfigurations for component " + config.getPid(), e);
					causes.add(e);
				}
			}
		}

		List<ComponentConfiguration> configs = buildCurrentConfiguration(configsToUpdate);
		
		if (takeSnapshot) {
			saveSnapshot(configs);
		}

		if (!causes.isEmpty()) {
			throw new KuraPartialSuccessException("updateConfigurations", causes);
		}
	}

	// ----------------------------------------------------------------
	//
	// Package APIs
	//
	// ----------------------------------------------------------------
	synchronized void registerComponentOCD(String metatypePid, Tocd ocd, boolean isFactory) throws KuraException {
		// metatypePid is either the 'pid' or 'factoryPid' attribute of the MetaType Designate element
		// 'pid' matches a service.pid, not a kura.service.pid
		if (!m_ocds.containsKey(metatypePid)) {
			s_logger.info("Registering metatype pid: {} with ocd: {} ...", metatypePid, ocd);
			m_ocds.put(metatypePid, ocd);
		}
		if (isFactory) {
			m_factoryPids.add(metatypePid);
		}
	}
	
	synchronized void registerComponentConfiguration(String pid, String servicePid, String factoryPid) {
		if (pid == null || servicePid == null) {
			s_logger.warn("Either kura.service.pid {} or service.pid {} are null", pid, servicePid);
			return;
		}
		if (!m_allActivatedPids.contains(pid)) {
			// register the component instance
			s_logger.info("Registration of ConfigurableComponent {} by {}...", pid, this);
			m_servicePidByPid.put(pid, servicePid);
			if (factoryPid != null) {
				m_factoryPidByPid.put(pid, factoryPid);
			}
			m_allActivatedPids.add(pid);
		}
	}

	synchronized void registerSelfConfiguringComponent(String pid) {
		if (pid == null) {
			s_logger.warn("pid is null");
			return;
		}
		if (!m_allActivatedPids.contains(pid)) {
			s_logger.info("Registration of SelfConfiguringComponent {} by {}...", pid, this);
			m_allActivatedPids.add(pid);
			m_activatedSelfConfigComponents.add(pid);
		}
	}

	synchronized void unregisterComponentConfiguration(String pid) {
		if (pid == null) {
			s_logger.warn("pid is null");
			return;
		}
		s_logger.info("Removing component configuration for pid {}", pid);
		m_servicePidByPid.remove(pid);
		m_factoryPidByPid.remove(pid);
		m_activatedSelfConfigComponents.remove(pid);
		m_allActivatedPids.remove(pid);
	}

	boolean mergeWithDefaults(OCD ocd, Map<String, Object> properties) throws KuraException {
		boolean changed = false;
		Set<String> keys = properties.keySet();

		Map<String, Object> defaults = ComponentUtil.getDefaultProperties(ocd, m_ctx);
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

	Map<String, Object> decryptPasswords(ComponentConfiguration config) {
		Map<String, Object> configProperties = config.getConfigurationProperties();
		for (Entry<String, Object> property : configProperties.entrySet()) {
			if (property.getValue() instanceof Password) {
				try {
					Password decryptedPassword = new Password(m_cryptoService.decryptAes(property.getValue().toString().toCharArray()));
					configProperties.put(property.getKey(), decryptedPassword);
				} catch (Exception e) {
				}
			}
		}
		return configProperties;
	}
	
	// ----------------------------------------------------------------
	//
	// Private APIs
	//
	// ----------------------------------------------------------------

	private void encryptPasswords(ComponentConfiguration config){
		Map<String, Object> propertiesToUpdate = config.getConfigurationProperties();
		encryptPasswords(propertiesToUpdate);
	}

	private void encryptPasswords(Map<String, Object> propertiesToUpdate){
		for (Entry<String, Object> property : propertiesToUpdate.entrySet()) {
			if (property.getValue() != null) {
				if (property.getValue() instanceof Password) {
					try {
						propertiesToUpdate.put(property.getKey(), new Password(m_cryptoService.encryptAes(property.getValue().toString().toCharArray())));
					} catch (Exception e) {
						s_logger.warn("Failed to encrypt Password property: {}", property.getKey());
						propertiesToUpdate.remove(property.getKey());
					}
				}
			}
		}
	}

	private void encryptConfigs(List<? extends ComponentConfiguration> configs) {
		for (ComponentConfiguration config : configs) {
			if (config instanceof ComponentConfigurationImpl) {
				encryptPasswords(config);
			}
		}
	}

	private boolean allSnapshotsUnencrypted() {
		try {
			Set<Long> snapshotIDs = getSnapshots();
			if (snapshotIDs == null || snapshotIDs.size() == 0) {
				return false;
			}
			Long[] snapshots = snapshotIDs.toArray(new Long[] {});

			for (Long snapshot : snapshots) {

				try {
					//Verify if the current snapshot is encrypted
					loadEncryptedSnapshotFileContent(snapshot);
					return false;
				} catch (Exception e) {
				}
			}
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	private void encryptPlainSnapshots() throws Exception {
		Set<Long> snapshotIDs = getSnapshots();
		if (snapshotIDs == null || snapshotIDs.isEmpty()) {
			return;
		}
		Long[] snapshots = snapshotIDs.toArray(new Long[] {});

		for (Long snapshot : snapshots) {
			File fSnapshot = getSnapshotFile(snapshot);
			if (!fSnapshot.exists()) {
				throw new KuraException(KuraErrorCode.CONFIGURATION_SNAPSHOT_NOT_FOUND, snapshot);
			}

			//
			// Unmarshall
			XmlComponentConfigurations xmlConfigs = null;
			FileReader fr = null;
			BufferedReader br = null;
			try {
				fr = new FileReader(fSnapshot);
				br = new BufferedReader(fr);
				String line = "";
				StringBuilder entireFile = new StringBuilder();
				while ((line = br.readLine()) != null) {
					entireFile.append(line);
				} // end while
				xmlConfigs = XmlUtil.unmarshal(entireFile.toString(), XmlComponentConfigurations.class);
			} finally {
				if (br != null) {
					br.close();
				}
				if (fr != null) {
					fr.close();
				}
			}
			List<ComponentConfigurationImpl> configs = xmlConfigs.getConfigurations();
			encryptConfigs(configs);

			//Writes an encrypted snapshot with encrypted passwords.
			writeSnapshot(snapshot, xmlConfigs);
		}
	}

	private synchronized long saveSnapshot(List<? extends ComponentConfiguration> configs) throws KuraException {

		List<ComponentConfigurationImpl> configImpls = new ArrayList<ComponentConfigurationImpl>();
		for (ComponentConfiguration config : configs) {
			if (config instanceof ComponentConfigurationImpl) {
				configImpls.add((ComponentConfigurationImpl) config);
			}
		}

		// Build the XML structure
		XmlComponentConfigurations conf = new XmlComponentConfigurations();
		conf.setConfigurations(configImpls);

		// Write it to disk: marshall
		long sid = (new Date()).getTime();

		// Do not save the snapshot in the past
		Set<Long> snapshotIDs = getSnapshots();
		if (snapshotIDs != null && snapshotIDs.size() > 0) {
			Long[] snapshots = snapshotIDs.toArray(new Long[] {});
			Long lastestID = snapshots[snapshotIDs.size() - 1];

			if (lastestID != null && sid <= lastestID) {
				s_logger.warn("Snapshot ID: {} is in the past. Adjusting ID to: {} + 1", sid, lastestID);
				sid = lastestID + 1;
			}
		}

		// Write snapshot
		writeSnapshot(sid, conf);

		// Garbage Collector for number of Snapshots Saved
		garbageCollectionOldSnapshots();
		return sid;
	}

	private void writeSnapshot(long sid, XmlComponentConfigurations conf) throws KuraException {
		File fSnapshot = getSnapshotFile(sid);

		FileOutputStream fos = null;
		OutputStreamWriter osw = null;
		try {
			s_logger.info("Writing snapshot - Saving {}...", fSnapshot.getAbsolutePath());
			fos = new FileOutputStream(fSnapshot);
			osw = new OutputStreamWriter(fos, "UTF-8");
			String xmlResult = XmlUtil.marshal(conf);
			char[] encryptedXML = m_cryptoService.encryptAes(xmlResult.toCharArray());
			osw.append(new String(encryptedXML));
			osw.flush();
			fos.flush();
			fos.getFD().sync();
			s_logger.info("Writing snapshot - Saving {}... Done.", fSnapshot.getAbsolutePath());
		} catch (Throwable t) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, t);
		} finally {
			if (osw != null){
				try {
					osw.close();
				} catch (IOException e) {

				}
			}
			if(fos != null){
				try {
					fos.close();
				} catch (IOException e) {

				}
			}
		}
	}

	private ComponentConfiguration getConfigurableComponentConfiguration(String pid) {
		ComponentConfiguration cc = null;
		try {

			Tocd ocd = getOCDForPid(pid);

			Configuration cfg = m_configurationAdmin.getConfiguration(m_servicePidByPid.get(pid), null);
			Map<String, Object> props = CollectionsUtil.dictionaryToMap(cfg.getProperties(), ocd);

			cc = new ComponentConfigurationImpl(pid, ocd, props);
		} catch (Exception e) {
			s_logger.error("Error getting Configuration for component: " + pid + ". Ignoring it.", e);
		}
		return cc;
	}

	private ComponentConfiguration getSelfConfiguringComponentConfiguration(String pid) {
		ComponentConfiguration cc = null;
		try {
			ServiceReference<?>[] refs = m_ctx.getBundleContext().getServiceReferences((String) null, null);
			if (refs != null) {
				for (ServiceReference<?> ref : refs) {
					String ppid = (String) ref.getProperty(Constants.SERVICE_PID);
					if (pid.equals(ppid)) {
						Object obj = m_ctx.getBundleContext().getService(ref);
						try {
							if (obj instanceof SelfConfiguringComponent) {
								SelfConfiguringComponent selfConfigComp = null;
								selfConfigComp = (SelfConfiguringComponent) obj;
								try {

									cc = selfConfigComp.getConfiguration();
									if (cc.getPid() == null || !cc.getPid().equals(pid)) {
										s_logger.error("Invalid pid for returned Configuration of SelfConfiguringComponent with pid: " + pid + ". Ignoring it.");
										return null;
									}

									OCD ocd = cc.getDefinition();
									if (ocd != null) {
										List<AD> ads = ocd.getAD();

										if (ads != null) {
											for (AD ad : ads) {
												String adId = ad.getId();
												String adType = ad.getType().value();

												if (adId == null) {
													s_logger.error(
															"null required id for AD for returned Configuration of SelfConfiguringComponent with pid: {}", pid);
													return null;
												}
												if (adType == null) {
													s_logger.error(
															"null required type for AD id: {} for returned Configuration of SelfConfiguringComponent with pid: {}",
															adId, pid);
													return null;
												}

												Map<String, Object> props = cc.getConfigurationProperties();
												if (props != null) {
													Object value = props.get(adId);
													if (value != null) {
														String propType = value.getClass().getSimpleName();
														try {
															s_logger.debug("pid: {}, property name: {}, type: {}, value: {}", new Object[] {pid, adId, propType, value});
															Scalar.fromValue(propType);
															if (!propType.equals(adType)) {
																s_logger.error("Type: {} for property named: {} does not match the AD type: {} for returned Configuration of SelfConfiguringComponent with pid: {}",
																		new Object[] {propType, adId, adType, pid});
																return null;
															}
														} catch (IllegalArgumentException e) {
															s_logger.error("Invalid class: {} for property named: {} for returned Configuration of SelfConfiguringComponent with pid: " + pid, propType, adId);
															return null;
														}
													}
												}
											}
										}
									}
								} catch (KuraException e) {
									s_logger.error("Error getting Configuration for component: " + pid + ". Ignoring it.", e);
								}
							} else {
								s_logger.error("Component " + obj + " is not a SelfConfiguringComponent. Ignoring it.");
							}
						} finally {
							m_ctx.getBundleContext().ungetService(ref);
						}
					}
				}
			}
		} catch (InvalidSyntaxException e) {
			s_logger.error("Error getting Configuration for component: " + pid + ". Ignoring it.", e);
		}

		return cc;
	}

	private TreeSet<Long> getSnapshotsInternal() {
		// keeps the list of snapshots ordered
		TreeSet<Long> ids = new TreeSet<Long>();
		String configDir = getSnapshotsDirectory();
		if (configDir != null) {
			File fConfigDir = new File(configDir);
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

	String getSnapshotsDirectory() {
		return m_systemService.getKuraSnapshotsDirectory();
	}

	private File getSnapshotFile(long id) {
		String configDir = getSnapshotsDirectory();
		StringBuilder sbSnapshot = new StringBuilder(configDir);
		sbSnapshot.append(File.separator).append("snapshot_").append(id).append(".xml");

		String snapshot = sbSnapshot.toString();
		return new File(snapshot);
	}

	private void garbageCollectionOldSnapshots() {
		// get the current snapshots and compared with the maximum number we
		// need to keep
		TreeSet<Long> sids = getSnapshotsInternal();

		int currCount = sids.size();
		int maxCount = m_systemService.getKuraSnapshotsCount();
		while (currCount > maxCount) {

			// preserve snapshot ID 0 as this will be considered the seeding
			// one.
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

	private void loadLatestSnapshotInConfigAdmin() throws KuraException {
		//
		// save away initial configuration
		List<ComponentConfigurationImpl> configs = loadLatestSnapshotConfigurations();
		if (configs == null) {
			return;
		}
		for (ComponentConfiguration config : configs) {
			if (config != null) {
				Map<String, Object> props = config.getConfigurationProperties();
				if (props != null) {
					String factoryPid = (String) props.get(ConfigurationAdmin.SERVICE_FACTORYPID);
					
					if (factoryPid != null) {
						String pid = config.getPid();
						s_logger.info("Creating configuration with pid: {} and factory pid: {}", pid, factoryPid);
						try {
							createFactoryConfiguration(factoryPid, pid, props, false);
						} catch (KuraException e) {
							s_logger.warn("Error creating configuration with pid: "+pid+" and factory pid: {}", factoryPid, e);
						}
					} else {
						try {
							s_logger.debug("Pushing config to config admin: {}", config.getPid());

							// push it to the ConfigAdmin
							Configuration cfg = m_configurationAdmin.getConfiguration(config.getPid(), null);
							
							// set kura.service.pid if missing
							Map<String, Object> newProperties = new HashMap<String, Object>(props);
							if (!newProperties.containsKey(ConfigurationService.KURA_SERVICE_PID)) {
								newProperties.put(ConfigurationService.KURA_SERVICE_PID, newProperties.get(Constants.SERVICE_PID));
							}
							
							cfg.update(CollectionsUtil.mapToDictionary(newProperties));

							// track it as a pending Configuration
							// for which we are expecting a confirmation
							m_pendingConfigurationPids.add(config.getPid());
						} catch (IOException e) {
							s_logger.warn("Error seeding initial properties to ConfigAdmin for pid: {}", config.getPid(), e);
						}
					}
				}
			}
		}
	}

	private List<ComponentConfigurationImpl> loadLatestSnapshotConfigurations() throws KuraException {
		//
		// Get the latest snapshot file to use as initialization
		Set<Long> snapshotIDs = getSnapshots();
		if (snapshotIDs == null || snapshotIDs.isEmpty()) {
			return null;
		}

		Long[] snapshots = snapshotIDs.toArray(new Long[] {});
		Long lastestID = snapshots[snapshotIDs.size() - 1];

		//
		// Unmarshall
		s_logger.info("Loading init configurations from: {}...", lastestID);

		List<ComponentConfigurationImpl> configs = null;
		try {
			XmlComponentConfigurations xmlConfigs = loadEncryptedSnapshotFileContent(lastestID);
			if (xmlConfigs != null) {
				configs = xmlConfigs.getConfigurations();
			}
		} catch (Exception e) {
			s_logger.info("Unable to decrypt snapshot! Fallback to unencrypted snapshots mode.");
			try {
				if (allSnapshotsUnencrypted()) {
					encryptPlainSnapshots();
					configs = loadLatestSnapshotConfigurations();
				}
			} catch (Exception ex) {
				throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
			}
		}
		
		return configs;
	}


	XmlComponentConfigurations loadEncryptedSnapshotFileContent(long snapshotID) throws KuraException {
		File fSnapshot = getSnapshotFile(snapshotID);
		if (!fSnapshot.exists()) {
			throw new KuraException(KuraErrorCode.CONFIGURATION_SNAPSHOT_NOT_FOUND, fSnapshot.getAbsolutePath());
		}

		FileReader fr = null;
		BufferedReader br = null;
		StringBuilder entireFile = new StringBuilder();
		try {
			fr = new FileReader(fSnapshot);
			br = new BufferedReader(fr);
			String line = "";
			while ((line = br.readLine()) != null) {
				entireFile.append(line);
			}
		} catch (IOException e) {
			s_logger.error("Error loading file from disk", e);
			return null;
		} finally {
			try {
				if (br != null) {
					br.close();
				}
			} catch (IOException e) {
			}
			try {
				if (fr != null) {
					fr.close();
				}
			} catch (IOException e) {
			}
		}
		
		//File loaded, try to decrypt and unmarshall
		String decryptedContent = new String(m_cryptoService.decryptAes(entireFile.toString().toCharArray()));
		

		XmlComponentConfigurations xmlConfigs = null;
		try {
			xmlConfigs = XmlUtil.unmarshal(decryptedContent, XmlComponentConfigurations.class);
		} catch (XMLStreamException e) {
			s_logger.warn("Error parsing xml", e);
		} catch (FactoryConfigurationError e) { // FIXME: is this really needed?
			s_logger.warn("Error parsing xml", e);
		}
		
		return ConfigurationUpgrade.upgrade(xmlConfigs);
	}
	
	private void updateConfigurationInternal(String pid, Map<String, Object> properties, boolean snapshotOnConfirmation) throws KuraException {
		s_logger.debug("Attempting update configuration for {}", pid);

		if (!m_allActivatedPids.contains(pid)) {
			s_logger.info("UpdatingConfiguration ignored as ConfigurableComponent {} is NOT tracked.", pid);
			return;
		}
		if (properties == null) {
			s_logger.info("UpdatingConfiguration ignored as properties for ConfigurableComponent {} are NULL.", pid);
			return;
		}

		// get the OCD from the registered ConfigurableComponents
		OCD registerdOCD = getRegisteredOCD(pid);
		if (registerdOCD == null) {
			s_logger.info("UpdatingConfiguration ignored as OCD for pid {} cannot be found.", pid);
			return;
		}

		Map<String, Object> mergedProperties = new HashMap<String, Object>();
		mergeWithDefaults(registerdOCD, mergedProperties);
		
		if (!m_activatedSelfConfigComponents.contains(pid)) {
			try {
				// get the current running configuration for the selected component
				Configuration config = m_configurationAdmin.getConfiguration(m_servicePidByPid.get(pid));
				Map<String, Object> runningProps = CollectionsUtil.dictionaryToMap(config.getProperties(), registerdOCD);

				mergedProperties.putAll(runningProps);
			} catch (IOException e) {
				s_logger.info("merge with running failed!");
				throw new KuraException(KuraErrorCode.CONFIGURATION_UPDATE, e, pid);
			}
		}
				
		mergedProperties.putAll(properties);

		try {
			updateComponentConfiguration(pid, mergedProperties, snapshotOnConfirmation);
			s_logger.info("Updating Configuration of ConfigurableComponent {} ... Done.", pid);
		} catch (IOException e) {
			s_logger.warn("Error updating Configuration of ConfigurableComponent with pid {}", pid, e);
			throw new KuraException(KuraErrorCode.CONFIGURATION_UPDATE, e, pid);
		}
	}

	private void rollbackConfigurationInternal(String pid, Map<String, Object> properties, boolean snapshotOnConfirmation) throws KuraException {
		s_logger.debug("Attempting to rollback configuration for {}", pid);

		if (!m_allActivatedPids.contains(pid)) {
			s_logger.info("UpdatingConfiguration ignored as ConfigurableComponent {} is NOT tracked.", pid);
			return;
		}
		if (properties == null) {
			s_logger.info("UpdatingConfiguration ignored as properties for ConfigurableComponent {} are NULL.", pid);
			return;
		}

		// get the OCD from the registered ConfigurableComponents
		OCD registerdOCD = getRegisteredOCD(pid);
		if (registerdOCD == null) {
			s_logger.info("UpdatingConfiguration ignored as OCD for pid {} cannot be found.", pid);
			return;
		}

		Map<String, Object> mergedProperties = new HashMap<String, Object>();
		mergeWithDefaults(registerdOCD, mergedProperties);
						
		mergedProperties.putAll(properties);
		try {
			updateComponentConfiguration(pid, mergedProperties, snapshotOnConfirmation);
			s_logger.info("Updating Configuration of ConfigurableComponent {} ... Done.", pid);
		} catch (IOException e) {
			s_logger.warn("Error updating Configuration of ConfigurableComponent with pid {}", pid, e);
			throw new KuraException(KuraErrorCode.CONFIGURATION_UPDATE, e, pid);
		}
	}

	private void updateComponentConfiguration(String pid, Map<String, Object> mergedProperties, boolean snapshotOnConfirmation) throws KuraException, IOException {
		if (!m_activatedSelfConfigComponents.contains(pid)) {

			// load the ocd to do the validation
			BundleContext ctx = m_ctx.getBundleContext();
			// FIXME: why the returned ocd is always null?
			ObjectClassDefinition ocd = ComponentUtil.getObjectClassDefinition(ctx, m_servicePidByPid.get(pid));

			// Validate the properties to be applied and set them
			validateProperties(pid, ocd, mergedProperties);
		} else {
			// FIXME: validation of properties for self-configuring
			// components
		}

		if (!snapshotOnConfirmation) {
			m_pendingConfigurationPids.add(pid);
		} else {
			s_logger.info("Snapshot on EventAdmin configuration will be taken for {}.", pid);
		}
		
		// Update the new properties
		// use ConfigurationAdmin to do the update
		Configuration config = m_configurationAdmin.getConfiguration(m_servicePidByPid.get(pid), null);
		config.update(CollectionsUtil.mapToDictionary(mergedProperties));
	}

	private OCD getRegisteredOCD(String pid) {
		// try to get the OCD from the registered ConfigurableComponents
		OCD registeredOCD = getOCDForPid(pid);
		// otherwise try to get it from the registered SelfConfiguringComponents
		if (registeredOCD == null) {
			ComponentConfiguration config = getSelfConfiguringComponentConfiguration(pid);
			if (config != null) {
				registeredOCD = config.getDefinition();
			}
		}
		return registeredOCD;
	}

	private void validateProperties(String pid, ObjectClassDefinition ocd, Map<String, Object> updatedProps) throws KuraException {
		if (ocd != null) {

			// build a map of all the attribute definitions
			Map<String, AttributeDefinition> attrDefs = new HashMap<String, AttributeDefinition>();
			AttributeDefinition[] defs = ocd.getAttributeDefinitions(ObjectClassDefinition.ALL);
			for (AttributeDefinition def : defs) {
				attrDefs.put(def.getID(), def);
			}

			// loop over the proposed property values
			// and validate them against the definition
			for (Entry<String, Object> property : updatedProps.entrySet()) {
				
				String key = property.getKey();
				AttributeDefinition attrDef = attrDefs.get(key);
				
				// is attribute undefined?
				if (attrDef == null) {
					// we do not have an attribute descriptor to the validation
					// against
					// As OSGI insert attributes at runtime like service.pid,
					// component.name,
					// for the attribute for which we do not have a definition,
					// just accept them.
					continue;
				}
				
				// validate the attribute value
				Object objectValue = property.getValue(); 
				String stringValue = StringUtil.valueToString(objectValue);
				if (stringValue != null) {
					String result = attrDef.validate(stringValue);
					if (result != null && !result.isEmpty()) {
						throw new KuraException(KuraErrorCode.CONFIGURATION_ATTRIBUTE_INVALID, attrDef.getID() + ": " + result);
					}
				}
				
			}

			// make sure all required properties are set
			OCD ocdFull = getOCDForPid(pid);
			if (ocdFull != null) {
				for (AD attrDef : ocdFull.getAD()) {
					// to the required attributes make sure a value is defined.
					if (attrDef.isRequired()) {
						if (updatedProps.get(attrDef.getId()) == null) {
							// if the default one is not defined, throw
							// exception.
							throw new KuraException(KuraErrorCode.CONFIGURATION_REQUIRED_ATTRIBUTE_MISSING, attrDef.getId());
						}
					}
				}
			}
		}
	}
		
	private synchronized List<ComponentConfiguration> buildCurrentConfiguration(List<ComponentConfiguration> configsToUpdate) throws KuraException {
		List<ComponentConfiguration> result = new ArrayList<ComponentConfiguration>();
		
		// Merge the current configuration of registered components with the provided configurations.
		// It is assumed that the PIDs in the provided configurations is a subset of the registered PIDs. 
		List<ComponentConfiguration> currentConfigs = getComponentConfigurations();
		if (currentConfigs != null) {
			for (ComponentConfiguration currentConfig : currentConfigs) {
				// either add this configuration or a new one obtained by merging its properties with the ones provided
				ComponentConfiguration cc = currentConfig;
				String pid = currentConfig.getPid();
				if (configsToUpdate != null) {
					for (ComponentConfiguration configToUpdate : configsToUpdate) {
						if (configToUpdate.getPid().equals(pid)) {
							Map<String, Object> props = new HashMap<String, Object>();
							if (currentConfig.getConfigurationProperties() != null) {
								props.putAll(currentConfig.getConfigurationProperties());
							}
							if (configToUpdate.getConfigurationProperties() != null) {
								props.putAll(configToUpdate.getConfigurationProperties());
							}
							cc = new ComponentConfigurationImpl(pid, (Tocd) configToUpdate.getDefinition(), props);
							break;
						}
					}
				}
				result.add(cc);
			}
		}

		// complete the returned configurations adding the snapshot configurations
		// of those components not yet in the list.
		List<ComponentConfigurationImpl> snapshotConfigs = loadLatestSnapshotConfigurations();
		if (snapshotConfigs != null) {
			for (ComponentConfigurationImpl snapshotConfig : snapshotConfigs) {
				boolean found = false;
				for (ComponentConfiguration config : result) {
					if (config.getPid().equals(snapshotConfig.getPid())) {
						found = true;
						break;
					}
				}
				if (!found) {
					// Add old configurations (or not yet tracked ones) present
					result.add(snapshotConfig);
				}
			}
		}

		// remove configurations being deleted
		Iterator<String> it = m_pendingDeletePids.iterator();
		while (it.hasNext()) {
			String deletePid = it.next();
			for (ComponentConfiguration config : result) {
				if (config.getPid().equals(deletePid)) {
					it.remove();
					result.remove(config);
					break;
				}
			}
		}

		return result;
	}
	
	private Tocd getOCDForPid(String pid){
		Tocd ocd = m_ocds.get(m_factoryPidByPid.get(pid));
		if (ocd == null) {
			ocd = m_ocds.get(m_servicePidByPid.get(pid));
		}
		return ocd;
	}
	
	private String getPidByServicePid(String servicePid){
		for(Entry<String, String> entry : m_servicePidByPid.entrySet()){
			if (entry.getValue().equals(servicePid)) {
				return entry.getKey();
			}
		}
		return servicePid;
	}
}

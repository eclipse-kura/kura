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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
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
public class ConfigurationServiceImpl implements ConfigurationService, ConfigurationListener {
	private static final Logger s_logger = LoggerFactory.getLogger(ConfigurationServiceImpl.class);

	private ComponentContext m_ctx;
	private CloudConfigurationHandler m_cloudHandler;
	private ServiceTracker<?, ?> m_serviceTracker;
	private BundleTracker<Bundle> m_bundleTracker;

	@SuppressWarnings("unused")
	private MetaTypeService m_metaTypeService;
	private ConfigurationAdmin m_configurationAdmin;
	private SystemService m_systemService;
	private CryptoService m_cryptoService;

	// contains all the PIDs - both of regular and self components
	private Set<String> m_allPids;

	// contains the self configuring components ONLY!
	private Set<String> m_selfConfigComponents;

	// contains the current configuration of regular components ONLY
	private Map<String, Tocd> m_ocds;

	// contains all pids which have been configured and for which we have not
	// received the corresponding ConfigurationEvent yet
	private Set<String> m_pendingConfigurationPids;

	private final char[] GENERIC_PLACEHOLDER = "PlaceHolder".toCharArray();

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
		m_allPids = new HashSet<String>();
		m_selfConfigComponents = new HashSet<String>();
		m_pendingConfigurationPids = new HashSet<String>();
		m_ocds = new HashMap<String, Tocd>();
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
		m_cloudHandler = new CloudConfigurationHandler(m_ctx.getBundleContext(), this, m_systemService);
		m_cloudHandler.open();

		m_serviceTracker = new ConfigurableComponentTracker(m_ctx.getBundleContext(), this);
		m_serviceTracker.open(true);

		m_bundleTracker = new ComponentMetaTypeBundleTracker(m_ctx.getBundleContext(), m_configurationAdmin, this);
		m_bundleTracker.open();
	}

	protected void deactivate(ComponentContext componentContext) {
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
	// ConfigurationListener
	//
	// ----------------------------------------------------------------

	@Override
	public synchronized void configurationEvent(ConfigurationEvent event) {
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
		} catch (Exception e) {
			s_logger.error("Error taking snapshot after ConfigurationEvent", e);
		}
	}

	// ----------------------------------------------------------------
	//
	// Service APIs
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
	public List<ComponentConfiguration> getComponentConfigurations() throws KuraException {
		List<ComponentConfiguration> configs = new ArrayList<ComponentConfiguration>();

		// assemble all the configurations we have
		// clone the list to avoid concurrent modifications
		List<String> allPids = new ArrayList<String>(m_allPids);
		ComponentConfiguration cc = null;
		for (String pid : allPids) {
			try {
				cc = null;
				cc= getComponentConfiguration(pid);
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
		ComponentConfiguration cc = null;
		if (!m_selfConfigComponents.contains(pid)) {
			cc = getConfigurableComponentConfiguration(pid);
		} else {
			cc = getSelfConfiguringComponentConfiguration(pid);
		}

		//decryptPasswords(cc);
		replacePasswordsWithPlaceholder(cc);
		return cc;
	}

	private Map<String, Object> replacePasswordsWithPlaceholder(ComponentConfiguration config) {
		Map<String, Object> configProperties = config.getConfigurationProperties();
		Iterator<String> keys = configProperties.keySet().iterator();
		while (keys.hasNext()) {
			String key = keys.next();
			Object value = configProperties.get(key);
			if (value instanceof Password) {
				try {
					Password decryptedPassword = new Password(GENERIC_PLACEHOLDER );
					configProperties.put(key, decryptedPassword);
				} catch (Exception e) {
				}
			}
		}
		return configProperties;
	}

	@SuppressWarnings("unchecked")
	@Override
	public synchronized void updateConfiguration(String pidToUpdate, Map<String, Object> propertiesToUpdate) throws KuraException {
		List<ComponentConfigurationImpl> configs = new ArrayList<ComponentConfigurationImpl>();
		ComponentConfigurationImpl cci = new ComponentConfigurationImpl(pidToUpdate, null, propertiesToUpdate);
		configs.add(cci);
		updateConfigurations((List<ComponentConfiguration>) (List<?>) configs);	
	}

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
		Set<String> pids = new HashSet<String>(m_allPids);
		pids.removeAll(m_selfConfigComponents);
		pids.removeAll(snapshotPids);

		for (String pid : pids) {
			s_logger.info("Rolling back to default configuration for component pid: '{}'", pid);
			try {
				ServiceReference<?>[] refs = m_ctx.getBundleContext().getServiceReferences((String) null, null);
				if (refs != null) {
					for (ServiceReference<?> ref : refs) {
						String ppid = (String) ref.getProperty("component.name");
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
		if (causes.size() > 0) {
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

	public List<ComponentConfiguration> getSnapshot(long sid) throws KuraException {
		XmlComponentConfigurations xmlConfigs = loadEncryptedSnapshotFileContent(sid);

		//List<ComponentConfigurationImpl> decryptedConfigs = new ArrayList<ComponentConfigurationImpl>();
		List<ComponentConfigurationImpl> configs = xmlConfigs.getConfigurations();
		for (ComponentConfigurationImpl config : configs) {
			if (config != null) {
				try {
					Map<String, Object> decryptedProperties = decryptPasswords(config);
					config.setProperties(decryptedProperties);
					//decryptedConfigs.add(config);
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
		boolean snapshotOnConfirmation = false;
		List<Throwable> causes = new ArrayList<Throwable>();
		for (ComponentConfiguration config : configsToUpdate) {
			if (config != null) {
				encryptPasswords(config);
				replacePlaceholdersWithPasswords(config);
			}
		}

		List<ComponentConfiguration> configs = buildCurrentConfiguration(configsToUpdate);

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

		saveSnapshot(configs);

		if (causes.size() > 0) {
			throw new KuraPartialSuccessException("updateConfigurations", causes);
		}
	}

	// ----------------------------------------------------------------
	//
	// Package APIs
	//
	// ----------------------------------------------------------------
	synchronized void registerComponentConfiguration(Bundle bundle, String pid) throws KuraException {
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
						m_ocds.put(pid, ocd);
					}
				} catch (Throwable t) {
					s_logger.error("Error reading ObjectClassDefinition for " + pid, t);
				}
				s_logger.info("Registration Completed for Component {}.", pid);
			} catch (Exception e) {
				s_logger.error("Error initializing Component Configuration", e);
			}
		}
	}

	synchronized void registerSelfConfiguringComponent(String pid) throws KuraException {
		if (pid == null) {
			throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR);
		}

		m_allPids.add(pid);
		m_selfConfigComponents.add(pid);
	}

	void unregisterComponentConfiguration(String pid) {
		if (pid == null) {
			return;
		}

		s_logger.debug("Removing component configuration for " + pid);
		m_allPids.remove(pid);
		m_ocds.remove(pid);
		m_selfConfigComponents.remove(pid);
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
		Iterator<String> keys = configProperties.keySet().iterator();
		while (keys.hasNext()) {
			String key = keys.next();
			Object value = configProperties.get(key);
			if (value instanceof Password) {
				try {
					Password decryptedPassword = new Password(m_cryptoService.decryptAes(value.toString().toCharArray()));
					configProperties.put(key, decryptedPassword);
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
		Iterator<String> keys = propertiesToUpdate.keySet().iterator();
		while (keys.hasNext()) {
			String key = keys.next();
			Object value = propertiesToUpdate.get(key);
			if (value != null) {
				if (value instanceof Password && !Arrays.equals(value.toString().toCharArray(), GENERIC_PLACEHOLDER)) {
					try {
						propertiesToUpdate.put(key, new Password(m_cryptoService.encryptAes(value.toString().toCharArray())));
					} catch (Exception e) {
						s_logger.warn("Failed to encrypt Password property: {}", key);
						propertiesToUpdate.remove(key);
					}
				}
			}
		}
	}
	
	private void replacePlaceholdersWithPasswords(ComponentConfiguration config) {
		Map<String, Object> propertiesToUpdate = config.getConfigurationProperties();
		Iterator<String> keys = propertiesToUpdate.keySet().iterator();
		while (keys.hasNext()) {
			String key = keys.next();
			Object value = propertiesToUpdate.get(key);
			if (value != null) {
				if (value instanceof Password && Arrays.equals(value.toString().toCharArray(), GENERIC_PLACEHOLDER)) {
					try {

						String pid= config.getPid();
						Tocd ocd = m_ocds.get(pid);

						Configuration cfg = m_configurationAdmin.getConfiguration(pid);
						Map<String, Object> props = CollectionsUtil.dictionaryToMap(cfg.getProperties(), ocd);
						
						Password encPassword= (Password) props.get(key);
						
						propertiesToUpdate.put(key, encPassword);
					} catch (Exception e) {
						s_logger.error("Error while trying to remove placeholders for: {}", key);
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
		if (snapshotIDs == null || snapshotIDs.size() == 0) {
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
				String entireFile = "";
				while ((line = br.readLine()) != null) {
					entireFile += line;
				} // end while
				xmlConfigs = XmlUtil.unmarshal(entireFile, XmlComponentConfigurations.class);
			} finally {
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
		//List<ComponentConfigurationImpl> configImpls = configs;//encryptConfigs(configs);

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
			Long[] snapshots = snapshotIDs.toArray(new Long[] {});
			Long lastestID = snapshots[snapshotIDs.size() - 1];

			if (lastestID != null && sid <= lastestID) {
				s_logger.warn("Snapshot ID: {} is in the past. Adjusting ID to: {} + 1", sid, lastestID);
				sid = lastestID + 1;
			}
		}

		//Write snapshot
		writeSnapshot(sid, conf);

		//
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

			Tocd ocd = m_ocds.get(pid);

			Configuration cfg = m_configurationAdmin.getConfiguration(pid);
			Map<String, Object> props = CollectionsUtil.dictionaryToMap(cfg.getProperties(), ocd);

			Map<String, Object> cleanedProps= cleanProperties(props, pid);

			cc = new ComponentConfigurationImpl(pid, ocd, cleanedProps); 
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
					String ppid = (String) ref.getProperty("component.name");
					if (pid.equals(ppid)) {		
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
				} 
			}
		} catch (InvalidSyntaxException e) {
			s_logger.error("Error getting Configuration for component: "+pid+". Ignoring it.", e);
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
		String configDirs = m_systemService.getKuraSnapshotsDirectory();
		return configDirs;
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
		if(configs == null){
			return;
		}
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
				} catch (IOException e) {
					s_logger.warn("Error seeding initial properties to ConfigAdmin for service: " + config.getPid(), e);
				}
			}
		}
	}

	private List<ComponentConfigurationImpl> loadLatestSnapshotConfigurations() throws KuraException {
		//
		// Get the latest snapshot file to use as initialization
		Set<Long> snapshotIDs = getSnapshots();
		if (snapshotIDs == null || snapshotIDs.size() == 0) {
			return null;
		}

		Long[] snapshots = snapshotIDs.toArray(new Long[] {});
		Long lastestID = snapshots[snapshotIDs.size() - 1];

		//
		// Unmarshall
		s_logger.info("Loading init configurations from: {}...", lastestID);
		XmlComponentConfigurations xmlConfigs = null;

		try {
			xmlConfigs = loadEncryptedSnapshotFileContent(lastestID);
		} catch (Exception e) {
			s_logger.info("Unable to decrypt snapshot! Fallback to unencrypted snapshots mode.");
			try {
				if (allSnapshotsUnencrypted()) {
					encryptPlainSnapshots();
					return loadLatestSnapshotConfigurations();
				}
			} catch (Exception ex) {
				throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
			}
		}
		return xmlConfigs.getConfigurations();
	}

	XmlComponentConfigurations loadEncryptedSnapshotFileContent(long snapshotID) throws KuraException{
		File fSnapshot = getSnapshotFile(snapshotID);
		if (!fSnapshot.exists()) {
			throw new KuraException(KuraErrorCode.CONFIGURATION_SNAPSHOT_NOT_FOUND, fSnapshot.getAbsolutePath());
		}

		XmlComponentConfigurations xmlConfigs= null;
		FileReader fr = null;
		BufferedReader br = null;
		try {
			fr = new FileReader(fSnapshot);
			br = new BufferedReader(fr);
			String line = "";
			String entireFile = "";
			while ((line = br.readLine()) != null) {
				entireFile += line;
			}

			//File loaded, try to decrypt and unmarshall
			String decryptedContent = new String(m_cryptoService.decryptAes(entireFile.toCharArray()));
			xmlConfigs = XmlUtil.unmarshal(decryptedContent, XmlComponentConfigurations.class);
		} catch (KuraException e) {
			s_logger.debug("KuraException: {}", e.getCode().toString());
			throw e;
		} catch (FileNotFoundException e) {
			s_logger.error("Error loading file from disk: not found. Message: {}", e.getMessage());
		} catch (IOException e) {
			s_logger.error("Error loading file from disk. Message: {}", e.getMessage());
		} catch (XMLStreamException e) {
			s_logger.error("Error parsing xml: {}", e.getMessage());
		} catch (FactoryConfigurationError e) {
			s_logger.error("Error parsing xml: {}", e.getMessage());
		}finally {			
			try {
				if (fr != null) {
					fr.close();
				}
			} catch (IOException e) {
			}
			try {
				if (br != null) {
					br.close();
				}
			} catch (IOException e) {

			}
		}
		return xmlConfigs;
	}

	private void updateConfigurationInternal(String pid, Map<String, Object> properties, boolean snapshotOnConfirmation) throws KuraException {
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
		OCD registerdOCD = getRegisteredOCD(pid);
		
		try {
			if (!m_selfConfigComponents.contains(pid) && registerdOCD != null) {
				//get the actual running configuration for the selected component
				Configuration config = m_configurationAdmin.getConfiguration(pid);
				Map<String, Object> runningProps = CollectionsUtil.dictionaryToMap(config.getProperties(), registerdOCD);

				//iterate through all the running properties and include in mergedProperties 
				//the ones that are missing in order to create a complete component configuration
				//eventual properties that are runtime only will be removed in next steps
				Iterator<String> keys= runningProps.keySet().iterator();
				while(keys.hasNext()){
					String key= keys.next();
					if(!mergedProperties.containsKey(key)){
						mergedProperties.put(key, runningProps.get(key));
					}
				}	
			}
		} catch (IOException e) {
			s_logger.info("merge with running failed!");
			throw new KuraException(KuraErrorCode.CONFIGURATION_UPDATE, e, pid);
		}

		if (registerdOCD != null) {
			boolean changed = mergeWithDefaults(registerdOCD, mergedProperties);
			if (changed) {
				s_logger.info("mergeWithDefaults returned " + changed);
			}
		}

		try {
			updateComponentConfiguration(pid, mergedProperties, snapshotOnConfirmation);

			s_logger.info("Updating Configuration of ConfigurableComponent {} ... Done.", pid);
		} catch (IOException e) {
			s_logger.error("Error updating Configuration of ConfigurableComponent " + pid, e);
			throw new KuraException(KuraErrorCode.CONFIGURATION_UPDATE, e, pid);
		}
	}

	private void rollbackConfigurationInternal(String pid, Map<String, Object> properties, boolean snapshotOnConfirmation) throws KuraException {
		s_logger.debug("Attempting to rollback configuration for {}", pid);

		if (!m_allPids.contains(pid)) {
			s_logger.info("RollbackConfiguration ignored as ConfigurableComponent {} is NOT tracked.", pid);
			return;
		}
		if (properties == null) {
			s_logger.info("RollbackConfiguration ignored as properties for ConfigurableComponent {} are NULL.", pid);
			return;
		}

		Map<String, Object> mergedProperties = new HashMap<String, Object>(properties);

		// Try to get the OCD from the registered ConfigurableComponents
		OCD registerdOCD = getRegisteredOCD(pid);

		if (registerdOCD != null) {
			boolean changed = mergeWithDefaults(registerdOCD, mergedProperties);
			if (changed) {
				s_logger.info("mergeWithDefaults returned " + changed);
			}
		}

		try {
			updateComponentConfiguration(pid, mergedProperties, snapshotOnConfirmation);

			s_logger.info("Updating Configuration of ConfigurableComponent {} to its default... Done.", pid);
		} catch (IOException e) {
			s_logger.error("Error updating Configuration of ConfigurableComponent " + pid, e);
			throw new KuraException(KuraErrorCode.CONFIGURATION_UPDATE, e, pid);
		}
	}
	
	private void updateComponentConfiguration(String pid, Map<String, Object> mergedProperties, boolean snapshotOnConfirmation) throws KuraException, IOException{
		if (!m_selfConfigComponents.contains(pid)) {

			// load the ocd to do the validation
			BundleContext ctx = m_ctx.getBundleContext();
			ObjectClassDefinition ocd = null;
			ocd = ComponentUtil.getObjectClassDefinition(ctx, pid);

			// Validate the properties to be applied and set them
			// TODO: it seems that code does not enter here: the ocd object is always null!
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

		Map<String, Object> cleanedProperties= cleanProperties(mergedProperties, pid);

		// Update the new properties
		// use ConfigurationAdmin to do the update
		Configuration config = m_configurationAdmin.getConfiguration(pid);
		config.update(CollectionsUtil.mapToDictionary(cleanedProperties));
	}

	private OCD getRegisteredOCD(String pid){
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
		return registerdOCD;
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
			Iterator<String> keys = updatedProps.keySet().iterator();
			while (keys.hasNext()) {

				String key = keys.next();
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
				Object objectValue = updatedProps.get(key);
				String stringValue = StringUtil.valueToString(objectValue);
				if (stringValue != null) {
					String result = attrDef.validate(stringValue);
					if (result != null && !result.isEmpty()) {
						throw new KuraException(KuraErrorCode.CONFIGURATION_ATTRIBUTE_INVALID, attrDef.getID() + ": " + result);
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
							// if the default one is not defined, throw
							// exception.
							throw new KuraException(KuraErrorCode.CONFIGURATION_REQUIRED_ATTRIBUTE_MISSING, attrDef.getId());
						}
					}
				}
			}
		}
	}

	private Map<String, Object> cleanProperties(Map<String, Object> mergedProperties, String pid) {
		if (!m_selfConfigComponents.contains(pid)) {
			Tocd componentOcd= m_ocds.get(pid);
			Set<String> metatypeNames= new HashSet<String>();
			if (componentOcd != null) {
				List<AD> attrDefs = componentOcd.getAD();
				if (attrDefs != null) {
					for (AD attrDef : attrDefs) {
						String name = attrDef.getName();
						if (!metatypeNames.contains(name)) {
							metatypeNames.add(name);
						}
					}
				}
			}

			Map<String, Object> cleanedProperties= new HashMap<String, Object> ();
			Set<String> mergedKeys= mergedProperties.keySet();

			for(String key: mergedKeys){
				if(metatypeNames.contains(key)){
					Object value= mergedProperties.get(key);
					cleanedProperties.put(key, value);
				}
			}
			return cleanedProperties;
		}else{
			Map<String, Object> cleanedProperties= new HashMap<String, Object> (mergedProperties);
			return cleanedProperties;
		}
	}

	private synchronized List<ComponentConfiguration> buildCurrentConfiguration(List<ComponentConfiguration> configsToUpdate) throws KuraException {
		// Build the current configuration
		ComponentConfiguration cc = null;
		ComponentConfigurationImpl cci = null;
		Map<String, Object> props = null;
		List<ComponentConfiguration> configs = new ArrayList<ComponentConfiguration>();

		// clone the list to avoid concurrent modifications
		List<String> allPids = new ArrayList<String>(m_allPids);
		for (String pid : allPids) {
			boolean isConfigToUpdate = false;
			if (configsToUpdate != null) {
				for (ComponentConfiguration configToUpdate : configsToUpdate) {
					if (configToUpdate.getPid().equals(pid)) {
						// found a match
						isConfigToUpdate = true;
						Map<String, Object> cleanedProps= cleanProperties(configToUpdate.getConfigurationProperties(), pid);
						ComponentConfiguration cleanedConfig= new ComponentConfigurationImpl(pid, (Tocd) configToUpdate.getDefinition(), cleanedProps);
						configs.add(cleanedConfig);
						break;
					}
				}
			}

			if (!isConfigToUpdate) {
				if (!m_selfConfigComponents.contains(pid)) {
					cc = getConfigurableComponentConfiguration(pid);
				} else {
					cc = getSelfConfiguringComponentConfiguration(pid);
				}
				if (cc != null && cc.getPid() != null && cc.getPid().equals(pid)) {
					props = cc.getConfigurationProperties();
					cci = new ComponentConfigurationImpl(pid, null, props);
					configs.add((ComponentConfigurationImpl) cci);
				}
			}
		}

		// merge the current configs with those in the latest snapshot
		List<ComponentConfigurationImpl> snapshotConfigs = loadLatestSnapshotConfigurations();
		if(snapshotConfigs != null){
			for (ComponentConfigurationImpl snapshotConfig : snapshotConfigs) {
				boolean found = false;
				for (ComponentConfiguration config : configs) {
					if (config.getPid().equals(snapshotConfig.getPid())) {
						found = true;
						break;
					}
				}
				if (!found) {
					configs.add(snapshotConfig);
				}
			}
		}

		return configs;
	}
}
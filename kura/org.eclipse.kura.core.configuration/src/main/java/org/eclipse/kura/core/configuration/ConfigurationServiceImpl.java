/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *     Red Hat Inc - Fix issue #462, Fix build warnings, Fix issue #596
 *        - Fix service registration
 *******************************************************************************/
package org.eclipse.kura.core.configuration;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
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
import org.eclipse.kura.configuration.ConfigurableComponent;
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
public class ConfigurationServiceImpl implements ConfigurationService {

    private static final Logger s_logger = LoggerFactory.getLogger(ConfigurationServiceImpl.class);

    private static final boolean TRACK_ONLY_RELEVANT_SERVICES = Boolean
            .getBoolean("org.eclipse.kura.core.configuration.trackOnlyRelevantServices");

    private interface ServiceHandler {

        void add(String servicePid, String kuraPid, String factoryPid);

        void remove(String servicePid, String kuraPid);
    }

    private final ServiceHandler trackerHandler1 = new ServiceHandler() {

        @Override
        public void add(String servicePid, String kuraPid, String factoryPid) {
            registerComponentConfiguration(kuraPid, servicePid, factoryPid);
        }

        @Override
        public void remove(String servicePid, String kuraPid) {
            unregisterComponentConfiguration(kuraPid);
        }
    };

    private final ServiceHandler trackerHandler2 = new ServiceHandler() {

        @Override
        public void add(String servicePid, String kuraPid, String factoryPid) {
            registerSelfConfiguringComponent(kuraPid, servicePid);
        }

        @Override
        public void remove(String servicePid, String kuraPid) {
            unregisterComponentConfiguration(servicePid);
        }
    };

    private ComponentContext m_ctx;

    private ServiceTracker<ConfigurableComponent, ConfigurableComponent> serviceTracker1;
    private ServiceTracker<SelfConfiguringComponent, SelfConfiguringComponent> serviceTracker2;
    private ConfigurableComponentTracker anyTracker;

    private BundleTracker<Bundle> m_bundleTracker;

    @SuppressWarnings("unused")
    private MetaTypeService m_metaTypeService;
    private ConfigurationAdmin m_configurationAdmin;
    private SystemService m_systemService;
    private CryptoService m_cryptoService;

    // contains all the PIDs (aka kura.service.pid) - both of configurable and self configuring components
    private final Set<String> m_allActivatedPids;

    // contains the self configuring components ONLY!
    private final Set<String> m_activatedSelfConfigComponents;

    // maps either service.pid or service.factoryPid to the related OCD
    private final Map<String, Tocd> m_ocds;

    // contains the service.factoryPid of all Factory Components
    private final Set<String> m_factoryPids;

    // maps the kura.service.pid to the associated service.factoryPid
    private final Map<String, String> m_factoryPidByPid;

    // contains all the pids (kura.service.pid) which have to be deleted
    private final Set<String> m_pendingDeletePids;

    // maps the kura.service.pid to the associated service.pid
    private final Map<String, String> m_servicePidByPid;

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
        this.m_allActivatedPids = new HashSet<String>();
        this.m_activatedSelfConfigComponents = new HashSet<String>();
        this.m_pendingDeletePids = new HashSet<String>();
        this.m_ocds = new HashMap<String, Tocd>();
        this.m_factoryPids = new HashSet<String>();
        this.m_factoryPidByPid = new HashMap<String, String>();
        this.m_servicePidByPid = new HashMap<String, String>();
    }

    // ----------------------------------------------------------------
    //
    // Activation APIs
    //
    // ----------------------------------------------------------------

    protected void activate(ComponentContext componentContext) throws InvalidSyntaxException {
        s_logger.info("activate...");

        // save the bundle context
        this.m_ctx = componentContext;

        // Load the latest snapshot and push it to ConfigurationAdmin
        try {
            loadLatestSnapshotInConfigAdmin();
        } catch (Exception e) {
            throw new ComponentException("Error loading latest snapshot", e);
        }

        //
        // start the trackers
        s_logger.info("Trackers being opened...");

        if (TRACK_ONLY_RELEVANT_SERVICES) {
            s_logger.info("Only tracking relevant services");
            this.serviceTracker1 = createTracker(ConfigurableComponent.class, this.trackerHandler1);
            this.serviceTracker2 = createTracker(SelfConfiguringComponent.class, this.trackerHandler2);

            this.serviceTracker1.open();
            this.serviceTracker2.open();
        } else {
            s_logger.info("Tracking all services");
            this.anyTracker = new ConfigurableComponentTracker(this.m_ctx.getBundleContext(), this);
            this.anyTracker.open(true);
        }

        this.m_bundleTracker = new ComponentMetaTypeBundleTracker(this.m_ctx.getBundleContext(), this);
        this.m_bundleTracker.open();
    }

    private <T> ServiceTracker<T, T> createTracker(final Class<T> clazz, final ServiceHandler handler) {
        return new ServiceTracker<T, T>(this.m_ctx.getBundleContext(), clazz, null) {

            @Override
            public T addingService(ServiceReference<T> reference) {
                s_logger.debug("addingService - ref: {}", reference);

                String servicePid = makeString(reference.getProperty(Constants.SERVICE_PID));
                String kuraPid = makeString(reference.getProperty(ConfigurationService.KURA_SERVICE_PID));
                String factoryPid = makeString(reference.getProperty(ConfigurationAdmin.SERVICE_FACTORYPID));

                if (servicePid == null) {
                    s_logger.debug("No servicePid found");
                    return null;
                }

                T service = super.addingService(reference);

                s_logger.debug("Adding service: {}", service);

                handler.add(servicePid, kuraPid, factoryPid);

                return service;
            }

            @Override
            public void removedService(ServiceReference<T> reference, T service) {
                s_logger.debug("removedService - ref: {}", reference);

                String servicePid = makeString(reference.getProperty(Constants.SERVICE_PID));
                String kuraPid = makeString(reference.getProperty(ConfigurationService.KURA_SERVICE_PID));

                if (servicePid == null) {
                    s_logger.debug("No servicePid found");
                    return;
                }

                s_logger.debug("remove - servicePid: {}, kuraPid: {}, service: {}",
                        new Object[] { servicePid, kuraPid, service });

                handler.remove(servicePid, kuraPid);

                super.removedService(reference, service);
            }
        };
    }

    protected void deactivate(ComponentContext componentContext) {
        s_logger.info("deactivate...");

        //
        // stop the trackers
        //

        if (this.anyTracker != null) {
            this.anyTracker.close();
            this.anyTracker = null;
        }
        if (this.serviceTracker2 != null) {
            this.serviceTracker2.close();
            this.serviceTracker2 = null;
        }
        if (this.serviceTracker1 != null) {
            this.serviceTracker1.close();
            this.serviceTracker1 = null;
        }
        if (this.m_bundleTracker != null) {
            this.m_bundleTracker.close();
            this.m_bundleTracker = null;
        }
    }

    // ----------------------------------------------------------------
    //
    // Service APIs
    //
    // ----------------------------------------------------------------

    @Override
    public Set<String> getConfigurableComponentPids() {
        if (this.m_allActivatedPids.isEmpty()) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(this.m_allActivatedPids);
    }

    // Don't perform internal calls to this method
    @Override
    public List<ComponentConfiguration> getComponentConfigurations() throws KuraException {
        return getComponentConfigurationsInternal();
    }

    // Don't perform internal calls to this method
    @Override
    public ComponentConfiguration getComponentConfiguration(String pid) throws KuraException {
        ComponentConfiguration tempConfig = getComponentConfigurationInternal(pid);
        if (tempConfig != null && tempConfig.getConfigurationProperties() != null) {
            decryptPasswords(tempConfig);
        }
        return tempConfig;
    }

    @Override
    public synchronized void updateConfiguration(String pidToUpdate, Map<String, Object> propertiesToUpdate)
            throws KuraException { // don't call this method internally
        updateConfiguration(pidToUpdate, propertiesToUpdate, true);
    }

    @Override
    public synchronized void updateConfiguration(String pidToUpdate, Map<String, Object> propertiesToUpdate,
            boolean takeSnapshot) throws KuraException { // don't call this method internally
        List<ComponentConfiguration> configs = new ArrayList<ComponentConfiguration>();
        ComponentConfigurationImpl cci = new ComponentConfigurationImpl(pidToUpdate, null, propertiesToUpdate);
        configs.add(cci);
        updateConfigurations(configs, takeSnapshot);
    }

    // Don't perform internal calls to this method
    @Override
    public synchronized void updateConfigurations(List<ComponentConfiguration> configsToUpdate) throws KuraException {
        updateConfigurations(configsToUpdate, true);
    }

    @Override
    public synchronized void updateConfigurations(List<ComponentConfiguration> configsToUpdate, boolean takeSnapshot)
            throws KuraException { // don't call this method internally
        for (ComponentConfiguration config : configsToUpdate) {
            if (config != null) {
                encryptPasswords(config);
            }
        }

        // only encrypted properties are passed to internal methods
        updateConfigurationsInternal(configsToUpdate, takeSnapshot);
    }

    // ----------------------------------------------------------------
    //
    // Service APIs: Factory Management
    //
    // ----------------------------------------------------------------
    @Override
    public Set<String> getFactoryComponentPids() {
        return Collections.unmodifiableSet(this.m_factoryPids);
    }

    @Override
    public ComponentConfiguration getDefaultComponentConfiguration(String pid) throws KuraException {
        Tocd ocd = getOCDForPid(pid);
        Map<String, Object> props = ComponentUtil.getDefaultProperties(ocd, this.m_ctx);
        return new ComponentConfigurationImpl(pid, ocd, props);
    }

    @Override
    public synchronized void createFactoryConfiguration(String factoryPid, String pid, Map<String, Object> properties,
            boolean takeSnapshot) throws KuraException {
        if (pid == null) {
            throw new KuraException(KuraErrorCode.INVALID_PARAMETER, "pid cannot be null");
        } else if (this.m_servicePidByPid.containsKey(pid)) {
            throw new KuraException(KuraErrorCode.INVALID_PARAMETER, "pid " + pid + " already exists");
        }

        try {
            // Second argument in createFactoryConfiguration is a bundle location. If left null the new bundle location
            // will be bound to the location of the first bundle that registers a Managed Service Factory with a
            // corresponding PID
            s_logger.info("Creating new configuration for factory pid {} and pid {}", factoryPid, pid);
            String servicePid = this.m_configurationAdmin.createFactoryConfiguration(factoryPid, null).getPid();

            s_logger.info("Updating newly created configuration for pid {}", pid);

            Map<String, Object> mergedProperties = new HashMap<String, Object>();
            if (properties != null) {
                mergedProperties.putAll(properties);
            }

            OCD ocd = this.m_ocds.get(factoryPid);
            mergeWithDefaults(ocd, mergedProperties);

            mergedProperties.put(ConfigurationService.KURA_SERVICE_PID, pid);

            Dictionary<String, Object> dict = CollectionsUtil.mapToDictionary(mergedProperties);
            Configuration config = this.m_configurationAdmin.getConfiguration(servicePid, "?");
            config.update(dict);

            registerComponentConfiguration(pid, servicePid, factoryPid);

            if (takeSnapshot) {
                snapshot();
            }
        } catch (IOException e) {
            throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR, e,
                    "Cannot create component instance for factory " + factoryPid);
        }
    }

    @Override
    public synchronized void deleteFactoryConfiguration(String pid, boolean takeSnapshot) throws KuraException {
        if (pid == null) {
            throw new KuraException(KuraErrorCode.INVALID_PARAMETER, "pid cannot be null");
        } else if (this.m_factoryPidByPid.get(pid) == null) {
            throw new KuraException(KuraErrorCode.INVALID_PARAMETER,
                    "pid " + pid + " is not a factory component instance");
        }

        try {
            s_logger.info("Deleting configuration for pid {}", pid);
            Configuration config = this.m_configurationAdmin.getConfiguration(this.m_servicePidByPid.get(pid), "?");

            if (config != null) {
                config.delete();
            }

            unregisterComponentConfiguration(pid);

            this.m_pendingDeletePids.add(pid);

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
                    rollbackConfigurationInternal(config.getPid(), config.getConfigurationProperties(),
                            snapshotOnConfirmation);
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
        Set<String> pids = new HashSet<String>(this.m_allActivatedPids);
        pids.removeAll(this.m_activatedSelfConfigComponents);
        pids.removeAll(snapshotPids);

        for (String pid : pids) {
            s_logger.info("Rolling back to default configuration for component pid: '{}'", pid);
            try {
                ServiceReference<?>[] refs = this.m_ctx.getBundleContext().getServiceReferences((String) null, null);
                if (refs != null) {
                    for (ServiceReference<?> ref : refs) {
                        String ppid = (String) ref.getProperty(Constants.SERVICE_PID);
                        if (pid.equals(ppid)) {
                            Bundle bundle = ref.getBundle();
                            try {
                                OCD ocd = ComponentUtil.readObjectClassDefinition(bundle, pid);
                                Map<String, Object> defaults = getDefaultProperties(ocd);
                                rollbackConfigurationInternal(pid, defaults, snapshotOnConfirmation);
                            } catch (Throwable t) {
                                s_logger.warn("Error during rollback for component " + pid, t);
                                causes.add(t);
                            }
                        }
                    }
                }
            } catch (InvalidSyntaxException e) {
                s_logger.warn("Error during rollback for component " + pid, e);
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
        returnConfigs.addAll(xmlConfigs.getConfigurations());

        return returnConfigs;
    }

    // ----------------------------------------------------------------
    //
    // Package APIs
    //
    // ----------------------------------------------------------------
    synchronized void registerComponentOCD(String metatypePid, Tocd ocd, boolean isFactory) throws KuraException {
        // metatypePid is either the 'pid' or 'factoryPid' attribute of the MetaType Designate element
        // 'pid' matches a service.pid, not a kura.service.pid
        s_logger.info("Registering metatype pid: {} with ocd: {} ...", metatypePid, ocd);
        this.m_ocds.put(metatypePid, ocd);

        if (isFactory) {
            registerFactoryComponentOCD(metatypePid, ocd);
        } else {
            try {
                updateWithDefaultConfiguration(metatypePid, ocd);
            } catch (IOException e) {
                throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
            }
        }
    }

    synchronized void registerComponentConfiguration(final String pid, final String servicePid,
            final String factoryPid) {
        if (pid == null || servicePid == null) {
            s_logger.warn("Either PID (kura.service.pid) {} or Service PID (service.pid) {} is null", pid, servicePid);
            return;
        }

        if (!this.m_allActivatedPids.contains(pid)) {
            // register the component instance
            s_logger.info("Registering ConfigurableComponent - {}....", pid);
            this.m_servicePidByPid.put(pid, servicePid);
            if (factoryPid != null) {
                this.m_factoryPidByPid.put(pid, factoryPid);
                Tocd factoryOCD = this.m_ocds.get(factoryPid);
                if (factoryOCD != null) {
                    try {
                        updateWithDefaultConfiguration(pid, factoryOCD);
                    } catch (KuraException e) {
                        s_logger.info("Error seeding updated configuration for pid: {}", pid);
                    } catch (IOException e) {
                        s_logger.info("Error seeding updated configuration for pid: {}", pid);
                    }
                }
            }
            this.m_allActivatedPids.add(pid);
            s_logger.info("Registering ConfigurableComponent - {}....Done", pid);
        }
    }

    synchronized void registerSelfConfiguringComponent(final String pid, final String servicePid) {
        if (pid == null) {
            s_logger.warn("PID (kura.service.pid) is null");
            return;
        }
        s_logger.info("Registering SelfConfiguringComponent - {}....", pid);
        if (!this.m_allActivatedPids.contains(pid)) {
            this.m_allActivatedPids.add(pid);
        }
        if (!this.m_activatedSelfConfigComponents.contains(pid)) {
            this.m_servicePidByPid.put(pid, servicePid);
            this.m_activatedSelfConfigComponents.add(pid);
        }
        s_logger.info("Registering SelfConfiguringComponent - {}....Done", pid);
    }

    synchronized void unregisterComponentConfiguration(String pid) {
        if (pid == null) {
            s_logger.warn("pid is null");
            return;
        }
        s_logger.info("Removing component configuration for pid {}", pid);
        this.m_servicePidByPid.remove(pid);
        this.m_factoryPidByPid.remove(pid);
        this.m_activatedSelfConfigComponents.remove(pid);
        this.m_allActivatedPids.remove(pid);
    }

    boolean mergeWithDefaults(OCD ocd, Map<String, Object> properties) throws KuraException {
        boolean changed = false;
        Set<String> keys = properties.keySet();

        Map<String, Object> defaults = getDefaultProperties(ocd);
        Set<String> defaultsKeys = defaults.keySet();

        defaultsKeys.removeAll(keys);
        if (!defaultsKeys.isEmpty()) {

            changed = true;
            s_logger.info("Merging configuration for pid: {}", ocd.getId());
            for (String key : defaultsKeys) {

                Object value = defaults.get(key);
                properties.put(key, value);
                s_logger.debug("Merged configuration properties with property with name: {} and default value {}", key,
                        value);
            }
        }
        return changed;
    }

    Map<String, Object> getDefaultProperties(OCD ocd) throws KuraException {
        return ComponentUtil.getDefaultProperties(ocd, this.m_ctx);
    }

    Map<String, Object> decryptPasswords(ComponentConfiguration config) {
        Map<String, Object> configProperties = config.getConfigurationProperties();
        for (Entry<String, Object> property : configProperties.entrySet()) {
            if (property.getValue() instanceof Password) {
                try {
                    Password decryptedPassword = new Password(
                            this.m_cryptoService.decryptAes(property.getValue().toString().toCharArray()));
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

    private synchronized void updateConfigurationsInternal(List<ComponentConfiguration> configsToUpdate,
            boolean takeSnapshot) throws KuraException {
        boolean snapshotOnConfirmation = false;
        List<Throwable> causes = new ArrayList<Throwable>();

        List<ComponentConfiguration> configs = buildCurrentConfiguration(configsToUpdate);

        for (ComponentConfiguration config : configs) {
            for (ComponentConfiguration configToUpdate : configsToUpdate) {
                if (config.getPid().equals(configToUpdate.getPid())) {
                    try {
                        updateConfigurationInternal(config.getPid(), config.getConfigurationProperties(),
                                snapshotOnConfirmation);
                        break;
                    } catch (KuraException e) {
                        s_logger.warn("Error during updateConfigurations for component " + config.getPid(), e);
                        causes.add(e);
                    }
                }
            }
        }

        if (takeSnapshot && configs != null && !configs.isEmpty()) {
            saveSnapshot(configs);
        }

        if (!causes.isEmpty()) {
            throw new KuraPartialSuccessException("updateConfigurations", causes);
        }
    }

    // returns configurations with encrypted passwords
    private List<ComponentConfiguration> getComponentConfigurationsInternal() throws KuraException {
        List<ComponentConfiguration> configs = new ArrayList<ComponentConfiguration>();

        // assemble all the configurations we have
        // clone the list to avoid concurrent modifications
        List<String> allPids = new ArrayList<String>(this.m_allActivatedPids);
        for (String pid : allPids) {
            try {
                ComponentConfiguration cc = getComponentConfigurationInternal(pid);
                if (cc != null) {
                    configs.add(cc);
                }
            } catch (Exception e) {
                s_logger.error("Error getting configuration for component " + pid, e);
                throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR, e,
                        "Error getting configuration for component " + pid);
            }
        }
        return configs;
    }

    // returns configurations with encrypted passwords
    private ComponentConfiguration getComponentConfigurationInternal(String pid) throws KuraException {
        ComponentConfiguration cc;
        if (!this.m_activatedSelfConfigComponents.contains(pid)) {
            cc = getConfigurableComponentConfiguration(pid);
        } else {
            cc = getSelfConfiguringComponentConfiguration(pid);
        }
        return cc;
    }

    private void updateWithDefaultConfiguration(String pid, Tocd ocd) throws KuraException, IOException {
        String servicePid = this.m_servicePidByPid.get(pid);
        if (servicePid == null) {
            servicePid = pid;
        }
        Configuration config = this.m_configurationAdmin.getConfiguration(servicePid, "?");
        if (config != null) {
            // get the properties from ConfigurationAdmin if any are present
            Map<String, Object> props = new HashMap<String, Object>();
            if (config.getProperties() != null) {
                props.putAll(CollectionsUtil.dictionaryToMap(config.getProperties(), ocd));
            }

            if (!props.containsKey(ConfigurationService.KURA_SERVICE_PID)) {
                props.put(ConfigurationService.KURA_SERVICE_PID, pid);
            }

            // merge the current properties, if any, with the defaults from metatype
            mergeWithDefaults(ocd, props);

            config.update(CollectionsUtil.mapToDictionary(props));
            s_logger.info("Seeding updated configuration for pid: {}", pid);
        }
    }

    private void registerFactoryComponentOCD(String metatypePid, Tocd ocd) throws KuraException {
        this.m_factoryPids.add(metatypePid);

        for (Map.Entry<String, String> entry : this.m_factoryPidByPid.entrySet()) {
            if (entry.getValue().equals(metatypePid) && this.m_servicePidByPid.get(entry.getKey()) != null) {
                try {
                    updateWithDefaultConfiguration(entry.getKey(), ocd);
                } catch (IOException e) {
                    throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
                }
            }
        }
    }

    private void encryptPasswords(ComponentConfiguration config) {
        Map<String, Object> propertiesToUpdate = config.getConfigurationProperties();
        if (propertiesToUpdate != null) {
            encryptPasswords(propertiesToUpdate);
        }
    }

    private void encryptPasswords(Map<String, Object> propertiesToUpdate) {
        for (Entry<String, Object> property : propertiesToUpdate.entrySet()) {
            if (property.getValue() != null && property.getValue() instanceof Password) {
                encryptPassword(propertiesToUpdate, property.getKey(), (Password) property.getValue());
            }
        }
    }

    private void encryptPassword(Map<String, Object> propertiesToUpdate, String key, Password password) {
        try {
            this.m_cryptoService.decryptAes(password.getPassword());
        } catch (Exception e1) {
            try {
                propertiesToUpdate.put(key, new Password(this.m_cryptoService.encryptAes(password.getPassword())));
            } catch (Exception e) {
                s_logger.warn("Failed to encrypt Password property: {}", key);
                propertiesToUpdate.remove(key);
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
            if (snapshotIDs == null || snapshotIDs.isEmpty()) {
                return false;
            }
            Long[] snapshots = snapshotIDs.toArray(new Long[] {});

            for (Long snapshot : snapshots) {

                try {
                    // Verify if the current snapshot is encrypted
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
                }        // end while
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

            // Writes an encrypted snapshot with encrypted passwords.
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
        long sid = new Date().getTime();

        // Do not save the snapshot in the past
        Set<Long> snapshotIDs = getSnapshots();
        if (snapshotIDs != null && !snapshotIDs.isEmpty()) {
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

        // Marshall the configuration into an XML
        String xmlResult;
        try {
            xmlResult = XmlUtil.marshal(conf);
            if (xmlResult.trim().isEmpty()) {
                throw new KuraException(KuraErrorCode.INVALID_PARAMETER, conf);
            }
        } catch (Exception e1) {
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e1);
        }

        // Encrypt the XML
        char[] encryptedXML = this.m_cryptoService.encryptAes(xmlResult.toCharArray());

        // Write the snapshot
        FileOutputStream fos = null;
        OutputStreamWriter osw = null;
        try {
            s_logger.info("Writing snapshot - Saving {}...", fSnapshot.getAbsolutePath());
            fos = new FileOutputStream(fSnapshot);
            osw = new OutputStreamWriter(fos, "UTF-8");
            osw.append(new String(encryptedXML));
            osw.flush();
            fos.flush();
            fos.getFD().sync();
            s_logger.info("Writing snapshot - Saving {}... Done.", fSnapshot.getAbsolutePath());
        } catch (FileNotFoundException e) {
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
        } catch (UnsupportedEncodingException e) {
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
        } catch (IOException e) {
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
        } finally {
            if (osw != null) {
                try {
                    osw.close();
                } catch (IOException e) {

                }
            }
            if (fos != null) {
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

            String servicePid = this.m_servicePidByPid.get(pid);

            if (servicePid != null) {
                Configuration cfg = this.m_configurationAdmin.getConfiguration(servicePid, "?");
                Map<String, Object> props = CollectionsUtil.dictionaryToMap(cfg.getProperties(), ocd);

                cc = new ComponentConfigurationImpl(pid, ocd, props);
            }
        } catch (Exception e) {
            s_logger.error("Error getting Configuration for component: " + pid + ". Ignoring it.", e);
        }
        return cc;
    }

    private ComponentConfiguration getSelfConfiguringComponentConfiguration(String pid) {
        ComponentConfiguration cc = null;
        try {
            ServiceReference<?>[] refs = this.m_ctx.getBundleContext().getServiceReferences((String) null, null);
            if (refs != null) {
                for (ServiceReference<?> ref : refs) {
                    String ppid = (String) ref.getProperty(KURA_SERVICE_PID);
                    if (pid.equals(ppid)) {
                        Object obj = this.m_ctx.getBundleContext().getService(ref);
                        try {
                            if (obj instanceof SelfConfiguringComponent) {
                                SelfConfiguringComponent selfConfigComp = null;
                                selfConfigComp = (SelfConfiguringComponent) obj;
                                try {
                                    cc = selfConfigComp.getConfiguration();
                                    if (cc.getPid() == null || !cc.getPid().equals(pid)) {
                                        s_logger.error(
                                                "Invalid pid for returned Configuration of SelfConfiguringComponent with pid: "
                                                        + pid + ". Ignoring it.");
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
                                                            "null required id for AD for returned Configuration of SelfConfiguringComponent with pid: {}",
                                                            pid);
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
                                                        String propType;
                                                        if (!value.getClass().isArray()) {
                                                            propType = value.getClass().getSimpleName();
                                                        } else {
                                                            propType = value.getClass().getComponentType()
                                                                    .getSimpleName();
                                                        }

                                                        try {
                                                            s_logger.debug(
                                                                    "pid: {}, property name: {}, type: {}, value: {}",
                                                                    new Object[] { pid, adId, propType, value });
                                                            Scalar.fromValue(propType);
                                                            if (!propType.equals(adType)) {
                                                                s_logger.error(
                                                                        "Type: {} for property named: {} does not match the AD type: {} for returned Configuration of SelfConfiguringComponent with pid: {}",
                                                                        new Object[] { propType, adId, adType, pid });
                                                                return null;
                                                            }
                                                        } catch (IllegalArgumentException e) {
                                                            s_logger.error(
                                                                    "Invalid class: {} for property named: {} for returned Configuration of SelfConfiguringComponent with pid: "
                                                                            + pid,
                                                                    propType, adId);
                                                            return null;
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } catch (KuraException e) {
                                    s_logger.error(
                                            "Error getting Configuration for component: " + pid + ". Ignoring it.", e);
                                }
                            } else {
                                s_logger.error("Component " + obj + " is not a SelfConfiguringComponent. Ignoring it.");
                            }
                        } finally {
                            this.m_ctx.getBundleContext().ungetService(ref);
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
        return this.m_systemService.getKuraSnapshotsDirectory();
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
        int maxCount = this.m_systemService.getKuraSnapshotsCount();
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
                            s_logger.warn("Error creating configuration with pid: " + pid + " and factory pid: {}",
                                    factoryPid, e);
                        }
                    } else {
                        try {
                            s_logger.debug("Pushing config to config admin: {}", config.getPid());

                            // push it to the ConfigAdmin
                            Configuration cfg = this.m_configurationAdmin.getConfiguration(config.getPid(), "?");

                            // set kura.service.pid if missing
                            Map<String, Object> newProperties = new HashMap<String, Object>(props);
                            if (!newProperties.containsKey(ConfigurationService.KURA_SERVICE_PID)) {
                                newProperties.put(ConfigurationService.KURA_SERVICE_PID, config.getPid());
                            }

                            cfg.update(CollectionsUtil.mapToDictionary(newProperties));

                        } catch (IOException e) {
                            s_logger.warn("Error seeding initial properties to ConfigAdmin for pid: {}",
                                    config.getPid(), e);
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

        // File loaded, try to decrypt and unmarshall
        String decryptedContent = new String(this.m_cryptoService.decryptAes(entireFile.toString().toCharArray()));

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

    private void updateConfigurationInternal(String pid, Map<String, Object> properties, boolean snapshotOnConfirmation)
            throws KuraException {
        s_logger.debug("Attempting update configuration for {}", pid);

        if (!this.m_allActivatedPids.contains(pid)) {
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

        if (!this.m_activatedSelfConfigComponents.contains(pid)) {
            try {
                // get the current running configuration for the selected component
                Configuration config = this.m_configurationAdmin.getConfiguration(this.m_servicePidByPid.get(pid), "?");
                Map<String, Object> runningProps = CollectionsUtil.dictionaryToMap(config.getProperties(),
                        registerdOCD);

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

    private void rollbackConfigurationInternal(String pid, Map<String, Object> properties,
            boolean snapshotOnConfirmation) throws KuraException {
        s_logger.debug("Attempting to rollback configuration for {}", pid);

        if (!this.m_allActivatedPids.contains(pid)) {
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

    private void updateComponentConfiguration(String pid, Map<String, Object> mergedProperties,
            boolean snapshotOnConfirmation) throws KuraException, IOException {
        if (!this.m_activatedSelfConfigComponents.contains(pid)) {

            // load the ocd to do the validation
            BundleContext ctx = this.m_ctx.getBundleContext();
            // FIXME: why the returned ocd is always null?
            ObjectClassDefinition ocd = ComponentUtil.getObjectClassDefinition(ctx, this.m_servicePidByPid.get(pid));

            // Validate the properties to be applied and set them
            validateProperties(pid, ocd, mergedProperties);
        } else {
            // FIXME: validation of properties for self-configuring
            // components
        }

        // Update the new properties
        // use ConfigurationAdmin to do the update
        Configuration config = this.m_configurationAdmin.getConfiguration(this.m_servicePidByPid.get(pid), "?");
        config.update(CollectionsUtil.mapToDictionary(mergedProperties));

        if (snapshotOnConfirmation) {
            snapshot();
        }
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

    private void validateProperties(String pid, ObjectClassDefinition ocd, Map<String, Object> updatedProps)
            throws KuraException {
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
                        throw new KuraException(KuraErrorCode.CONFIGURATION_ATTRIBUTE_INVALID,
                                attrDef.getID() + ": " + result);
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
                            throw new KuraException(KuraErrorCode.CONFIGURATION_REQUIRED_ATTRIBUTE_MISSING,
                                    attrDef.getId());
                        }
                    }
                }
            }
        }
    }

    private synchronized List<ComponentConfiguration> buildCurrentConfiguration(
            List<ComponentConfiguration> configsToUpdate) throws KuraException {
        List<ComponentConfiguration> result = new ArrayList<ComponentConfiguration>();

        // Merge the current configuration of registered components with the provided configurations.
        // It is assumed that the PIDs in the provided configurations is a subset of the registered PIDs.
        List<ComponentConfiguration> currentConfigs = getComponentConfigurationsInternal();
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
        Iterator<String> it = this.m_pendingDeletePids.iterator();
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

    private Tocd getOCDForPid(String pid) {
        Tocd ocd = this.m_ocds.get(this.m_factoryPidByPid.get(pid));
        if (ocd == null) {
            ocd = this.m_ocds.get(pid);
        }
        return ocd;
    }

    /**
     * Convert property value to string
     *
     * @param value
     *            the input value
     * @return the string property value, or {@code null}
     */
    private static String makeString(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String) {
            return (String) value;
        }
        return value.toString();
    }
}
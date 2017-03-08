/*******************************************************************************
 * Copyright (c) 2016, 2017 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *  Eurotech
 *  Amit Kumar Mondal
 *
 *******************************************************************************/
package org.eclipse.kura.web.server;

import static org.eclipse.kura.configuration.ConfigurationService.KURA_SERVICE_PID;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.asset.AssetConstants;
import org.eclipse.kura.asset.provider.BaseChannelDescriptor;
import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.configuration.Password;
import org.eclipse.kura.configuration.metatype.AD;
import org.eclipse.kura.configuration.metatype.Option;
import org.eclipse.kura.driver.ChannelDescriptor;
import org.eclipse.kura.driver.Driver;
import org.eclipse.kura.driver.DriverService;
import org.eclipse.kura.web.server.util.GwtServerUtil;
import org.eclipse.kura.web.server.util.GwtWireServiceUtil;
import org.eclipse.kura.web.server.util.ServiceLocator;
import org.eclipse.kura.web.shared.GwtKuraErrorCode;
import org.eclipse.kura.web.shared.GwtKuraException;
import org.eclipse.kura.web.shared.model.GwtChannelInfo;
import org.eclipse.kura.web.shared.model.GwtConfigComponent;
import org.eclipse.kura.web.shared.model.GwtConfigParameter;
import org.eclipse.kura.web.shared.model.GwtConfigParameter.GwtConfigParameterType;
import org.eclipse.kura.web.shared.model.GwtWireComponentConfiguration;
import org.eclipse.kura.web.shared.model.GwtWireConfiguration;
import org.eclipse.kura.web.shared.model.GwtWiresConfiguration;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.service.GwtWireService;
import org.eclipse.kura.wire.WireComponent;
import org.eclipse.kura.wire.WireConfiguration;
import org.eclipse.kura.wire.WireEmitter;
import org.eclipse.kura.wire.WireHelperService;
import org.eclipse.kura.wire.WireReceiver;
import org.eclipse.kura.wire.WireService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;

/**
 * The class GwtWireServiceImpl implements {@link GwtWireService}
 */
public final class GwtWireServiceImpl extends OsgiRemoteServiceServlet implements GwtWireService {

    private static final int SERVICE_WAIT_TIMEOUT = 60000;

    private static final String CONSUMER = "consumer";
    private static final String GRAPH = "wiregraph";
    private static final String PRODUCER = "producer";

    private static final Logger logger = LoggerFactory.getLogger(GwtWireServiceImpl.class);

    private static final long serialVersionUID = -6577843865830245755L;

    private static final String WIRE_SERVICE_PID = "org.eclipse.kura.wire.WireService";

    private static Map<String, Object> fillPropertiesFromConfiguration(final GwtConfigComponent config,
            final ComponentConfiguration currentCC) {
        // Build the new properties
        final Map<String, Object> properties = new HashMap<>();
        final ComponentConfiguration backupCC = currentCC;
        if (backupCC == null) {
            return null;
        }
        final Map<String, Object> backupConfigProp = backupCC.getConfigurationProperties();
        for (final GwtConfigParameter gwtConfigParam : config.getParameters()) {

            Object objValue;

            final Map<String, Object> currentConfigProp = currentCC.getConfigurationProperties();
            final Object currentObjValue = currentConfigProp.get(gwtConfigParam.getName());

            final int cardinality = gwtConfigParam.getCardinality();
            if (cardinality == 0 || cardinality == 1 || cardinality == -1) {

                final String strValue = gwtConfigParam.getValue();

                if (currentObjValue instanceof Password && PLACEHOLDER.equals(strValue)) {
                    objValue = currentConfigProp.get(gwtConfigParam.getName());
                } else {
                    objValue = GwtServerUtil.getObjectValue(gwtConfigParam, strValue);
                }
            } else {

                final String[] strValues = gwtConfigParam.getValues();

                if (currentObjValue instanceof Password[]) {
                    final Password[] currentPasswordValue = (Password[]) currentObjValue;
                    for (int i = 0; i < strValues.length; i++) {
                        if (PLACEHOLDER.equals(strValues[i])) {
                            strValues[i] = new String(currentPasswordValue[i].getPassword());
                        }
                    }
                }

                objValue = GwtServerUtil.getObjectValue(gwtConfigParam, strValues);
            }
            properties.put(gwtConfigParam.getName(), objValue);
        }

        // Force kura.service.pid into properties, if originally present
        if (backupConfigProp.get(KURA_SERVICE_PID) != null) {
            properties.put(KURA_SERVICE_PID, backupConfigProp.get(KURA_SERVICE_PID));
        }
        return properties;
    }

    /**
     * Returns the formatted component string required for JS
     *
     * @param pid
     *            the PID to parse
     * @return the formatted string
     * @throws GwtKuraException
     */
    private static String getComponentString(final String pid) throws GwtKuraException {
        final StringBuilder result = new StringBuilder();

        final BundleContext ctx = FrameworkUtil.getBundle(GwtWireServiceImpl.class).getBundleContext();
        final Collection<ServiceReference<WireComponent>> refs = ServiceLocator.getInstance()
                .getServiceReferences(WireComponent.class, null);
        for (final ServiceReference<WireComponent> ref : refs) {
            if (ref.getProperty(KURA_SERVICE_PID).equals(pid)) {
                final String fPid = (String) ref.getProperty(ConfigurationAdmin.SERVICE_FACTORYPID);
                final WireComponent comp = ctx.getService(ref);
                String compType;
                if (comp instanceof WireEmitter && comp instanceof WireReceiver) {
                    compType = "both";
                } else if (comp instanceof WireEmitter) {
                    compType = PRODUCER;
                } else {
                    compType = CONSUMER;
                }
                result.append(fPid).append("|").append(pid).append("|").append(pid).append("|").append(compType);
                return result.toString();
            }
        }
        logger.error("Could not find WireComponent for pid {}", pid);
        throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR);
    }

    private GwtChannelInfo getChannelFromProperties(final int channelIndex, final GwtConfigComponent descriptor,
            final GwtConfigComponent asset) {
        final GwtChannelInfo ci = new GwtChannelInfo();
        String indexPrefix = String.valueOf(channelIndex) + ".CH.";
        ci.setName(asset.getParameter(indexPrefix + "name").getValue());
        ci.setId(String.valueOf(channelIndex));
        ci.setType(asset.getParameter(indexPrefix + "type").getValue());
        ci.setValueType(asset.getParameter(indexPrefix + "value.type").getValue());
        indexPrefix += "DRIVER.";
        for (final GwtConfigParameter param : descriptor.getParameters()) {
            ci.set(param.getName(), asset.getParameter(indexPrefix + param.getName()).getValue());
        }

        return ci;
    }

    /** {@inheritDoc} */
    @Override
    public List<String> getDriverInstances(final GwtXSRFToken xsrfToken) throws GwtKuraException {
        this.checkXSRFToken(xsrfToken);
        final Collection<ServiceReference<Driver>> refs = ServiceLocator.getInstance()
                .getServiceReferences(Driver.class, null);
        final List<String> drivers = new ArrayList<>();
        for (final ServiceReference<Driver> ref : refs) {
            drivers.add(String.valueOf(ref.getProperty(ConfigurationService.KURA_SERVICE_PID)));
        }
        return drivers;
    }

    @Override
    public GwtConfigComponent getGwtBaseChannelDescriptor(final GwtXSRFToken xsrfToken) throws GwtKuraException {
        final BaseChannelDescriptor bcd = new BaseChannelDescriptor();
        try {
            @SuppressWarnings("unchecked")
            final List<AD> params = (List<AD>) bcd.getDescriptor();

            final GwtConfigComponent gwtConfig = new GwtConfigComponent();
            gwtConfig.setComponentId("BaseChannelDescriptor");

            final List<GwtConfigParameter> gwtParams = new ArrayList<>();
            gwtConfig.setParameters(gwtParams);
            for (final AD ad : params) {
                final GwtConfigParameter gwtParam = new GwtConfigParameter();
                gwtParam.setId(ad.getId());
                gwtParam.setName(ad.getName());
                gwtParam.setDescription(ad.getDescription());
                gwtParam.setType(GwtConfigParameterType.valueOf(ad.getType().name()));
                gwtParam.setRequired(ad.isRequired());
                gwtParam.setCardinality(ad.getCardinality());
                if (ad.getOption() != null && !ad.getOption().isEmpty()) {
                    final Map<String, String> options = new HashMap<>();
                    for (final Option option : ad.getOption()) {
                        options.put(option.getLabel(), option.getValue());
                    }
                    gwtParam.setOptions(options);
                }
                gwtParam.setMin(ad.getMin());
                gwtParam.setMax(ad.getMax());
                gwtParam.setDefault(ad.getDefault());

                gwtParams.add(gwtParam);
            }
            return gwtConfig;
        } catch (final Exception ex) {
            throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR);
        }
    }

    @Override
    public GwtConfigComponent getGwtChannelDescriptor(final GwtXSRFToken xsrfToken, final String driverPid)
            throws GwtKuraException {
        final DriverService driverService = ServiceLocator.getInstance().getService(DriverService.class);

        final Driver d = driverService.getDriver(driverPid);
        final ChannelDescriptor cd = d.getChannelDescriptor();
        try {
            @SuppressWarnings("unchecked")
            final List<AD> params = (List<AD>) cd.getDescriptor();

            final GwtConfigComponent gwtConfig = new GwtConfigComponent();
            gwtConfig.setComponentId(driverPid);

            final List<GwtConfigParameter> gwtParams = new ArrayList<>();
            gwtConfig.setParameters(gwtParams);
            for (final AD ad : params) {
                final GwtConfigParameter gwtParam = new GwtConfigParameter();
                gwtParam.setId(ad.getId());
                gwtParam.setName(ad.getName());
                gwtParam.setDescription(ad.getDescription());
                gwtParam.setType(GwtConfigParameterType.valueOf(ad.getType().name()));
                gwtParam.setRequired(ad.isRequired());
                gwtParam.setCardinality(ad.getCardinality());
                if (ad.getOption() != null && !ad.getOption().isEmpty()) {
                    final Map<String, String> options = new HashMap<>();
                    for (final Option option : ad.getOption()) {
                        options.put(option.getLabel(), option.getValue());
                    }
                    gwtParam.setOptions(options);
                }
                gwtParam.setMin(ad.getMin());
                gwtParam.setMax(ad.getMax());
                gwtParam.setDefault(ad.getDefault());

                gwtParams.add(gwtParam);
            }
            return gwtConfig;
        } catch (final Exception ex) {
            throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR);
        }

    }

    @Override
    public List<GwtChannelInfo> getGwtChannels(final GwtXSRFToken xsrfToken, final GwtConfigComponent descriptor,
            final GwtConfigComponent asset) throws GwtKuraException {

        final List<GwtChannelInfo> result = new ArrayList<>();

        final Set<Integer> channelIndexes = new HashSet<>();
        for (final GwtConfigParameter param : asset.getParameters()) {
            if (param != null && param.getName() != null && param.getName().endsWith("CH.name")) {
                final String[] tokens = param.getName().split("\\.");
                channelIndexes.add(Integer.parseInt(tokens[0]));
            }
        }

        for (final Integer index : channelIndexes) {
            final GwtChannelInfo ci = getChannelFromProperties(index, descriptor, asset);
            result.add(ci);
        }

        return result;
    }

    /** {@inheritDoc} */
    @Override
    public GwtWiresConfiguration getWiresConfiguration(final GwtXSRFToken xsrfToken) throws GwtKuraException {
        this.checkXSRFToken(xsrfToken);
        return getWiresConfigurationInternal();
    }

    private GwtWiresConfiguration getWiresConfigurationInternal() throws GwtKuraException {
        final ConfigurationService configService = ServiceLocator.getInstance().getService(ConfigurationService.class);
        final WireService wireService = ServiceLocator.getInstance().getService(WireService.class);

        final Set<WireConfiguration> wireConfigurations = wireService.getWireConfigurations();
        final List<String> wireEmitterFactoryPids = new ArrayList<>();
        final List<String> wireReceiverFactoryPids = new ArrayList<>();
        final List<String> wireComponents = new ArrayList<>();

        GwtServerUtil.fillFactoriesLists(wireEmitterFactoryPids, wireReceiverFactoryPids);

        String sGraph = null;
        // Get Graph JSON from WireService
        try {
            final Map<String, Object> wsProps = configService.getComponentConfiguration(WIRE_SERVICE_PID)
                    .getConfigurationProperties();
            sGraph = (String) wsProps.get(GRAPH);
        } catch (final KuraException exception) {
            throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR, exception);
        }

        // create the JSON for the Wires Configuration
        final JsonObject wireConfig = Json.object();
        int i = 0;
        for (final WireConfiguration wireConfiguration : wireConfigurations) {
            final String emitterPid = wireConfiguration.getEmitterPid();
            final String receiverPid = wireConfiguration.getReceiverPid();
            wireComponents.add(getComponentString(emitterPid));
            wireComponents.add(getComponentString(receiverPid));

            final JsonObject wireConf = Json.object();
            wireConf.add("p", emitterPid).add("c", receiverPid);
            wireConfig.add(String.valueOf(++i), wireConf);
        }
        final List<GwtWireComponentConfiguration> configs = new ArrayList<>();
        for (final String wc : GwtWireServiceUtil.getWireComponents()) {
            // create instance of GWT Wire Component Configuration to hold all
            // the information for a Wire Component
            final GwtWireComponentConfiguration config = new GwtWireComponentConfiguration();
            config.setFactoryPid(GwtWireServiceUtil.getFactoryPid(wc));
            config.setType(GwtWireServiceUtil.getType(wc));
            config.setPid(wc);
            config.setDriverPid(GwtWireServiceUtil.getDriverByPid(wc));
            configs.add(config);
        }

        final List<GwtWireConfiguration> wires = new ArrayList<>();
        for (final WireConfiguration wc : GwtWireServiceUtil.getWireConfigurations()) {
            final GwtWireConfiguration config = new GwtWireConfiguration();
            config.setEmitterPid(wc.getEmitterPid());
            config.setReceiverPid(wc.getReceiverPid());
            wires.add(config);
        }

        final GwtWiresConfiguration configuration = new GwtWiresConfiguration();
        configuration.getWireEmitterFactoryPids().addAll(wireEmitterFactoryPids);
        configuration.getWireReceiverFactoryPids().addAll(wireReceiverFactoryPids);
        configuration.getWireComponents().addAll(wireComponents);
        configuration.setWiresConfigurationJson(wireConfig.toString());
        configuration.setGraph(sGraph == null ? "{}" : sGraph);
        configuration.setWireComponentsJson(GwtWireServiceUtil.getWireComponentsJson(configs));
        configuration.setWireConfigurationsJson(GwtWireServiceUtil.getWireConfigurationsJson(wires));

        return configuration;
    }

    /** {@inheritDoc} */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public void updateWireConfiguration(final GwtXSRFToken xsrfToken, final String newJsonConfiguration,
            final Map<String, GwtConfigComponent> configurations) throws GwtKuraException {
        this.checkXSRFToken(xsrfToken);

        JsonObject jWireGraph = null;
        final WireService wireService = ServiceLocator.getInstance().getService(WireService.class);
        final WireHelperService wireHelperService = ServiceLocator.getInstance().getService(WireHelperService.class);
        final ConfigurationService configService = ServiceLocator.getInstance().getService(ConfigurationService.class);

        try {
            jWireGraph = Json.parse(newJsonConfiguration).asObject().get(GRAPH).asObject();
            // don't consider the "wires" JSON
            final int length = jWireGraph.size() - 1;
            // Delete wires
            final Set<WireConfiguration> set = new CopyOnWriteArraySet<>(wireService.getWireConfigurations());
            Iterator<WireConfiguration> iterator = set.iterator();
            while (iterator.hasNext()) {
                final WireConfiguration wireConfiguration = iterator.next();
                // check if jObj is an empty JSON. It means all the existing
                // wire configurations need to be deleted
                if (length == 0) {
                    logger.info("Deleting Wire: Emitter PID -> " + wireConfiguration.getEmitterPid()
                            + " | Receiver PID -> " + wireConfiguration.getReceiverPid());
                    wireService.deleteWireConfiguration(wireConfiguration);
                }
            }

            final Set<WireConfiguration> configs = new CopyOnWriteArraySet<>(wireService.getWireConfigurations());
            iterator = configs.iterator();
            while (iterator.hasNext()) {
                final WireConfiguration wireConfiguration = iterator.next();
                boolean isFound = false;
                for (final GwtWireConfiguration configuration : GwtWireServiceUtil
                        .getWireConfigurationsFromJson(jWireGraph.get("wires").asObject())) {
                    final WireConfiguration temp = new WireConfiguration(configuration.getEmitterPid(),
                            configuration.getReceiverPid());
                    if (temp.equals(wireConfiguration)) {
                        isFound = true;
                    }
                }
                if (!isFound) {
                    logger.info("Deleting Wire: Emitter PID -> " + wireConfiguration.getEmitterPid()
                            + " | Receiver PID -> " + wireConfiguration.getReceiverPid());
                    wireService.deleteWireConfiguration(wireConfiguration);
                }
            }

            final List<String> wireComponents = deleteWireComponents(jWireGraph, configService, length);

            for (int i = 0; i < length; i++) {
                final JsonObject jsonObject = jWireGraph.get(String.valueOf(i)).asObject();
                String pid = null;
                String fpid = null;
                String driver = null;
                pid = jsonObject.getString("pid", null);
                fpid = jsonObject.getString("fpid", null);
                driver = jsonObject.getString("driver", null);
                Map<String, Object> properties = null;
                if (pid != null && !wireComponents.contains(pid)) {
                    logger.info("Creating new Wire Component: Factory PID -> " + fpid + " | PID -> " + pid);
                    if (driver != null) {
                        properties = new HashMap<>();
                        properties.put("asset.desc", "Sample Asset");
                        properties.put("driver.pid", driver);
                    }
                    configService.createFactoryConfiguration(fpid, pid, properties, false);
                }
            }

            // Create new wires
            final Set<WireConfiguration> wireConfs = wireService.getWireConfigurations();
            for (final GwtWireConfiguration conf : GwtWireServiceUtil
                    .getWireConfigurationsFromJson(jWireGraph.get("wires").asObject())) {
                final String emitterPid = conf.getEmitterPid();
                final String receiverPid = conf.getReceiverPid();
                final WireConfiguration temp = new WireConfiguration(emitterPid, receiverPid);
                if (!wireConfs.contains(temp)) {

                    // track and wait for the emitter
                    final BundleContext bundleContext = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
                    String filterString = "(" + ConfigurationService.KURA_SERVICE_PID + "=" + emitterPid + ")";
                    Filter filter = bundleContext.createFilter(filterString);
                    final ServiceTracker producerTracker = new ServiceTracker(bundleContext, filter, null);
                    producerTracker.open();

                    // track and wait for the receiver
                    filterString = "(" + ConfigurationService.KURA_SERVICE_PID + "=" + receiverPid + ")";
                    filter = bundleContext.createFilter(filterString);
                    final ServiceTracker consumerTracker = new ServiceTracker(bundleContext, filter, null);
                    consumerTracker.open();

                    producerTracker.waitForService(SERVICE_WAIT_TIMEOUT);
                    consumerTracker.waitForService(SERVICE_WAIT_TIMEOUT);

                    producerTracker.close();
                    consumerTracker.close();

                    logger.info(
                            "Creating new wire: Emitter PID -> " + emitterPid + " | Consumer PID -> " + receiverPid);
                    logger.info("Service PID for Emitter before tracker: {}",
                            wireHelperService.getServicePid(emitterPid));
                    logger.info("Service PID for Receiver before tracker: {}",
                            wireHelperService.getServicePid(receiverPid));

                    wireService.createWireConfiguration(emitterPid, receiverPid);
                }
            }

            // Update configuration for all changes tracked in Wires Composer
            for (final String pid : configurations.keySet()) {
                final GwtConfigComponent config = configurations.get(pid);
                if (config != null) {
                    final ComponentConfiguration currentConf = configService.getComponentConfiguration(pid);
                    Map<String, Object> prop = null;
                    if (currentConf != null) {
                        prop = currentConf.getConfigurationProperties();
                    }
                    Object val = null;
                    if (prop != null) {
                        val = prop.get(ConfigurationAdmin.SERVICE_FACTORYPID);
                    }
                    String runtimeWireComponentFactoryPid = val != null ? val.toString() : null;
                    final Map<String, Object> props = fillPropertiesFromConfiguration(config, currentConf);
                    if (props != null) {
                        final String factoryPid = config.getFactoryId();
                        // if the Wire Component to be created with the same name is of the same type
                        // as the recently removed Wire Component
                        boolean isSame = false;
                        if (runtimeWireComponentFactoryPid != null) {
                            isSame = runtimeWireComponentFactoryPid.equalsIgnoreCase(factoryPid);
                        }
                        if ("org.eclipse.kura.wire.WireAsset".equalsIgnoreCase(factoryPid) || !isSame) {
                            configService.deleteFactoryConfiguration(pid, false);
                            configService.createFactoryConfiguration(factoryPid, pid, props, false);
                            continue;
                        }
                        configService.updateConfiguration(pid, props, false);
                        removeDeletedFromWireGraphProperty(pid);
                    }
                }
            }
            final Map<String, Object> props = configService.getComponentConfiguration(WIRE_SERVICE_PID)
                    .getConfigurationProperties();
            // remove wires JSON from actual wire graph
            jWireGraph.remove("wires");
            props.put(GRAPH, jWireGraph.toString());
            configService.updateConfiguration(WIRE_SERVICE_PID, props, true);
            configurations.clear();
        } catch (final KuraException | InterruptedException | InvalidSyntaxException exception) {
            throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR, exception);
        }
    }

    private void removeDeletedFromWireGraphProperty(String pid) throws GwtKuraException {
        final ConfigurationAdmin configAdmin = ServiceLocator.getInstance().getService(ConfigurationAdmin.class);
        final WireHelperService wireHelperService = ServiceLocator.getInstance().getService(WireHelperService.class);
        try {
            final String servicePid = wireHelperService.getServicePid(pid);
            Configuration conf = null;
            if (servicePid != null) {
                conf = configAdmin.getConfiguration(servicePid);
            }
            Dictionary<String, Object> props = null;
            if (conf != null) {
                props = conf.getProperties();
            }
            if (props != null) {
                props.remove(DELETED_WIRE_COMPONENT);
            }
            if (conf != null) {
                conf.update(props);
            }
        } catch (IOException e) {
            // no need
        }
    }

    private List<String> deleteWireComponents(JsonObject jWireGraph, final ConfigurationService configService,
            final int length) throws GwtKuraException, KuraException {
        // Delete Wire Component instances
        final List<String> wireComponents = GwtWireServiceUtil.getWireComponents();
        for (final String componentPid : GwtWireServiceUtil.getWireComponents()) {
            // check if jObj is an empty JSON. It means all the existing
            // wire components need to be deleted
            if (length == 0) {
                logger.info("Deleting Wire Component: PID -> " + componentPid);
                configService.deleteFactoryConfiguration(componentPid, false);
                continue;
            }
            boolean isFound = false;
            for (int i = 0; i < length; i++) {
                final JsonObject jsonObject = jWireGraph.get(String.valueOf(i)).asObject();
                final String component = jsonObject.getString("pid", null);
                if (component.equalsIgnoreCase(componentPid)) {
                    isFound = true;
                    break;
                }
            }
            if (!isFound) {
                logger.info("Deleting Wire Component: PID -> " + componentPid);
                configService.deleteFactoryConfiguration(componentPid, false);
            }
        }
        return wireComponents;
    }

    /** {@inheritDoc} */
    @Override
    public String getDriverPidProp() {
        return AssetConstants.ASSET_DRIVER_PROP.value();
    }
}

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

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.eclipse.kura.configuration.ConfigurationService.KURA_SERVICE_PID;
import static org.osgi.service.cm.ConfigurationAdmin.SERVICE_FACTORYPID;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.asset.provider.BaseChannelDescriptor;
import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.configuration.metatype.AD;
import org.eclipse.kura.configuration.metatype.Option;
import org.eclipse.kura.driver.DriverDescriptor;
import org.eclipse.kura.driver.DriverService;
import org.eclipse.kura.util.service.ServiceUtil;
import org.eclipse.kura.web.server.util.GwtServerUtil;
import org.eclipse.kura.web.server.util.GwtWireServiceUtil;
import org.eclipse.kura.web.server.util.GwtWireServiceUtil.WireComponentDescriptor;
import org.eclipse.kura.web.server.util.ServiceLocator;
import org.eclipse.kura.web.shared.AssetConstants;
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
import org.eclipse.kura.wire.WireReceiver;
import org.eclipse.kura.wire.WireService;
import org.eclipse.kura.wire.graph.WireComponentConfiguration;
import org.eclipse.kura.wire.graph.WireGraphConfiguration;
import org.eclipse.kura.wire.graph.WireGraphService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;

/**
 * The class GwtWireServiceImpl implements {@link GwtWireService}
 */
public final class GwtWireServiceImpl extends OsgiRemoteServiceServlet implements GwtWireService {

    private static final int TIMEOUT = 60;
    private static final String CONSUMER = "consumer";
    private static final String GRAPH = "wiregraph";
    private static final String PRODUCER = "producer";
    private static final Logger logger = LoggerFactory.getLogger(GwtWireServiceImpl.class);
    private static final long serialVersionUID = -6577843865830245755L;
    private static final String WIRE_SERVICE_PID = "org.eclipse.kura.wire.WireService";

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
                final String fPid = (String) ref.getProperty(SERVICE_FACTORYPID);
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

    private GwtChannelInfo getChannelFromProperties(final String channelName, final GwtConfigComponent descriptor,
            final GwtConfigComponent asset) {
        final GwtChannelInfo ci = new GwtChannelInfo();
        String prefix = channelName + AssetConstants.CHANNEL_PROPERTY_SEPARATOR.value();
        ci.setName(channelName);
        ci.setType(asset.getParameter(prefix + AssetConstants.TYPE.value()).getValue());
        ci.setValueType(asset.getParameter(prefix + AssetConstants.VALUE_TYPE.value()).getValue());
        for (final GwtConfigParameter param : descriptor.getParameters()) {
            ci.set(param.getName(), asset.getParameter(prefix + param.getName()).getValue());
        }

        return ci;
    }

    /** {@inheritDoc} */
    @Override
    public List<String> getDriverInstances(final GwtXSRFToken xsrfToken) throws GwtKuraException {
        this.checkXSRFToken(xsrfToken);
        final DriverService driverService = ServiceLocator.getInstance().getService(DriverService.class);
        List<DriverDescriptor> drivers = driverService.listDriverDescriptors();

        List<String> driverPids = new ArrayList<>();
        for (DriverDescriptor driverDescriptor : drivers) {
            driverPids.add(driverDescriptor.getPid());
        }

        return driverPids;
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
            throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR, ex);
        }
    }

    @Override
    public GwtConfigComponent getGwtChannelDescriptor(final GwtXSRFToken xsrfToken, final String driverPid)
            throws GwtKuraException {
        final DriverService driverService = ServiceLocator.getInstance().getService(DriverService.class);

        Optional<DriverDescriptor> driverDescriptorOptional = driverService.getDriverDescriptor(driverPid);

        if (driverDescriptorOptional.isPresent()) {
            DriverDescriptor driverDescriptor = driverDescriptorOptional.get();
            return getGwtConfigComponent(driverDescriptor);
        } else {
            throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR);
        }
    }

    private GwtConfigComponent getGwtConfigComponent(DriverDescriptor driverDescriptor) {
        @SuppressWarnings("unchecked")
        final List<AD> params = (List<AD>) driverDescriptor.getChannelDescriptor();
        final GwtConfigComponent gwtConfig = new GwtConfigComponent();
        gwtConfig.setComponentId(driverDescriptor.getPid());

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
    }

    private String getChannelName(String propertyKey) {
        int pos = propertyKey.indexOf(AssetConstants.CHANNEL_PROPERTY_SEPARATOR.value());
        if (pos <= 0) {
            return null;
        }
        return propertyKey.substring(0, pos);
    }

    @Override
    public List<GwtChannelInfo> getGwtChannels(final GwtXSRFToken xsrfToken, final GwtConfigComponent descriptor,
            final GwtConfigComponent asset) throws GwtKuraException {

        final List<GwtChannelInfo> result = new ArrayList<>();

        final Set<String> channelNames = new HashSet<>();

        for (final GwtConfigParameter param : asset.getParameters()) {
            String channelName = getChannelName(param.getName());

            if (channelName != null) {
                channelNames.add(channelName);
            }
        }

        for (final String channelName : channelNames) {
            final GwtChannelInfo ci = getChannelFromProperties(channelName, descriptor, asset);
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
        final WireGraphService wireGraphService = ServiceLocator.getInstance().getService(WireGraphService.class);

        WireGraphConfiguration wireGraphConfiguration = null;
        Set<WireConfiguration> wireConfigurations = new HashSet<>();
        List<WireComponentConfiguration> wireComponentConfigurations = new ArrayList<>();
        try {
            wireGraphConfiguration = wireGraphService.get();
            wireConfigurations = new HashSet<>(wireGraphConfiguration.getWireConfigurations());
            wireComponentConfigurations = wireGraphConfiguration.getWireComponentConfigurations();
        } catch (KuraException e) {

        }

        final List<String> wireEmitterFactoryPids = new ArrayList<>();
        final List<String> wireReceiverFactoryPids = new ArrayList<>();
        final List<String> wireComponents = new ArrayList<>();

        Set<String> wireComponentsPids = new HashSet<>();

        GwtServerUtil.fillFactoriesLists(wireEmitterFactoryPids, wireReceiverFactoryPids);

        // create the JSON for the Wires Configuration
        final JsonObject wireConfig = Json.object();
        int i = 0;
        for (final WireConfiguration wireConfiguration : wireConfigurations) {
            final String emitterPid = wireConfiguration.getEmitterPid();
            final String receiverPid = wireConfiguration.getReceiverPid();
            wireComponents.add(getComponentString(emitterPid));
            wireComponents.add(getComponentString(receiverPid));
            wireComponentsPids.add(emitterPid);
            wireComponentsPids.add(receiverPid);

            final JsonObject wireConf = Json.object();
            wireConf.add("p", emitterPid).add("c", receiverPid);
            wireConfig.add(String.valueOf(++i), wireConf);
        }

        for (WireComponentConfiguration wireComponentConfiguration : wireComponentConfigurations) {
            ComponentConfiguration config = wireComponentConfiguration.getConfiguration();
            wireComponentsPids.add(config.getPid());
        }

        final List<GwtWireComponentConfiguration> configs = new ArrayList<>();
        for (final String wc : GwtWireServiceUtil.getWireComponents()) {
            // create instance of GWT Wire Component Configuration to hold all
            // the information for a Wire Component
            if (wireComponentsPids.contains(wc)) {
                final GwtWireComponentConfiguration config = new GwtWireComponentConfiguration();
                config.setFactoryPid(GwtWireServiceUtil.getFactoryPid(wc));
                config.setType(GwtWireServiceUtil.getType(wc));
                config.setPid(wc);
                config.setDriverPid(GwtWireServiceUtil.getDriverByPid(wc));
                configs.add(config);
            }
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
        configuration.getWireComponentPids().addAll(wireComponentsPids);
        configuration.setWiresConfigurationJson(wireConfig.toString());
        // configuration.setGraph(sGraph == null ? "{}" : sGraph);
        configuration.setWireComponentsJson(GwtWireServiceUtil.getWireComponentsJson(configs));
        configuration.setWireConfigurationsJson(GwtWireServiceUtil.getWireConfigurationsJson(wires));

        return configuration;
    }

    private boolean equals(WireConfiguration wire, GwtWireConfiguration gwtWire) {
        return wire.getEmitterPid().equals(gwtWire.getEmitterPid())
                && wire.getReceiverPid().equals(gwtWire.getReceiverPid());
    }

    private void createNewWireComponent(ConfigurationService configurationService, WireComponentDescriptor desc,
            GwtConfigComponent configuration) throws KuraException {
        final Map<String, Object> properties;
        if (configuration != null) {
            properties = GwtServerUtil.fillPropertiesFromConfiguration(configuration, null);
        } else {
            properties = new HashMap<>();
        }
        if (desc.getDriverPid() != null) {
            properties.put("asset.desc", "Sample Asset");
            properties.put("driver.pid", desc.getDriverPid());
        }
        logger.info(
                "Creating new Wire Component: Factory PID -> " + desc.getFactoryPid() + " | PID -> " + desc.getPid());
        configurationService.createFactoryConfiguration(desc.getFactoryPid(), desc.getPid(), properties, false);
    }

    /** {@inheritDoc} */
    @Override
    public void updateWireConfiguration(final GwtXSRFToken xsrfToken, final String newJsonConfiguration,
            final Map<String, GwtConfigComponent> configurations) throws GwtKuraException {
        this.checkXSRFToken(xsrfToken);

        JsonObject jWireGraph = null;
        final WireService wireService = ServiceLocator.getInstance().getService(WireService.class);
        final ConfigurationService configService = ServiceLocator.getInstance().getService(ConfigurationService.class);

        jWireGraph = Json.parse(newJsonConfiguration).asObject().get(GRAPH).asObject();

        final List<String> existingWireComponentPids = GwtWireServiceUtil.getWireComponents();
        Set<WireConfiguration> existingWires = new CopyOnWriteArraySet<>(wireService.getWireConfigurations());

        final List<GwtWireConfiguration> wiresInReceivedConfig = GwtWireServiceUtil
                .getWireConfigurationsFromJson(jWireGraph.get("wires").asObject());
        final Map<String, WireComponentDescriptor> wireComponentsInReceivedConfig = GwtWireServiceUtil
                .getWireComponentsFromJson(jWireGraph);
        boolean hasFailures = false;

        // remove no longer existing wires
        for (WireConfiguration existingWire : existingWires) {
            if (!wiresInReceivedConfig.stream().filter(receivedWire -> equals(existingWire, receivedWire)).findFirst()
                    .isPresent()) {
                logger.info("Deleting Wire: Emitter PID -> {} | Receiver PID -> {}", existingWire.getEmitterPid(),
                        existingWire.getReceiverPid());
                wireService.deleteWireConfiguration(existingWire);
            }
        }

        // remove no longer existing components
        for (String pid : existingWireComponentPids) {
            if (!wireComponentsInReceivedConfig.containsKey(pid)) {
                logger.info("Deleting Wire Component: PID -> {}", pid);
                try {
                    configService.deleteFactoryConfiguration(pid, false);
                } catch (KuraException e) {
                    logger.warn("Failed to delete wire component with pid: {}", pid, e);
                    hasFailures = true;
                }
            }
        }

        final Set<String> justCreatedComponents = new HashSet<>();
        // create new components
        for (Entry<String, WireComponentDescriptor> entry : wireComponentsInReceivedConfig.entrySet()) {
            if (!existingWireComponentPids.contains(entry.getKey())) {
                final WireComponentDescriptor desc = entry.getValue();
                try {
                    createNewWireComponent(configService, desc, configurations.get(desc.getPid()));
                    justCreatedComponents.add(desc.getPid());
                } catch (Exception e) {
                    logger.warn("Failed to create wire component", e);
                    hasFailures = true;
                }
            }
        }

        List<ComponentConfiguration> configurationsToUpdate = new ArrayList<>();
        // update existing components
        for (Entry<String, WireComponentDescriptor> entry : wireComponentsInReceivedConfig.entrySet()) {

            final String pid = entry.getKey();
            final GwtConfigComponent config = configurations.get(pid);

            if (justCreatedComponents.contains(pid) || config == null) {
                continue;
            }

            try {
                final ComponentConfiguration currentConf = configService.getComponentConfiguration(pid);
                final String currentFactoryPid = currentConf.getConfigurationProperties().get(SERVICE_FACTORYPID)
                        .toString();
                final String factoryPid = config.getFactoryId();

                if (!currentFactoryPid.equals(factoryPid)
                        || "org.eclipse.kura.wire.WireAsset".equalsIgnoreCase(factoryPid)) {
                    configService.deleteFactoryConfiguration(pid, false);
                    createNewWireComponent(configService, entry.getValue(), config);
                } else {
                    currentConf.getConfigurationProperties()
                            .putAll(GwtServerUtil.fillPropertiesFromConfiguration(config, currentConf));
                    configurationsToUpdate.add(currentConf);
                }
            } catch (Exception e) {
                logger.warn("Failed to update the configuration of wire component with pid: {}", pid, e);
                hasFailures = true;
            }
        }

        if (!configurationsToUpdate.isEmpty()) {
            try {
                configService.updateConfigurations(configurationsToUpdate, false);
            } catch (KuraException e) {
                logger.warn("Failed to update wire component configurations", e);
                hasFailures = true;
            }
        }

        existingWires = new CopyOnWriteArraySet<>(wireService.getWireConfigurations());
        for (final GwtWireConfiguration conf : wiresInReceivedConfig) {
            final String emitterPid = conf.getEmitterPid();
            final String receiverPid = conf.getReceiverPid();
            final WireConfiguration temp = new WireConfiguration(emitterPid, receiverPid);
            if (!existingWires.contains(temp)) {
                // track and wait for the emitter and receiver
                final String emitterFilter = "(" + KURA_SERVICE_PID + "=" + emitterPid + ")";
                final String receiverFilter = "(" + KURA_SERVICE_PID + "=" + receiverPid + ")";

                try {
                    final Optional<Object> emitter = ServiceUtil.waitForService(emitterFilter, TIMEOUT, SECONDS);
                    final Optional<Object> receiver = ServiceUtil.waitForService(receiverFilter, TIMEOUT, SECONDS);

                    if (emitter.isPresent() && receiver.isPresent()) {
                        logger.info("Creating New Wire: Emitter PID -> {} | Consumer PID -> {}", emitterPid,
                                receiverPid);
                        wireService.createWireConfiguration(emitterPid, receiverPid);
                    }
                } catch (Exception e) {
                    logger.warn("Failed to create wire", e);
                    hasFailures = true;
                }
            }
        }

        try {
            final Map<String, Object> props = configService.getComponentConfiguration(WIRE_SERVICE_PID)
                    .getConfigurationProperties();
            // remove wires JSON from actual wire graph
            jWireGraph.remove("wires");
            props.put(GRAPH, jWireGraph.toString());
            configService.updateConfiguration(WIRE_SERVICE_PID, props, true);
        } catch (Exception e) {
            logger.warn("Failed to update wire service", e);
            hasFailures = true;
        }

        if (hasFailures) {
            throw new GwtKuraException("Failed to update wire configuration");
        }
    }
}

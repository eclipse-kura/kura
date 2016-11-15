/*******************************************************************************
 * Copyright (c) 2016 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *******************************************************************************/
package org.eclipse.kura.web.server;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.eclipse.kura.KuraException;
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
import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The class GwtWireServiceImpl implements {@link GwtWireService}
 */
public final class GwtWireServiceImpl extends OsgiRemoteServiceServlet implements GwtWireService {

    private static final String CONSUMER = "consumer";
    private static final String GRAPH = "wiregraph";
    private static final String PRODUCER = "producer";
    /** The Logger instance. */
    private static final Logger s_logger = LoggerFactory.getLogger(GwtWireServiceImpl.class);

    /** Serial Version */
    private static final long serialVersionUID = -6577843865830245755L;

    /** Wire Service PID Property */
    private static final String WIRE_SERVICE_PID = "org.eclipse.kura.wire.WireService";

    private static Map<String, Object> fillPropertiesFromConfiguration(final GwtConfigComponent config,
            final ComponentConfiguration currentCC) {
        // Build the new properties
        final Map<String, Object> properties = new HashMap<String, Object>();
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
            if ((cardinality == 0) || (cardinality == 1) || (cardinality == -1)) {

                final String strValue = gwtConfigParam.getValue();

                if ((currentObjValue instanceof Password) && PLACEHOLDER.equals(strValue)) {
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
        if (backupConfigProp.get("kura.service.pid") != null) {
            properties.put("kura.service.pid", backupConfigProp.get("kura.service.pid"));
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
            if (ref.getProperty(ConfigurationService.KURA_SERVICE_PID).equals(pid)) {
                final String fPid = (String) ref.getProperty(ConfigurationAdmin.SERVICE_FACTORYPID);
                final WireComponent comp = ctx.getService(ref);
                String compType;
                if ((comp instanceof WireEmitter) && (comp instanceof WireReceiver)) {
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
        s_logger.error("Could not find WireComponent for pid {}", pid);
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
        final List<String> drivers = new ArrayList<String>();
        for (final ServiceReference<Driver> ref : refs) {
            drivers.add(String.valueOf(ref.getProperty("kura.service.pid")));
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

            final List<GwtConfigParameter> gwtParams = new ArrayList<GwtConfigParameter>();
            gwtConfig.setParameters(gwtParams);
            for (final AD ad : params) {
                final GwtConfigParameter gwtParam = new GwtConfigParameter();
                gwtParam.setId(ad.getId());
                gwtParam.setName(ad.getName());
                gwtParam.setDescription(ad.getDescription());
                gwtParam.setType(GwtConfigParameterType.valueOf(ad.getType().name()));
                gwtParam.setRequired(ad.isRequired());
                gwtParam.setCardinality(ad.getCardinality());
                if ((ad.getOption() != null) && !ad.getOption().isEmpty()) {
                    final Map<String, String> options = new HashMap<String, String>();
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

            final List<GwtConfigParameter> gwtParams = new ArrayList<GwtConfigParameter>();
            gwtConfig.setParameters(gwtParams);
            for (final AD ad : params) {
                final GwtConfigParameter gwtParam = new GwtConfigParameter();
                gwtParam.setId(ad.getId());
                gwtParam.setName(ad.getName());
                gwtParam.setDescription(ad.getDescription());
                gwtParam.setType(GwtConfigParameterType.valueOf(ad.getType().name()));
                gwtParam.setRequired(ad.isRequired());
                gwtParam.setCardinality(ad.getCardinality());
                if ((ad.getOption() != null) && !ad.getOption().isEmpty()) {
                    final Map<String, String> options = new HashMap<String, String>();
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

        final List<GwtChannelInfo> result = new ArrayList<GwtChannelInfo>();

        final Set<Integer> channelIndexes = new HashSet<Integer>();
        for (final GwtConfigParameter param : asset.getParameters()) {
            if (param.getName().endsWith("CH.name")) {
                final String[] tokens = param.getName().split("\\.");
                channelIndexes.add(Integer.parseInt(tokens[0]));
            }
        }

        for (final Integer index : channelIndexes) {
            final GwtChannelInfo ci = this.getChannelFromProperties(index, descriptor, asset);
            result.add(ci);
        }

        return result;
    }

    /** {@inheritDoc} */
    @Override
    public GwtWiresConfiguration getWiresConfiguration(final GwtXSRFToken xsrfToken) throws GwtKuraException {
        this.checkXSRFToken(xsrfToken);
        return this.getWiresConfigurationInternal();
    }

    private GwtWiresConfiguration getWiresConfigurationInternal() throws GwtKuraException {
        final ConfigurationService configService = ServiceLocator.getInstance().getService(ConfigurationService.class);
        final WireService wireService = ServiceLocator.getInstance().getService(WireService.class);

        final Set<WireConfiguration> wireConfigurations = wireService.getWireConfigurations();
        final List<String> wireEmitterFactoryPids = new ArrayList<String>();
        final List<String> wireReceiverFactoryPids = new ArrayList<String>();
        final List<String> wireComponents = new ArrayList<String>();

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
        final JSONObject wireConfig = new JSONObject();
        int i = 0;
        for (final WireConfiguration wireConfiguration : wireConfigurations) {
            final String emitterPid = wireConfiguration.getEmitterPid();
            final String receiverPid = wireConfiguration.getReceiverPid();
            wireComponents.add(getComponentString(emitterPid));
            wireComponents.add(getComponentString(receiverPid));

            final JSONObject wireConf = new JSONObject();
            try {
                wireConf.put("p", emitterPid);
                wireConf.put("c", receiverPid);
                wireConfig.put(String.valueOf(++i), wireConf);
            } catch (final JSONException exception) {
                throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR, exception);
            }
        }
        final List<GwtWireComponentConfiguration> configs = new ArrayList<GwtWireComponentConfiguration>();
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

        final List<GwtWireConfiguration> wires = new ArrayList<GwtWireConfiguration>();
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
    public GwtWiresConfiguration updateWireConfiguration(final GwtXSRFToken xsrfToken,
            final String newJsonConfiguration, final Map<String, GwtConfigComponent> configurations)
                    throws GwtKuraException {
        this.checkXSRFToken(xsrfToken);

        JSONObject jWireGraph = null;
        final WireService wireService = ServiceLocator.getInstance().getService(WireService.class);
        final WireHelperService wireHelperService = ServiceLocator.getInstance().getService(WireHelperService.class);
        final ConfigurationService configService = ServiceLocator.getInstance().getService(ConfigurationService.class);

        try {
            jWireGraph = new JSONObject(newJsonConfiguration).getJSONObject(GRAPH);
            // don't consider the "wires" JSON
            final int length = jWireGraph.length() - 1;
            // Delete wires
            final Set<WireConfiguration> set = new CopyOnWriteArraySet<WireConfiguration>(
                    wireService.getWireConfigurations());
            Iterator<WireConfiguration> iterator = set.iterator();
            while (iterator.hasNext()) {
                final WireConfiguration wireConfiguration = iterator.next();
                // check if jObj is an empty JSON. It means all the existing
                // wire configurations need to be deleted
                if (length == 0) {
                    s_logger.info("Deleting Wire: Emitter PID -> " + wireConfiguration.getEmitterPid()
                            + " | Receiver PID -> " + wireConfiguration.getReceiverPid());
                    wireService.deleteWireConfiguration(wireConfiguration);
                }
            }

            final Set<WireConfiguration> configs = new CopyOnWriteArraySet<WireConfiguration>(
                    wireService.getWireConfigurations());
            iterator = configs.iterator();
            while (iterator.hasNext()) {
                final WireConfiguration wireConfiguration = iterator.next();
                boolean isFound = false;
                for (final GwtWireConfiguration configuration : GwtWireServiceUtil
                        .getWireConfigurationsFromJson(jWireGraph.getJSONObject("wires"))) {
                    final WireConfiguration temp = new WireConfiguration(configuration.getEmitterPid(),
                            configuration.getReceiverPid(), null);
                    if (temp.equals(wireConfiguration)) {
                        isFound = true;
                    }
                }
                if (!isFound) {
                    s_logger.info("Deleting Wire: Emitter PID -> " + wireConfiguration.getEmitterPid()
                            + " | Receiver PID -> " + wireConfiguration.getReceiverPid());
                    wireService.deleteWireConfiguration(wireConfiguration);
                }
            }

            // Delete Wire Component instances
            final List<String> wireComponents = GwtWireServiceUtil.getWireComponents();
            for (final String componentPid : GwtWireServiceUtil.getWireComponents()) {
                // check if jObj is an empty JSON. It means all the existing
                // wire components need to be deleted
                if (length == 0) {
                    s_logger.info("Deleting Wire Component: PID -> " + componentPid);
                    configService.deleteFactoryConfiguration(componentPid, false);
                    continue;
                }
                boolean isFound = false;
                for (int i = 0; i < length; i++) {
                    final JSONObject jsonObject = jWireGraph.getJSONObject(String.valueOf(i));
                    final String component = jsonObject.getString("pid");
                    if (component.equalsIgnoreCase(componentPid)) {
                        isFound = true;
                        break;
                    }
                }
                if (!isFound) {
                    s_logger.info("Deleting Wire Component: PID -> " + componentPid);
                    configService.deleteFactoryConfiguration(componentPid, false);
                }
            }

            for (int i = 0; i < length; i++) {
                final JSONObject jsonObject = jWireGraph.getJSONObject(String.valueOf(i));
                String pid = null;
                String fpid = null;
                String driver = null;
                try {
                    pid = jsonObject.getString("pid");
                    fpid = jsonObject.getString("fpid");
                    driver = jsonObject.getString("driver");
                } catch (final JSONException jse) {
                    // no need
                }
                Map<String, Object> properties = null;
                if ((pid != null) && !wireComponents.contains(pid)) {
                    s_logger.info("Creating new Wire Component: Factory PID -> " + fpid + " | PID -> " + pid);
                    if (driver != null) {
                        properties = new HashMap<String, Object>();
                        properties.put("asset.desc", "Sample Asset");
                        properties.put("driver.pid", driver);
                    }
                    configService.createFactoryConfiguration(fpid, pid, properties, false);
                }
            }

            // Create new wires
            final Set<WireConfiguration> wireConfs = wireService.getWireConfigurations();
            for (final GwtWireConfiguration conf : GwtWireServiceUtil
                    .getWireConfigurationsFromJson(jWireGraph.getJSONObject("wires"))) {
                final String emitterPid = conf.getEmitterPid();
                final String receiverPid = conf.getReceiverPid();
                final WireConfiguration temp = new WireConfiguration(emitterPid, receiverPid, null);
                if (!wireConfs.contains(temp)) {
                    s_logger.info(
                            "Creating new wire: Emitter PID -> " + emitterPid + " | Consumer PID -> " + receiverPid);
                    s_logger.info("Service PID for Emitter before tracker: {}",
                            wireHelperService.getServicePid(emitterPid));
                    s_logger.info("Service PID for Receiver before tracker: {}",
                            wireHelperService.getServicePid(receiverPid));

                    // track and wait for the emitter
                    final String pPid = wireHelperService.getServicePid(emitterPid);
                    final BundleContext bundleContext = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
                    String filterString = "(" + Constants.SERVICE_PID + "=" + pPid + ")";
                    Filter filter = bundleContext.createFilter(filterString);
                    final ServiceTracker producerTracker = new ServiceTracker(bundleContext, filter, null);
                    producerTracker.open();
                    producerTracker.waitForService(5000);
                    producerTracker.close();

                    // track and wait for the receiver
                    final String cPid = wireHelperService.getServicePid(receiverPid);
                    filterString = "(" + Constants.SERVICE_PID + "=" + cPid + ")";
                    filter = bundleContext.createFilter(filterString);
                    final ServiceTracker consumerTracker = new ServiceTracker(bundleContext, filter, null);
                    consumerTracker.open();
                    consumerTracker.waitForService(5000);
                    consumerTracker.close();

                    wireService.createWireConfiguration(emitterPid, receiverPid);
                }
            }

            // Update configuration for all changes tracked in Wires Composer
            for (final String pid : configurations.keySet()) {
                final GwtConfigComponent config = configurations.get(pid);
                if (config != null) {
                    final ComponentConfiguration currentConf = configService.getComponentConfiguration(pid);
                    final Map<String, Object> props = fillPropertiesFromConfiguration(config, currentConf);
                    if (props != null) {
                        final String factoryPid = config.getFactoryId();
                        if ("org.eclipse.kura.wire.WireAsset".equalsIgnoreCase(factoryPid)) {
                            configService.deleteFactoryConfiguration(pid, false);
                            configService.createFactoryConfiguration(factoryPid, pid, props, false);
                        }
                        configService.updateConfiguration(pid, props, false);
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
        } catch (final JSONException exception) {
            throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR, exception);
        } catch (final KuraException exception) {
            throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR, exception);
        } catch (final InterruptedException exception) {
            throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR, exception);
        } catch (final InvalidSyntaxException exception) {
            throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR, exception);
        }
        return this.getWiresConfigurationInternal();
    }

}

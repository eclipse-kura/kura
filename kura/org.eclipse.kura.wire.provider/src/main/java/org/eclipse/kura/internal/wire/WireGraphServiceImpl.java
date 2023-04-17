/*******************************************************************************
 * Copyright (c) 2016, 2023 Eurotech and/or its affiliates and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Eurotech
 *  Amit Kumar Mondal
 *  Red Hat Inc
 *******************************************************************************/
package org.eclipse.kura.internal.wire;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static org.eclipse.kura.configuration.ConfigurationService.KURA_SERVICE_PID;
import static org.osgi.framework.Constants.SERVICE_PID;
import static org.osgi.service.cm.ConfigurationAdmin.SERVICE_FACTORYPID;
import static org.osgi.service.wireadmin.WireConstants.WIREADMIN_CONSUMER_PID;
import static org.osgi.service.wireadmin.WireConstants.WIREADMIN_PRODUCER_PID;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.core.configuration.ComponentConfigurationImpl;
import org.eclipse.kura.core.configuration.metatype.Tocd;
import org.eclipse.kura.marshalling.Marshaller;
import org.eclipse.kura.marshalling.Unmarshaller;
import org.eclipse.kura.util.service.ServiceUtil;
import org.eclipse.kura.wire.WireComponent;
import org.eclipse.kura.wire.graph.Constants;
import org.eclipse.kura.wire.graph.MultiportWireConfiguration;
import org.eclipse.kura.wire.graph.WireComponentConfiguration;
import org.eclipse.kura.wire.graph.WireGraphConfiguration;
import org.eclipse.kura.wire.graph.WireGraphService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.wireadmin.Wire;
import org.osgi.service.wireadmin.WireAdmin;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Class {@link WireGraphServiceImpl} implements {@link WireGraphService}
 */
public class WireGraphServiceImpl implements ConfigurableComponent, WireGraphService {

    private static final String OUTPUT_PORT_COUNT = "outputPortCount";

    private static final String INPUT_PORT_COUNT = "inputPortCount";

    private static final String WIRE_ASSET_FACTORY_PID = "org.eclipse.kura.wire.WireAsset";

    private static final String NEW_WIRE_GRAPH_PROPERTY = "WireGraph";

    /** Configuration PID Property */
    private static final String CONF_PID = "org.eclipse.kura.wire.graph.WireGraphService";

    private static final Logger logger = LoggerFactory.getLogger(WireGraphServiceImpl.class);

    private volatile WireAdmin wireAdmin;

    private ServiceTracker<WireComponent, WireComponent> wireComponentServiceTracker;

    private ConfigurationService configurationService;
    private BundleContext bundleContext;

    private WireGraphConfiguration currentConfiguration;

    private static final Filter WIRE_COMPONENT_FILTER = getWireComponentConfigurationFilter();

    private static final Map<String, Object> DEFAULT_RENDERING_PROPERTIES = buildDefaultRenderingProperties();

    /**
     * Binds the {@link WireAdmin} dependency
     *
     * @param wireAdmin
     *                  the new {@link WireAdmin} service dependency
     */
    public void bindWireAdmin(final WireAdmin wireAdmin) {
        if (isNull(this.wireAdmin)) {
            this.wireAdmin = wireAdmin;
        }
    }

    /**
     * Unbinds {@link WireAdmin} dependency
     *
     * @param wireAdmin
     *                  the new {@link WireAdmin} instance
     */
    public void unbindWireAdmin(final WireAdmin wireAdmin) {
        if (this.wireAdmin == wireAdmin) {
            this.wireAdmin = null;
        }
    }

    public void setConfigurationService(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    public void unsetConfigurationService(ConfigurationService configurationService) {
        this.configurationService = null;
    }

    protected void activate(final ComponentContext componentContext, final Map<String, Object> properties) {
        logger.info("Activating Wire Service...");
        this.bundleContext = componentContext.getBundleContext();

        updated(properties);

        logger.info("Activating Wire Service...Done");
    }

    public synchronized void updated(final Map<String, Object> properties) {

        try {
            logger.info("Updating Wire Graph Service Component...");

            final WireGraphConfiguration config = loadWireGraphConfiguration(properties);

            this.currentConfiguration = new WireGraphConfiguration(
                    fillRenderingPropertyDefaults(config.getWireComponentConfigurations()),
                    config.getWireConfigurations());

            if (this.wireComponentServiceTracker == null) {
                logger.info("Opening Wire Component Service tracker...");
                WireComponentTrackerCustomizer wireComponentTrackerCustomizer = new WireComponentTrackerCustomizer(
                        this.bundleContext, this);
                this.wireComponentServiceTracker = new ServiceTracker<>(this.bundleContext, WireComponent.class,
                        wireComponentTrackerCustomizer);
                this.wireComponentServiceTracker.open();
                logger.info("Opening Wire Component Service tracker...done");
            }

            createWires();

            logger.info("Updating Wire Service Component...Done");
        } catch (Exception e) {
            logger.warn("Failed to update WireGraphServiceImpl", e);
        }

    }

    protected void deactivate(final ComponentContext componentContext) {
        logger.info("Deactivating Wire Service Component...");
        this.bundleContext = null;
        if (this.wireComponentServiceTracker != null) {
            this.wireComponentServiceTracker.close();
        }

        deleteAllWires();

        logger.info("Deactivating Wire Service Component...Done");
    }

    private boolean checkWireExistence(final String emitterServicePid, final String receiverServicePid,
            final int emitterPort, final int receiverPort) throws InvalidSyntaxException {
        requireNonNull(emitterServicePid, "Emitter Service PID cannot be null");
        requireNonNull(receiverServicePid, "Receiver Service PID cannot be null");

        final Wire[] wires = this.wireAdmin.getWires(null);
        if (nonNull(wires)) {
            for (final Wire w : wires) {
                final Dictionary<?, ?> props = w.getProperties();
                if (emitterServicePid.equals(props.get(WIREADMIN_PRODUCER_PID))
                        && receiverServicePid.equals(props.get(WIREADMIN_CONSUMER_PID))
                        && emitterPort == (Integer) props.get(Constants.WIRE_EMITTER_PORT_PROP_NAME.value())
                        && receiverPort == (Integer) props.get(Constants.WIRE_RECEIVER_PORT_PROP_NAME.value())) {
                    return true;
                }
            }
        }
        return false;

    }

    private void createConfiguration(Collection<ServiceReference<WireComponent>> wireComponentServiceReferences,
            final MultiportWireConfiguration conf) {
        requireNonNull(conf, "Wire Configuration cannot be null");

        String emitterPid = conf.getEmitterPid();
        String receiverPid = conf.getReceiverPid();
        try {
            final String emitterServicePid = getServicePidByKuraServicePid(wireComponentServiceReferences, emitterPid);
            final String receiverServicePid = getServicePidByKuraServicePid(wireComponentServiceReferences,
                    receiverPid);
            final int emitterPort = conf.getEmitterPort();
            final int receiverPort = conf.getReceiverPort();
            if (nonNull(emitterServicePid) && nonNull(receiverServicePid)) {
                final boolean found = checkWireExistence(emitterServicePid, receiverServicePid, emitterPort,
                        receiverPort);
                if (!found) {
                    logger.info("Creating wire between {}/{} and {}/{}...", emitterPid, emitterPort, receiverPid,
                            receiverPort);
                    final Dictionary<String, Object> properties = new Hashtable<>();
                    properties.put(Constants.WIRE_EMITTER_PORT_PROP_NAME.value(), emitterPort);
                    properties.put(Constants.WIRE_RECEIVER_PORT_PROP_NAME.value(), receiverPort);
                    properties.put(Constants.EMITTER_KURA_SERVICE_PID_PROP_NAME.value(), emitterPid);
                    properties.put(Constants.RECEIVER_KURA_SERVICE_PID_PROP_NAME.value(), receiverPid);
                    final Wire wire = this.wireAdmin.createWire(emitterServicePid, receiverServicePid, properties);
                    conf.setWire(wire);
                    logger.info("Creating wire.....Done");
                }
            }

        } catch (final InvalidSyntaxException e) {
            logger.error("Error while creating wires configuration...", e);
        }
    }

    /**
     * Create the wires based on the provided wire configurations
     */
    synchronized void createWires() {
        try {
            Collection<ServiceReference<WireComponent>> wireComponentServiceReferences = this.bundleContext
                    .getServiceReferences(WireComponent.class, null);
            for (final MultiportWireConfiguration wireConfig : this.currentConfiguration.getWireConfigurations()) {
                createConfiguration(wireComponentServiceReferences, wireConfig);
            }
        } catch (InvalidSyntaxException e) {
            logger.error("Error while creating wires...", e);
        }
    }

    private static MultiportWireConfiguration toWireConfiguration(Wire wire) {
        final Dictionary<?, ?> wireProps = wire.getProperties();

        final Object emitterKuraServicePid = wireProps.get(Constants.EMITTER_KURA_SERVICE_PID_PROP_NAME.value());
        final Object receiverKuraServicePid = wireProps.get(Constants.RECEIVER_KURA_SERVICE_PID_PROP_NAME.value());
        final Object emitterPort = wireProps.get(Constants.WIRE_EMITTER_PORT_PROP_NAME.value());
        final Object receiverPort = wireProps.get(Constants.WIRE_RECEIVER_PORT_PROP_NAME.value());

        if (!(emitterKuraServicePid instanceof String) || !(receiverKuraServicePid instanceof String)
                || !(emitterPort instanceof Integer) || !(receiverPort instanceof Integer)) {
            return null;
        }

        return new MultiportWireConfiguration((String) emitterKuraServicePid, (String) receiverKuraServicePid,
                (Integer) emitterPort, (Integer) receiverPort);
    }

    private void deleteNoLongerExistingWires(final Set<MultiportWireConfiguration> newWires,
            final Set<String> componentsToDelete) {
        try {
            final Wire[] wiresList = this.wireAdmin.getWires(null);
            if (wiresList == null) {
                return;
            }
            for (final Wire osgiWire : wiresList) {
                final MultiportWireConfiguration wire = toWireConfiguration(osgiWire);
                if (wire == null) {
                    continue;
                }
                if (!newWires.contains(wire) || componentsToDelete.contains(wire.getEmitterPid())
                        || componentsToDelete.contains(wire.getReceiverPid())) {
                    logger.info("Removing wire between {} and {} ...", wire.getEmitterPid(), wire.getReceiverPid());
                    this.wireAdmin.deleteWire(osgiWire);
                    logger.info("Removing wire between {} and {} ... done", wire.getEmitterPid(),
                            wire.getReceiverPid());
                }
            }
        } catch (InvalidSyntaxException e) {
            // no need since no filter is passed to getWires()
        }
    }

    private void deleteAllWires() {

        try {
            final Wire[] wires = this.wireAdmin.getWires(null);

            if (wires == null) {
                return;
            }

            for (Wire w : wires) {
                if (toWireConfiguration(w) != null) {
                    this.wireAdmin.deleteWire(w);
                }
            }
        } catch (InvalidSyntaxException e) {
            // no need since no filter is passed to getWires()
        }

    }

    @Override
    public synchronized void update(WireGraphConfiguration newConfiguration) throws KuraException {

        final WireGraphConfiguration currentGraphConfiguration = get();
        final List<WireComponentConfiguration> currentWireComponents = currentGraphConfiguration
                .getWireComponentConfigurations();

        List<ComponentConfiguration> componentConfigurations = new ArrayList<>();

        List<WireComponentConfiguration> newWireComponentConfigurations = newConfiguration
                .getWireComponentConfigurations();

        validateWireComponentConfigurations(newWireComponentConfigurations);

        newWireComponentConfigurations = fillRenderingPropertyDefaults(newWireComponentConfigurations);

        Set<MultiportWireConfiguration> newWires = new HashSet<>(newConfiguration.getWireConfigurations());

        // Evaluate deletable components
        Set<String> componentsToDeletePids = getComponentsToDelete(currentWireComponents,
                newWireComponentConfigurations);

        List<WireComponentConfiguration> componentsToCreate = getComponentsToCreate(currentWireComponents,
                newWireComponentConfigurations);

        validateComponentsToCreate(componentsToCreate);

        logger.info("Closing Wire Component Service tracker...");
        if (this.wireComponentServiceTracker != null) {
            this.wireComponentServiceTracker.close();
            this.wireComponentServiceTracker = null;
        }
        logger.info("Closing Wire Component Service tracker...done");

        for (final String pid : componentsToDeletePids) {
            this.configurationService.deleteFactoryConfiguration(pid, false);
        }

        deleteNoLongerExistingWires(newWires, componentsToDeletePids);
        // create new components

        List<String> createdPids = new ArrayList<>();
        for (WireComponentConfiguration componentToCreate : componentsToCreate) {

            final ComponentConfiguration configToCreate = componentToCreate.getConfiguration();
            try {
                final Map<String, Object> wireComponentProps = componentToCreate.getProperties();
                final Map<String, Object> configurationProps = configToCreate.getConfigurationProperties();

                String factoryPid = (String) configurationProps.get(SERVICE_FACTORYPID);

                configurationProps.put(Constants.RECEIVER_PORT_COUNT_PROP_NAME.value(),
                        wireComponentProps.get(INPUT_PORT_COUNT));
                configurationProps.put(Constants.EMITTER_PORT_COUNT_PROP_NAME.value(),
                        wireComponentProps.get(OUTPUT_PORT_COUNT));

                this.configurationService.createFactoryConfiguration(factoryPid, configToCreate.getPid(),
                        configurationProps, false);
            } catch (Exception e) {
                deleteConfigurations(createdPids);
                throw e;
            }
            createdPids.add(configToCreate.getPid());
        }

        // Evaluate updatable components
        List<WireComponentConfiguration> componentsToUpdate = getComponentsToUpdate(newWireComponentConfigurations,
                componentsToCreate);
        for (WireComponentConfiguration componentToUpdate : componentsToUpdate) {
            componentConfigurations.add(componentToUpdate.getConfiguration());
        }

        final WireGraphConfiguration resultConfig = new WireGraphConfiguration(newWireComponentConfigurations,
                newConfiguration.getWireConfigurations());

        String jsonConfig = marshal(resultConfig);
        ComponentConfiguration wireGraphServiceComponentConfig = this.configurationService
                .getComponentConfiguration(CONF_PID);
        wireGraphServiceComponentConfig.getConfigurationProperties().put(NEW_WIRE_GRAPH_PROPERTY, jsonConfig);

        componentConfigurations.add(wireGraphServiceComponentConfig);

        this.currentConfiguration = resultConfig;
        this.configurationService.updateConfigurations(componentConfigurations, true);
    }

    private void validateComponentsToCreate(final List<WireComponentConfiguration> componentsToCreate)
            throws KuraException {
        for (WireComponentConfiguration componentToCreate : componentsToCreate) {

            final ComponentConfiguration configToCreate = componentToCreate.getConfiguration();
            final Map<String, Object> wireComponentProps = componentToCreate.getProperties();
            final Map<String, Object> configurationProps = configToCreate.getConfigurationProperties();

            if (configurationProps == null) {
                throw new KuraException(KuraErrorCode.BAD_REQUEST, null,
                        configurationPropertiesMissingMessage(componentToCreate));
            }

            if (!(configurationProps.get(SERVICE_FACTORYPID) instanceof String)) {
                throw new KuraException(KuraErrorCode.BAD_REQUEST, null,
                        propertyMissingMessage(componentToCreate, SERVICE_FACTORYPID));
            }

            if (!(wireComponentProps.get(INPUT_PORT_COUNT) instanceof Integer)) {
                throw new KuraException(KuraErrorCode.BAD_REQUEST, null,
                        propertyMissingMessage(componentToCreate, INPUT_PORT_COUNT));
            }

            if (!(wireComponentProps.get(OUTPUT_PORT_COUNT) instanceof Integer)) {
                throw new KuraException(KuraErrorCode.BAD_REQUEST, null,
                        propertyMissingMessage(componentToCreate, OUTPUT_PORT_COUNT));
            }
        }
    }

    private void validateWireComponentConfigurations(final List<WireComponentConfiguration> componentsToCreate)
            throws KuraException {
        for (WireComponentConfiguration componentToCreate : componentsToCreate) {

            final Map<String, Object> wireComponentProps = componentToCreate.getProperties();

            if (!(wireComponentProps.get(INPUT_PORT_COUNT) instanceof Integer)) {
                throw new KuraException(KuraErrorCode.BAD_REQUEST, null,
                        propertyMissingMessage(componentToCreate, INPUT_PORT_COUNT));
            }

            if (!(wireComponentProps.get(OUTPUT_PORT_COUNT) instanceof Integer)) {
                throw new KuraException(KuraErrorCode.BAD_REQUEST, null,
                        propertyMissingMessage(componentToCreate, OUTPUT_PORT_COUNT));
            }
        }
    }

    private String propertyMissingMessage(WireComponentConfiguration componentToCreate, final String property) {
        return "Component " + componentToCreate.getConfiguration().getPid() + " must be created but the \"" + property
                + "\" property is not valid";
    }

    private String configurationPropertiesMissingMessage(WireComponentConfiguration componentToCreate) {
        return "Component " + componentToCreate.getConfiguration().getPid()
                + " must be created but configuration properties are not specified";
    }

    private void deleteConfigurations(List<String> createdPids) {
        for (String createdPid : createdPids) {
            try {
                this.configurationService.deleteFactoryConfiguration(createdPid, false);
            } catch (Exception e1) {
                logger.debug("Failed to delete factory configuration", e1);
            }
        }
    }

    private List<WireComponentConfiguration> getComponentsToUpdate(
            List<WireComponentConfiguration> newWireComponentConfigurations,
            List<WireComponentConfiguration> newComponents) {
        List<WireComponentConfiguration> componentsToUpdate = new ArrayList<>();

        for (WireComponentConfiguration newWireComponentConfiguration : newWireComponentConfigurations) {
            if (newWireComponentConfiguration.getConfiguration().getConfigurationProperties() != null) {
                componentsToUpdate.add(newWireComponentConfiguration);
            }
        }

        componentsToUpdate.removeAll(newComponents);
        return componentsToUpdate;
    }

    private List<WireComponentConfiguration> getComponentsToCreate(
            List<WireComponentConfiguration> oldWireComponentConfigurations,
            List<WireComponentConfiguration> newWireComponentConfigurations) {

        Set<String> oldPids = oldWireComponentConfigurations.stream().map(com -> com.getConfiguration().getPid())
                .collect(Collectors.toSet());

        return newWireComponentConfigurations.stream().filter(newCom -> {
            final Optional<String> factoryPid = getFactoryPid(newCom);

            final boolean isNewComponent = !oldPids.contains(newCom.getConfiguration().getPid());
            final boolean isWireAsset = factoryPid.isPresent()
                    && WIRE_ASSET_FACTORY_PID.contentEquals(factoryPid.get());

            return isNewComponent || isWireAsset;
        }).collect(Collectors.toList());

    }

    private Set<String> getComponentsToDelete(List<WireComponentConfiguration> oldWireComponentConfigurations,
            List<WireComponentConfiguration> newWireComponentConfigurations) {

        final Map<String, WireComponentConfiguration> newGrouped = newWireComponentConfigurations.stream()
                .collect(Collectors.toMap(c -> c.getConfiguration().getPid(), c -> c));

        return Stream.concat(//
                oldWireComponentConfigurations.stream().filter(comp -> {
                    final ComponentConfiguration config = comp.getConfiguration();

                    final String pid = config.getPid();

                    final Optional<String> oldFactoryPid = getFactoryPid(comp);
                    final Optional<String> newFactoryPid = Optional.ofNullable(newGrouped.get(pid))
                            .flatMap(WireGraphServiceImpl::getFactoryPid);

                    final boolean hasBeenRemoved = !newGrouped.containsKey(pid);
                    final boolean isWireAsset = oldFactoryPid.isPresent()
                            && WIRE_ASSET_FACTORY_PID.equals(oldFactoryPid.get());
                    final boolean hasChangedFactoryPid = newFactoryPid.isPresent() && oldFactoryPid.isPresent()
                            && !oldFactoryPid.get().contentEquals(newFactoryPid.get());

                    return hasChangedFactoryPid || hasBeenRemoved && !isWireAsset;
                }), //
                newWireComponentConfigurations.stream().filter(WireGraphServiceImpl::isWireAsset)//
        ).map(c -> c.getConfiguration().getPid()).collect(Collectors.toSet());

    }

    @Override
    public synchronized void delete() throws KuraException {
        deleteAllWires();

        for (WireComponentConfiguration config : this.currentConfiguration.getWireComponentConfigurations()) {
            this.configurationService.deleteFactoryConfiguration(config.getConfiguration().getPid(), false);
        }

        WireGraphConfiguration newWireGraphConfiguration = new WireGraphConfiguration(new ArrayList<>(),
                new ArrayList<>());

        String jsonConfig = marshal(newWireGraphConfiguration);
        ComponentConfiguration wireGraphServiceComponentConfig = this.configurationService
                .getComponentConfiguration(CONF_PID);
        wireGraphServiceComponentConfig.getConfigurationProperties().put(NEW_WIRE_GRAPH_PROPERTY, jsonConfig);

        this.configurationService.updateConfiguration(CONF_PID,
                wireGraphServiceComponentConfig.getConfigurationProperties(), true);
        this.currentConfiguration = newWireGraphConfiguration;
    }

    @Override
    public WireGraphConfiguration get() throws KuraException {

        List<ComponentConfiguration> configServiceComponentConfigurations = this.configurationService
                .getComponentConfigurations(WIRE_COMPONENT_FILTER);

        WireGraphConfiguration wireGraphConfiguration = this.currentConfiguration;

        List<WireComponentConfiguration> wireComponentConfigurations = wireGraphConfiguration
                .getWireComponentConfigurations();

        List<WireComponentConfiguration> completeWireComponentConfigurations = new ArrayList<>();
        for (WireComponentConfiguration wireComponentConfiguration : wireComponentConfigurations) {
            ComponentConfiguration wComponentConfiguration = wireComponentConfiguration.getConfiguration();
            Map<String, Object> wComponentProperties = wireComponentConfiguration.getProperties();
            String wComponentPid = wComponentConfiguration.getPid();

            for (ComponentConfiguration configServiceComponentConfiguration : configServiceComponentConfigurations) {
                if (wComponentPid.equals(configServiceComponentConfiguration.getPid())) {
                    wComponentConfiguration = new ComponentConfigurationImpl(wComponentPid,
                            (Tocd) configServiceComponentConfiguration.getDefinition(),
                            configServiceComponentConfiguration.getConfigurationProperties());
                    break;
                }
            }
            completeWireComponentConfigurations
                    .add(new WireComponentConfiguration(wComponentConfiguration, wComponentProperties));
        }

        return new WireGraphConfiguration(completeWireComponentConfigurations,
                wireGraphConfiguration.getWireConfigurations());
    }

    private ServiceReference<Marshaller>[] getJsonMarshallers() {
        return ServiceUtil.getServiceReferences(this.bundleContext, Marshaller.class,
                "(kura.service.pid=org.eclipse.kura.json.marshaller.unmarshaller.provider)");
    }

    private ServiceReference<Unmarshaller>[] getJsonUnmarshallers() {
        return ServiceUtil.getServiceReferences(this.bundleContext, Unmarshaller.class,
                "(kura.service.pid=org.eclipse.kura.json.marshaller.unmarshaller.provider)");
    }

    protected <T> T unmarshal(String string, Class<T> clazz) throws KuraException {
        T result = null;
        final ServiceReference<Unmarshaller>[] unmarshallerSRs = getJsonUnmarshallers();
        try {
            for (final ServiceReference<Unmarshaller> unmarshallerSR : unmarshallerSRs) {
                final Unmarshaller unmarshaller = this.bundleContext.getService(unmarshallerSR);
                try {
                    result = unmarshaller.unmarshal(string, clazz);
                    if (result != null) {
                        break;
                    }
                } finally {
                    this.bundleContext.ungetService(unmarshallerSR);
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to extract persisted configuration.", e);
        }
        if (result == null) {
            throw new KuraException(KuraErrorCode.DECODER_ERROR, "configuration");
        }
        return result;
    }

    protected String marshal(Object object) {
        String result = null;
        final ServiceReference<Marshaller>[] marshallerSRs = getJsonMarshallers();
        try {
            for (final ServiceReference<Marshaller> marshallerSR : marshallerSRs) {
                final Marshaller marshaller = this.bundleContext.getService(marshallerSR);
                try {
                    result = marshaller.marshal(object);
                    if (result != null) {
                        break;
                    }
                } finally {
                    this.bundleContext.ungetService(marshallerSR);
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to marshal configuration.", e);
        }
        return result;
    }

    private String getServicePidByKuraServicePid(
            Collection<ServiceReference<WireComponent>> wireComponentServiceReferences, String kuraServicePid) {
        return wireComponentServiceReferences.stream()
                .filter(ref -> kuraServicePid.equals(ref.getProperty(KURA_SERVICE_PID)))
                .map(ref -> (String) ref.getProperty(SERVICE_PID)).findAny().orElse(null);
    }

    protected String getServicePidByKuraServicePid(String kuraServicePid) {
        try {
            return getServicePidByKuraServicePid(this.bundleContext.getServiceReferences(WireComponent.class, null),
                    kuraServicePid);
        } catch (InvalidSyntaxException e) {
            return null;
        }
    }

    private WireGraphConfiguration loadWireGraphConfiguration(Map<String, Object> properties) throws KuraException {
        String jsonWireGraph = (String) properties.get(NEW_WIRE_GRAPH_PROPERTY);
        return unmarshal(jsonWireGraph, WireGraphConfiguration.class);
    }

    private static boolean isWireAsset(final WireComponentConfiguration config) {
        final Optional<String> factoryPid = getFactoryPid(config);

        return factoryPid.isPresent() && WIRE_ASSET_FACTORY_PID.contentEquals(factoryPid.get());
    }

    private static Map<String, Object> buildDefaultRenderingProperties() {
        final Map<String, Object> result = new HashMap<>(2);

        result.put("position.x", 0.0f);
        result.put("position.y", 0.0f);

        return Collections.unmodifiableMap(result);
    }

    private static List<WireComponentConfiguration> fillRenderingPropertyDefaults(
            List<WireComponentConfiguration> newWireComponentConfigurations) {

        return newWireComponentConfigurations.stream().map(config -> {
            final Optional<Map<String, Object>> originalRenderingProperties = Optional
                    .ofNullable(config.getProperties());

            final Map<String, Object> result = new HashMap<>(DEFAULT_RENDERING_PROPERTIES);

            if (originalRenderingProperties.isPresent()) {
                result.putAll(originalRenderingProperties.get());
            }

            return new WireComponentConfiguration(config.getConfiguration(), result);
        }).collect(Collectors.toList());
    }

    private static Optional<String> getFactoryPid(final WireComponentConfiguration config) {
        final ComponentConfiguration compConfig = config.getConfiguration();

        if (compConfig == null) {
            return Optional.empty();
        }

        final Map<String, Object> properties = compConfig.getConfigurationProperties();

        if (properties == null) {
            return Optional.empty();
        }

        final Object rawFactoryPid = properties.get(SERVICE_FACTORYPID);

        if (!(rawFactoryPid instanceof String)) {
            return Optional.empty();
        }

        return Optional.of((String) rawFactoryPid);
    }

    private static Filter getWireComponentConfigurationFilter() {
        try {
            return FrameworkUtil.createFilter(
                    "(|(objectClass=org.eclipse.kura.wire.WireComponent)(objectClass=org.eclipse.kura.wire.WireEmitter)"
                            + "(objectClass=org.eclipse.kura.wire.WireReceiver))");
        } catch (final Exception e) {
            logger.warn("failed to init wire component configuration filter", e);
            return null;
        }
    }

}

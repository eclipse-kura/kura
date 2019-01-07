/*******************************************************************************
 * Copyright (c) 2016, 2019 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Eurotech
 *  Amit Kumar Mondal
 *  Red Hat Inc
 *
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
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    /**
     * Binds the {@link WireAdmin} dependency
     *
     * @param wireAdmin
     *            the new {@link WireAdmin} service dependency
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
     *            the new {@link WireAdmin} instance
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

            this.currentConfiguration = loadWireGraphConfiguration(properties);

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

                try {
                    final Dictionary<?, ?> props = w.getProperties();
                    if (emitterServicePid.equals(props.get(WIREADMIN_PRODUCER_PID))
                            && receiverServicePid.equals(props.get(WIREADMIN_CONSUMER_PID))
                            && emitterPort == (Integer) props.get(Constants.WIRE_EMITTER_PORT_PROP_NAME.value())
                            && receiverPort == (Integer) props.get(Constants.WIRE_RECEIVER_PORT_PROP_NAME.value())) {
                        return true;
                    }
                } catch (Exception e) {
                    continue;
                }
            }
        }
        return false;

    }

    private void createConfiguration(final MultiportWireConfiguration conf) {
        requireNonNull(conf, "Wire Configuration cannot be null");

        String emitterPid = conf.getEmitterPid();
        String receiverPid = conf.getReceiverPid();
        try {
            final String emitterServicePid = getServicePidByKuraServicePid(emitterPid);
            final String receiverServicePid = getServicePidByKuraServicePid(receiverPid);
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
            logger.error("Error while creating wires...", e);
        }
    }

    public MultiportWireConfiguration createWireConfigurationInternal(final String emitterPid, final String receiverPid,
            final int emitterPort, final int receiverPort) throws KuraException {
        if (!emitterPid.equals(receiverPid)) {
            logger.info("Creating wire between {} and {}....", emitterPid, receiverPid);
            final String emitterServicePid = getServicePidByKuraServicePid(emitterPid);
            final String receiverServicePid = getServicePidByKuraServicePid(receiverPid);
            if (isNull(emitterServicePid) || isNull(receiverServicePid)) {
                throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR,
                        "Unable to retrieve Factory PIDs of one of the provided Wire Components");
            }
            MultiportWireConfiguration conf = new MultiportWireConfiguration(emitterPid, receiverPid, emitterPort,
                    receiverPort);
            WireGraphConfiguration wireGraphConfiguration = get();
            final ArrayList<MultiportWireConfiguration> wireConfigurations = new ArrayList<>(
                    wireGraphConfiguration.getWireConfigurations());
            wireConfigurations.add(conf);
            update(new WireGraphConfiguration(wireGraphConfiguration.getWireComponentConfigurations(),
                    wireConfigurations));
            logger.info("Creating wire between {} and {}....Done", emitterPid, receiverPid);
            return conf;
        }
        return null;
    }

    /**
     * Create the wires based on the provided wire configurations
     */
    synchronized void createWires() {

        for (final MultiportWireConfiguration wireConfig : this.currentConfiguration.getWireConfigurations()) {
            createConfiguration(wireConfig);
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
        logger.info("Closing Wire Component Service tracker...");
        if (this.wireComponentServiceTracker != null) {
            this.wireComponentServiceTracker.close();
            this.wireComponentServiceTracker = null;
        }
        logger.info("Closing Wire Component Service tracker...done");

        final WireGraphConfiguration currentGraphConfiguration = get();
        final List<WireComponentConfiguration> currentWireComponents = currentGraphConfiguration
                .getWireComponentConfigurations();

        List<ComponentConfiguration> componentConfigurations = new ArrayList<>();

        List<WireComponentConfiguration> newWireComponentConfigurations = newConfiguration
                .getWireComponentConfigurations();
        Set<MultiportWireConfiguration> newWires = new HashSet<>(newConfiguration.getWireConfigurations());

        // Evaluate deletable components
        Set<String> componentsToDelete = getComponentsToDelete(currentWireComponents, newWireComponentConfigurations);
        for (String componentToDelete : componentsToDelete) {
            this.configurationService.deleteFactoryConfiguration(componentToDelete, false);
        }

        deleteNoLongerExistingWires(newWires, componentsToDelete);

        // create new components
        List<WireComponentConfiguration> componentsToCreate = getComponentsToCreate(currentWireComponents,
                newWireComponentConfigurations);
        for (WireComponentConfiguration componentToCreate : componentsToCreate) {
            final ComponentConfiguration configToCreate = componentToCreate.getConfiguration();
            final Map<String, Object> wireComponentProps = componentToCreate.getProperties();
            final Map<String, Object> configurationProps = configToCreate.getConfigurationProperties();
            String factoryPid = (String) configurationProps.get(SERVICE_FACTORYPID);
            configurationProps.put(Constants.RECEIVER_PORT_COUNT_PROP_NAME.value(),
                    wireComponentProps.get("inputPortCount"));
            configurationProps.put(Constants.EMITTER_PORT_COUNT_PROP_NAME.value(),
                    wireComponentProps.get("outputPortCount"));
            this.configurationService.createFactoryConfiguration(factoryPid, configToCreate.getPid(),
                    configurationProps, false);
        }

        // Evaluate updatable components
        List<WireComponentConfiguration> componentsToUpdate = getComponentsToUpdate(newWireComponentConfigurations,
                componentsToCreate);
        for (WireComponentConfiguration componentToUpdate : componentsToUpdate) {
            componentConfigurations.add(componentToUpdate.getConfiguration());
        }

        String jsonConfig = marshal(newConfiguration);
        ComponentConfiguration wireGraphServiceComponentConfig = this.configurationService
                .getComponentConfiguration(CONF_PID);
        wireGraphServiceComponentConfig.getConfigurationProperties().put(NEW_WIRE_GRAPH_PROPERTY, jsonConfig);

        componentConfigurations.add(wireGraphServiceComponentConfig);

        this.configurationService.updateConfigurations(componentConfigurations, true);
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
        // TODO: test if it makes sense to fill the list with all the new and remove as far as a matching old is found;
        List<WireComponentConfiguration> componentsToCreate = new ArrayList<>();

        for (WireComponentConfiguration newWireComponentConfiguration : newWireComponentConfigurations) {
            ComponentConfiguration newComponentConfig = newWireComponentConfiguration.getConfiguration();
            String newPid = newComponentConfig.getPid();
            Map<String, Object> newProps = newComponentConfig.getConfigurationProperties();
            String newFactoryPid = null;
            if (newProps != null) {
                newFactoryPid = (String) newComponentConfig.getConfigurationProperties().get(SERVICE_FACTORYPID);
            }

            boolean found = false;
            for (WireComponentConfiguration oldWireComponentConfiguration : oldWireComponentConfigurations) {
                ComponentConfiguration oldComponentConfig = oldWireComponentConfiguration.getConfiguration();
                String oldPid = oldComponentConfig.getPid();
                Map<String, Object> oldProps = oldComponentConfig.getConfigurationProperties();
                String oldFactoryPid = null;
                if (oldProps != null) {
                    oldFactoryPid = (String) oldComponentConfig.getConfigurationProperties().get(SERVICE_FACTORYPID);
                }

                if (oldPid.equals(newPid) && (newFactoryPid == null || newFactoryPid.equals(oldFactoryPid))
                        && !WIRE_ASSET_FACTORY_PID.equals(newFactoryPid)) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                componentsToCreate.add(newWireComponentConfiguration);
            }
        }

        return componentsToCreate;
    }

    private Set<String> getComponentsToDelete(List<WireComponentConfiguration> oldWireComponentConfigurations,
            List<WireComponentConfiguration> newWireComponentConfigurations) {
        Set<String> componentsToDelete = new HashSet<>();

        for (WireComponentConfiguration newWireComponentConfiguration : newWireComponentConfigurations) {
            ComponentConfiguration newComponentConfig = newWireComponentConfiguration.getConfiguration();
            Map<String, Object> newProps = newComponentConfig.getConfigurationProperties();
            String newFactoryPid = null;
            if (newProps != null) {
                newFactoryPid = (String) newComponentConfig.getConfigurationProperties().get(SERVICE_FACTORYPID);
            }

            if (WIRE_ASSET_FACTORY_PID.equals(newFactoryPid)) {
                componentsToDelete.add(newComponentConfig.getPid());
            }
        }

        for (WireComponentConfiguration oldWireComponentConfiguration : oldWireComponentConfigurations) {
            ComponentConfiguration oldComponentConfig = oldWireComponentConfiguration.getConfiguration();
            String oldPid = oldComponentConfig.getPid();
            Map<String, Object> oldProps = oldComponentConfig.getConfigurationProperties();
            String oldFactoryPid = null;
            if (oldProps != null) {
                oldFactoryPid = (String) oldComponentConfig.getConfigurationProperties().get(SERVICE_FACTORYPID);
            }

            boolean found = false;
            for (WireComponentConfiguration newWireComponentConfiguration : newWireComponentConfigurations) {
                ComponentConfiguration newComponentConfig = newWireComponentConfiguration.getConfiguration();
                String newPid = newComponentConfig.getPid();
                Map<String, Object> newProps = newComponentConfig.getConfigurationProperties();
                String newFactoryPid = null;
                if (newProps != null) {
                    newFactoryPid = (String) newComponentConfig.getConfigurationProperties().get(SERVICE_FACTORYPID);
                }

                // TODO: check better the conditions for this test
                if (oldPid.equals(newPid) && (newFactoryPid == null || newFactoryPid.equals(oldFactoryPid)
                        || WIRE_ASSET_FACTORY_PID.equals(newFactoryPid))) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                componentsToDelete.add(oldComponentConfig.getPid());
            }
        }

        return componentsToDelete;

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
            ComponentConfiguration componentConfiguration = wireComponentConfiguration.getConfiguration();
            Map<String, Object> componentProperties = wireComponentConfiguration.getProperties();
            String componentPid = componentConfiguration.getPid();

            for (ComponentConfiguration configServiceComponentConfiguration : configServiceComponentConfigurations) {
                if (componentPid.equals(configServiceComponentConfiguration.getPid())) {
                    componentConfiguration = new ComponentConfigurationImpl(componentPid,
                            (Tocd) configServiceComponentConfiguration.getDefinition(),
                            configServiceComponentConfiguration.getConfigurationProperties());
                    break;
                }
            }
            completeWireComponentConfigurations
                    .add(new WireComponentConfiguration(componentConfiguration, componentProperties));
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
            throw new KuraException(KuraErrorCode.DECODER_ERROR);
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

    protected String getServicePidByKuraServicePid(String kuraServicePid) {
        try {
            return this.bundleContext.getServiceReferences(WireComponent.class, null).stream()
                    .filter(ref -> kuraServicePid.equals(ref.getProperty(KURA_SERVICE_PID)))
                    .map(ref -> (String) ref.getProperty(SERVICE_PID)).findAny().orElse(null);
        } catch (InvalidSyntaxException e) {
            return null;
        }
    }

    private WireGraphConfiguration loadWireGraphConfiguration(Map<String, Object> properties) throws KuraException {
        String jsonWireGraph = (String) properties.get(NEW_WIRE_GRAPH_PROPERTY);
        return unmarshal(jsonWireGraph, WireGraphConfiguration.class);
    }

    private static Filter getWireComponentConfigurationFilter() {
        try {
            return FrameworkUtil.createFilter(
                    "(|(objectClass=org.eclipse.kura.wire.WireComponent)(objectClass=org.eclipse.kura.wire.WireEmitter)(objectClass=org.eclipse.kura.wire.WireReceiver))");
        } catch (final Exception e) {
            logger.warn("failed to init wire component configuration filter", e);
            return null;
        }
    }
}

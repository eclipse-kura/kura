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
import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.core.configuration.ComponentConfigurationImpl;
import org.eclipse.kura.core.configuration.metatype.Tocd;
import org.eclipse.kura.localization.LocalizationAdapter;
import org.eclipse.kura.localization.resources.WireMessages;
import org.eclipse.kura.marshalling.Marshaller;
import org.eclipse.kura.marshalling.Unmarshaller;
import org.eclipse.kura.util.collection.CollectionUtil;
import org.eclipse.kura.util.service.ServiceUtil;
import org.eclipse.kura.wire.WireComponent;
import org.eclipse.kura.wire.WireConfiguration;
import org.eclipse.kura.wire.WireService;
import org.eclipse.kura.wire.graph.Constants;
import org.eclipse.kura.wire.graph.MultiportWireConfiguration;
import org.eclipse.kura.wire.graph.WireComponentConfiguration;
import org.eclipse.kura.wire.graph.WireGraphConfiguration;
import org.eclipse.kura.wire.graph.WireGraphService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.wireadmin.Wire;
import org.osgi.service.wireadmin.WireAdmin;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Class {@link WireServiceImpl} implements {@link WireService} and {@link WireGraphService}
 */
public class WireServiceImpl implements ConfigurableComponent, WireService, WireGraphService {

    private static final String WIRE_ASSET_FACTORY_PID = "org.eclipse.kura.wire.WireAsset";

    private static final String NEW_WIRE_GRAPH_PROPERTY = "WireGraph";

    /** Configuration PID Property */
    private static final String CONF_PID = "org.eclipse.kura.wire.WireService";

    private static final Logger logger = LoggerFactory.getLogger(WireServiceImpl.class);

    private static final WireMessages message = LocalizationAdapter.adapt(WireMessages.class);

    private static final ExecutorService processExecutor = Executors.newSingleThreadExecutor();

    /** The service component properties. */
    private Map<String, Object> properties;

    private volatile WireAdmin wireAdmin;

    private ServiceTracker<WireComponent, WireComponent> wireComponentServiceTracker;

    private ConfigurationService configurationService;
    private BundleContext bundleContext;

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
        logger.info(message.activatingWireService());
        this.bundleContext = componentContext.getBundleContext();
        this.properties = properties;

        WireComponentTrackerCustomizer wireComponentTrackerCustomizer = new WireComponentTrackerCustomizer(
                this.bundleContext, this);
        this.wireComponentServiceTracker = new ServiceTracker<>(this.bundleContext, WireComponent.class,
                wireComponentTrackerCustomizer);
        createWires();

        this.wireComponentServiceTracker.open();
        logger.info(message.activatingWireServiceDone());
    }

    public void updated(final Map<String, Object> properties) {
        logger.info(message.updatingWireService() + properties);
        this.properties = properties;

        try {
            deleteAllWires();
        } catch (InvalidSyntaxException e) {
            logger.warn("Error deleting all wires.");
        }

        createWires();

        this.wireComponentServiceTracker.open();
        logger.info(message.updatingWireServiceDone());
    }

    protected void deactivate(final ComponentContext componentContext) {
        logger.info(message.deactivatingWireService());
        this.bundleContext = null;
        this.wireComponentServiceTracker.close();

        try {
            deleteAllWires();
        } catch (InvalidSyntaxException e) {
            logger.warn("Error deleting all wires.");
        }

        processExecutor.shutdownNow();
        logger.info(message.deactivatingWireServiceDone());
    }

    private boolean checkWireExistence(final String emitterServicePid, final String receiverServicePid,
            final int emitterPort, final int receiverPort) throws InvalidSyntaxException {
        requireNonNull(emitterServicePid, message.emitterServicePidNonNull());
        requireNonNull(receiverServicePid, message.receiverServicePidNonNull());
        requireNonNull(emitterPort);
        requireNonNull(receiverPort);

        boolean found = false;
        final Wire[] wires = this.wireAdmin.getWires(null);
        if (nonNull(wires)) {
            for (final Wire w : wires) {

                try {
                    final Dictionary<?, ?> props = w.getProperties();
                    if (emitterServicePid.equals(props.get(WIREADMIN_PRODUCER_PID))
                            && receiverServicePid.equals(props.get(WIREADMIN_CONSUMER_PID))
                            && emitterPort == (Integer) props.get(Constants.WIRE_EMITTER_PORT_PROP_NAME)
                            && receiverPort == (Integer) props.get(Constants.WIRE_RECEIVER_PORT_PROP_NAME)) {
                        found = true;
                        break;
                    }
                } catch (Exception e) {
                    continue;
                }
            }
        }
        return found;

    }

    private void createConfiguration(final MultiportWireConfiguration conf) {
        requireNonNull(conf, message.wireConfigurationNonNull());

        String emitterPid = conf.getEmitterPid();
        String receiverPid = conf.getReceiverPid();
        try {
            final String emitterServicePid = getServicePidByKuraServicePid(emitterPid);
            final String receiverServicePid = getServicePidByKuraServicePid(receiverPid);
            final int emitterPort = conf.getEmitterPort();
            final int receiverPort = conf.getReceiverPort();
            if (nonNull(emitterServicePid) && nonNull(receiverServicePid) && nonNull(emitterPort)
                    && nonNull(receiverPort) && isNull(conf.getWire())) {
                final boolean found = checkWireExistence(emitterServicePid, receiverServicePid, emitterPort,
                        receiverPort);
                if (!found) {
                    logger.info(message.creatingWire(emitterPid, receiverPid));
                    final Dictionary<String, Object> properties = new Hashtable<>();
                    properties.put(Constants.WIRE_EMITTER_PORT_PROP_NAME, emitterPort);
                    properties.put(Constants.WIRE_RECEIVER_PORT_PROP_NAME, receiverPort);
                    final Wire wire = this.wireAdmin.createWire(emitterServicePid, receiverServicePid, properties);
                    conf.setWire(wire);
                    logger.info(message.creatingWiresDone());
                }
            }

        } catch (final InvalidSyntaxException e) {
            logger.error(message.errorCreatingWires(), e);
        }
    }

    public MultiportWireConfiguration createWireConfigurationInternal(final String emitterPid, final String receiverPid,
            final int emitterPort, final int receiverPort) throws KuraException {
        if (!emitterPid.equals(receiverPid)) {
            logger.info(message.creatingWire(emitterPid, receiverPid));
            final String emitterServicePid = getServicePidByKuraServicePid(emitterPid);
            final String receiverServicePid = getServicePidByKuraServicePid(receiverPid);
            if (isNull(emitterServicePid) || isNull(receiverServicePid)) {
                throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR, message.componentPidsNull());
            }
            MultiportWireConfiguration conf = new MultiportWireConfiguration(emitterPid, receiverPid, emitterPort,
                    receiverPort);
            WireGraphConfiguration wireGraphConfiguration = get();
            wireGraphConfiguration.getWireConfigurations().add(conf);
            update(wireGraphConfiguration);
            logger.info(message.creatingWireDone(emitterPid, receiverPid));
            return conf;
        }
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public WireConfiguration createWireConfiguration(final String emitterPid, final String receiverPid)
            throws KuraException {
        requireNonNull(emitterPid, message.emitterPidNonNull());
        requireNonNull(receiverPid, message.receiverPidNonNull());

        return createWireConfigurationInternal(emitterPid, receiverPid, 0, 0);
    }

    /**
     * Create the wires based on the provided wire configurations
     */
    synchronized void createWires() {

        Set<MultiportWireConfiguration> wireConfigurations = getWireConfigurationsInternal();
        final List<MultiportWireConfiguration> cloned = CollectionUtil.newArrayList();
        for (final MultiportWireConfiguration wc : wireConfigurations) {
            final MultiportWireConfiguration wireConf = new MultiportWireConfiguration(wc.getEmitterPid(),
                    wc.getReceiverPid(), wc.getEmitterPort(), wc.getReceiverPort());
            wireConf.setFilter(wc.getFilter());
            cloned.add(wireConf);
        }
        for (final MultiportWireConfiguration wireConfig : cloned) {
            createConfiguration(wireConfig);
        }
    }

    private synchronized void deleteAllWires() throws InvalidSyntaxException {

        final Wire[] wires = this.wireAdmin.getWires(null);

        if (wires == null) {
            return;
        }

        for (Wire w : wires) {
            this.wireAdmin.deleteWire(w);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void deleteWireConfiguration(WireConfiguration wireConfiguration) {
        requireNonNull(wireConfiguration, message.wireConfigurationNonNull());

        try {
            WireGraphConfiguration wireGraphConfiguration = get();
            List<MultiportWireConfiguration> wireConfigurations = wireGraphConfiguration.getWireConfigurations();

            if (wireConfiguration.getClass() == WireConfiguration.class) {
                wireConfiguration = new MultiportWireConfiguration(wireConfiguration.getEmitterPid(),
                        wireConfiguration.getReceiverPid(), 0, 0);
            }

            if (wireConfigurations.contains(wireConfiguration)) {
                final Wire[] wiresList = this.wireAdmin.getWires(null);
                for (final Wire wire : wiresList) {
                    final Dictionary<?, ?> props = wire.getProperties();
                    final String producerPid = props.get(WIREADMIN_PRODUCER_PID).toString();
                    final String consumerPid = props.get(WIREADMIN_CONSUMER_PID).toString();
                    if (wireConfiguration.getEmitterPid().equals(producerPid)
                            && wireConfiguration.getReceiverPid().equals(consumerPid)) {
                        this.wireAdmin.deleteWire(wire);
                        break;
                    }
                }
                wireConfigurations.remove(wireConfiguration);
                update(wireGraphConfiguration);
            }

        } catch (KuraException | InvalidSyntaxException e) {
            throw new IllegalArgumentException(e);
        }
        logger.info(message.removingWiresDone());
    }

    public Set<MultiportWireConfiguration> getWireConfigurationsInternal() {
        logger.debug(message.exectractingProp());
        final Set<MultiportWireConfiguration> set = CollectionUtil.newHashSet();
        Set<MultiportWireConfiguration> wireConfigurations = Collections.synchronizedSet(set);

        String jsonWireGraph = (String) this.properties.get(NEW_WIRE_GRAPH_PROPERTY);

        WireGraphConfiguration wireGraphConfiguration;
        try {
            wireGraphConfiguration = unmarshal(jsonWireGraph, WireGraphConfiguration.class);
            for (final MultiportWireConfiguration conf : wireGraphConfiguration.getWireConfigurations()) {
                wireConfigurations.add(conf);
            }
        } catch (KuraException e) {
        }
        logger.debug(message.exectractingPropDone());
        return wireConfigurations;
    }

    /** {@inheritDoc} */
    @Override
    public Set<WireConfiguration> getWireConfigurations() {
        return getWireConfigurationsInternal().stream().map(conf -> (MultiportWireConfiguration) conf)
                .collect(Collectors.toSet());
    }

    @Override
    public void update(WireGraphConfiguration graphConfiguration) throws KuraException {
        this.wireComponentServiceTracker.close();

        List<ComponentConfiguration> componentConfigurations = new ArrayList<>();

        WireGraphConfiguration oldGraphConfig = get();

        List<WireComponentConfiguration> oldWireComponentConfigurations = oldGraphConfig
                .getWireComponentConfigurations();
        List<WireComponentConfiguration> newWireComponentConfigurations = new ArrayList<>(
                graphConfiguration.getWireComponentConfigurations());

        // Evaluate deletable components
        List<ComponentConfiguration> componentsToDelete = getComponentsToDelete(oldWireComponentConfigurations,
                newWireComponentConfigurations);
        for (ComponentConfiguration componentToDelete : componentsToDelete) {
            this.configurationService.deleteFactoryConfiguration(componentToDelete.getPid(), false);
        }

        // create new components
        List<WireComponentConfiguration> componentsToCreate = getComponentsToCreate(oldWireComponentConfigurations,
                newWireComponentConfigurations);
        for (WireComponentConfiguration componentToCreate : componentsToCreate) {
            final ComponentConfiguration configToCreate = componentToCreate.getConfiguration();
            final Map<String, Object> wireComponentProps = componentToCreate.getProperties();
            final Map<String, Object> configurationProps = configToCreate.getConfigurationProperties();
            String factoryPid = (String) configurationProps.get(SERVICE_FACTORYPID);
            configurationProps.put(Constants.RECEIVER_PORT_COUNT_PROP_NAME, wireComponentProps.get("inputPortCount"));
            configurationProps.put(Constants.EMITTER_PORT_COUNT_PROP_NAME, wireComponentProps.get("outputPortCount"));
            this.configurationService.createFactoryConfiguration(factoryPid, configToCreate.getPid(),
                    configurationProps, false);
        }

        // Evaluate updatable components
        List<WireComponentConfiguration> componentsToUpdate = getComponentsToUpdate(newWireComponentConfigurations,
                componentsToCreate);
        for (WireComponentConfiguration componentToUpdate : componentsToUpdate) {
            componentConfigurations.add(componentToUpdate.getConfiguration());
        }

        String jsonConfig = marshal(graphConfiguration);
        ComponentConfiguration wireServiceComponentConfig = this.configurationService
                .getComponentConfiguration(CONF_PID);
        wireServiceComponentConfig.getConfigurationProperties().put(NEW_WIRE_GRAPH_PROPERTY, jsonConfig);

        componentConfigurations.add(wireServiceComponentConfig);

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

    private List<ComponentConfiguration> getComponentsToDelete(
            List<WireComponentConfiguration> oldWireComponentConfigurations,
            List<WireComponentConfiguration> newWireComponentConfigurations) {
        List<ComponentConfiguration> componentsToDelete = new ArrayList<>();

        for (WireComponentConfiguration newWireComponentConfiguration : newWireComponentConfigurations) {
            ComponentConfiguration newComponentConfig = newWireComponentConfiguration.getConfiguration();
            Map<String, Object> newProps = newComponentConfig.getConfigurationProperties();
            String newFactoryPid = null;
            if (newProps != null) {
                newFactoryPid = (String) newComponentConfig.getConfigurationProperties().get(SERVICE_FACTORYPID);
            }

            if (WIRE_ASSET_FACTORY_PID.equals(newFactoryPid)) {
                componentsToDelete.add(newComponentConfig);
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
                componentsToDelete.add(oldComponentConfig);
            }
        }

        return componentsToDelete;
    }

    @Override
    public void delete() throws KuraException {
        String oldJson = (String) this.properties.get(NEW_WIRE_GRAPH_PROPERTY);
        WireGraphConfiguration oldGraphConfig = unmarshal(oldJson, WireGraphConfiguration.class);

        List<WireComponentConfiguration> oldWireComponentConfigurations = oldGraphConfig
                .getWireComponentConfigurations();
        List<WireComponentConfiguration> newWireComponentConfigurations = new ArrayList<>();

        // Evaluate deletable components
        List<ComponentConfiguration> componentsToDelete = getComponentsToDelete(oldWireComponentConfigurations,
                newWireComponentConfigurations);
        for (ComponentConfiguration componentToDelete : componentsToDelete) {
            this.configurationService.deleteFactoryConfiguration(componentToDelete.getPid(), false);
        }

        WireGraphConfiguration newWireGraphConfiguration = new WireGraphConfiguration(new ArrayList<>(),
                new ArrayList<>());

        String jsonConfig = marshal(newWireGraphConfiguration);
        ComponentConfiguration wireServiceComponentConfig = this.configurationService
                .getComponentConfiguration(CONF_PID);
        wireServiceComponentConfig.getConfigurationProperties().put(NEW_WIRE_GRAPH_PROPERTY, jsonConfig);

        this.configurationService.updateConfiguration(CONF_PID, wireServiceComponentConfig.getConfigurationProperties(),
                true);
    }

    @Override
    public WireGraphConfiguration get() throws KuraException {
        String jsonString = (String) this.properties.get(NEW_WIRE_GRAPH_PROPERTY);

        List<ComponentConfiguration> configServiceComponentConfigurations = this.configurationService
                .getComponentConfigurations();

        WireGraphConfiguration wireGraphConfiguration = unmarshal(jsonString, WireGraphConfiguration.class);

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
        String filterString = String.format("(&(kura.service.pid=%s))",
                "org.eclipse.kura.json.marshaller.unmarshaller.provider");
        return ServiceUtil.getServiceReferences(this.bundleContext, Marshaller.class, filterString);
    }

    private ServiceReference<Unmarshaller>[] getJsonUnmarshallers() {
        String filterString = String.format("(&(kura.service.pid=%s))",
                "org.eclipse.kura.json.marshaller.unmarshaller.provider");
        return ServiceUtil.getServiceReferences(this.bundleContext, Unmarshaller.class, filterString);
    }

    private void ungetServiceReferences(final ServiceReference<?>[] refs) {
        ServiceUtil.ungetServiceReferences(this.bundleContext, refs);
    }

    protected <T> T unmarshal(String string, Class<T> clazz) throws KuraException {
        T result = null;
        ServiceReference<Unmarshaller>[] unmarshallerSRs = getJsonUnmarshallers();
        try {
            for (final ServiceReference<Unmarshaller> unmarshallerSR : unmarshallerSRs) {
                Unmarshaller unmarshaller = this.bundleContext.getService(unmarshallerSR);
                result = unmarshaller.unmarshal(string, clazz);
                if (result != null) {
                    break;
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to extract persisted configuration.");
        } finally {
            ungetServiceReferences(unmarshallerSRs);
        }
        if (result == null) {
            throw new KuraException(KuraErrorCode.DECODER_ERROR);
        }
        return result;
    }

    protected String marshal(Object object) {
        String result = null;
        ServiceReference<Marshaller>[] marshallerSRs = getJsonMarshallers();
        try {
            for (final ServiceReference<Marshaller> marshallerSR : marshallerSRs) {
                Marshaller marshaller = this.bundleContext.getService(marshallerSR);
                result = marshaller.marshal(object);
                if (result != null) {
                    break;
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to marshal configuration.");
        } finally {
            ungetServiceReferences(marshallerSRs);
        }
        return result;
    }

    protected String getServicePidByKuraServicePid(String kuraServicePid) {
        try {
            return bundleContext.getServiceReferences(WireComponent.class, null).stream()
                    .filter(ref -> kuraServicePid.equals(ref.getProperty(KURA_SERVICE_PID)))
                    .map(ref -> (String) ref.getProperty(SERVICE_PID)).findAny().orElse(null);
        } catch (InvalidSyntaxException e) {
            return null;
        }
    }

}

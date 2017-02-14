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
import static org.eclipse.kura.internal.wire.WireServiceOptions.SEPARATOR;
import static org.osgi.service.wireadmin.WireConstants.WIREADMIN_CONSUMER_PID;
import static org.osgi.service.wireadmin.WireConstants.WIREADMIN_PRODUCER_PID;

import java.util.Collections;
import java.util.Dictionary;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.SelfConfiguringComponent;
import org.eclipse.kura.core.configuration.ComponentConfigurationImpl;
import org.eclipse.kura.core.configuration.metatype.Tocd;
import org.eclipse.kura.localization.LocalizationAdapter;
import org.eclipse.kura.localization.resources.WireMessages;
import org.eclipse.kura.util.collection.CollectionUtil;
import org.eclipse.kura.wire.WireComponent;
import org.eclipse.kura.wire.WireConfiguration;
import org.eclipse.kura.wire.WireHelperService;
import org.eclipse.kura.wire.WireService;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.wireadmin.Wire;
import org.osgi.service.wireadmin.WireAdmin;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Class {@link WireServiceImpl} implements {@link WireService}
 */
public final class WireServiceImpl implements SelfConfiguringComponent, WireService {

    /** Configuration PID Property */
    private static final String CONF_PID = "org.eclipse.kura.wire.WireService";

    private static final Logger logger = LoggerFactory.getLogger(WireServiceImpl.class);

    private static final WireMessages message = LocalizationAdapter.adapt(WireMessages.class);

    /** The service component properties. */
    private Map<String, Object> properties;

    private volatile WireAdmin wireAdmin;

    private ServiceTracker<WireComponent, WireComponent> wireComponentServiceTracker;

    private WireComponentTrackerCustomizer wireComponentTrackerCustomizer;

    private final Set<WireConfiguration> wireConfigs;

    private volatile WireHelperService wireHelperService;

    private WireServiceOptions wireServiceOptions;

    public WireServiceImpl() {
        final Set<WireConfiguration> set = CollectionUtil.newHashSet();
        this.wireConfigs = Collections.synchronizedSet(set);
    }

    /**
     * OSGi service component callback while activation
     *
     * @param componentContext
     *            the component context
     * @param properties
     *            the service properties
     */
    protected void activate(final ComponentContext componentContext, final Map<String, Object> properties) {
        logger.debug(message.activatingWireService());
        extractProperties(properties);
        try {
            this.wireComponentTrackerCustomizer = new WireComponentTrackerCustomizer(
                    componentContext.getBundleContext(), this);
            this.wireComponentServiceTracker = new ServiceTracker<>(componentContext.getBundleContext(),
                    WireComponent.class, this.wireComponentTrackerCustomizer);
            this.wireComponentServiceTracker.open();
        } catch (final InvalidSyntaxException exception) {
            logger.error(message.error(), exception);
        }
        createWires();
        logger.debug(message.activatingWireServiceDone());
    }

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
     * Binds the {@link WireHelperService}.
     *
     * @param wireHelperService
     *            the new {@link WireHelperService}
     */
    public void bindWireHelperService(final WireHelperService wireHelperService) {
        if (isNull(this.wireHelperService)) {
            this.wireHelperService = wireHelperService;
        }
    }

    /**
     * Checks for existence of {@link WireAdmin}'s {@link Wire} instance between the
     * provided Emitter Service PID and Receiver Service PID
     *
     * @param emitterServicePid
     *            Wire Emitter Service PID {@code service.pid}
     * @param receiverServicePid
     *            Wire Receiver Service PID {@code service.pid}
     * @return true if exists, otherwise false
     * @throws InvalidSyntaxException
     *             If the {@link Wire} filter has an invalid LDAP syntax ({@code null} accepted}
     * @throws NullPointerException
     *             if any of the provided arguments is null
     */
    private boolean checkWireExistence(final String emitterServicePid, final String receiverServicePid)
            throws InvalidSyntaxException {
        requireNonNull(emitterServicePid, message.emitterServicePidNonNull());
        requireNonNull(receiverServicePid, message.receiverServicePidNonNull());

        boolean found = false;
        final Wire[] wires = this.wireAdmin.getWires(null);
        if (nonNull(wires)) {
            for (final Wire w : wires) {
                final Dictionary<?, ?> props = w.getProperties();
                if (props.get(WIREADMIN_PRODUCER_PID).equals(emitterServicePid)
                        && props.get(WIREADMIN_CONSUMER_PID).equals(receiverServicePid)) {
                    found = true;
                    break;
                }
            }
        }
        return found;
    }

    /**
     * Creates the {@link WireAdmin}'s {@link Wire} instance between the provided
     * Emitter PID and Receiver PID and sets the created {@link Wire} instance to the
     * provided {@link WireConfiguration} instance
     *
     * @param conf
     *            the {@link WireConfiguration} instance
     * @param emitterPid
     *            Wire Emitter PID
     * @param receiverPid
     *            Wire Receiver PID
     * @throws NullPointerException
     *             if any of the arguments is null
     */
    private void createConfiguration(final WireConfiguration conf, final String emitterPid, final String receiverPid) {
        requireNonNull(conf, message.wireConfigurationNonNull());
        requireNonNull(emitterPid, message.emitterPidNonNull());
        requireNonNull(receiverPid, message.receiverPidNonNull());

        final String emitterServicePid = this.wireHelperService.getServicePid(emitterPid);
        final String receiverServicePid = this.wireHelperService.getServicePid(receiverPid);
        if (nonNull(emitterServicePid) && nonNull(receiverServicePid) && isNull(conf.getWire())) {
            try {
                final boolean found = checkWireExistence(emitterServicePid, receiverServicePid);
                if (!found) {
                    final Wire wire = this.wireAdmin.createWire(emitterServicePid, receiverServicePid, null);
                    conf.setWire(wire);
                }
            } catch (final InvalidSyntaxException e) {
                logger.error(message.errorCreatingWires(), e);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public WireConfiguration createWireConfiguration(final String emitterPid, final String receiverPid)
            throws KuraException {
        requireNonNull(emitterPid, message.emitterPidNonNull());
        requireNonNull(receiverPid, message.receiverPidNonNull());

        logger.info(message.creatingWire(emitterPid, receiverPid));
        WireConfiguration conf = null;
        if (!emitterPid.equals(receiverPid)) {
            final String emitterServicePid = this.wireHelperService.getServicePid(emitterPid);
            final String receiverServicePid = this.wireHelperService.getServicePid(receiverPid);
            if (isNull(emitterServicePid) || isNull(receiverServicePid)) {
                throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR, message.componentPidsNull());
            }
            if (!(this.wireHelperService.isEmitter(emitterPid) || this.wireHelperService.isReceiver(receiverPid))) {
                throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR, message.componentsNotApplicable());
            }
            conf = new WireConfiguration(emitterPid, receiverPid);
            final Wire wire = this.wireAdmin.createWire(emitterServicePid, receiverServicePid, null);
            if (nonNull(wire)) {
                conf.setWire(wire);
                this.wireConfigs.add(conf);
            }
            logger.info(message.creatingWireDone(emitterPid, receiverPid));
        }
        return conf;
    }

    /**
     * Create the wires based on the provided wire configurations
     */
    synchronized void createWires() {
        logger.debug(message.creatingWires());
        final List<WireConfiguration> cloned = CollectionUtil.newArrayList();
        for (final WireConfiguration wc : this.wireConfigs) {
            final WireConfiguration wireConf = new WireConfiguration(wc.getEmitterPid(), wc.getReceiverPid());
            wireConf.setFilter(wc.getFilter());
            cloned.add(wireConf);
        }
        for (final WireConfiguration wireConfig : cloned) {
            final String emitterPid = wireConfig.getEmitterPid();
            final String receiverPid = wireConfig.getReceiverPid();

            final boolean emitterFound = this.wireComponentTrackerCustomizer.getWireEmitters().contains(emitterPid);
            final boolean receiverFound = this.wireComponentTrackerCustomizer.getWireReceivers().contains(receiverPid);

            if (emitterFound && receiverFound) {
                logger.info(message.creatingWire(emitterPid, receiverPid));
                createConfiguration(wireConfig, emitterPid, receiverPid);
                logger.info(message.creatingWiresDone());
            }
        }
    }

    /**
     * OSGi service component callback while deactivation
     *
     * @param componentContext
     *            the component context
     */
    protected void deactivate(final ComponentContext componentContext) {
        logger.debug(message.deactivatingWireService());
        this.wireComponentServiceTracker.close();
        for (final WireConfiguration wireConfiguration : this.wireConfigs) {
            deleteWireConfiguration(wireConfiguration);
        }
        logger.debug(message.deactivatingWireServiceDone());
    }

    /**
     * Deletes the provided {@link WireConfiguration} and the provided {@link Wire} instance if these exists between
     * the provided Wire Emitter PID and Wire Receiver PID
     *
     * @param wireConfiguration
     *            the {@link WireConfiguration} instance
     * @param wire
     *            the {@link Wire} instance
     * @param producerPid
     *            Wire Emitter PID
     * @param consumerPid
     *            Wire Receiver PID
     * @throws NullPointerException
     *             if any of the arguments is null
     */
    private void deleteConfiguration(final WireConfiguration wireConfiguration, final Wire wire,
            final String producerPid, final String consumerPid) {
        requireNonNull(wireConfiguration, message.wireConfigurationNonNull());
        requireNonNull(wire, message.wireNonNull());
        requireNonNull(producerPid, message.emitterPidNonNull());
        requireNonNull(consumerPid, message.receiverPidNonNull());

        final String emitterServicePid = this.wireHelperService.getServicePid(wireConfiguration.getEmitterPid());
        final String receiverServicePid = this.wireHelperService.getServicePid(wireConfiguration.getReceiverPid());

        if (nonNull(emitterServicePid) && nonNull(receiverServicePid) && producerPid.equals(emitterServicePid)
                && consumerPid.equals(receiverServicePid)) {
            // just to make sure the deletion does not result in ConcurrentModification exception
            synchronized (this.wireConfigs) {
                for (final Iterator<WireConfiguration> iter = this.wireConfigs.iterator(); iter.hasNext();) {
                    final WireConfiguration configuration = iter.next();
                    if (configuration.equals(wireConfiguration)) {
                        iter.remove();
                        this.wireAdmin.deleteWire(wire);
                        break;
                    }
                }
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void deleteWireConfiguration(final WireConfiguration wireConfiguration) {
        requireNonNull(wireConfiguration, message.wireConfigurationNonNull());
        logger.info(message.removingWires());
        try {
            final Wire[] wiresList = this.wireAdmin.getWires(null);
            if (nonNull(wiresList)) {
                for (final Wire wire : wiresList) {
                    final Dictionary<?, ?> props = wire.getProperties();
                    final String producerPid = props.get(WIREADMIN_PRODUCER_PID).toString();
                    final String consumerPid = props.get(WIREADMIN_CONSUMER_PID).toString();

                    deleteConfiguration(wireConfiguration, wire, producerPid, consumerPid);
                }
            }
        } catch (final InvalidSyntaxException e) {
            throw new IllegalArgumentException(e);
        }
        logger.info(message.removingWiresDone());
    }

    /**
     * Retrieves the properties required for Wire Service
     *
     * @param properties
     *            the provided properties to parse
     * @throws NullPointerException
     *             if argument is null
     */
    private void extractProperties(final Map<String, Object> properties) {
        requireNonNull(properties, message.propertiesNonNull());
        logger.debug(message.exectractingProp());
        // clear the configurations first
        if (this.wireServiceOptions != null) {
            final List<WireConfiguration> list = this.wireServiceOptions.getWireConfigurations();
            list.clear();
        }
        this.wireServiceOptions = WireServiceOptions.getInstance(properties);
        this.properties = properties;
        for (final WireConfiguration conf : this.wireServiceOptions.getWireConfigurations()) {
            this.wireConfigs.add(conf);
        }
        logger.debug(message.exectractingPropDone());
    }

    /** {@inheritDoc} */
    @Override
    public ComponentConfiguration getConfiguration() throws KuraException {
        final Tocd wiresOCD = new Tocd();
        wiresOCD.setId(CONF_PID);
        wiresOCD.setName(message.name());
        wiresOCD.setDescription(message.description());

        final Map<String, Object> props = CollectionUtil.newHashMap();
        for (final Map.Entry<String, Object> entry : this.properties.entrySet()) {
            final String key = entry.getKey();
            final boolean isNotWireConfigurationProperty = !(key.endsWith(message.emitter())
                    || key.endsWith(message.receiver()) || key.endsWith(message.filter()));
            if (isNotWireConfigurationProperty) {
                props.put(key, entry.getValue());
            }
        }
        int i = 0;
        for (final WireConfiguration wireConfiguration : this.wireConfigs) {
            final String emitterKey = String.valueOf(++i) + SEPARATOR + message.emitter();
            final String receiverKey = String.valueOf(i) + SEPARATOR + message.receiver();
            final String filterKey = String.valueOf(i) + SEPARATOR + message.filter();
            props.put(emitterKey, wireConfiguration.getEmitterPid());
            props.put(receiverKey, wireConfiguration.getReceiverPid());
            props.put(filterKey, wireConfiguration.getFilter());
        }
        return new ComponentConfigurationImpl(CONF_PID, wiresOCD, props);
    }

    /** {@inheritDoc} */
    @Override
    public Set<WireConfiguration> getWireConfigurations() {
        return this.wireConfigs;
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

    /**
     * Unbinds the {@link WireHelperService} Service.
     *
     * @param wireHelperService
     *            the new {@link WireHelperService} Service
     */
    public void unbindWireHelperService(final WireHelperService wireHelperService) {
        if (this.wireHelperService == wireHelperService) {
            this.wireHelperService = null;
        }
    }

    /**
     * OSGi service component callback while updating
     *
     * @param properties
     *            the configuration properties
     */
    public void updated(final Map<String, Object> properties) {
        logger.debug(message.updatingWireService() + properties);
        extractProperties(properties);
        createWires();
        logger.debug(message.updatingWireServiceDone());
    }
}

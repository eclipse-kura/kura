/**
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Eurotech
 *   Amit Kumar Mondal (admin@amitinside.com)
 */
package org.eclipse.kura.internal.wire;

import static org.eclipse.kura.Preconditions.checkNull;
import static org.osgi.service.wireadmin.WireConstants.WIREADMIN_CONSUMER_PID;
import static org.osgi.service.wireadmin.WireConstants.WIREADMIN_PRODUCER_PID;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.KuraRuntimeException;
import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.configuration.SelfConfiguringComponent;
import org.eclipse.kura.core.configuration.ComponentConfigurationImpl;
import org.eclipse.kura.core.configuration.metatype.Tocd;
import org.eclipse.kura.localization.LocalizationAdapter;
import org.eclipse.kura.localization.resources.WireMessages;
import org.eclipse.kura.util.base.ThrowableUtil;
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
 * The Class WireServiceImpl implements {@link WireService}
 */
public final class WireServiceImpl implements SelfConfiguringComponent, WireService {

	/** Configuration PID Property */
	private static final String CONF_PID = "org.eclipse.kura.wire.WireService";

	/** The Logger instance. */
	private static final Logger s_logger = LoggerFactory.getLogger(WireServiceImpl.class);

	/** Localization Resource */
	private static final WireMessages s_message = LocalizationAdapter.adapt(WireMessages.class);

	/** The Configuration Service instance. */
	private volatile ConfigurationService m_configService;

	/** The Service Component Context. */
	private ComponentContext m_ctx;

	/** The Wire Service options. */
	private WireServiceOptions m_options;

	/** The service component properties. */
	private Map<String, Object> m_properties;

	/** Wire Component Tracker. */
	private ServiceTracker<WireComponent, WireComponent> m_serviceTracker;

	/** The service tracker to track all wire components */
	private WireComponentTrackerCustomizer m_trackerCustomizer;

	/** The Wire Admin dependency. */
	private volatile WireAdmin m_wireAdmin;

	/** The list of wire configurations */
	private final List<WireConfiguration> m_wireConfigs;

	/** The Wire Helper Service. */
	private volatile WireHelperService m_wireHelperService;

	/** Constructor */
	public WireServiceImpl() {
		final List<WireConfiguration> list = CollectionUtil.newArrayList();
		this.m_wireConfigs = Collections.synchronizedList(list);
	}

	/**
	 * OSGi service component callback while activation
	 *
	 * @param componentContext
	 *            the component context
	 * @param properties
	 *            the service properties
	 */
	protected synchronized void activate(final ComponentContext componentContext,
			final Map<String, Object> properties) {
		s_logger.debug(s_message.activatingWireService());
		this.m_ctx = componentContext;
		try {
			this.extractProperties(properties);
			this.m_trackerCustomizer = new WireComponentTrackerCustomizer(this.m_ctx.getBundleContext(), this);
			this.m_serviceTracker = new ServiceTracker<WireComponent, WireComponent>(
					componentContext.getBundleContext(), WireComponent.class.getName(), this.m_trackerCustomizer);
			this.m_serviceTracker.open();
			this.createWires();
		} catch (final Exception exception) {
			s_logger.error(ThrowableUtil.stackTraceAsString(exception));
		}
		s_logger.debug(s_message.activatingWireServiceDone());
	}

	/**
	 * Binds the configuration service.
	 *
	 * @param configService
	 *            the new configuration service
	 */
	public synchronized void bindConfigurationService(final ConfigurationService configService) {
		if (this.m_configService == null) {
			this.m_configService = configService;
		}
	}

	/**
	 * Binds the wire admin dependency
	 *
	 * @param wireAdmin
	 *            the new wire admin service dependency
	 */
	public synchronized void bindWireAdmin(final WireAdmin wireAdmin) {
		if (this.m_wireAdmin == null) {
			this.m_wireAdmin = wireAdmin;
		}
	}

	/**
	 * Binds the Wire Helper Service.
	 *
	 * @param wireHelperService
	 *            the new Wire Helper Service
	 */
	public synchronized void bindWireHelperService(final WireHelperService wireHelperService) {
		if (this.m_wireHelperService == null) {
			this.m_wireHelperService = wireHelperService;
		}
	}

	/** {@inheritDoc} */
	@Override
	public WireConfiguration createWireConfiguration(final String emitterPid, final String receiverPid)
			throws KuraException {
		checkNull(emitterPid, s_message.emitterPidNonNull());
		checkNull(receiverPid, s_message.receiverPidNonNull());

		s_logger.info(s_message.creatingWire(emitterPid, receiverPid));
		WireConfiguration conf = null;
		if (!emitterPid.equals(receiverPid)) {
			final String emitterServicePid = this.m_wireHelperService.getServicePid(emitterPid);
			final String receiverServicePid = this.m_wireHelperService.getServicePid(receiverPid);
			if ((emitterServicePid == null) || (receiverServicePid == null)) {
				throw new KuraException(KuraErrorCode.INTERNAL_ERROR, s_message.componentPidsNull());
			}
			conf = this.m_wireHelperService.newWireConfiguration(emitterPid, receiverPid, null);
			final Wire wire = this.m_wireAdmin.createWire(emitterServicePid, receiverServicePid, null);
			if (wire != null) {
				conf.setWire(wire);
				this.m_wireConfigs.add(conf);
			}
			s_logger.info(s_message.creatingWireDone(emitterPid, receiverPid));
		}
		return conf;
	}

	/**
	 * Create the wires based on the provided wire configurations
	 *
	 * @throws KuraException
	 *             if there doesn't exist any Wire Component having provided
	 *             emitter PID or any Wire Component having provided receiver
	 *             PID
	 */
	synchronized void createWires() throws KuraException {
		s_logger.debug(s_message.creatingWires());
		final List<WireConfiguration> cloned = CollectionUtil.newArrayList();
		for (final WireConfiguration wc : this.m_wireConfigs) {
			cloned.add(this.m_wireHelperService.newWireConfiguration(wc.getEmitterPid(), wc.getReceiverPid(),
					wc.getFilter()));
		}
		for (final WireConfiguration conf : cloned) {
			final String emitterPid = conf.getEmitterPid();
			final String receiverPid = conf.getReceiverPid();
			final boolean emitterFound = this.m_trackerCustomizer.getWireEmitters().contains(emitterPid);
			final boolean receiverFound = this.m_trackerCustomizer.getWireReceivers().contains(receiverPid);
			if (emitterFound && receiverFound) {
				s_logger.info(s_message.creatingWire(emitterPid, receiverPid));
				final String emitterServicePid = this.m_wireHelperService.getServicePid(emitterPid);
				final String receiverServicePid = this.m_wireHelperService.getServicePid(receiverPid);
				if ((emitterServicePid != null) && (receiverServicePid != null)) {
					final Wire wire = this.m_wireAdmin.createWire(emitterServicePid, receiverServicePid, null);
					conf.setWire(wire);
				}
			}
		}
		s_logger.debug(s_message.creatingWiresDone());
	}

	/**
	 * OSGi service component callback while deactivation
	 *
	 * @param componentContext
	 *            the component context
	 */
	protected synchronized void deactivate(final ComponentContext componentContext) {
		s_logger.debug(s_message.deactivatingWireService());
		this.m_serviceTracker.close();
		for (final WireConfiguration wireConfiguration : this.m_wireConfigs) {
			this.deleteWireConfiguration(wireConfiguration);
		}
		s_logger.debug(s_message.deactivatingWireServiceDone());
	}

	/** {@inheritDoc} */
	@Override
	public void deleteWireConfiguration(final WireConfiguration wireConfiguration) {
		checkNull(wireConfiguration, s_message.wireConfigurationNonNull());
		s_logger.info(s_message.removingWires());
		try {
			final Wire[] wiresList = this.m_wireAdmin.getWires(null);
			if (wiresList != null) {
				for (final Wire wire : wiresList) {
					final String producerPid = wire.getProperties().get(WIREADMIN_PRODUCER_PID).toString();
					final String consumerPid = wire.getProperties().get(WIREADMIN_CONSUMER_PID).toString();
					final String emitterFactoryPid = this.m_wireHelperService
							.getServicePid(wireConfiguration.getEmitterPid());
					final String receiverFactoryPid = this.m_wireHelperService
							.getServicePid(wireConfiguration.getReceiverPid());
					if ((emitterFactoryPid != null) && (receiverFactoryPid != null)
							&& producerPid.equals(emitterFactoryPid) && consumerPid.equals(receiverFactoryPid)) {
						// just to make sure the deletion does not incur
						// ConcurrentModification exception
						synchronized (this.m_wireConfigs) {
							for (final Iterator<WireConfiguration> iter = this.m_wireConfigs.listIterator(); iter
									.hasNext();) {
								final WireConfiguration configuration = iter.next();
								if (configuration.equals(wireConfiguration)) {
									iter.remove();
									this.m_wireAdmin.deleteWire(wire);
									break;
								}
							}
						}
					}
				}
			}
		} catch (final InvalidSyntaxException e) {
			throw new KuraRuntimeException(KuraErrorCode.INTERNAL_ERROR, ThrowableUtil.stackTraceAsString(e));
		}
		s_logger.info(s_message.removingWiresDone());
	}

	/**
	 * Retrieves the properties required for Wire Service
	 *
	 * @param properties
	 *            the provided properties to parse
	 * @throws KuraRuntimeException
	 *             if argument is null
	 */
	private void extractProperties(final Map<String, Object> properties) {
		checkNull(properties, s_message.propertiesNonNull());
		s_logger.debug(s_message.exectractingProp());
		// clear the configurations first
		if (this.m_options != null) {
			final List<WireConfiguration> list = this.m_options.getWireConfigurations();
			list.clear();
		}
		this.m_options = WireServiceOptions.getInstance(properties, this.m_wireHelperService);
		this.m_properties = properties;

		for (final WireConfiguration conf : this.m_options.getWireConfigurations()) {
			this.m_wireConfigs.add(conf);
		}
		s_logger.debug(s_message.exectractingPropDone());
	}

	/** {@inheritDoc} */
	@Override
	public ComponentConfiguration getConfiguration() throws KuraException {
		final Tocd wiresOCD = new Tocd();
		wiresOCD.setId(CONF_PID);
		wiresOCD.setName(s_message.name());
		wiresOCD.setDescription(s_message.description());

		final Map<String, Object> props = CollectionUtil.newHashMap();
		for (final Map.Entry<String, Object> entry : this.m_properties.entrySet()) {
			props.put(entry.getKey(), entry.getValue());
		}
		int i = 1;
		for (final WireConfiguration wireConfiguration : this.m_wireConfigs) {
			final String emitterKey = String.valueOf(i++) + ".emitter";
			final String receiverKey = String.valueOf(i++) + ".receiver";
			final String filterKey = String.valueOf(i++) + ".filter";
			props.put(emitterKey, wireConfiguration.getEmitterPid());
			props.put(receiverKey, wireConfiguration.getReceiverPid());
			props.put(filterKey, wireConfiguration.getFilter());
		}
		return new ComponentConfigurationImpl(CONF_PID, wiresOCD, props);
	}

	/** {@inheritDoc} */
	@Override
	public List<WireConfiguration> getWireConfigurations() {
		return this.m_wireConfigs;
	}

	/**
	 * Unbinds configuration service dependency
	 *
	 * @param configService
	 *            the configuration service
	 */
	public synchronized void unbindConfigurationService(final ConfigurationService configService) {
		if (this.m_configService == configService) {
			this.m_configService = null;
		}
	}

	/**
	 * Unbinds wire admin dependency
	 *
	 * @param wireAdmin
	 *            the wire admin
	 */
	public synchronized void unbindWireAdmin(final WireAdmin wireAdmin) {
		if (this.m_wireAdmin == wireAdmin) {
			this.m_wireAdmin = null;
		}
	}

	/**
	 * Unbinds the Wire Helper Service.
	 *
	 * @param wireHelperService
	 *            the new Wire Helper Service
	 */
	public synchronized void unbindWireHelperService(final WireHelperService wireHelperService) {
		if (this.m_wireHelperService == wireHelperService) {
			this.m_wireHelperService = null;
		}
	}

	/**
	 * OSGi service component callback while updating
	 *
	 * @param properties
	 *            the properties
	 */
	public synchronized void updated(final Map<String, Object> properties) {
		s_logger.debug(s_message.updatingWireService() + properties);
		this.extractProperties(properties);
		try {
			this.createWires();
		} catch (final KuraException e) {
			s_logger.error(ThrowableUtil.stackTraceAsString(e));
		}
		s_logger.debug(s_message.updatingWireServiceDone());
	}

}

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
package org.eclipse.kura.wire.internal;

import static org.eclipse.kura.Preconditions.checkNull;
import static org.osgi.service.wireadmin.WireConstants.WIREADMIN_CONSUMER_PID;
import static org.osgi.service.wireadmin.WireConstants.WIREADMIN_PRODUCER_PID;

import java.util.List;
import java.util.Map;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.KuraRuntimeException;
import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.configuration.SelfConfiguringComponent;
import org.eclipse.kura.core.configuration.ComponentConfigurationImpl;
import org.eclipse.kura.core.configuration.metatype.Tocd;
import org.eclipse.kura.localization.LocalizationAdapter;
import org.eclipse.kura.localization.WireMessages;
import org.eclipse.kura.wire.WireComponent;
import org.eclipse.kura.wire.WireConfiguration;
import org.eclipse.kura.wire.WireService;
import org.eclipse.kura.wire.Wires;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.wireadmin.Wire;
import org.osgi.service.wireadmin.WireAdmin;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Throwables;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.Monitor;

/**
 * The Class WireServiceImpl implements Wire Service
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

	/** Synchronization Monitor. */
	private final Monitor m_monitor;

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

	/** Constructor */
	public WireServiceImpl() {
		this.m_wireConfigs = Lists.newArrayList();
		this.m_monitor = new Monitor();
	}

	/**
	 * OSGi service component callback while activation
	 *
	 * @param componentContext
	 *            the component context
	 * @param properties
	 *            the properties
	 */
	protected synchronized void activate(final ComponentContext componentContext,
			final Map<String, Object> properties) {
		s_logger.info(s_message.activatingWireService());
		this.m_ctx = componentContext;
		try {
			this.extractProperties(properties);
			this.m_trackerCustomizer = new WireComponentTrackerCustomizer(this.m_ctx.getBundleContext(), this);
			this.m_serviceTracker = new ServiceTracker<WireComponent, WireComponent>(
					componentContext.getBundleContext(), WireComponent.class.getName(), this.m_trackerCustomizer);
			this.m_serviceTracker.open();
			this.createWires();
		} catch (final Exception exception) {
			s_logger.error(Throwables.getStackTraceAsString(exception));
		}
		s_logger.info(s_message.activatingWireServiceDone());
	}

	/**
	 * Bind the configuration service.
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
	 * Bind the wire admin dependency
	 *
	 * @param wireAdmin
	 *            the new wire admin service dependency
	 */
	public synchronized void bindWireAdmin(final WireAdmin wireAdmin) {
		if (this.m_wireAdmin == null) {
			this.m_wireAdmin = wireAdmin;
		}
	}

	/** {@inheritDoc} */
	@Override
	public Wire createWire(final String emitterPid, final String receiverPid) {
		checkNull(emitterPid, s_message.emitterPidNonNull());
		checkNull(receiverPid, s_message.receiverPidNonNull());

		s_logger.info(s_message.creatingWire(emitterPid, receiverPid));
		final WireConfiguration conf = Wires.newWireConfiguration(emitterPid, receiverPid, null);
		this.m_wireConfigs.add(conf);
		s_logger.info(s_message.creatingWireDone(emitterPid, receiverPid));
		return this.m_wireAdmin.createWire(emitterPid, receiverPid, null);
	}

	/** {@inheritDoc} */
	@Override
	public void createWireComponent(final String factoryPid, final String name) throws KuraException {
		checkNull(factoryPid, s_message.factoryPidNonNull());
		checkNull(name, s_message.wireComponentNameNonNull());

		s_logger.info(s_message.creatingWireComponent(name));
		this.m_configService.createFactoryConfiguration(factoryPid, name, null, true);
	}

	/**
	 * Create the wires based on the provided wire configurations
	 */
	synchronized void createWires() throws KuraException {
		s_logger.info(s_message.creatingWires());
		// remove existing wires
		this.removeExistingWires();
		// create new wires
		final List<WireConfiguration> cloned = Lists.newArrayList();
		for (final WireConfiguration wc : this.m_wireConfigs) {
			cloned.add(Wires.newWireConfiguration(wc.getEmitterPid(), wc.getReceiverPid(), wc.getFilter(),
					wc.isCreated()));
		}
		for (final WireConfiguration conf : cloned) {
			final String emitter = conf.getEmitterPid();
			final String receiver = conf.getReceiverPid();
			this.updatePidsInList(emitter, receiver);
			boolean emitterFound = false;
			boolean receiverFound = false;
			for (final String s : this.m_trackerCustomizer.getWireEmitters()) {
				if (s.equals(emitter)) {
					emitterFound = true;
					break;
				}
			}
			for (final String s : this.m_trackerCustomizer.getWireReceivers()) {
				if (s.equals(receiver)) {
					receiverFound = true;
					break;
				}
			}
			if (emitterFound && receiverFound && !this.wireAlreadyCreated(emitter, receiver)) {
				s_logger.info(s_message.creatingWire(emitter, receiver));
				this.m_wireAdmin.createWire(emitter, receiver, null);
				final WireConfiguration wc = this.getWireConfiguration(emitter, receiver);
				wc.setCreated(true);
				this.persistWires(false);
			}
		}
		s_logger.info(s_message.creatingWiresDone());
	}

	/**
	 * OSGi service component callback while deactivation
	 *
	 * @param componentContext
	 *            the component context
	 */
	protected synchronized void deactivate(final ComponentContext componentContext) {
		s_logger.info(s_message.deactivatingWireService());
		this.m_configService = null;
		this.m_wireAdmin = null;
		this.m_serviceTracker.close();
		s_logger.info(s_message.deactivatingWireServiceDone());
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
		this.m_options = WireServiceOptions.getInstance(properties);
		this.m_properties = properties;

		for (final WireConfiguration conf : this.m_options.getWireConfigurations()) {
			this.m_wireConfigs.add(conf);
		}
	}

	/** {@inheritDoc} */
	@Override
	public ComponentConfiguration getConfiguration() throws KuraException {
		final Tocd wiresOCD = new Tocd();
		wiresOCD.setId(CONF_PID);
		wiresOCD.setName(s_message.name());
		wiresOCD.setDescription(s_message.description());

		final Map<String, Object> props = Maps.newHashMap();

		for (final Map.Entry<String, Object> entry : this.m_properties.entrySet()) {
			props.put(entry.getKey(), entry.getValue());
		}
		int i = 1;
		for (final WireConfiguration wireConfiguration : this.m_wireConfigs) {
			final String emitterKey = String.valueOf(i++) + "emitter";
			final String receiverKey = String.valueOf(i++) + "receiver";
			final String filterKey = String.valueOf(i++) + "filter";
			props.put(emitterKey, wireConfiguration.getEmitterPid());
			props.put(receiverKey, wireConfiguration.getReceiverPid());
			props.put(filterKey, wireConfiguration.getFilter());
		}
		return new ComponentConfigurationImpl(CONF_PID, wiresOCD, props);
	}

	/**
	 * Gets the wire configuration.
	 *
	 * @param emitter
	 *            the wire emitter
	 * @param receiver
	 *            the wire receiver
	 * @return the wire configuration
	 * @throws KuraRuntimeException
	 *             if any of the provided arguments is null
	 */
	private WireConfiguration getWireConfiguration(final String emitter, final String receiver) {
		checkNull(emitter, s_message.emitterPidNonNull());
		checkNull(receiver, s_message.receiverPidNonNull());

		for (final WireConfiguration wc : this.m_wireConfigs) {
			if (wc.getEmitterPid().equals(emitter) && wc.getReceiverPid().equals(receiver)) {
				return wc;
			}
		}
		final WireConfiguration wc = Wires.newWireConfiguration(emitter, receiver, null);
		this.m_wireConfigs.add(wc);
		return wc;
	}

	/**
	 * Persist wires in the snapshot
	 *
	 * @param shouldPersist
	 *            if set to true a snapshot will be taken
	 */
	private void persistWires(final boolean shouldPersist) {
		s_logger.info(s_message.persistingWires());
		final List<WireConfiguration> list = this.m_options.getWires();
		list.clear();
		for (final WireConfiguration w : this.m_wireConfigs) {
			list.add(w);
		}
		try {
			this.m_configService.updateConfiguration(CONF_PID, this.m_properties, shouldPersist);
		} catch (final KuraException exception) {
			s_logger.error(Throwables.getStackTraceAsString(exception));
		}
		s_logger.info(s_message.persistingWiresDone());
	}

	/**
	 * Removes the existing wires between the emitters and receivers from the
	 * stored configurations
	 */
	private void removeExistingWires() {
		for (final WireConfiguration configuration : this.m_wireConfigs) {
			final String emitter = configuration.getEmitterPid();
			final String receiver = configuration.getReceiverPid();
			this.removeWire(emitter, receiver);
		}
	}

	/**
	 * Removes all the existing wires related to the specific PID.
	 *
	 * @param pid
	 *            the wire component PID
	 * @return true, if successful
	 * @throws KuraRuntimeException
	 *             if argument is null
	 */
	private boolean removePidRelatedWires(final String pid) {
		checkNull(pid, s_message.pidNonNull());
		boolean atLeatOneRemoved = false;
		final WireConfiguration[] copy = FluentIterable.from(this.m_wireConfigs).toArray(WireConfiguration.class);
		for (final WireConfiguration wc : copy) {
			if (wc.getEmitterPid().equals(pid) || wc.getReceiverPid().equals(pid)) {
				this.removeWire(wc.getEmitterPid(), wc.getReceiverPid());
				atLeatOneRemoved = true;
			}
		}
		return atLeatOneRemoved;
	}

	/** {@inheritDoc} */
	@Override
	public boolean removeWire(final String emitterPid, final String receiverPid) {
		checkNull(emitterPid, s_message.emitterPidNonNull());
		checkNull(receiverPid, s_message.receiverPidNonNull());

		s_logger.info(s_message.removingWires());
		try {
			WireConfiguration wire = null;
			for (final WireConfiguration conf : this.m_wireConfigs) {
				if (conf.getEmitterPid().equals(emitterPid) && conf.getReceiverPid().equals(receiverPid)) {
					wire = conf;
					break;
				}
			}
			if (wire != null) {
				final Wire[] wiresList = this.m_wireAdmin.getWires(null);
				if (wiresList != null) {
					for (final Wire w : wiresList) {
						final String producer = w.getProperties().get(WIREADMIN_PRODUCER_PID).toString();
						final String consumer = w.getProperties().get(WIREADMIN_CONSUMER_PID).toString();
						if (producer.equals(wire.getEmitterPid()) && consumer.equals(wire.getReceiverPid())) {
							this.m_wireAdmin.deleteWire(w);
							this.m_wireConfigs.remove(wire);
							this.persistWires(true);
							return true;
						}
					}
				}
			}
		} catch (final InvalidSyntaxException e) {
			Throwables.propagate(e);
		}
		s_logger.info(s_message.removingWiresDone());
		return false;
	}

	/** {@inheritDoc} */
	@Override
	public boolean removeWireComponent(final String pid) {
		checkNull(pid, s_message.pidNonNull());
		s_logger.info(s_message.removingWireComponent());
		// Search for wires using the PID we are going to delete
		this.removePidRelatedWires(pid);
		// Then delete the instance
		try {
			this.m_configService.deleteFactoryConfiguration(pid, false);
		} catch (final KuraException e) {
			s_logger.error(Throwables.getStackTraceAsString(e));
			return false;
		}
		this.persistWires(true);
		s_logger.info(s_message.removingWireComponentDone());
		return true;
	}

	/**
	 * Unbind configuration service dependency
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
	 * Unbind wire admin dependency
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
	 * OSGi service component callback while updating
	 *
	 * @param properties
	 *            the properties
	 */
	public synchronized void updated(final Map<String, Object> properties) {
		s_logger.info(s_message.updatingWireService() + properties);
		this.extractProperties(properties);
		try {
			this.createWires();
		} catch (final KuraException e) {
			s_logger.error(Throwables.getStackTraceAsString(e));
		}
		s_logger.info(s_message.updatingWireServiceDone());
	}

	/**
	 * Updates wire component PIDs in list.
	 *
	 * @param oldEmitterPid
	 *            the old emitter PID
	 * @param oldReceiverPid
	 *            the old receiver PID
	 * @throws KuraRuntimeException
	 *             if any of the provided arguments is null
	 */
	private void updatePidsInList(final String oldEmitterPid, final String oldReceiverPid) throws KuraException {
		checkNull(oldEmitterPid, s_message.emitterPidNonNull());
		checkNull(oldReceiverPid, s_message.receiverPidNonNull());

		final String newEmitter = this.m_configService.getComponentConfiguration(oldEmitterPid).getPid();
		final String newReceiver = this.m_configService.getComponentConfiguration(oldReceiverPid).getPid();
		if ((newEmitter != oldEmitterPid) || (newReceiver != oldReceiverPid)) {
			this.m_monitor.enter();
			try {
				for (final WireConfiguration wc : this.m_wireConfigs) {
					if ((wc.getEmitterPid() == oldEmitterPid) || (wc.getReceiverPid() == oldReceiverPid)) {
						wc.update(newEmitter, newReceiver);
					}
				}
			} finally {
				this.m_monitor.leave();
			}
		}
	}

	/**
	 * Checks to see if wire already created between the provided emitter and
	 * receiver
	 *
	 * @param emitterPid
	 *            the wire emitter PID
	 * @param receiverPid
	 *            the wire receiver PID
	 * @return true, if successful
	 * @throws KuraRuntimeException
	 *             if any of the provided arguments is null
	 */
	private boolean wireAlreadyCreated(final String emitterPid, final String receiverPid) {
		checkNull(emitterPid, s_message.emitterPidNonNull());
		checkNull(receiverPid, s_message.receiverPidNonNull());

		for (final WireConfiguration wc : this.m_wireConfigs) {
			if (wc.getEmitterPid().equals(emitterPid) && wc.getReceiverPid().equals(receiverPid) && wc.isCreated()) {
				return true;
			}
		}
		return false;
	}

}

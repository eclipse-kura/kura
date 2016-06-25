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

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.KuraRuntimeException;
import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.configuration.SelfConfiguringComponent;
import org.eclipse.kura.core.configuration.ComponentConfigurationImpl;
import org.eclipse.kura.core.configuration.metatype.Tad;
import org.eclipse.kura.core.configuration.metatype.Tocd;
import org.eclipse.kura.core.configuration.metatype.Toption;
import org.eclipse.kura.core.configuration.metatype.Tscalar;
import org.eclipse.kura.localization.LocalizationAdapter;
import org.eclipse.kura.localization.WireMessages;
import org.eclipse.kura.wire.WireComponent;
import org.eclipse.kura.wire.WireConfiguration;
import org.eclipse.kura.wire.WireEmitter;
import org.eclipse.kura.wire.WireReceiver;
import org.eclipse.kura.wire.WireService;
import org.eclipse.kura.wire.WireServiceOptions;
import org.eclipse.kura.wire.Wires;
import org.json.JSONException;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentException;
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

	/**
	 * String literal for emitter and receiver to be used in wire configuration
	 */
	private static final String INSTANCE = "INSTANCE";

	/** String literal for NONE to be used in properties */
	private static final String NONE = "NONE";

	/** The Constant denoting Wire Receiver. */
	private static final String PROP_CONSUMER_PID = "wireadmin.consumer.pid";

	/** The Constant denoting Wire Service PID */
	private static final String PROP_PID = "org.eclipse.kura.wire.internal.WireServiceImpl";

	/** The Constant denoting Wire Emitter. */
	private static final String PROP_PRODUCER_PID = "wireadmin.producer.pid";

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
	 * @throws ComponentException
	 *             the component exception
	 */
	protected synchronized void activate(final ComponentContext componentContext, final Map<String, Object> properties)
			throws Exception {
		s_logger.info(s_message.activatingWireService());
		this.m_ctx = componentContext;
		try {
			this.m_options = Wires.newWireServiceOptions(properties);
			this.m_properties = properties;

			for (final WireConfiguration conf : this.m_options.getWireConfigurations()) {
				this.m_wireConfigs.add(conf);
			}
			this.m_trackerCustomizer = new WireComponentTrackerCustomizer(this.m_ctx.getBundleContext(), this);
			this.m_serviceTracker = new ServiceTracker<WireComponent, WireComponent>(
					componentContext.getBundleContext(), WireComponent.class.getName(), this.m_trackerCustomizer);
			this.m_serviceTracker.open();
			this.createWires();
		} catch (final Exception exception) {
			Throwables.propagateIfInstanceOf(exception, JSONException.class);
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

	/**
	 * Creates the wire component from the property.
	 *
	 * @param value
	 *            the value
	 * @param name
	 *            the name
	 * @return the string
	 * @throws KuraRuntimeException
	 *             if any of the provided argument is null
	 */
	private String createComponentFromProperty(final String value, final String name) {
		checkNull(value, s_message.valueNonNull());
		checkNull(name, s_message.componentNameNonNull());

		final String[] tokens = value.split("\\|");
		if ("FACTORY".equals(tokens[0])) {
			return this.createWireComponent(tokens[1], name);
		} else {
			return tokens[1];
		}
	}

	/** {@inheritDoc} */
	@Override
	public Wire createWire(final String emitterName, final String receiverName) {
		checkNull(emitterName, s_message.emitterNameNonNull());
		checkNull(receiverName, s_message.receiverNameNonNull());

		s_logger.info(s_message.creatingWire(emitterName, receiverName));
		final WireConfiguration conf = Wires.newWireConfiguration(emitterName, receiverName, null);
		this.m_wireConfigs.add(conf);
		s_logger.info(s_message.creatingWireDone(emitterName, receiverName));
		return this.m_wireAdmin.createWire(emitterName, receiverName, null);
	}

	/** {@inheritDoc} */
	@Override
	public String createWireComponent(final String factoryPid, final String name) {
		checkNull(factoryPid, s_message.factoryPidNonNull());
		checkNull(name, s_message.wireComponentNameNonNull());

		s_logger.info(s_message.creatingWireComponent(name));
		try {
			this.m_configService.createFactoryConfiguration(factoryPid, name, null, false);
		} catch (final KuraException ex) {
			s_logger.error(s_message.errorCreatingWireComponent() + Throwables.getStackTraceAsString(ex));
		}
		return factoryPid;
	}

	/**
	 * Create the wires.
	 */
	protected synchronized void createWires() throws KuraException {
		s_logger.info(s_message.creatingWires());
		// remove existing wires
		this.removeExistingWires();
		// create new wires
		final List<WireConfiguration> cloned = Lists.newArrayList();
		for (final WireConfiguration wc : this.m_wireConfigs) {
			cloned.add(Wires.newWireConfiguration(wc.getEmitterName(), wc.getReceiverName(), wc.getFilter(),
					wc.isCreated()));
		}
		for (final WireConfiguration conf : cloned) {
			this.updatePidNamesInList(conf.getEmitterName(), conf.getReceiverName());
			final String emitter = this.m_configService.getComponentConfiguration(conf.getEmitterName()).getPid();
			final String receiver = this.m_configService.getComponentConfiguration(conf.getReceiverName()).getPid();
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
				try {
					this.persistWires(false);
				} catch (final JSONException e) {
					s_logger.error(s_message.errorPersistingWires() + Throwables.getStackTraceAsString(e));
				}
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

	/** {@inheritDoc} */
	@Override
	public ComponentConfiguration getConfiguration() throws KuraException {
		final Tocd wiresOCD = new Tocd();
		wiresOCD.setId(this.getClass().getName());
		wiresOCD.setName(s_message.wireService());
		wiresOCD.setDescription(s_message.creatingNewWire());

		// All attribute definitions
		final String emitterPidAd = "emitter.pids";
		final String emitterNameAd = "emitter.name";
		final String receiverPidAd = "receiver.pids";
		final String receiverNameAd = "receiver.name";
		final String deleteWiresAd = "delete.wires";
		final String deleteInstanceAd = "delete.instances";

		final List<String> emittersOptions = Lists.newArrayList();
		final Set<String> factoryPids = this.m_configService.getFactoryComponentPids();

		for (final String factoryPid : factoryPids) {
			emittersOptions.addAll(WireUtils.getFactoriesAndInstances(this.m_ctx, factoryPid, WireEmitter.class));
		}

		final Tad emitterTad = new Tad();
		emitterTad.setId(emitterPidAd);
		emitterTad.setName(emitterPidAd);
		emitterTad.setType(Tscalar.STRING);
		emitterTad.setCardinality(0);
		emitterTad.setRequired(true);

		final Toption defaultOpt = new Toption();
		defaultOpt.setLabel(s_message.noNewInstance());
		defaultOpt.setValue(NONE);
		emitterTad.getOption().add(defaultOpt);

		StringBuilder sb = new StringBuilder();
		for (final String emitterOption : emittersOptions) {
			final Toption opt = new Toption();
			opt.setLabel(emitterOption);
			opt.setValue(emitterOption);
			emitterTad.getOption().add(opt);
			sb.append(" ,");
		}
		emitterTad.setDefault(sb.toString());
		emitterTad.setDescription(s_message.chooseEmitter());
		wiresOCD.addAD(emitterTad);

		final Tad emitterName = new Tad();
		emitterName.setId(emitterNameAd);
		emitterName.setName(emitterNameAd);
		emitterName.setType(Tscalar.STRING);
		emitterName.setCardinality(0);
		emitterName.setRequired(false);
		emitterName.setDefault("");
		emitterName.setDescription(s_message.multitonInstanceName());
		wiresOCD.addAD(emitterName);

		// Create an option element for each producer factory
		final List<String> receiversOptions = Lists.newArrayList();

		for (final String factoryPid : factoryPids) {
			receiversOptions.addAll(WireUtils.getFactoriesAndInstances(this.m_ctx, factoryPid, WireReceiver.class));
		}

		final Tad receiverTad = new Tad();
		receiverTad.setId(receiverPidAd);
		receiverTad.setName(receiverPidAd);
		receiverTad.setType(Tscalar.STRING);
		receiverTad.setCardinality(0);
		receiverTad.setRequired(true);
		receiverTad.getOption().add(defaultOpt);
		sb = new StringBuilder();
		for (final String receiverOption : receiversOptions) {
			final Toption opt = new Toption();
			opt.setLabel(receiverOption);
			opt.setValue(receiverOption);
			receiverTad.getOption().add(opt);
			sb.append(" ,");
		}
		receiverTad.setDefault(sb.toString());
		receiverTad.setDescription(s_message.chooseReceiver());
		wiresOCD.addAD(receiverTad);

		final Tad receiverName = new Tad();
		receiverName.setId(receiverNameAd);
		receiverName.setName(receiverNameAd);
		receiverName.setType(Tscalar.STRING);
		receiverName.setCardinality(0);
		receiverName.setRequired(false);
		receiverName.setDefault("");

		sb = new StringBuilder(s_message.multitonInstanceName() + s_message.activeWires());
		sb.append("<table style=\"width:100%; border: 1px solid black;\">");
		sb.append("<tr><td><b>Emitter</b></td><td><b>Receiver</b></td></tr>");
		for (final WireConfiguration wc : this.m_wireConfigs) {
			sb.append("<tr><td>").append(wc.getEmitterName()).append("</td><td>").append(wc.getReceiverName())
					.append("</td></tr>");
		}
		sb.append("</table>");
		receiverName.setDescription(sb.toString());
		wiresOCD.addAD(receiverName);

		final Tad wiresTad = new Tad();
		wiresTad.setName(deleteWiresAd);
		wiresTad.setId(deleteWiresAd);
		wiresTad.setType(Tscalar.STRING);
		wiresTad.setCardinality(0);
		wiresTad.setRequired(true);
		wiresTad.setDefault(NONE);
		for (int i = 0; i < this.m_wireConfigs.size(); i++) {
			final WireConfiguration wc = this.m_wireConfigs.get(i);
			final Toption o = new Toption();
			o.setLabel("P:" + wc.getEmitterName() + " - C:" + wc.getReceiverName());
			o.setValue(String.valueOf(i));
			wiresTad.getOption().add(o);
		}
		Toption o = new Toption();
		o.setLabel(s_message.noDeleteWire());
		o.setValue(NONE);
		wiresTad.getOption().add(o);
		wiresTad.setDescription(s_message.selectWire());
		wiresOCD.addAD(wiresTad);

		final Tad servicesTad = new Tad();
		servicesTad.setName(deleteInstanceAd);
		servicesTad.setId(deleteInstanceAd);
		servicesTad.setType(Tscalar.STRING);
		servicesTad.setCardinality(0);
		servicesTad.setRequired(true);
		servicesTad.setDefault(NONE);
		final Toption opt = new Toption();
		opt.setLabel(s_message.noDeleteInstance());
		opt.setValue(NONE);
		servicesTad.getOption().add(opt);

		for (final String s : WireUtils.getEmittersAndReceivers(this.m_ctx)) {
			o = new Toption();
			o.setLabel(s);
			o.setValue(s);
			servicesTad.getOption().add(o);
		}

		servicesTad.setDescription(s_message.selectInstance());
		wiresOCD.addAD(servicesTad);

		try {
			this.m_properties = Maps.newHashMap();
			// Put the JSON configuration into properties to persist in snapshot
			this.m_properties.put("wires", this.m_options.toJsonString());
			this.m_properties.put(deleteInstanceAd, NONE);
			this.m_properties.put(deleteWiresAd, NONE);
			this.m_properties.put(receiverPidAd, NONE);
			this.m_properties.put(emitterPidAd, NONE);
		} catch (final JSONException e) {
			Throwables.propagate(e);
		}
		return new ComponentConfigurationImpl(PROP_PID, wiresOCD, this.m_properties);
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
		checkNull(emitter, s_message.emitterNameNonNull());
		checkNull(receiver, s_message.receiverNameNonNull());

		for (final WireConfiguration wc : this.m_wireConfigs) {
			if (wc.getEmitterName().equals(emitter) && wc.getReceiverName().equals(receiver)) {
				return wc;
			}
		}
		final WireConfiguration wc = Wires.newWireConfiguration(emitter, receiver, null);
		this.m_wireConfigs.add(wc);
		return wc;
	}

	/**
	 * Persist wires in the configuration
	 *
	 * @param shouldPersist
	 *            if set to true a snapshot will be taken
	 * @throws JSONException
	 *             if persistence fails
	 */
	private void persistWires(final boolean shouldPersist) throws JSONException {
		s_logger.info(s_message.persistingWires());
		final List<WireConfiguration> list = this.m_options.getWires();
		list.clear();
		for (final WireConfiguration w : this.m_wireConfigs) {
			list.add(w);
		}
		final Map<String, Object> newProperties = Maps.newHashMap(this.m_properties);
		try {
			final String jsonString = this.m_options.toJsonString();
			newProperties.put(WireServiceOptions.CONF_WIRES, jsonString);
			this.m_configService.updateConfiguration(PROP_PID, newProperties, shouldPersist);
		} catch (final Exception exception) {
			Throwables.propagateIfInstanceOf(exception, JSONException.class);
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
			final String emitter = configuration.getEmitterName();
			final String receiver = configuration.getReceiverName();
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
			if (wc.getEmitterName().equals(pid) || wc.getReceiverName().equals(pid)) {
				this.removeWire(wc.getEmitterName(), wc.getReceiverName());
				atLeatOneRemoved = true;
			}
		}
		return atLeatOneRemoved;
	}

	/** {@inheritDoc} */
	@Override
	public boolean removeWire(final String emitterPid, final String receiverPid) {
		checkNull(emitterPid, s_message.emitterNameNonNull());
		checkNull(receiverPid, s_message.receiverNameNonNull());

		s_logger.info(s_message.removingWires());
		try {
			WireConfiguration wire = null;
			for (final WireConfiguration conf : this.m_wireConfigs) {
				if (conf.getEmitterName().equals(emitterPid) && conf.getReceiverName().equals(receiverPid)) {
					wire = conf;
					break;
				}
			}
			if (wire != null) {
				final Wire[] wiresList = this.m_wireAdmin.getWires(null);
				if (wiresList != null) {
					for (final Wire w : wiresList) {
						final String producer = w.getProperties().get(PROP_PRODUCER_PID).toString();
						final String consumer = w.getProperties().get(PROP_CONSUMER_PID).toString();
						if (producer.equals(wire.getEmitterName()) && consumer.equals(wire.getReceiverName())) {
							this.m_wireAdmin.deleteWire(w);
							this.m_wireConfigs.remove(wire);
							try {
								this.persistWires(true);
							} catch (final JSONException e) {
								s_logger.error(s_message.errorPersistingWires() + Throwables.getStackTraceAsString(e));
							}
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
		try {
			this.persistWires(true);
		} catch (final JSONException e) {
			s_logger.error(s_message.errorPersistingWires() + Throwables.getStackTraceAsString(e));
		}
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
		try {
			final Object emitterPid = properties.get("emitter.pids");
			final Object receiverPid = properties.get("receiver.pids");

			String emitterName = null;
			String receiverName = null;
			final String emitterKeyProperty = "emitter.name";
			final String receiverKeyProperty = "receiver.name";

			if (properties.containsKey(emitterKeyProperty)) {
				emitterName = (String) properties.get(emitterKeyProperty);
			}
			if (properties.containsKey(receiverKeyProperty)) {
				receiverName = (String) properties.get(receiverKeyProperty);
			}
			// New Wire
			if ((emitterPid != null) && (receiverPid != null) && (!NONE.equals(emitterPid.toString()))
					&& (!NONE.equals(receiverPid.toString()))) {
				final String emitterString = this.createComponentFromProperty(emitterPid.toString(), emitterName);
				final String receiverString = this.createComponentFromProperty(receiverPid.toString(), receiverName);
				final WireConfiguration wc = Wires.newWireConfiguration(emitterString, receiverString, null, false);
				this.m_wireConfigs.add(wc);
				this.createWires();
				if (emitterPid.toString().startsWith(INSTANCE) && receiverPid.toString().startsWith(INSTANCE)) {
					this.m_configService.snapshot();
				}
			}
			final String deleteWiresKeyProperty = "delete.wires";
			// Delete Existing Wire
			Object wiresDelete = null;
			if (properties.containsKey(deleteWiresKeyProperty)) {
				wiresDelete = properties.get(deleteWiresKeyProperty);
			}
			if ((wiresDelete != null) && (!NONE.equals(wiresDelete.toString()))) {
				final int index = Integer.parseInt(wiresDelete.toString());
				final WireConfiguration wc = this.m_wireConfigs.get(index);
				this.removeWire(wc.getEmitterName(), wc.getReceiverName());
			}

			final String deleteInstancesKeyProperty = "delete.instances";
			// Delete Emitter/Receiver Instance
			Object instancesDelete = null;
			if (properties.containsKey(deleteInstancesKeyProperty)) {
				instancesDelete = properties.get(deleteInstancesKeyProperty);
			}
			if ((instancesDelete != null) && (!NONE.equals(instancesDelete.toString()))) {
				this.removeWireComponent(instancesDelete.toString());
			}
		} catch (final KuraException ex) {
			s_logger.error(s_message.errorUpdatingWireService() + Throwables.getStackTraceAsString(ex));
		}
		s_logger.info(s_message.updatingWireServiceDone());
	}

	/**
	 * Update wire component names in list.
	 *
	 * @param oldEmitterName
	 *            the old emitter name
	 * @param oldReceiverName
	 *            the old receiver name
	 * @throws KuraRuntimeException
	 *             if any of the provided arguments is null
	 */
	private void updatePidNamesInList(final String oldEmitterName, final String oldReceiverName) throws KuraException {
		checkNull(oldEmitterName, s_message.emitterNameNonNull());
		checkNull(oldReceiverName, s_message.receiverNameNonNull());

		final String newEmitter = this.m_configService.getComponentConfiguration(oldEmitterName).getPid();
		final String newReceiver = this.m_configService.getComponentConfiguration(oldReceiverName).getPid();
		if ((newEmitter != oldEmitterName) || (newReceiver != oldReceiverName)) {
			this.m_monitor.enter();
			try {
				for (final WireConfiguration wc : this.m_wireConfigs) {
					if ((wc.getEmitterName() == oldEmitterName) || (wc.getReceiverName() == oldReceiverName)) {
						wc.update(newEmitter, newReceiver);
					}
				}
			} finally {
				this.m_monitor.leave();
			}
		}
	}

	/**
	 * Checks to see if wire already created.
	 *
	 * @param emitter
	 *            the wire emitter name
	 * @param receiver
	 *            the wire receiver name
	 * @return true, if successful
	 * @throws KuraRuntimeException
	 *             if any of the provided arguments is null
	 */
	private boolean wireAlreadyCreated(final String emitter, final String receiver) {
		checkNull(emitter, s_message.emitterNameNonNull());
		checkNull(receiver, s_message.receiverNameNonNull());

		for (final WireConfiguration wc : this.m_wireConfigs) {
			if (wc.getEmitterName().equals(emitter) && wc.getReceiverName().equals(receiver) && wc.isCreated()) {
				return true;
			}
		}
		return false;
	}

}

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

import java.util.HashMap;
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
import org.eclipse.kura.wire.WireConfiguration;
import org.eclipse.kura.wire.WireEmitter;
import org.eclipse.kura.wire.WireReceiver;
import org.eclipse.kura.wire.WireService;
import org.eclipse.kura.wire.WireServiceOptions;
import org.eclipse.kura.wire.util.Wires;
import org.json.JSONException;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentException;
import org.osgi.service.wireadmin.Wire;
import org.osgi.service.wireadmin.WireAdmin;
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

	/** The Wire Service options. */
	private static WireServiceOptions m_options;

	/** The Constant denoting Wire Receiver. */
	private static final String PROP_CONSUMER_PID = "wireadmin.consumer.pid";

	/** The Constant denoting Wire Service PID */
	private static final String PROP_PID = "org.eclipse.kura.wire.internal.WireServiceImpl";

	/** The Constant denoting Wire Emitter. */
	private static final String PROP_PRODUCER_PID = "wireadmin.producer.pid";

	/** The Logger instance. */
	private static final Logger s_logger = LoggerFactory.getLogger(WireServiceImpl.class);

	/** The Configuration Service instance. */
	private volatile ConfigurationService m_configService;

	/** The Service Component Context. */
	private ComponentContext m_ctx;

	/** Synchronization Monitor. */
	private final Monitor m_monitor;

	/** The service component properties. */
	private Map<String, Object> m_properties;

	/** The service tracker to track all wire components */
	private WireSeviceTracker m_serviceTracker;

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
		s_logger.info("Activating Wire Service...");

		this.m_ctx = componentContext;
		try {
			m_options = Wires.newWireServiceOptions(properties);
			this.m_properties = properties;

			for (final WireConfiguration conf : m_options.getWireConfigurations()) {
				this.m_wireConfigs.add(conf);
			}
			this.m_serviceTracker = new WireSeviceTracker(this.m_ctx.getBundleContext(), this);
			this.m_serviceTracker.open();
			this.createWires();
		} catch (final Throwable throwable) {
			Throwables.propagateIfInstanceOf(throwable, JSONException.class);
			s_logger.error(Throwables.getStackTraceAsString(throwable));
		}
		s_logger.info("Activating Wire Service...Done");
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
		checkNull(value, "Value cannot be null");
		checkNull(name, "Component name cannot be null");

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
		checkNull(emitterName, "Emitter name cannot be null");
		checkNull(receiverName, "Receiver name cannot be null");

		s_logger.info("Creating wire between..." + emitterName + " and " + receiverName + ".....");
		final WireConfiguration conf = Wires.newWireConfiguration(emitterName, receiverName, null);
		this.m_wireConfigs.add(conf);
		s_logger.info("Creating wire between..." + emitterName + " and " + receiverName + ".....Done");
		return this.m_wireAdmin.createWire(emitterName, receiverName, null);
	}

	/** {@inheritDoc} */
	@Override
	public String createWireComponent(final String factoryPid, final String name) {
		checkNull(factoryPid, "Factory PID cannot be null");
		checkNull(name, "Wire Component name cannot be null");

		s_logger.info("Creating wire component of " + name + ".....");
		try {
			this.m_configService.createFactoryConfiguration(factoryPid, name, null, false);
		} catch (final KuraException ex) {
			s_logger.error("Error while creating wire component..." + Throwables.getStackTraceAsString(ex));
		}
		return factoryPid;
	}

	/**
	 * Creates the wires.
	 */
	protected synchronized void createWires() throws KuraException {
		s_logger.info("Creating wires.....");
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
			for (final String s : this.m_serviceTracker.getWireEmitters()) {
				if (s.equals(emitter)) {
					emitterFound = true;
					break;
				}
			}
			for (final String s : this.m_serviceTracker.getWireReceivers()) {
				if (s.equals(receiver)) {
					receiverFound = true;
					break;
				}
			}
			if (emitterFound && receiverFound && !this.wireAlreadyCreated(emitter, receiver)) {
				s_logger.info("Creating wire between {} and {}", emitter, receiver);
				this.m_wireAdmin.createWire(emitter, receiver, null);
				final WireConfiguration wc = this.getWireConfiguration(emitter, receiver);
				wc.setCreated(true);
				this.persistWires(false);
			}
		}
		s_logger.info("Creating wires.....Done");
	}

	/**
	 * OSGi service component callback while deactivation
	 *
	 * @param componentContext
	 *            the component context
	 */
	protected synchronized void deactivate(final ComponentContext componentContext) {
		s_logger.info("Deactivating Wire Service Component..");
		this.m_configService = null;
		this.m_wireAdmin = null;
		s_logger.info("Deactivating Wire Service Component..Done");
	}

	/** {@inheritDoc} */
	@Override
	public ComponentConfiguration getConfiguration() throws KuraException {
		final Tocd wiresOCD = new Tocd();
		wiresOCD.setId("WireServiceImpl");
		wiresOCD.setName("Wire Service");
		wiresOCD.setDescription("Create a new Wire");

		// All attribute definitions
		final String EMITTER_PID_AD = "emitter.pids";
		final String EMITTER_NAME_AD = "emitter.name";
		final String RECEIVER_PID_AD = "receiver.pids";
		final String RECEIVER_NAME_AD = "receiver.name";
		final String DELETE_WIRES_AD = "delete.wires";
		final String DELETE_INSTANCE_AD = "delete.instances";

		final List<String> emittersOptions = Lists.newArrayList();
		final Set<String> factoryPids = this.m_configService.getFactoryComponentPids();

		for (final String factoryPid : factoryPids) {
			emittersOptions.addAll(WireUtils.getFactoriesAndInstances(this.m_ctx, factoryPid, WireEmitter.class));
		}

		final Tad emitterTad = new Tad();
		emitterTad.setId(EMITTER_PID_AD);
		emitterTad.setName(EMITTER_PID_AD);
		emitterTad.setType(Tscalar.STRING);
		emitterTad.setCardinality(0);
		emitterTad.setRequired(true);

		final Toption defaultOpt = new Toption();
		defaultOpt.setLabel("No new instance");
		defaultOpt.setValue("NONE");
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
		emitterTad.setDescription("Choose a Wire Emitter");
		wiresOCD.addAD(emitterTad);

		final Tad emitterName = new Tad();
		emitterName.setId(EMITTER_NAME_AD);
		emitterName.setName(EMITTER_NAME_AD);
		emitterName.setType(Tscalar.STRING);
		emitterName.setCardinality(0);
		emitterName.setRequired(false);
		emitterName.setDefault("");
		emitterName.setDescription(
				"multiton.instance.name for the resulting component. If left null it will be equal to service.pid");
		wiresOCD.addAD(emitterName);

		// Create an option element for each producer factory
		final List<String> receiversOptions = Lists.newArrayList();

		for (final String factoryPid : factoryPids) {
			receiversOptions.addAll(WireUtils.getFactoriesAndInstances(this.m_ctx, factoryPid, WireReceiver.class));
		}

		final Tad receiverTad = new Tad();
		receiverTad.setId(RECEIVER_PID_AD);
		receiverTad.setName(RECEIVER_PID_AD);
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
		receiverTad.setDescription("Choose a Wire Receiver");
		wiresOCD.addAD(receiverTad);

		final Tad receiverName = new Tad();
		receiverName.setId(RECEIVER_NAME_AD);
		receiverName.setName(RECEIVER_NAME_AD);
		receiverName.setType(Tscalar.STRING);
		receiverName.setCardinality(0);
		receiverName.setRequired(false);
		receiverName.setDefault("");

		sb = new StringBuilder(
				"multiton.instance.name for the resulting component. If left null it will equal  toservice.pid<br /><br /><b>Active wires:</b><br />");
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
		wiresTad.setName(DELETE_WIRES_AD);
		wiresTad.setId(DELETE_WIRES_AD);
		wiresTad.setType(Tscalar.STRING);
		wiresTad.setCardinality(0);
		wiresTad.setRequired(true);
		wiresTad.setDefault("NONE");
		for (int i = 0; i < this.m_wireConfigs.size(); i++) {
			final WireConfiguration wc = this.m_wireConfigs.get(i);
			final Toption o = new Toption();
			o.setLabel("P:" + wc.getEmitterName() + " - C:" + wc.getReceiverName());
			o.setValue(String.valueOf(i));
			wiresTad.getOption().add(o);
		}
		Toption o = new Toption();
		o.setLabel("Do not delete any wire");
		o.setValue("NONE");
		wiresTad.getOption().add(o);
		wiresTad.setDescription("Select a Wire from the list. It will be deleted when submitting the changes.");
		wiresOCD.addAD(wiresTad);

		final Tad servicesTad = new Tad();
		servicesTad.setName(DELETE_INSTANCE_AD);
		servicesTad.setId(DELETE_INSTANCE_AD);
		servicesTad.setType(Tscalar.STRING);
		servicesTad.setCardinality(0);
		servicesTad.setRequired(true);
		servicesTad.setDefault("NONE");
		final Toption opt = new Toption();
		opt.setLabel("Do not delete any instance");
		opt.setValue("NONE");
		servicesTad.getOption().add(opt);

		for (final String s : WireUtils.getEmittersAndReceivers(this.m_ctx)) {
			o = new Toption();
			o.setLabel(s);
			o.setValue(s);
			servicesTad.getOption().add(o);
		}

		servicesTad.setDescription(
				"Select an Instance from the list. The instance and all connected Wires will be deledet when submitting the changes.");
		wiresOCD.addAD(servicesTad);

		try {
			this.m_properties = new HashMap<String, Object>();
			// Put the json configuration into the properties to persist in the
			// snapshot
			this.m_properties.put("wires", m_options.toJsonString());
			this.m_properties.put(DELETE_INSTANCE_AD, "NONE");
			this.m_properties.put(DELETE_WIRES_AD, "NONE");
			this.m_properties.put(RECEIVER_PID_AD, "NONE");
			this.m_properties.put(EMITTER_PID_AD, "NONE");
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
		checkNull(emitter, "Wire Emitter name cannot be null");
		checkNull(receiver, "Wire Receiver name cannot be null");

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
	 */
	private void persistWires(final boolean shouldPersist) {
		s_logger.info("Persisting Wires..");

		final List<WireConfiguration> list = m_options.getWires();
		list.clear();
		for (final WireConfiguration w : this.m_wireConfigs) {
			list.add(w);
		}
		final Map<String, Object> newProperties = Maps.newHashMap(this.m_properties);
		try {
			final String jsonString = m_options.toJsonString();
			newProperties.put(WireServiceOptions.CONF_WIRES, jsonString);
			this.m_configService.updateConfiguration(PROP_PID, newProperties, shouldPersist);
		} catch (final Throwable throwable) {
			Throwables.propagateIfInstanceOf(throwable, JSONException.class);
			s_logger.error(Throwables.getStackTraceAsString(throwable));
		}
		s_logger.info("Persisting Wires..Done");

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
		checkNull(pid, "PID cannot be null");
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
		checkNull(emitterPid, "Emitter name cannot be null");
		checkNull(receiverPid, "Receiver name cannot be null");

		s_logger.info("Removing Wires..");
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
							this.persistWires(true);
							return true;
						}
					}
				}
			}
		} catch (final InvalidSyntaxException e) {
			Throwables.propagate(e);
		}
		s_logger.info("Removing Wires..Done");
		return false;
	}

	/** {@inheritDoc} */
	@Override
	public boolean removeWireComponent(final String pid) {
		checkNull(pid, "Wire Component PID cannot be null");
		s_logger.info("Removing Wire Component..");
		// Search for wires using the pid we are going to delete
		this.removePidRelatedWires(pid);
		// Then delete the instance
		try {
			this.m_configService.deleteFactoryConfiguration(pid, false);
		} catch (final KuraException e) {
			s_logger.error(Throwables.getStackTraceAsString(e));
			return false;
		}
		this.persistWires(true);
		s_logger.info("Removing Wire Component..Done");
		return true;
	}

	/**
	 * Deregisters configuration service dependency
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
	 * Deregisters wire admin dependency
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
		s_logger.info("Updating Wire Service Component...: " + properties);

		try {
			final Object emitterPid = properties.get("emitter.pids");
			final Object receiverPid = properties.get("receiver.pids");

			String emitterName = null;
			final String receiverName = null;
			if (properties.containsKey("emitter.name")) {
				emitterName = (String) properties.get("emitter.name");
			}
			if (properties.containsKey("receiver.name")) {
				emitterName = (String) properties.get("receiver.name");
			}
			// NEW WIRE
			if ((emitterPid != null) && (receiverPid != null)) {
				if (!emitterPid.toString().equals("NONE") && !receiverPid.toString().equals("NONE")) {
					final String emitterString = this.createComponentFromProperty(emitterPid.toString(), emitterName);
					final String receiverString = this.createComponentFromProperty(receiverPid.toString(),
							receiverName);
					final WireConfiguration wc = Wires.newWireConfiguration(emitterString, receiverString, null, false);
					this.m_wireConfigs.add(wc);
					this.createWires();
					if (emitterPid.toString().startsWith("INSTANCE") && receiverPid.toString().startsWith("INSTANCE")) {
						this.m_configService.snapshot();
					}
				}
			}

			// DELETE EXISTING WIRE
			final Object wiresDelete = properties.get("delete.wires");
			if ((wiresDelete != null) && !wiresDelete.toString().equals("NONE")) {
				final int index = Integer.parseInt(wiresDelete.toString());
				final WireConfiguration wc = this.m_wireConfigs.get(index);
				this.removeWire(wc.getEmitterName(), wc.getReceiverName());
			}
			// DELETE EMITTER/RECEIVER INSTANCE
			final Object instancesDelete = properties.get("delete.instances");
			if ((instancesDelete != null) && !instancesDelete.toString().equals("NONE")) {
				this.removeWireComponent(instancesDelete.toString());
			}
		} catch (final Exception ex) {
			s_logger.error("Error during WireServiceImpl update! Something went wrong..."
					+ Throwables.getStackTraceAsString(ex));
		}
		s_logger.info("Updating Wire Service Component...Done");
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
		checkNull(oldEmitterName, "Emitter name cannot be null");
		checkNull(oldReceiverName, "Receiver name cannot be null");

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
		checkNull(emitter, "Emitter name cannot be null");
		checkNull(receiver, "Receiver name cannot be null");

		for (final WireConfiguration wc : this.m_wireConfigs) {
			if (wc.getEmitterName().equals(emitter) && wc.getReceiverName().equals(receiver) && wc.isCreated()) {
				return true;
			}
		}
		return false;
	}

}

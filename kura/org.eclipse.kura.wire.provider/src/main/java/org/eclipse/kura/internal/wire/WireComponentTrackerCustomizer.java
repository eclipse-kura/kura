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
import static org.eclipse.kura.configuration.ConfigurationService.KURA_SERVICE_PID;

import java.util.List;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.KuraRuntimeException;
import org.eclipse.kura.localization.LocalizationAdapter;
import org.eclipse.kura.localization.resources.WireMessages;
import org.eclipse.kura.util.base.ThrowableUtil;
import org.eclipse.kura.util.collection.CollectionUtil;
import org.eclipse.kura.wire.WireComponent;
import org.eclipse.kura.wire.WireConfiguration;
import org.eclipse.kura.wire.WireEmitter;
import org.eclipse.kura.wire.WireReceiver;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Class WireComponentTrackerCustomizer represents an OSGi service tracker
 * to track Wire Components (Wire Receiver and Wire Emitter)
 */
final class WireComponentTrackerCustomizer implements ServiceTrackerCustomizer<WireComponent, WireComponent> {

	/** The Logger instance. */
	private static final Logger s_logger = LoggerFactory.getLogger(WireComponentTrackerCustomizer.class);

	/** Localization Resource */
	private static final WireMessages s_message = LocalizationAdapter.adapt(WireMessages.class);

	/**
	 * Instantiates a new wire service tracker.
	 *
	 * @param context
	 *            the bundle context
	 * @param wireService
	 *            the wire service
	 * @throws InvalidSyntaxException
	 *             the invalid syntax exception
	 * @throws KuraRuntimeException
	 *             if any of the arguments is null
	 */
	WireComponentTrackerCustomizer(final BundleContext context, final WireServiceImpl wireService)
			throws InvalidSyntaxException {
		checkNull(context, s_message.bundleContextNonNull());
		checkNull(wireService, s_message.wireServiceNonNull());

		this.m_wireEmitters = CollectionUtil.newArrayList();
		this.m_wireReceivers = CollectionUtil.newArrayList();
		this.m_wireService = wireService;
		this.m_context = context;
	}

	/** Bundle Context */
	private final BundleContext m_context;

	/** The wire emitter PIDs. */
	private final List<String> m_wireEmitters;

	/** The wire receiver PIDs. */
	private final List<String> m_wireReceivers;

	/** The wire service. */
	private final WireServiceImpl m_wireService;

	/** {@inheritDoc} */
	@Override
	public WireComponent addingService(final ServiceReference<WireComponent> reference) {
		final WireComponent service = this.m_context.getService(reference);
		s_logger.debug(s_message.addingWireComponent());
		boolean flag = false;
		final String property = (String) reference.getProperty(KURA_SERVICE_PID);
		if (service instanceof WireEmitter) {
			this.m_wireEmitters.add(property);
			s_logger.debug(s_message.registeringEmitter(property));
			flag = true;
		}
		if (service instanceof WireReceiver) {
			this.m_wireReceivers.add(property);
			s_logger.debug(s_message.registeringReceiver(property));
			flag = true;
		}
		if (flag) {
			try {
				this.m_wireService.createWires();
			} catch (final KuraException e) {
				s_logger.error(s_message.errorCreatingWires() + ThrowableUtil.stackTraceAsString(e));
			}
		}
		s_logger.debug(s_message.addingWireComponentDone());
		return service;
	}

	/**
	 * Gets the wire emitter PIDs.
	 *
	 * @return the wire emitter PIDs
	 */
	List<String> getWireEmitters() {
		return this.m_wireEmitters;
	}

	/**
	 * Gets the wire receiver PIDs.
	 *
	 * @return the wire receiver PIDs
	 */
	List<String> getWireReceivers() {
		return this.m_wireReceivers;
	}

	/** {@inheritDoc} */
	@Override
	public void modifiedService(final ServiceReference<WireComponent> reference, final WireComponent service) {
		// Not required
	}

	/** {@inheritDoc} */
	@Override
	public void removedService(final ServiceReference<WireComponent> reference, final WireComponent service) {
		s_logger.debug(s_message.removingWireComponent());
		final String property = (String) reference.getProperty(KURA_SERVICE_PID);
		if (property != null) {
			this.removeWireComponent(property);
			if (service instanceof WireEmitter) {
				this.m_wireEmitters.remove(property);
				s_logger.debug(s_message.deregisteringEmitter(property));
			}
			if (service instanceof WireReceiver) {
				this.m_wireReceivers.remove(property);
				s_logger.debug(s_message.deregisteringReceiver(property));
			}
		}
		this.m_context.ungetService(reference);
		s_logger.debug(s_message.removingWireComponentDone());
	}

	/**
	 * Removes all the Wire Configurations related to the provided PID
	 * (kura.service.pid)
	 *
	 * @param pid
	 *            the wire component PID
	 * @throws KuraRuntimeException
	 *             if the argument is null
	 */
	private void removeWireComponent(final String pid) {
		checkNull(pid, s_message.pidNonNull());
		for (final WireConfiguration wireConfiguration : this.m_wireService.getWireConfigurations()) {
			if ((wireConfiguration.getWire() != null) && (pid.equals(wireConfiguration.getEmitterPid())
					|| (pid.equals(wireConfiguration.getReceiverPid())))) {
				this.m_wireService.deleteWireConfiguration(wireConfiguration);
			}
		}
	}

}

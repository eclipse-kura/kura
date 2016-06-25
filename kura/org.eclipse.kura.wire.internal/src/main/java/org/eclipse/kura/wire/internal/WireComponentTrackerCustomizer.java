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

import java.util.List;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.localization.LocalizationAdapter;
import org.eclipse.kura.localization.WireMessages;
import org.eclipse.kura.wire.WireComponent;
import org.eclipse.kura.wire.WireEmitter;
import org.eclipse.kura.wire.WireReceiver;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

/**
 * The Class WireSeviceTracker represents a OSGi service tracker to track Wire
 * Components (Wire Receiver and Wire Emitter)
 */
final class WireComponentTrackerCustomizer implements ServiceTrackerCustomizer<WireComponent, WireComponent> {

	/** The Logger instance. */
	private static final Logger s_logger = LoggerFactory.getLogger(WireComponentTrackerCustomizer.class);

	/** Localization Resource */
	private static final WireMessages s_message = LocalizationAdapter.adapt(WireMessages.class);

	/** Bundle Context */
	private final BundleContext m_context;

	/** The wire emitters. */
	private final List<String> m_wireEmitters;

	/** The wire receivers. */
	private final List<String> m_wireReceivers;

	/** The wire service. */
	private final WireServiceImpl m_wireService;

	/**
	 * Instantiates a new wire service tracker.
	 *
	 * @param context
	 *            the bundle context
	 * @param wireService
	 *            the wire service
	 * @throws InvalidSyntaxException
	 *             the invalid syntax exception
	 */
	WireComponentTrackerCustomizer(final BundleContext context, final WireServiceImpl wireService)
			throws InvalidSyntaxException {
		this.m_wireEmitters = Lists.newArrayList();
		this.m_wireReceivers = Lists.newArrayList();
		this.m_wireService = wireService;
		this.m_context = context;
	}

	/** {@inheritDoc} */
	@Override
	public WireComponent addingService(final ServiceReference<WireComponent> reference) {
		final WireComponent service = this.m_context.getService(reference);
		s_logger.debug(s_message.removingWireComponent());
		this.m_context.ungetService(reference);
		boolean flag = false;
		final String property = (String) reference.getProperty(WireUtils.SERVICE_PID_PROPERTY);
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
				s_logger.error(s_message.errorCreatingWires() + Throwables.getStackTraceAsString(e));
			}
		}
		s_logger.info(s_message.addingWireComponentDone());
		return service;
	}

	/**
	 * Gets the wire emitters.
	 *
	 * @return the wire emitters
	 */
	public List<String> getWireEmitters() {
		return ImmutableList.copyOf(this.m_wireEmitters);
	}

	/**
	 * Gets the wire receivers.
	 *
	 * @return the wire receivers
	 */
	public List<String> getWireReceivers() {
		return ImmutableList.copyOf(this.m_wireReceivers);
	}

	/** {@inheritDoc} */
	@Override
	public void modifiedService(final ServiceReference<WireComponent> reference, final WireComponent service) {
		this.removedService(reference, service);
		this.addingService(reference);
	}

	/** {@inheritDoc} */
	@Override
	public void removedService(final ServiceReference<WireComponent> reference, final WireComponent service) {
		s_logger.debug(s_message.addingWireComponent());
		this.m_context.ungetService(reference);
		boolean flag = false;
		final String property = (String) reference.getProperty(WireUtils.SERVICE_PID_PROPERTY);
		if (service instanceof WireEmitter) {
			this.m_wireEmitters.remove(property);
			s_logger.debug(s_message.deregisteringEmitter(property));
			flag = true;
		}
		if (service instanceof WireReceiver) {
			this.m_wireReceivers.remove(property);
			s_logger.debug(s_message.deregisteringReceiver(property));
			flag = true;
		}
		if (flag) {
			this.m_wireService.removeWireComponent(property);
		}
		s_logger.info(s_message.addingWireComponentDone());
	}

}

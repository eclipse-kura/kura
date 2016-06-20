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

import java.util.Collection;
import java.util.List;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.wire.WireEmitter;
import org.eclipse.kura.wire.WireReceiver;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

/**
 * The Class WireSeviceTracker represents a OSGi service tracker to track Wire
 * Components (Wire Receiver and Wire Emitter)
 */
public final class WireSeviceTracker extends ServiceTracker<Object, Object> {

	/** The Logger instance. */
	private static final Logger s_logger = LoggerFactory.getLogger(WireSeviceTracker.class);

	/** Service PID Constant */
	private static final String SERVICE_PID = "service.pid";

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
	public WireSeviceTracker(final BundleContext context, final WireServiceImpl wireService)
			throws InvalidSyntaxException {
		super(context, context.createFilter("(" + Constants.OBJECTCLASS + "=*)"), null);
		this.m_wireEmitters = Lists.newArrayList();
		this.m_wireReceivers = Lists.newArrayList();
		this.m_wireService = wireService;
	}

	/** {@inheritDoc} */
	@Override
	public Object addingService(final ServiceReference<Object> reference) {
		s_logger.debug("Adding Wire Components....");
		final Object service = super.addingService(reference);
		boolean flag = false;
		final String property = (String) reference.getProperty(SERVICE_PID);
		if (service instanceof WireEmitter) {
			this.m_wireEmitters.add(property);
			s_logger.debug("Registering Wire Emitter..." + property);
			flag = true;
		}
		if (service instanceof WireReceiver) {
			this.m_wireReceivers.add(property);
			s_logger.debug("Registering Wire Receiver..." + property);
			flag = true;
		}
		if (flag) {
			try {
				this.m_wireService.createWires();
			} catch (final KuraException e) {
				s_logger.error("Error while creating wires..." + Throwables.getStackTraceAsString(e));
			}
		}
		s_logger.info("Adding Wire Components....Done");
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
	public void open() {
		s_logger.debug("Starting to track Wire Components....");
		super.open();
		try {
			final Collection<ServiceReference<WireEmitter>> emitterRefs = this.context
					.getServiceReferences(WireEmitter.class, null);
			for (final ServiceReference<?> ref : emitterRefs) {
				this.m_wireEmitters.add((String) ref.getProperty(SERVICE_PID));
			}
			final Collection<ServiceReference<WireReceiver>> receiverRefs = this.context
					.getServiceReferences(WireReceiver.class, null);
			for (final ServiceReference<?> ref : receiverRefs) {
				this.m_wireReceivers.add((String) ref.getProperty(SERVICE_PID));
			}
		} catch (final InvalidSyntaxException e) {
			Throwables.propagate(e);
		}
		s_logger.debug("Starting to track Wire Components....Done");
	}

}

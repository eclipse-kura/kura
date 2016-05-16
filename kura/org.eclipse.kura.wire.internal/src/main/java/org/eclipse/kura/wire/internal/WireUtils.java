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
 */
package org.eclipse.kura.wire.internal;

import static org.eclipse.kura.device.internal.DevicePreconditions.checkCondition;

import java.util.Collection;
import java.util.List;

import org.eclipse.kura.KuraRuntimeException;
import org.eclipse.kura.wire.WireEmitter;
import org.eclipse.kura.wire.WireReceiver;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;

/**
 * The Class WireUtil comprises all necessary utility methods of Kura Wires
 * Topology
 */
public final class WireUtils {

	/** The Constant s_logger. */
	private static final Logger s_logger = LoggerFactory.getLogger(WireUtils.class);

	/** Service Property */
	private static final String SERVICE_PID_PROPERTY = "service.pid";

	/**
	 * Gets the wire emitters and receivers.
	 *
	 * @param ctx
	 *            the bundle context
	 * @return the wire emitters and receivers
	 * @throws KuraRuntimeException
	 *             if provided component context is null
	 */
	public static List<String> getEmittersAndReceivers(final ComponentContext ctx) {
		checkCondition(ctx == null, "Component context cannot be null");

		final List<String> result = Lists.newArrayList();
		try {
			final Collection<ServiceReference<WireEmitter>> emitters = ctx.getBundleContext()
					.getServiceReferences(WireEmitter.class, null);
			for (final ServiceReference<WireEmitter> service : emitters) {
				result.add(service.getProperty(SERVICE_PID_PROPERTY).toString());
			}
			final Collection<ServiceReference<WireReceiver>> consumers = ctx.getBundleContext()
					.getServiceReferences(WireReceiver.class, null);
			for (final ServiceReference<WireReceiver> service : consumers) {
				result.add(service.getProperty(SERVICE_PID_PROPERTY).toString());
			}
		} catch (final InvalidSyntaxException e) {
			s_logger.error("Invalid Syntax during Service Lookup..." + Throwables.getStackTraceAsString(e));
		}
		return result;
	}

	/**
	 * Gets the factories and instances.
	 *
	 * @param ctx
	 *            the service component context
	 * @param factoryPid
	 *            the factory pid
	 * @param iface
	 *            the interface
	 * @return the factories and instances
	 * @throws KuraRuntimeException
	 *             if any of the provided argument is null
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static List<String> getFactoriesAndInstances(final ComponentContext ctx, final String factoryPid,
			final Class iface) {
		checkCondition(ctx == null, "Component context cannot be null");
		checkCondition(factoryPid == null, "Factory PID cannot be null");
		checkCondition(iface == null, "Interface class cannot be null");

		final List<String> result = Lists.newArrayList();
		// Iterate through the bundles
		for (final Bundle b : ctx.getBundleContext().getBundles()) {
			// Search for a possible candidate for the factoryPid
			if (factoryPid.startsWith(b.getSymbolicName())) {
				// Try instantiating the factory. If it fails, move on to next
				// iteration
				try {
					final ClassLoader cl = b.adapt(BundleWiring.class).getClassLoader();
					final Class<?> clazz = Class.forName(factoryPid, false, cl);
					// If it doesn't fail introspect for the interface
					if (iface.isAssignableFrom(clazz)) {
						// Found a class implementing the interface.
						result.add("FACTORY|" + factoryPid);
					} else {
						// Found the class, but it doesn't implement the
						// interface.
						// Probably another multiton component.
						break;
					}
				} catch (final ClassNotFoundException e) {
					s_logger.error("Internal Error..." + Throwables.getStackTraceAsString(e));
				}
			}
		}

		// After the factories, iterate through available services implementing
		// the passed interface
		try {
			final Collection<ServiceReference<?>> services = ctx.getBundleContext().getServiceReferences(iface, null);
			for (final ServiceReference<?> service : services) {
				result.add("INSTANCE|" + service.getProperty(SERVICE_PID_PROPERTY));
			}
		} catch (final InvalidSyntaxException e) {
			s_logger.error("Invalid Syntax during Service Lookup...." + Throwables.getStackTraceAsString(e));
		}
		return result;
	}

	/**
	 * Checks if it is a Wire emitter.
	 *
	 * @param ctx
	 *            the bundle context
	 * @param name
	 *            the name
	 * @return true, if it is a Wire Emitter
	 * @throws KuraRuntimeException
	 *             if any of the provided argument is null
	 */
	public static boolean isEmitter(final BundleContext ctx, final String name) {
		checkCondition(ctx == null, "Bundle context cannot be null");
		checkCondition(name == null, "Wire Emitter name cannot be null");

		try {
			final Collection<ServiceReference<WireEmitter>> services = ctx.getServiceReferences(WireEmitter.class,
					null);
			for (final ServiceReference<?> service : services) {
				if (service.getProperty(SERVICE_PID_PROPERTY).equals(name)) {
					return true;
				}
			}
		} catch (final InvalidSyntaxException e) {
			s_logger.error("Invalid Syntax during Service Lookup....." + Throwables.getStackTraceAsString(e));
		}
		return false;
	}

	/**
	 * Checks if it is a Wire Receiver.
	 *
	 * @param ctx
	 *            the bundle context
	 * @param pid
	 *            the wire receiver pid
	 * @return true, if it is a Wire Receiver
	 * @throws KuraRuntimeException
	 *             if any of the provided argument is null
	 */
	public static boolean isReceiver(final BundleContext ctx, final String pid) {
		checkCondition(ctx == null, "Bundle context cannot be null");
		checkCondition(pid == null, "Wire Receiver PID cannot be null");

		try {
			final Collection<ServiceReference<WireReceiver>> services = ctx.getServiceReferences(WireReceiver.class,
					null);
			for (final ServiceReference<?> service : services) {
				if (service.getProperty(SERVICE_PID_PROPERTY).equals(pid)) {
					return true;
				}
			}
		} catch (final InvalidSyntaxException e) {
			s_logger.error("Invalid Syntax during Service Lookup......" + Throwables.getStackTraceAsString(e));
		}
		return false;
	}

	/**
	 * Instantiates a new wire utils.
	 */
	private WireUtils() {
		// Not needed because it is a utility class
	}

}

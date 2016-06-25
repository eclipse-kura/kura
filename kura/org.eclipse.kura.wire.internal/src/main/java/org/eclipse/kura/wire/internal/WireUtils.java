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

import static org.eclipse.kura.Preconditions.checkNull;

import java.util.Collection;
import java.util.List;

import org.eclipse.kura.KuraRuntimeException;
import org.eclipse.kura.localization.LocalizationAdapter;
import org.eclipse.kura.localization.WireMessages;
import org.eclipse.kura.wire.WireEmitter;
import org.eclipse.kura.wire.WireReceiver;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.component.ComponentContext;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;

/**
 * The Class WireUtils comprises all necessary utility methods of Kura Wires
 * Topology
 */
final class WireUtils {

	/** Localization Resource */
	private static final WireMessages s_messages = LocalizationAdapter.adapt(WireMessages.class);

	/** Service Property */
	public static final String SERVICE_PID_PROPERTY = "service.pid";
	
	/** Constructor */
	private WireUtils() {
		// Static Factory Methods container. No need to instantiate.
	}

	/**
	 * Gets the wire emitters and receivers.
	 *
	 * @param context
	 *            the bundle context
	 * @return the wire emitters and receivers
	 * @throws KuraRuntimeException
	 *             if provided component context is null
	 */
	static List<String> getEmittersAndReceivers(final ComponentContext context) {
		checkNull(context, s_messages.componentContextNonNull());
		final List<String> result = Lists.newArrayList();
		try {
			final Collection<ServiceReference<WireEmitter>> emitters = context.getBundleContext()
					.getServiceReferences(WireEmitter.class, null);
			for (final ServiceReference<WireEmitter> service : emitters) {
				result.add(service.getProperty(SERVICE_PID_PROPERTY).toString());
			}
			final Collection<ServiceReference<WireReceiver>> consumers = context.getBundleContext()
					.getServiceReferences(WireReceiver.class, null);
			for (final ServiceReference<WireReceiver> service : consumers) {
				result.add(service.getProperty(SERVICE_PID_PROPERTY).toString());
			}
		} catch (final InvalidSyntaxException e) {
			Throwables.propagate(e);
		}
		return result;
	}

	/**
	 * Gets the factories and instances.
	 *
	 * @param context
	 *            the service component context
	 * @param factoryPid
	 *            the factory PID
	 * @param iface
	 *            the interface
	 * @return the factories and instances
	 * @throws KuraRuntimeException
	 *             if any of the provided argument is null
	 */
	static List<String> getFactoriesAndInstances(final ComponentContext context, final String factoryPid,
			final Class<?> iface) {
		checkNull(context, s_messages.componentContextNonNull());
		checkNull(factoryPid, s_messages.factoryPidNonNull());
		checkNull(iface, s_messages.interfaceClassNonNull());

		final List<String> result = Lists.newArrayList();
		// Iterate through the bundles
		for (final Bundle b : context.getBundleContext().getBundles()) {
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
						// interface. Probably another multiton component.
						break;
					}
				} catch (final ClassNotFoundException e) {
					Throwables.propagate(e);
				}
			}
		}
		// After the factories, iterate through available services implementing
		// the passed interface
		try {
			final Collection<?> services = context.getBundleContext().getServiceReferences(iface, null);
			for (final Object service : services) {
				if (service instanceof ServiceReference) {
					final ServiceReference<?> reference = (ServiceReference<?>) service;
					result.add("INSTANCE|" + reference.getProperty(SERVICE_PID_PROPERTY));
				}
			}
		} catch (final InvalidSyntaxException e) {
			Throwables.propagate(e);
		}
		return result;
	}

	/**
	 * Checks if the provided name corresponds to any Wire emitter.
	 *
	 * @param context
	 *            the bundle context
	 * @param name
	 *            the name
	 * @return true, if it is a Wire Emitter
	 * @throws KuraRuntimeException
	 *             if any of the provided argument is null
	 */
	static boolean isEmitter(final BundleContext context, final String name) {
		checkNull(context, s_messages.bundleContextNonNull());
		checkNull(name, s_messages.emitterNameNonNull());

		try {
			final Collection<ServiceReference<WireEmitter>> services = context.getServiceReferences(WireEmitter.class,
					null);
			for (final ServiceReference<?> service : services) {
				if (service.getProperty(SERVICE_PID_PROPERTY).equals(name)) {
					return true;
				}
			}
		} catch (final InvalidSyntaxException e) {
			Throwables.propagate(e);
		}
		return false;
	}

	/**
	 * Checks if the provided name corresponds to any Wire receiver.
	 *
	 * @param context
	 *            the bundle context
	 * @param pid
	 *            the wire receiver PID
	 * @return true, if it is a Wire Receiver
	 * @throws KuraRuntimeException
	 *             if any of the provided argument is null
	 */
	static boolean isReceiver(final BundleContext context, final String pid) {
		checkNull(context, s_messages.bundleContextNonNull());
		checkNull(pid, s_messages.receiverNameNonNull());

		try {
			final Collection<ServiceReference<WireReceiver>> services = context.getServiceReferences(WireReceiver.class,
					null);
			for (final ServiceReference<?> service : services) {
				if (service.getProperty(SERVICE_PID_PROPERTY).equals(pid)) {
					return true;
				}
			}
		} catch (final InvalidSyntaxException e) {
			Throwables.propagate(e);
		}
		return false;
	}

}

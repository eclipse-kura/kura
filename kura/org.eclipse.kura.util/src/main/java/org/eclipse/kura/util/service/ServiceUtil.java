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
package org.eclipse.kura.util.service;

import static org.eclipse.kura.Preconditions.checkNull;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraRuntimeException;
import org.eclipse.kura.localization.LocalizationAdapter;
import org.eclipse.kura.localization.resources.UtilMessages;
import org.eclipse.kura.util.base.ThrowableUtil;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

/**
 * The Class ServiceUtil contains all necessary static factory methods to deal
 * with OSGi services
 */
public final class ServiceUtil {

	/** Localization Resource. */
	private static final UtilMessages s_message = LocalizationAdapter.adapt(UtilMessages.class);

	/** Constructor */
	private ServiceUtil() {
		// Static Factory Methods container. No need to instantiate.
	}

	/**
	 * Returns references to <em>all</em> services matching the given class name
	 * and OSGi filter.
	 *
	 * @param bundleContext
	 *            OSGi bundle context
	 * @param clazz
	 *            fully qualified class name (can be <code>null</code>)
	 * @param filter
	 *            valid OSGi filter (can be <code>null</code>)
	 * @return non-<code>null</code> array of references to matching services
	 * @throws KuraRuntimeException
	 *             if the filter syntax is wrong (even though filter is
	 *             nullable) or bundle syntax or class instance name is null
	 */
	public static <T> ServiceReference<T>[] getServiceReferences(final BundleContext bundleContext,
			final Class<T> clazz, final String filter) {
		checkNull(bundleContext, s_message.bundleContextNonNull());
		checkNull(bundleContext, s_message.clazzNonNull());

		try {
			final ServiceReference<?>[] refs = bundleContext.getServiceReferences(clazz.getName(), filter);
			@SuppressWarnings("unchecked")
			final ServiceReference<T>[] reference = (refs == null ? new ServiceReference[0] : refs);
			return reference;
		} catch (final InvalidSyntaxException ise) {
			throw new KuraRuntimeException(KuraErrorCode.INTERNAL_ERROR, ThrowableUtil.stackTraceAsString(ise));
		}
	}

}

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
import static org.osgi.framework.Constants.SERVICE_PID;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraRuntimeException;
import org.eclipse.kura.localization.LocalizationAdapter;
import org.eclipse.kura.localization.resources.WireMessages;
import org.eclipse.kura.util.base.ThrowableUtil;
import org.eclipse.kura.wire.WireComponent;
import org.eclipse.kura.wire.WireHelperService;
import org.eclipse.kura.wire.WireSupport;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

/**
 * The Class WireHelperServiceImpl is the implementation of
 * {@link WireHelperService}
 */
public final class WireHelperServiceImpl implements WireHelperService {

	/** Localization Resource */
	private static final WireMessages s_message = LocalizationAdapter.adapt(WireMessages.class);

	/** {@inheritDoc} */
	@Override
	public String getPid(final WireComponent wireComponent) {
		checkNull(wireComponent, s_message.wireComponentNonNull());
		final BundleContext context = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
		final ServiceReference<?>[] refs = this.getServiceReferences(context, WireComponent.class.getName(), null);
		for (final ServiceReference<?> ref : refs) {
			final WireComponent wc = (WireComponent) context.getService(ref);
			if (wc == wireComponent) {
				return ref.getProperty(KURA_SERVICE_PID).toString();
			}
		}
		return null;
	}

	/** {@inheritDoc} */
	@Override
	public String getServicePid(final String wireComponentPid) {
		checkNull(wireComponentPid, s_message.wireComponentPidNonNull());
		final BundleContext context = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
		final ServiceReference<?>[] refs = this.getServiceReferences(context, WireComponent.class.getName(), null);
		for (final ServiceReference<?> ref : refs) {
			if (ref.getProperty(KURA_SERVICE_PID).equals(wireComponentPid)) {
				return ref.getProperty(SERVICE_PID).toString();
			}
		}
		return null;
	}

	/** {@inheritDoc} */
	@Override
	public String getServicePid(final WireComponent wireComponent) {
		checkNull(wireComponent, s_message.wireComponentNonNull());
		final BundleContext context = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
		final ServiceReference<?>[] refs = this.getServiceReferences(context, WireComponent.class.getName(), null);
		for (final ServiceReference<?> ref : refs) {
			final WireComponent wc = (WireComponent) context.getService(ref);
			if (wc == wireComponent) {
				return ref.getProperty(SERVICE_PID).toString();
			}
		}
		return null;
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
	private ServiceReference<?>[] getServiceReferences(final BundleContext bundleContext, final String clazz,
			final String filter) {
		checkNull(bundleContext, s_message.bundleContextNonNull());
		checkNull(bundleContext, s_message.clazzNonNull());

		try {
			final ServiceReference<?>[] refs = bundleContext.getServiceReferences(clazz, filter);
			return refs == null ? new ServiceReference[0] : refs;
		} catch (final InvalidSyntaxException ise) {
			throw new KuraRuntimeException(KuraErrorCode.INTERNAL_ERROR, ThrowableUtil.stackTraceAsString(ise));
		}
	}

	/** {@inheritDoc} */
	@Override
	public WireSupport newWireSupport(final WireComponent wireComponent) {
		return new WireSupportImpl(wireComponent, this);
	}

}

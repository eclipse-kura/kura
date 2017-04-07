/*******************************************************************************
 * Copyright (c) 2016, 2017 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Eurotech
 *  Amit Kumar Mondal
 *
 *******************************************************************************/
package org.eclipse.kura.internal.wire.helper;

import static java.util.Objects.requireNonNull;
import static org.eclipse.kura.configuration.ConfigurationService.KURA_SERVICE_PID;
import static org.osgi.framework.Constants.SERVICE_PID;

import java.util.Collection;
import java.util.Optional;

import org.eclipse.kura.localization.LocalizationAdapter;
import org.eclipse.kura.localization.resources.WireMessages;
import org.eclipse.kura.util.service.ServiceSupplier;
import org.eclipse.kura.util.service.ServiceUtil;
import org.eclipse.kura.wire.WireComponent;
import org.eclipse.kura.wire.WireEmitter;
import org.eclipse.kura.wire.WireHelperService;
import org.eclipse.kura.wire.WireReceiver;
import org.eclipse.kura.wire.WireSupport;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.EventAdmin;

/**
 * The Class {@link WireHelperServiceImpl} is the implementation of
 * {@link WireHelperService}
 */
public final class WireHelperServiceImpl implements WireHelperService {

	/** Localization instance */
	private static final WireMessages wireMessages = LocalizationAdapter.adapt(WireMessages.class);

	private volatile EventAdmin eventAdmin;

	/**
	 * Binds the {@link EventAdmin} service instance
	 *
	 * @param eventAdmin
	 *            the new {@link EventAdmin} service instance
	 */
	public void bindEventAdmin(final EventAdmin eventAdmin) {
		if (this.eventAdmin == null) {
			this.eventAdmin = eventAdmin;
		}
	}

	/**
	 * Unbinds the {@link EventAdmin} service instance
	 *
	 * @param eventAdmin
	 *            the {@link EventAdmin} service instance
	 */
	public void unbindEventAdmin(final EventAdmin eventAdmin) {
		if (this.eventAdmin == eventAdmin) {
			this.eventAdmin = null;
		}
	}

	/** {@inheritDoc} */
	@Override
	public Optional<String> getPid(final WireComponent wireComponent) {
		requireNonNull(wireComponent, wireMessages.wireComponentNonNull());
		final Collection<ServiceReference<WireComponent>> refs = ServiceUtil.getServiceReferences(WireComponent.class,
				null);
		for (final ServiceReference<WireComponent> ref : refs) {
			try (ServiceSupplier<WireComponent> wcRef = ServiceSupplier.supply(ref)) {
				final Optional<WireComponent> wcOptional = wcRef.get().findFirst();
				return wcOptional.filter(c -> c == wireComponent)
						.map(r -> ref.getProperty(KURA_SERVICE_PID).toString());
			}
		}
		return Optional.empty();
	}

	/** {@inheritDoc} */
	@Override
	public Optional<String> getServicePid(final String wireComponentPid) {
		requireNonNull(wireComponentPid, wireMessages.wireComponentPidNonNull());
		final Collection<ServiceReference<WireComponent>> refs = ServiceUtil.getServiceReferences(WireComponent.class,
				null);
		for (final ServiceReference<WireComponent> ref : refs) {
			if (ref.getProperty(KURA_SERVICE_PID).equals(wireComponentPid)) {
				return Optional.of(ref.getProperty(SERVICE_PID).toString());
			}
		}
		return Optional.empty();
	}

	/** {@inheritDoc} */
	@Override
	public Optional<String> getServicePid(final WireComponent wireComponent) {
		requireNonNull(wireComponent, wireMessages.wireComponentNonNull());
		final Collection<ServiceReference<WireComponent>> refs = ServiceUtil.getServiceReferences(WireComponent.class,
				null);
		for (final ServiceReference<WireComponent> ref : refs) {
			try (ServiceSupplier<WireComponent> wcRef = ServiceSupplier.supply(ref)) {
				final Optional<WireComponent> wcOptional = wcRef.get().findFirst();
				return wcOptional.filter(c -> c == wireComponent).map(r -> ref.getProperty(SERVICE_PID).toString());
			}
		}
		return Optional.empty();
	}

	/** {@inheritDoc} */
	@Override
	public boolean isEmitter(final String wireComponentPid) {
		requireNonNull(wireComponentPid, wireMessages.wireComponentPidNonNull());
		final String filter = "(" + KURA_SERVICE_PID + "=" + wireComponentPid + ")";
		try (ServiceSupplier<WireComponent> supplier = ServiceSupplier.supply(WireComponent.class, filter)) {
			final Optional<WireComponent> wcOptional = supplier.get().findFirst();
			return wcOptional.filter(c -> c instanceof WireEmitter).map(r -> true).get();
		}
	}

	/** {@inheritDoc} */
	@Override
	public boolean isReceiver(final String wireComponentPid) {
		requireNonNull(wireComponentPid, wireMessages.wireComponentPidNonNull());
		final String filter = "(" + KURA_SERVICE_PID + "=" + wireComponentPid + ")";
		try (ServiceSupplier<WireComponent> supplier = ServiceSupplier.supply(WireComponent.class, filter)) {
			final Optional<WireComponent> wcOptional = supplier.get().findFirst();
			return wcOptional.filter(c -> c instanceof WireReceiver).map(r -> true).get();
		}
	}

	/** {@inheritDoc} */
	@Override
	public WireSupport newWireSupport(final WireComponent wireComponent) {
		return new WireSupportImpl(wireComponent, this, eventAdmin);
	}
}

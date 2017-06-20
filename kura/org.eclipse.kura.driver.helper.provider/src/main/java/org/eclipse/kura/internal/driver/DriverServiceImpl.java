/*******************************************************************************
 * Copyright (c) 2016, 2017 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *     Red Hat Inc
 *     Amit Kumar Mondal
 *******************************************************************************/
package org.eclipse.kura.internal.driver;

import static java.util.Objects.requireNonNull;
import static org.eclipse.kura.configuration.ConfigurationService.KURA_SERVICE_PID;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.kura.driver.Driver;
import org.eclipse.kura.driver.DriverService;
import org.eclipse.kura.localization.LocalizationAdapter;
import org.eclipse.kura.localization.resources.AssetMessages;
import org.eclipse.kura.util.service.ServiceSupplier;
import org.eclipse.kura.util.service.ServiceUtil;
import org.osgi.framework.ServiceReference;

/**
 * The Class {@link DriverServiceImpl} is an implementation of the utility API
 * {@link DriverService} to provide useful factory methods for {@link Driver}s
 */
public final class DriverServiceImpl implements DriverService {

	/** Localization Resource */
	private static final AssetMessages message = LocalizationAdapter.adapt(AssetMessages.class);

	/** {@inheritDoc} */
	@Override
	public Optional<Driver> getDriver(final String driverPid) {
		requireNonNull(driverPid, message.driverPidNonNull());
		final String filter = "(" + KURA_SERVICE_PID + "=" + driverPid + ")";
		try (ServiceSupplier<Driver> driver = ServiceSupplier.supply(Driver.class, filter)) {
			return driver.get().findFirst();
		}
	}

	/** {@inheritDoc} */
	@Override
	public Optional<String> getDriverPid(final Driver driver) {
		requireNonNull(driver, message.driverNonNull());
		final Collection<ServiceReference<Driver>> refs = ServiceUtil.getServiceReferences(Driver.class, null);
		for (final ServiceReference<Driver> ref : refs) {
			try (ServiceSupplier<Driver> driverRef = ServiceSupplier.supply(ref)) {
				final Optional<Driver> driverOptional = driverRef.get().findFirst();
				return driverOptional.filter(c -> c == driver).map(r -> ref.getProperty(KURA_SERVICE_PID).toString());
			}
		}
		return Optional.empty();
	}

	/** {@inheritDoc} */
	@Override
	public List<Driver> listDrivers() {
		try (ServiceSupplier<Driver> driverRef = ServiceSupplier.supply(Driver.class, null)) {
			return driverRef.get().collect(Collectors.toList());
		}
	}

}

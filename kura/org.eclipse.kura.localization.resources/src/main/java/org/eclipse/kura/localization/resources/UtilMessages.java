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
package org.eclipse.kura.localization.resources;

import com.github.rodionmoiseev.c10n.annotations.En;

/**
 * {@link UtilMessages} is considered to be a localization resource for
 * {@code Utility} bundle. It contains all the necessary translations for every
 * string literals mentioned in {@code Utility} bundle.
 */
public interface UtilMessages {

	@En("Service supplier instance has already been closed.")
	public String alreadyClosed();

	@En("Bundle context cannot be null.")
	public String bundleContextNonNull();

	@En("Class intance name cannot be null.")
	public String clazzNonNull();

	@En("Exception while closing ServiceSupplier.")
	public String closeFailed();

	@En("Collection cannot be null.")
	public String collectionNonNull();

	@En("Delimiter cannot be null.")
	public String delimiterNonNull();

	@En("Dictionary cannot be null.")
	public String dictionaryNonNull();

	@En("Service instance cannot be retrieved")
	public String errorRetrievingService();

	@En("Filter cannot be null.")
	public String filterNonNull();

	@En("Initial array size must not be less than 0.")
	public String initialArraySize();

	@En("Iterable elements cannot be null.")
	public String iterableNonNull();

	@En("Map cannot be null.")
	public String mapNonNull();

	@En("Could not acquire bundle context for {0}")
	public String noBundleContext(String clazz);

	@En("Service reference cannot be null.")
	public String referenceNonNull();

	@En("Service references cannot be null.")
	public String referencesNonNull();

	@En("Target Class Instance cannot be null.")
	public String targetNonNull();

	@En("Timeout period cannot be zero or negative.")
	public String timeoutError();

	@En("TimeUnit cannot be null.")
	public String timeunitNonNull();

	@En("Value cannot be null.")
	public String valueNonNull();
}

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
package org.eclipse.kura.localization.resources;

import com.github.rodionmoiseev.c10n.annotations.En;

/**
 * UtilMessages is considered to be a localization resource for {@code Util}
 * bundle. It contains all the necessary translation for every string literals
 * mentioned in {@code Util} bundle.
 */
public interface UtilMessages {

	@En("Bundle context cannot be null")
	public String bundleContextNonNull();

	@En("Class intance name cannot be null")
	public String clazzNonNull();

	@En("Dictionary cannot be null")
	public String dictionaryNonNull();

	@En("Initial Array size must not be less than 0")
	public String initialArraySize();

	@En("Map cannot be null")
	public String mapNonNull();

	@En("Value cannot be null")
	public String valueNonNull();

}

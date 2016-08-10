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
package org.eclipse.kura.util.base;

import org.eclipse.kura.annotation.Nullable;

/**
 * The Class StringUtil contains all necessary static factory methods for
 * manipulating String instances
 */
public final class StringUtil {

	/** Constructor */
	private StringUtil() {
		// Static Factory Methods container. No need to instantiate.
	}

	/**
	 * Returns {@code true} if the given string is null or is the empty string.
	 *
	 * @param string
	 *            a string reference to check
	 * @return {@code true} if the string is null or is the empty string
	 */
	public static boolean isNullOrEmpty(@Nullable final String string) {
		return (string == null) || string.isEmpty();
	}

}

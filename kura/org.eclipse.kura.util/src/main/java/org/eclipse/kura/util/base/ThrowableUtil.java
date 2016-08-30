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

import java.io.PrintWriter;
import java.io.StringWriter;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraRuntimeException;

/**
 * The Class ThrowableUtil contains all necessary static factory methods for
 * manipulating Throwable instances
 */
public final class ThrowableUtil {

	/** Constructor */
	private ThrowableUtil() {
		// Static Factory Methods container. No need to instantiate.
	}

	/**
	 * Propagates a {@code throwable}.
	 *
	 * @param throwable
	 *            the throwable to propagate
	 */
	public static void propagate(final Throwable throwable) {
		throw new KuraRuntimeException(KuraErrorCode.INTERNAL_ERROR, throwable);
	}

	/**
	 * Returns a string containing the result of {@link Throwable#toString()},
	 * comprising recursive stack trace of {@code throwable}.
	 *
	 * @param throwable
	 *            The {@code throwable} instance
	 * @return the string
	 */
	public static String stackTraceAsString(final Throwable throwable) {
		final StringWriter sw = new StringWriter();
		final PrintWriter pw = new PrintWriter(sw);
		throwable.printStackTrace(pw);
		return sw.toString();
	}

}

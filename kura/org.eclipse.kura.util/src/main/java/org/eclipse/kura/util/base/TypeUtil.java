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

import static org.eclipse.kura.Preconditions.checkNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import org.eclipse.kura.KuraRuntimeException;
import org.eclipse.kura.localization.LocalizationAdapter;
import org.eclipse.kura.localization.resources.UtilMessages;

/**
 * The Class TypeUtil contains all necessary static factory methods for
 * manipulating java type instances
 */
public final class TypeUtil {

	/** Localization Resource. */
	private static final UtilMessages s_message = LocalizationAdapter.adapt(UtilMessages.class);

	/** Constructor */
	private TypeUtil() {
		// Static Factory Methods container. No need to instantiate.
	}

	/**
	 * Returns a byte array representation of the provided integer value.
	 *
	 * @param value
	 *            The provided integer value
	 * @return the byte array instance
	 */
	public static byte[] intToBytes(final int value) {
		final byte[] result = new byte[4];
		result[0] = (byte) (value >> 24);
		result[1] = (byte) (value >> 16);
		result[2] = (byte) (value >> 8);
		result[3] = (byte) (value);
		return result;
	}

	/**
	 * Converts to the byte array from the provided object instance
	 *
	 * @param value
	 * @return the byte array
	 * @throws IOException
	 *             if the access to byte stream fails
	 * @throws KuraRuntimeException
	 *             if the argument is null
	 */
	public static byte[] objectToByteArray(final Object value) throws IOException {
		checkNull(value, s_message.valueNonNull());
		final ByteArrayOutputStream b = new ByteArrayOutputStream();
		final ObjectOutputStream o = new ObjectOutputStream(b);
		o.writeObject(value);
		return b.toByteArray();
	}

}

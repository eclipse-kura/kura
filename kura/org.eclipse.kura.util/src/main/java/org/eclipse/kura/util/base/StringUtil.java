/*******************************************************************************
 * Copyright (c) 2016 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.util.base;

import static java.util.Objects.requireNonNull;

import java.util.Iterator;

import org.eclipse.kura.annotation.Nullable;
import org.eclipse.kura.localization.LocalizationAdapter;
import org.eclipse.kura.localization.resources.UtilMessages;

/**
 * The Class StringUtil contains all necessary static factory methods for
 * manipulating String instances
 */
public final class StringUtil {

    /** Localization Resource. */
    private static final UtilMessages s_message = LocalizationAdapter.adapt(UtilMessages.class);

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

    /**
     * Returns a string containing the tokens joined by delimiters.
     *
     * @param tokens
     *            an array objects to be joined. Strings will be formed from the
     *            objects by calling object.toString().
     * @throws NullPointerException
     *             if any of the arguments is null
     */
    public static String join(final CharSequence delimiter, final Iterable<?> tokens) {
        requireNonNull(delimiter, s_message.delimiterNonNull());
        requireNonNull(tokens, s_message.iterableNonNull());

        final StringBuilder sb = new StringBuilder();
        final Iterator<?> it = tokens.iterator();
        if (it.hasNext()) {
            sb.append(it.next());
            while (it.hasNext()) {
                sb.append(delimiter);
                sb.append(it.next());
            }
        }
        return sb.toString();
    }

}

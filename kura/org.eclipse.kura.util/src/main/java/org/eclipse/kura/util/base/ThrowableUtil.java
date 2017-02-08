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
package org.eclipse.kura.util.base;

import static java.util.Objects.requireNonNull;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.eclipse.kura.localization.LocalizationAdapter;
import org.eclipse.kura.localization.resources.UtilMessages;

/**
 * The Class ThrowableUtil contains all necessary static factory methods for
 * manipulating Throwable instances
 */
public final class ThrowableUtil {

    /** Localization Resource. */
    private static final UtilMessages message = LocalizationAdapter.adapt(UtilMessages.class);

    /** Constructor */
    private ThrowableUtil() {
        // Static Factory Methods container. No need to instantiate.
    }

    /**
     * Propagates a {@code throwable} instance if the throwable is instance of the provided type
     *
     * Usage:
     *
     * <pre>
     * <code>
     * Throwable throwable = null;
     *
     * try {
     *  ....
     * } catch (Exception1 | Exception2 | Exception3 e) {
     *   throwable = e;
     *   ThrowableUtil.propagateIfInstanceOf(e, Exception3.class);
     *   //if not an instance of Exception3
     *   doSomethingForException1OrException2();
     * }
     *
     * if (Objects.nonNull(throwable)) {
     *  //now only propagate if instance of Exception2
     *  ThrowableUtil.propagateIfInstanceOf(throwable, Exception2.class);
     * }
     * </code>
     * </pre>
     *
     * @param throwable
     *            the throwable to propagate
     * @param clazz
     *            the type instance to check
     * @throws NullPointerException
     *             if any of the provided arguments is null
     */
    public static <E extends Throwable> void propagateIfInstanceOf(final Throwable throwable, final Class<E> clazz)
            throws E {
        requireNonNull(throwable, message.throwableNonNull());
        requireNonNull(clazz, message.clazzNonNull());

        if (clazz.isInstance(throwable)) {
            throw clazz.cast(throwable);
        }
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

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
package org.eclipse.kura.localization;

import static java.util.Objects.requireNonNull;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import com.github.rodionmoiseev.c10n.C10N;
import com.github.rodionmoiseev.c10n.annotations.DefaultC10NAnnotations;

/**
 * The {@link LocalizationAdapter} is an utility class to adapt the message
 * resources to their {@code C10N} instance for internationalization
 *
 * The available locale options for resources are as follows:
 *
 * <ul>
 * <li>EN : English</li>
 * <li>DE : German</li>
 * <li>FR : French</li>
 * <li>IT : Italian</li>
 * <li>JA : Japanese</li>
 * <li>KO : Korean</li>
 * <li>RU : Russian</li>
 * <li>ZH : Chinese</li>
 * </ul>
 */
public final class LocalizationAdapter {

    private static final Locale CURRENT_LOCALE;

    static {
        C10N.configure(new DefaultC10NAnnotations());
        CURRENT_LOCALE = Locale.getDefault();
    }

    private LocalizationAdapter() {
        // Static Factory Methods container. No need to instantiate.
    }

    private static class C10NWrapper<T> implements InvocationHandler {

        private final T wrapped;
        private final Map<Method, String> cache = new HashMap<>();

        public C10NWrapper(final T wrapped) {
            this.wrapped = wrapped;
        }

        @Override
        public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
            if (args != null && args.length > 0) {
                return method.invoke(this.wrapped, args);
            }

            String cached = this.cache.get(method);
            if (cached != null) {
                return cached;
            }

            cached = (String) method.invoke(this.wrapped, args);

            this.cache.put(method, cached);
            return cached;
        }
    }

    /**
     * Adapt the provided message resource to its {@code C10N} type with the current locale.
     *
     * @param <T>
     *            the generic type
     * @param clazz
     *            the message resource
     * @throws NullPointerException
     *             if the argument is null
     * @return the instance of the {@code C10N} resource
     */
    public static <T> T adapt(final Class<T> clazz) {
        return adapt(clazz, CURRENT_LOCALE);
    }

    /**
     * Adapt the provided message resource to its {@code C10N} type with provided locale.
     *
     * @param <T>
     *            the generic type
     * @param clazz
     *            the message resource
     * @param locale
     *            the {@link Locale} instance to use
     * @throws NullPointerException
     *             if any of the arguments is null
     * @return the instance of the {@code C10N} resource adapting the specified {@link Locale} instance
     */
    @SuppressWarnings("unchecked")
    public static <T> T adapt(final Class<T> clazz, final Locale locale) {
        requireNonNull(clazz, "Class instance of localization resource cannot be null");
        requireNonNull(clazz, "Locale instance cannot be null");
        return (T) Proxy.newProxyInstance(clazz.getClassLoader(), new Class<?>[] { clazz },
                new C10NWrapper<>(C10N.get(clazz, locale)));
    }
}
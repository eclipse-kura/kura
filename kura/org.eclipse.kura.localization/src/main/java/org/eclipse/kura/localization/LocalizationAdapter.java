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

import com.github.rodionmoiseev.c10n.C10N;
import com.github.rodionmoiseev.c10n.annotations.DefaultC10NAnnotations;

/**
 * The Class LocalizationAdapter is an utility class to adapt the Message
 * resources to their C10N instance for internationalization
 *
 * The available locale options for resources are as follows (case-insensitive).
 *
 * <ul>
 * <li>EN</li> : English
 * <li>DE</li> : German
 * <li>FR</li> : French
 * <li>IT</li> : Italian
 * <li>JA</li> : Japanese
 * <li>KO</li> : Korean
 * <li>RU</li> : Russian
 * <li>ZH</li> : Chinese
 * </ul>
 *
 * The usage on changing locale: Setting system property of {@code nl} to one of
 * the aforementioned locale options
 */
public final class LocalizationAdapter {

    private static final Locale CURRENT_LOCALE;

    static {
        C10N.configure(new DefaultC10NAnnotations());
        CURRENT_LOCALE = Locale.getDefault();
    }

    /** Constructor */
    private LocalizationAdapter() {
        // Static Factory Methods container. No need to instantiate.
    }

    private static class C10NWrapper<T> implements InvocationHandler {

        private final T wrapped;
        private final HashMap<Method, String> cache = new HashMap<>();

        public C10NWrapper(T wrapped) {
            this.wrapped = wrapped;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (args != null && args.length > 0) {
                return method.invoke(wrapped, args);
            }

            String cached = cache.get(method);
            if (cached != null) {
                return cached;
            }

            cached = (String) method.invoke(wrapped, args);

            cache.put(method, cached);
            return cached;
        }
    }

    /**
     * Adapt the provided message resource to its C10N type
     *
     * @param <T>
     *            the generic type
     * @param clazz
     *            the message resource
     * @throws NullPointerException
     *             if the argument is null
     * @return the instance of the C10N resource
     */
    @SuppressWarnings("unchecked")
    public static <T> T adapt(final Class<T> clazz) {
        requireNonNull(clazz, "Class instance of localization resource cannot be null");
        return (T) Proxy.newProxyInstance(clazz.getClassLoader(), new Class<?>[] { clazz },
                new C10NWrapper<T>(C10N.get(clazz, CURRENT_LOCALE)));
    }
}
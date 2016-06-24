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
package org.eclipse.kura.localization;

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

	/** System property for localization */
	private static final String LOCALE_PROPERTY = "nl";

	static {
		C10N.configure(new DefaultC10NAnnotations());
	}
	
	/** Constructor */
	private LocalizationAdapter() {
		// Static Factory Methods container. No need to instantiate.
	}

	/**
	 * Adapt the provided message resource to its C10N type
	 *
	 * @param <T>
	 *            the generic type
	 * @param clazz
	 *            the message resource
	 * @return the instance of the C10N resource
	 */
	public static <T> T adapt(final Class<T> clazz) {
		return C10N.get(clazz, getSystemLocale());
	}

	/**
	 * Returns the locale as set in the system property
	 */
	private static Locale getSystemLocale() {
		final String locale = System.getProperty(LOCALE_PROPERTY);
		if (locale != null) {
			if ("EN".equalsIgnoreCase(locale)) {
				return Locale.ENGLISH;
			}
			if ("IT".equalsIgnoreCase(locale)) {
				return Locale.ITALIAN;
			}
			if ("DE".equalsIgnoreCase(locale)) {
				return Locale.GERMAN;
			}
			if ("FR".equalsIgnoreCase(locale)) {
				return Locale.FRENCH;
			}
			if ("JA".equalsIgnoreCase(locale)) {
				return Locale.JAPANESE;
			}
			if ("KO".equalsIgnoreCase(locale)) {
				return Locale.KOREAN;
			}
			if ("RU".equalsIgnoreCase(locale)) {
				return new Locale.Builder().setLanguage("ru").setRegion("RU").build();
			}
			if ("ZH".equalsIgnoreCase(locale)) {
				return Locale.CHINESE;
			}
		}
		return Locale.ENGLISH;
	}

}

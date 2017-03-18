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
 * UtilMessages is considered to be a localization resource for {@code Util}
 * bundle. It contains all the necessary translation for every string literals
 * mentioned in {@code Utility} bundle.
 */
public interface UtilMessages {

    @En("Bundle context cannot be null.")
    public String bundleContextNonNull();

    @En("Class intance name cannot be null.")
    public String clazzNonNull();

    @En("Collection cannot be null.")
    public String collectionNonNull();

    @En("Delimiter cannot be null.")
    public String delimiterNonNull();

    @En("Dictionary cannot be null.")
    public String dictionaryNonNull();

    @En("Filter cannot be null.")
    public String filterNonNull();

    @En("Initial Array size must not be less than 0.")
    public String initialArraySize();

    @En("Iterable elements cannot be null.")
    public String iterableNonNull();

    @En("Map cannot be null.")
    public String mapNonNull();

    @En("Service References cannot be null.")
    public String referencesNonNull();

    @En("Timeout period cannot be zero or negative")
    public String timeoutError();

    @En("TimeUnit cannot be null")
    public String timeunitNonNull();

    @En("Value cannot be null.")
    public String valueNonNull();
}

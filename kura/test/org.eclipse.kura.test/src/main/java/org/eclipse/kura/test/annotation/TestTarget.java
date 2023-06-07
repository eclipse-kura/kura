/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 *******************************************************************************/
package org.eclipse.kura.test.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface TestTarget {

    public static final String PLATFORM_ALL = "PLATFORM_ALL";

    public static final String PLATFORM_DURACOR = "duracor";
    public static final String PLATFORM_DYNACOR = "dynacor";
    public static final String PLATFORM_HELIOS = "helios";
    public static final String PLATFORM_MGW = "Mini-Gateway";
    public static final String PLATFORM_RASPBERRY_PI = "Raspberr-Pi";
    public static final String PLATFORM_RELIAGATE = "reliagate";

    public String[]targetPlatforms();
}

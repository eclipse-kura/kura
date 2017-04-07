/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
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

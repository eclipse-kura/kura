/*******************************************************************************
 * Copyright (c) 2016 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *******************************************************************************/
package org.eclipse.kura.annotation;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * This annotation denotes that the annotated type is thread-safe. This
 * inherently means that there should not be any sequence of operations followed
 * which could potentially render the instance of the annotated type into an
 * invalid state.
 * @since 1.2
 */
@Documented
@Target(TYPE)
@Retention(RUNTIME)
public @interface ThreadSafe {
}
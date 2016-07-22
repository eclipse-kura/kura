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
package org.eclipse.kura.annotation;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * The existence of this annotation indicates that the author believes the
 * annotated class to be thread-safe. As such, there should be no sequence of
 * accessing the public methods or fields that could put an instance of this
 * class into an invalid state, irrespective of any rearrangement of those
 * operations by the Java Runtime and without introducing any requirements for
 * synchronization or coordination by the caller.
 */
@Documented
@Target(TYPE)
@Retention(RUNTIME)
public @interface ThreadSafe {
}
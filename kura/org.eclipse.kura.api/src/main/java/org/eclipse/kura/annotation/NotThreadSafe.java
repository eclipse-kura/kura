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
 * The existence of this annotation indicates that the author believes the class
 * is not thread-safe. The absence of this annotation does not indicate that the
 * class is thread-safe, instead this annotation is for cases where a naive
 * assumption could be easily made that the class is thread-safe. In general, it
 * is a bad plan to assume a class is thread safe without any good reason.
 */
@Documented
@Target(TYPE)
@Retention(RUNTIME)
public @interface NotThreadSafe {
}
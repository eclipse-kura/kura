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
 * This annotation denotes that the annotated type is not thread-safe. On the
 * contrary, if any type is not annotated with this annotation, does not
 * necessarily indicate that the type in consideration is thread-safe. The
 * motive of this annotation is to inform consumers of the annotated type that
 * consumers must not make any abrupt assumption that the type is thread-safe.
 * If the author of the type believes that the consumer can make such
 * assumptions of thread-safety for types even though the types are not, it is
 * better to annotate the type with this annotation.
 * @since 1.2
 */
@Documented
@Target(TYPE)
@Retention(RUNTIME)
public @interface NotThreadSafe {
}
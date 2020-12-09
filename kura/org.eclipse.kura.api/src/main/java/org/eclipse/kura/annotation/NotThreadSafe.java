/*******************************************************************************
 * Copyright (c) 2016, 2020 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 ******************************************************************************/
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
 *
 * @since 1.2
 */
@Documented
@Target(TYPE)
@Retention(RUNTIME)
public @interface NotThreadSafe {
}

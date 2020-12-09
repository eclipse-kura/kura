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
 * This annotation denotes that the annotated type is immutable and eventually
 * it becomes thread-safe. By definition, a class is considered to be an
 * immutable class in which the state of its instance cannot be <i>observed</i>
 * to be changed. So, inherently this implies that,
 * <ul>
 * <li>All of its public fields must be declared as {@code final}</li>
 * <li>All of its public final reference fields are either {@code null} or refer
 * to other immutable objects</li>
 * <li>Constructors and methods do not contain references to any potentially
 * mutable internal state.</li>
 * </ul>
 * <p/>
 * In addition, the immutable objects are inherently thread-safe and that is the
 * reason, they can be passed between threads or published without explicit
 * synchronization or locks.
 *
 * @since 1.2
 */
@Documented
@Target(TYPE)
@Retention(RUNTIME)
public @interface Immutable {
}

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
 * to be immutable and hence inherently thread-safe. An immutable class is one
 * where the state of an instance cannot be <i>seen</i> to change. As a result
 * <ul>
 * <li>All public fields must be {@code final}</li>
 * <li>All public final reference fields are either {@code null} or refer to
 * other immutable objects</li>
 * <li>Constructors and methods do not publish references to any potentially
 * mutable internal state.</li>
 * </ul>
 * Performance optimization may mean that instances of an immutable class may
 * have mutable internal state. The critical point is that callers cannot tell
 * the difference. For example {@link String} is an immutable class, despite
 * having an internal integer that is non-final but used as a cache for
 * {@link String#hashCode()}.
 * <p/>
 * Immutable objects are inherently thread-safe; they may be passed between
 * threads or published without synchronization.
 */
@Documented
@Target(TYPE)
@Retention(RUNTIME)
public @interface Immutable {
}
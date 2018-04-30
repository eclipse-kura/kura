/*******************************************************************************
 * Copyright (c) 2018 Red Hat Inc and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Inc - Initial API and implementation
 *******************************************************************************/
package org.eclipse.kura.camel.component;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import org.apache.camel.CamelContext;

/**
 * Provide a way to interact with Camel context instances.
 */
public interface CamelContextWorker {

    /**
     * Get a Camel context from a component.
     * 
     * @return The Camel context or {@code null} if there currently is none.
     */
    public CamelContext getCamelContext();

    /**
     * Run an operation of the Camel context is present.
     * 
     * @param operation
     *            The operation to execute if the Camel context is not {@code null}.
     */
    public default void withCamelContext(final Consumer<CamelContext> operation) {
        Objects.requireNonNull(operation);

        final CamelContext context = getCamelContext();
        if (context != null) {
            operation.accept(context);
        }
    }

    /**
     * Run an operation with an optional Camel context.
     * 
     * @param operation
     *            The operation to execute. If the camel context is not present, the Optional will be <em>empty</em>.
     *            But it will never be {@code null}.
     */
    public default void withOptionalCamelContext(final Consumer<Optional<CamelContext>> operation) {
        Objects.requireNonNull(operation);

        operation.accept(Optional.ofNullable(getCamelContext()));
    }
}

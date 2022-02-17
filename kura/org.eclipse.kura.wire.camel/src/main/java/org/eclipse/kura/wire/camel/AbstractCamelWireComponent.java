/*******************************************************************************
 * Copyright (c) 2018, 2022 Red Hat Inc and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Red Hat Inc
 *  heyoulin <heyoulin@gmail.com>
 *******************************************************************************/
package org.eclipse.kura.wire.camel;

import java.util.Map;
import java.util.function.Consumer;

import org.apache.camel.CamelContext;
import org.eclipse.kura.camel.component.Configuration;
import org.eclipse.kura.util.osgi.FilterUtil;
import org.eclipse.kura.util.osgi.SingleServiceTracker;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An abstract base class for creating wire components using a CamelContext.
 */
public abstract class AbstractCamelWireComponent extends AbstractWireComponent {

    private static final Logger logger = LoggerFactory.getLogger(AbstractCamelWireComponent.class);

    protected final BundleContext context = FrameworkUtil.getBundle(AbstractCamelWireComponent.class)
            .getBundleContext();

    private SingleServiceTracker<CamelContext> tracker;

    private String contextId;

    protected void closeContextTracker() {

        if (this.tracker != null) {
            this.tracker.close();
            this.tracker = null;
        }

    }

    protected void openContextTracker(final String contextId) throws InvalidSyntaxException {

        closeContextTracker();

        if (contextId == null || contextId.isEmpty()) {
            return;
        }

        final Filter filter = this.context
                .createFilter(FilterUtil.simpleFilter(CamelContext.class, "camel.context.id", contextId));

        this.tracker = new SingleServiceTracker<>(this.context, filter, this::bindContext);
        this.tracker.open();
    }

    protected void bindContext(final CamelContext context) {
    }

    @Override
    protected void activate(final ComponentContext componentContext, final Map<String, ?> properties) throws Exception {

        super.activate(componentContext, properties);

        this.contextId = Configuration.asString(properties, "id");
        openContextTracker(this.contextId);
    }

    @Override
    protected void modified(final ComponentContext componentContext, Map<String, ?> properties) throws Exception {
        deactivate();
        activate(componentContext, properties);
    }

    @Override
    protected void deactivate() {

        closeContextTracker();

        super.deactivate();
    }

    protected void withContext(final Consumer<CamelContext> consumer) {
        if (this.tracker == null) {
            logger.info("Camel tracker for context: {} is closed. skip processReceive massage", this.contextId);
        } else {
            final CamelContext camelContext = this.tracker.getService();

            if (camelContext != null) {
                consumer.accept(camelContext);
            } else {
                logger.warn("Missing Camel context: {}", this.contextId);
            }
        }
    }
}

/*******************************************************************************
 * Copyright (c) 2016 Red Hat Inc and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Inc - Initial API and implementation
 *******************************************************************************/
package org.eclipse.kura.camel.router;

import java.util.Objects;

import org.apache.camel.CamelContext;
import org.apache.camel.component.kura.KuraRouter;
import org.eclipse.kura.camel.cloud.KuraCloudComponent;
import org.eclipse.kura.cloud.CloudService;

/**
 * An abstract base camel router for the use inside of Kura
 */
public abstract class AbstractCamelRouter extends KuraRouter {

    public static final String COMPONENT_NAME_KURA_CLOUD = "kura-cloud";

    @Override
    protected void beforeStart(final CamelContext camelContext) {
        camelContext.getShutdownStrategy().setTimeout(5);
        camelContext.disableJMX();

        super.beforeStart(camelContext);
    }

    /**
     * Register a custom cloud service
     * <p>
     * <strong>Note: </strong> This method should be called from {@link #beforeStart(CamelContext)} method
     * </p>
     * @param camelContext the camel context
     * @param cloudService the cloud service
     */
    protected void registerCloudService(final CamelContext camelContext, final CloudService cloudService) {
        Objects.requireNonNull(camelContext);
        Objects.requireNonNull(cloudService);

        final KuraCloudComponent component = new KuraCloudComponent(camelContext);
        component.setCloudService(cloudService);

        camelContext.addComponent(COMPONENT_NAME_KURA_CLOUD, component);
    }
}

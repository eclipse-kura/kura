/*******************************************************************************
 * Copyright (c) 2016, 2020 Red Hat Inc and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Red Hat Inc
 *  Eurotech
 *******************************************************************************/
package org.eclipse.kura.camel.cloud;

import org.apache.camel.CamelContext;
import org.apache.camel.Component;
import org.apache.camel.spi.ComponentResolver;
import org.eclipse.kura.cloud.CloudService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A resolver for "kura-cloud"
 * <p>
 * This resolver will register to any instance of {@link CloudService} and
 * wrap it into a {@link KuraCloudComponent} instance.
 * </p>
 * <p>
 * If you need finer grained control, consider using the {@link org.eclipse.kura.camel.runner.CamelRunner} mechanism.
 * </p>
 */
public class KuraCloudComponentResolver implements ComponentResolver {

    private static final Logger logger = LoggerFactory.getLogger(KuraCloudComponentResolver.class);

    private CloudService cloudService;

    public void setCloudService(final CloudService cloudService) {
        this.cloudService = cloudService;
    }

    @Override
    public Component resolveComponent(final String name, final CamelContext context) throws Exception {
        switch (name) {
        case KuraCloudComponent.DEFAULT_NAME:
            final KuraCloudComponent component = new KuraCloudComponent(context, this.cloudService);
            logger.debug("Created new cloud component: {}", component);
            return component;
        default:
        }
        return null;
    }

}

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
package org.eclipse.kura.camel.cloud;

import org.apache.camel.CamelContext;
import org.apache.camel.Component;
import org.apache.camel.spi.ComponentResolver;
import org.eclipse.kura.cloud.CloudService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        }
        return null;
    }

}

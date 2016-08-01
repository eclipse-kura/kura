/*******************************************************************************
 * Copyright (c) 2016 Red Hat Inc and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jens Reimann <jreimann@redhat.com> - Initial API and implementation
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
	
	public void setCloudService(CloudService cloudService) {
		this.cloudService = cloudService;
	}
	
	@Override
	public Component resolveComponent(String name, CamelContext context) throws Exception {
		if ("kura-cloud".equals(name)) {
			KuraCloudComponent component = new KuraCloudComponent(context);
			logger.debug("Created new cloud component: {}", component);
			component.setCloudService(this.cloudService);
			return component;
		}
		return null;
	}

}

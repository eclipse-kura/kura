/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *     Jens Reimann <jreimann@redhat.com> - Fix reloading issue of XML routes
 *         - Refactor basic XML component
 *******************************************************************************/
package org.eclipse.kura.camel.router;

import org.apache.camel.CamelContext;
import org.apache.camel.component.kura.KuraRouter;

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
}

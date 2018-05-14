/*******************************************************************************
 * Copyright (c) 2018 Red Hat Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Inc
 *******************************************************************************/
package org.eclipse.kura.wire.camel;

import static org.eclipse.kura.camel.component.Configuration.asString;

import java.util.Map;

import org.osgi.service.component.ComponentContext;

public abstract class AbstractEndpointWireComponent extends AbstractCamelWireComponent {

    protected volatile String endpointUri;

    public void setEndpointUri(final String endpointUri) {
        this.endpointUri = endpointUri;
    }

    @Override
    protected void activate(final ComponentContext componentContext, Map<String, ?> properties) throws Exception {
        setEndpointUri(asString(properties, "endpointUri"));
        super.activate(componentContext, properties);
    }

    @Override
    protected void deactivate() {
        super.deactivate();
        setEndpointUri(null);
    }

}

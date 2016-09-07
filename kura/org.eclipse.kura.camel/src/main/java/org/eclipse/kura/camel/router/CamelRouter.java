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

import org.eclipse.kura.configuration.ConfigurableComponent;

/**
 * @deprecated use either {@link AbstractXmlCamelComponent} or {@link AbstractCamelRouter} as a base class
 */
@Deprecated
public abstract class CamelRouter extends AbstractXmlCamelComponent implements ConfigurableComponent {
    public CamelRouter() {
        super("camel.route.xml");
    }
}

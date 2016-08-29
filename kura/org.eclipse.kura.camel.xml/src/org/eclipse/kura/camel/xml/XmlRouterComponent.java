/*******************************************************************************
 * Copyright (c) 2016 Red Hat Inc and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Inc - initial API and implementation
 *******************************************************************************/
package org.eclipse.kura.camel.xml;

import org.eclipse.kura.camel.router.AbstractXmlCamelComponent;
import org.eclipse.kura.configuration.ConfigurableComponent;

public class XmlRouterComponent extends AbstractXmlCamelComponent implements ConfigurableComponent {
    public XmlRouterComponent() {
        super("xml.data");
    }
}

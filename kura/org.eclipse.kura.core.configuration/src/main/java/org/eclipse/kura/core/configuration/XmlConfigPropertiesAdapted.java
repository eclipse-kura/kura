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
 *******************************************************************************/
package org.eclipse.kura.core.configuration;

/**
 * Helper class to serialize a property in XML.
 */
public class XmlConfigPropertiesAdapted {

    private XmlConfigPropertyAdapted[] properties;

    public XmlConfigPropertiesAdapted() {
    }

    public XmlConfigPropertyAdapted[] getProperties() {
        return this.properties;
    }

    public void setProperties(XmlConfigPropertyAdapted[] properties) {
        this.properties = properties;
    }
}

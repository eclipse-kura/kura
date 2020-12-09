/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
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

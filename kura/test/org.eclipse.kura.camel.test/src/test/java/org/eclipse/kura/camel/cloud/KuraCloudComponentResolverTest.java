/*******************************************************************************
 * Copyright (c) 2017, 2020 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 ******************************************************************************/
package org.eclipse.kura.camel.cloud;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.apache.camel.CamelContext;
import org.apache.camel.Component;
import org.junit.Test;

public class KuraCloudComponentResolverTest {

    @Test
    public void testResolveComponentWrongName() throws Exception {
        KuraCloudComponentResolver resolver = new KuraCloudComponentResolver();
        String name = "test";
        CamelContext context = null;

        Component component = resolver.resolveComponent(name, context);

        assertNull(component);
    }

    @Test
    public void testResolveComponent() throws Exception {
        KuraCloudComponentResolver resolver = new KuraCloudComponentResolver();
        String name = KuraCloudComponent.DEFAULT_NAME;
        CamelContext context = null;

        Component component = resolver.resolveComponent(name, context);

        assertNotNull(component);
        assertNull(component.getCamelContext());
    }


}

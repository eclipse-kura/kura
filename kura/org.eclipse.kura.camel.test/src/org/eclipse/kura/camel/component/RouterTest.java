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
package org.eclipse.kura.camel.component;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.Map;

import org.junit.Test;

public class RouterTest extends AbstractRouterTest {

    @Test
    public void testLoadNothing() throws Exception {

        // Given

        final Map<String, Object> properties = Collections.emptyMap();

        // When

        this.router.modified(properties);

        // Then

        // expect no route
        assertEquals(0, this.router.getCamelContext().getRoutes().size());
    }

    @Test
    public void testLoadXmlOnce() throws Exception {

        // Given

        final Map<String, Object> properties = xmlProperties("xml/test1.xml");

        // When

        this.router.modified(properties);

        // Then

        // expect one route
        assertEquals(1, this.router.getCamelContext().getRoutes().size());

        // with ID = test1
        assertEquals("test1", firstRoute().getId());
    }

    @Test
    public void testLoadXmlTwice() throws Exception {

        // Given

        final Map<String, Object> properties = xmlProperties("xml/test1.xml");

        // When

        this.router.modified(properties);
        this.router.modified(properties);

        // Then

        // expect still one route
        assertEquals(1, this.router.getCamelContext().getRoutes().size());

        // with ID = test1
        assertEquals("test1", firstRoute().getId());
    }

    @Test
    public void testLoadXmlUpdateSame() throws Exception {

        // Given

        final Map<String, Object> properties1 = xmlProperties("xml/test1.xml");
        final Map<String, Object> properties2 = xmlProperties("xml/test1a.xml");

        // When

        this.router.modified(properties1);

        // Then

        // expect still one route
        assertEquals(1, this.router.getCamelContext().getRoutes().size());
        // with ID = test1
        assertEquals("test1", firstRoute().getId());

        // When

        this.router.modified(properties2);

        // Then

        // expect still one route
        assertEquals(1, this.router.getCamelContext().getRoutes().size());
        // with ID = test1
        assertEquals("test1", firstRoute().getId());
    }

    @Test
    public void testLoadXmlUpdateOther() throws Exception {

        // Given

        final Map<String, Object> properties1 = xmlProperties("xml/test1.xml");
        final Map<String, Object> properties2 = xmlProperties("xml/test2.xml");

        // When ... load test1

        this.router.modified(properties1);

        // Then

        // expect one route
        assertEquals(1, this.router.getCamelContext().getRoutes().size());
        // with ID = test1
        assertEquals("test1", firstRoute().getId());

        // When ... update to test2

        this.router.modified(properties2);

        // Then

        // expect still one route
        assertEquals(1, this.router.getCamelContext().getRoutes().size());
        // with ID = test2
        assertEquals("test2", firstRoute().getId());
    }

}

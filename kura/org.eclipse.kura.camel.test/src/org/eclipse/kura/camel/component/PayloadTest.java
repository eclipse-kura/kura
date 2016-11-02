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

import java.util.Map;

import org.apache.camel.ProducerTemplate;
import org.eclipse.kura.message.KuraPayload;
import org.junit.Test;

public class PayloadTest extends AbstractRouterTest {

    @Test
    public void test1() throws Exception {

        // Given

        final Map<String, Object> properties = xmlProperties("xml/payload1.xml");

        // Expect

        getMockEndpoint("mock:foo-string").expectedBodiesReceived("bar");
        getMockEndpoint("mock:foo-int").expectedBodiesReceived(42);
        getMockEndpoint("mock:foo-boolean").expectedBodiesReceived(true);
        
        // When

        this.router.modified(properties);

        final ProducerTemplate pt = this.router.getCamelContext().createProducerTemplate();
        pt.sendBody("direct:start", createPayload1());

        // Assert

        assertEquals(1, this.router.getCamelContext().getRoutes().size());
        assertMockEndpoints();
    }

    private KuraPayload createPayload1() {
        final KuraPayload payload = new KuraPayload();

        payload.addMetric("foo-string", "bar");
        payload.addMetric("foo-int", 42);
        payload.addMetric("foo-boolean", true);

        return payload;
    }
}

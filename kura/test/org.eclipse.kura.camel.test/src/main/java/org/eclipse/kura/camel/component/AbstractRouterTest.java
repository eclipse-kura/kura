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

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.Route;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.osgi.framework.FrameworkUtil;

/**
 * Unit test base for testing an activated router
 */
public abstract class AbstractRouterTest {

    private static final String XML_PROPERTY = "xml.data";
    protected AbstractXmlCamelComponent router;

    @Before
    public void before() throws Exception {
        this.router = createRouter();
        this.router.activate(FrameworkUtil.getBundle(AbstractRouterTest.class).getBundleContext(),
                Collections.<String, Object> emptyMap());
    }

    @After
    public void after() throws Exception {
        this.router.stop();
    }

    protected CamelContext getCamelContext() {
        return this.router.getCamelContext();
    }

    protected Route firstRoute() {
        return this.router.getCamelContext().getRoutes().iterator().next();
    }

    protected static Map<String, Object> xmlProperties(String resourceName) {
        return Collections.<String, Object> singletonMap(XML_PROPERTY, readStringResource(resourceName));
    }

    protected static AbstractXmlCamelComponent createRouter() {
        return new AbstractXmlCamelComponent(XML_PROPERTY) {
        };
    }

    protected static String readStringResource(String resourceName) {
        try (InputStreamReader reader = new InputStreamReader(RouterTest.class.getResourceAsStream(resourceName),
                StandardCharsets.UTF_8)) {
            return IOUtils.toString(reader);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get a mock endpoint
     * <br>
     * <strong>Note:</strong> This call will fail if the endpoint is not of
     * instance MockEndpoint.
     */
    protected MockEndpoint getMockEndpoint(String endpoint) {
        return (MockEndpoint) this.router.getCamelContext().getEndpoint(endpoint);
    }

    /**
     * Assert all mock endpoints
     */
    protected void assertMockEndpoints() throws InterruptedException {
        MockEndpoint.assertIsSatisfied(this.router.getCamelContext());
    }
}

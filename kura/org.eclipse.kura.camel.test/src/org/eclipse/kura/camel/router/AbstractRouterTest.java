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
package org.eclipse.kura.camel.router;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

import org.apache.camel.Route;
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
        this.router.start(FrameworkUtil.getBundle(RouterTest.class).getBundleContext());
    }

    @After
    public void after() throws Exception {
        this.router.stop(FrameworkUtil.getBundle(RouterTest.class).getBundleContext());
    }

    protected Route firstRoute() {
        return this.router.getContext().getRoutes().iterator().next();
    }

    protected static Map<String, Object> xmlProperties(String resourceName) {
        return Collections.<String, Object>singletonMap(XML_PROPERTY, readStringResource(resourceName));
    }

    protected static AbstractXmlCamelComponent createRouter() {
        return new AbstractXmlCamelComponent(XML_PROPERTY) {
        };
    }

    protected static String readStringResource(String resourceName) {
        try (InputStreamReader reader = new InputStreamReader(RouterTest.class.getResourceAsStream(resourceName), StandardCharsets.UTF_8)) {
            return IOUtils.toString(reader);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}

/*******************************************************************************
 * Copyright (c) 2023 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.rest.system.provider.test;

import static org.eclipse.kura.rest.system.Constants.MQTT_APP_ID;
import static org.eclipse.kura.rest.system.Constants.RESOURCE_BUNDLES;
import static org.eclipse.kura.rest.system.Constants.RESOURCE_PROPERTIES;
import static org.eclipse.kura.rest.system.Constants.RESOURCE_PROPERTIES_FILTER;
import static org.eclipse.kura.rest.system.Constants.REST_APP_ID;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;

import java.util.Arrays;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.core.Response.Status;

import org.eclipse.kura.core.testutil.requesthandler.AbstractRequestHandlerTest;
import org.eclipse.kura.core.testutil.requesthandler.MqttTransport;
import org.eclipse.kura.core.testutil.requesthandler.RestTransport;
import org.eclipse.kura.core.testutil.requesthandler.Transport;
import org.eclipse.kura.core.testutil.requesthandler.Transport.MethodSpec;
import org.eclipse.kura.rest.system.SystemRestService;
import org.eclipse.kura.system.SystemService;
import org.eclipse.kura.util.wire.test.WireTestUtil;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

@RunWith(Parameterized.class)
public class EndpointsTest extends AbstractRequestHandlerTest {

    private static final String EXPECTED_PROPERTIES_RESPONSE = new Scanner(
            EndpointsTest.class.getResourceAsStream("/PROPERTIES_RESPONSE"), "UTF-8").useDelimiter("\\A").next()
                    .replace(" ", "");
    private static final String EXPECTED_EXTENDED_PROPERTIES_RESPONSE = new Scanner(
            EndpointsTest.class.getResourceAsStream("/EXTENDED_PROPERTIES_RESPONSE"), "UTF-8")
                    .useDelimiter("\\A").next().replace(" ", "");
    private static final String EXPECTED_BUNDLES_RESPONSE = new Scanner(
            EndpointsTest.class.getResourceAsStream("/BUNDLES_RESPONSE"), "UTF-8").useDelimiter("\\A").next()
                    .replace(" ", "");
    private static final String PROPERTIES_FILTER_REQUEST = new Scanner(
            EndpointsTest.class.getResourceAsStream("/PROPERTIES_FILTER_REQUEST"), "UTF-8").useDelimiter("\\A").next()
                    .replace(" ", "");

    private static final String METHOD_SPEC_GET = "GET";
    private static final String METHOD_SPEC_POST = "POST";

    private static SystemService systemServiceMock = mock(SystemService.class);

    @Parameterized.Parameters
    public static Collection<Transport> transports() {
        return Arrays.asList(new RestTransport(REST_APP_ID), new MqttTransport(MQTT_APP_ID));
    }

    public EndpointsTest(Transport transport) {
        super(transport);
    }

    /*
     * Scenarios
     */

    @Test
    public void shouldReturnExpectedProperties() {
        givenSystemServiceMockWithoutExtendedProperties();

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_GET), RESOURCE_PROPERTIES);

        thenRequestSucceeds();
        thenResponseBodyEqualsJson(EXPECTED_PROPERTIES_RESPONSE);
    }

    @Test
    public void shouldReturnExpectedBundles() {
        givenSystemServiceMockWithoutExtendedProperties();

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_GET), RESOURCE_BUNDLES);

        thenRequestSucceeds();
        thenResponseBodyEqualsJson(EXPECTED_BUNDLES_RESPONSE);
    }

    @Test
    public void shouldReturnExpectedExtendedProperties() {
        givenSystemServiceMockWithExtendedProperties();

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_GET), RESOURCE_PROPERTIES);

        thenRequestSucceeds();
        thenResponseBodyEqualsJson(EXPECTED_EXTENDED_PROPERTIES_RESPONSE);
    }

    @Test
    public void shouldReturnFilteredProperties() {
        givenSystemServiceMockWithExtendedProperties();

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_POST), RESOURCE_PROPERTIES_FILTER, PROPERTIES_FILTER_REQUEST);

        thenRequestSucceeds();
        thenResponseBodyEqualsJson(EXPECTED_EXTENDED_PROPERTIES_RESPONSE);
    }

    // Exceptions test

    @Test
    public void shouldRethrowWebApplicationExceptionOnFailingProperties() {
        givenFailingSystemServiceMock();

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_GET), RESOURCE_PROPERTIES);

        thenResponseCodeIs(Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test
    public void shouldRethrowWebApplicationExceptionOnFailingBundles() {
        givenFailingSystemServiceMock();

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_GET), RESOURCE_BUNDLES);

        thenResponseCodeIs(Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test
    public void shouldRethrowWebApplicationExceptionOnFailingExtendedProperties() {
        givenFailingSystemServiceMock();

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_GET), RESOURCE_PROPERTIES);

        thenResponseCodeIs(Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test
    public void shouldRethrowWebApplicationExceptionOnFailingPropertiesFilter() {
        givenFailingSystemServiceMock();

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_POST), RESOURCE_PROPERTIES_FILTER, PROPERTIES_FILTER_REQUEST);

        thenResponseCodeIs(Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    /*
     * Steps
     */

    private static void givenSystemServiceMockWithoutExtendedProperties() {
        reset(systemServiceMock);
        SystemServiceMockDecorator.addPropertiesMockMethods(systemServiceMock);
    }

    private static void givenSystemServiceMockWithExtendedProperties() {
        reset(systemServiceMock);
        SystemServiceMockDecorator.addPropertiesMockMethods(systemServiceMock);
        SystemServiceMockDecorator.addExtendedPropertiesMockMethods(systemServiceMock);
    }

    private static void givenFailingSystemServiceMock() {
        reset(systemServiceMock);
        SystemServiceMockDecorator.addFailingMockMethods(systemServiceMock);
    }

    /*
     * Utilities
     */

    @BeforeClass
    public static void setUp() throws Exception {
        createSystemServiceMock();
        registerSystemServiceMock();
    }

    private static void createSystemServiceMock() {
        givenSystemServiceMockWithoutExtendedProperties();

        final Dictionary<String, Object> configurationServiceProperties = new Hashtable<>();
        configurationServiceProperties.put("service.ranking", Integer.MIN_VALUE);
        configurationServiceProperties.put("kura.service.pid", "mockSystemService");
        FrameworkUtil.getBundle(EndpointsTest.class).getBundleContext().registerService(SystemService.class,
                systemServiceMock, configurationServiceProperties);
    }

    private static void registerSystemServiceMock() throws Exception {
        final Dictionary<String, Object> properties = new Hashtable<>();
        properties.put("SystemService.target", "(kura.service.pid=mockSystemService)");

        final ConfigurationAdmin configurationAdmin = WireTestUtil
                .trackService(ConfigurationAdmin.class, Optional.empty()).get(30, TimeUnit.SECONDS);
        final Configuration config = configurationAdmin.getConfiguration(SystemRestService.class.getName(), "?");
        config.update(properties);
    }

}

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
import static org.eclipse.kura.rest.system.Constants.RESOURCE_EXTENDED_PROPERTIES;
import static org.eclipse.kura.rest.system.Constants.RESOURCE_EXTENDED_PROPERTIES_FILTER;
import static org.eclipse.kura.rest.system.Constants.RESOURCE_FRAMEWORK_PROPERTIES;
import static org.eclipse.kura.rest.system.Constants.RESOURCE_FRAMEWORK_PROPERTIES_FILTER;
import static org.eclipse.kura.rest.system.Constants.RESOURCE_KURA_PROPERTIES;
import static org.eclipse.kura.rest.system.Constants.RESOURCE_KURA_PROPERTIES_FILTER;
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

    private static final String EXPECTED_FRAMEWORK_PROPERTIES_RESPONSE = new Scanner(
            EndpointsTest.class.getResourceAsStream("/FRAMEWORK_PROPERTIES_RESPONSE"), "UTF-8")
                    .useDelimiter("\\A").next().replace(" ", "");
    private static final String EXPECTED_EXTENDED_PROPERTIES_RESPONSE = new Scanner(
            EndpointsTest.class.getResourceAsStream("/EXTENDED_PROPERTIES_RESPONSE"), "UTF-8")
                    .useDelimiter("\\A").next().replace(" ", "");
    private static final String EXPECTED_KURA_PROPERTIES_RESPONSE = new Scanner(
            EndpointsTest.class.getResourceAsStream("/KURA_PROPERTIES_RESPONSE"), "UTF-8").useDelimiter("\\A").next()
                    .replace(" ", "");

    private static final String FRAMEWORK_PROPERTIES_FILTER_REQUEST = new Scanner(
            EndpointsTest.class.getResourceAsStream("/FRAMEWORK_PROPERTIES_FILTER_REQUEST"), "UTF-8")
                    .useDelimiter("\\A").next().replace(" ", "");
    private static final String EXTENDED_PROPERTIES_FILTER_REQUEST = new Scanner(
            EndpointsTest.class.getResourceAsStream("/EXTENDED_PROPERTIES_FILTER_REQUEST"), "UTF-8").useDelimiter("\\A")
                    .next().replace(" ", "");
    private static final String KURA_PROPERTIES_FILTER_REQUEST = new Scanner(
            EndpointsTest.class.getResourceAsStream("/KURA_PROPERTIES_FILTER_REQUEST"), "UTF-8").useDelimiter("\\A")
                    .next().replace(" ", "");


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

    // Positive tests

    // GET

    @Test
    public void shouldReturnExpectedFrameworkProperties() {
        givenSystemServiceMockWithFrameworkProperties();

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_GET), RESOURCE_FRAMEWORK_PROPERTIES);

        thenRequestSucceeds();
        thenResponseBodyEqualsJson(EXPECTED_FRAMEWORK_PROPERTIES_RESPONSE);
    }

    @Test
    public void shouldReturnExpectedExtendedProperties() {
        givenSystemServiceMockWithExtendedProperties();

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_GET), RESOURCE_EXTENDED_PROPERTIES);

        thenRequestSucceeds();
        thenResponseBodyEqualsJson(EXPECTED_EXTENDED_PROPERTIES_RESPONSE);
    }

    @Test
    public void shouldReturnCorrectKuraProperties() {
        givenSystemServiceMockWithKuraProperties();

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_GET), RESOURCE_KURA_PROPERTIES);

        thenRequestSucceeds();
        thenResponseBodyEqualsJson(EXPECTED_KURA_PROPERTIES_RESPONSE);
    }

    // POST

    @Test
    public void shouldReturnFilteredFrameworkProperties() {
        givenSystemServiceMockWithFrameworkProperties();

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_POST), RESOURCE_FRAMEWORK_PROPERTIES_FILTER,
                FRAMEWORK_PROPERTIES_FILTER_REQUEST);

        thenRequestSucceeds();
        thenResponseBodyEqualsJson(EXPECTED_FRAMEWORK_PROPERTIES_RESPONSE);
    }

    @Test
    public void shouldReturnFilteredExtendedProperties() {
        givenSystemServiceMockWithExtendedProperties();

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_POST), RESOURCE_EXTENDED_PROPERTIES_FILTER,
                EXTENDED_PROPERTIES_FILTER_REQUEST);

        thenRequestSucceeds();
        thenResponseBodyEqualsJson(EXPECTED_EXTENDED_PROPERTIES_RESPONSE);
    }

    @Test
    public void shouldReturnFilteredKuraProperties() {
        givenSystemServiceMockWithKuraProperties();

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_POST), RESOURCE_KURA_PROPERTIES_FILTER,
                KURA_PROPERTIES_FILTER_REQUEST);

        thenRequestSucceeds();
        thenResponseBodyEqualsJson(EXPECTED_KURA_PROPERTIES_RESPONSE);
    }

    // Exceptions test

    // GET

    @Test
    public void shouldRethrowWebApplicationExceptionOnFailingFrameworkProperties() {
        givenFailingSystemServiceMock();

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_GET), RESOURCE_FRAMEWORK_PROPERTIES);

        thenResponseCodeIs(Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test
    public void shouldRethrowWebApplicationExceptionOnFailingExtendedProperties() {
        givenFailingSystemServiceMock();

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_GET), RESOURCE_FRAMEWORK_PROPERTIES);

        thenResponseCodeIs(Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test
    public void shouldRethrowWebApplicationExceptionOnFailingKuraProperties() {
        givenFailingSystemServiceMock();

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_GET), RESOURCE_KURA_PROPERTIES);

        thenResponseCodeIs(Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    // POST

    @Test
    public void shouldRethrowWebApplicationExceptionOnFailingFrameworkPropertiesFilter() {
        givenFailingSystemServiceMock();

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_POST), RESOURCE_FRAMEWORK_PROPERTIES_FILTER,
                FRAMEWORK_PROPERTIES_FILTER_REQUEST);

        thenResponseCodeIs(Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test
    public void shouldRethrowWebApplicationExceptionOnFailingExtendedPropertiesFilter() {
        givenFailingSystemServiceMock();

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_POST), RESOURCE_EXTENDED_PROPERTIES_FILTER,
                EXTENDED_PROPERTIES_FILTER_REQUEST);

        thenResponseCodeIs(Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test
    public void shouldRethrowWebApplicationExceptionOnFailingKuraPropertiesFilter() {
        givenFailingSystemServiceMock();

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_POST), RESOURCE_KURA_PROPERTIES_FILTER,
                KURA_PROPERTIES_FILTER_REQUEST);

        thenResponseCodeIs(Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    /*
     * Steps
     */

    private static void givenSystemServiceMockWithFrameworkProperties() {
        reset(systemServiceMock);
        SystemServiceMockDecorator.addFrameworkPropertiesMockMethods(systemServiceMock);
    }

    private static void givenSystemServiceMockWithExtendedProperties() {
        reset(systemServiceMock);
        SystemServiceMockDecorator.addExtendedPropertiesMockMethods(systemServiceMock);
    }

    private static void givenSystemServiceMockWithKuraProperties() {
        reset(systemServiceMock);
        SystemServiceMockDecorator.addKuraPropertiesMockMethods(systemServiceMock);
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
        givenSystemServiceMockWithFrameworkProperties();

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

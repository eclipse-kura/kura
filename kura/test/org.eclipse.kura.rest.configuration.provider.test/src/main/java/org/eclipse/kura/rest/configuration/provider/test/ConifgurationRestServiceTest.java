/*******************************************************************************
 * Copyright (c) 2021 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.rest.configuration.provider.test;

import static java.util.Collections.singletonMap;
import static org.eclipse.kura.rest.configuration.provider.test.ConfigurationUtil.adBuilder;
import static org.eclipse.kura.rest.configuration.provider.test.ConfigurationUtil.configurationBuilder;
import static org.eclipse.kura.rest.configuration.provider.test.ConfigurationUtil.ocdBuilder;
import static org.eclipse.kura.rest.configuration.provider.test.JsonProjection.self;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.configuration.Password;
import org.eclipse.kura.configuration.metatype.AD;
import org.eclipse.kura.configuration.metatype.Icon;
import org.eclipse.kura.configuration.metatype.OCD;
import org.eclipse.kura.configuration.metatype.Scalar;
import org.eclipse.kura.crypto.CryptoService;
import org.eclipse.kura.rest.configuration.provider.test.Transport.MethodSpec;
import org.eclipse.kura.rest.configuration.provider.test.Transport.Response;
import org.eclipse.kura.util.wire.test.WireTestUtil;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonValue;

@RunWith(Parameterized.class)
public class ConifgurationRestServiceTest {

    @Test
    public void shouldSupportGetSnapshots() throws KuraException {
        givenMockGetSnapshotsReturnEmpty();

        whenRequestIsPerformed(new MethodSpec("GET"), "/snapshots");

        thenRequestSucceeds();
        thenResponseBodyEquals("{\"ids\":[]}");
    }

    @Test
    public void testListSnapshots() throws KuraException {
        givenMockGetSnapshotsReturnSome(5);

        whenRequestIsPerformed(new MethodSpec("GET"), "/snapshots");

        thenRequestSucceeds();
        thenResponseBodyEquals("{\"ids\":[10000,10001,10002,10003,10004]}");
    }

    @Test
    public void testListSnapshotsKuraException() throws KuraException {
        givenMockGetSnapshotsReturnException();

        whenRequestIsPerformed(new MethodSpec("GET"), "/snapshots");

        thenResponseCodeIs(500);
    }

    @Test
    public void testListFactoryComponentsPidsEmpty() throws KuraException {
        givenMockGetFactoryComponentPidsReturnEmpty();

        whenRequestIsPerformed(new MethodSpec("GET"), "/factoryComponents");

        thenRequestSucceeds();
        thenResponseBodyEquals("{\"pids\":[]}");
    }

    @Test
    public void testListFactoryComponentsPids() throws KuraException {
        givenMockGetFactoryComponentPidsReturnSome(5);

        whenRequestIsPerformed(new MethodSpec("GET"), "/factoryComponents");

        thenRequestSucceeds();
        thenResponseBodyEquals("{\"pids\":[\"pid0\",\"pid1\",\"pid2\",\"pid3\",\"pid4\"]}");
    }

    @Test
    public void testListConfigurableComponentsPidsEmpty() throws KuraException {
        givenMockGetConfigurableComponentPidsReturnEmpty();

        whenRequestIsPerformed(new MethodSpec("GET"), "/configurableComponents");

        thenRequestSucceeds();
        thenResponseBodyEquals("{\"pids\":[]}");
    }

    @Test
    public void testListConfigurableComponentsPids() throws KuraException {
        givenMockGetConfigurableComponentPidsReturnSome(5);

        whenRequestIsPerformed(new MethodSpec("GET"), "/configurableComponents");

        thenRequestSucceeds();
        thenResponseBodyEquals("{\"pids\":[\"pid0\",\"pid1\",\"pid2\",\"pid3\",\"pid4\"]}");
    }

    @Test
    public void testListComponentConfigurationsException() throws KuraException {
        givenMockGetComponentConfigurationsReturnException();

        whenRequestIsPerformed(new MethodSpec("GET"), "/configurableComponents/configurations");

        thenResponseCodeIs(500);
    }

    @Test
    public void testListComponentConfigurationsEmpty() throws KuraException {
        givenMockGetComponentConfigurationsReturnEmpty();

        whenRequestIsPerformed(new MethodSpec("GET"), "/configurableComponents/configurations");

        thenRequestSucceeds();
        thenResponseBodyEquals("{\"configs\":[]}");
    }

    @Test
    public void testListComponentConfigurations() throws KuraException {
        givenMockGetComponentConfigurationsReturnSome(5);

        whenRequestIsPerformed(new MethodSpec("GET"), "/configurableComponents/configurations");

        thenRequestSucceeds();
        thenResponseBodyEquals("{\"configs\":[" + "{\"pid\":\"pid0\"}," + "{\"pid\":\"pid1\"}," + "{\"pid\":\"pid2\"},"
                + "{\"pid\":\"pid3\"}," + "{\"pid\":\"pid4\"}]" + "}");
    }

    @Test
    public void testGetBooleanPropertyTrue() throws KuraException {
        givenATestConfigurationPropertyWithAdTypeAndValue(Scalar.BOOLEAN, true);

        whenRequestIsPerformed(new MethodSpec("GET"), "/configurableComponents/configurations");

        thenRequestSucceeds();
        thenTestPropertyTypeIs(Json.value("BOOLEAN"));
        thenTestPropertyValueIs(Json.value(true));
    }

    @Test
    public void testGetBooleanPropertyFalse() throws KuraException {
        givenATestConfigurationPropertyWithAdTypeAndValue(Scalar.BOOLEAN, false);

        whenRequestIsPerformed(new MethodSpec("GET"), "/configurableComponents/configurations");

        thenRequestSucceeds();
        thenTestPropertyTypeIs(Json.value("BOOLEAN"));
        thenTestPropertyValueIs(Json.value(false));
    }

    @Test
    public void testGetByteProperty() throws KuraException {
        givenATestConfigurationPropertyWithAdTypeAndValue(Scalar.BYTE, (byte) 12);

        whenRequestIsPerformed(new MethodSpec("GET"), "/configurableComponents/configurations");

        thenRequestSucceeds();
        thenTestPropertyTypeIs(Json.value("BYTE"));
        thenTestPropertyValueIs(Json.value(12));
    }

    @Test
    public void testGetCharProperty() throws KuraException {
        givenATestConfigurationPropertyWithAdTypeAndValue(Scalar.CHAR, 'f');

        whenRequestIsPerformed(new MethodSpec("GET"), "/configurableComponents/configurations");

        thenRequestSucceeds();
        thenTestPropertyTypeIs(Json.value("CHAR"));
        thenTestPropertyValueIs(Json.value("f"));
    }

    @Test
    public void testGetDoubleProperty() throws KuraException {
        givenATestConfigurationPropertyWithAdTypeAndValue(Scalar.DOUBLE, 123.1d);

        whenRequestIsPerformed(new MethodSpec("GET"), "/configurableComponents/configurations");

        thenRequestSucceeds();
        thenTestPropertyTypeIs(Json.value("DOUBLE"));
        thenTestPropertyValueIs(Json.value(123.1d));
    }

    @Test
    public void testGetFloatProperty() throws KuraException {
        givenATestConfigurationPropertyWithAdTypeAndValue(Scalar.FLOAT, 123.1f);

        whenRequestIsPerformed(new MethodSpec("GET"), "/configurableComponents/configurations");

        thenRequestSucceeds();
        thenTestPropertyTypeIs(Json.value("FLOAT"));
        thenTestPropertyValueIs(Json.value(123.1d));
    }

    @Test
    public void testGetIntegerProperty() throws KuraException {
        givenATestConfigurationPropertyWithAdTypeAndValue(Scalar.INTEGER, 123);

        whenRequestIsPerformed(new MethodSpec("GET"), "/configurableComponents/configurations");

        thenRequestSucceeds();
        thenTestPropertyTypeIs(Json.value("INTEGER"));
        thenTestPropertyValueIs(Json.value(123));
    }

    @Test
    public void testGetLongProperty() throws KuraException {
        givenATestConfigurationPropertyWithAdTypeAndValue(Scalar.LONG, 123L);

        whenRequestIsPerformed(new MethodSpec("GET"), "/configurableComponents/configurations");

        thenRequestSucceeds();
        thenTestPropertyTypeIs(Json.value("LONG"));
        thenTestPropertyValueIs(Json.value(123));
    }

    @Test
    public void testGetPasswordProperty() throws KuraException {
        givenATestConfigurationPropertyWithAdTypeAndValue(Scalar.PASSWORD,
                new Password(cryptoService.encryptAes("foobar".toCharArray())));

        whenRequestIsPerformed(new MethodSpec("GET"), "/configurableComponents/configurations");

        thenRequestSucceeds();
        thenTestPropertyTypeIs(Json.value("PASSWORD"));
        thenTestPropertyValueIs(Json.value("foobar"));
    }

    @Test
    public void testGetNullProperty() throws KuraException {
        givenATestConfigurationPropertyWithAdTypeAndValue(Scalar.STRING, null);

        whenRequestIsPerformed(new MethodSpec("GET"), "/configurableComponents/configurations");

        thenRequestSucceeds();
        thenTestPropertyIsMissing();
    }

    @Test
    public void testGetNullsInArrayProperty() throws KuraException {
        givenATestConfigurationPropertyWithAdTypeAndValue(Scalar.STRING, new String[] { "foo", null, null });

        whenRequestIsPerformed(new MethodSpec("GET"), "/configurableComponents/configurations");

        thenRequestSucceeds();
        thenTestPropertyTypeIs(Json.value("STRING"));
        thenTestPropertyValueIs(Json.array("foo", null, null));
    }

    @Test
    public void testGetStringProperty() throws KuraException {
        givenATestConfigurationPropertyWithAdTypeAndValue(Scalar.STRING, "test string");

        whenRequestIsPerformed(new MethodSpec("GET"), "/configurableComponents/configurations");

        thenRequestSucceeds();
        thenTestPropertyTypeIs(Json.value("STRING"));
        thenTestPropertyValueIs(Json.value("test string"));
    }

    @Test
    public void testGetBooleanArrayProperty() throws KuraException {
        givenATestConfigurationPropertyWithAdTypeAndValue(Scalar.BOOLEAN, new Boolean[] { true, false, true });

        whenRequestIsPerformed(new MethodSpec("GET"), "/configurableComponents/configurations");

        thenRequestSucceeds();
        thenTestPropertyTypeIs(Json.value("BOOLEAN"));
        thenTestPropertyValueIs(Json.array(true, false, true));
    }

    @Test
    public void testGetByteArrayProperty() throws KuraException {
        givenATestConfigurationPropertyWithAdTypeAndValue(Scalar.BYTE, new Byte[] { 1, 2, 3 });

        whenRequestIsPerformed(new MethodSpec("GET"), "/configurableComponents/configurations");

        thenRequestSucceeds();
        thenTestPropertyTypeIs(Json.value("BYTE"));
        thenTestPropertyValueIs(Json.array(1, 2, 3));
    }

    @Test
    public void testGetCharArrayProperty() throws KuraException {
        givenATestConfigurationPropertyWithAdTypeAndValue(Scalar.CHAR, new Character[] { 'a', 'b', 'c' });

        whenRequestIsPerformed(new MethodSpec("GET"), "/configurableComponents/configurations");

        thenRequestSucceeds();
        thenTestPropertyTypeIs(Json.value("CHAR"));
        thenTestPropertyValueIs(Json.array("a", "b", "c"));
    }

    @Test
    public void testGetDoubleArrayProperty() throws KuraException {
        givenATestConfigurationPropertyWithAdTypeAndValue(Scalar.DOUBLE, new Double[] { 1.0d, 2.0d, 3.0d });

        whenRequestIsPerformed(new MethodSpec("GET"), "/configurableComponents/configurations");

        thenRequestSucceeds();
        thenTestPropertyTypeIs(Json.value("DOUBLE"));
        thenTestPropertyValueIs(Json.parse("[1.0,2.0,3.0]"));
    }

    @Test
    public void testGetFloatArrayProperty() throws KuraException {
        givenATestConfigurationPropertyWithAdTypeAndValue(Scalar.FLOAT, new Float[] { 1.0f, 2.0f, 3.0f });

        whenRequestIsPerformed(new MethodSpec("GET"), "/configurableComponents/configurations");

        thenRequestSucceeds();
        thenTestPropertyTypeIs(Json.value("FLOAT"));
        thenTestPropertyValueIs(Json.parse("[1.0,2.0,3.0]"));
    }

    @Test
    public void testGetIntegerArrayProperty() throws KuraException {
        givenATestConfigurationPropertyWithAdTypeAndValue(Scalar.INTEGER, new Integer[] { 1, 2, 3 });

        whenRequestIsPerformed(new MethodSpec("GET"), "/configurableComponents/configurations");

        thenRequestSucceeds();
        thenTestPropertyTypeIs(Json.value("INTEGER"));
        thenTestPropertyValueIs(Json.array(1, 2, 3));
    }

    @Test
    public void testGetLongArrayProperty() throws KuraException {
        givenATestConfigurationPropertyWithAdTypeAndValue(Scalar.LONG, new Long[] { 1L, 2L, 3L });

        whenRequestIsPerformed(new MethodSpec("GET"), "/configurableComponents/configurations");

        thenRequestSucceeds();
        thenTestPropertyTypeIs(Json.value("LONG"));
        thenTestPropertyValueIs(Json.array(1, 2, 3));
    }

    @Test
    public void testGetPasswordArrayProperty() throws KuraException {
        givenATestConfigurationPropertyWithAdTypeAndValue(Scalar.PASSWORD,
                new Password[] { new Password(cryptoService.encryptAes("foobar".toCharArray())) });

        whenRequestIsPerformed(new MethodSpec("GET"), "/configurableComponents/configurations");

        thenRequestSucceeds();
        thenTestPropertyTypeIs(Json.value("PASSWORD"));
        thenTestPropertyValueIs(Json.array("foobar"));
    }

    @Test
    public void testGetStringArrayProperty() throws KuraException {
        givenATestConfigurationPropertyWithAdTypeAndValue(Scalar.STRING, new String[] { "foo", "bar", "baz" });

        whenRequestIsPerformed(new MethodSpec("GET"), "/configurableComponents/configurations");

        thenRequestSucceeds();
        thenTestPropertyTypeIs(Json.value("STRING"));
        thenTestPropertyValueIs(Json.array("foo", "bar", "baz"));
    }

    @Test
    public void testUpdateConfigurationBooleanPropertyTrue() throws KuraException {
        whenRequestIsPerformed(new MethodSpec("PUT"), "/configurableComponents/configurations/_update",
                "{\"configs\":[" + "{\"pid\":\"foo\","
                        + "properties: {\"testProp\":{\"type\":\"BOOLEAN\",\"value\":true}}" + "}" + "]}");

        thenRequestSucceeds();
        thenReceivedPropertiesForPidContains("foo", "testProp", true);
    }

    @Test
    public void testUpdateConfigurationBooleanPropertyFalse() throws KuraException {
        whenRequestIsPerformed(new MethodSpec("PUT"), "/configurableComponents/configurations/_update",
                "{\"configs\":[" + "{\"pid\":\"foo\","
                        + "properties: {\"testProp\":{\"type\":\"BOOLEAN\",\"value\":false}}" + "}" + "]}");

        thenRequestSucceeds();
        thenReceivedPropertiesForPidContains("foo", "testProp", false);
    }

    @Test
    public void testUpdateByteProperty() throws KuraException {
        whenRequestIsPerformed(new MethodSpec("PUT"), "/configurableComponents/configurations/_update", "{\"configs\":["
                + "{\"pid\":\"foo\"," + "properties: {\"testProp\":{\"type\":\"BYTE\",\"value\":15}}" + "}" + "]}");

        thenRequestSucceeds();
        thenReceivedPropertiesForPidContains("foo", "testProp", (byte) 15);
    }

    @Test
    public void testUpdateCharProperty() throws KuraException {
        whenRequestIsPerformed(new MethodSpec("PUT"), "/configurableComponents/configurations/_update", "{\"configs\":["
                + "{\"pid\":\"foo\"," + "properties: {\"testProp\":{\"type\":\"CHAR\",\"value\":\"a\"}}" + "}" + "]}");

        thenRequestSucceeds();
        thenReceivedPropertiesForPidContains("foo", "testProp", 'a');
    }

    @Test
    public void testUpdateDoubleProperty() throws KuraException {
        whenRequestIsPerformed(new MethodSpec("PUT"), "/configurableComponents/configurations/_update", "{\"configs\":["
                + "{\"pid\":\"foo\"," + "properties: {\"testProp\":{\"type\":\"DOUBLE\",\"value\":2.0}}" + "}" + "]}");

        thenRequestSucceeds();
        thenReceivedPropertiesForPidContains("foo", "testProp", 2.0d);
    }

    @Test
    public void testUpdateFloatProperty() throws KuraException {
        whenRequestIsPerformed(new MethodSpec("PUT"), "/configurableComponents/configurations/_update", "{\"configs\":["
                + "{\"pid\":\"foo\"," + "properties: {\"testProp\":{\"type\":\"FLOAT\",\"value\":2.0}}" + "}" + "]}");

        thenRequestSucceeds();
        thenReceivedPropertiesForPidContains("foo", "testProp", 2.0f);
    }

    @Test
    public void testUpdateIntegerProperty() throws KuraException {
        whenRequestIsPerformed(new MethodSpec("PUT"), "/configurableComponents/configurations/_update", "{\"configs\":["
                + "{\"pid\":\"foo\"," + "properties: {\"testProp\":{\"type\":\"INTEGER\",\"value\":123}}" + "}" + "]}");

        thenRequestSucceeds();
        thenReceivedPropertiesForPidContains("foo", "testProp", 123);
    }

    @Test
    public void testUpdateLongProperty() throws KuraException {
        whenRequestIsPerformed(new MethodSpec("PUT"), "/configurableComponents/configurations/_update", "{\"configs\":["
                + "{\"pid\":\"foo\"," + "properties: {\"testProp\":{\"type\":\"LONG\",\"value\":123}}" + "}" + "]}");

        thenRequestSucceeds();
        thenReceivedPropertiesForPidContains("foo", "testProp", 123L);
    }

    @Test
    public void testUpdatePasswordProperty() throws KuraException {
        whenRequestIsPerformed(new MethodSpec("PUT"), "/configurableComponents/configurations/_update",
                "{\"configs\":[" + "{\"pid\":\"foo\","
                        + "properties: {\"testProp\":{\"type\":\"PASSWORD\",\"value\":\"foobar\"}}" + "}" + "]}");

        thenRequestSucceeds();
        thenReceivedPropertiesForPidContainsPassword("foo", "testProp", "foobar");
    }

    @Test
    public void testUpdateStringProperty() throws KuraException {
        whenRequestIsPerformed(new MethodSpec("PUT"), "/configurableComponents/configurations/_update",
                "{\"configs\":[" + "{\"pid\":\"foo\","
                        + "properties: {\"testProp\":{\"type\":\"STRING\",\"value\":\"test string\"}}" + "}" + "]}");

        thenRequestSucceeds();
        thenReceivedPropertiesForPidContains("foo", "testProp", "test string");
    }

    @Test
    public void testUpdateBooleanArrayProperty() throws KuraException {
        whenRequestIsPerformed(new MethodSpec("PUT"), "/configurableComponents/configurations/_update",
                "{\"configs\":[" + "{\"pid\":\"foo\","
                        + "properties: {\"testProp\":{\"type\":\"BOOLEAN\",\"value\":[false,true,false]}}" + "}"
                        + "]}");

        thenRequestSucceeds();
        thenReceivedPropertiesForPidContainsArray("foo", "testProp", new Boolean[] { false, true, false });
    }

    @Test
    public void testUpdateByteArrayProperty() throws KuraException {
        whenRequestIsPerformed(new MethodSpec("PUT"), "/configurableComponents/configurations/_update",
                "{\"configs\":[" + "{\"pid\":\"foo\","
                        + "properties: {\"testProp\":{\"type\":\"BYTE\",\"value\":[15,12]}}" + "}" + "]}");

        thenRequestSucceeds();
        thenReceivedPropertiesForPidContainsArray("foo", "testProp", new Byte[] { 15, 12 });
    }

    @Test
    public void testUpdateCharArrayProperty() throws KuraException {
        whenRequestIsPerformed(new MethodSpec("PUT"), "/configurableComponents/configurations/_update",
                "{\"configs\":[" + "{\"pid\":\"foo\","
                        + "properties: {\"testProp\":{\"type\":\"CHAR\",\"value\":[\"a\",\"b\",\"c\"]}}" + "}" + "]}");

        thenRequestSucceeds();
        thenReceivedPropertiesForPidContainsArray("foo", "testProp", new Character[] { 'a', 'b', 'c' });
    }

    @Test
    public void testUpdateDoubleArrayProperty() throws KuraException {
        whenRequestIsPerformed(new MethodSpec("PUT"), "/configurableComponents/configurations/_update",
                "{\"configs\":[" + "{\"pid\":\"foo\","
                        + "properties: {\"testProp\":{\"type\":\"DOUBLE\",\"value\":[2.0,3.0]}}" + "}" + "]}");

        thenRequestSucceeds();
        thenReceivedPropertiesForPidContainsArray("foo", "testProp", new Double[] { 2.0d, 3.0d });
    }

    @Test
    public void testUpdateFloatArrayProperty() throws KuraException {
        whenRequestIsPerformed(new MethodSpec("PUT"), "/configurableComponents/configurations/_update",
                "{\"configs\":[" + "{\"pid\":\"foo\","
                        + "properties: {\"testProp\":{\"type\":\"FLOAT\",\"value\":[2.0,4.0]}}" + "}" + "]}");

        thenRequestSucceeds();
        thenReceivedPropertiesForPidContainsArray("foo", "testProp", new Float[] { 2.0f, 4.0f });
    }

    @Test
    public void testUpdateIntegerArrayProperty() throws KuraException {
        whenRequestIsPerformed(new MethodSpec("PUT"), "/configurableComponents/configurations/_update",
                "{\"configs\":[" + "{\"pid\":\"foo\","
                        + "properties: {\"testProp\":{\"type\":\"INTEGER\",\"value\":[1,2,3]}}" + "}" + "]}");

        thenRequestSucceeds();
        thenReceivedPropertiesForPidContainsArray("foo", "testProp", new Integer[] { 1, 2, 3 });
    }

    @Test
    public void testUpdateLongArrayProperty() throws KuraException {
        whenRequestIsPerformed(new MethodSpec("PUT"), "/configurableComponents/configurations/_update",
                "{\"configs\":[" + "{\"pid\":\"foo\","
                        + "properties: {\"testProp\":{\"type\":\"LONG\",\"value\":[1,2,3]}}" + "}" + "]}");

        thenRequestSucceeds();
        thenReceivedPropertiesForPidContainsArray("foo", "testProp", new Long[] { 1L, 2L, 3L });
    }

    @Test
    public void testUpdatePasswordArrayProperty() throws KuraException {
        whenRequestIsPerformed(new MethodSpec("PUT"), "/configurableComponents/configurations/_update",
                "{\"configs\":[" + "{\"pid\":\"foo\","
                        + "properties: {\"testProp\":{\"type\":\"PASSWORD\",\"value\":[\"foobar\",\"a\"]}}" + "}"
                        + "]}");

        thenRequestSucceeds();
        thenReceivedPropertiesForPidContainsPasswords("foo", "testProp", "foobar", "a");
    }

    @Test
    public void testUpdateStringArrayProperty() throws KuraException {
        whenRequestIsPerformed(new MethodSpec("PUT"), "/configurableComponents/configurations/_update",
                "{\"configs\":[" + "{\"pid\":\"foo\","
                        + "properties: {\"testProp\":{\"type\":\"STRING\",\"value\":[\"test string\",\"foo\"]}}" + "}"
                        + "]}");

        thenRequestSucceeds();
        thenReceivedPropertiesForPidContainsArray("foo", "testProp", new String[] { "test string", "foo" });
    }

    @Test
    public void testUpdateNullProperty() throws KuraException {
        whenRequestIsPerformed(new MethodSpec("PUT"), "/configurableComponents/configurations/_update", "{\"configs\":["
                + "{\"pid\":\"foo\","
                + "properties: {\"testProp\":{\"type\":\"STRING\"},\"otherProp\":{\"type\":\"STRING\",\"value\":null}}"
                + "}" + "]}");

        thenRequestSucceeds();
        thenReceivedPropertiesForPidContains("foo", "testProp", null);
        thenReceivedPropertiesForPidContains("foo", "otherProp", null);
    }

    @Test
    public void testUpdateNullsInArrayProperty() throws KuraException {
        whenRequestIsPerformed(new MethodSpec("PUT"), "/configurableComponents/configurations/_update",
                "{\"configs\":[" + "{\"pid\":\"foo\","
                        + "properties: {\"testProp\":{\"type\":\"STRING\",value:[null,\"foo\",null]}}" + "}" + "]}");

        thenRequestSucceeds();
        thenReceivedPropertiesForPidContainsArray("foo", "testProp", new String[] { null, "foo", null });
    }

    @Test
    public void testListComponentConfigurationsByPidException() throws KuraException {
        givenMockGetComponentConfigurationsReturnException();

        whenRequestIsPerformed(new MethodSpec("POST"), "/configurableComponents/configurations/byPid",
                "{\"pids\":[\"foo\"]}");

        thenResponseCodeIs(500);
    }

    @Test
    public void testListComponentConfigurationsByPidEmpty() throws KuraException {
        givenMockGetComponentConfigurationsReturnEmpty();

        whenRequestIsPerformed(new MethodSpec("POST"), "/configurableComponents/configurations/byPid",
                "{\"pids\":[\"foo\"]}");

        thenRequestSucceeds();
        thenResponseBodyEquals("{\"configs\":[]}");
    }

    @Test
    public void testListComponentConfigurationsByPid() throws KuraException {
        givenMockGetComponentConfigurationsReturnSome(5);

        whenRequestIsPerformed(new MethodSpec("POST"), "/configurableComponents/configurations/byPid",
                "{\"pids\":[\"pid1\",\"pid3\"]}");

        thenRequestSucceeds();
        thenResponseBodyEquals("{\"configs\":[" + "{\"pid\":\"pid1\"}," + "{\"pid\":\"pid3\"}]" + "}");
    }

    @Test
    public void testListDefaultComponentConfigurationException() throws KuraException {
        givenMockGetDefaultComponentConfigurationReturnException();

        whenRequestIsPerformed(new MethodSpec("POST"), "/configurableComponents/configurations/byPid/_default",
                "{\"pids\":[\"foo\"]}");

        thenRequestSucceeds();
        thenResponseBodyEquals("{\"configs\":[]}");
    }

    @Test
    public void testListDefaultComponentConfiguration() throws KuraException {
        givenMockGetDefaultComponentConfigurationReturnOne("test");

        whenRequestIsPerformed(new MethodSpec("POST"), "/configurableComponents/configurations/byPid/_default",
                "{\"pids\":[\"test\"]}");

        thenRequestSucceeds();
        thenResponseBodyEquals("{\"configs\":[{\"pid\":\"test\",\"definition\":{\"name\":\"test\",\"id\":\"test\"}}]}");
    }

    @Test
    public void testGetSnapshotException() throws KuraException {
        givenMockGetSnapshotReturnException();

        whenRequestIsPerformed(new MethodSpec("POST"), "/snapshots/byId", "{\"id\":12345}");

        thenResponseCodeIs(500);
    }

    @Test
    public void testGetSnapshot() throws KuraException {
        givenMockGetSnapshotReturnSome(12345, 5);

        whenRequestIsPerformed(new MethodSpec("POST"), "/snapshots/byId", "{\"id\":12345}");

        thenRequestSucceeds();
        thenResponseBodyEquals("{\"configs\":[" + "{\"pid\":\"pid0\"}," + "{\"pid\":\"pid1\"}," + "{\"pid\":\"pid2\"},"
                + "{\"pid\":\"pid3\"}," + "{\"pid\":\"pid4\"}]" + "}");
    }

    @Test
    public void testTakeSnapshotException() throws KuraException {
        givenMockSnapshotReturnException();

        whenRequestIsPerformed(new MethodSpec("POST", "EXEC"), "/snapshots/_write");

        thenResponseCodeIs(500);
    }

    @Test
    public void testTakeSnapshot() throws KuraException {
        givenMockSnapshotReturnOne(12345);

        whenRequestIsPerformed(new MethodSpec("POST", "EXEC"), "/snapshots/_write");

        thenRequestSucceeds();
        thenResponseBodyEquals("{\"id\":12345}");
    }

    @Test
    public void testRollbackException() throws KuraException {
        givenMockRollbackReturnException();

        whenRequestIsPerformed(new MethodSpec("POST", "EXEC"), "/snapshots/_rollback");

        thenResponseCodeIs(500);
    }

    @Test
    public void testRollback() throws KuraException {
        givenMockRollbackReturnOne(11111);

        whenRequestIsPerformed(new MethodSpec("POST", "EXEC"), "/snapshots/_rollback");

        thenRequestSucceeds();
        thenResponseBodyEquals("{\"id\":11111}");
    }

    @Test
    public void testRollbackToIdException() throws KuraException {
        givenMockRollbackReturnException(12345);

        whenRequestIsPerformed(new MethodSpec("POST", "EXEC"), "/snapshots/byId/_rollback", "{\"id\":12345}");

        thenResponseCodeIs(500);
    }

    @Test
    public void testRollbackToId() throws KuraException {
        givenMockRollbackReturnNothing(12345);

        whenRequestIsPerformed(new MethodSpec("POST", "EXEC"), "/snapshots/byId/_rollback", "{\"id\":12345}");

        thenRequestSucceeds();
        thenResponseBodyIsEmpty();
    }

    @Test
    public void testADIdAndType() throws KuraException {
        givenConfigurations(configurationBuilder("foo") //
                .withDefinition( //
                        ocdBuilder("foo") //
                                .withAd(adBuilder("fooAdName", Scalar.BOOLEAN).build()) //
                                .build()) //
                .build());

        whenRequestIsPerformed(new MethodSpec("POST"), "/configurableComponents/configurations/byPid",
                "{\"pids\":[\"foo\"]}");

        thenRequestSucceeds();
        thenResponseElementIs(Json.value("fooAdName"),
                self().field("configs").arrayItem(0).field("definition").field("ad").arrayItem(0).field("id"));
        thenResponseElementIs(Json.value("BOOLEAN"),
                self().field("configs").arrayItem(0).field("definition").field("ad").arrayItem(0).field("type"));
    }

    @Test
    public void testADShouldNotSerializeFieldsIfUnset() throws KuraException {
        givenConfigurations(configurationBuilder("foo") //
                .withDefinition( //
                        ocdBuilder("foo") //
                                .withAd(adBuilder("fooAdName", Scalar.BOOLEAN).build()) //
                                .build()) //
                .build());

        whenRequestIsPerformed(new MethodSpec("POST"), "/configurableComponents/configurations/byPid",
                "{\"pids\":[\"foo\"]}");

        thenRequestSucceeds();
        thenResponseElementIs(null,
                self().field("configs").arrayItem(0).field("definition").field("ad").arrayItem(0).field("name"));
        thenResponseElementIs(null,
                self().field("configs").arrayItem(0).field("definition").field("ad").arrayItem(0).field("description"));
        thenResponseElementIs(null,
                self().field("configs").arrayItem(0).field("definition").field("ad").arrayItem(0).field("min"));
        thenResponseElementIs(null,
                self().field("configs").arrayItem(0).field("definition").field("ad").arrayItem(0).field("max"));
        thenResponseElementIs(null, self().field("configs").arrayItem(0).field("definition").field("ad").arrayItem(0)
                .field("defaultValue"));
        thenResponseElementIs(null, self().field("configs").arrayItem(0).field("definition").field("ad").arrayItem(0)
                .field("defaultValue"));
        thenResponseElementIs(null,
                self().field("configs").arrayItem(0).field("definition").field("ad").arrayItem(0).field("option"));
    }

    @Test
    public void testADName() throws KuraException {
        givenConfigurations(configurationBuilder("foo") //
                .withDefinition( //
                        ocdBuilder("foo") //
                                .withAd(adBuilder("fooAdName", Scalar.BOOLEAN).withName("testName").build()) //
                                .build()) //
                .build());

        whenRequestIsPerformed(new MethodSpec("POST"), "/configurableComponents/configurations/byPid",
                "{\"pids\":[\"foo\"]}");

        thenRequestSucceeds();
        thenResponseElementIs(Json.value("testName"),
                self().field("configs").arrayItem(0).field("definition").field("ad").arrayItem(0).field("name"));
    }

    @Test
    public void testADDescription() throws KuraException {
        givenConfigurations(configurationBuilder("foo") //
                .withDefinition( //
                        ocdBuilder("foo") //
                                .withAd(adBuilder("fooAdName", Scalar.BOOLEAN).withDescription("test description")
                                        .build()) //
                                .build()) //
                .build());

        whenRequestIsPerformed(new MethodSpec("POST"), "/configurableComponents/configurations/byPid",
                "{\"pids\":[\"foo\"]}");

        thenRequestSucceeds();
        thenResponseElementIs(Json.value("test description"),
                self().field("configs").arrayItem(0).field("definition").field("ad").arrayItem(0).field("description"));
    }

    @Test
    public void testADCardinality() throws KuraException {
        givenConfigurations(configurationBuilder("foo") //
                .withDefinition( //
                        ocdBuilder("foo") //
                                .withAd(adBuilder("fooAdName", Scalar.BOOLEAN).withCardinality(1).build()) //
                                .build()) //
                .build());

        whenRequestIsPerformed(new MethodSpec("POST"), "/configurableComponents/configurations/byPid",
                "{\"pids\":[\"foo\"]}");

        thenRequestSucceeds();
        thenResponseElementIs(Json.value(1),
                self().field("configs").arrayItem(0).field("definition").field("ad").arrayItem(0).field("cardinality"));
    }

    @Test
    public void testADMin() throws KuraException {
        givenConfigurations(configurationBuilder("foo") //
                .withDefinition( //
                        ocdBuilder("foo") //
                                .withAd(adBuilder("fooAdName", Scalar.BOOLEAN).withMin("10").build()) //
                                .build()) //
                .build());

        whenRequestIsPerformed(new MethodSpec("POST"), "/configurableComponents/configurations/byPid",
                "{\"pids\":[\"foo\"]}");

        thenRequestSucceeds();
        thenResponseElementIs(Json.value("10"),
                self().field("configs").arrayItem(0).field("definition").field("ad").arrayItem(0).field("min"));
    }

    @Test
    public void testADMax() throws KuraException {
        givenConfigurations(configurationBuilder("foo") //
                .withDefinition( //
                        ocdBuilder("foo") //
                                .withAd(adBuilder("fooAdName", Scalar.BOOLEAN).withMax("10").build()) //
                                .build()) //
                .build());

        whenRequestIsPerformed(new MethodSpec("POST"), "/configurableComponents/configurations/byPid",
                "{\"pids\":[\"foo\"]}");

        thenRequestSucceeds();
        thenResponseElementIs(Json.value("10"),
                self().field("configs").arrayItem(0).field("definition").field("ad").arrayItem(0).field("max"));
    }

    @Test
    public void testADDefaultValue() throws KuraException {
        givenConfigurations(configurationBuilder("foo") //
                .withDefinition( //
                        ocdBuilder("foo") //
                                .withAd(adBuilder("fooAdName", Scalar.BOOLEAN).withDefault("true").build()) //
                                .build()) //
                .build());

        whenRequestIsPerformed(new MethodSpec("POST"), "/configurableComponents/configurations/byPid",
                "{\"pids\":[\"foo\"]}");

        thenRequestSucceeds();
        thenResponseElementIs(Json.value("true"), self().field("configs").arrayItem(0).field("definition").field("ad")
                .arrayItem(0).field("defaultValue"));
    }

    @Test
    public void testADOption() throws KuraException {
        givenConfigurations(configurationBuilder("foo") //
                .withDefinition( //
                        ocdBuilder("foo") //
                                .withAd(adBuilder("fooAdName", Scalar.BOOLEAN) //
                                        .withOption(null, "foo") //
                                        .withOption("bar", "baz") //
                                        .build()) //
                                .build()) //
                .build());

        whenRequestIsPerformed(new MethodSpec("POST"), "/configurableComponents/configurations/byPid",
                "{\"pids\":[\"foo\"]}");

        thenRequestSucceeds();
        thenResponseElementIs(Json.parse("[{\"value\":\"foo\"},{\"label\":\"bar\",\"value\":\"baz\"}]"),
                self().field("configs").arrayItem(0).field("definition").field("ad").arrayItem(0).field("option"));
    }

    private final Transport transport;
    private static ConfigurationService configurationService = Mockito.mock(ConfigurationService.class);
    private final CryptoService cryptoService;

    private Optional<Response> response = Optional.empty();
    private Map<String, Map<String, Object>> receivedConfigsByPid = new HashMap<>();

    @Parameterized.Parameters
    public static Collection<Transport> transports() {
        return Arrays.asList(new RestTransport(), new MqttTransport());
    }

    @BeforeClass
    public static void setUp() throws Exception {
        WireTestUtil.trackService(ConfigurationService.class, Optional.empty()).get(30, TimeUnit.SECONDS);

        final ConfigurationAdmin configurationAdmin = WireTestUtil
                .trackService(ConfigurationAdmin.class, Optional.empty()).get(30, TimeUnit.SECONDS);

        final Configuration config = configurationAdmin
                .getConfiguration("org.eclipse.kura.internal.rest.configuration.ConfigurationRestService", "?");
        final Dictionary<String, Object> properties = new Hashtable<>();
        properties.put("ConfigurationService.target", "(kura.service.pid=mockConfigurationService)");

        config.update(properties);

        final Dictionary<String, Object> configurationServiceProperties = new Hashtable<>();
        configurationServiceProperties.put("service.ranking", Integer.MIN_VALUE);
        configurationServiceProperties.put("kura.service.pid", "mockConfigurationService");

        FrameworkUtil.getBundle(ConifgurationRestServiceTest.class).getBundleContext()
                .registerService(ConfigurationService.class, configurationService, configurationServiceProperties);

    }

    @SuppressWarnings("unchecked")
    public ConifgurationRestServiceTest(final Transport transport) throws InterruptedException, ExecutionException,
            TimeoutException, KuraException, InvalidSyntaxException, IOException {
        this.transport = transport;
        this.transport.init();
        this.cryptoService = WireTestUtil.trackService(CryptoService.class, Optional.empty()).get(30, TimeUnit.SECONDS);
        Mockito.reset(configurationService);
        Mockito.doAnswer(i -> {
            Optional.of(i.getArgumentAt(0, List.class));
            return (Void) null;
        }).when(configurationService).updateConfigurations(Mockito.any());
        final Answer<?> configurationUpdateAnswer = i -> {
            receivedConfigsByPid.put(i.getArgumentAt(0, String.class), i.getArgumentAt(1, Map.class));
            return (Void) null;
        };
        Mockito.doAnswer(configurationUpdateAnswer).when(configurationService).updateConfiguration(Mockito.any(),
                Mockito.any());
        Mockito.doAnswer(configurationUpdateAnswer).when(configurationService).updateConfiguration(Mockito.any(),
                Mockito.any(), Mockito.anyBoolean());
    }

    private void whenRequestIsPerformed(final MethodSpec method, final String resource) {
        this.response = Optional.of(this.transport.runRequest(resource, method));
    }

    private void whenRequestIsPerformed(final MethodSpec method, final String resource, final String body) {
        this.response = Optional.of(this.transport.runRequest(resource, method, body));
    }

    private void thenRequestSucceeds() {
        thenResponseCodeIs(200);
    }

    private void thenResponseCodeIs(final int expectedResponseCode) {
        final Response currentResponse = expectResponse();

        if (currentResponse.status != expectedResponseCode) {
            fail("expected status: " + expectedResponseCode + " but was: " + currentResponse.status + " body: "
                    + currentResponse.body);
        }
    }

    private void thenResponseBodyIsEmpty() {
        assertEquals(Optional.empty(), expectResponse().body);
    }

    private void thenResponseBodyEquals(final String value) {
        assertEquals(Json.parse(value), Json
                .parse(expectResponse().body.orElseThrow(() -> new IllegalStateException("expected response body"))));
    }

    private void thenResponseElementIs(final JsonValue expected, final JsonProjection projection) {
        final JsonValue root = Json
                .parse(expectResponse().body.orElseThrow(() -> new IllegalStateException("expected body")));
        final JsonValue actual;

        try {
            actual = projection.apply(root);
        } catch (final Exception e) {
            fail("failed to apply " + projection + " to " + root);
            throw new IllegalStateException("unreachable");
        }

        assertEquals("after applying " + projection + " to " + root, expected, actual);
    }

    private Response expectResponse() {
        return response.orElseThrow(() -> new IllegalStateException("response not available"));
    }

    private void givenMockGetSnapshotsReturnEmpty() throws KuraException {
        when(configurationService.getSnapshots()).thenReturn(new TreeSet<Long>());
    }

    private void givenMockGetSnapshotsReturnSome(int howManySnapshots) throws KuraException {
        Set<Long> snapshots = new TreeSet<>();
        for (int i = 0; i < howManySnapshots; i++) {
            snapshots.add((long) i + 10000);
        }
        when(configurationService.getSnapshots()).thenReturn(snapshots);
    }

    private void givenMockGetSnapshotsReturnException() throws KuraException {
        when(configurationService.getSnapshots()).thenThrow(new KuraException(KuraErrorCode.CONFIGURATION_ERROR));
    }

    private void givenMockGetFactoryComponentPidsReturnEmpty() throws KuraException {
        when(configurationService.getFactoryComponentPids()).thenReturn(Collections.emptySet());
    }

    private void givenMockGetFactoryComponentPidsReturnSome(int howManyComponents) throws KuraException {
        Set<String> components = new HashSet<>();
        for (int i = 0; i < howManyComponents; i++) {
            components.add("pid" + i);
        }
        when(configurationService.getFactoryComponentPids()).thenReturn(components);
    }

    private void givenMockGetConfigurableComponentPidsReturnEmpty() throws KuraException {
        when(configurationService.getConfigurableComponentPids()).thenReturn(new HashSet<String>());
    }

    private void givenMockGetConfigurableComponentPidsReturnSome(int howManyComponents) throws KuraException {
        Set<String> components = new HashSet<>();
        for (int i = 0; i < howManyComponents; i++) {
            components.add("pid" + i);
        }
        when(configurationService.getConfigurableComponentPids()).thenReturn(components);
    }

    private void givenMockGetComponentConfigurationsReturnException() throws KuraException {
        when(configurationService.getComponentConfigurations()).thenThrow(new KuraException(KuraErrorCode.BAD_REQUEST));
    }

    private void givenMockGetComponentConfigurationsReturnEmpty() throws KuraException {
        when(configurationService.getComponentConfigurations()).thenReturn(Collections.emptyList());
    }

    private void givenMockGetComponentConfigurationsReturnSome(int howManyConfigurations) throws KuraException {
        List<ComponentConfiguration> configs = new ArrayList<>();
        for (int i = 0; i < howManyConfigurations; i++) {
            final String generatedPid = "pid" + i;
            configs.add(new ComponentConfiguration() {

                @Override
                public String getPid() {
                    return generatedPid;
                }

                @Override
                public OCD getDefinition() {
                    return null;
                }

                @Override
                public Map<String, Object> getConfigurationProperties() {
                    return null;
                }
            });
        }
        when(configurationService.getComponentConfigurations()).thenReturn(configs);
    }

    private void givenMockGetDefaultComponentConfigurationReturnException() throws KuraException {
        when(configurationService.getDefaultComponentConfiguration(Mockito.anyObject()))
                .thenThrow(new KuraException(KuraErrorCode.BAD_REQUEST));
    }

    private void givenMockGetDefaultComponentConfigurationReturnOne(String pid) throws KuraException {
        ComponentConfiguration config = new ComponentConfiguration() {

            @Override
            public String getPid() {
                return pid;
            }

            @Override
            public OCD getDefinition() {
                return new OCD() {

                    @Override
                    public List<AD> getAD() {
                        return null;
                    }

                    @Override
                    public List<Icon> getIcon() {
                        return null;
                    }

                    @Override
                    public String getName() {
                        return pid;
                    }

                    @Override
                    public String getDescription() {
                        return null;
                    }

                    @Override
                    public String getId() {
                        return pid;
                    }
                };
            }

            @Override
            public Map<String, Object> getConfigurationProperties() {
                return null;
            }
        };

        when(configurationService.getDefaultComponentConfiguration(Mockito.anyObject())).thenReturn(config);
    }

    private void givenMockGetSnapshotReturnException() throws KuraException {
        when(configurationService.getSnapshot(12345)).thenThrow(new KuraException(KuraErrorCode.BAD_REQUEST));
    }

    private void givenMockGetSnapshotReturnSome(long snapshotId, int howManyConfigurations) throws KuraException {
        List<ComponentConfiguration> configs = new ArrayList<>();
        for (int i = 0; i < howManyConfigurations; i++) {
            final String generatedPid = "pid" + i;
            configs.add(new ComponentConfiguration() {

                @Override
                public String getPid() {
                    return generatedPid;
                }

                @Override
                public OCD getDefinition() {
                    return null;
                }

                @Override
                public Map<String, Object> getConfigurationProperties() {
                    return null;
                }
            });
        }
        when(configurationService.getSnapshot(snapshotId)).thenReturn(configs);
    }

    private void givenMockSnapshotReturnException() throws KuraException {
        when(configurationService.snapshot()).thenThrow(new KuraException(KuraErrorCode.BAD_REQUEST));
    }

    private void givenMockSnapshotReturnOne(long snapshotId) throws KuraException {
        when(configurationService.snapshot()).thenReturn(snapshotId);
    }

    private void givenMockRollbackReturnException() throws KuraException {
        when(configurationService.rollback()).thenThrow(new KuraException(KuraErrorCode.BAD_REQUEST));
    }

    private void givenMockRollbackReturnException(long snapshotId) throws KuraException {
        doThrow(new KuraException(KuraErrorCode.BAD_REQUEST)).when(configurationService).rollback(snapshotId);
    }

    private void givenMockRollbackReturnOne(long snapshotId) throws KuraException {
        when(configurationService.rollback()).thenReturn(snapshotId);
    }

    private void givenMockRollbackReturnNothing(long snapshotId) throws KuraException {
        doNothing().when(configurationService).rollback(snapshotId);
    }

    private void givenConfigurations(final ComponentConfiguration... configurations) throws KuraException {
        final Map<String, ComponentConfiguration> byPid = Arrays.stream(configurations)
                .collect(Collectors.toMap(c -> c.getPid(), c -> c));

        Mockito.when(configurationService.getComponentConfigurations())
                .thenReturn(byPid.values().stream().collect(Collectors.toList()));

        Mockito.when(configurationService.getComponentConfiguration(Mockito.any())).thenAnswer(i -> {
            final String pid = i.getArgumentAt(0, String.class);
            return byPid.get(pid);
        });
    }

    private void givenATestConfigurationPropertyWithAdTypeAndValue(final Scalar type, final Object value)
            throws KuraException {
        givenConfigurations(configurationBuilder("foo") //
                .withDefinition( //
                        ocdBuilder("foo") //
                                .withAd(adBuilder("testProp", type) //
                                        .build()) //
                                .build()) //
                .withConfigurationProperties(singletonMap("testProp", value)).build());
    }

    private void thenTestPropertyTypeIs(final JsonValue type) {
        thenResponseElementIs(type,
                self().field("configs").arrayItem(0).field("definition").field("ad").arrayItem(0).field("type"));
        thenResponseElementIs(type,
                self().field("configs").arrayItem(0).field("properties").field("testProp").field("type"));
    }

    private void thenTestPropertyIsMissing() {
        thenResponseElementIs(null, self().field("configs").arrayItem(0).field("properties").field("testProp"));
    }

    private void thenTestPropertyValueIs(final JsonValue value) {
        thenResponseElementIs(value,
                self().field("configs").arrayItem(0).field("properties").field("testProp").field("value"));
    }

    private void thenReceivedPropertiesForPidContains(final String pid, final String expectedKey,
            final Object expectedValue) {
        assertEquals(expectedValue, receivedConfigsByPid.get(pid).get(expectedKey));
    }

    private void thenReceivedPropertiesForPidContainsArray(final String pid, final String expectedKey,
            final Object[] expectedValue) {
        assertArrayEquals(expectedValue, (Object[]) receivedConfigsByPid.get(pid).get(expectedKey));
    }

    private void thenReceivedPropertiesForPidContainsPassword(final String pid, final String expectedKey,
            final String expectedValue) {
        assertEquals(expectedValue,
                new String(((Password) receivedConfigsByPid.get(pid).get(expectedKey)).getPassword()));
    }

    private void thenReceivedPropertiesForPidContainsPasswords(final String pid, final String expectedKey,
            final String... expectedValues) {
        final Password[] passwords = (Password[]) receivedConfigsByPid.get(pid).get(expectedKey);

        for (int i = 0; i < expectedValues.length; i++) {
            assertEquals(expectedValues[i], new String(passwords[i].getPassword()));
        }

    }
}

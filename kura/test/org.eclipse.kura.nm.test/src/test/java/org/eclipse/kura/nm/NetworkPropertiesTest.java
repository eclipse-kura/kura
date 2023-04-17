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
package org.eclipse.kura.nm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.eclipse.kura.configuration.Password;
import org.junit.Test;

public class NetworkPropertiesTest {

    private NetworkProperties netProps;
    private Map<String, Object> properties = new HashMap<>();
    private Optional<?> optResult;
    private String stringResult;
    private Boolean booleanResult;
    private Short shortResult;
    private Integer intResult;
    private Long longResult;
    private Password passwordResult;

    private List<String> stringListResult;
    private Map<String, Object> resultMap;

    private Boolean hasNullPointExceptionBeenThrown = false;
    private Boolean hasNoSuchElementExceptionBeenThrown = false;

    @Test
    public void constructorShouldThrowWithNullMap() {
        givenNetworkPropertiesBuiltWith(null);
        thenANullPointerExceptionOccured();
    }

    @Test
    public void getPropertiesShouldWorkWithEmptyMap() {
        givenNetworkPropertiesBuiltWith(new HashMap<String, Object>());
        whenGetPropertiesIsCalled();
        thenNoExceptionsOccured();
        thenMapResultEquals(new HashMap<String, Object>());
    }

    @Test
    public void getPropertiesShouldWorkWithSimpleMap() {
        givenMapWith("testKey1", "testString1");
        givenNetworkPropertiesBuiltWith(this.properties);
        whenGetPropertiesIsCalled();
        thenNoExceptionsOccured();
        thenMapResultEquals(Collections.singletonMap("testKey1", "testString1"));
    }

    @Test
    public void getShouldWorkWithString() {
        givenMapWith("testKey1", "testString1");
        givenNetworkPropertiesBuiltWith(this.properties);
        whenGetIsCalledWith("testKey1", String.class);
        thenNoExceptionsOccured();
        thenStringResultEquals("testString1");
    }

    @Test
    public void getShouldWorkWithDecryptedPassword() {
        givenMapWith("testKey1", new Password("testPassword1"));
        givenNetworkPropertiesBuiltWith(this.properties);
        whenGetIsCalledWith("testKey1", Password.class);
        thenNoExceptionsOccured();
        thenPasswordResultEquals(new Password("testPassword1"));
    }

    @Test
    public void getShouldThrowWithNullValue() {
        givenMapWith("testKey1", null);
        givenNetworkPropertiesBuiltWith(this.properties);
        whenGetIsCalledWith("testKey1", String.class);
        thenANoSuchElementExceptionOccured();
    }

    @Test
    public void getShouldThrowWithMissingKey() {
        givenMapWith("testKey1", null);
        givenNetworkPropertiesBuiltWith(this.properties);
        whenGetIsCalledWith("testKey1-nonExistant", String.class);
        thenANoSuchElementExceptionOccured();
    }

    @Test
    public void getShouldThrowWithEmptyString() {
        givenMapWith("Empty-String", "");
        givenNetworkPropertiesBuiltWith(this.properties);
        whenGetIsCalledWith("Empty-String", String.class);
        thenANoSuchElementExceptionOccured();
    }

    @Test
    public void getShouldThrowWithEmptyPassword() {
        givenMapWith("Empty-Password", new Password(""));
        givenNetworkPropertiesBuiltWith(this.properties);
        whenGetIsCalledWith("Empty-Password", Password.class);
        thenANoSuchElementExceptionOccured();
    }

    @Test
    public void getShouldWorkWithEmptyStringKey() {
        givenMapWith("", "Empty String Test");
        givenNetworkPropertiesBuiltWith(this.properties);
        whenGetIsCalledWith("", String.class);
        thenNoExceptionsOccured();
        thenStringResultEquals("Empty String Test");
    }

    @Test
    public void getShouldWorkWithBoolean() {
        givenMapWith("testKey1", false);
        givenNetworkPropertiesBuiltWith(this.properties);
        whenGetIsCalledWith("testKey1", Boolean.class);
        thenNoExceptionsOccured();
        thenBooleanResultEquals(false);
    }

    @Test
    public void getShouldWorkWithShort() {
        givenMapWith("testKey1", Short.valueOf((short) 10));
        givenNetworkPropertiesBuiltWith(this.properties);
        whenGetIsCalledWith("testKey1", Short.class);
        thenNoExceptionsOccured();
        thenShortResultEquals(Short.valueOf((short) 10));
    }

    @Test
    public void getShouldWorkWithInteger() {
        givenMapWith("testKey1", Integer.valueOf(34));
        givenNetworkPropertiesBuiltWith(this.properties);
        whenGetIsCalledWith("testKey1", Integer.class);
        thenNoExceptionsOccured();
        thenIntResultEquals(Integer.valueOf(34));
    }

    @Test
    public void getShouldWorkWithLong() {
        givenMapWith("testKey1", Long.valueOf(23324234));
        givenNetworkPropertiesBuiltWith(this.properties);
        whenGetIsCalledWith("testKey1", Long.class);
        thenNoExceptionsOccured();
        thenLongResultEquals(Long.valueOf(23324234));
    }

    @Test
    public void getOptShouldWorkWithString() {
        givenMapWith("testKey1", "testString1");
        givenNetworkPropertiesBuiltWith(this.properties);
        whenGetOptIsCalledWith("testKey1", String.class);
        thenNoExceptionsOccured();
        thenOptionalResultEquals(Optional.of("testString1"));
    }

    @Test
    public void getOptShouldWorkWithBoolean() {
        givenMapWith("testKey1", true);
        givenNetworkPropertiesBuiltWith(this.properties);
        whenGetOptIsCalledWith("testKey1", Boolean.class);
        thenNoExceptionsOccured();
        thenOptionalResultEquals(Optional.of(true));
    }

    @Test
    public void getOptShouldWorkWithShort() {
        givenMapWith("testKey1", (short) 42);
        givenNetworkPropertiesBuiltWith(this.properties);
        whenGetOptIsCalledWith("testKey1", Short.class);
        thenNoExceptionsOccured();
        thenOptionalResultEquals(Optional.of((short) 42));
    }

    @Test
    public void getOptShouldWorkWithInteger() {
        givenMapWith("testKey1", 42);
        givenNetworkPropertiesBuiltWith(this.properties);
        whenGetOptIsCalledWith("testKey1", Integer.class);
        thenNoExceptionsOccured();
        thenOptionalResultEquals(Optional.of(42));
    }

    @Test
    public void getOptShouldWorkWithLong() {
        givenMapWith("testKey1", (long) 4738758);
        givenNetworkPropertiesBuiltWith(this.properties);
        whenGetOptIsCalledWith("testKey1", Long.class);
        thenNoExceptionsOccured();
        thenOptionalResultEquals(Optional.of((long) 4738758));
    }

    @Test
    public void getOptShouldWorkWithEmptyStringValue() {
        givenMapWith("testKeyEmpty", "");
        givenNetworkPropertiesBuiltWith(this.properties);
        whenGetOptIsCalledWith("testKeyEmpty", String.class);
        thenNoExceptionsOccured();
        thenOptionalResultEquals(Optional.empty());
    }

    @Test
    public void getOptShouldWorkWithNullValue() {
        givenMapWith("testKeyNull", null);
        givenNetworkPropertiesBuiltWith(this.properties);
        whenGetOptIsCalledWith("testKeyNull", String.class);
        thenNoExceptionsOccured();
        thenOptionalResultEquals(Optional.empty());
    }

    @Test
    public void getOptShouldWorkWithEmptyKey() {
        givenMapWith("", "test value");
        givenMapWith("testKey1", "testString1");
        givenMapWith("testKeyNull2", null);
        givenNetworkPropertiesBuiltWith(this.properties);
        whenGetOptIsCalledWith("", String.class);
        thenNoExceptionsOccured();
        thenOptionalResultEquals(Optional.of("test value"));
    }

    @Test
    public void getOptShouldWorkWithDecryptedPassword() {
        givenMapWith("aPassword", new Password("superSecurePassword"));
        givenNetworkPropertiesBuiltWith(this.properties);
        whenGetOptIsCalledWith("aPassword", Password.class);
        thenNoExceptionsOccured();
        thenOptionalPasswordResultEquals(Optional.of(new Password("superSecurePassword")));
    }

    @Test
    public void getOptShouldWorkWithDecryptedEmptyPassword() {
        givenMapWith("aPassword", new Password(""));
        givenNetworkPropertiesBuiltWith(this.properties);
        whenGetOptIsCalledWith("aPassword", Password.class);
        thenNoExceptionsOccured();
        thenOptionalPasswordResultEquals(Optional.empty());
    }

    @Test
    public void getStringListShouldWorkWithSimpleMap() {
        givenMapWith("testKeyNull", null);
        givenMapWith("testKey1", "testString1");
        givenMapWith("testKey-comma-seperated", "commaSeparated1,commaSeparated2,commaSeparated3");
        givenNetworkPropertiesBuiltWith(this.properties);
        whenGetStringListIsCalledWith("testKey-comma-seperated");
        thenNoExceptionsOccured();
        thenStringListResultEquals(Arrays.asList("commaSeparated1", "commaSeparated2", "commaSeparated3"));
    }

    @Test
    public void getStringListShouldWorkWithMultipleCommas() {
        givenMapWith("testKeyNull", null);
        givenMapWith("testKey1", "testString1");
        givenMapWith("testKey-comma-seperated", ",,   ,,,commaSeparated1, ,,,,commaSeparated2,   ,,commaSeparated3,");
        givenNetworkPropertiesBuiltWith(this.properties);
        whenGetStringListIsCalledWith("testKey-comma-seperated");
        thenNoExceptionsOccured();
        thenStringListResultEquals(Arrays.asList("commaSeparated1", "commaSeparated2", "commaSeparated3"));
    }

    @Test
    public void getStringListShouldThrowWithNullValue() {
        givenMapWith("testKey-comma-seperated", null);
        givenNetworkPropertiesBuiltWith(this.properties);
        whenGetStringListIsCalledWith("testKey-comma-seperated");
        thenANoSuchElementExceptionOccured();
    }

    @Test
    public void getStringListShouldThrowWithNonExistantKey() {
        givenMapWith("testKey-comma-seperated", null);
        givenNetworkPropertiesBuiltWith(this.properties);
        whenGetStringListIsCalledWith("testKey-comma-seperated-not-existant");
        thenANoSuchElementExceptionOccured();
    }

    @Test
    public void getStringListShouldWorkWithNoCommas() {
        givenMapWith("testKey1", "testString1");
        givenNetworkPropertiesBuiltWith(this.properties);
        whenGetStringListIsCalledWith("testKey1");
        thenNoExceptionsOccured();
        thenStringListResultEquals(Arrays.asList("testString1"));
    }

    @Test
    public void getOptStringListShouldWorkWithSimpleMap() {
        givenMapWith("testKey-comma-seperated", "commaSeparated1,commaSeparated2,commaSeparated3");
        givenNetworkPropertiesBuiltWith(this.properties);
        whenGetOptStringListIsCalledWith("testKey-comma-seperated");
        thenNoExceptionsOccured();
        thenOptionalResultEquals(Optional.of(Arrays.asList("commaSeparated1", "commaSeparated2", "commaSeparated3")));
    }

    @Test
    public void getOptStringListWithSpacesShouldWorkWithSimpleMap() {
        givenMapWith("testKey-comma-seperated", "commaSeparated1 ,commaSeparated2 ,commaSeparated3");
        givenNetworkPropertiesBuiltWith(this.properties);
        whenGetOptStringListIsCalledWith("testKey-comma-seperated");
        thenNoExceptionsOccured();
        thenOptionalResultEquals(Optional.of(Arrays.asList("commaSeparated1", "commaSeparated2", "commaSeparated3")));
    }

    @Test
    public void getOptStringListShouldWorkWithMalformed() {
        givenMapWith("testKey-comma-seperated",
                ", , ,,,,commaSeparated1, , , ,,,,,commaSeparated2,,,, ,, ,,commaSeparated3,, , ,,,, ,");
        givenNetworkPropertiesBuiltWith(this.properties);
        whenGetOptStringListIsCalledWith("testKey-comma-seperated");
        thenNoExceptionsOccured();
        thenOptionalResultEquals(Optional.of(Arrays.asList("commaSeparated1", "commaSeparated2", "commaSeparated3")));
    }

    @Test
    public void getOptStringListShouldWorkWithNoCommas() {
        givenMapWith("testKey1", "testString1");
        givenNetworkPropertiesBuiltWith(this.properties);
        whenGetOptStringListIsCalledWith("testKey1");
        thenNoExceptionsOccured();
        thenOptionalResultEquals(Optional.of(Arrays.asList("testString1")));
    }

    @Test
    public void getOptStringListShouldWorkWithNullValue() {
        givenMapWith("testKeyNull", null);
        givenNetworkPropertiesBuiltWith(this.properties);
        whenGetOptStringListIsCalledWith("testKeyNull");
        thenNoExceptionsOccured();
        thenOptionalResultEquals(Optional.empty());
    }

    @Test
    public void getOptStringListShouldWorkWithMissingKey() {
        givenMapWith("", "Empty String Test");
        givenNetworkPropertiesBuiltWith(this.properties);
        whenGetOptStringListIsCalledWith("");
        thenNoExceptionsOccured();
        thenOptionalResultEquals(Optional.of(Arrays.asList("Empty String Test")));
    }

    /*
     * Given
     */

    public void givenMapWith(String key, Object pair) {
        this.properties.put(key, pair);
    }

    public void givenNetworkPropertiesBuiltWith(Map<String, Object> properties) {

        try {
            this.netProps = new NetworkProperties(properties);

        } catch (NullPointerException e) {
            this.hasNullPointExceptionBeenThrown = true;
        } catch (NoSuchElementException e) {
            this.hasNoSuchElementExceptionBeenThrown = true;
        }
    }

    /*
     * When
     */

    public void whenGetPropertiesIsCalled() {
        this.resultMap = this.netProps.getProperties();
    }

    public void whenGetIsCalledWith(String key, Class<?> clazz) {

        try {
            if (clazz == String.class) {
                this.stringResult = this.netProps.get(String.class, key, "");
            } else if (clazz == Boolean.class) {
                this.booleanResult = this.netProps.get(Boolean.class, key, "");
            } else if (clazz == Short.class) {
                this.shortResult = this.netProps.get(Short.class, key, "");
            } else if (clazz == Integer.class) {
                this.intResult = this.netProps.get(Integer.class, key, "");
            } else if (clazz == Long.class) {
                this.longResult = this.netProps.get(Long.class, key, "");
            } else if (clazz == Password.class) {
                this.passwordResult = this.netProps.get(Password.class, key, "");
            } else {
                throw new IllegalArgumentException("Data type is not supported with this Test");
            }

        } catch (NullPointerException e) {
            this.hasNullPointExceptionBeenThrown = true;
        } catch (NoSuchElementException e) {
            this.hasNoSuchElementExceptionBeenThrown = true;
        }
    }

    public void whenGetOptIsCalledWith(String key, Class<?> clazz) {
        try {
            this.optResult = this.netProps.getOpt(clazz, key, "");
        } catch (NullPointerException e) {
            this.hasNullPointExceptionBeenThrown = true;
        } catch (NoSuchElementException e) {
            this.hasNoSuchElementExceptionBeenThrown = true;
        }
    }

    public void whenGetStringListIsCalledWith(String key) {
        try {
            this.stringListResult = this.netProps.getStringList(key, "");

        } catch (NullPointerException e) {
            this.hasNullPointExceptionBeenThrown = true;
        } catch (NoSuchElementException e) {
            this.hasNoSuchElementExceptionBeenThrown = true;
        }
    }

    public void whenGetOptStringListIsCalledWith(String key) {
        try {
            this.optResult = this.netProps.getOptStringList(key, "");

        } catch (NullPointerException e) {
            this.hasNullPointExceptionBeenThrown = true;
        } catch (NoSuchElementException e) {
            this.hasNoSuchElementExceptionBeenThrown = true;
        }
    }

    /*
     * Then
     */

    public void thenStringResultEquals(String result) {
        assertEquals(result, this.stringResult);
    }

    private void thenPasswordResultEquals(Password result) {
        assertEquals(result.toString(), this.passwordResult.toString());
    }

    public void thenBooleanResultEquals(Boolean result) {
        assertEquals(result, this.booleanResult);
    }

    public void thenShortResultEquals(Short result) {
        assertEquals(result, this.shortResult);
    }

    public void thenIntResultEquals(Integer result) {
        assertEquals(result, this.intResult);
    }

    public void thenLongResultEquals(Long result) {
        assertEquals(result, this.longResult);
    }

    public void thenMapResultEquals(Map<String, Object> result) {
        assertEquals(result, this.resultMap);
    }

    public void thenOptionalResultEquals(Optional<?> result) {
        assertEquals(result, this.optResult);
    }

    public void thenStringListResultEquals(List<String> result) {
        assertEquals(result, this.stringListResult);
    }

    private void thenOptionalPasswordResultEquals(Optional<Password> optPasswordResult) {
        if (optPasswordResult.isPresent()) {
            // Workaround to compare Password.class since it doesn't have an equals method
            assertEquals(optPasswordResult.get().toString(), this.optResult.get().toString());
        } else {
            assertEquals(optPasswordResult, this.optResult);
        }
    }

    public void thenANullPointerExceptionOccured() {
        assertTrue(this.hasNullPointExceptionBeenThrown);
    }

    public void thenANoSuchElementExceptionOccured() {
        assertTrue(this.hasNoSuchElementExceptionBeenThrown);
    }

    public void thenNoExceptionsOccured() {
        assertFalse(this.hasNullPointExceptionBeenThrown);
        assertFalse(this.hasNoSuchElementExceptionBeenThrown);
    }

}

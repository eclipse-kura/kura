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
package org.eclipse.kura.nm.configuration;

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

import org.freedesktop.dbus.errors.NotSupported;
import org.junit.Test;

public class NetworkPropertiesTest {

	NetworkProperties netProps;
	Map<String, Object> properties = new HashMap<String, Object>();
	Optional<?> optResult;
	String stringResult;
	Boolean booleanResult;
	Short shortResult;
	Integer intResult;
	Long longResult;

	List<String> stringListResult;
	Map<String, Object> resultMap;

	Boolean hasNullPointExceptionBeenThrown = false;
	Boolean hasNoSuchElementExceptionBeenThrown = false;

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
	public void getShouldReturnStringWithEmptyString() {
		givenMapWith("Empty-String", "");
		givenNetworkPropertiesBuiltWith(this.properties);
		whenGetIsCalledWith("Empty-String", String.class);
		thenNoExceptionsOccured();
		thenANoSuchElementExceptionOccured();
	}
	
	@Test
	public void getShouldThrowWithEmptyStringKey() {
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
	public void getShouldReturnShort() {
		givenMapWith("testKey1", Short.valueOf((short) 10));
		givenNetworkPropertiesBuiltWith(this.properties);
		whenGetIsCalledWith("testKey1", Short.class);
		thenNoExceptionsOccured();
		thenShortResultEquals(Short.valueOf((short) 10));
	}

	@Test
	public void getShouldReturnInt() {
		givenMapWith("testKey1", Integer.valueOf(34));
		givenNetworkPropertiesBuiltWith(this.properties);
		whenGetIsCalledWith("testKey1", Integer.class);
		thenNoExceptionsOccured();
		thenIntResultEquals(Integer.valueOf(34));
	}

	@Test
	public void getShouldReturnLong() {
		givenMapWith("testKey1", Long.valueOf(23324234));
		givenNetworkPropertiesBuiltWith(this.properties);
		whenGetIsCalledWith("testKey1", Long.class);
		thenNoExceptionsOccured();
		thenLongResultEquals(Long.valueOf(23324234));
	}

	@Test
	public void getOptShouldReturnString() {
		givenMapWith("testKey1", "testString1");
		givenNetworkPropertiesBuiltWith(this.properties);
		whenGetOptIsCalledWith("testKey1", String.class);
		thenNoExceptionsOccured();
		thenOptionalResultEquals(Optional.of("testString1"));
	}

	@Test
	public void getOptShouldReturnBoolean() {
		givenMapWith("testKey1", true);
		givenNetworkPropertiesBuiltWith(this.properties);
		whenGetOptIsCalledWith("testKey1", Boolean.class);
		thenNoExceptionsOccured();
		thenOptionalResultEquals(Optional.of(true));
	}

	@Test
	public void getOptShouldReturnShort() {
		givenMapWith("testKey1", (short) 42);
		givenNetworkPropertiesBuiltWith(this.properties);
		whenGetOptIsCalledWith("testKey1", Short.class);
		thenNoExceptionsOccured();
		thenOptionalResultEquals(Optional.of((short) 42));
	}

	@Test
	public void getOptShouldReturnInteger() {
		givenMapWith("testKey1", (int) 42);
		givenNetworkPropertiesBuiltWith(this.properties);
		whenGetOptIsCalledWith("testKey1", Integer.class);
		thenNoExceptionsOccured();
		thenOptionalResultEquals(Optional.of((int) 42));
	}

	@Test
	public void getOptShouldReturnLong() {
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
		thenOptionalResultEquals(Optional.of("test value"));
	}

	@Test
	public void getStringListShouldWorkWithSimpleMap() {
		givenMapWith("testKeyNull", null);
		givenMapWith("testKey1", "testString1");
		givenMapWith("testKey-comma-seperated", "commaSeperated1,commaSeperated2,commaSeperated3");
		givenNetworkPropertiesBuiltWith(this.properties);
		whenGetStringListIsCalledWith("testKey-comma-seperated");
		thenNoExceptionsOccured();
		thenStringListResultEquals(Arrays.asList("commaSeperated1", "commaSeperated2", "commaSeperated3"));
	}

	@Test
	public void getStringListShouldWorkWithMultipleCommas() {
		givenMapWith("testKeyNull", null);
		givenMapWith("testKey1", "testString1");
		givenMapWith("testKey-comma-seperated", ",,   ,,,commaSeperated1, ,,,,commaSeperated2,   ,,commaSeperated3,");
		givenNetworkPropertiesBuiltWith(this.properties);
		whenGetStringListIsCalledWith("testKey-comma-seperated");
		thenNoExceptionsOccured();
		thenStringListResultEquals(Arrays.asList("commaSeperated1", "commaSeperated2", "commaSeperated3"));
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
		givenMapWith("testKey-comma-seperated", "commaSeperated1,commaSeperated2,commaSeperated3");
		givenNetworkPropertiesBuiltWith(this.properties);
		whenGetOptStringListIsCalledWith("testKey-comma-seperated");
		thenNoExceptionsOccured();
		thenOptionalResultEquals(Optional.of(Arrays.asList("commaSeperated1", "commaSeperated2", "commaSeperated3")));
	}

	@Test
	public void getOptStringListShouldWorkWithMalformed() {
		givenMapWith("testKey-comma-seperated",
				", , ,,,,commaSeperated1, , , ,,,,,commaSeperated2,,,, ,, ,,commaSeperated3,, , ,,,, ,");
		givenNetworkPropertiesBuiltWith(this.properties);
		whenGetOptStringListIsCalledWith("testKey-comma-seperated");
		thenNoExceptionsOccured();
		thenOptionalResultEquals(Optional.of(Arrays.asList("commaSeperated1", "commaSeperated2", "commaSeperated3")));
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
		thenOptionalResultEquals(Optional.empty());
	}

	@Test
	public void getOptStringListShouldWorkWithMissingKey() {
		givenMapWith("", "Empty String Test");
		givenNetworkPropertiesBuiltWith(this.properties);
		whenGetOptStringListIsCalledWith("");
		thenOptionalResultEquals(Optional.of(Arrays.asList("Empty String Test")));
	}

	/*
	 * Given
	 */

	public void givenMapWith(String key, Object pair) {
		properties.put(key, pair);
	}

	public void givenNetworkPropertiesBuiltWith(Map<String, Object> properties) {

		try {
			netProps = new NetworkProperties(properties);

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
			} else {
				throw new NotSupported("Data type is not supported with this Test");
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
		assertEquals(this.resultMap, result);
	}

	public void thenOptionalResultEquals(Optional<?> result) {
		assertEquals(this.optResult, result);
	}

	public void thenStringListResultEquals(List<String> result) {
		assertEquals(this.stringListResult, result);
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

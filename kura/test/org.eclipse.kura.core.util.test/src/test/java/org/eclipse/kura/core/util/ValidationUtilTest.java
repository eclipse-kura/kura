/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.kura.core.util;

import static org.junit.Assert.*;

import org.eclipse.kura.KuraException;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ValidationUtilTest {

	@Test
	public void testNotNullWithObject() throws KuraException {
		Object value = new Object();
		ValidationUtil.notNull(value, "value");
	}

	@Test(expected = KuraException.class)
	public void testNotNullWithNullObject() throws KuraException {
		Object value = null;
		ValidationUtil.notNull(value, "value");
	}

	@Test
	public void testNotEmptyOrNullWithNonEmptyString() throws KuraException {
		String value = "xyz";
		ValidationUtil.notEmptyOrNull(value, "value");
	}

	@Test(expected = KuraException.class)
	public void testNotEmptyOrNullWithNullString() throws KuraException {
		String value = null;
		ValidationUtil.notEmptyOrNull(value, "value");
	}

	@Test(expected = KuraException.class)
	public void testNotEmptyOrNullWithEmptyString() throws KuraException {
		String value = "";
		ValidationUtil.notEmptyOrNull(value, "value");
	}

	@Test(expected = KuraException.class)
	public void testNotEmptyOrNullWithWhitespace() throws KuraException {
		String value = " \t\n\r\f";
		ValidationUtil.notEmptyOrNull(value, "value");
	}

	@Test
	public void testNotNegativeIntStringWithZero() throws KuraException {
		int value = 0;
		ValidationUtil.notNegative(value, "value");
	}

	@Test
	public void testNotNegativeIntStringWithPositive() throws KuraException {
		int value = 1;
		ValidationUtil.notNegative(value, "value");
	}

	@Test(expected = KuraException.class)
	public void testNotNegativeIntStringWithNegative() throws KuraException {
		int value = -1;
		ValidationUtil.notNegative(value, "value");
	}

	@Test
	public void testNotNegativeShortStringWithZero() throws KuraException {
		short value = 0;
		ValidationUtil.notNegative(value, "value");
	}

	@Test
	public void testNotNegativeShortStringWithPositive() throws KuraException {
		short value = 1;
		ValidationUtil.notNegative(value, "value");
	}

	@Test(expected = KuraException.class)
	public void testNotNegativeShortStringWithNegative() throws KuraException {
		short value = -1;
		ValidationUtil.notNegative(value, "value");
	}

	@Test
	public void testNotNegativeLongStringWithZero() throws KuraException {
		long value = 0;
		ValidationUtil.notNegative(value, "value");
	}

	@Test
	public void testNotNegativeLongStringWithPositive() throws KuraException {
		long value = 1;
		ValidationUtil.notNegative(value, "value");
	}

	@Test(expected = KuraException.class)
	public void testNotNegativeLongStringWithNegative() throws KuraException {
		long value = -1;
		ValidationUtil.notNegative(value, "value");
	}
}

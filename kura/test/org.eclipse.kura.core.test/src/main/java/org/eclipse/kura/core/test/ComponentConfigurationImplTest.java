/*******************************************************************************
 * Copyright (c) 2011, 2017 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.core.test;

import org.eclipse.kura.core.configuration.util.StringUtil;
import org.eclipse.kura.test.annotation.TestTarget;
import org.junit.Test;

import junit.framework.TestCase;

public class ComponentConfigurationImplTest extends TestCase {

    @TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
    @Test
    public void testSplitDefaultValues() throws Exception {
        String defaultString = "abc, def, 124  , qwer\\, ty , ed, \\ \\ spa ce\\ , 789";
        String[] defaultValues = StringUtil.splitValues(defaultString);
        for (String s : defaultValues) {
            System.err.println(s);
        }
        assertEquals(7, defaultValues.length);
        assertEquals("abc", defaultValues[0]);
        assertEquals("def", defaultValues[1]);
        assertEquals("124", defaultValues[2]);
        assertEquals("qwer, ty", defaultValues[3]);
        assertEquals("ed", defaultValues[4]);
        assertEquals("  spa ce ", defaultValues[5]);
        assertEquals("789", defaultValues[6]);

        String expected = "abc,def,124,qwer\\,\\ ty,ed,\\ \\ spa\\ ce\\ ,789";
        String joined = StringUtil.valueToString(defaultValues);
        assertEquals(expected, joined);
    }

    @TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
    @Test
    public void testSplitDefaultValues2() throws Exception {
        String defaultString = "  a\\,b,b\\,c,\\ c\\\\,d   ";
        String[] defaultValues = StringUtil.splitValues(defaultString);

        assertEquals(4, defaultValues.length);
        assertEquals("a,b", defaultValues[0]);
        assertEquals("b,c", defaultValues[1]);
        assertEquals(" c\\", defaultValues[2]);
        assertEquals("d", defaultValues[3]);
    }
}

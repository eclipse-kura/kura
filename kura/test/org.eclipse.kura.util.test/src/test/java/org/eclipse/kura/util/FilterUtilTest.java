/*******************************************************************************
 * Copyright (c) 2018 Red Hat Inc.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/

package org.eclipse.kura.util;

import static org.eclipse.kura.util.osgi.FilterUtil.and;
import static org.eclipse.kura.util.osgi.FilterUtil.equal;
import static org.eclipse.kura.util.osgi.FilterUtil.not;
import static org.eclipse.kura.util.osgi.FilterUtil.or;
import static org.junit.Assert.assertEquals;
import static org.osgi.framework.FrameworkUtil.createFilter;

import org.eclipse.kura.util.base.StringUtil;
import org.eclipse.kura.util.osgi.FilterUtil;
import org.junit.Assert;
import org.junit.Test;
import org.osgi.framework.InvalidSyntaxException;

public class FilterUtilTest {

    @Test
    public void testEqual1() throws InvalidSyntaxException {
        testFilter("(foo=bar)", equal("foo", "bar"));
    }

    @Test
    public void testEqual2() throws InvalidSyntaxException {
        testFilter("", equal(null, "bar"));
    }

    @Test
    public void testAnd1() throws InvalidSyntaxException {
        testFilter("(&(foo1=bar1)(foo2=bar2))", and(equal("foo1", "bar1"), equal("foo2", "bar2")));
    }

    @Test
    public void testAnd2() throws InvalidSyntaxException {
        testFilter("(&(foo1=bar1)(foo2=bar2)(foo3=bar3))",
                and(equal("foo1", "bar1"), equal("foo2", "bar2"), equal("foo3", "bar3")));
    }

    @Test
    public void testAnd3() throws InvalidSyntaxException {
        testFilter("(foo1=bar1)", and(equal("foo1", "bar1"), equal("", "bar2")));
    }

    @Test
    public void testSpecialAnd1() throws InvalidSyntaxException {
        testFilter("(foo1=bar1)", and(equal("foo1", "bar1")));
    }

    @Test
    public void testOr1() throws InvalidSyntaxException {
        testFilter("(|(foo1=bar1)(foo2=bar2))", or(equal("foo1", "bar1"), equal("foo2", "bar2")));
    }

    @Test
    public void testNot1() throws InvalidSyntaxException {
        testFilter("(!(foo1=bar1))", not(equal("foo1", "bar1")));
    }

    @Test
    public void testNot2() throws InvalidSyntaxException {
        testFilter("", not(equal("", "bar1")));
    }

    @Test
    public void testSimpleFilter() throws InvalidSyntaxException {
        testFilter("(&(objectClass=org.eclipse.kura.util.FilterUtilTest)(foo=bar))",
                FilterUtil.simpleFilter(FilterUtilTest.class, "foo", "bar"));
    }

    @Test
    public void testQuote1() throws InvalidSyntaxException {
        testFilter("(foo=bar\\*bar)", equal("foo", "bar*bar"));
    }

    @Test
    public void testQuote2() throws InvalidSyntaxException {
        testFilter("(foo\\*foo=bar\\*bar)", equal("foo*foo", "bar*bar"));
    }

    @Test
    public void testQuoteNull() throws InvalidSyntaxException {
        Assert.assertNull(FilterUtil.quote(null));
    }

    private void testFilter(final String expected, final String actual) throws InvalidSyntaxException {
        if (!StringUtil.isNullOrEmpty(expected)) {
            createFilter(expected);
        }

        if (!StringUtil.isNullOrEmpty(actual)) {
            createFilter(actual);
        }

        assertEquals(expected, actual);
    }
}

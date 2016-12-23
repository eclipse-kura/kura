/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class Test {

    private Method m_beforeClass;
    private Method m_afterClass;
    private Method m_before;
    private Method m_after;
    private final List<Method> tests;

    public Test() {
        this.tests = new ArrayList<Method>();
    }

    public Method getBeforeClass() {
        return this.m_beforeClass;
    }

    public void setBeforeClass(Method beforeClass) {
        this.m_beforeClass = beforeClass;
    }

    public Method getAfterClass() {
        return this.m_afterClass;
    }

    public void setAfterClass(Method afterClass) {
        this.m_afterClass = afterClass;
    }

    public Method getBefore() {
        return this.m_before;
    }

    public void setBefore(Method before) {
        this.m_before = before;
    }

    public Method getAfter() {
        return this.m_after;
    }

    public void setAfter(Method after) {
        this.m_after = after;
    }

    public List<Method> getTests() {
        return this.tests;
    }

    public void addTest(Method method) {
        this.tests.add(method);
    }
}
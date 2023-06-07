/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class Test {

    private Method beforeClass;
    private Method afterClass;
    private Method before;
    private Method after;
    private final List<Method> tests;

    public Test() {
        this.tests = new ArrayList<Method>();
    }

    public Method getBeforeClass() {
        return this.beforeClass;
    }

    public void setBeforeClass(Method beforeClass) {
        this.beforeClass = beforeClass;
    }

    public Method getAfterClass() {
        return this.afterClass;
    }

    public void setAfterClass(Method afterClass) {
        this.afterClass = afterClass;
    }

    public Method getBefore() {
        return this.before;
    }

    public void setBefore(Method before) {
        this.before = before;
    }

    public Method getAfter() {
        return this.after;
    }

    public void setAfter(Method after) {
        this.after = after;
    }

    public List<Method> getTests() {
        return this.tests;
    }

    public void addTest(Method method) {
        this.tests.add(method);
    }
}
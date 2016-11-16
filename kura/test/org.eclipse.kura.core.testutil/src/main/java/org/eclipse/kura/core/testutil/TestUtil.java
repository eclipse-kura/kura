/*******************************************************************************
 * Copyright (c) 2016 Eurotech and/or its affiliates and others
 *
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.kura.core.testutil;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestUtil {

    private static final Logger logger = LoggerFactory.getLogger(TestUtil.class);

    private static Field getField(Object svc, String fieldName) throws NoSuchFieldException {
        Field field = null;
        Class clazz = svc.getClass();
        while (!(clazz == Object.class || field != null)) {
            try {
                field = clazz.getDeclaredField(fieldName);
                break;
            } catch (NoSuchFieldException e) {
                // don't worry about it, here
            }
            clazz = clazz.getSuperclass();
        }

        if (field == null) {
            throw new NoSuchFieldException(String.format("Field not found: %s", fieldName));
        }

        return field;
    }

    public static Object getFieldValue(Object svc, String fieldName) throws NoSuchFieldException {
        Object result = null;

        Field field = getField(svc, fieldName);

        try {
            field.setAccessible(true);
            result = field.get(svc);
        } catch (IllegalArgumentException e) {
            logger.warn(e.getMessage(), e);
        } catch (IllegalAccessException e) {
            logger.warn(e.getMessage(), e);
        }

        return result;
    }

    private static Method getMethod(Object svc, String methodName, Class... paramTypes) throws NoSuchMethodException {
        Method method = null;
        Class<?> clazz = svc.getClass();
        hwile: while (!(clazz == Object.class || method != null)) {
            Method[] methods = clazz.getDeclaredMethods();
            for (Method m : methods) {
                if (m.getName().compareTo(methodName) == 0) {
                    method = m;
                    break hwile;
                }
            }
            clazz = clazz.getSuperclass();
        }

        if (method == null) {
            throw new NoSuchMethodException(String.format("Method not found: %s", methodName));
        }

        return method;
    }

    public static Object invokePrivate(Object svc, String methodName, Object... params) throws Throwable {
        Method method = getMethod(svc, methodName);

        method.setAccessible(true);

        try {
            Object result = method.invoke(svc, params);
            return result;
        } catch (IllegalAccessException e) {
            logger.warn(e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            logger.warn(e.getMessage(), e);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }

        return null;
    }

    public static void setFieldValue(Object svc, String fieldName, Object value) throws NoSuchFieldException {
        Field field = getField(svc, fieldName);

        field.setAccessible(true);

        try {
            field.set(svc, value);
        } catch (IllegalArgumentException e) {
            logger.warn(e.getMessage(), e);
        } catch (IllegalAccessException e) {
            logger.warn(e.getMessage(), e);
        }
    }

}

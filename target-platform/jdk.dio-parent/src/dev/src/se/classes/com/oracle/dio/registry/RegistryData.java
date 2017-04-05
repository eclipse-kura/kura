/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package com.oracle.dio.registry;

import java.util.Hashtable;
import java.util.Enumeration;

public final class RegistryData {
    private final Hashtable<String, Object> data;

    public RegistryData() {
        data = new Hashtable<>();
    }

    private RegistryData(Hashtable<String, Object> data) {
        this.data = data;
    }

    public String getCharacterProperty(String key) {
        return (String)data.get(key);
    }

    public void putCharacterProperty(String key, String value) {
        data.put(key, value);
    }

    public int getIntegerProperty(String key, int def) {
        return (int)getIntegerProperty(key, (long)def);
    }

    public long getIntegerProperty(String key, long def) {
        String value = (String)data.get(key);
        if (value == null) {
            return def;
        }
        try {
            int radix = 10;
            if (value.startsWith("0x")) {
                value = value.substring(2);
                radix = 16;
            }
            return Long.parseLong(value, radix);
        } catch (NumberFormatException | IndexOutOfBoundsException e) {
            return def;
        }
    }

    public void putIntegerProperty(String key, long value, int radix) {
        data.put(key, Long.toString(value, radix));
    }

    public void putIntegerProperty(String key, int value) {
        data.put(key, Integer.toString(value));
    }

    public boolean getBooleanProperty(String key, boolean def) {
        String value = (String)data.get(key);
        if (value == null) {
            return def;
        }
        if ("true".equals(value) || "1".equals(value) || "yes".equals(value)) {
            return true;
        } else if ("false".equals(value) || "0".equals(value) || "no".equals(value)) {
            return false;
        }
        return def;
    }

    public void putBooleanProperty(String key, boolean value) {
        data.put(key, Boolean.toString(value));
    }

    public RegistryData getCompoundProperty(String key) {
        return (RegistryData)data.get(key);
    }

    public void putCompoundProperty(String key, RegistryData value) {
        data.put(key, value);
    }

    public RegistryList getListProperty(String key) {
        return (RegistryList)data.get(key);
    }

    public void putListProperty(String key, RegistryList value) {
        data.put(key, value);
    }

    public Enumeration<String> keys() {
        return (Enumeration<String>)data.keys();
    }

    public boolean hasProperty(String key) {
        return data.containsKey(key);
    }

    public boolean isEmpty() {
        return data.isEmpty();
    }

    public void copyProperty(String key, RegistryData source) {
        Object value = source.data.get(key);
        data.put(key, value);
    }

    @Override
    public String toString() {
        StringBuffer output = new StringBuffer();
        Enumeration<String> keys = (Enumeration<String>)data.keys();
        while (keys.hasMoreElements()) {
            String key = keys.nextElement();
            output.append(key + ':');
            Object value = data.get(key);
            if (value instanceof RegistryData) {
                output.append('{');
                output.append(value.toString());
                output.append('}');
            } else {
                output.append(value.toString());
            }
            output.append(',');
        }
        return output.toString();
    }

    @Override
    public Object clone() {
        return new RegistryData((Hashtable<String, Object>)data.clone());
    }
}

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

final class RegistryContent {
    private final Hashtable<Integer, RegistryData> entries;
    private final RegistryData defaults;

    public RegistryContent() {
        entries = new Hashtable<>();
        defaults = new RegistryData();
    }

    public void put(int id, RegistryData data) {
        entries.put(id, data);
    }

    public RegistryData get(int id) {
        return entries.get(id);
    }

    public void remove(int id) {
        entries.remove(id);
    }

    public Enumeration<Integer> entries() {
        return (Enumeration<Integer>)entries.keys();
    }

    public void putType(String type, RegistryData typeDefaults) {
        defaults.putCompoundProperty(type, typeDefaults);
    }

    public RegistryData getType(String type) {
        return defaults.getCompoundProperty(type);
    }
}

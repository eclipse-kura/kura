/*******************************************************************************
 * Copyright (c) 2017, 2018 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/

package org.eclipse.kura.wire.script.filter.provider;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.kura.type.TypedValue;

import jdk.nashorn.api.scripting.AbstractJSObject;

class WireRecordWrapper extends AbstractJSObject {

    Map<String, TypedValue<?>> properties;

    public WireRecordWrapper() {
        this.properties = new HashMap<>();
    }

    public WireRecordWrapper(Map<String, TypedValue<?>> properties) {
        this.properties = properties;
    }

    @Override
    public Object getMember(String member) {
        return this.properties.get(member);
    }

    @Override
    public boolean hasMember(String name) {
        return this.properties.containsKey(name);
    }

    @Override
    public Set<String> keySet() {
        return Collections.unmodifiableSet(this.properties.keySet());
    }

    @Override
    public Collection<Object> values() {
        return Collections.unmodifiableCollection(this.properties.values());
    }

    @Override
    public void setMember(String name, Object value) {
        if (value == null) {
            this.properties.remove(name);
            return;
        }
        if (!(value instanceof TypedValue)) {
            throw new IllegalArgumentException("WireRecord properties must be instances of TypedValue");
        }
        this.properties.put(name, (TypedValue<?>) value);
    }

    @Override
    public void removeMember(String name) {
        this.properties.remove(name);
    }
}

/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.oracle.dio.impl;
import java.util.Set;

import jdk.dio.Device;
import jdk.dio.DeviceConfig;
import jdk.dio.DeviceDescriptor;
import com.oracle.dio.utils.Constants;
import com.oracle.dio.utils.Logging;

import romizer.*;

import serializator.*;

/**
 * Default implementation of {@link DeviceDescriptor}
 *
 */
@SerializeMe
@DontRenameClass
public class PeripheralDescriptorImpl<C extends DeviceConfig<T>, T extends Device<? super T>> implements DeviceDescriptor<T> {

    private C config;
    private String clazz;
    private int id = DeviceDescriptor.UNDEFINED_ID;
    private String name;
    private String[] props;

    public PeripheralDescriptorImpl() {
    }

    public PeripheralDescriptorImpl(int id, String name, C config, Class<T> intf, String[] props) {
        this.config = config;
        if (null == config) {
            throw new NullPointerException();
        }
        this.clazz = intf.getName();
        this.id = id;
        this.name = name;
        this.props = (props == null) ? props : props.clone();

    }

    @Override
    public C getConfiguration() {
        return config;
    }

    @Override
    public Class<T> getInterface() {
        try {
            return (Class<T>)Class.forName(clazz);
        } catch (ClassNotFoundException | RuntimeException e) {
            Logging.reportError("Can't restore class at PeripheralDescriptorImpl");
        }
        return null;
    }

    @Override
    public int getID() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String[] getProperties() {
        return (props == null) ? props : props.clone();
    }
}


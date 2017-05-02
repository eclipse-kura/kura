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

import jdk.dio.Device;
import jdk.dio.DeviceDescriptor;
import java.io.IOException;
import com.oracle.dio.utils.Constants;

public abstract class DeviceRegistryFactory {

    public static final String DEVICE_TYPE = "deviceType";
    private static final String FACTORY_POSTFIX = "RegistryFactory";

    public abstract DeviceDescriptor createDeviceDescriptor(int id, RegistryData data) throws IOException;

    public abstract RegistryData createRegistryData(DeviceDescriptor descriptor);

    public static String registryFactoryName(String deviceType) {
        int pos = deviceType.indexOf('.');
        if (pos == -1) {
            return null;
        }
        String pack = deviceType.substring(0, pos);
        String type = deviceType.substring(pos + 1);
        String factoryName = Constants.FACTORY_PREFIX + pack + Constants.IMPL + type + FACTORY_POSTFIX;
        return factoryName;
    }

    public static <T extends Device> String registryFactoryName(Class<T> intf) {
        String clazz = intf.getName();
        if (clazz.indexOf(Constants.PREFIX) != 0) {
            return null;
        }
        String deviceType = clazz.substring(Constants.PREFIX.length());
        return registryFactoryName(deviceType);
    }

    protected String[] getProperties(RegistryData data) {
        RegistryList list = data.getListProperty(Constants.PROPERTIES);
        String[] properties = null;
        if (list != null && list.size() > 0) {
            properties = new String[list.size()];
            try {
                System.arraycopy(list.toArray(), 0, properties, 0, properties.length);
            } catch (ArrayStoreException e) {
                throw new ClassCastException();
            }
        }
        return properties;
    }

    protected void putProperties(RegistryData data, String[] properties) {
        if (properties != null && properties.length > 0) {
            RegistryList list = new RegistryList();
            for (int i = 0; i < properties.length; i++) {
                list.add(properties[i]);
            }
            data.putListProperty(Constants.PROPERTIES, list);
        }
    }
}

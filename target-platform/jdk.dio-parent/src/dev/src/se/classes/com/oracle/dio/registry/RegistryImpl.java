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
import jdk.dio.DeviceConfig;
import jdk.dio.DeviceDescriptor;
import jdk.dio.UnsupportedDeviceTypeException;
import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.Properties;
import java.util.Enumeration;
import java.util.Vector;
import java.util.Collection;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import com.oracle.dio.impl.PeripheralDescriptorImpl;
import com.oracle.dio.utils.Configuration;
import com.oracle.dio.utils.Constants;

class RegistryImpl<T extends Device> extends Registry<T> {

    private static final String REGISTRY_FILE_PATH = "jdk.dio.registry";
    private static final String PREDEFINED = "predefined";

    private WeakReference<Properties> registryCache;
    private WeakReference<RegistryContent> contentCache;

    @Override
    public synchronized DeviceDescriptor<? super T> get(int id) {
        Properties registry = loadRegistry();
        RegistryContent content = readRegistryContent(registry);
        return createDescriptor(id, content);
    }

    @Override
    public synchronized Iterator<DeviceDescriptor<? super T>> get(String name, Class<T> intf, String... properties) {
        return createDescriptorList(name, intf, properties);
    }

    @Override
    public synchronized void register(int id, Class<T> intf,
                                      DeviceConfig<? super T> config,
                                      String name,
                                      String... properties)
                                      throws UnsupportedOperationException, IOException {
        Properties registry = loadRegistry();
        RegistryContent content = readRegistryContent(registry);
        String factory = DeviceRegistryFactory.registryFactoryName(intf);
        if (factory == null) {
            throw new UnsupportedDeviceTypeException("Unsupported type: " + intf.getName());
        }
        DeviceDescriptor<? super T> descriptor = new PeripheralDescriptorImpl(id, name, config, intf, properties);
        RegistryData data = null;
        try {
            data = ((DeviceRegistryFactory)Class.forName(factory).newInstance()).createRegistryData(descriptor);
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            throw new UnsupportedDeviceTypeException("Unsupported type: " + intf.getName());
        }
        content.put(id, data);
        registry.setProperty(Integer.toString(id), data.toString());
        try {
            saveRegistry(registry);
        } catch (Exception e) {
            content.remove(id);
            registry.remove(Integer.toString(id));
            throw e;
        }
    }

    @Override
    public synchronized void unregister(int id) {
        Properties registry = loadRegistry();
        RegistryContent content = readRegistryContent(registry);
        RegistryData config = content.get(id);
        if (config == null) {
            return;
        }
        if (config.getBooleanProperty(PREDEFINED, false)) {
            throw new IllegalArgumentException("Device cannot be unregistered");
        }
        content.remove(id);
        registry.remove(Integer.toString(id));
        try {
            saveRegistry(registry);
        } catch (IOException e) {
        }
    }

    @Override
    public synchronized Iterator<DeviceDescriptor<? super T>> list(Class<T> intf) {
        return createDescriptorList(null, intf);
    }

    private Properties loadRegistry() {
        Properties registry = null;
        if (registryCache != null) {
            registry = registryCache.get();
        }
        if (registry == null) {
            registry = new Properties();
            try {
                String path = Configuration.getSystemProperty(REGISTRY_FILE_PATH);
                if (path != null) {
                    registry.load(new InputStreamReader(new FileInputStream(path)));
                }
            } catch (IOException | SecurityException | IllegalArgumentException e) {
                registry.clear();
            }
            registryCache = new WeakReference(registry);
        }
        return registry;
    }

    private void saveRegistry(Properties registry) throws IOException {
        String path = Configuration.getSystemProperty(REGISTRY_FILE_PATH);
        if (path == null) {
            throw new IOException("Registry is not available");
        }
        registry.store(new OutputStreamWriter(new FileOutputStream(path)), null);
    }

    private RegistryContent readRegistryContent(Properties registry) {
        RegistryContent content = null;
        if (contentCache != null) {
            content = contentCache.get();
        }
        if (content == null) {
            content = new RegistryContent();
            Enumeration<?> names = registry.propertyNames();
            while (names.hasMoreElements()) {
                String name = (String)names.nextElement();
                if (!name.isEmpty()) {
                    String value = registry.getProperty(name);
                    processProperty(name, value, content);
                }
            }
            contentCache = new WeakReference(content);
        }
        return content;
    }

    private void processProperty(String name, String value, RegistryContent content) {
        try {
            int id = Integer.parseInt(name);
            processEntryProperty(id, value, content);
        } catch(NumberFormatException e) {
            processTypeProperty(name, value, content);
        }
    }

    private void processEntryProperty(int id, String value, RegistryContent content) {
        RegistryData data = new RegistryData();
        parseBlock(value, data);
        if (!data.isEmpty()) {
            content.put(id, data);
        }
    }

    private void processTypeProperty(String name, String value, RegistryContent content) {
        RegistryData data = new RegistryData();
        parseBlock(value, data);
        if (!data.isEmpty()) {
            content.putType(name, data);
        }
    }

    private static void parseBlock(String block, RegistryData data) {
        while (true) {
            int inc = 2, pos = block.indexOf("\\:");
            if (pos == -1) {
                inc = 1; pos = block.indexOf(':');
                if (pos == -1) {
                    break;
                }
            }
            String key = block.substring(0, pos).trim();
            block = block.substring(pos + inc).trim();
            if (block.charAt(0) == '[') {
                int end = block.indexOf(']');
                if (end == -1) {
                    break;
                }
                RegistryList list = new RegistryList();
                parseList(block.substring(1, end).trim(), list);
                if (!list.isEmpty()) {
                    data.putListProperty(key, list);
                }
                pos = block.indexOf(',', end + 1);
                if (pos == -1) {
                    break;
                }
                block = block.substring(pos + 1).trim();
            } else {
                int end = block.indexOf(',');
                String value;
                if (end == -1) {
                    value = block;
                    block = "";
                } else {
                    value = block.substring(0, end).trim();
                    block = block.substring(end + 1).trim();
                }
                if (!value.isEmpty()) {
                    data.putCharacterProperty(key, value);
                }
            }
        }
    }

    private static void parseList(String block, RegistryList list) {
        while (!block.isEmpty()) {
            if (block.charAt(0) == '{') {
                int pos = block.indexOf('}');
                if (pos == -1) {
                    break;
                }
                RegistryData data = new RegistryData();
                parseBlock(block.substring(1, pos), data);
                if (!data.isEmpty()) {
                    list.add(data);
                }
                int end = block.indexOf(',', pos + 1);
                if (end == -1) {
                    break;
                }
                block = block.substring(end + 1).trim();
            } else if (block.charAt(0) == '\"') {
                int pos = block.indexOf('\"', 1);
                if (pos == -1) {
                    break;
                }
                String value = block.substring(1, pos).trim();
                if (!value.isEmpty()) {
                    list.add(value);
                }
                int end = block.indexOf(',', pos);
                if (end == -1) {
                    break;
                }
                block = block.substring(end + 1).trim();
            } else {
                break;
            }
        }
    }

    private DeviceDescriptor createDescriptor(int id, RegistryContent content) {
        RegistryData config = content.get(id);
        if (config == null) {
            return null;
        }
        String type = config.getCharacterProperty(DeviceRegistryFactory.DEVICE_TYPE);
        if (type == null) {
            return null;
        }
        String factory = DeviceRegistryFactory.registryFactoryName(type);
        if (factory == null) {
            return null;
        }
        RegistryData defaults = content.getType(type);
        if (defaults != null) {
            config = (RegistryData)config.clone();
            Enumeration<String> keys = defaults.keys();
            while (keys.hasMoreElements()) {
                String key = (String)keys.nextElement();
                if (!config.hasProperty(key)) {
                    config.copyProperty(key, defaults);
                }
            }
        }
        try {
            return ((DeviceRegistryFactory)Class.forName(factory).newInstance()).createDeviceDescriptor(id, config);
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | ClassCastException | IOException e) {
            return null;
        }
    }

    private Iterator<DeviceDescriptor<? super T>> createDescriptorList(String name, Class<T> intf, String... properties) {
        Collection<DeviceDescriptor<? super T>> result = new Vector<>();
        Properties registry = loadRegistry();
        RegistryContent content = readRegistryContent(registry);
        Enumeration<Integer> entries = content.entries();
        while (entries.hasMoreElements()) {
            int id = entries.nextElement().intValue();
            RegistryData config = content.get(id);
            if (intf != null) {
                String deviceType = config.getCharacterProperty(DeviceRegistryFactory.DEVICE_TYPE);
                if (deviceType == null || intf.getName().indexOf(deviceType) == -1) {
                    continue;
                }
            }
            if (name != null) {
                String deviceName = config.getCharacterProperty(Constants.NAME);
                if (deviceName == null || !deviceName.equals(name)) {
                    continue;
                }
            }
            DeviceDescriptor<? super T> descriptor = createDescriptor(id, content);
            if (descriptor == null) {
                continue;
            }
            if (properties != null && properties.length > 0) {
                String[] descriptorProps = descriptor.getProperties();
                if (descriptorProps == null || descriptorProps.length == 0) {
                    continue;
                }
                boolean match = true;
                for (int i = 0; match && (i < properties.length); i++) {
                    match = false;
                    for (int j = 0; j < descriptorProps.length; j++) {
                        if (properties[i].equalsIgnoreCase(descriptorProps[j])) {
                            match = true;
                            break;
                        }
                    }
                }
                if (!match) {
                    continue;
                }
            }
            result.add(descriptor);
        }
        return result.iterator();
    }
}

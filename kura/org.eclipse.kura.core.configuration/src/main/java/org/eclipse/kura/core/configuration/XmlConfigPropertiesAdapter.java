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
package org.eclipse.kura.core.configuration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.kura.configuration.Password;
import org.eclipse.kura.core.configuration.XmlConfigPropertyAdapted.ConfigPropertyType;
import org.eclipse.kura.crypto.CryptoService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

/**
 * Helper class to serialize a set of properties in XML.
 */
public class XmlConfigPropertiesAdapter {

    public XmlConfigPropertiesAdapted marshal(Map<String, Object> props) throws Exception {
        List<XmlConfigPropertyAdapted> adaptedValues = new ArrayList<XmlConfigPropertyAdapted>();
        if (props != null) {

            for (Entry<String, Object> prop : props.entrySet()) {

                XmlConfigPropertyAdapted adaptedValue = new XmlConfigPropertyAdapted();

                String key = prop.getKey();
                adaptedValue.setName(key);

                Object value = prop.getValue();
                if (value instanceof String) {
                    adaptedValue.setArray(false);
                    adaptedValue.setType(ConfigPropertyType.STRING_TYPE);
                    adaptedValue.setValues(new String[] { value.toString() });
                } else if (value instanceof Long) {
                    adaptedValue.setArray(false);
                    adaptedValue.setType(ConfigPropertyType.LONG_TYPE);
                    adaptedValue.setValues(new String[] { value.toString() });
                } else if (value instanceof Double) {
                    adaptedValue.setArray(false);
                    adaptedValue.setType(ConfigPropertyType.DOUBLE_TYPE);
                    adaptedValue.setValues(new String[] { value.toString() });
                } else if (value instanceof Float) {
                    adaptedValue.setArray(false);
                    adaptedValue.setType(ConfigPropertyType.FLOAT_TYPE);
                    adaptedValue.setValues(new String[] { value.toString() });
                } else if (value instanceof Integer) {
                    adaptedValue.setArray(false);
                    adaptedValue.setType(ConfigPropertyType.INTEGER_TYPE);
                    adaptedValue.setValues(new String[] { value.toString() });
                } else if (value instanceof Byte) {
                    adaptedValue.setArray(false);
                    adaptedValue.setType(ConfigPropertyType.BYTE_TYPE);
                    adaptedValue.setValues(new String[] { value.toString() });
                } else if (value instanceof Character) {
                    adaptedValue.setArray(false);
                    adaptedValue.setType(ConfigPropertyType.CHAR_TYPE);
                    adaptedValue.setValues(new String[] { value.toString() });
                } else if (value instanceof Boolean) {
                    adaptedValue.setArray(false);
                    adaptedValue.setType(ConfigPropertyType.BOOLEAN_TYPE);
                    adaptedValue.setValues(new String[] { value.toString() });
                } else if (value instanceof Short) {
                    adaptedValue.setArray(false);
                    adaptedValue.setType(ConfigPropertyType.SHORT_TYPE);
                    adaptedValue.setValues(new String[] { value.toString() });
                } else if (value instanceof Password) {
                    BundleContext bundleContext = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
                    ServiceReference<CryptoService> cryptoServiceRef = bundleContext
                            .getServiceReference(CryptoService.class);
                    try {
                        CryptoService cryptoService = bundleContext.getService(cryptoServiceRef);

                        adaptedValue.setArray(false);
                        adaptedValue.setEncrypted(true);
                        adaptedValue.setType(ConfigPropertyType.PASSWORD_TYPE);
                        adaptedValue.setValues(new String[] { cryptoService.encodeBase64(value.toString()) });
                    } finally {
                        bundleContext.ungetService(cryptoServiceRef);
                    }
                } else if (value instanceof String[]) {
                    adaptedValue.setArray(true);
                    adaptedValue.setType(ConfigPropertyType.STRING_TYPE);
                    adaptedValue.setValues((String[]) value);
                } else if (value instanceof Long[]) {
                    adaptedValue.setArray(true);
                    adaptedValue.setType(ConfigPropertyType.LONG_TYPE);
                    Long[] nativeValues = (Long[]) value;
                    String[] stringValues = new String[nativeValues.length];
                    for (int i = 0; i < nativeValues.length; i++) {
                        if (nativeValues[i] != null) {
                            stringValues[i] = nativeValues[i].toString();
                        }
                    }
                    adaptedValue.setValues(stringValues);
                } else if (value instanceof Double[]) {
                    adaptedValue.setArray(true);
                    adaptedValue.setType(ConfigPropertyType.DOUBLE_TYPE);
                    Double[] nativeValues = (Double[]) value;
                    String[] stringValues = new String[nativeValues.length];
                    for (int i = 0; i < nativeValues.length; i++) {
                        if (nativeValues[i] != null) {
                            stringValues[i] = nativeValues[i].toString();
                        }
                    }
                    adaptedValue.setValues(stringValues);
                } else if (value instanceof Float[]) {
                    adaptedValue.setArray(true);
                    adaptedValue.setType(ConfigPropertyType.FLOAT_TYPE);
                    Float[] nativeValues = (Float[]) value;
                    String[] stringValues = new String[nativeValues.length];
                    for (int i = 0; i < nativeValues.length; i++) {
                        if (nativeValues[i] != null) {
                            stringValues[i] = nativeValues[i].toString();
                        }
                    }
                    adaptedValue.setValues(stringValues);
                } else if (value instanceof Integer[]) {
                    adaptedValue.setArray(true);
                    adaptedValue.setType(ConfigPropertyType.INTEGER_TYPE);
                    Integer[] nativeValues = (Integer[]) value;
                    String[] stringValues = new String[nativeValues.length];
                    for (int i = 0; i < nativeValues.length; i++) {
                        if (nativeValues[i] != null) {
                            stringValues[i] = nativeValues[i].toString();
                        }
                    }
                    adaptedValue.setValues(stringValues);
                } else if (value instanceof Byte[]) {
                    adaptedValue.setArray(true);
                    adaptedValue.setType(ConfigPropertyType.BYTE_TYPE);
                    Byte[] nativeValues = (Byte[]) value;
                    String[] stringValues = new String[nativeValues.length];
                    for (int i = 0; i < nativeValues.length; i++) {
                        if (nativeValues[i] != null) {
                            stringValues[i] = nativeValues[i].toString();
                        }
                    }
                    adaptedValue.setValues(stringValues);
                } else if (value instanceof Character[]) {
                    adaptedValue.setArray(true);
                    adaptedValue.setType(ConfigPropertyType.CHAR_TYPE);
                    Character[] nativeValues = (Character[]) value;
                    String[] stringValues = new String[nativeValues.length];
                    for (int i = 0; i < nativeValues.length; i++) {
                        if (nativeValues[i] != null) {
                            stringValues[i] = nativeValues[i].toString();
                        }
                    }
                    adaptedValue.setValues(stringValues);
                } else if (value instanceof Boolean[]) {
                    adaptedValue.setArray(true);
                    adaptedValue.setType(ConfigPropertyType.BOOLEAN_TYPE);
                    Boolean[] nativeValues = (Boolean[]) value;
                    String[] stringValues = new String[nativeValues.length];
                    for (int i = 0; i < nativeValues.length; i++) {
                        if (nativeValues[i] != null) {
                            stringValues[i] = nativeValues[i].toString();
                        }
                    }
                    adaptedValue.setValues(stringValues);
                } else if (value instanceof Short[]) {
                    adaptedValue.setArray(true);
                    adaptedValue.setType(ConfigPropertyType.SHORT_TYPE);
                    Short[] nativeValues = (Short[]) value;
                    String[] stringValues = new String[nativeValues.length];
                    for (int i = 0; i < nativeValues.length; i++) {
                        if (nativeValues[i] != null) {
                            stringValues[i] = nativeValues[i].toString();
                        }
                    }
                    adaptedValue.setValues(stringValues);
                } else if (value instanceof Password[]) {
                    BundleContext bundleContext = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
                    ServiceReference<CryptoService> cryptoServiceRef = bundleContext
                            .getServiceReference(CryptoService.class);
                    try {
                        CryptoService cryptoService = bundleContext.getService(cryptoServiceRef);

                        adaptedValue.setArray(true);
                        adaptedValue.setEncrypted(true);
                        adaptedValue.setType(ConfigPropertyType.PASSWORD_TYPE);
                        Password[] nativeValues = (Password[]) value;
                        String[] stringValues = new String[nativeValues.length];
                        for (int i = 0; i < nativeValues.length; i++) {
                            if (nativeValues[i] != null) {
                                stringValues[i] = cryptoService.encodeBase64(nativeValues[i].toString());
                            }
                        }
                        adaptedValue.setValues(stringValues);
                    } finally {
                        bundleContext.ungetService(cryptoServiceRef);
                    }
                }

                if (adaptedValue.getValues() == null || adaptedValue.getValues().length > 0) {
                    adaptedValues.add(adaptedValue);
                }
            }
        }

        XmlConfigPropertiesAdapted result = new XmlConfigPropertiesAdapted();
        result.setProperties(adaptedValues.toArray(new XmlConfigPropertyAdapted[] {}));
        return result;
    }

    public Map<String, Object> unmarshal(XmlConfigPropertiesAdapted adaptedPropsAdapted) throws Exception {
        Map<String, Object> properties = new HashMap<String, Object>();
        XmlConfigPropertyAdapted[] adaptedProps = adaptedPropsAdapted.getProperties();
        if (adaptedProps == null) {
            return properties;
        }
        for (XmlConfigPropertyAdapted adaptedProp : adaptedProps) {
            String propName = adaptedProp.getName();
            String[] values = adaptedProp.getValues();
            if (values == null || values.length == 0) {
                properties.put(propName, null);
                continue;
            }
            ConfigPropertyType type = adaptedProp.getType();
            if (type != null) {
                Object propvalue = null;
                if (adaptedProp.getArray() == false) {
                    switch (adaptedProp.getType()) {
                    case STRING_TYPE:
                        propvalue = values[0];
                        break;
                    case LONG_TYPE:
                        propvalue = Long.parseLong(values[0]);
                        break;
                    case DOUBLE_TYPE:
                        propvalue = Double.parseDouble(values[0]);
                        break;
                    case FLOAT_TYPE:
                        propvalue = Float.parseFloat(values[0]);
                        break;
                    case INTEGER_TYPE:
                        propvalue = Integer.parseInt(values[0]);
                        break;
                    case BYTE_TYPE:
                        propvalue = Byte.parseByte(values[0]);
                        break;
                    case CHAR_TYPE:
                        String s = values[0];
                        propvalue = Character.valueOf(s.charAt(0));
                        break;
                    case BOOLEAN_TYPE:
                        propvalue = Boolean.parseBoolean(values[0]);
                        break;
                    case SHORT_TYPE:
                        propvalue = Short.parseShort(values[0]);
                        break;
                    case PASSWORD_TYPE:
                        BundleContext bundleContext = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
                        ServiceReference<CryptoService> cryptoServiceRef = bundleContext
                                .getServiceReference(CryptoService.class);
                        try {
                            CryptoService cryptoService = bundleContext.getService(cryptoServiceRef);

                            propvalue = values[0];
                            if (adaptedProp.isEncrypted()) {
                                propvalue = new Password(cryptoService.decodeBase64((String) propvalue));
                            } else {
                                propvalue = new Password((String) propvalue);
                            }
                        } finally {
                            bundleContext.ungetService(cryptoServiceRef);
                        }
                        break;
                    }
                } else {
                    // If we are dealing with an empty array, skip this element.
                    // Starting from 1.2.0 an empty array will never be present in a snapshot.
                    switch (adaptedProp.getType()) {
                    case STRING_TYPE:
                        propvalue = values;
                        break;
                    case LONG_TYPE:
                        Long[] longValues = new Long[values.length];
                        for (int i = 0; i < values.length; i++) {
                            if (values[i] != null) {
                                longValues[i] = Long.parseLong(values[i]);
                            }
                        }
                        propvalue = longValues;
                        break;
                    case DOUBLE_TYPE:
                        Double[] doubleValues = new Double[values.length];
                        for (int i = 0; i < values.length; i++) {
                            if (values[i] != null) {
                                doubleValues[i] = Double.parseDouble(values[i]);
                            }
                        }
                        propvalue = doubleValues;
                        break;
                    case FLOAT_TYPE:
                        Float[] floatValues = new Float[values.length];
                        for (int i = 0; i < values.length; i++) {
                            if (values[i] != null) {
                                floatValues[i] = Float.parseFloat(values[i]);
                            }
                        }
                        propvalue = floatValues;
                        break;
                    case INTEGER_TYPE:
                        Integer[] intValues = new Integer[values.length];
                        for (int i = 0; i < values.length; i++) {
                            if (values[i] != null) {
                                intValues[i] = Integer.parseInt(values[i]);
                            }
                        }
                        propvalue = intValues;
                        break;
                    case BYTE_TYPE:
                        Byte[] byteValues = new Byte[values.length];
                        for (int i = 0; i < values.length; i++) {
                            if (values[i] != null) {
                                byteValues[i] = Byte.parseByte(values[i]);
                            }
                        }
                        propvalue = byteValues;
                        break;
                    case CHAR_TYPE:
                        Character[] charValues = new Character[values.length];
                        for (int i = 0; i < values.length; i++) {
                            if (values[i] != null) {
                                String s = values[i];
                                charValues[i] = Character.valueOf(s.charAt(0));
                            }
                        }
                        propvalue = charValues;
                        break;
                    case BOOLEAN_TYPE:
                        Boolean[] booleanValues = new Boolean[values.length];
                        for (int i = 0; i < values.length; i++) {
                            if (values[i] != null) {
                                booleanValues[i] = Boolean.parseBoolean(values[i]);
                            }
                        }
                        propvalue = booleanValues;
                        break;
                    case SHORT_TYPE:
                        Short[] shortValues = new Short[values.length];
                        for (int i = 0; i < values.length; i++) {
                            if (values[i] != null) {
                                shortValues[i] = Short.parseShort(values[i]);
                            }
                        }
                        propvalue = shortValues;
                        break;
                    case PASSWORD_TYPE:
                        BundleContext bundleContext = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
                        ServiceReference<CryptoService> cryptoServiceRef = bundleContext
                                .getServiceReference(CryptoService.class);
                        CryptoService cryptoService = bundleContext.getService(cryptoServiceRef);
                        try {
                            Password[] pwdValues = new Password[values.length];
                            for (int i = 0; i < values.length; i++) {
                                if (values[i] != null) {
                                    if (adaptedProp.isEncrypted()) {
                                        pwdValues[i] = new Password(cryptoService.decodeBase64(values[i]));
                                    } else {
                                        pwdValues[i] = new Password(values[i]);
                                    }
                                }
                            }
                            propvalue = pwdValues;
                        } finally {
                            bundleContext.ungetService(cryptoServiceRef);
                        }
                        break;
                    }
                }
                properties.put(propName, propvalue);
            }
        }
        return properties;
    }
}

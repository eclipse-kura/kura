/*******************************************************************************
 * Copyright (c) 2023 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.internal.rest.cloudconnection.provider.util;

import static org.eclipse.kura.configuration.ConfigurationService.KURA_SERVICE_PID;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.configuration.Password;
import org.eclipse.kura.core.configuration.ComponentConfigurationImpl;
import org.eclipse.kura.internal.rest.cloudconnection.provider.ConfigParameterType;
import org.eclipse.kura.internal.rest.cloudconnection.provider.dto.ConfigComponentDTO;
import org.eclipse.kura.internal.rest.cloudconnection.provider.dto.ConfigParameterDTO;

public class ComponentUtils {

    public static final String PASSWORD_PLACEHOLDER = "Placeholder";

    private ComponentUtils() {
    }

    public static ComponentConfiguration getComponentConfiguration(ConfigurationService cs,
            ConfigComponentDTO componentConfig) throws KuraException {
        Map<String, Object> properties = new HashMap<>();
        ComponentConfiguration currentCC = cs.getComponentConfiguration(componentConfig.getComponentId());

        Map<String, Object> currentConfigProp = currentCC.getConfigurationProperties();
        for (ConfigParameterDTO configParam : componentConfig.getParameters()) {
            Object objValue;
            Object currentValue = currentConfigProp.get(configParam.getId());

            boolean isReadOnly = configParam.getMin() != null
                    && configParam.getMin().equals(configParam.getMax());
            if (isReadOnly) {
                objValue = currentValue;
            } else {
                objValue = getUserDefinedObject(configParam, currentValue);
            }
            properties.put(configParam.getId(), objValue);
        }

        // Force kura.service.pid into properties, if originally present
        if (currentConfigProp.get(KURA_SERVICE_PID) != null) {
            properties.put(KURA_SERVICE_PID, currentConfigProp.get(KURA_SERVICE_PID));
        }
        return new ComponentConfigurationImpl(currentCC.getPid(), null, properties);
    }

    private static Object getUserDefinedObject(ConfigParameterDTO param, Object currentObjValue) {
        Object objValue;

        final int cardinality = param.getCardinality();
        if (cardinality == 0 || cardinality == 1 || cardinality == -1) {
            String strValue = param.getValue();

            if (currentObjValue instanceof Password && PASSWORD_PLACEHOLDER.equals(strValue)) {
                objValue = currentObjValue;
            } else {
                objValue = getObjectValue(param);
            }
        } else {
            String[] strValues = param.getValues();

            if (currentObjValue instanceof Password[]) {
                Password[] currentPasswordValue = (Password[]) currentObjValue;
                for (int i = 0; i < strValues.length; i++) {
                    if (PASSWORD_PLACEHOLDER.equals(strValues[i])) {
                        strValues[i] = new String(currentPasswordValue[i].getPassword());
                    }
                }
            }

            objValue = getObjectValues(param, strValues);
        }
        return objValue;
    }

    private static Object getObjectValue(ConfigParameterDTO param) {
        Object objValue = null;
        ConfigParameterType type = param.getType();
        final String strValue = param.getValue();

        if (type == ConfigParameterType.STRING) {
            objValue = strValue;
        } else if (strValue != null && !strValue.trim().isEmpty()) {
            final String trimmedValue = strValue.trim();
            switch (type) {
            case LONG:
                objValue = Long.parseLong(trimmedValue);
                break;
            case DOUBLE:
                objValue = Double.parseDouble(trimmedValue);
                break;
            case FLOAT:
                objValue = Float.parseFloat(trimmedValue);
                break;
            case INTEGER:
                objValue = Integer.parseInt(trimmedValue);
                break;
            case SHORT:
                objValue = Short.parseShort(trimmedValue);
                break;
            case BYTE:
                objValue = Byte.parseByte(trimmedValue);
                break;
            case BOOLEAN:
                objValue = Boolean.parseBoolean(trimmedValue);
                break;
            case PASSWORD:
                objValue = new Password(trimmedValue);
                break;
            case CHAR:
                objValue = Character.valueOf(trimmedValue.charAt(0));
                break;
            default:
                break;
            }
        }
        return objValue;
    }

    private static Object[] getObjectValues(ConfigParameterDTO param, String[] defaultValues) {
        final List<Object> values = new ArrayList<>();
        final ConfigParameterType type = param.getType();

        List<String> trimmedValues = Stream.of(defaultValues).map(String::trim).collect(Collectors.toList());

        switch (type) {
        case BOOLEAN:
            for (String value : trimmedValues) {
                if (!value.isEmpty()) {
                    values.add(Boolean.valueOf(value));
                }
            }
            return values.toArray(new Boolean[] {});

        case BYTE:
            for (String value : trimmedValues) {
                if (!value.isEmpty()) {
                    values.add(Byte.valueOf(value));
                }
            }
            return values.toArray(new Byte[] {});

        case CHAR:
            for (String value : trimmedValues) {
                if (!value.isEmpty()) {
                    values.add(value.charAt(0));
                }
            }
            return values.toArray(new Character[] {});

        case DOUBLE:
            for (String value : trimmedValues) {
                if (!value.isEmpty()) {
                    values.add(Double.valueOf(value));
                }
            }
            return values.toArray(new Double[] {});

        case FLOAT:
            for (String value : trimmedValues) {
                if (!value.isEmpty()) {
                    values.add(Float.valueOf(value));
                }
            }
            return values.toArray(new Float[] {});

        case INTEGER:
            for (String value : trimmedValues) {
                if (!value.isEmpty()) {
                    values.add(Integer.valueOf(value));
                }
            }
            return values.toArray(new Integer[] {});

        case LONG:
            for (String value : trimmedValues) {
                if (!value.isEmpty()) {
                    values.add(Long.valueOf(value));
                }
            }
            return values.toArray(new Long[] {});

        case SHORT:
            for (String value : trimmedValues) {
                if (!value.isEmpty()) {
                    values.add(Short.valueOf(value));
                }
            }
            return values.toArray(new Short[] {});

        case PASSWORD:
            for (String value : trimmedValues) {
                if (!value.isEmpty()) {
                    values.add(new Password(value));
                }
            }
            return values.toArray(new Password[] {});

        case STRING:
            for (String value : trimmedValues) {
                if (!value.isEmpty()) {
                    values.add(value);
                }
            }
            return values.toArray(new String[] {});
        default:
            return null;
        }
    }
}

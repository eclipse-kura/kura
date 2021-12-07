/*******************************************************************************
 * Copyright (c) 2021 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.rest.configuration.api;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import javax.ws.rs.core.Response.Status;

import org.eclipse.kura.configuration.Password;
import org.eclipse.kura.configuration.metatype.Scalar;
import org.eclipse.kura.internal.rest.configuration.FailureHandler;

public class PropertyDTO implements Validable {

    private final Object value;
    private final Scalar type;

    public PropertyDTO(final Object value, final Scalar type) {
        this.value = value;
        this.type = type;
    }

    public Scalar getType() {
        return this.type;
    }

    public Object getValue() {
        return this.value;
    }

    @Override
    public void validate() {
        FailureHandler.requireParameter(this.type, "type");

        if (value instanceof List<?>) {
            validateArrayProperty(type, (List<?>) value);
        } else {
            validateSingletonProperty(type, value);
        }
    }

    private static void validateArrayProperty(final Scalar type, final List<?> values) {
        for (final Object singletonValue : values) {
            validateSingletonProperty(type, singletonValue);
        }
    }

    private static void validateSingletonProperty(final Scalar type, final Object value) {
        if (value == null) {
            return;
        }

        final boolean isValid;

        switch (type) {
        case BYTE:
        case FLOAT:
        case LONG:
        case INTEGER:
        case SHORT:
        case DOUBLE:
            isValid = value instanceof Number;
            break;
        case PASSWORD:
        case STRING:
            isValid = value instanceof String;
            break;
        case CHAR:
            isValid = value instanceof String && ((String) value).length() == 1;
            break;
        case BOOLEAN:
            isValid = value instanceof Boolean;
            break;
        default:
            isValid = false;
        }

        if (!isValid) {
            throw FailureHandler.toWebApplicationException(Status.BAD_REQUEST,
                    "Invalid property for type " + type + ": " + value);
        }
    }

    public static Optional<PropertyDTO> fromConfigurationProperty(final Object property) {

        return Optional.ofNullable(property).flatMap(p -> scalarFromClass(p.getClass()))
                .map(type -> new PropertyDTO(configurationPropertyToDTOProperty(property), type));
    }

    public Optional<Object> toConfigurationProperty() {
        if (value == null) {
            return Optional.empty();
        }

        final Optional<Object> asSingleton = singletonToProperty(value, type);

        if (asSingleton.isPresent()) {
            return asSingleton;
        }

        return arrayToProperty(value, type);
    }

    @SuppressWarnings("unchecked")
    private static <T> T assertType(final Object value, final Class<T> clazz) {
        if (value.getClass() == clazz) {
            return (T) value;
        } else {
            throw new IllegalArgumentException();
        }
    }

    private static <T> Function<Object, T> nullOrElse(final Function<Object, T> func) {
        return v -> {
            if (v == null) {
                return null;
            } else {
                return func.apply(v);
            }
        };
    }

    private static Optional<Object> singletonToProperty(final Object value, final Scalar type) {
        final Object result;

        try {
            switch (type) {
            case BOOLEAN:
                result = assertType(value, Boolean.class);
                break;
            case BYTE:
                result = ((Number) value).byteValue();
                break;
            case CHAR:
                result = ((String) value).charAt(0);
                break;
            case DOUBLE:
                result = ((Number) value).doubleValue();
                break;
            case FLOAT:
                result = ((Number) value).floatValue();
                break;
            case INTEGER:
                result = ((Number) value).intValue();
                break;
            case LONG:
                result = ((Number) value).longValue();
                break;
            case PASSWORD:
                result = new Password(assertType(value, String.class));
                break;
            case SHORT:
                result = ((Number) value).shortValue();
                break;
            case STRING:
                result = assertType(value, String.class);
                break;
            default:
                return Optional.empty();
            }

            return Optional.of(result);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private static Optional<Object> arrayToProperty(final Object propertyValue, final Scalar type) {
        final Object result;

        try {
            switch (type) {
            case BOOLEAN:
                result = ((List<?>) propertyValue).stream().map(nullOrElse(v -> assertType(v, Boolean.class)))
                        .toArray(Boolean[]::new);
                break;
            case BYTE:
                result = ((List<?>) propertyValue).stream().map(nullOrElse(v -> ((Number) v).byteValue()))
                        .toArray(Byte[]::new);
                break;
            case CHAR:
                result = ((List<?>) propertyValue).stream().map(nullOrElse(v -> ((String) v).charAt(0)))
                        .toArray(Character[]::new);
                break;
            case DOUBLE:
                result = ((List<?>) propertyValue).stream().map(nullOrElse(v -> ((Number) v).doubleValue()))
                        .toArray(Double[]::new);
                break;
            case FLOAT:
                result = ((List<?>) propertyValue).stream().map(nullOrElse(v -> ((Number) v).floatValue()))
                        .toArray(Float[]::new);
                break;
            case INTEGER:
                result = ((List<?>) propertyValue).stream().map(nullOrElse(v -> ((Number) v).intValue()))
                        .toArray(Integer[]::new);
                break;
            case LONG:
                result = ((List<?>) propertyValue).stream().map(nullOrElse(v -> ((Number) v).longValue()))
                        .toArray(Long[]::new);
                break;
            case PASSWORD:
                result = ((List<?>) propertyValue).stream()
                        .map(nullOrElse(v -> new Password(assertType(v, String.class)))).toArray(Password[]::new);
                break;
            case SHORT:
                result = ((List<?>) propertyValue).stream().map(nullOrElse(v -> ((Number) v).shortValue()))
                        .toArray(Short[]::new);
                break;
            case STRING:
                result = ((List<?>) propertyValue).stream().map(nullOrElse(v -> assertType(v, String.class)))
                        .toArray(String[]::new);
                break;
            default:
                return Optional.empty();
            }

            return Optional.of(result);
        } catch (ClassCastException e) {
            return Optional.empty();
        }
    }

    public static Optional<Scalar> scalarFormSingletonClass(final Class<?> clazz) {
        final Scalar result;

        if (clazz == Boolean.class) {
            result = Scalar.BOOLEAN;
        } else if (clazz == Byte.class) {
            result = Scalar.BYTE;
        } else if (clazz == Character.class) {
            result = Scalar.CHAR;
        } else if (clazz == Double.class) {
            result = Scalar.DOUBLE;
        } else if (clazz == Float.class) {
            result = Scalar.FLOAT;
        } else if (clazz == Integer.class) {
            result = Scalar.INTEGER;
        } else if (clazz == Long.class) {
            result = Scalar.LONG;
        } else if (clazz == Password.class) {
            result = Scalar.PASSWORD;
        } else if (clazz == Short.class) {
            result = Scalar.SHORT;
        } else if (clazz == String.class) {
            result = Scalar.STRING;
        } else {
            return Optional.empty();
        }

        return Optional.of(result);
    }

    public static Optional<Scalar> scalarFromClass(final Class<?> clazz) {
        if (clazz.isArray()) {
            return scalarFormSingletonClass(clazz.getComponentType());
        } else {
            return scalarFormSingletonClass(clazz);
        }
    }

    private static Object configurationPropertyToDTOProperty(final Object property) {
        if (property instanceof Password) {
            return new String(((Password) property).getPassword());
        } else if (property instanceof Password[]) {
            return Arrays.stream((Password[]) property).map(p -> p == null ? null : new String(p.getPassword()))
                    .toArray(String[]::new);
        } else {
            return property;
        }
    }
}

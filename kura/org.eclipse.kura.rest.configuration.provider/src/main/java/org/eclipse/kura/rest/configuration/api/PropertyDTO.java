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
                result = ((Double) value).byteValue();
                break;
            case CHAR:
                result = ((String) value).charAt(0);
                break;
            case DOUBLE:
                result = assertType(value, Double.class);
                break;
            case FLOAT:
                result = ((Double) value).floatValue();
                break;
            case INTEGER:
                result = ((Double) value).intValue();
                break;
            case LONG:
                result = ((Double) value).longValue();
                break;
            case PASSWORD:
                result = new Password(assertType(value, String.class));
                break;
            case SHORT:
                result = ((Double) value).shortValue();
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
                result = ((List<?>) propertyValue).stream().map(nullOrElse(v -> ((Double) v).byteValue()))
                        .toArray(Byte[]::new);
                break;
            case CHAR:
                result = ((List<?>) propertyValue).stream().map(nullOrElse(v -> ((String) v).charAt(0)))
                        .toArray(Character[]::new);
                break;
            case DOUBLE:
                result = ((List<?>) propertyValue).stream().map(nullOrElse(v -> assertType(v, Double.class)))
                        .toArray(Double[]::new);
                break;
            case FLOAT:
                result = ((List<?>) propertyValue).stream().map(nullOrElse(v -> ((Double) v).floatValue()))
                        .toArray(Float[]::new);
                break;
            case INTEGER:
                result = ((List<?>) propertyValue).stream().map(nullOrElse(v -> ((Double) v).intValue()))
                        .toArray(Integer[]::new);
                break;
            case LONG:
                result = ((List<?>) propertyValue).stream().map(nullOrElse(v -> ((Double) v).longValue()))
                        .toArray(Long[]::new);
                break;
            case PASSWORD:
                result = ((List<?>) propertyValue).stream()
                        .map(nullOrElse(v -> new Password(assertType(v, String.class)))).toArray(Password[]::new);
                break;
            case SHORT:
                result = ((List<?>) propertyValue).stream().map(nullOrElse(v -> ((Double) v).shortValue()))
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

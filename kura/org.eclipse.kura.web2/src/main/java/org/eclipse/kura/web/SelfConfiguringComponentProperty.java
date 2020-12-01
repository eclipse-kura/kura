/*******************************************************************************
 * Copyright (c) 2020 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Eurotech
 *******************************************************************************/
package org.eclipse.kura.web;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.Password;
import org.eclipse.kura.configuration.metatype.AD;
import org.eclipse.kura.configuration.metatype.Scalar;
import org.eclipse.kura.core.configuration.metatype.Tad;
import org.eclipse.kura.crypto.CryptoService;

public class SelfConfiguringComponentProperty<T> {

    private static final Pattern COMMA = Pattern.compile(",");

    private final Tad ad;
    private final Class<?> valueType;
    private final Optional<CryptoService> cryptoService;
    private Optional<T> value;

    public SelfConfiguringComponentProperty(final Tad ad, final Class<T> classz, final CryptoService cryptoService) {
        this(ad, classz, Optional.of(cryptoService));
    }

    public SelfConfiguringComponentProperty(final Tad ad, final Class<T> classz) {
        this(ad, classz, Optional.empty());
    }

    private SelfConfiguringComponentProperty(final Tad ad, final Class<T> classz,
            final Optional<CryptoService> cryptoService) {
        this.ad = ad;
        this.cryptoService = cryptoService;
        this.valueType = classz;
        check(ad.getType(), ad.getCardinality(), classz);
        this.value = extractDefault(ad);
    }

    public Tad getAd() {
        return ad;
    }

    public void fillValue(final Map<String, Object> properties) {
        if (value.isPresent() && !properties.containsKey(ad.getId())) {
            if (ad.getType() == Scalar.PASSWORD) {
                properties.put(this.ad.getId(), new Password(value.get().toString().toCharArray()));
            } else {
                properties.put(this.ad.getId(), value.get());
            }
        }
    }

    @SuppressWarnings("unchecked")
    public void update(final Map<String, Object> properties) {
        final Object providedValue = properties.get(this.ad.getId());

        if (this.valueType.isInstance(providedValue)) {
            this.value = Optional.of((T) providedValue);
        } else if (this.ad.getType() == Scalar.PASSWORD && providedValue instanceof Password) {
            final Password providedPassword = (Password) providedValue;

            this.value = Optional.of((T) new String((providedPassword.getPassword())));
        }
    }

    public T get() {
        return value.orElseThrow(() -> new IllegalStateException("property value has not been set"));
    }

    public Optional<T> getOptional() {
        return value;
    }

    private static void check(final Scalar scalar, final int cardinality, final Class<?> clazz) {

        final Class<?> expected;

        if (scalar == Scalar.BOOLEAN) {
            expected = Boolean.class;
        } else if (scalar == Scalar.BYTE) {
            expected = Byte.class;
        } else if (scalar == Scalar.CHAR) {
            expected = Character.class;
        } else if (scalar == Scalar.DOUBLE) {
            expected = Double.class;
        } else if (scalar == Scalar.FLOAT) {
            expected = Float.class;
        } else if (scalar == Scalar.INTEGER) {
            expected = Integer.class;
        } else if (scalar == Scalar.LONG) {
            expected = Long.class;
        } else if (scalar == Scalar.PASSWORD) {
            expected = String.class;
        } else if (scalar == Scalar.SHORT) {
            expected = Short.class;
        } else if (scalar == Scalar.STRING) {
            expected = String.class;
        } else {
            throw new IllegalArgumentException(scalar == null ? null : scalar.toString());
        }

        if (cardinality != 0) {
            if (!clazz.isArray()) {
                throw new IllegalArgumentException("class must be an array");
            }

            if (clazz.getComponentType() != expected) {
                throw new IllegalArgumentException("AD type mismatch");
            }
        } else if (clazz != expected) {
            throw new IllegalArgumentException("AD type mismatch");
        }

    }

    private Object extractScalar(final Scalar scalar, final String value) {
        if (scalar == Scalar.BOOLEAN) {
            return (Boolean) Boolean.parseBoolean(value);
        } else if (scalar == Scalar.BYTE) {
            return (Boolean) Boolean.parseBoolean(value);
        } else if (scalar == Scalar.CHAR) {
            return (Character) value.charAt(0);
        } else if (scalar == Scalar.DOUBLE) {
            return (Double) Double.parseDouble(value);
        } else if (scalar == Scalar.FLOAT) {
            return (Float) Float.parseFloat(value);
        } else if (scalar == Scalar.INTEGER) {
            return (Integer) Integer.parseInt(value);
        } else if (scalar == Scalar.LONG) {
            return (Long) Long.parseLong(value);
        } else if (scalar == Scalar.PASSWORD) {
            try {
                return new String(unwrapCryptoService().encryptAes(value.toCharArray()));
            } catch (KuraException e) {
                throw new IllegalStateException("failed to encrypt password", e);
            }
        } else if (scalar == Scalar.SHORT) {
            return (Short) Short.parseShort(value);
        } else if (scalar == Scalar.STRING) {
            return (String) value;
        } else {
            throw new IllegalArgumentException(scalar == null ? null : scalar.toString());
        }
    }

    private CryptoService unwrapCryptoService() {
        if (!cryptoService.isPresent()) {
            throw new IllegalArgumentException("CryptoService is required for defining a password property");
        }

        return cryptoService.get();
    }

    @SuppressWarnings("unchecked")
    private Optional<T> extractDefault(final AD ad) {

        final String defaultValue = ad.getDefault();

        if (defaultValue == null) {
            return Optional.empty();
        }

        final Scalar scalar = ad.getType();
        final int cardinality = ad.getCardinality();

        if (cardinality == 0) {
            return Optional.of((T) extractScalar(scalar, defaultValue));
        } else {
            final List<?> result = COMMA.splitAsStream(defaultValue).map(String::trim).filter(String::isEmpty)
                    .map(s -> extractScalar(scalar, s)).collect(Collectors.toList());

            return Optional.of((T) result.toArray(null));
        }

    }
}

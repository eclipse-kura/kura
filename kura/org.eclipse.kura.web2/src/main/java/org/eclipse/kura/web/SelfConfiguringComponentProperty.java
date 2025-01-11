/*******************************************************************************
 * Copyright (c) 2020, 2024 Eurotech and/or its affiliates and others
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
        return this.ad;
    }

    public void fillValue(final Map<String, Object> properties) {
        if (this.value.isPresent() && !properties.containsKey(this.ad.getId())) {
            if (this.ad.getType() == Scalar.PASSWORD) {
                properties.put(this.ad.getId(), new Password(this.value.get().toString().toCharArray()));
            } else {
                properties.put(this.ad.getId(), this.value.get());
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

            this.value = Optional.of((T) new String(providedPassword.getPassword()));
        }
    }

    public T get() {
        return this.value.orElseThrow(() -> new IllegalStateException("property value has not been set"));
    }

    public Optional<T> getOptional() {
        return this.value;
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
            return Boolean.parseBoolean(value);
        } else if (scalar == Scalar.BYTE) {
            return Byte.parseByte(value);
        } else if (scalar == Scalar.CHAR) {
            return value.charAt(0);
        } else if (scalar == Scalar.DOUBLE) {
            return Double.parseDouble(value);
        } else if (scalar == Scalar.FLOAT) {
            return Float.parseFloat(value);
        } else if (scalar == Scalar.INTEGER) {
            return Integer.parseInt(value);
        } else if (scalar == Scalar.LONG) {
            return Long.parseLong(value);
        } else if (scalar == Scalar.PASSWORD) {
            try {
                return new String(unwrapCryptoService().encryptAes(value.toCharArray()));
            } catch (KuraException e) {
                throw new IllegalStateException("failed to encrypt password", e);
            }
        } else if (scalar == Scalar.SHORT) {
            return Short.parseShort(value);
        } else if (scalar == Scalar.STRING) {
            return value;
        } else {
            throw new IllegalArgumentException(scalar == null ? null : scalar.toString());
        }
    }

    private Object createScalarArray(final Scalar scalar) {
        if (scalar == Scalar.BOOLEAN) {
            return new Boolean[0];
        } else if (scalar == Scalar.BYTE) {
            return new Byte[0];
        } else if (scalar == Scalar.CHAR) {
            return new Character[0];
        } else if (scalar == Scalar.DOUBLE) {
            return new Double[0];
        } else if (scalar == Scalar.FLOAT) {
            return new Float[0];
        } else if (scalar == Scalar.INTEGER) {
            return new Integer[0];
        } else if (scalar == Scalar.LONG) {
            return new Long[0];
        } else if (scalar == Scalar.SHORT) {
            return new Short[0];
        } else if (scalar == Scalar.STRING || scalar == Scalar.PASSWORD) {
            return new String[0];
        } else {
            throw new IllegalArgumentException(scalar == null ? null : scalar.toString());
        }
    }

    private CryptoService unwrapCryptoService() {
        if (!this.cryptoService.isPresent()) {
            throw new IllegalArgumentException("CryptoService is required for defining a password property");
        }

        return this.cryptoService.get();
    }

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
            final List<?> result = COMMA.splitAsStream(defaultValue).map(String::trim).filter(s -> !s.isEmpty())
                    .map(s -> extractScalar(scalar, s)).collect(Collectors.toList());

            return Optional.of((T) result.toArray((T[]) createScalarArray(scalar)));
        }

    }
}

/*******************************************************************************
 * Copyright (c) 2020 Eurotech and/or its affiliates and others
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

package org.eclipse.kura.web.client.util;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.shared.model.GwtConfigComponent;
import org.eclipse.kura.web.shared.model.GwtConfigParameter;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.TakesValue;

public final class ValidationUtil {

    private static final String CONFIG_MAX_VALUE = "configMaxValue";
    private static final String CONFIG_MIN_VALUE = "configMinValue";
    private static final String INVALID_VALUE = "invalidValue";
    private static final String INVALID_BOOLEAN_VALUE = "invalidBooleanValue";

    protected static final Messages MSGS = GWT.create(Messages.class);
    protected static final Logger errorLogger = Logger.getLogger("ErrorLogger");

    private ValidationUtil() {
    }

    public static void validateParameters(GwtConfigComponent componentConfig, ValidationErrorConsumer consumer) {
        for (final GwtConfigParameter param : componentConfig.getParameters()) {
            validateParameter(param, param.getValue(), consumer);
        }
    }

    // Validates all the entered values
    public static void validateParameter(GwtConfigParameter param, String value, ValidationErrorConsumer consumer) {

        if (value == null) {
            if (!param.isRequired()) {
                return;
            } else {
                consumer.addError(MSGS.formRequiredParameter());
                return;
            }
        }

        String trimmedValue = value.trim();

        if (param.isRequired() && trimmedValue.isEmpty()) {
            consumer.addError(MSGS.formRequiredParameter());
            return;
        }

        try {
            switch (param.getType()) {
            case BOOLEAN:
                validateBoolean(trimmedValue, param, consumer);
                break;
            case CHAR:
                validateChar(trimmedValue, param, consumer);
                break;
            case STRING:
                validateString(trimmedValue, param, consumer);
                break;
            case FLOAT:
                validateFloat(trimmedValue, param, consumer);
                break;
            case INTEGER:
                validateInteger(trimmedValue, param, consumer);
                break;
            case SHORT:
                validateShort(trimmedValue, param, consumer);
                break;
            case BYTE:
                validateByte(trimmedValue, param, consumer);
                break;
            case LONG:
                validateLong(trimmedValue, param, consumer);
                break;
            case DOUBLE:
                validateDouble(trimmedValue, param, consumer);
                break;
            case PASSWORD:
                break;
            default:
                consumer.addError("Unsupported data type: " + param.getType().toString());
                break;
            }
        } catch (NumberFormatException e) {
            consumer.addError(MessageUtils.get(INVALID_VALUE, trimmedValue));
        }

    }

    public static boolean validateParameters(GwtConfigComponent component) {
        final TakesValue<Boolean> isValid = new TakesValue<Boolean>() {

            private boolean value = true;

            @Override
            public void setValue(Boolean value) {
                this.value = value;
            }

            @Override
            public Boolean getValue() {
                return this.value;
            }
        };

        validateParameters(component, errorDescription -> {
            errorLogger.info("parameter is not valid " + errorDescription);
            isValid.setValue(false);
        });

        return isValid.getValue();
    }

    public static boolean validateParameter(GwtConfigParameter param, String value) {
        final TakesValue<Boolean> isValid = new TakesValue<Boolean>() {

            private boolean value = true;

            @Override
            public void setValue(Boolean value) {
                this.value = value;
            }

            @Override
            public Boolean getValue() {
                return this.value;
            }
        };

        validateParameter(param, value, errorDescription -> {
            errorLogger.info("parameter is not valid " + errorDescription);
            isValid.setValue(false);
        });

        return isValid.getValue();
    }

    private static void validateBoolean(final String value, final GwtConfigParameter param,
            final ValidationErrorConsumer consumer) {
        if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) {
            consumer.addError(MessageUtils.get(INVALID_BOOLEAN_VALUE, value));
        }
    }

    private static void validateChar(final String value, final GwtConfigParameter param,
            final ValidationErrorConsumer consumer) {
        if (value.length() > 1) {
            consumer.addError(MessageUtils.get(Integer.toString(value.length()), value));
        }
        if (param.getMin() != null && param.getMin().charAt(0) > value.charAt(0)) {
            consumer.addError(MessageUtils.get(CONFIG_MIN_VALUE, param.getMin().charAt(0)));
        }
        if (param.getMax() != null && param.getMax().charAt(0) < value.charAt(0)) {
            consumer.addError(MessageUtils.get(CONFIG_MAX_VALUE, param.getMax().charAt(0)));
        }
    }

    private static void validateString(final String value, final GwtConfigParameter param,
            final ValidationErrorConsumer consumer) {
        int configMinValue = 0;
        int configMaxValue = Integer.MAX_VALUE;
        try {
            configMinValue = Integer.parseInt(param.getMin());
        } catch (NumberFormatException nfe) {
            errorLogger.log(Level.FINE, "Configuration min value error! Applying UI defaults...");
        }
        try {
            configMaxValue = Integer.parseInt(param.getMax());
        } catch (NumberFormatException nfe) {
            errorLogger.log(Level.FINE, "Configuration max value error! Applying UI defaults...");
        }

        if (String.valueOf(value).length() < configMinValue) {
            consumer.addError(MessageUtils.get(CONFIG_MIN_VALUE, configMinValue));
        }
        if (String.valueOf(value).length() > configMaxValue) {
            consumer.addError(MessageUtils.get(CONFIG_MAX_VALUE, configMaxValue));
        }
    }

    private static void validateLong(final String value, final GwtConfigParameter param,
            final ValidationErrorConsumer consumer) {
        final long asLong = Long.parseLong(value);
        if (param.getMin() != null && Long.parseLong(param.getMin()) > asLong) {
            consumer.addError(MessageUtils.get(CONFIG_MIN_VALUE, param.getMin()));
        }
        if (param.getMax() != null && Long.parseLong(param.getMax()) < asLong) {
            consumer.addError(MessageUtils.get(CONFIG_MAX_VALUE, param.getMax()));
        }
    }

    private static void validateDouble(final String value, final GwtConfigParameter param,
            final ValidationErrorConsumer consumer) {
        final double asDouble = Double.parseDouble(value);
        if (param.getMin() != null && Double.parseDouble(param.getMin()) > asDouble) {
            consumer.addError(MessageUtils.get(CONFIG_MIN_VALUE, param.getMin()));
        }
        if (param.getMax() != null && Double.parseDouble(param.getMax()) < asDouble) {
            consumer.addError(MessageUtils.get(CONFIG_MAX_VALUE, param.getMax()));
        }
    }

    private static void validateByte(final String value, final GwtConfigParameter param,
            final ValidationErrorConsumer consumer) {
        final byte asByte = Byte.parseByte(value);
        if (param.getMin() != null && Byte.parseByte(param.getMin()) > asByte) {
            consumer.addError(MessageUtils.get(CONFIG_MIN_VALUE, param.getMin()));
        }
        if (param.getMax() != null && Byte.parseByte(param.getMax()) < asByte) {
            consumer.addError(MessageUtils.get(CONFIG_MAX_VALUE, param.getMax()));
        }
    }

    private static void validateShort(final String value, final GwtConfigParameter param,
            final ValidationErrorConsumer consumer) {
        final short asShort = Short.parseShort(value);
        if (param.getMin() != null && Short.parseShort(param.getMin()) > asShort) {
            consumer.addError(MessageUtils.get(CONFIG_MIN_VALUE, param.getMin()));
        }
        if (param.getMax() != null && Short.parseShort(param.getMax()) < asShort) {
            consumer.addError(MessageUtils.get(CONFIG_MAX_VALUE, param.getMax()));
        }
    }

    private static void validateInteger(final String value, final GwtConfigParameter param,
            final ValidationErrorConsumer consumer) {
        final int asInt = Integer.parseInt(value);
        if (param.getMin() != null && Integer.parseInt(param.getMin()) > asInt) {
            consumer.addError(MessageUtils.get(CONFIG_MIN_VALUE, param.getMin()));
        }
        if (param.getMax() != null && Integer.parseInt(param.getMax()) < asInt) {
            consumer.addError(MessageUtils.get(CONFIG_MAX_VALUE, param.getMax()));
        }
    }

    private static void validateFloat(final String value, final GwtConfigParameter param,
            final ValidationErrorConsumer consumer) {
        final float asFloat = Float.parseFloat(value);
        if (param.getMin() != null && Float.parseFloat(param.getMin()) > asFloat) {
            consumer.addError(MessageUtils.get(CONFIG_MIN_VALUE, param.getMin()));
        }
        if (param.getMax() != null && Float.parseFloat(param.getMax()) < asFloat) {
            consumer.addError(MessageUtils.get(CONFIG_MAX_VALUE, param.getMax()));
        }
    }

    public interface ValidationErrorConsumer {

        public void addError(String errorDescription);
    }
}

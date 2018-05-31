/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.web.server.util;

import static java.lang.String.format;
import static org.eclipse.kura.configuration.ConfigurationService.KURA_SERVICE_PID;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import javax.servlet.ServletException;

import org.eclipse.kura.configuration.metatype.Option;
import org.eclipse.kura.configuration.metatype.Scalar;
import org.eclipse.kura.core.configuration.metatype.Tad;
import org.eclipse.kura.driver.Driver;
import org.eclipse.kura.internal.wire.asset.WireAssetChannelDescriptor;
import org.eclipse.kura.web.server.util.ServiceLocator.ServiceConsumer;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AssetConfigValidator {

    private static Logger logger = LoggerFactory.getLogger(AssetConfigValidator.class);

    private static final AssetConfigValidator _instance = new AssetConfigValidator();
    private static int lineNumber = 0;

    public static AssetConfigValidator get() {
        return _instance;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> validateCsv(String CSV, String assetPid, String driverPid, List<String> errors)
            throws ServletException {

        try {
            errors.clear();
            List<Tad> fullChannelMetatype = new ArrayList<>();
            List<String> propertyNames = new ArrayList<>();

            ((List<Tad>) WireAssetChannelDescriptor.get().getDescriptor()).forEach(element -> {
                fullChannelMetatype.add(element);
                propertyNames.add("+" + element.getName());
            });

            withDriver(driverPid, descriptor -> {
                ((List<Tad>) descriptor.getChannelDescriptor().getDescriptor()).forEach(element -> {
                    fullChannelMetatype.add(element);
                    propertyNames.add(element.getName());
                });
            });

            Map<String, Object> updatedAssetProps = new HashMap<>();
            try (Scanner sc = new Scanner(CSV)) {
                sc.nextLine();
                lineNumber = 1;
                if (!sc.hasNext()) {
                    errors.add("Empty CSV file.");
                    throw new Exception();
                }
                sc.forEachRemaining(line -> {
                    lineNumber++;
                    List<Object> channelValues = new ArrayList<>();
                    String channelName = "";
                    StringBuilder sb = new StringBuilder();
                    String[] tokens = line.split(",");
                    boolean errorInChannel = false;
                    for (int i = 0; i < tokens.length; i++) {
                        tokens[i] = tokens[i].substring(1, tokens[i].length() - 1);
                        if (tokens.length < fullChannelMetatype.size()) {
                            errors.add("Incorrect number of fields in CSV line " + String.valueOf(lineNumber));
                            errorInChannel = true;
                            break;
                        }
                        try {
                            channelValues.add(validate(fullChannelMetatype.get(i), tokens[i], errors, lineNumber));
                            sb.append(fullChannelMetatype.get(i).getName()).append("=").append(tokens[i]).append(" - ");
                            if (fullChannelMetatype.get(i).getName().equals("name")) {
                                channelName = tokens[i];
                            }
                        } catch (Exception ex) {
                            errorInChannel = true;
                            break;
                        }
                    }
                    if (!errorInChannel) {
                        for (int i = 0; i < propertyNames.size(); i++) {
                            updatedAssetProps.put(channelName + "#" + propertyNames.get(i), channelValues.get(i));
                        }
                    }
                });
            }
            if (errors.size() > 0) {
                throw new Exception();
            }

            return updatedAssetProps;

        } catch (Exception ex) {
            throw new ServletException("Validation exception on CSV file.");
        }

    }

    private void withDriver(final String kuraServicePid, final ServiceConsumer<Driver> consumer) throws Exception {
        final BundleContext ctx = FrameworkUtil.getBundle(ServiceLocator.class).getBundleContext();

        final String filter = format("(%s=%s)", KURA_SERVICE_PID, kuraServicePid);
        final Collection<ServiceReference<Driver>> refs = ctx.getServiceReferences(Driver.class, filter);

        if (refs == null || refs.isEmpty()) {
            return;
        }

        final ServiceReference<Driver> driverRef = refs.iterator().next();

        try {
            consumer.consume(ctx.getService(driverRef));
        } finally {
            ctx.ungetService(driverRef);
        }
    }

    private String errorString(int lineNumber, Tad field, String value) {
        return new StringBuilder().append("\"").append(value).append("\" is not a valid value for field ")
                .append(field.getName()).append(" on line ").append(String.valueOf(lineNumber)).append(".").toString();
    }

    // Validates all the entered values
    // TODO: validation should be done like in the old web ui: cleaner approach
    protected Object validate(Tad field, String value, List<String> errors, int lineNumber) throws Exception {

        String trimmedValue = null;
        final boolean isEmpty = value == null || (trimmedValue = value.trim()).isEmpty();

        if (field.isRequired() && isEmpty) {
            throw new Exception();
        }

        if (!isEmpty) {
            // Validate "Options" field first. Data type will be taken care of next.
            if (field.getOption().size() > 0) {
                boolean foundEqual = false;
                for (Option o : field.getOption()) {
                    foundEqual |= o.getValue().toString().trim().equals(value.trim());
                }
                if (!foundEqual) {
                    final StringBuilder sb = new StringBuilder();
                    sb.append("Value \"").append(value).append("\" on line ").append(String.valueOf(lineNumber))
                            .append(" should be one of [");
                    field.getOption().forEach(option -> {
                        sb.append("\"").append(option.getValue()).append("\", ");
                    });
                    sb.delete(sb.length() - 2, sb.length());
                    sb.append("]");
                    errors.add(sb.toString());
                    throw new Exception();
                }
            }
            // Validate Data type and constraints.
            if (field.getType().equals(Scalar.CHAR)) {
                if (value.length() > 1) {
                    errors.add(errorString(lineNumber, field, value));
                    throw new Exception();
                }
                if (field.getMin() != null && field.getMin().charAt(0) > trimmedValue.charAt(0)) {
                    errors.add(errorString(lineNumber, field, value));
                    throw new Exception();
                }
                if (field.getMax() != null && field.getMax().charAt(0) < trimmedValue.charAt(0)) {
                    errors.add(errorString(lineNumber, field, value));
                    throw new Exception();
                }
                return trimmedValue.charAt(0);
            } else if (field.getType().equals(Scalar.STRING)) {
                int configMinValue = 0;
                int configMaxValue = Integer.MAX_VALUE;
                try {
                    configMinValue = Integer.parseInt(field.getMin());
                } catch (NumberFormatException nfe) {
                    logger.debug("Configuration min value error! Applying UI defaults...");
                }
                try {
                    configMaxValue = Integer.parseInt(field.getMax());
                } catch (NumberFormatException nfe) {
                    logger.debug("Configuration max value error! Applying UI defaults...");
                }

                if (String.valueOf(trimmedValue).length() < configMinValue) {
                    errors.add(errorString(lineNumber, field, value));
                    throw new Exception();
                }
                if (String.valueOf(trimmedValue).length() > configMaxValue) {
                    errors.add(errorString(lineNumber, field, value));
                    throw new Exception();
                }
                return trimmedValue;
            } else {
                try {
                    // numeric value
                    if (field.getType().equals(Scalar.FLOAT)) {
                        Float uiValue = Float.parseFloat(trimmedValue);
                        if (field.getMin() != null && Float.parseFloat(field.getMin()) > uiValue) {
                            errors.add(errorString(lineNumber, field, value));
                            throw new Exception();
                        }
                        if (field.getMax() != null && Float.parseFloat(field.getMax()) < uiValue) {
                            errors.add(errorString(lineNumber, field, value));
                            throw new Exception();
                        }
                        return uiValue;
                    } else if (field.getType().equals(Scalar.INTEGER)) {
                        Integer uiValue = Integer.parseInt(trimmedValue);
                        if (field.getMin() != null && Integer.parseInt(field.getMin()) > uiValue) {
                            errors.add(errorString(lineNumber, field, value));
                            throw new Exception();
                        }
                        if (field.getMax() != null && Integer.parseInt(field.getMax()) < uiValue) {
                            errors.add(errorString(lineNumber, field, value));
                            throw new Exception();
                        }
                        return uiValue;
                    } else if (field.getType().equals(Scalar.SHORT)) {
                        Short uiValue = Short.parseShort(trimmedValue);
                        if (field.getMin() != null && Short.parseShort(field.getMin()) > uiValue) {
                            errors.add(errorString(lineNumber, field, value));
                            throw new Exception();
                        }
                        if (field.getMax() != null && Short.parseShort(field.getMax()) < uiValue) {
                            errors.add(errorString(lineNumber, field, value));
                            throw new Exception();
                        }
                        return uiValue;
                    } else if (field.getType().equals(Scalar.BYTE)) {
                        Byte uiValue = Byte.parseByte(trimmedValue);
                        if (field.getMin() != null && Byte.parseByte(field.getMin()) > uiValue) {
                            errors.add(errorString(lineNumber, field, value));
                            throw new Exception();
                        }
                        if (field.getMax() != null && Byte.parseByte(field.getMax()) < uiValue) {
                            errors.add(errorString(lineNumber, field, value));
                            throw new Exception();
                        }
                        return uiValue;
                    } else if (field.getType().equals(Scalar.LONG)) {
                        Long uiValue = Long.parseLong(trimmedValue);
                        if (field.getMin() != null && Long.parseLong(field.getMin()) > uiValue) {
                            errors.add(errorString(lineNumber, field, value));
                            throw new Exception();
                        }
                        if (field.getMax() != null && Long.parseLong(field.getMax()) < uiValue) {
                            errors.add(errorString(lineNumber, field, value));
                            throw new Exception();
                        }
                        return uiValue;
                    } else if (field.getType().equals(Scalar.DOUBLE)) {
                        Double uiValue = Double.parseDouble(trimmedValue);
                        if (field.getMin() != null && Double.parseDouble(field.getMin()) > uiValue) {
                            errors.add(errorString(lineNumber, field, value));
                            throw new Exception();
                        }
                        if (field.getMax() != null && Double.parseDouble(field.getMax()) < uiValue) {
                            errors.add(errorString(lineNumber, field, value));
                            throw new Exception();
                        }
                        return uiValue;
                    }
                } catch (NumberFormatException e) {
                    errors.add(errorString(lineNumber, field, value));
                    throw new Exception();
                }
            }

        }
        return null;
    }

    protected interface ValidationErrorConsumer {

        public void addError(String errorMsg);
    }

}

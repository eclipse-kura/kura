/*******************************************************************************
 * Copyright (c) 2018, 2021 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.web.server.util;

import static java.lang.String.format;
import static org.eclipse.kura.configuration.ConfigurationService.KURA_SERVICE_PID;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.servlet.ServletException;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.eclipse.kura.configuration.metatype.Option;
import org.eclipse.kura.core.configuration.metatype.Tad;
import org.eclipse.kura.driver.Driver;
import org.eclipse.kura.internal.wire.asset.WireAssetChannelDescriptor;
import org.eclipse.kura.web.server.util.ServiceLocator.ServiceConsumer;
import org.eclipse.kura.web.shared.model.GwtConfigParameter;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AssetConfigValidator {

    private static Logger logger = LoggerFactory.getLogger(AssetConfigValidator.class);

    private int lineNumber;

    public static AssetConfigValidator get() {
        return new AssetConfigValidator();
    }

    @SuppressWarnings("unchecked")
    private void fillLists(List<Tad> metatypes, List<String> properties, String driverPid) throws Exception {
        ((List<Tad>) WireAssetChannelDescriptor.get().getDescriptor()).forEach(element -> {
            metatypes.add(element);
            properties.add(element.getId());
        });

        withDriver(driverPid,
                descriptor -> ((List<Tad>) descriptor.getChannelDescriptor().getDescriptor()).forEach(element -> {
                    metatypes.add(element);
                    properties.add(element.getId());
                }));
    }

    private class LineScanner {

        private final List<Tad> adsByIndex;
        private final List<GwtConfigParameter> defaultValues;

        public LineScanner(final List<Tad> fullChannelMetatype, final List<String> columnHeaders) {
            this.adsByIndex = probeAdsByIndex(fullChannelMetatype, columnHeaders);
            this.defaultValues = probeDefaultValues(fullChannelMetatype, columnHeaders);
        }

        private List<Tad> probeAdsByIndex(final List<Tad> fullChannelMetatype, final List<String> columnHeaders) {
            final List<Tad> result = new ArrayList<>();

            for (final String h : columnHeaders) {
                final Optional<Tad> exactMatch = fullChannelMetatype.stream().filter(p -> h.equals(p.getId()))
                        .findAny();

                if (exactMatch.isPresent()) {
                    result.add(exactMatch.get());
                    continue;
                }

                final String assetId = "+" + h;

                final Optional<Tad> assetParam = fullChannelMetatype.stream().filter(p -> assetId.equals(p.getId()))
                        .findAny();

                if (assetParam.isPresent()) {
                    result.add(assetParam.get());
                } else {
                    throw new IllegalStateException("Unknown parameter name " + h);
                }
            }

            return result;
        }

        private List<GwtConfigParameter> probeDefaultValues(final List<Tad> fullChannelMetatype,
                final List<String> columnHeaders) {
            final List<Tad> missing = fullChannelMetatype.stream().filter(c -> {
                if (c.getId().startsWith("+")) {
                    return !columnHeaders.contains(c.getId().substring(1));
                } else {
                    return !columnHeaders.contains(c.getId());
                }

            }).collect(Collectors.toList());

            final List<GwtConfigParameter> result = new ArrayList<>();

            for (final Tad ad : missing) {
                if (!ad.isRequired() || ad.getDefault() == null || ad.getDefault().isEmpty()) {
                    result.add(GwtServerUtil.toGwtConfigParameter(ad, null));
                    continue;
                }

                try {
                    final Object defaultValue = validate(ad, ad.getDefault(), new ArrayList<>(), 0);
                    result.add(GwtServerUtil.toGwtConfigParameter(ad, defaultValue));
                } catch (final Exception e) {
                    logger.warn("Ad default value is probably not valid", e);
                }
            }

            return result;
        }

        public String scan(final CSVRecord line, final Consumer<GwtConfigParameter> parameterConsumer,
                List<String> errors) {
            String channelName = "";
            boolean errorInChannel = false;
            if (line.size() != adsByIndex.size()) {
                errors.add("Incorrect number of fields in CSV line " + lineNumber);
                errorInChannel = true;
            }
            if (!errorInChannel) {
                for (int i = 0; i < line.size(); i++) {
                    try {
                        String token = line.get(i);
                        if (adsByIndex.get(i).getId().substring(1).equals("name")) {
                            token = token.replace(" ", "_");
                            token = token.replace("#", "_");
                            token = token.replace("+", "_");
                            channelName = token;
                        }
                        final Tad ad = adsByIndex.get(i);
                        final Object value = validate(ad, token, errors, lineNumber);

                        parameterConsumer.accept(GwtServerUtil.toGwtConfigParameter(ad, value));
                    } catch (Exception ex) {
                        errorInChannel = true;
                    }
                    if (errorInChannel) {
                        channelName = "";
                        break;
                    }
                }
            }

            this.defaultValues.forEach(d -> parameterConsumer.accept(new GwtConfigParameter(d)));
            return channelName;
        }
    }

    public List<GwtConfigParameter> validateCsv(String csv, String driverPid, List<String> errors)
            throws ServletException {

        try (CSVParser parser = CSVParser.parse(csv, CSVFormat.RFC4180)) {
            errors.clear();
            List<Tad> fullChannelMetatype = new ArrayList<>();
            List<String> propertyNames = new ArrayList<>();
            fillLists(fullChannelMetatype, propertyNames, driverPid);
            List<GwtConfigParameter> updatedAssetProps = new ArrayList<>();
            List<CSVRecord> lines = parser.getRecords();
            Set<String> channels = new HashSet<>();
            if (lines.size() <= 1) {
                errors.add("Empty CSV file.");
                throw new ValidationException();
            }
            this.lineNumber = 1;
            final List<String> header = StreamSupport.stream(lines.remove(0).spliterator(), false)
                    .collect(Collectors.toList());

            final LineScanner scanner = new LineScanner(fullChannelMetatype, header);

            lines.forEach(record -> {
                final List<GwtConfigParameter> params = new ArrayList<>();
                String channelName = scanner.scan(record, params::add, errors);
                if (!channelName.isEmpty() && !channels.contains(channelName)) {
                    channels.add(channelName);
                    for (final GwtConfigParameter param : params) {
                        final String id = channelName + "#" + param.getId();
                        param.setId(id);
                        updatedAssetProps.add(param);
                    }
                }
            });
            if (!errors.isEmpty()) {
                throw new ValidationException();
            }

            return updatedAssetProps;

        } catch (Exception ex) {
            errors.add("Validation exception on CSV file.");
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
    protected Object validate(Tad field, String value, List<String> errors, int lineNumber) throws ValidationException {

        String trimmedValue = value.trim();
        final boolean isEmpty = trimmedValue.isEmpty();

        if (field.isRequired() && isEmpty) {
            throw new ValidationException();
        }

        if (!isEmpty) {
            // Validate "Options" field first. Data type will be taken care of next.
            if (!field.getOption().isEmpty()) {
                boolean foundEqual = false;
                for (Option o : field.getOption()) {
                    foundEqual |= o.getValue().trim().equals(value.trim());
                }
                if (!foundEqual) {
                    final StringBuilder sb = new StringBuilder();
                    sb.append("Value \"").append(value).append("\" on line ").append(String.valueOf(lineNumber))
                            .append(" should be one of [");
                    field.getOption().forEach(option -> sb.append("\"").append(option.getValue()).append("\", "));
                    sb.delete(sb.length() - 2, sb.length());
                    sb.append("]");
                    errors.add(sb.toString());
                    throw new ValidationException();
                }
            }
            // Validate Data type and constraints.
            try {
                switch (field.getType()) {
                case CHAR:
                    return new CharGwtValue(trimmedValue, field, errors).getValue();
                case STRING:
                    return new StringGwtValue(trimmedValue, field, errors).getValue();
                case FLOAT:
                    return new FloatGwtValue(trimmedValue, field, errors).getValue();
                case INTEGER:
                    return new IntegerGwtValue(trimmedValue, field, errors).getValue();
                case SHORT:
                    return new ShortGwtValue(trimmedValue, field, errors).getValue();
                case BYTE:
                    return new ByteGwtValue(trimmedValue, field, errors).getValue();
                case LONG:
                    return new LongGwtValue(trimmedValue, field, errors).getValue();
                case DOUBLE:
                    return new DoubleGwtValue(trimmedValue, field, errors).getValue();
                case BOOLEAN:
                    return new BooleanGwtValue(trimmedValue, field, errors).getValue();
                default:
                    errors.add("Unsupported data type: " + field.getType().toString());
                    break;
                }
            } catch (NumberFormatException ex) {
                errors.add(errorString(lineNumber, field, value));
                throw new ValidationException();
            }
        }
        return null;
    }

    protected interface ValidationErrorConsumer {

        public void addError(String errorMsg);
    }

    protected class ValidationException extends Exception {

        private static final long serialVersionUID = 5954147929480218028L;

    }

    private abstract class GwtValue<T> {

        T value;

        public GwtValue(String csvInput, Tad param, List<String> errors) throws ValidationException {
            setValue(csvInput, param, errors);
        }

        public abstract void setValue(String csvInput, Tad param, List<String> errors) throws ValidationException;

        public Object getValue() {
            return this.value;
        }
    }

    private class BooleanGwtValue extends GwtValue<Object> {

        public BooleanGwtValue(String csvInput, Tad param, List<String> errors) throws ValidationException {
            super(csvInput, param, errors);
        }

        @Override
        public void setValue(String csvInput, Tad param, List<String> errors) throws ValidationException {
            this.value = Boolean.parseBoolean(csvInput);

        }

    }

    private class CharGwtValue extends GwtValue<Object> {

        public CharGwtValue(String csvInput, Tad param, List<String> errors) throws ValidationException {
            super(csvInput, param, errors);
        }

        @Override
        public void setValue(String csvInput, Tad field, List<String> errors) throws ValidationException {
            this.value = csvInput.charAt(0);
            if (csvInput.length() > 1) {
                errors.add(errorString(AssetConfigValidator.this.lineNumber, field, csvInput));
                throw new ValidationException();
            }
            if (field.getMin() != null && field.getMin().charAt(0) > csvInput.charAt(0)) {
                errors.add(errorString(AssetConfigValidator.this.lineNumber, field, csvInput));
                throw new ValidationException();
            }
            if (field.getMax() != null && field.getMax().charAt(0) < csvInput.charAt(0)) {
                errors.add(errorString(AssetConfigValidator.this.lineNumber, field, csvInput));
                throw new ValidationException();
            }

        }
    }

    private class StringGwtValue extends GwtValue<Object> {

        public StringGwtValue(String csvInput, Tad param, List<String> errors) throws ValidationException {
            super(csvInput, param, errors);
        }

        @Override
        public void setValue(String csvInput, Tad field, List<String> errors) throws ValidationException {
            this.value = csvInput;
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

            if (String.valueOf(csvInput).length() < configMinValue) {
                errors.add(errorString(AssetConfigValidator.this.lineNumber, field, csvInput));
                throw new ValidationException();
            }
            if (String.valueOf(csvInput).length() > configMaxValue) {
                errors.add(errorString(AssetConfigValidator.this.lineNumber, field, csvInput));
                throw new ValidationException();
            }
        }
    }

    private class LongGwtValue extends GwtValue<Object> {

        public LongGwtValue(String csvInput, Tad param, List<String> errors) throws ValidationException {
            super(csvInput, param, errors);
        }

        @Override
        public void setValue(String csvInput, Tad field, List<String> errors) throws ValidationException {
            this.value = Long.parseLong(csvInput);
            if (field.getMin() != null && Long.parseLong(field.getMin()) > (Long) this.value) {
                errors.add(errorString(AssetConfigValidator.this.lineNumber, field, csvInput));
                throw new ValidationException();
            }
            if (field.getMax() != null && Long.parseLong(field.getMax()) < (Long) this.value) {
                errors.add(errorString(AssetConfigValidator.this.lineNumber, field, csvInput));
                throw new ValidationException();
            }

        }
    }

    private class DoubleGwtValue extends GwtValue<Object> {

        public DoubleGwtValue(String csvInput, Tad param, List<String> errors) throws ValidationException {
            super(csvInput, param, errors);
        }

        @Override
        public void setValue(String csvInput, Tad field, List<String> errors) throws ValidationException {
            this.value = Double.parseDouble(csvInput);
            if (field.getMin() != null && Double.parseDouble(field.getMin()) > (Double) this.value) {
                errors.add(errorString(AssetConfigValidator.this.lineNumber, field, csvInput));
                throw new ValidationException();
            }
            if (field.getMax() != null && Double.parseDouble(field.getMax()) < (Double) this.value) {
                errors.add(errorString(AssetConfigValidator.this.lineNumber, field, csvInput));
                throw new ValidationException();
            }

        }
    }

    private class ByteGwtValue extends GwtValue<Object> {

        public ByteGwtValue(String csvInput, Tad param, List<String> errors) throws ValidationException {
            super(csvInput, param, errors);
        }

        @Override
        public void setValue(String csvInput, Tad field, List<String> errors) throws ValidationException {
            this.value = Byte.parseByte(csvInput);
            if (field.getMin() != null && Byte.parseByte(field.getMin()) > (Byte) this.value) {
                errors.add(errorString(AssetConfigValidator.this.lineNumber, field, csvInput));
                throw new ValidationException();
            }
            if (field.getMax() != null && Byte.parseByte(field.getMax()) < (Byte) this.value) {
                errors.add(errorString(AssetConfigValidator.this.lineNumber, field, csvInput));
                throw new ValidationException();
            }
        }
    }

    private class ShortGwtValue extends GwtValue<Object> {

        public ShortGwtValue(String csvInput, Tad param, List<String> errors) throws ValidationException {
            super(csvInput, param, errors);
        }

        @Override
        public void setValue(String csvInput, Tad field, List<String> errors) throws ValidationException {
            this.value = Short.parseShort(csvInput);
            if (field.getMin() != null && Short.parseShort(field.getMin()) > (Short) this.value) {
                errors.add(errorString(AssetConfigValidator.this.lineNumber, field, csvInput));
                throw new ValidationException();
            }
            if (field.getMax() != null && Short.parseShort(field.getMax()) < (Short) this.value) {
                errors.add(errorString(AssetConfigValidator.this.lineNumber, field, csvInput));
                throw new ValidationException();
            }

        }
    }

    private class IntegerGwtValue extends GwtValue<Object> {

        public IntegerGwtValue(String csvInput, Tad param, List<String> errors) throws ValidationException {
            super(csvInput, param, errors);
        }

        @Override
        public void setValue(String csvInput, Tad field, List<String> errors) throws ValidationException {
            this.value = Integer.parseInt(csvInput);
            if (field.getMin() != null && Integer.parseInt(field.getMin()) > (Integer) this.value) {
                errors.add(errorString(AssetConfigValidator.this.lineNumber, field, csvInput));
                throw new ValidationException();
            }
            if (field.getMax() != null && Integer.parseInt(field.getMax()) < (Integer) this.value) {
                errors.add(errorString(AssetConfigValidator.this.lineNumber, field, csvInput));
                throw new ValidationException();
            }

        }
    }

    private class FloatGwtValue extends GwtValue<Object> {

        public FloatGwtValue(String csvInput, Tad param, List<String> errors) throws ValidationException {
            super(csvInput, param, errors);
        }

        @Override
        public void setValue(String csvInput, Tad field, List<String> errors) throws ValidationException {
            this.value = Float.parseFloat(csvInput);
            if (field.getMin() != null && Float.parseFloat(field.getMin()) > (Float) this.value) {
                errors.add(errorString(AssetConfigValidator.this.lineNumber, field, csvInput));
                throw new ValidationException();
            }
            if (field.getMax() != null && Float.parseFloat(field.getMax()) < (Float) this.value) {
                errors.add(errorString(AssetConfigValidator.this.lineNumber, field, csvInput));
                throw new ValidationException();
            }
        }
    }

}

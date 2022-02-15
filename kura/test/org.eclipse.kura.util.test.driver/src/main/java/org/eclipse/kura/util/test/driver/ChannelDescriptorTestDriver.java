/*******************************************************************************
 * Copyright (c) 2022 Eurotech and/or its affiliates and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Eurotech
 *  Red Hat Inc
 *******************************************************************************/
package org.eclipse.kura.util.test.driver;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.kura.channel.ChannelRecord;
import org.eclipse.kura.channel.listener.ChannelListener;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.configuration.metatype.AD;
import org.eclipse.kura.core.configuration.metatype.Tad;
import org.eclipse.kura.core.configuration.metatype.Toption;
import org.eclipse.kura.core.configuration.metatype.Tscalar;
import org.eclipse.kura.driver.ChannelDescriptor;
import org.eclipse.kura.driver.Driver;
import org.eclipse.kura.driver.PreparedRead;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChannelDescriptorTestDriver implements Driver, ConfigurableComponent {

    private static final Logger logger = LoggerFactory.getLogger(ChannelDescriptorTestDriver.class);

    public void activate() {
        logger.info("activating");
    }

    public void update() {
        logger.info("updating");
    }

    public void deactivate() {
        logger.info("deactivating");
    }

    @Override
    public void connect() throws ConnectionException {
        throw new ConnectionException("Connection is not supported");
    }

    @Override
    public void disconnect() throws ConnectionException {
        throw new ConnectionException("Disconnection is not supported");
    }

    @Override
    public ChannelDescriptor getChannelDescriptor() {
        return new ChannelDescriptorImpl();
    }

    @Override
    public void read(List<ChannelRecord> records) throws ConnectionException {
        throw new ConnectionException("Read is not supported");

    }

    @Override
    public void registerChannelListener(Map<String, Object> channelConfig, ChannelListener listener)
            throws ConnectionException {
        throw new ConnectionException("Register channle listener is not supported");

    }

    @Override
    public void unregisterChannelListener(ChannelListener listener) throws ConnectionException {
        throw new ConnectionException("Unregister channle listener is not supported");

    }

    @Override
    public void write(List<ChannelRecord> records) throws ConnectionException {
        throw new ConnectionException("Write is not supported");

    }

    @Override
    public PreparedRead prepareRead(List<ChannelRecord> records) {
        return new PreparedRead() {

            @Override
            public void close() throws Exception {
                // nothing to close
            }

            @Override
            public List<ChannelRecord> getChannelRecords() {
                return records;
            }

            @Override
            public List<ChannelRecord> execute() throws ConnectionException {
                read(records);
                return records;
            }
        };
    }

    private static class ChannelDescriptorImpl implements ChannelDescriptor {

        private final List<AD> ads = new ArrayList<>();

        public ChannelDescriptorImpl() {
            generateAds(Tscalar.STRING, "foo", "bar", "foo");
            generateAds(Tscalar.BOOLEAN, "true");
            generateAds(Tscalar.BYTE, "10", "20", "15");
            generateAds(Tscalar.CHAR, "b", "l", "c");
            generateAds(Tscalar.DOUBLE, "13.5", "20.5", "16");
            generateAds(Tscalar.FLOAT, "13.5", "20.5", "16");
            generateAds(Tscalar.INTEGER, "-200000", "300000", "10");
            generateAds(Tscalar.LONG, "" + ((long) Integer.MIN_VALUE - 10000), "" + ((long) Integer.MAX_VALUE + 10000),
                    "2");
            // generateAds(Tscalar.PASSWORD, "foo", "bar", "foo");
            generateAds(Tscalar.SHORT, "-20000", "20000", "1");

            generateOptionAd(Tscalar.STRING, "foo", "bar", "baz");
            generateOptionAd(Tscalar.BYTE, "10", "20", "30");
            generateOptionAd(Tscalar.CHAR, "a", "b", "c");
            generateOptionAd(Tscalar.DOUBLE, "-10.21", "20.123123", "30.23123");
            generateOptionAd(Tscalar.FLOAT, "10.4", "-12.4", "0.5");
            generateOptionAd(Tscalar.INTEGER, "-200000", "300000", "10");
            generateOptionAd(Tscalar.LONG, "" + ((long) Integer.MIN_VALUE - 10000),
                    "" + ((long) Integer.MAX_VALUE + 10000), "-34");
            generateOptionAd(Tscalar.SHORT, "-20000", "20000", "1");
        }

        private AdBuilder createBuilder(final Tscalar type, final String defaultValue) {
            return new AdBuilder(type + ".prop", type).withName(type + " property")
                    .withDescription("A " + type + " property").withDefaultValue(defaultValue);
        }

        private void generateAds(final Tscalar type, final String defaultValue) {
            final AdBuilder builder = createBuilder(type, defaultValue);

            ads.add(builder.build());

            ads.add(builder.duplicate().withId(builder.id + "not.required")
                    .withName(builder.name.orElse("") + " not required")
                    .withDescription(builder.description.orElse("") + " not required").withoutRequired()
                    .withoutDefaultValue().build());

        }

        private void generateAds(final Tscalar type, final String min, final String max, final String defaultValue) {
            generateAds(type, defaultValue);

            final AdBuilder builder = createBuilder(type, defaultValue);

            ads.add(builder.duplicate().withId(builder.id + ".min.max")
                    .withName(builder.name.orElse("") + " with min and max")
                    .withDescription(builder.description.orElse("") + " with min : " + min + " and max : " + max)
                    .withMin(min).withMax(max).build());
        }

        private void generateOptionAd(final Tscalar type, final String... values) {
            final AdBuilder builder = new AdBuilder(type + ".options", type).withDefaultValue(values[0])
                    .withName(type + " property with options").withDescription("A " + type + " property wit options")
                    .withDefaultValue(values[0]);

            for (int i = 0; i < values.length; i++) {
                builder.withOption("Choice " + i + ", value : " + values[i], values[i]);
            }

            ads.add(builder.build());
        }

        @Override
        public Object getDescriptor() {
            return ads;
        }

    }

    private static class AdBuilder {

        private String id;
        private Tscalar type;
        private Optional<Integer> cardinality = Optional.empty();
        private Optional<String> defaultValue = Optional.empty();
        private Optional<String> description = Optional.empty();
        private Optional<String> max = Optional.empty();
        private Optional<String> min = Optional.empty();
        private Optional<String> name = Optional.empty();
        private List<Toption> options = new ArrayList<>();
        private Optional<Boolean> required = Optional.empty();

        public AdBuilder(final String id, final Tscalar type) {
            this.id = id;
            this.type = type;
        }

        public AdBuilder(final AdBuilder other) {
            this.id = other.id;
            this.type = other.type;
            this.cardinality = other.cardinality;
            this.defaultValue = other.defaultValue;
            this.description = other.description;
            this.max = other.max;
            this.min = other.min;
            this.name = other.name;
            this.options = other.options;
            this.required = other.required;
        }

        public AdBuilder withId(final String id) {
            this.id = id;
            return this;
        }

        public AdBuilder withType(final Tscalar type) {
            this.type = type;
            return this;
        }

        public AdBuilder withCardinality(final int cardinality) {
            this.cardinality = Optional.of(cardinality);
            return this;
        }

        public AdBuilder withoutCardinality() {
            this.cardinality = Optional.empty();
            return this;
        }

        public AdBuilder withDefaultValue(final String defaultValue) {
            this.defaultValue = Optional.of(defaultValue);
            return this;
        }

        public AdBuilder withoutDefaultValue() {
            this.defaultValue = Optional.empty();
            return this;
        }

        public AdBuilder withDescription(final String description) {
            this.description = Optional.of(description);
            return this;
        }

        public AdBuilder withoutDescription() {
            this.description = Optional.empty();
            return this;
        }

        public AdBuilder withMax(final String max) {
            this.max = Optional.of(max);
            return this;
        }

        public AdBuilder witoutMax() {
            this.max = Optional.empty();
            return this;
        }

        public AdBuilder withMin(final String min) {
            this.max = Optional.of(min);
            return this;
        }

        public AdBuilder withoutMin() {
            this.max = Optional.empty();
            return this;
        }

        public AdBuilder withName(final String name) {
            this.name = Optional.of(name);
            return this;
        }

        public AdBuilder withoutName() {
            this.name = Optional.empty();
            return this;
        }

        public AdBuilder withOption(final String label, final String value) {
            final Toption option = new Toption();

            option.setLabel(label);
            option.setValue(value);
            options.add(option);

            return this;
        }

        public AdBuilder withRequired(final boolean required) {
            this.required = Optional.of(required);
            return this;
        }

        public AdBuilder withoutRequired() {
            this.required = Optional.empty();
            return this;
        }

        public AdBuilder duplicate() {
            return new AdBuilder(this);
        }

        public AD build() {
            final Tad ad = new Tad();

            ad.setId(id);
            ad.setType(type);

            cardinality.ifPresent(ad::setCardinality);
            defaultValue.ifPresent(ad::setDefault);
            description.ifPresent(ad::setDescription);
            max.ifPresent(ad::setMax);
            min.ifPresent(ad::setMin);
            name.ifPresent(ad::setName);
            ad.getOption().addAll(this.options);
            required.ifPresent(ad::setRequired);

            return ad;
        }
    }

}

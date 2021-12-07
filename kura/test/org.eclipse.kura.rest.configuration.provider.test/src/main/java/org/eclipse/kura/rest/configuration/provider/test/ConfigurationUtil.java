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
 ******************************************************************************/
package org.eclipse.kura.rest.configuration.provider.test;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.metatype.AD;
import org.eclipse.kura.configuration.metatype.Icon;
import org.eclipse.kura.configuration.metatype.OCD;
import org.eclipse.kura.configuration.metatype.Option;
import org.eclipse.kura.configuration.metatype.Scalar;

public class ConfigurationUtil {

    private ConfigurationUtil() {
    }

    public static AdBuilder adBuilder(final String id, final Scalar type) {
        return new AdBuilder(id, type);
    }

    public static OCDBuilder ocdBuilder(final String id) {
        return new OCDBuilder(id);
    }

    public static ComponentConfigurationBuilder configurationBuilder(final String pid) {
        return new ComponentConfigurationBuilder(pid);
    }

    public static class AdBuilder {

        private final String id;
        private final Scalar type;
        private int cardinality;
        private String defaultValue;
        private String description;
        private String max;
        private String min;
        private String name;
        private List<Option> options = new ArrayList<>();
        private boolean isRequired;

        private AdBuilder(final String id, final Scalar type) {
            this.id = id;
            this.type = type;
        }

        public AdBuilder withCardinality(final int cardinality) {
            this.cardinality = cardinality;
            return this;
        }

        public AdBuilder withDefault(final String defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public AdBuilder withDescription(final String description) {
            this.description = description;
            return this;
        }

        public AdBuilder withMax(final String max) {
            this.max = max;
            return this;
        }

        public AdBuilder withMin(final String min) {
            this.min = min;
            return this;
        }

        public AdBuilder withName(final String name) {
            this.name = name;
            return this;
        }

        public AdBuilder withOption(final String label, final String value) {
            this.options.add(new Option() {

                @Override
                public String getLabel() {
                    return label;
                }

                @Override
                public String getValue() {
                    return value;
                }
            });
            return this;
        }

        public AdBuilder withRequired(final boolean isRequired) {
            this.isRequired = isRequired;
            return this;
        }

        public AD build() {
            return new AD() {

                @Override
                public List<Option> getOption() {
                    return options;
                }

                @Override
                public String getName() {
                    return name;
                }

                @Override
                public String getDescription() {
                    return description;
                }

                @Override
                public String getId() {
                    return id;
                }

                @Override
                public Scalar getType() {
                    return type;
                }

                @Override
                public int getCardinality() {
                    return cardinality;
                }

                @Override
                public String getMin() {
                    return min;
                }

                @Override
                public String getMax() {
                    return max;
                }

                @Override
                public String getDefault() {
                    return defaultValue;
                }

                @Override
                public boolean isRequired() {
                    return isRequired;
                }

            };
        }
    }

    public static class OCDBuilder {

        private final String id;
        private final List<Icon> icon = new ArrayList<>();
        private final List<AD> ads = new ArrayList<>();

        private String description;
        private String name;

        private OCDBuilder(final String id) {
            this.id = id;
        }

        public OCDBuilder withAd(final AD ad) {
            this.ads.add(ad);
            return this;
        }

        public OCDBuilder withDescription(final String description) {
            this.description = description;
            return this;
        }

        public OCDBuilder withIcon(final String resource, final BigInteger size) {
            this.icon.add(new Icon() {

                @Override
                public String getResource() {
                    return resource;
                }

                @Override
                public BigInteger getSize() {
                    return size;
                }

            });
            return this;
        }

        public OCDBuilder withName(final String name) {
            this.name = name;
            return this;
        }

        public OCD build() {
            return new OCD() {

                @Override
                public List<AD> getAD() {
                    return ads;
                }

                @Override
                public List<Icon> getIcon() {
                    return icon;
                }

                @Override
                public String getName() {
                    return name;
                }

                @Override
                public String getDescription() {
                    return description;
                }

                @Override
                public String getId() {
                    return id;
                }

            };
        }
    }

    public static class ComponentConfigurationBuilder {

        private final String pid;
        private OCD definition;
        private Map<String, Object> configurationProperties;

        private ComponentConfigurationBuilder(final String pid) {
            this.pid = pid;
        }

        public ComponentConfigurationBuilder withDefinition(final OCD definition) {
            this.definition = definition;
            return this;
        }

        public ComponentConfigurationBuilder withConfigurationProperties(
                final Map<String, Object> configurationProperties) {
            this.configurationProperties = configurationProperties;
            return this;
        }

        public ComponentConfiguration build() {
            return new ComponentConfiguration() {

                @Override
                public String getPid() {
                    return pid;
                }

                @Override
                public OCD getDefinition() {
                    return definition;
                }

                @Override
                public Map<String, Object> getConfigurationProperties() {
                    return configurationProperties;
                }
            };
        }
    }
}

/*******************************************************************************
 * Copyright (c) 2020 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.web;

import static java.util.Objects.requireNonNull;

import org.eclipse.kura.core.configuration.metatype.Tad;
import org.eclipse.kura.core.configuration.metatype.Toption;
import org.eclipse.kura.core.configuration.metatype.Tscalar;

public class AdBuilder {

    private final Tad ad = new Tad();

    public AdBuilder(final String id, final String name, final Tscalar type) {
        ad.setId(requireNonNull(id));
        ad.setName(requireNonNull(name));
        ad.setType(requireNonNull(type));
        ad.setCardinality(0);
        ad.setRequired(true);
    }

    public AdBuilder setName(final String name) {
        ad.setName(name);
        return this;
    }

    public AdBuilder setDescription(final String description) {
        ad.setDescription(description);
        return this;
    }

    public AdBuilder addOption(final String label, final String value) {
        final Toption option = new Toption();
        option.setLabel(label);
        option.setValue(value);
        return this;
    }

    public AdBuilder setCardinality(final int cardinality) {
        ad.setCardinality(cardinality);
        return this;
    }

    public AdBuilder setMin(final String min) {
        ad.setMin(min);
        return this;
    }

    public AdBuilder setMax(final String max) {
        ad.setMax(max);
        return this;
    }

    public AdBuilder setDefault(final String defaultValue) {
        ad.setDefault(defaultValue);
        return this;
    }

    public AdBuilder setRequired(final boolean required) {
        ad.setRequired(required);
        return this;
    }

    public Tad build() {
        return ad;
    }
}

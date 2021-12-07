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
package org.eclipse.kura.rest.configuration.api;

import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.kura.configuration.metatype.AD;
import org.eclipse.kura.configuration.metatype.Option;
import org.eclipse.kura.configuration.metatype.Scalar;

public class AdDTO implements AD {

    private final List<Option> option;
    private final String name;
    private final String description;
    private final String id;
    private final Scalar type;
    private final int cardinality;
    private final String min;
    private final String max;
    private final String defaultValue;
    private final boolean isRequired;

    public AdDTO(final AD ad) {
        this.option = ad.getOption() == null || ad.getOption().isEmpty() ? null
                : ad.getOption().stream().map(OptionDTO::new).collect(Collectors.toList());
        this.name = ad.getName();
        this.description = ad.getDescription();
        this.id = ad.getId();
        this.type = ad.getType();
        this.cardinality = ad.getCardinality();
        this.min = ad.getMin();
        this.max = ad.getMax();
        this.defaultValue = ad.getDefault();
        this.isRequired = ad.isRequired();
    }

    @Override
    public List<Option> getOption() {
        return option;
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

}

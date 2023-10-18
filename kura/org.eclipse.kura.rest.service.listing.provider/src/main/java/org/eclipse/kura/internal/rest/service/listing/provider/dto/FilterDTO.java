/*******************************************************************************
 * Copyright (c) 2023 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.internal.rest.service.listing.provider.dto;

import java.util.List;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.internal.rest.service.listing.provider.util.FilterBuilder;

public class FilterDTO {

    private final String name;
    private final String value;
    private final List<FilterDTO> and;
    private final List<FilterDTO> or;
    private final FilterDTO not;

    public FilterDTO(String name, String value, List<FilterDTO> and, List<FilterDTO> or, FilterDTO not) {
        this.name = name;
        this.value = value;
        this.and = and;
        this.or = or;
        this.not = not;
    }

    public String toOSGIFilter() {

        return toOSGIFilter(new FilterBuilder()).build();
    }

    public void validate() throws KuraException {
        int count = 0;

        if (name != null) {
            if (name.contains(" ")) {
                throw new KuraException(KuraErrorCode.BAD_REQUEST, "Name must contain no spaces");
            }

            if (name.trim().isEmpty()) {
                throw new KuraException(KuraErrorCode.BAD_REQUEST, "Name must not be empty");
            }

            count++;
        }

        if (and != null) {
            count++;
        }

        if (or != null) {
            count++;
        }

        if (not != null) {
            count++;
        }

        if (count != 1) {
            throw new KuraException(KuraErrorCode.BAD_REQUEST,
                    "Exactly one among the \"name\", \"and\", \"or\" and \"not\" properties must be specified");
        }
    }

    private FilterBuilder toOSGIFilter(final FilterBuilder filterBuilder) {
        if (name != null) {
            filterBuilder.property(name, value);
        } else if (and != null) {
            filterBuilder.and(b -> and.forEach(f -> f.toOSGIFilter(b)));
        } else if (or != null) {
            filterBuilder.or(b -> or.forEach(f -> f.toOSGIFilter(b)));
        } else if (not != null) {
            filterBuilder.not(not::toOSGIFilter);
        }

        return filterBuilder;
    }

}

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

import org.eclipse.kura.configuration.metatype.Option;

public class OptionDTO implements Option {

    private final String label;
    private final String value;

    public OptionDTO(final Option other) {
        this.label = other.getLabel();
        this.value = other.getValue();
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public String getValue() {
        return value;
    }

}

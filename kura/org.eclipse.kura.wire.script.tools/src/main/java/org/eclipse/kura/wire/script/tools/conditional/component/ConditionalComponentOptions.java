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
 *******************************************************************************/
package org.eclipse.kura.wire.script.tools.conditional.component;

import java.util.Map;
import java.util.Optional;

public class ConditionalComponentOptions {

    public static final String FILTER_LANGUAGE_ID = "js";
    public static final String CONDITION_PROPERTY_KEY = "condition";

    private String booleanExpression;

    ConditionalComponentOptions(final Map<String, Object> properties) {
        this.booleanExpression = (String) properties.get(CONDITION_PROPERTY_KEY);
        this.booleanExpression = this.booleanExpression == null ? "" : this.booleanExpression.trim();
    }

    Optional<String> getBooleanExpression() {
        if (this.booleanExpression.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(this.booleanExpression);
    }

}

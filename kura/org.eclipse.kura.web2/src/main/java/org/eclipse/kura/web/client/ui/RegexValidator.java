/*******************************************************************************
 * Copyright (c) 2019 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.web.client.ui;

import com.google.gwt.regexp.shared.RegExp;

public class RegexValidator extends PredicateValidator {

    public RegexValidator(final String pattern, final String message) {
        this(pattern, "g", message);
    }

    public RegexValidator(final String pattern, final String flags, final String message) {
        super(v -> RegExp.compile(pattern, flags).exec(v) != null, message);
    }

}

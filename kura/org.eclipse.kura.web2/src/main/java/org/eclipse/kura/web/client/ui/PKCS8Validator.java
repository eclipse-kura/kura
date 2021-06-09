/*******************************************************************************
 * Copyright (c) 2011, 2021 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.web.client.ui;

public class PKCS8Validator extends RegexValidator {

    private static final String PKCS8 = "-{3,}BEGIN \\w+ PRIVATE KEY-{3,}(\\W|.)*?-{3,}END \\w+ PRIVATE KEY-{3,}";

    public PKCS8Validator(String message) {
        super(PKCS8, message);
    }
}

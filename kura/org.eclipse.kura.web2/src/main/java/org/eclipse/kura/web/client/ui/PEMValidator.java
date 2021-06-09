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

public class PEMValidator extends RegexValidator {

    private static final String PEM_REGEX = "^-{3,}BEGIN CERTIFICATE-{3,}[\\W\\w]*?-{3,}END CERTIFICATE-{3,}$";

    public PEMValidator(String message) {
        super(PEM_REGEX, message);
    }

}

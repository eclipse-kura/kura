/*******************************************************************************
 * Copyright (c) 2026 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.internal.rest.auth.jwt;

public class JwtRestServiceConstants {

    private JwtRestServiceConstants() {
    }

    public static final String AUDIT_FAILURE_FORMAT_STRING = "{} Rest - Failure - {}";
    public static final String SERVICE_PATH = "/token/jwt/v1";
    public static final String ISSUE_PATH = "/issue";
    public static final String REFRESH_PATH = "/refresh";
    public static final String INTENDED_REFRESH_CONSUMER = SERVICE_PATH + REFRESH_PATH;
    public static final String INTENDED_ACCESS_CONSUMER = SERVICE_PATH + "/access";
    public static final String TOKEN_TYPE = "Bearer";
}

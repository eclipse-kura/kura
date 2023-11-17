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
package org.eclipse.kura.internal.rest.auth;

public class SessionRestServiceConstants {

    public static final String BASE_PATH = "/session/v1";
    public static final String LOGIN_PASSWORD_PATH = "/login/password";
    public static final String LOGIN_CERTIFICATE_PATH = "/login/certificate";
    public static final String XSRF_TOKEN_PATH = "/xsrfToken";
    public static final String CHANGE_PASSWORD_PATH = "/changePassword";
    public static final String LOGOUT_PATH = "/logout";
    public static final String CURRENT_IDENTITY = "/currentIdentity";
    public static final String AUTHENTICATION_INFO = "/authenticationInfo";

    private SessionRestServiceConstants() {
    }
}

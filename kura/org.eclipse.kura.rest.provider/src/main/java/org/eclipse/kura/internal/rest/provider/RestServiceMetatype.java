/*******************************************************************************
 * Copyright (c) 2011, 2026 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.internal.rest.provider;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(id = "org.eclipse.kura.internal.rest.provider.RestService", name = "RestService", description = "This service allows to configure settings related to Kura REST APIs")
public @interface RestServiceMetatype {

    @AttributeDefinition(name = "Allowed Ports", cardinality = 3, required = false, min = "1", max = "65535", description = "If set to a non empty list, REST API access will be allowed only on the specified ports. If set to an empty list, access will be allowed on all ports. Please make sure that the allowed ports are open in HttpService and Firewall configuration.")
    int[] allowed_ports() default { 443, 4443 };

    @AttributeDefinition(name = "Password Authentication Enabled", description = "Enables or disables the built-in password authentication support.")
    boolean auth_password_enabled() default true;

    @AttributeDefinition(name = "Certificate Authentication Enabled", description = "Enables or disables the built-in certificate authentication support.")
    boolean auth_certificate_enabled() default true;

    @AttributeDefinition(name = "Session Based Authentication Enabled", description = "If set to true, enables authentication using the dedicated /services/session/v1 endpoints and cookie based session management.")
    boolean session_management_enabled() default true;

    @AttributeDefinition(name = "Session Inactivity Interval (Seconds)", min = "1", description = "The session inactivity interval, sessions will expire if no request is performed for the amount of time specified by this parameter in seconds. This parameter is ignored if Session Based Authentication Enabled is set to false.")
    int session_inactivity_interval() default 900;

    @AttributeDefinition(name = "Basic Authentication Enabled", description = "Allows to perform authentication by providing identity name and password as BASIC credentials in the request to any resource endpoint. Requires that the Password Authentication Enabled parameter is set to true.")
    boolean auth_basic_enabled() default true;

    @AttributeDefinition(name = "Enable Certificate Authentication Without Session Management", description = "If set to true, calling /services/session/v1/certificate to create a session will not be necessary in order to perform certificate based authentication. Presenting a valid HTTPS client certificate and accessing resource endpoint directly is enough for authentication to succeed. Requires that the Certificate Authentication Enabled parameter is set to true.")
    boolean auth_certificate_stateless_enabled() default true;

}



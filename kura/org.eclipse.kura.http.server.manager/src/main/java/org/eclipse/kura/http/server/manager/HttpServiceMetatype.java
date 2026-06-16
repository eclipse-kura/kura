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
package org.eclipse.kura.http.server.manager;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.Option;

@ObjectClassDefinition(id = "org.eclipse.kura.http.server.manager.HttpService", name = "HttpService", description = "This service allows the user to enable and configure the http and https connectors in Kura web server. Every change to this service will cause a restart of the web server and a possible temporary UI unavailability.")
public @interface HttpServiceMetatype {

    @AttributeDefinition(name = "HTTP Ports", cardinality = 3, required = false, min = "1", max = "65535", description = "Specifies a list of ports for unencrypted HTTP. If set to an empty list, unencrypted HTTP will be disabled.")
    int http_ports();

    @AttributeDefinition(name = "HTTPS Without Certificate Authentication Ports", cardinality = 3, required = false, min = "1", max = "65535", description = "Specifies a list of ports for HTTPS without client side certificate authentication. If set to an empty list, HTTPS without client side certificate authentication will be disabled.")
    int https_ports();

    @AttributeDefinition(name = "HTTPS With Certificate Authentication Ports", cardinality = 3, required = false, min = "1", max = "65535", description = "Specifies a list of ports for HTTPS with client side certificate authentication. If set to an empty list, HTTPS with client side certificate authentication will be disabled.")
    int https_client_auth_ports();

    @AttributeDefinition(name = "KeystoreService Target Filter", description = "Specifies, as an OSGi target filter, the pid of the KeystoreService used to manage the HTTPS Keystore.")
    String KeystoreService_target() default "(kura.service.pid=changeme)";

    @AttributeDefinition(name = "Revocation Check Enabled", required = false, description = "If enabled, the revocation status of client certificates will be checked during TLS handshake. If a revoked certificate is detected, the following handshake will fail. The revocation status will be checked using OCSP, CRLDP, or the CRLs cached by the attached KeystoreService instance, depending on the value of the Revocation Check Mode parameter. If not enabled, the revocation check will not be performed.")
    boolean https_revocation_check_enabled() default false;

    @AttributeDefinition(name = "Revocation Check Mode", options = { @Option(label = "Use OCSP first and then KeystoreService CRLs and CRLDP", value = "PREFER_OCSP"), @Option(label = "Use KeystoreService CRLs and CRLDP first and then OCSP", value = "PREFER_CRL"), @Option(label = "Use only KeystoreService CRLs and CRLDP", value = "CRL_ONLY") }, description = "Specifies the mode for performing revocation check. This parameter is ignored if Revocation Check Enabled is set to false.")
    String ssl_revocation_mode() default "PREFER_OCSP";

    @AttributeDefinition(name = "Revocation Soft-fail Enabled", required = false, description = "Specifies whether the revocation soft fail is enabled or not. If it is enabled and the gateway is not able to verify the revocation status of a client certificate (for example due to a connectivity problem), the certificate will be rejected. This parameter is ignored if Revocation Check Enabled is set to false.")
    boolean https_client_revocation_soft_fail() default false;

}



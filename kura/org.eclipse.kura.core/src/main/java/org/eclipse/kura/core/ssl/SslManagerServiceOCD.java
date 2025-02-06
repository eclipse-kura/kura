/*******************************************************************************
 * Copyright (c) 2025 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.core.ssl;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Icon;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.Option;

@SuppressWarnings("checkstyle:MethodName")
@ObjectClassDefinition(id = "org.eclipse.kura.ssl.SslManagerService", //
        name = "SslManagerService", //
        description = "The SslManagerService is responsible to manage the configuration of the SSL connections.", //
        icon = @Icon(resource = "SslManagerService", size = 32) //
)
public interface SslManagerServiceOCD {

    @AttributeDefinition(name = "ssl.default.protocol", //
            required = false, //
            defaultValue = "TLSv1.2", //
            description = "The protocol to use to initialize the SSLContext. "
                    + "If not specified, the default JVM SSL Context will be used.")
    String ssl_default_protocol();

    @AttributeDefinition(name = "ssl.hostname.verification", //
            required = false, //
            defaultValue = "true", //
            description = "Enable or disable hostname verification.")
    Boolean ssl_hostname_verification();

    @AttributeDefinition(name = "Keystore Target Filter", //
            defaultValue = "(kura.service.pid=changeme)", //
            description = "Specifies, as an OSGi target filter, "
                    + "the pid of the KeystoreService used to manage the SSL key store.")
    String KeystoreService_target();

    @AttributeDefinition(name = "Truststore Target Filter", //
            defaultValue = "(kura.service.pid=changeme)", //
            description = "Specifies, as an OSGi target filter, "
                    + "the pid of the KeystoreService used to manage the SSL trust Store."
                    + " If the target service cannot be found, "
                    + "the service configured with the Keystore Target Filter parameter will be used as truststore.")
    String TruststoreKeystoreService_target();

    @AttributeDefinition(name = "ssl.default.cipherSuites", //
            required = false, //
            description = "Comma-separated list of allowed ciphers."
                    + " If not specifed, all Java VM ciphers will be allowed.")
    String ssl_default_cipherSuites();

    @AttributeDefinition(name = "Revocation Check Enabled", //
            required = false, //
            defaultValue = "false", //
            description = "If enabled, "
                    + "the revocation status of server certificates will be ckeched during TLS handshake. "
                    + "If a revoked certificate is detected, handshake will fail. "
                    + "The revocation status will be checked using OCSP, CRLDP or the CRLs cached by the attached KeystoreService instance, "
                    + "depending on the value of the Revocation Check Mode parameter. "
                    + "If not enabled, revocation ckeck will not be performed.")
    Boolean ssl_revocation_check_enabled();

    @AttributeDefinition(name = "Revocation Check Mode", //
            defaultValue = "PREFER_OCSP", //
            description = "Specifies the mode for performing revocation check. "
                    + "This parameter is ignored if Revocation Check Enabled is set to false.", //
            options = {
                    @Option(label = "Use OCSP first and then KeystoreService CRLs and CRLDP", value = "PREFER_OCSP"), //
                    @Option(label = "Use KeystoreService CRLs and CRLDP first and then OCSP", value = "PREFER_CRL"), //
                    @Option(label = "Use only KeystoreService CRLs and CRLDP", value = "CRL_ONLY") //
            })
    String ssl_revocation_mode();

    @AttributeDefinition(name = "Revocation Soft-fail Enabled", //
            defaultValue = "false", //
            description = "Specifies whether the revocation soft fail is enabled or not." //
                    + " If it is not enabled and the gateway is not able to determine the revocation status of a server certificate,"
                    + " for example due to a network error, the certificate will be rejected."
                    + " This parameter is ignored if Revocation Check Enabled is set to false.")
    Boolean ssl_revocation_soft_fail();

}

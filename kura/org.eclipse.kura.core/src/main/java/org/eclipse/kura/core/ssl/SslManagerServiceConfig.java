package org.eclipse.kura.core.ssl;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Icon;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.Option;

@ObjectClassDefinition(
        id = "org.eclipse.kura.ssl.SslManagerService",
        name = "SslManagerService",
        description = "The SslManagerService is responsible to manage the configuration of the SSL connections.",
        localization = "en_us",
        icon = @Icon(resource = "SslManagerService", size = 32))
public @interface SslManagerServiceConfig {

    @AttributeDefinition(
            name = "ssl.default.protocol",
            cardinality = 0,
            required = false,
            description = "The protocol to use to initialize the SSLContext. If not specified, the default JVM SSL Context will be used.")
    String ssl_default_protocol() default "TLSv1.2";

    @AttributeDefinition(
            name = "ssl.hostname.verification",
            cardinality = 0,
            required = false,
            description = "Enable or disable hostname verification.")
    boolean ssl_hostname_verification() default true;

    @AttributeDefinition(
            name = "KeystoreService Target Filter",
            cardinality = 0,
            required = true,
            description = "Specifies, as an OSGi target filter, the pid of the KeystoreService used to manage the HTTPS Keystore.")
    String KeystoreService_target() default "(kura.service.pid=changeme)";

    @AttributeDefinition(
            name = "ssl.default.cipherSuites",
            cardinality = 0,
            required = false,
            description = "Comma-separated list of allosed ciphers. If not specifed, all Java VM ciphers will be allowed.")
    String ssl_default_cipherSuites() default "";

    @AttributeDefinition(
            name = "Revocation Check Enabled",
            cardinality = 0,
            required = false,
            description = "If enabled, the revocation status of server certificates will be ckeched during TLS handshake. If a revoked certificate"
                    + " is detected, handshake will fail. The revocation status will be checked using OCSP, CRLDP or the CRLs cached by the"
                    + " attached KeystoreService instance, depending on the value of the Revocation Check Mode parameter. If not enabled,"
                    + " revocation ckeck will not be performed.")
    boolean ssl_revocation_check_enabled() default false;

    @AttributeDefinition(
            name = "Revocation Check Mode",
            cardinality = 0,
            required = true,
            description = "Specifies the mode for performing revocation check. This parameter is ignored if Revocation Check Enabled"
                    + " is set to false.",
            options = {
                    @Option(label = "Use OCSP first and then KeystoreService CRLs and CRLDP", value = "PREFER_OCSP"),
                    @Option(label = "Use KeystoreService CRLs and CRLDP first and then OCSP", value = "PREFER_CRL"),
                    @Option(label = "Use only KeystoreService CRLs and CRLDP", value = "CRL_ONLY") })
    String ssl_revocation_mode() default "PREFER_OCSP";

    @AttributeDefinition(
            name = "Revocation Soft-fail Enabled",
            cardinality = 0,
            required = true,
            description = "Specifies whether the revocation soft fail is enabled or not. If it is not enabled and the gateway is not able"
                    + " to determine the revocation status of a server certificate, for example due to a network error, the certificate"
                    + " will be rejected. This parameter is ignored if Revocation Check Enabled is set to false.")
    boolean ssl_revocation_soft_fail() default false;

}

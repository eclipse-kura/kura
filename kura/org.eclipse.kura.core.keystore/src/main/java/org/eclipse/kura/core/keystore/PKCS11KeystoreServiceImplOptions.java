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
package org.eclipse.kura.core.keystore;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.Option;

@ObjectClassDefinition(id = "org.eclipse.kura.core.keystore.PKCS11KeystoreServiceImpl", name = "FilesystemKeystoreServiceImpl", description = "The service allows to expose a PKCS11 module as a KeystoreService. See the https://docs.oracle.com/javase/8/docs/technotes/guides/security/p11guide.html document for more information about the configuration parameters.")
public @interface PKCS11KeystoreServiceImplOptions {

    @AttributeDefinition(name = "PKCS11 Implementation Library Path", description = "The path to the PKCS11 implementation library shared object. e.g. /lib/libmypkcs11.so")
    String library_path() default "/lib/libmypkcs11.so";

    @AttributeDefinition(name = "Pin", type = AttributeType.PASSWORD, required = false, description = "The PIN to be used for PKCS11 operations.")
    String pin();

    @AttributeDefinition(name = "Slot", required = false, min = "0", description = "The slot parameter as an integer. This parameter is optional, at most one of the slot and slotListIndex properties can be specified.")
    int slot();

    @AttributeDefinition(name = "Slot List Index", required = false, description = "The slotListIndex parameter as an integer. This parameter is optional, at most one of the slot and slotListIndex properties can be specified.")
    int slot_list_index();

    @AttributeDefinition(name = "Enabled Mechanisms", required = false, description = "The enabledMechanisms parameter as a list of whitespace separated strings. The curly braces must be omitted.")
    String enabled_mechanisms();

    @AttributeDefinition(name = "Disabled Mechanisms", required = false, description = "The disabledMechanisms parameter as a list of whitespace separated strings. The curly braces must be omitted.")
    String disabled_mechanisms();

    @AttributeDefinition(name = "Attributes", required = false, description = "The attributes parameter. The value of this field will be appended to the provider configuration.|TextArea")
    String attributes();

    @AttributeDefinition(name = "CRL Store Path", required = false, description = "The path where to store the cached CRLs. If left empty, the CRLs will be stored in a new file in the security subfolder of the Kura user configuration directory.")
    String crl_store_path();

    @AttributeDefinition(name = "CRL Cache Enabled", description = "If enabled, the service will maintain a local CRL cache by periodically downloading and storing CRLs from HTTP distribution points. The distribution points specified in the trusted certificates added to keystore will be considered along with the HTTP URLs specified using the CRL URLs parameter.")
    boolean crl_management_enabled() default true;

    @AttributeDefinition(name = "CRL URLs", cardinality = 5, required = false, description = "Alllows to specify a list of HTTP CRL distribution points that will be considered along with the HTTP distribution points specified in the trusted certificates added to keystore.")
    String crl_urls() default "";

    @AttributeDefinition(name = "CRL Force Update Interval", description = "Defines a time interval for forcing the update of cached CRLs, this interval will be considered in addition to CRL next update date. The minimum time interval is 30 seconds.")
    long crl_update_interval() default 1L;

    @AttributeDefinition(name = "CRL Force Update Interval Time Unit", options = { @Option(label = "SECONDS", value = "SECONDS"), @Option(label = "MINUTES", value = "MINUTES"), @Option(label = "HOURS", value = "HOURS"), @Option(label = "DAYS", value = "DAYS") }, description = "The time unit for the CRL Update Interval parameter")
    String crl_update_interval_time_unit() default "DAYS";

    @AttributeDefinition(name = "CRL Check Interval", description = "Defines a time interval for the periodic check of stored CRLs. Durning the periodic check the stored CRLs will be processed and updated if needed.")
    long crl_check_interval() default 5L;

    @AttributeDefinition(name = "CRL Check Interval Time Unit", options = { @Option(label = "SECONDS", value = "SECONDS"), @Option(label = "MINUTES", value = "MINUTES"), @Option(label = "HOURS", value = "HOURS"), @Option(label = "DAYS", value = "DAYS") }, description = "The time unit for the CRL Check Interval parameter")
    String crl_check_interval_time_unit() default "MINUTES";

    @AttributeDefinition(name = "Enable CRL Verification", description = "If set to true, the downloaded CRLs will be stored only if signed with the public key of one of trusted certificates in this keystore.")
    boolean verify_crl() default true;

}



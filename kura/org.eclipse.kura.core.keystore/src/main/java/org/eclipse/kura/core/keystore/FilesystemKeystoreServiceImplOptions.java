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

@ObjectClassDefinition(id = "org.eclipse.kura.core.keystore.FilesystemKeystoreServiceImpl", name = "FilesystemKeystoreServiceImpl", description = "The service allows to reference a Java Keystore in the filesystem. The default password provided can be randomized by the framework to get a per instance specific password.")
public @interface FilesystemKeystoreServiceImplOptions {

    @AttributeDefinition(name = "Keystore Path", description = "Specifies the filesystem path to a Java Keystore. If not present the file will be created.")
    String keystore_path() default "/tmp/keystore.ks";

    @AttributeDefinition(name = "Keystore Password", type = AttributeType.PASSWORD, required = false, description = "The password value associated to the keystore path specified.")
    String keystore_password() default "changeit";

    @AttributeDefinition(name = "Randomize Password", required = false, description = "Specifies if the defined password will be randomized at the next keystore access. If this value is set to true and the keystore can be accessed, the password will be randomized and this field will automatically set to false.")
    boolean randomize_password() default false;

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

    @AttributeDefinition(name = "CRL Store Path", required = false, description = "Defines the path of the CRL store file, as an absolute path. If left empty, a default store file path will be computed by adding a .crl suffix to the value of the Keystore Path parameter.")
    String crl_store_path() default "";

    @AttributeDefinition(name = "Enable CRL Verification", description = "If set to true, the downloaded CRLs will be stored only if signed with the public key of one of trusted certificates in this keystore.")
    boolean verify_crl() default true;

}



/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates and others
 *
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.kura.core.deployment;

import static java.util.Objects.requireNonNull;

import java.util.Properties;

import org.osgi.service.component.ComponentException;

public class CloudDeploymentHandlerOptions {

    private static final String DPA_CONF_PATH_PROPNAME = "dpa.configuration";
    private static final String PACKAGES_PATH_PROPNAME = "kura.packages";
    private static final String KURA_DATA_DIR = "kura.data";

    private final Properties properties;

    public CloudDeploymentHandlerOptions(Properties properties) {
        requireNonNull(properties, "Properties cannot be null");
        this.properties = properties;
    }

    public String getDpaConfigurationFilePath() {
        String dpaPath = System.getProperty(DPA_CONF_PATH_PROPNAME);
        if (dpaPath == null || dpaPath.isEmpty()) {
            throw new ComponentException("The value of '" + DPA_CONF_PATH_PROPNAME + "' is not defined");
        }
        return dpaPath;
    }

    public String getPackagesPath() {
        String packagesPath = this.properties.getProperty(PACKAGES_PATH_PROPNAME);
        if (packagesPath == null || packagesPath.isEmpty()) {
            throw new ComponentException("The value of '" + PACKAGES_PATH_PROPNAME + "' is not defined");
        }
        
        return packagesPath;
    }

    public String getKuraDataDir() {
        String kuraDataDir = this.properties.getProperty(KURA_DATA_DIR);
        if (kuraDataDir == null || kuraDataDir.isEmpty()) {
            throw new ComponentException("The value of '" + KURA_DATA_DIR + "' is not defined");
        }
        return kuraDataDir;
    }

}

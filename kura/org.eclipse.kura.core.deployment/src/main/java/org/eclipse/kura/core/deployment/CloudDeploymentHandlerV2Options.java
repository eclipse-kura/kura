/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.core.deployment;

import java.io.File;
import java.util.Map;

public class CloudDeploymentHandlerV2Options {

    private ConfigurationProperty<String> PROPERTY_DOWNLOADS_DIRECTORY = new ConfigurationProperty<>(
            "downloads.directory", "/tmp");
    private ConfigurationProperty<String> PROPERTY_HOOK_ASSOCIATIONS = new ConfigurationProperty<>(
            "deployment.hook.associations", "");

    private String downloadsDirectory;
    private String hookAssociations;

    public CloudDeploymentHandlerV2Options(Map<String, Object> properties) {
        this.hookAssociations = PROPERTY_HOOK_ASSOCIATIONS.get(properties);
        final File downloadsDirectory = new File(PROPERTY_DOWNLOADS_DIRECTORY.get(properties));

        boolean isDirectoryValid = true;
        if (!downloadsDirectory.exists()) {
            isDirectoryValid = downloadsDirectory.mkdirs();
        }
        isDirectoryValid = isDirectoryValid && downloadsDirectory.isDirectory();

        this.downloadsDirectory = isDirectoryValid ? downloadsDirectory.getAbsolutePath()
                : PROPERTY_DOWNLOADS_DIRECTORY.defaultValue;
    }

    public String getDownloadsDirectory() {
        return downloadsDirectory;
    }

    public String getHookAssociations() {
        return hookAssociations;
    }

    private static class ConfigurationProperty<T> {

        private final String key;
        private final T defaultValue;

        public ConfigurationProperty(String key, T defaultValue) {
            this.key = key;
            this.defaultValue = defaultValue;
        }

        @SuppressWarnings("unchecked")
        public T get(Map<String, Object> properties) {
            final Object value = properties.get(this.key);
            if (this.defaultValue.getClass().isInstance(value)) {
                return (T) value;
            }
            return defaultValue;
        }
    }

}

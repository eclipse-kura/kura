/*******************************************************************************
 * Copyright (c) 2019, 2020 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.core.linux.executor.unprivileged;

import java.util.Map;

public class UnprivilegedExecutorServiceOptions {

    public static final String PROP_COMMAND_USERNAME = "command.username";

    public static final String PROP_DEFAULT_COMMAND_USERNAME = "kura";

    private final Map<String, Object> properties;

    public UnprivilegedExecutorServiceOptions(Map<String, Object> properties) {
        this.properties = properties;
    }

    public Map<String, Object> getConfigurationProperties() {
        return this.properties;
    }

    /**
     * Returns the command username.
     *
     * @return
     */
    public String getCommandUsername() {
        if (this.properties != null && this.properties.get(PROP_COMMAND_USERNAME) != null
                && this.properties.get(PROP_COMMAND_USERNAME) instanceof String) {
            return (String) this.properties.get(PROP_COMMAND_USERNAME);
        }
        return PROP_DEFAULT_COMMAND_USERNAME;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (this.properties == null ? 0 : this.properties.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        UnprivilegedExecutorServiceOptions other = (UnprivilegedExecutorServiceOptions) obj;
        if (this.properties == null) {
            if (other.properties != null) {
                return false;
            }
        } else if (!this.properties.equals(other.properties)) {
            return false;
        }
        return true;
    }

}

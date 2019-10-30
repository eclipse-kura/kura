/*******************************************************************************
 * Copyright (c) 2019 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
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
        result = prime * result + ((properties == null) ? 0 : properties.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        UnprivilegedExecutorServiceOptions other = (UnprivilegedExecutorServiceOptions) obj;
        if (properties == null) {
            if (other.properties != null)
                return false;
        } else if (!properties.equals(other.properties))
            return false;
        return true;
    }

}

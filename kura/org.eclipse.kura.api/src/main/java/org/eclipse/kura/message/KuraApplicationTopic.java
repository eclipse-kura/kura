/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.message;

import org.osgi.annotation.versioning.ProviderType;

/**
 * Models the application specific part of the topic for messages posted to the Kura platform.
 *
 * @noextend This class is not intended to be subclassed by clients.
 * @since 2.0
 */
@ProviderType
public abstract class KuraApplicationTopic {

    protected String applicationId;
    protected String applicationTopic;

    public String getApplicationId() {
        return this.applicationId;
    }

    public String getApplicationTopic() {
        return this.applicationTopic;
    }
}

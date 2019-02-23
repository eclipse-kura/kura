/*******************************************************************************
 * Copyright (c) 2011, 2018 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
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

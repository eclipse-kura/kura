/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.data;

import org.osgi.annotation.versioning.ProviderType;

/**
 * DataTransportToken is an receipt returned by the {@link DataTransportService} after the publishing of a message.
 * Such receipt can be used to track and compare subsequence message confirmation callbacks.
 * 
 * @noextend This class is not intended to be subclassed by clients.
 */
@ProviderType
public class DataTransportToken {

    private final int messageId;
    private final String sessionId;

    public DataTransportToken(int messageId, String sessionId) {
        super();
        this.messageId = messageId;
        this.sessionId = sessionId;
    }

    public int getMessageId() {
        return this.messageId;
    }

    public String getSessionId() {
        return this.sessionId;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + this.messageId;
        result = prime * result + (this.sessionId == null ? 0 : this.sessionId.hashCode());
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
        DataTransportToken other = (DataTransportToken) obj;
        if (this.messageId != other.messageId) {
            return false;
        }
        if (this.sessionId == null) {
            if (other.sessionId != null) {
                return false;
            }
        } else if (!this.sessionId.equals(other.sessionId)) {
            return false;
        }
        return true;
    }
}

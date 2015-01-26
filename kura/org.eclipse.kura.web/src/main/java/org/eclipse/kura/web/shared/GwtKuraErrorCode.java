/**
 * Copyright (c) 2011, 2014 Eurotech and/or its affiliates
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Eurotech
 */
package org.eclipse.kura.web.shared;

public enum GwtKuraErrorCode {

    DUPLICATE_NAME, 
    CANNOT_REMOVE_LAST_ADMIN,
    ILLEGAL_ACCESS,
    ILLEGAL_ARGUMENT, 
    ILLEGAL_NULL_ARGUMENT, 
    INVALID_USERNAME_PASSWORD,
    INVALID_RULE_QUERY,
    INTERNAL_ERROR, 
    OVER_RULE_LIMIT,
    UNAUTHENTICATED,
    WARNING,
    CURRENT_ADMIN_PASSWORD_DOES_NOT_MATCH,
    OPERATION_NOT_SUPPORTED,
    SERVICE_NOT_ENABLED;
}

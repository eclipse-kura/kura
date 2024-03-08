/*******************************************************************************
 * Copyright (c) 2011, 2024 Eurotech and/or its affiliates and others
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
    SERVICE_NOT_ENABLED,
    CONNECTION_FAILURE,
    MARKETPLACE_COMPATIBILITY_VERSION_UNSUPPORTED,
    RESOURCE_FETCHING_FAILURE,
    CERTIFICATE_PARSE_FAILURE,
    FAILURE_CLOSING_RESOURCES,
    PASSWORD_CHANGE_SAME_PASSWORD,
    PASSWORD_NEVER_SET;
}

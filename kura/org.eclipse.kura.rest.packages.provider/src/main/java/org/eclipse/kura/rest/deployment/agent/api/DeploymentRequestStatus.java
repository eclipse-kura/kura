/*******************************************************************************
 * Copyright (c) 2023 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.rest.deployment.agent.api;

/**
 * Enumeration representing the status of the deployment requests received via REST.
 *
 */
public enum DeploymentRequestStatus {
    REQUEST_RECEIVED,
    INSTALLING,
    UNINSTALLING;
}

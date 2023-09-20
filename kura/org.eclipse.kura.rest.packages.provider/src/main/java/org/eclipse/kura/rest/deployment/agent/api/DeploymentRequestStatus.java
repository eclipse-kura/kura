/*******************************************************************************
 * Copyright (c) 2020 Eurotech and/or its affiliates. All rights reserved.
 *******************************************************************************/
package com.eurotech.framework.rest.deployment.agent.api;

/**
 * Enumeration representing the status of the deployment requests received via REST.
 *
 */
public enum DeploymentRequestStatus {
    REQUEST_RECEIVED,
    INSTALLING,
    UNINSTALLING;
}

/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Eurotech
 *******************************************************************************/
/**
 * Provides services for managing connections between the IoT framework and the remote servers.
 * The term Cloud Connection identifies a set of interfaces that allow to specify and manage the cloud endpoint
 * specified.
 * Using the provided interfaces, the user application is able to publish messages to the cloud platform without knowing
 * the low level specificities of the underneath protocols used to communicate with the cloud platform. Interfaces are
 * available also to register subscribers that will receive and process messages from the cloud platform to the end
 * device.
 *
 */
package org.eclipse.kura.cloudconnection;
/*******************************************************************************
 * Copyright (c) 2018, 2020 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.cloudconnection.publisher;

import org.osgi.annotation.versioning.ProviderType;

/**
 * Marker interface used to identify a Cloud Publisher dedicated to send notifications until completion of a long-lived
 * operation initially handled by a {@link org.eclipse.kura.cloudconnection.request.RequestHandler}.
 *
 * @since 2.0
 * @noimplement This interface is not intended to be implemented by clients.
 */
@ProviderType
public interface CloudNotificationPublisher extends CloudPublisher {

}

/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.cloudconnection.publisher;

import org.eclipse.kura.cloudconnection.request.RequestHandler;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Marker interface used to identify a Cloud Publisher dedicated to send notifications until completion of a long-lived
 * operation initially handled by a {@link RequestHandler}.
 *
 * @since 2.0
 * @noimplement This interface is not intended to be implemented by clients.
 */
@ProviderType
public interface CloudNotificationPublisher extends CloudPublisher {

}

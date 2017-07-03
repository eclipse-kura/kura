/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/

package org.eclipse.kura.db;

import org.osgi.annotation.versioning.ProviderType;

/**
 * A {@link H2DbService} instance provides an implementation of {@link BaseDbService} using the H2 database engine.
 * 
 * @since 1.3
 * @noimplement This interface is not intended to be implemented by clients.
 */
@ProviderType
public interface H2DbService extends BaseDbService {

    public static final String DEFAULT_INSTANCE_PID = "org.eclipse.kura.db.H2DbService";
}

/*******************************************************************************
 * Copyright (c) 2011, 2017 Eurotech and/or its affiliates
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
 * A {@link DbService} instance provides an implementation of {@link BaseDbService} using the HSQL database engine.
 * The purpose of this interface is to provide backwards compatibility to applications that do not support the H2
 * database engine.
 * 
 * @deprecated
 * @noimplement This interface is not intended to be implemented by clients.
 */
@Deprecated
@ProviderType
public interface DbService extends BaseDbService {
}

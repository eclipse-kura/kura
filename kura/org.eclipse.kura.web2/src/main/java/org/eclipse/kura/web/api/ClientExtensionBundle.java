/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.web.api;

import java.util.Map;
import java.util.Set;

public interface ClientExtensionBundle {

    public Map<String, String> getProperties();

    public String getEntryPointUrl();

    public Set<String> getProvidedAuthenticationMethods();
}

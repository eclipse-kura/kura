/*******************************************************************************
 * Copyright (c) 2025 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.identity;

import java.util.Optional;

import org.osgi.annotation.versioning.ProviderType;

/**
 * A service interface that provides messages that should be shown to the user
 * before and/or after login by user interface services (e.g. a web console).
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 * @since 3.0.0
 */
@ProviderType
public interface LoginBannerService {

    /**
     * Returns the message that should be shown before user login.
     * 
     * @return the pre login banner message, or an empty optional if no message should be shown
     */
    public Optional<String> getPreLoginBanner();

    /**
     * Returns the message that should be shown after a successful user login.
     * 
     * @return the post login banner message, or an empty optional if no message should be shown
     */
    public Optional<String> getPostLoginBanner();

}

/*******************************************************************************
 * Copyright (c) 2021 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.security.tamper.detection;

import org.eclipse.kura.KuraException;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Specifies a service that implements tamper detection features.
 * This service introduces the concept of tamper status. The definition of tamper status depends on the implementation,
 * for example a device might be considered to be tampered if its external enclosure has been opened.
 * The tamper status is represented by the {@link TamperStatus} class.
 * The tamper status can be reset by calling the {@link TamperDetectionService#resetTamperStatus()} method.
 * This service must also generate {@link TamperEvent} EventAdmin events in particular conditions, see the corresponding
 * Javadoc for more details.
 *
 * @since 3.0
 * @noimplement This interface is not intended to be implemented by clients.
 */
@ProviderType
public interface TamperDetectionService {

    /**
     * Returns an user friendly name describing this service.
     * 
     * @return an user friendly name describing this service.
     */
    public String getDisplayName();

    /**
     * Returns the current {@link TamperStatus}.
     *
     * @return the current {@link TamperStatus}.
     * @throws KuraException
     *             in case of an implementation failure in determining the tamper status.
     */
    public TamperStatus getTamperStatus() throws KuraException;

    /**
     * Allows to reset the tamper state. After this method returns, the result of calling
     * {@link TamperDetectionService#getTamperStatus()} should have the tamper flag set to false, until the next tamper
     * event is detected.
     *
     * @throws KuraException
     *             in case of reset implementation failure.
     */
    public void resetTamperStatus() throws KuraException;
}

/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.status;

import org.osgi.annotation.versioning.ConsumerType;

/**
 * This interface must be implemented by classes which want to trigger a status change on the
 * {@link CloudConnectionStatusService}
 */
@ConsumerType
public interface CloudConnectionStatusComponent {

    /**
     * Returns the notification priority for this Component. Usually a constant.<br>
     * Priorities range from {@code Integer.MIN_VALUE} to {@code Integer.MAX_VALUE}.<br>
     * <br>
     * Several constants are available for most used priorities:<br>
     * <table summary="">
     * <tbody>
     * <tr>
     * <td>{@code CloudConnectionStatusService.PRIORITY_MAX}</td>
     * <td>Maximum priority</td>
     * </tr>
     * <tr>
     * <td>{@code CloudConnectionStatusService.PRIORITY_CRITICAL}</td>
     * <td>400</td>
     * </tr>
     * <tr>
     * <td>{@code CloudConnectionStatusService.PRIORITY_HIGH}</td>
     * <td>300</td>
     * </tr>
     * <tr>
     * <td>{@code CloudConnectionStatusService.PRIORITY_MEDIUM}</td>
     * <td>200</td>
     * </tr>
     * <tr>
     * <td>{@code CloudConnectionStatusService.PRIORITY_LOW}</td>
     * <td>100</td>
     * </tr>
     * <tr>
     * <td>{@code CloudConnectionStatusService.PRIORITY_MIN}</td>
     * <td>Minimum priority</td>
     * </tr>
     * </tbody>
     * </table>
     *
     * @return An Integer indicating the priority for this component
     */
    public int getNotificationPriority();

    /**
     * Invoked by {@link CloudConnectionStatusService} to retrieve the current {@link CloudConnectionStatusEnum} status
     * for this component
     *
     * @return {@link CloudConnectionStatusEnum} representing the current status of the component
     */
    public CloudConnectionStatusEnum getNotificationStatus();

    /**
     * Invoked internally by {@link CloudConnectionStatusService} to persist the status of the component
     *
     * @param status
     *            New status of this component
     */
    public void setNotificationStatus(CloudConnectionStatusEnum status);

}

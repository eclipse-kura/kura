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
package org.eclipse.kura.core.status;

import org.eclipse.kura.status.CloudConnectionStatusComponent;
import org.eclipse.kura.status.CloudConnectionStatusEnum;
import org.eclipse.kura.status.CloudConnectionStatusService;

public class IdleStatusComponent implements CloudConnectionStatusComponent {

    @Override
    public int getNotificationPriority() {
        return CloudConnectionStatusService.PRIORITY_MIN;
    }

    @Override
    public CloudConnectionStatusEnum getNotificationStatus() {
        return CloudConnectionStatusEnum.OFF;
    }

    @Override
    public void setNotificationStatus(CloudConnectionStatusEnum status) {
        // We need a always present minimum priority status of OFF, se we don't want
        // the default notification status to be changed.

        // Do nothing
    }

}

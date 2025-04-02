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
 *******************************************************************************/

package org.eclipse.kura.linux.position.provider.condition;

import org.eclipse.kura.linux.position.provider.LinuxPositionProviderConstants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.condition.Condition;

@Component(property = Condition.CONDITION_ID + "=" + LinuxPositionProviderConstants.CONDITION_ID)
public class LinuxPositionServiceCondition implements Condition {
}
